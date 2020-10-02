package blindsend;

import api.BlindsendAPI;
import crypto.CryptoFactory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BlindsendUtil;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

/**
 * Class {@code FileSender} provides methods for encrypting and uploading encrypted files to blindsend.
 * These methods are responsible for generating necessary cryptographic information and for communication
 * with the blindsend API in the correct workflow.
 */
public class FileSender {

    final static Logger LOGGER = LogManager.getLogger(FileSender.class);

    private BlindsendAPI api;

    /**
     * Creates new {@code FileSender}
     * @param api Blindsend API
     */
    public FileSender(BlindsendAPI api){
        this.api = api;
    }

    /**
     * Encrypts a file found at <i>inputFilePath</i> and uploads it to blindsend.
     * Called for sending files in the <i>request</i> use case, when there is a file request from the Receiver.
     * <p>
     * Encrypted file can be uploaded in chunks of specified size.
     * </p>
     * @param linkUrl File exchange link, obtained from the Receiver
     * @param inputFilePath Path to a file to be exchanged
     * @param chunkSize Size in bytes of one upload chunk. If null, 0 or greater than file size, the file is uploaded in one piece
     */
    public void encryptAndSendFile(URL linkUrl, Path inputFilePath, Optional<Integer> chunkSize) throws GeneralSecurityException, IOException, InterruptedException {
        String linkId = BlindsendUtil.extractLinkId(linkUrl.toString());

        byte[] pkReceiverBytes = BlindsendUtil.toByte(BlindsendUtil.extractUriFragment(linkUrl.toString()));
        KeyFactory kf = KeyFactory.getInstance("XDH", "BC");
        PublicKey pkReceiver = kf.generatePublic(new X509EncodedKeySpec(pkReceiverBytes));

        String uploadId = this.api.prepareUpload(linkId);

        KeyPair keyPairSender = CryptoFactory.generateKeyPair();
        byte[] masterKey = CryptoFactory.generateMasterKey(keyPairSender.getPrivate(), pkReceiver);

        String encryptedFilePath = System.getProperty("java.io.tmpdir") + "tempUploadedEncrypted";
        File inputFile = new File(inputFilePath.toString());
        String fileName = inputFile.getName();
        LOGGER.info("Loaded file for encryption " + inputFilePath);

        CryptoFactory.encryptAndSaveFile(masterKey, inputFile, encryptedFilePath);

        File encryptedFile = new File(encryptedFilePath);
        long fileSize = encryptedFile.length();
        Optional<String> uploadIdOpt = Optional.of(uploadId);
        uploadChunks(linkId, uploadIdOpt, encryptedFile, chunkSize.orElse(0));

        this.api.finishUpload(
                linkId,
                keyPairSender.getPublic().getEncoded(),
                "",
                fileName,
                fileSize
        );
    }

    /**
     * Encrypts a file found at <i>inputFilePath</i> and uploads it to blindsend.
     * Called for sending files in the <i>send</i> use case.
     * <p>
     * Encrypted file can be uploaded in chunks of specified size.
     * </p>
     * @param pass Optional password. If provided, it must be passed and used by file Receiver. Files are still end-to-end
     *             encrypted even without the password.
     * @param inputFilePath Path to a file to be exchanged
     * @param chunkSize Size in bytes of one upload chunk. If null, 0 or greater than file size, the file is uploaded in one piece
     * @return File exchange link, to be passed to the Receiver for downloading the file
     */
    public URL encryptAndSendFile(String pass, Path inputFilePath, Optional<Integer> chunkSize) throws NoSuchProviderException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, IOException {
        byte[] seed1 = CryptoFactory.generateRandom(16);
        byte[] kdfSalt = CryptoFactory.generateRandom(16);
        int kdfOps = 1;
        int kdfMemLimit = 8192;
        byte[] fileMetadataEncNonce = CryptoFactory.generateRandom(16);
        byte[] fileMetadataKey = CryptoFactory.kdf(seed1, "filemeta".getBytes());
        byte[] seed2 = CryptoFactory.generateSeed(pass, kdfSalt, kdfOps, kdfMemLimit);
        byte[] hash = CryptoFactory.hash(BlindsendUtil.concatenate(seed1, seed2));
        byte[] fileKey = CryptoFactory.kdf(hash, "filekey-".getBytes());
        byte[] fileEncNonce = CryptoFactory.generateRandom(16);

        File inputFile = new File(inputFilePath.toString());
        LOGGER.info("Loaded file for encryption " + inputFilePath);
        String fileName = inputFile.getName();
        long fileSize = inputFile.length();
        String fileMetadata = fileName + "-" + fileSize;
        byte[] encryptedFileMetadata = CryptoFactory.encryptFileMetadata(fileMetadata.getBytes(), fileMetadataKey, fileMetadataEncNonce);

        String encryptedFilePath = System.getProperty("java.io.tmpdir") + "tempUploadedEncrypted";
        CryptoFactory.encryptAndSaveFile(fileKey, inputFile, encryptedFilePath);
        File encryptedFile = new File(encryptedFilePath);
        long encFileSize = encryptedFile.length();

        String linkId = this.api.initializeSendSession(kdfSalt, kdfOps, kdfMemLimit, fileEncNonce, fileMetadataEncNonce, encFileSize, encryptedFileMetadata);
        Optional<String> uploadId = Optional.empty();
        URL link = uploadChunks(linkId, uploadId, encryptedFile, chunkSize.orElse(0));

        return new URL(link.toString() + "#" + BlindsendUtil.toHex(seed1));
    }

    private URL uploadChunks(String linkId, Optional<String> uploadId, File encryptedFile, int chunkSize) throws IOException {
        byte[] encryptedFileBytes = FileUtils.readFileToByteArray(encryptedFile);
        long encryptedFileSize = encryptedFile.length();
        if(uploadId.isPresent())
            this.api.initializeFileUpload(linkId, uploadId.get(), encryptedFileSize);

        if (chunkSize == 0 || encryptedFileSize <= chunkSize) {
            if (uploadId.isPresent()) {
                this.api.uploadFileChunk(linkId, uploadId.get(), 1, (int) encryptedFileSize, true, encryptedFileBytes);
                return null;
            } else
                return this.api.uploadFileChunk(linkId, 1, (int) encryptedFileSize, true, encryptedFileBytes);
        } else {
            long numChunks = Math.floorDiv(encryptedFileSize, chunkSize);
            long lastChunkSize = Math.floorMod(encryptedFileSize, chunkSize);
            InputStream encStream = new FileInputStream(encryptedFile);
            for(int i=1; i<=numChunks; i++){
                byte[] chunkBytes = new byte[chunkSize];
                encStream.read(chunkBytes);
                if(uploadId.isPresent())
                    this.api.uploadFileChunk(linkId, uploadId.get(), i, chunkSize, false, chunkBytes);
                else
                    this.api.uploadFileChunk(linkId, i, chunkSize, false, chunkBytes);
            }
            byte[] chunkBytes = new byte[(int)lastChunkSize];
            encStream.read(chunkBytes);
            if(uploadId.isPresent()) {
                this.api.uploadFileChunk(linkId, uploadId.get(), (int) (numChunks + 1), (int) lastChunkSize, true, chunkBytes);
                return null;
            }
            else
                return this.api.uploadFileChunk(linkId, (int)(numChunks+1), (int)lastChunkSize, true, chunkBytes);
        }
    }
}

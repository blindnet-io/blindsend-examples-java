package blindsend;

import api.BlindsendAPI;
import crypto.CryptoFactory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BlindsendUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

/**
 * The FileSender class provides methods for encrypting and uploading encrypted files to blindsend
 */
public class FileSender {

    final static Logger LOGGER = LogManager.getLogger(FileSender.class);

    private BlindsendAPI api;

    /**
     * Creates new FileSender
     */
    public FileSender(BlindsendAPI api){
        this.api = api;
    }

    /**
     * Encrypts a file from inputFilePath and sends it to blindsend
     * Also saves the encrypted file to encryptedFilePath
     * Encrypted file can be uploaded in chunks of specified size
     * @param linkUrl File exchange link
     * @param inputFilePath Path to a file to be exchanged
     * @param chunkSize Size in bytes of one upload chunk. If 0, the file is uploaded in one piece
     */
    public void encryptAndSendFile(URL linkUrl, Path inputFilePath, int chunkSize) throws GeneralSecurityException, IOException, InterruptedException {
        String linkId = BlindsendUtil.extractLinkId(linkUrl.toString());

        byte[] pkReceiverBytes = BlindsendUtil.toByte(BlindsendUtil.extractKey(linkUrl.toString()));
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

        uploadChunks(linkId, uploadId, encryptedFilePath, chunkSize);

        File encryptedFile = new File(encryptedFilePath);
        long fileSize = encryptedFile.length();

        this.api.finishUpload(
                linkId,
                keyPairSender.getPublic().getEncoded(),
                "",
                fileName,
                fileSize
        );
    }

    private void uploadChunks(String linkId, String uploadId, String filePath, int chunkSize) throws IOException, InterruptedException {
        File encryptedFile = new File(filePath);
        byte[] encryptedFileBytes = FileUtils.readFileToByteArray(new File(filePath));
        long enciptedFileSize = encryptedFile.length();
        this.api.initializeFileUpload(linkId, uploadId, enciptedFileSize);

        if (chunkSize == 0 || enciptedFileSize <= chunkSize)
            this.api.uploadFileChunk(linkId, uploadId, 1, (int)enciptedFileSize, true, encryptedFileBytes);
        else {
            int id = 0;
            long numChunks = Math.floorDiv(enciptedFileSize, chunkSize);
            long lastChunkSize = Math.floorMod(enciptedFileSize, chunkSize);
            InputStream encStream = new FileInputStream(encryptedFile);
            for(int i=1; i<=numChunks; i++){
                byte[] chunkBytes = new byte[chunkSize];
                encStream.read(chunkBytes);
                this.api.uploadFileChunk(linkId, uploadId, i, chunkSize, false, chunkBytes);
                Thread.sleep(5000);
            }
            byte[] chunkBytes = new byte[(int)lastChunkSize];
            encStream.read(chunkBytes);
            this.api.uploadFileChunk(linkId, uploadId, (int)(numChunks+1), (int)lastChunkSize, true, chunkBytes);
        }
    }
}

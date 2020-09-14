package blindsend;

import api.BlindsendAPI;
import crypto.CryptoFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BlindsendUtil;
import util.ContHandshakeResp;

import java.io.File;
import java.io.IOException;
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
     * Encrypts a file from inputFilePath and sends it to blindsend.
     * Also saves the encrypted file to encryptedFilePath
     * @param linkUrl File exchange link
     * @param inputFilePath Path to a file to be exchanged
     */
    public void encryptAndSendFile(URL linkUrl, Path inputFilePath) throws GeneralSecurityException, IOException  {
        String linkId = BlindsendUtil.extractLinkId(linkUrl.toString());
        ContHandshakeResp contResp = this.api.continueHandshake(linkId);

        byte[] pkRequestorBytes = contResp.getPkRequestor();
        String uploadId = contResp.getUploadId();

        KeyPair keyPairSender = CryptoFactory.generateKeyPair();
        
        KeyFactory kf = KeyFactory.getInstance("XDH", "BC");

        PublicKey pkRequestor = kf.generatePublic(new X509EncodedKeySpec(pkRequestorBytes));

        byte[] masterKey = CryptoFactory.generateMasterKey(keyPairSender.getPrivate(), pkRequestor);

        byte[] masterKeyHash = CryptoFactory.generateSkEncryptionKeyHash(masterKey);
        String encryptedFilePath = System.getProperty("java.io.tmpdir") + "tempUploadedEncrypted";

        File inputFile = new File(inputFilePath.toString());
        String fileName = inputFile.getName();
        LOGGER.info("Loaded file for encryption " + inputFilePath);

        CryptoFactory.encryptAndSaveFile(masterKey, inputFile, encryptedFilePath);

        this.api.uploadFile(linkId, uploadId, encryptedFilePath);

        File encryptedFile = new File(encryptedFilePath);
        long fileSize = encryptedFile.length();

        this.api.finishHandshake(
                linkId,
                keyPairSender.getPublic().getEncoded(),
                masterKeyHash,
                "",
                fileName,
                fileSize
        );
    }
}

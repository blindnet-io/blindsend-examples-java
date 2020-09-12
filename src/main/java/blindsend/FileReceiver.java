package blindsend;

import api.BlindsendAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Keys;
import crypto.CryptoFactory;
import util.BlindsendUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * The FileReceiver class provides methods for requesting links and receiving encrypted files from blindsend. It also handles
 * decryption of the received files
 */
public class FileReceiver {

    final static Logger LOGGER = LogManager.getLogger(FileReceiver.class);

    private BlindsendAPI api;

    /**
     * Creates new FileReceiver
     */
    public FileReceiver(BlindsendAPI api){
        this.api = api;
    }

    /**
     * Obtains a link for file exchange via blindsend
     * @param pass Password
     * @return File exchange link
     */
    public URL getLink(String pass) throws IOException, GeneralSecurityException {
        String linkId = this.api.getLinkId();

        KeyPair keyPairRequestor = CryptoFactory.generateKeyPair();

        byte[] kdfSalt = CryptoFactory.generateRandom(16);
        int kdfOps = 1;
        int kdfMemLimit = 8192;
        byte[] skEncryptionKey = CryptoFactory.generateSkEncryptionKey(pass, kdfSalt, kdfOps, kdfMemLimit);
        byte[] skEncryptionKeyHash = CryptoFactory.generateSkEncryptionKeyHash(skEncryptionKey);
        byte[] skEncryptionIv = CryptoFactory.generateRandom(24);
        byte[] encryptedSK = CryptoFactory.encryptSK(keyPairRequestor.getPrivate().getEncoded(), skEncryptionKey, skEncryptionIv);

        String link = this.api.beginHandshake(
                linkId,
                keyPairRequestor.getPublic().getEncoded(),
                skEncryptionIv,
                encryptedSK,
                kdfSalt,
                kdfOps,
                kdfMemLimit,
                skEncryptionKeyHash
        );
        return new URL(link);
    }

    /**
     * Downloads encrypted file from blindsend, and decrypts it to decryptedFilePath
     * @param linkUrl File exchange link
     * @param pass Password
     * @param decryptedFileFolder Folder to save decrypted file into
     */
    public void receiveAndDecryptFile(URL linkUrl, String pass, Path decryptedFileFolder) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        String tempFilePath = System.getProperty("java.io.tmpdir") + "tempDownloadedEncrypted";
        String linkId = BlindsendUtil.extractLinkId(linkUrl.toString());
        String fileName = this.api.getFileName(linkId);
        String decryptedFilePath = decryptedFileFolder + "/" + fileName;
        Keys keys = this.api.getKeys(linkId);

        byte[] kdfSalt = keys.getKdfSalt();
        int kdfOps = keys.getKdfOps();
        int kdfMemLimit = keys.getKdfMemLimit();
        byte[] skEncryptionKey = CryptoFactory.generateSkEncryptionKey(pass, kdfSalt, kdfOps, kdfMemLimit);

        byte[] skEncryptionIv = keys.getSkEncryptionIv();
        byte[] encryptedSK = keys.getEncryptedSK();
        byte[] decryptedSK = CryptoFactory.decryptSK(encryptedSK, skEncryptionKey, skEncryptionIv);

        KeyFactory kf = KeyFactory.getInstance("XDH");
        
        PrivateKey decryptedSKAsPrivateK = kf.generatePrivate(new PKCS8EncodedKeySpec(decryptedSK));

        byte[] pkSenderBytes = keys.getPkSender();
        PublicKey pkSender = kf.generatePublic(new X509EncodedKeySpec(pkSenderBytes));

        byte[] masterKey2 = CryptoFactory.generateMasterKey(decryptedSKAsPrivateK, pkSender);

        byte[] skEncryptionKeyHash = CryptoFactory.generateSkEncryptionKeyHash(skEncryptionKey);
        File encryptedFile = this.api.downloadFile(linkId, skEncryptionKeyHash, tempFilePath);

        LOGGER.info("Decrypting saved file to " + decryptedFileFolder + fileName);
        CryptoFactory.decryptAndSaveFile(masterKey2, encryptedFile, decryptedFilePath);
    }
}

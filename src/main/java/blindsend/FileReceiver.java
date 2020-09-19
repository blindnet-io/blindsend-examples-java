package blindsend;

import api.BlindsendAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Keys;
import crypto.CryptoFactory;
import util.BlindsendUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
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

        byte[] kdfSalt = CryptoFactory.generateRandom(16);
        int kdfOps = 1;
        int kdfMemLimit = 8192;
        byte[] passSeed = CryptoFactory.generateKeyPairSeed(pass, kdfSalt, kdfOps, kdfMemLimit);
        KeyPair keyPairReceiver = CryptoFactory.generateKeyPair(passSeed);
        String pkReceiver = BlindsendUtil.toHex(keyPairReceiver.getPublic().getEncoded());

        String link = this.api.initializeSession(
                linkId,
                kdfSalt,
                kdfOps,
                kdfMemLimit
        );
        return new URL(link + "#" + pkReceiver);
    }

    /**
     * Downloads encrypted file from blindsend, and decrypts it to decryptedFilePath
     * @param linkUrl File exchange link
     * @param pass Password
     * @param decryptedFileFolder Folder to save decrypted file into
     */
    public void receiveAndDecryptFile(URL linkUrl, String pass, Path decryptedFileFolder) throws GeneralSecurityException, IOException {
        String tempFilePath = System.getProperty("java.io.tmpdir") + "tempDownloadedEncrypted";
        String linkId = BlindsendUtil.extractLinkId(linkUrl.toString());

        String fileName = this.api.getFileName(linkId);
        String decryptedFilePath = decryptedFileFolder + "/" + fileName;
        Keys keys = this.api.getKeys(linkId);

        byte[] kdfSalt = keys.getKdfSalt();
        int kdfOps = keys.getKdfOps();
        int kdfMemLimit = keys.getKdfMemLimit();

        KeyFactory kf = KeyFactory.getInstance("XDH", "BC");
        byte[] pkSenderBytes = keys.getPkSender();
        PublicKey pkSender = kf.generatePublic(new X509EncodedKeySpec(pkSenderBytes));

        byte[] passSeed = CryptoFactory.generateKeyPairSeed(pass, kdfSalt, kdfOps, kdfMemLimit);
        KeyPair keyPairReceiver = CryptoFactory.generateKeyPair(passSeed);
        PrivateKey skReceiver = keyPairReceiver.getPrivate();

        byte[] masterKey = CryptoFactory.generateMasterKey(skReceiver, pkSender);

        File encryptedFile = this.api.downloadFile(linkId, tempFilePath);

        LOGGER.info("Decrypting saved file to " + decryptedFileFolder + "/" + fileName);
        CryptoFactory.decryptAndSaveFile(masterKey, encryptedFile, decryptedFilePath);
    }
}

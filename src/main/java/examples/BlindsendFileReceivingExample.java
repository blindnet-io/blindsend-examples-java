package examples;

import api.BlindsendAPI;
import blindsend.FileReceiver;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable example for receiving files via blindsend
 * When run, the example will use blindsend link to download a file and decrypted it and save it to disk.
 * NOTE: the example requires blindsend link to be specified in link attribute
 */
public class BlindsendFileReceivingExample {

    public static void main(String[] args) throws MalformedURLException {
        Security.addProvider(new BouncyCastleProvider());

        Path decryptedFilePath = Paths.get("src/main/resources/files/pcr_decrypted.pdf");

        BlindsendAPI api = new BlindsendAPI("https://blindsend.tech/api");
        FileReceiver receiver = new FileReceiver(api);

        URL link = new URL(""); // insert link here

        try {
            receiver.receiveAndDecryptFile(
                    link,
                    "mypass",
                    decryptedFilePath
            );
        } catch (InvalidKeySpecException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "InvalidKeySpecException", e);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "NoSuchAlgorithmException", e);
        } catch (IOException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "IOException", e);
        } catch (NoSuchProviderException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "NoSuchProviderException", e);
        } catch (InvalidKeyException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "InvalidKeyException", e);
        } catch (NoSuchPaddingException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "NoSuchPaddingException", e);
        } catch (IllegalBlockSizeException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "IllegalBlockSizeException", e);
        } catch (BadPaddingException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "BadPaddingException", e);
        } catch (InvalidAlgorithmParameterException e) {
            Logger.getLogger(BlindsendFileReceivingExample.class.getName()).log(Level.SEVERE, "InvalidAlgorithmParameterException", e);
        }
    }
}

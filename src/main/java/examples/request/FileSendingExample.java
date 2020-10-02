package examples.request;

import api.BlindsendAPI;
import blindsend.FileReceiver;
import blindsend.FileSender;
import org.apache.logging.log4j.LogManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable example for sending files via blindsend in the <i>request</i> use case.
 * When run, the example will generate file sharing link as Receiver, and then encrypt a file and
 * upload it to blindsend as Sender.
 * <p>
 * The first element of the argument of the main method specifies the path to a file to send. If not specified,
 * the example will use default file located in the {@code resources/files} folder.
 * </p>
 * <p>
 * This example acts both as file Receiver (to request file exchange and obtain file exchange link),
 * and as file Sender (to send the requested file using obtained link).
 * </p>
 * <p>
 * Use the generated link for receiving the file in {@code request.FileReceivingExample}.
 * </p>
 */
public class FileSendingExample {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(FileSendingExample.class.getName());

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        Path fileToSendPath;
        if (args.length == 0)
            fileToSendPath = Paths.get("src/main/resources/files/pcr.pdf");
        else
            fileToSendPath = Paths.get(args[0]);

        BlindsendAPI api = new BlindsendAPI("https://blindsend.tech/api");
        LOGGER.info("Blindsend API endpoint: " + api.getEndpoint());
        FileReceiver receiver = new FileReceiver(api);
        FileSender sender = new FileSender(api);

        Optional<Integer> numChunks = Optional.empty();
        String password = "mypass";
        try {
            URL link = receiver.generateFileExchangeLink(password);
            sender.encryptAndSendFile(
                    link,
                    fileToSendPath,
                    numChunks
            );
            LOGGER.info("Blindsend file exchange link: " + link.toString());
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "NoSuchAlgorithmException", e);
        } catch (InvalidKeySpecException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "InvalidKeySpecException", e);
        } catch (IOException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "IOException", e);
        } catch (GeneralSecurityException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "GeneralSecurityException", e);
        } catch (NullPointerException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "NullPointerException", e);
        } catch (InterruptedException e) {
            Logger.getLogger(FileSendingExample.class.getName()).log(Level.SEVERE, "InterruptedException", e);
        }
    }
}

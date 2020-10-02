package examples.send;

import api.BlindsendAPI;
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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable example for sending files via blindsend in the <i>send</i> use case.
 * When run, the example will encrypt a file and upload it to blindsend as Sender.
 * After uploading, file exchange link is obtained.
 * <p>
 * The first element of the argument of the main method specifies the path to a file to send. If not specified,
 * the example will use default file located in the {@code resources/files} folder.
 * </p>
 * <p>
 * Use the generated link for receiving the file in {@code send.FileReceivingExample}.
 * </p>
 */
public class FileSendingExample {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(examples.request.FileSendingExample.class.getName());

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        Path fileToSendPath;
        if (args.length == 0)
            fileToSendPath = Paths.get("src/main/resources/files/pcr.pdf");
        else
            fileToSendPath = Paths.get(args[0]);

        BlindsendAPI api = new BlindsendAPI("https://blindsend.tech/api");
        LOGGER.info("Blindsend API endpoint: " + api.getEndpoint());
        FileSender sender = new FileSender(api);

        Optional<Integer> numChunks = Optional.empty();
        String password = "mypass";
        try {
            URL link = sender.encryptAndSendFile(
                    password,
                    fileToSendPath,
                    numChunks
            );
            LOGGER.info("Blindsend file exchange link: " + link.toString());
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(examples.request.FileSendingExample.class.getName()).log(Level.SEVERE, "NoSuchAlgorithmException", e);
        } catch (IOException e) {
            Logger.getLogger(examples.request.FileSendingExample.class.getName()).log(Level.SEVERE, "IOException", e);
        } catch (GeneralSecurityException e) {
            Logger.getLogger(examples.request.FileSendingExample.class.getName()).log(Level.SEVERE, "GeneralSecurityException", e);
        } catch (NullPointerException e) {
            Logger.getLogger(examples.request.FileSendingExample.class.getName()).log(Level.SEVERE, "NullPointerException", e);
        }
    }
}

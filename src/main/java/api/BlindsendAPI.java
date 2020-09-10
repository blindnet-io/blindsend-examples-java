package api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Keys;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import util.BlindsendUtil;
import util.ContHandshakeResp;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The BlindsendAPI class provides methods for the communication with blindsend REST API
 */
public class BlindsendAPI {

    final static Logger LOGGER = LogManager.getLogger(BlindsendAPI.class);

    private String endpoint;

    final String link = "link";
    final String linkId = "link_id";
    final String publicKey = "public_key";
    final String skEncryptionNonce = "secret_key_encryption_nonce";
    final String encryptedSK = "encrypted_secret_key";
    final String kdfSalt = "kdf_salt";
    final String kdfOps = "kdf_ops";
    final String kdfMemLimit = "kdf_memory_limit";
    final String keyHash = "key_hash";
    final String publicKey1 = "pk1";
    final String uploadId = "upload_id";
    final String publicKey2 = "pk2";
    final String header = "header";
    final String fileName = "file_name";
    final String fileSize = "file_size";
    final String streamEncHeader = "stream_enc_header";
    final String pk1Resp = "public_key_1";
    final String pk2Resp = "public_key_2";

    public BlindsendAPI(String endpoint) {
        this.endpoint = endpoint;
    }

  
    /**
     * Calls blindsend API to obtain linkId.
     * @return Link id
     * @throws IOException
     */
    public String getLinkId() throws IOException {
        URL urlForGetRequest = new URL(endpoint + "/get-link");
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("GET");

        int responseCode = conection.getResponseCode();
        LOGGER.info("/get-link Response code " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in .readLine()) != null) {
                response.append(readLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String linkId = json.getString(this.linkId);
            return linkId;
        } else {
            throw new RuntimeException("/get-link on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to submit receiver's cryptographic information and obtain file exchange link. Called by file receiver
     * This handshake is the first exchange of information by file receiver with blindsend, needed for private file exchange
     * @param linkId Link id obtained from blindsend API
     * @param pkRequestor Public key of file requestor
     * @param skEncryptionNonce Nonce for the encryption of secret key
     * @param encryptedSK Encrypted secret key
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @param skEncryptionKeyHash Hash of the secret key encryption key
     * @return Blindsend link for file exchange
     * @throws IOException
     */
    public String beginHandshake(
            String linkId,
            byte[] pkRequestor,
            byte[] skEncryptionNonce,
            byte[] encryptedSK,
            byte[] kdfSalt,
            int kdfOps,
            int kdfMemLimit,
            byte[] skEncryptionKeyHash
    ) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.publicKey + "\": \"" + BlindsendUtil.toHex(pkRequestor) + "\",\r\n" +
                "   \"" + this.skEncryptionNonce + "\": \"" + BlindsendUtil.toHex(skEncryptionNonce) + "\",\r\n" +
                "   \"" + this.encryptedSK + "\": \"" + BlindsendUtil.toHex(encryptedSK) + "\",\r\n" +
                "   \"" + this.kdfSalt + "\": \"" + BlindsendUtil.toHex(kdfSalt) + "\",\r\n" +
                "   \"" + this.kdfOps + "\": " + kdfOps + ",\r\n" +
                "   \"" + this.kdfMemLimit +"\": " + kdfMemLimit + " ,\r\n" +
                "   \"" + this.keyHash + "\": \"" + BlindsendUtil.toHex(skEncryptionKeyHash) + "\" \n}";

        URL obj = new URL(endpoint + "/begin-hs");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/begin-hs Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String link = json.getString(this.link);
            LOGGER.info("Obtained link from /begin-hs: " + link);
            return link;
        } else {
            throw new RuntimeException("/begin-hs on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to obtain receiver's cryptographic information. Called by file sender
     * This handshake is the first exchange of information by file sender with blindsend, needed for private file exchange
     * @param linkId Link id extracted from blindsend link
     * @return ContHandshakeResp object, containing receiver's public key and upload id
     * @throws IOException
     */
    public ContHandshakeResp continueHandshake(String linkId) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/cont-hs");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/cont-hs Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String pkReqHex = json.getString(this.publicKey1);
            String uploadId = json.getString(this.uploadId);
            return new ContHandshakeResp(BlindsendUtil.toByte(pkReqHex), uploadId);
        } else {
            throw new RuntimeException("/cont-hs on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to upload the encrypted file
     * @param linkId Link id
     * @param uploadId Upload id
     * @param filePath Path to encrypted file to be sent to blindsend
     * @throws IOException
     */
    public void uploadFile(String linkId, String uploadId, String filePath) throws IOException{
        byte[] fileAsBytes = FileUtils.readFileToByteArray(new File(filePath));
        LOGGER.info("Loaded file to send to API " + filePath);
        final byte[] POST_PARAMS = fileAsBytes;

        URL obj = new URL(endpoint + "/send-file/" + linkId + "/" + uploadId);
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS);
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/send-file Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
        } else {
            throw new RuntimeException("/send-file on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to submit cryptographic information related to file encryption. Called by file sender after encryption
     * and uploading of the file
     * This handshake is the second exchange of information by file sender with blindsend, performed after uploading encrypted file
     * @param linkId Link id
     * @param pkSender Public key of a sender
     * @param masterKeyHash Hash of the master key (file encryption key)
     * @param streamEncryptionHeader Stream encryption header
     * @param fileName Name of the exchanged file
     * @param fileSize Size of the exchanged file in bytes
     * @throws IOException
     */
    public void finishHandshake(
            String linkId,
            byte[] pkSender,
            byte[] masterKeyHash,
            String streamEncryptionHeader,
            String fileName,
            long fileSize
    ) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.publicKey2 + "\": \"" + BlindsendUtil.toHex(pkSender) + "\",\r\n" +
                "   \"" + this.keyHash + "\": \"" + BlindsendUtil.toHex(masterKeyHash) + "\",\r\n" +
                "   \"" + this.header + "\": \"" + streamEncryptionHeader + "\",\r\n" +
                "   \"" + this.fileName + "\": \"" + fileName + "\",\r\n" +
                "   \"" + this.fileSize + "\": " + fileSize + " \n}";

        URL obj = new URL(endpoint + "/finish-hs");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/finish-hs Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
        } else {
            throw new RuntimeException("/finish-hs on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to obtain cryptographic information necessary for decryption of the file. Called by file receiver
     * @param linkId Link id
     * @return Keys object, containing cryptographic information necessary for decryption of the file
     * @throws IOException
     */
    public Keys getKeys(String linkId) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/get-keys");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/get-keys Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String pkSenderHex = json.getString(this.pk2Resp);
            String pkRequestorHex = json.getString(this.pk1Resp);
            String skEncryptionNonce = json.getString(this.skEncryptionNonce);
            String encryptedSK = json.getString(this.encryptedSK);
            String kdfSalt = json.getString(this.kdfSalt);
            int kdfOps = json.getInt(this.kdfOps);
            int kdfMemLimit = json.getInt(this.kdfMemLimit);
            String streamEncryptionHeader = json.getString(this.streamEncHeader);
            return new Keys(
                    BlindsendUtil.toByte(pkSenderHex),
                    BlindsendUtil.toByte(pkRequestorHex),
                    BlindsendUtil.toByte(skEncryptionNonce),
                    BlindsendUtil.toByte(encryptedSK),
                    BlindsendUtil.toByte(kdfSalt),
                    kdfOps,
                    kdfMemLimit,
                    streamEncryptionHeader
            );
        } else {
            throw new RuntimeException("/get-keys on BlindsendAPI failed");
        }
    }

//    wrapuj byte[] u klasu
//     public static File downloadFile(URL linkId, byte[] skEncryptionKeyHash, Path downloadPath) {
//         
//     }
    
    /**
     * Calls blindsend API to download encrypted file
     * @param linkId Link id
     * @param skEncryptionKeyHash Hash of the secret key encryption key
     * @param downloadPath Path to a file for downloaded encrypted file
     * @return Encrypted file
     * @throws IOException
     */
    public File downloadFile(String linkId, byte[] skEncryptionKeyHash, String downloadPath) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.keyHash + "\": \"" + BlindsendUtil.toHex(skEncryptionKeyHash) + "\" \n}";

        URL obj = new URL(endpoint + "/get-file");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();


        int responseCode = postConnection.getResponseCode();
        LOGGER.info("/get-file Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            byte[] fileBytes = IOUtils.toByteArray(postConnection.getInputStream());
            FileUtils.writeByteArrayToFile(new File(downloadPath), fileBytes);
            LOGGER.info("File obtained from the API saved to " + downloadPath);
            return new File(downloadPath);
        } else {
            throw new RuntimeException("/get-file on BlindsendAPI failed");
        }
    }
}

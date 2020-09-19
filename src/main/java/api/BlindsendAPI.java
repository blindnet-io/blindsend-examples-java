package api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Keys;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import util.BlindsendUtil;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The BlindsendAPI class provides methods for the communication with blindsend REST API (v0.1.0)
 */
public class BlindsendAPI {

    final static Logger LOGGER = LogManager.getLogger(BlindsendAPI.class);

    private String endpoint;

    final String link = "link";
    final String linkId = "link_id";
    final String kdfSalt = "kdf_salt";
    final String kdfOps = "kdf_ops";
    final String kdfMemLimit = "kdf_memory_limit";
    final String uploadId = "upload_id";
    final String publicKey2 = "pk2";
    final String header = "header";
    final String fileName = "file_name";
    final String fileSize = "file_size";
    final String streamEncHeader = "stream_enc_header";
    final String pk2Resp = "public_key_2";

    public BlindsendAPI(String endpoint) {
        this.endpoint = endpoint;
    }
  
    /**
     * Calls blindsend API to obtain link Id.
     * @return Link id
     * @throws IOException
     */
    public String getLinkId() throws IOException {
        URL urlForGetRequest = new URL(endpoint + "/request/init-link-id");
        String readLine = null;
        HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
        conection.setRequestMethod("GET");

        int responseCode = conection.getResponseCode();
        LOGGER.info("/request/init-link-id Response code " + responseCode);
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
            throw new RuntimeException("/request/init-link-id on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to submit receiver's cryptographic information and obtain file exchange link. Called by file receiver
     * This request is the first exchange of information by file receiver with blindsend, needed for private file exchange
     * @param linkId Link id obtained from blindsend API
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @return Blindsend link for file exchange
     * @throws IOException
     */
    public String initializeSession(
            String linkId,
            byte[] kdfSalt,
            int kdfOps,
            int kdfMemLimit
    ) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.kdfSalt + "\": \"" + BlindsendUtil.toHex(kdfSalt) + "\",\r\n" +
                "   \"" + this.kdfOps + "\": " + kdfOps + ",\r\n" +
                "   \"" + this.kdfMemLimit +"\": " + kdfMemLimit + " \n}";

        URL obj = new URL(endpoint + "/request/init-session");
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
        LOGGER.info("/request/init-session Response Code :  " + responseCode);
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
            LOGGER.info("Obtained link from /init-session: " + link);
            return link;
        } else {
            throw new RuntimeException("/request/init-session on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to obtain upload Id. Called by file sender
     * This request is the first exchange of information by file sender with blindsend, needed for private file exchange
     * @param linkId Link id extracted from blindsend link
     * @return Upload id
     * @throws IOException
     */
    public String prepareUpload(String linkId) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/request/prepare-upload");
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
        LOGGER.info("/request/prepare-upload Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String uploadId = json.getString(this.uploadId);
            return uploadId;
        } else {
            throw new RuntimeException("/request/prepare-upload on BlindsendAPI failed");
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

        URL obj = new URL(endpoint + "/request/send-file/" + linkId + "/" + uploadId);
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
        LOGGER.info("/request/send-file Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
        } else {
            throw new RuntimeException("/request/send-file on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to submit cryptographic information related to file encryption. Called by file sender after encryption
     * and uploading of the file
     * This request is the third exchange of information by file sender with blindsend, and should be performed after uploading encrypted file
     * @param linkId Link id
     * @param pkSender Public key of a sender
     * @param streamEncryptionHeader Stream encryption header
     * @param fileName Name of the exchanged file
     * @param fileSize Size of the exchanged file in bytes
     * @throws IOException
     */
    public void finishUpload(
            String linkId,
            byte[] pkSender,
            String streamEncryptionHeader,
            String fileName,
            long fileSize
    ) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.publicKey2 + "\": \"" + BlindsendUtil.toHex(pkSender) + "\",\r\n" +
                "   \"" + this.header + "\": \"" + streamEncryptionHeader + "\",\r\n" +
                "   \"" + this.fileName + "\": \"" + fileName + "\",\r\n" +
                "   \"" + this.fileSize + "\": " + fileSize + " \n}";

        URL obj = new URL(endpoint + "/request/finish-upload");
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
        LOGGER.info("/request/finish-upload Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
        } else {
            throw new RuntimeException("/request/finish-upload on BlindsendAPI failed");
        }
    }

    /**
     * Returns the name of the file exchanged with given link Id
     * @param linkId link Id
     * @return file name
     * @throws IOException
     */
    public String getFileName(String linkId) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/request/get-file-metadata");
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
        LOGGER.info("/request/get-file-metadata Response code " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String fileName = json.getString(this.fileName);
            return fileName;
        } else {
            throw new RuntimeException("/request/get-file-metadata on BlindsendAPI failed");
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

        URL obj = new URL(endpoint + "/request/get-keys");
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
        LOGGER.info("/request/get-keys Response Code :  " + responseCode);
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
            String kdfSalt = json.getString(this.kdfSalt);
            int kdfOps = json.getInt(this.kdfOps);
            int kdfMemLimit = json.getInt(this.kdfMemLimit);
            String streamEncryptionHeader = json.getString(this.streamEncHeader);
            return new Keys(
                    BlindsendUtil.toByte(pkSenderHex),
                    BlindsendUtil.toByte(kdfSalt),
                    kdfOps,
                    kdfMemLimit,
                    streamEncryptionHeader
            );
        } else {
            throw new RuntimeException("/request/get-keys on BlindsendAPI failed");
        }
    }
    
    /**
     * Calls blindsend API to download encrypted file
     * @param linkId Link id
     * @param downloadPath Path to a file for downloaded encrypted file
     * @return Encrypted file
     * @throws IOException
     */
    public File downloadFile(String linkId, String downloadPath) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/request/get-file");
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
        LOGGER.info("/request/get-file Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            byte[] fileBytes = IOUtils.toByteArray(postConnection.getInputStream());
            FileUtils.writeByteArrayToFile(new File(downloadPath), fileBytes);
            LOGGER.info("File obtained from the API saved to " + downloadPath);
            return new File(downloadPath);
        } else {
            throw new RuntimeException("/request/get-file on BlindsendAPI failed");
        }
    }

    /**
     * Getter for api endpoint url
     * @return Blindsend API URL
     */
    public String getEndpoint(){
        return this.endpoint;
    }
}

package api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.FileMetadataRequest;
import util.FileMetadataSend;
import util.Keys;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import util.BlindsendUtil;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class {@code BlindsendAPI} provides methods for the communication with
 * <a href="https://github.com/blindnet-io/blindsend-be">blindsend REST API</a>.
 * Blindsend supports two use cases. <i>Request</i> use case, covered with {@code /request} API routes, is when file Receiver
 * initiates file exchange by requesting the file exchange link from blindsend. <i>Send</i> use case, covered with {@code /send}
 * API routes, is when file Sender initiates file exchange by uploading a file to blindsend and obtaining the download link.
 * <p>
 * Read more about blindsend and how the two use cases work at <a href="https://developer.blindnet.io/blindsend/">blindsend
 * documentation pages</a>.
 * </p>
 */
public class BlindsendAPI {

    final static Logger LOGGER = LogManager.getLogger(BlindsendAPI.class);

    private String endpoint;

    final String link = "link";
    final String linkId = "link_id";
    final String uploadId = "upload_id";
    final String kdfSalt = "kdf_salt";
    final String kdfOps = "kdf_ops";
    final String kdfMemLimit = "kdf_memory_limit";
    final String kdfMemLimitSend = "kdf_mem_limit";
    final String publicKey2 = "pk2";
    final String header = "header";
    final String fileName = "file_name";
    final String fileSize = "file_size";
    final String streamEncHeader = "stream_enc_header";
    final String pk2Resp = "public_key_2";
    final String encFileSize = "size";
    final String encFileMeta = "enc_file_meta";
    final String fileEncNonce = "file_enc_nonce";
    final String fileMetaEncNonce = "meta_enc_nonce";

    public BlindsendAPI(String endpoint) {
        this.endpoint = endpoint;
    }
  
    /**
     * Calls blindsend API to obtain link Id. Called by file Receiver in the <i>request</i> use case.
     * @return Link id
     * @throws IOException
     */
    public String requestLinkId() throws IOException {
        URL urlForGetRequest = new URL(endpoint + "/request/init-link-id");
        String readLine = null;
        HttpURLConnection connection = (HttpURLConnection) urlForGetRequest.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        LOGGER.info("/request/init-link-id Response code " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
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
     * Calls blindsend API to submit Receiver's cryptographic information and obtain file exchange link.
     * Called by file Receiver in the <i>request</i> use case.
     * <p>
     * This request is the first exchange of information by file Receiver with blindsend in the <i>request</i> use case.
     * </p>
     * @param linkId Link id obtained from blindsend API
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @return Blindsend file exchange link
     * @throws IOException
     */
    public URL initializeRequestSession(
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
            return new URL(link);
        } else {
            throw new RuntimeException("/request/init-session on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to submit Sender's cryptographic information and obtain file exchange link.
     * Called by file Sender in the <i>send</i> use case.
     * <p>
     * This request is the first exchange of information by file Sender with blindsend in the <i>send</i> use case.
     * </p>
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @param fileEncNonce File encryption nonce
     * @param fileMetaEncNonce File metadata encryption nonce
     * @param sizeEncFile Size of the encrypted file, in bytes
     * @param encFileMetadata Encrypted file metadata
     * @return Link id
     * @throws IOException
     */
    public String initializeSendSession(
            byte[] kdfSalt,
            int kdfOps,
            int kdfMemLimit,
            byte[] fileEncNonce,
            byte[] fileMetaEncNonce,
            long sizeEncFile,
            byte[] encFileMetadata
    ) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.kdfSalt + "\": \"" + BlindsendUtil.toHex(kdfSalt) + "\",\r\n" +
                "   \"" + this.kdfOps + "\": " + kdfOps + ",\r\n" +
                "   \"" + this.kdfMemLimitSend +"\": " + kdfMemLimit + ",\r\n" +
                "   \"" + this.fileEncNonce + "\": \"" + BlindsendUtil.toHex(fileEncNonce) + "\",\r\n" +
                "   \"" + this.fileMetaEncNonce + "\": \"" + BlindsendUtil.toHex(fileMetaEncNonce) + "\",\r\n" +
                "   \"" + this.encFileSize + "\": " + sizeEncFile + ",\r\n" +
                "   \"" + this.encFileMeta + "\": \"" + BlindsendUtil.toHex(encFileMetadata) + "\" \n}";

        URL obj = new URL(endpoint + "/send/init-session");
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
        LOGGER.info("/send/init-session Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String linkId = json.getString(this.linkId);
            return linkId;
        } else {
            throw new RuntimeException("/send/init-session on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to obtain upload Id. Called by file Sender in the <i>request</i> use case.
     * <p>
     * This request is the first exchange of information by file Sender with blindsend in the <i>request</i> use case.
     * </p>
     * @param linkId Link id, extracted from blindsend link
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
     * Calls blindsend API to initialize file upload, must be called before uploading the file.
     * Called by file Sender in the <i>request</i> use case.
     * <p>
     * This request is the second exchange of information by file Sender with blindsend in the <i>request</i> use case.
     * </p>
     * @param linkId Link id
     * @param uploadId Upload id
     * @param fileSize Size of the encrypted file in bytes
     * @throws IOException
     */
    public void initializeFileUpload(String linkId, String uploadId, long fileSize) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\",\r\n" +
                "   \"" + this.uploadId + "\": \"" + uploadId + "\",\r\n" +
                "   \"" + this.fileSize +"\": " + fileSize + " \n}";

        URL obj = new URL(endpoint + "/request/init-send-file");
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
        LOGGER.info("/request/init-send-file Response Code :  " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new RuntimeException("/request/init-send-file on BlindsendAPI failed");
    }

    /**
     * Calls blindsend API to upload one chunk of the encrypted file. Called by file Sender in the <i>request</i> use case.
     * Chunk uploads are only supported for files stored on Google Cloud Storage.
     * <p>
     * To upload the whole file in one API call, use <i>file size</i> in bytes for {@code chunkSize}, <i>1</i> for {@code chunkId},
     * and <i>true</i> for {@code isLast}, and the whole file for {@code chunkBytes}.
     * </p>
     * @param linkId Link id
     * @param uploadId Upload id
     * @param chunkId Id of the chunk being uploaded. Chunk ids start at 1
     * @param chunkSize Chunk size in bytes
     * @param isLast If the chunk being uploaded is the last chunk
     * @param chunkBytes File chunk to upload
     * @throws IOException
     */
    public void uploadFileChunk(String linkId, String uploadId, int chunkId, int chunkSize, boolean isLast, byte[] chunkBytes) throws IOException{
        URL obj = new URL(endpoint + "/request/send-file-part/" + linkId + "/" + uploadId +
                "?part_id=" + chunkId  + "&chunk_size=" + chunkSize  + "&last=" + isLast);
        LOGGER.info("Uploading chunk #" + chunkId + " to blindsend");
        upload(obj, chunkBytes, false, "/request");
    }

    /**
     * Calls blindsend API to upload one chunk of the encrypted file. Called by file Sender in the <i>send</i> use case.
     * Chunk uploads are only supported for files stored on Google Cloud Storage
     * <p>
     * To upload the whole file in one API call, use <i>file size</i> in bytes for {@code chunkSize}, <i>1</i> for {@code chunkId},
     * and <i>true</i> for {@code isLast}, and the whole file for {@code chunkBytes}.
     * </p>
     * @param linkId Link id
     * @param chunkId Id of the chunk being uploaded. Chunk ids start at 1
     * @param chunkSize Chunk size in bytes
     * @param isLast If the chunk being uploaded is the last chunk
     * @param chunkBytes File chunk to upload
     * @return File exchange link, to be passed to file Receiver for receiving the file
     * @throws IOException
     */
    public URL uploadFileChunk(String linkId, int chunkId, int chunkSize, boolean isLast, byte[] chunkBytes) throws IOException{
        URL obj = new URL(endpoint + "/send/send-file-part/" + linkId +
                "?part_id=" + chunkId  + "&chunk_size=" + chunkSize  + "&last=" + isLast);
        LOGGER.info("Uploading chunk #" + chunkId + " to blindsend");
        return upload(obj, chunkBytes, isLast, "/send");
    }

    private URL upload(URL route, byte[] chunkBytes, boolean getLink, String scenario) throws IOException {
        final byte[] POST_PARAMS = chunkBytes;

        HttpURLConnection postConnection = (HttpURLConnection) route.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS);
        os.flush();
        os.close();

        int responseCode = postConnection.getResponseCode();
        LOGGER.info(scenario + "/send-file-part Response Code :  " + responseCode);
        if(responseCode == HttpURLConnection.HTTP_OK && getLink) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            String link = response.toString();
            return new URL(link);
        }
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new RuntimeException("Blindsend API error on route " + route.toString());
        else return null;
    }

    /**
     * Calls blindsend API to submit cryptographic information related to file encryption. Called by file Sender
     * in the <i>request</i> use case after uploading encrypted file to blindsend.
     * <p>
     * This request is the fourth exchange of information by file Sender with blindsend in the <i>request</i> use case,
     * and must be performed after uploading the encrypted file.
     * </p>
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
     * Calls blindsend API to retrieve metadata of the file associated with the link id. Called by file Receiver
     * in the <i>request</i> use case before downloading the encrypted file from blindsend.
     * <p>
     * This is the first call by the Receiver in the <i>request</i> use case when fetching files from blindsend.
     * </p>
     * @param linkId link Id
     * @return FileMetadataRequest object, containing file metadata
     * @throws IOException
     */
    public FileMetadataRequest getFileMetadataRequest(String linkId) throws IOException {
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
            long fileSize = json.getLong(this.fileSize);
            return new FileMetadataRequest(fileName, fileSize);
        } else {
            throw new RuntimeException("/request/get-file-metadata on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to retrieve metadata of the file associated with the link id. Called by file Receiver
     * in the <i>send</i> use case before downloading the encrypted file from blindsend.
     * <p>
     * This is the first call by the Receiver in the <i>send</i> use case when fetching files from blindsend.
     * </p>
     * @param linkId link Id
     * @return FileMetadataRequest object, containing file metadata
     * @throws IOException
     */
    public FileMetadataSend getFileMetadataSend(String linkId) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        URL obj = new URL(endpoint + "/send/get-file-metadata");
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
        LOGGER.info("/send/get-file-metadata Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in .readLine()) != null) {
                response.append(inputLine);
            } in .close();
            JSONObject json = new JSONObject(response.toString());
            String kdfSalt = json.getString(this.kdfSalt);
            int kdfOps = json.getInt(this.kdfOps);
            int kdfMemLimit = json.getInt(this.kdfMemLimitSend);
            String fileEncNonce = json.getString(this.fileEncNonce);
            String fileMetaEncNonce = json.getString(this.fileMetaEncNonce);
            long sizeEncFile = json.getLong(this.encFileSize);
            String encFileMetadata = json.getString(this.encFileMeta);
            return new FileMetadataSend(
                    BlindsendUtil.toByte(kdfSalt),
                    kdfOps,
                    kdfMemLimit,
                    BlindsendUtil.toByte(fileEncNonce),
                    BlindsendUtil.toByte(fileMetaEncNonce),
                    sizeEncFile,
                    BlindsendUtil.toByte(encFileMetadata)
            );
        } else {
            throw new RuntimeException("/send/get-file-metadata on BlindsendAPI failed");
        }
    }

    /**
     * Calls blindsend API to obtain cryptographic information necessary for decryption of the file. Called by file Receiver
     * in the <i>request</i> use case.
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
     * Calls blindsend API to download the encrypted file. Called by file Receiver in the <i>request</i> use case.
     * @param linkId Link id
     * @param downloadPath Path to a file where to save the downloaded encrypted file
     * @return Encrypted file
     * @throws IOException
     */
    public File downloadFileRequest(String linkId, String downloadPath) throws IOException {
        URL obj = new URL(endpoint + "/request/get-file");
        return downloadFile(obj, linkId, downloadPath, "/request");
    }

    /**
     * Calls blindsend API to download the encrypted file. Called by file Receiver in the <i>send</i> use case.
     * @param linkId Link id
     * @param downloadPath Path to a file where to save the downloaded encrypted file
     * @return Encrypted file
     * @throws IOException
     */
    public File downloadFileSend(String linkId, String downloadPath) throws IOException {
        URL obj = new URL(endpoint + "/send/get-file");
        return downloadFile(obj, linkId, downloadPath, "/send");
    }

    private File downloadFile(URL route, String linkId, String downloadPath, String scenario) throws IOException {
        final String POST_PARAMS = "{\n" +
                "   \"" + this.linkId + "\": \"" + linkId + "\" \n}";

        HttpURLConnection postConnection = (HttpURLConnection) route.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoOutput(true);

        postConnection.setDoOutput(true);
        OutputStream os = postConnection.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();


        int responseCode = postConnection.getResponseCode();
        LOGGER.info(scenario + "/get-file Response Code :  " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            byte[] fileBytes = IOUtils.toByteArray(postConnection.getInputStream());
            FileUtils.writeByteArrayToFile(new File(downloadPath), fileBytes);
            LOGGER.info("File obtained from the API saved to " + downloadPath);
            return new File(downloadPath);
        } else {
            throw new RuntimeException(scenario + "/get-file on BlindsendAPI failed");
        }
    }

    /**
     * Getter for the api endpoint url
     * @return Blindsend API URL
     */
    public String getEndpoint(){
        return this.endpoint;
    }
}

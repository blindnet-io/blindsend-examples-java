package util;

/**
 * Wrapper for <i>/request/get-file-metadata</i> http response from blindsend API call.
 * It provides a constructor to wrap the response in an object and obtain information from the response via getters.
 */
public class FileMetadataRequest {

    private String fileName;
    private long fileSize;

    /**
     * Creates file metadata
     * @param fileName File name
     * @param fileSize File size in bytes
     */
    public FileMetadataRequest(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    /**
     *
     * @return File name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     *
     * @return File size in bytes
     */
    public long getFileSize() {
        return fileSize;
    }
}

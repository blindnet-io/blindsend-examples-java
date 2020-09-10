package util;

/**
 * Wrapper for /cont-hs http response from blindsend API call.
 * It provides a constructor to wrap the response in an object and obtain information from the responce via getters
 */
public class ContHandshakeResp {

    private byte[] pkRequestor;
    private String uploadId;

    /**
     * Creates new ContHsResp
     * @param pkRequestor Public key of file requestor (receiver)
     * @param uploadId File exchange upload id
     */
    public ContHandshakeResp(byte[] pkRequestor, String uploadId) {
        this.pkRequestor = pkRequestor;
        this.uploadId = uploadId;
    }

    /**
     *
     * @return Public key of file requestor (receiver)
     */
    public byte[] getPkRequestor() {
        return pkRequestor;
    }

    /**
     *
     * @return File exchange upload id
     */
    public String getUploadId() {
        return uploadId;
    }
}

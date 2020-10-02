package util;

/**
 * Wrapper for <i>/request/get-keys</i> http response from blindsend API call.
 * It provides a constructor to wrap the response in an object and obtain information from the response via getters.
 */
public class Keys {

    private byte[] pkSender;
    private byte[] kdfSalt;
    private int kdfOps;
    private int kdfMemLimit;
    private String streamEncryptionHeader;

    /**
     * Creates new Key
     * @param pkSender Public key of file sender
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @param streamEncryptionHeader Stream encryption header
     */
    public Keys(
            byte[] pkSender,
            byte[] kdfSalt,
            int kdfOps,
            int kdfMemLimit,
            String streamEncryptionHeader
    ){
        this.pkSender = pkSender;
        this.kdfSalt = kdfSalt;
        this.kdfOps = kdfOps;
        this.kdfMemLimit = kdfMemLimit;
        this.streamEncryptionHeader = streamEncryptionHeader;
    }

    /**
     *
     * @return Public key of file sender
     */
    public byte[] getPkSender() {
        return pkSender;
    }

    /**
     *
     * @return Hashing salt
     */
    public byte[] getKdfSalt() {
        return kdfSalt;
    }

    /**
     *
     * @return Hashing cycles
     */
    public int getKdfOps() {
        return kdfOps;
    }

    /**
     *
     * @return Hashing RAM limit
     */
    public int getKdfMemLimit() { return kdfMemLimit; }

    /**
     *
     * @return Stream encryption header
     */
    public String getStreamEncryptionHeader() {
        return streamEncryptionHeader;
    }
}

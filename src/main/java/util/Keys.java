package util;

/**
 * Wrapper for /get-keys http response from blindsend API call
 * It provides a constructor to wrap the response in an object and obtain information from the responce via getters
 */
public class Keys {

    private byte[] pkSender;
    private byte[] pkRequestor;
    private byte[] skEncryptionIv;
    private byte[] encryptedSK;
    private byte[] kdfSalt;
    private int kdfOps;
    private int kdfMemLimit;
    private String streamEncryptionHeader; // not needed for AES-GCM encryption used in the example

    /**
     * Creates new Key
     * @param pkSender Public key of file sender
     * @param pkRequestor Public key of file requestor (receiver)
     * @param skEncryptionIv Nonce for the encryption of secret key
     * @param encryptedSK Encrypted secret key
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @param streamEncryptionHeader Stream encryption header
     */
    public Keys(
            byte[] pkSender,
            byte[] pkRequestor,
            byte[] skEncryptionIv,
            byte[] encryptedSK,
            byte[] kdfSalt,
            int kdfOps,
            int kdfMemLimit,
            String streamEncryptionHeader
    ){
        this.pkSender = pkSender;
        this.pkRequestor = pkRequestor;
        this.skEncryptionIv = skEncryptionIv;
        this.encryptedSK = encryptedSK;
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
     * @return Nonce for the encryption of secret key
     */
    public byte[] getSkEncryptionIv() {
        return skEncryptionIv;
    }

    /**
     *
     * @return Encrypted secret key
     */
    public byte[] getEncryptedSK() {
        return encryptedSK;
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
    public int getKdfMemLimit() {
        return kdfMemLimit;
    }
}

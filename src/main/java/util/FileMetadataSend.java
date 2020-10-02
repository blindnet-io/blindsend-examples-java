package util;

/**
 * Wrapper for <i>/send/get-file-metadata</i> http response from blindsend API call.
 * It provides a constructor to wrap the response in an object and obtain information from the response via getters.
 */
public class FileMetadataSend {

    private byte[] kdfSalt;
    private int kdfOps;
    private int kdfMemLimit;
    private byte[] fileEncNonce;
    private byte[] fileMetaEncNonce;
    private long sizeEncFile;
    private byte[] encFileMetadata;

    /**
     * Creates file metadata
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMemLimit Hashing RAM limit
     * @param fileEncNonce File encryption nonce
     * @param fileMetaEncNonce File metadata encryption nonce
     * @param sizeEncFile Size of the encrypted file in bytes
     * @param encFileMetadata Encrypted file metadata
     */
    public FileMetadataSend(byte[] kdfSalt,
                            int kdfOps,
                            int kdfMemLimit,
                            byte[] fileEncNonce,
                            byte[] fileMetaEncNonce,
                            long sizeEncFile,
                            byte[] encFileMetadata) {
        this.kdfSalt = kdfSalt;
        this.kdfOps = kdfOps;
        this.kdfMemLimit = kdfMemLimit;
        this.fileEncNonce = fileEncNonce;
        this.fileMetaEncNonce = fileMetaEncNonce;
        this.sizeEncFile = sizeEncFile;
        this.encFileMetadata = encFileMetadata;
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

    /**
     *
     * @return File encryption nonce
     */
    public byte[] getFileEncNonce() {
        return fileEncNonce;
    }

    /**
     *
     * @return File metadata encryption nonce
     */
    public byte[] getFileMetaEncNonce() {
        return fileMetaEncNonce;
    }

    /**
     *
     * @return Size of the encrypted file in bytes
     */
    public long getSizeEncFile() {
        return sizeEncFile;
    }

    /**
     *
     * @return Encrypted file metadata
     */
    public byte[] getEncFileMetadata() {
        return encFileMetadata;
    }
}

package crypto;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.encoders.Hex;
import util.BlindsendUtil;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * The CryptoFactory class provides methods for generating cryptographic primitives required by blindsend.
 * It also provides methods for encryption and decryption of files
 */
public class CryptoFactory {

    /**
     * Generates PK-SK (X25519)
     * @return Key pair
     * @throws GeneralSecurityException
     */
    public static KeyPair generateKeyPair()
            throws GeneralSecurityException {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance("X25519", "BC");
        keyPair.initialize(256);
        return keyPair.generateKeyPair();
    }

    /**
     * Generates a random bytes of length len
     * @param len length of random value to generate
     * @return salt
     */
    public static byte[] generateRandom(int len) throws NoSuchProviderException, NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("NonceAndIV", "BC");
        byte[] kBytes = new byte[len];
        random.nextBytes(kBytes);
        return kBytes;
    }

    /**
     * Generates a key to be used for the encryption of a secret key. Uses Argon2id hashing algorithm
     * @param password Password
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMin Hashing RAM limit
     * @return A key for the encryption of a secret key
     */
    public static byte[] generateSkEncryptionKey(String password, byte[] kdfSalt, int kdfOps, int kdfMin) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).
                withSalt(kdfSalt).
                withParallelism(kdfOps).
                withMemoryAsKB(kdfMin).
                withIterations(3);
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());
        byte[] result = new byte[32];
        gen.generateBytes(password.toCharArray(), result, 0, result.length);
        return result;
    }

    /**
     * Hashes a key used for the encryption of a secret key. Uses BLAKE2b
     * @param skEncryptionKey A key to hash
     * @return Hashed key
     */
    public static byte[] generateSkEncryptionKeyHash(byte[] skEncryptionKey){
        Blake2bDigest messageDigest = new Blake2bDigest(256);
        messageDigest.update(skEncryptionKey, 0, skEncryptionKey.length);
        byte[] out = new byte[messageDigest.getDigestSize()];
        messageDigest.doFinal(out, 0);
        return Hex.encode(out);
    }

    /**
     * Encrypts a secret key
     * @param sk Secret key to encrypt
     * @param skEncryptionKey Encryption key
     * @param skEncryptionIv Encryption nonce
     * @return Encrypted secret key
     */
    public static byte[] encryptSK(byte[] sk, byte[] skEncryptionKey, byte[] skEncryptionIv) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey key = new SecretKeySpec(skEncryptionKey, 0, skEncryptionKey.length, "AES");
        return encryptAesGcm(sk, key, skEncryptionIv);
    }

    /**
     * Decrypts a secret key
     * @param encSK Encrypted secret key
     * @param skEncryptionKey Encryption key
     * @param skEncryptionIv Encryption nonce
     * @return Decrypted secret key
     */
    public static byte[] decryptSK(byte[] encSK, byte[] skEncryptionKey, byte[] skEncryptionIv) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey key = new SecretKeySpec(skEncryptionKey, 0, skEncryptionKey.length, "AES");
        return decryptAesGcm(encSK, key, skEncryptionIv);
    }

    /**
     * Generates a master key (file encryption/decryption key)
     * @param sk Secret key
     * @param pk Public key
     * @return Master key to be used for encryption/decryption
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static byte[] generateMasterKey(PrivateKey sk, PublicKey pk) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement agreement = KeyAgreement.getInstance("XDH", "BC");
        agreement.init(sk);
        agreement.doPhase(pk, true);
        return agreement.generateSecret("AES").getEncoded();
    }

    /**
     * Encrypts a file and saves it to disk
     * @param masterKey Master key for file encryption
     * @param inputFile File to encrypt
     * @param encryptedFilePath Path to save encrypted file
     * @throws IOException
     */
    public static void encryptAndSaveFile(byte[] masterKey, File inputFile, String encryptedFilePath) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] fileAsBytes = FileUtils.readFileToByteArray(inputFile);
        byte[] iv = generateRandom(16);
        SecretKey key = new SecretKeySpec(masterKey, 0, masterKey.length, "AES");
        byte[] encryptedFileBytes = encryptAesGcm(fileAsBytes, key, iv);
        FileUtils.writeByteArrayToFile(new File(encryptedFilePath), BlindsendUtil.concatenate(iv, encryptedFileBytes));
    }

    /**
     * Decrypts a file and saves it to disk
     * @param masterKey Master key for file decryption
     * @param encryptedFile Encrypted file
     * @param decryptedFilePath Path to save decrypted file
     * @throws IOException
     */
    public static void decryptAndSaveFile(byte[] masterKey, File encryptedFile, String decryptedFilePath) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] fileBytes = FileUtils.readFileToByteArray(encryptedFile);
        InputStream encStream = new FileInputStream(encryptedFile);
        byte[] iv = new byte[16];
        encStream.read(iv, 0, 16);
        byte[] encryptedFileAsBytes = new byte[fileBytes.length - 16];
        encStream.read(encryptedFileAsBytes, 0, fileBytes.length - 16);

        SecretKey key = new SecretKeySpec(masterKey, 0, masterKey.length, "AES");
        byte[] decryptedFileBytes = decryptAesGcm(encryptedFileAsBytes, key, iv);
        FileUtils.writeByteArrayToFile(new File(decryptedFilePath), BlindsendUtil.concatenate(iv, decryptedFileBytes));
    }

    protected static byte[] encryptAesGcm(byte[] msg, SecretKey key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(msg);
    }

    protected static byte[] decryptAesGcm(byte[] ct, SecretKey key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ct);
    }
}

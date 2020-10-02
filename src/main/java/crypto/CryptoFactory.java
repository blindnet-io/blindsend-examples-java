package crypto;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.prng.FixedSecureRandom;
import org.bouncycastle.util.encoders.Hex;
import util.BlindsendUtil;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * Class {@code CryptoFactory} provides methods for generating cryptographic primitives required by blindsend,
 * and for encryption and decryption of files.
 */
public class CryptoFactory {

    /**
     * Generates PK-SK (X25519)
     * @return Key pair
     * @throws GeneralSecurityException
     */
    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance("X25519", "BC");
        keyPair.initialize(256);
        return keyPair.generateKeyPair();
    }

    /**
     * Generates PK-SK (X25519)
     * @param keyPairSeed Seed for key pair generation
     * @return Key pair
     * @throws GeneralSecurityException
     */
    public static KeyPair generateKeyPair(byte[] keyPairSeed) throws GeneralSecurityException {
        SecureRandom random = new FixedSecureRandom(keyPairSeed);
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance("X25519", "BC");
        keyPair.initialize(256, random);
        return keyPair.generateKeyPair();
    }

    /**
     * Generates random bytes of length <i>len</i>
     * @param len length of the random bytes to generate
     * @return Random bytes
     */
    public static byte[] generateRandom(int len) throws NoSuchProviderException, NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("NonceAndIV", "BC");
        byte[] kBytes = new byte[len];
        random.nextBytes(kBytes);
        return kBytes;
    }

    /**
     * Generates a password-based seed by using <i>Argon2id</i> hashing algorithm.
     * It is used to generate a seed for the key pair generation in the <i>/request</i> use case
     * and file key generation in the <i>/send</i> use case.
     * @param password Password
     * @param kdfSalt Hashing salt
     * @param kdfOps Hashing cycles
     * @param kdfMin Hashing RAM limit
     * @return Seed
     */
    public static byte[] generateSeed(String password, byte[] kdfSalt, int kdfOps, int kdfMin) {
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
     * Derives a key using <i>BLAKE2s</i> algorithm
     * @param seed Seed to derive the key from
     * @param context Context, 8 bytes long
     * @return Derived key
     */
    public static byte[] kdf(byte[] seed, byte[] context){
        Blake2sDigest messageDigest = new Blake2sDigest(seed, 16, "10000000".getBytes(), context);
        byte[] out = new byte[messageDigest.getDigestSize()];
        messageDigest.doFinal(out, 0);
        return Hex.encode(out);
    }

    /**
     * Hashing using <i>BLAKE2b</i> algorithm
     * @param value Value to hash
     * @return Hash
     */
    public static byte[] hash(byte[] value){
        Blake2bDigest messageDigest = new Blake2bDigest(128);
        messageDigest.update(value, 0, value.length);
        byte[] out = new byte[messageDigest.getDigestSize()];
        messageDigest.doFinal(out, 0);
        return Hex.encode(out);
    }

    /**
     * Encrypts file metadata using the <i>AES-GCM</i> algorithm
     * @param fileMetadata File metadata
     * @param key Encryption key
     * @param iv IV
     * @return
     */
    public static byte[] encryptFileMetadata(byte[] fileMetadata, byte[] key, byte[] iv) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey sk = new SecretKeySpec(key, 0, key.length, "AES");
        return encryptAesGcm(fileMetadata, sk, iv);
    }

    /**
     * Decrypts file metadata using the <i>AES-GCM</i> algorithm
     * @param encFileMetadata Encrypted file metadata
     * @param key Decryption key
     * @param iv IV
     * @return
     */
    public static byte[] decryptFileMetadata(byte[] encFileMetadata, byte[] key, byte[] iv) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey sk = new SecretKeySpec(key, 0, key.length, "AES");
        return decryptAesGcm(encFileMetadata, sk, iv);
    }

    /**
     * Generates a master key used for file encryption/decryption
     * @param sk Secret key
     * @param pk Public key
     * @return Master key to be used for file encryption/decryption
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
     * Encrypts a file using the <i>AES-GCM</i> algorithm, and saves it locally
     * @param masterKey Master key for file encryption
     * @param inputFile File to encrypt
     * @param encryptedFilePath Path to save the encrypted file
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
     * Decrypts a file using the <i>AES-GCM</i> algorithm, and saves it locally
     * @param masterKey Master key for file decryption
     * @param encryptedFile Encrypted file
     * @param decryptedFilePath Path to save the decrypted file
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

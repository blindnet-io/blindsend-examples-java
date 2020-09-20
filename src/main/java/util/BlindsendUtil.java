package util;

/**
 * The BlindsendUtil class provides helper methods for for working with different representation of data.
 * It also provides helper methods useful for manipulating blindsend links and link ids
 */
public class BlindsendUtil {

    /**
     * Converts byte array to hex string
     * @param byteArray Array to convert
     * @return Hex representation
     */
    public static String toHex(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    /**
     * Converts hex string to byte array
     * @param hexString Hex string to convert
     * @return Byte array
     */
    public static byte[] toByte(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    protected static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    protected static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    protected static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    /**
     * Concatenates two byte arrays
     * @param b1 first byte array
     * @param b2 second byte array
     * @return concatenation
     */
    public static byte[] concatenate(byte[] b1, byte[] b2){
        byte[] c = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, c, 0, b1.length);
        System.arraycopy(b2, 0, c, b1.length, b2.length);
        return c;
    }

    /**
     * Extracts link id from blindsend link
     * @param link Link
     * @return Link id
     */
    public static String extractLinkId(String link) {
        if (link.contains("#")) {
            int first = link.lastIndexOf('/') + 1;
            int last = link.lastIndexOf('#');
            return link.substring(first, last);
        } else {
            int pos = link.lastIndexOf('/') + 1;
            return link.substring(pos);
        }
    }

    /**
     * Extracts public key from blindsend link
     * @param link Link
     * @return Public key
     */
    public static String extractKey(String link) {
        int pos = link.lastIndexOf('#') + 1;
        return link.substring(pos);
    }
}

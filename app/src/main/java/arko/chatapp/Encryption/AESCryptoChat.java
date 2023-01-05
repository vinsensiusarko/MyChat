package arko.chatapp.Encryption;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.google.android.gms.common.util.Base64Utils.decode;
import static com.google.android.gms.common.util.Base64Utils.encode;

public class AESCryptoChat {

    public static final String ALGORITHM = "AES";
    public byte[] keyValue;

    public AESCryptoChat(String key) {
        keyValue = key.getBytes();
    }

    public String encrypt(String plainText) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(plainText.getBytes());
        String encryptedValue = encode(encVal);
        return encryptedValue;
    }

    public String decrypt(String cipherText) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = decode(cipherText);
        byte[] decValue = c.doFinal(decodedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    private Key generateKey() throws Exception{
        Key key = new SecretKeySpec(keyValue, ALGORITHM);
        return key;
    }

//    public static void main(String[] args) {
//        try {
//
//            AESCrypt aes = new AESCrypt("lv39eptlvuhaqqsr");
//
//            String encryptedText = aes.encrypt("Meet is a good Boy");
//            System.out.println("Encrypted Text - " + encryptedText);
//
//            String decryptedText = aes.decrypt(encryptedText);
//            System.out.println("Decrypted Text - " + decryptedText);
//
//        } catch (Exception e) {
//
//            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, e);
//        }
//    }
}

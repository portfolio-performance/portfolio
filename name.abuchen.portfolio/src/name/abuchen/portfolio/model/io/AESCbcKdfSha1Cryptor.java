package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.text.MessageFormat;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import name.abuchen.portfolio.Messages;

public class AESCbcKdfSha1Cryptor extends BlockCipherCryptor
{

    private static final byte[] SIGNATURE = new byte[] { 'P', 'O', 'R', 'T', 'F', 'O', 'L', 'I', 'O' };
    
    private static final byte[] SALT = new byte[] { 112, 67, 103, 107, -92, -125, -112, -95, //
        -97, -114, 117, -56, -53, -69, -25, -28 };
    private static final int ITERATION_COUNT = 65536;
    
    private static final int IV_LENGTH = 16;
    
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"; //$NON-NLS-1$
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1"; //$NON-NLS-1$
    
    private static final int AES128_KEYLENGTH = 128;
    private static final int AES256_KEYLENGTH = 256;
    
    public AESCbcKdfSha1Cryptor(char[] password)
    {
        super(password);
    }
    
    @Override
    public byte[] getSignature()
    {
        return SIGNATURE;
    }

    @Override
    int resolveMethodToKeylength(int method)
    {
        return method == 1 ? AES256_KEYLENGTH : AES128_KEYLENGTH;
    }

    @Override
    int resolveKeyLengthToMethod(int keyLength)
    {
        return keyLength == AES256_KEYLENGTH ? 1 : 0;
    }

    @Override
    public boolean isKeyLengthSupported(int keyLength)
    {
        try
        {
            return keyLength <= Cipher.getMaxAllowedKeyLength(getCipherAlgorithm());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorEncrypting, e.getMessage()), e);
        }
    }

    @Override
    SecretKey buildSecretKey(int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(getPassword(), SALT, ITERATION_COUNT, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    @Override
    int getInitializationVectorLength()
    {
        return IV_LENGTH;
    }

    @Override
    String getCipherAlgorithm()
    {
        return CIPHER_ALGORITHM;
    }

    @Override
    Cipher initCipherFromStream(InputStream input, SecretKey secret) throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
    {
        // read initialization vector
        byte[] iv = new byte[getInitializationVectorLength()];
        input.read(iv);
      
        // build cipher
        Cipher cipher = Cipher.getInstance(getCipherAlgorithm());
        // init cipher and stream
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
        
        return cipher;
    }

    @Override
    void writeCipherParametersToStream(Cipher cipher, OutputStream output) throws InvalidParameterSpecException, IOException
    {
        AlgorithmParameters params = cipher.getParameters();
        
        // write initialization vector
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        output.write(iv);
    }

}

package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.text.MessageFormat;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import name.abuchen.portfolio.Messages;

public class AESGcmKdfSha256Cryptor extends BlockCipherCryptor
{

    private static final byte[] SIGNATURE = new byte[] { 'X', 'P', 'O', 'R', 'T', 'F', 'O', 'L', 'I', 'O', '0', '0', '2' };
    
    private static final byte[] SALT = new byte[] { 13, 81, 55, 107, -92, -125, -112, -95, //
        77, -114, -67, 76, -3, 89, 115, -8 };
    private static final int ITERATION_COUNT = 65536;
    
    private static final int IV_LENGTH = 12;
    
    public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding"; //$NON-NLS-1&
    
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"; //$NON-NLS-1$
    
    private static final int AES128_KEYLENGTH = 128;
    private static final int AES256_KEYLENGTH = 256;
    
    public AESGcmKdfSha256Cryptor(char[] password)
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
    Cipher initCipherFromStream(InputStream input, SecretKey secret) throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, NoSuchProviderException
    {
        // read initialization vector
        byte[] iv = new byte[getInitializationVectorLength()];
        input.read(iv);
        // read tlen
        byte[] bytes = new byte[4];
        input.read(bytes);
        int tlen = ByteBuffer.wrap(bytes).getInt();
        // read AAD tag
        byte[] tag = new byte[tlen];
        input.read(tag);
   
        // build cipher
        Cipher cipher = Cipher.getInstance(getCipherAlgorithm());
        // init cipher and stream
        cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(tlen, iv));
        
        // update AAD
        cipher.updateAAD(tag);
        
        return cipher;
    }

    @Override
    void writeCipherParametersToStream(Cipher cipher, OutputStream output) throws InvalidParameterSpecException, IOException
    {
        AlgorithmParameters params = cipher.getParameters();
        
        // write initialization vector
        byte[] iv = params.getParameterSpec(GCMParameterSpec.class).getIV();
        output.write(iv);
        // write authentication tag length
        int tlen = params.getParameterSpec(GCMParameterSpec.class).getTLen();
        output.write(ByteBuffer.allocate(4).putInt(tlen).array());
        // init authentication data
        byte[] tag = new byte[tlen];
        try
        {
            SecureRandom.getInstanceStrong().nextBytes(tag);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Unexpected : " + e.getMessage(), e);
        }
        output.write(tag);
        // update AAD
        cipher.updateAAD(tag);
    }

}

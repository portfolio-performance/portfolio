package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESGcmKdfSha256Cryptor extends BlockCipherCryptor
{
    public static final String METHOD_AES128GCM = "AES128/GCM"; //$NON-NLS-1$
    public static final String METHOD_AES256GCM = "AES256/GCM"; //$NON-NLS-1$

    private static final byte[] SIGNATURE = new byte[] { 'X', 'P', 'O', 'R', 'T', 'F', 'O', 'L', 'I', 'O', '0', '0', '2' };
    
    private static final byte[] SALT = new byte[] { 13, 81, 55, 107, -92, -125, -112, -95, //
        77, -114, -67, 76, -3, 89, 115, -8 };
    private static final int ITERATION_COUNT = 65536;
    
    private static final int IV_LENGTH = 12;
    
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding"; //$NON-NLS-1$
    
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"; //$NON-NLS-1$
    
    private static final int AES128_KEYLENGTH = 128;
    private static final int AES256_KEYLENGTH = 256;
    
    public AESGcmKdfSha256Cryptor(String encryptionMethod, char[] password)
    {
        super(encryptionMethod, password);

        if (!METHOD_AES128GCM.equals(encryptionMethod) && !METHOD_AES256GCM.equalsIgnoreCase(encryptionMethod))
            throw new IllegalArgumentException();
    }

    public AESGcmKdfSha256Cryptor(char[] password)
    {
        this(METHOD_AES128GCM, password);
    }

    @Override
    public byte[] getSignature()
    {
        return SIGNATURE;
    }

    @Override
    int getKeyLength()
    {
        return METHOD_AES128GCM.equals(getEncryptionMethod()) ? AES128_KEYLENGTH : AES256_KEYLENGTH;
    }

    @Override
    void setEncryptionMethodFromKeyLengthFlag(int flag)
    {
        this.setEncryptionMethod(flag == 1 ? METHOD_AES256GCM : METHOD_AES128GCM);
    }

    @Override
    int getKeyLengthFlag()
    {
        return METHOD_AES128GCM.equals(getEncryptionMethod()) ? 0 : 1;
    }

    @Override
    SecretKey buildSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(getPassword(), SALT, ITERATION_COUNT, getKeyLength());
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES"); //$NON-NLS-1$
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
    Cipher initCipherFromStream(InputStream input, SecretKey secret) throws IOException, GeneralSecurityException
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
    void writeCipherParametersToStream(Cipher cipher, OutputStream output) throws IOException, GeneralSecurityException
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
        SecureRandom.getInstanceStrong().nextBytes(tag);
        output.write(tag);
        // update AAD
        cipher.updateAAD(tag);
    }

}

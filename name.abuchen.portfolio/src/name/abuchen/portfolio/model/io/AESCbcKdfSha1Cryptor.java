package name.abuchen.portfolio.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCbcKdfSha1Cryptor extends BlockCipherCryptor
{
    public static final String METHOD_AES128CBC = "AES128/CBC"; //$NON-NLS-1$
    public static final String METHOD_AES256CBC = "AES256/CBC"; //$NON-NLS-1$

    private static final byte[] SIGNATURE = new byte[] { 'P', 'O', 'R', 'T', 'F', 'O', 'L', 'I', 'O' };

    private static final byte[] SALT = new byte[] { 112, 67, 103, 107, -92, -125, -112, -95, //
                    -97, -114, 117, -56, -53, -69, -25, -28 };
    private static final int ITERATION_COUNT = 65536;

    private static final int IV_LENGTH = 16;

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"; //$NON-NLS-1$
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1"; //$NON-NLS-1$

    private static final int AES128_KEYLENGTH = 128;
    private static final int AES256_KEYLENGTH = 256;

    public AESCbcKdfSha1Cryptor(String encryptionMethod, char[] password)
    {
        super(encryptionMethod, password);

        if (!METHOD_AES128CBC.equals(encryptionMethod) && !METHOD_AES256CBC.equalsIgnoreCase(encryptionMethod))
            throw new IllegalArgumentException();
    }

    public AESCbcKdfSha1Cryptor(char[] password)
    {
        this(METHOD_AES128CBC, password);
    }

    @Override
    public byte[] getSignature()
    {
        return SIGNATURE;
    }

    @Override
    int getKeyLength()
    {
        return METHOD_AES128CBC.equals(getEncryptionMethod()) ? AES128_KEYLENGTH : AES256_KEYLENGTH;
    }

    @Override
    void setEncryptionMethodFromKeyLengthFlag(int flag)
    {
        this.setEncryptionMethod(flag == 1 ? METHOD_AES256CBC : METHOD_AES128CBC);
    }

    @Override
    int getKeyLengthFlag()
    {
        return METHOD_AES128CBC.equals(getEncryptionMethod()) ? 0 : 1;
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

        // build cipher
        Cipher cipher = Cipher.getInstance(getCipherAlgorithm());
        // init cipher and stream
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        return cipher;
    }

    @Override
    void writeCipherParametersToStream(Cipher cipher, OutputStream output) throws IOException, GeneralSecurityException
    {
        AlgorithmParameters params = cipher.getParameters();

        // write initialization vector
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        output.write(iv);
    }

}

package name.abuchen.portfolio.model.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory.XmlSerialization;

public abstract class BlockCipherCryptor implements ClientPersister
{
    abstract String getCipherAlgorithm();

    abstract byte[] getSignature();

    abstract int getKeyLength();

    abstract void setEncryptionMethodFromKeyLengthFlag(int flag);

    abstract int getKeyLengthFlag();

    abstract SecretKey buildSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException;

    abstract int getInitializationVectorLength();

    abstract Cipher initCipherFromStream(InputStream input, SecretKey secret) throws IOException,
                    GeneralSecurityException;

    abstract void writeCipherParametersToStream(Cipher cipher, OutputStream output) throws IOException,
                    GeneralSecurityException;

    public static BlockCipherCryptor buildCryptorFromFileSignature(File file, char[] password) throws IOException
    {
        List<Class<? extends BlockCipherCryptor>> cryptors = new ArrayList<Class<? extends BlockCipherCryptor>>();
        cryptors.add(AESCbcKdfSha1Cryptor.class);
        cryptors.add(AESGcmKdfSha256Cryptor.class);

        for (Class<? extends BlockCipherCryptor> cryptorClasss : cryptors)
        {
            BlockCipherCryptor cryptor;
            try
            {
                cryptor = cryptorClasss.getConstructor(char[].class).newInstance(password);
            }
            catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e)
            {
                throw new RuntimeException(e);
            }

            try (FileInputStream fis = new FileInputStream(file))
            {
                final byte[] signatureBuffer = new byte[cryptor.getSignature().length];
                fis.read(signatureBuffer);
                fis.close();
                if (Arrays.equals(signatureBuffer, cryptor.getSignature())) { return cryptor; }
            }
        }

        throw new IllegalArgumentException(Messages.MsgMissingCryptorImplementation);
    }

    private String encryptionMethod;
    private final char[] password;

    public BlockCipherCryptor(String encryptionMethod, char[] password)
    {
        this.encryptionMethod = encryptionMethod;
        this.password = password;
    }

    /* package */String getEncryptionMethod()
    {
        return encryptionMethod;
    }

    /* package */void setEncryptionMethod(String encryptionMethod)
    {
        this.encryptionMethod = encryptionMethod;
    }

    /* package */char[] getPassword()
    {
        return password;
    }

    public final boolean isKeyLengthSupported()
    {
        try
        {
            return getKeyLength() <= Cipher.getMaxAllowedKeyLength(getCipherAlgorithm());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorEncrypting, e.getMessage()), e);
        }
    }

    @Override
    public final Client load(final InputStream input) throws IOException
    {
        InputStream decrypted = null;

        try
        {
            // check signature
            byte[] signature = new byte[getSignature().length];
            input.read(signature);
            if (!Arrays.equals(signature, getSignature()))
                throw new IOException(Messages.MsgNotAPortflioFile);

            // read key length flag (0 = 128, 1 = 256) and set method
            int flag = input.read();
            setEncryptionMethodFromKeyLengthFlag(flag);

            // check if key length is supported
            if (!isKeyLengthSupported())
                throw new IOException(Messages.MsgKeyLengthNotSupported);

            // build secret key
            SecretKey secret = buildSecretKey();

            // init cipher from parameters stored in stream
            Cipher cipher = initCipherFromStream(input, secret);

            // build stream
            decrypted = new CipherInputStream(input, cipher);

            // read version information
            byte[] bytes = new byte[4];
            decrypted.read(bytes); // major version number
            int majorVersion = ByteBuffer.wrap(bytes).getInt();
            decrypted.read(bytes); // version number
            int version = ByteBuffer.wrap(bytes).getInt();

            if (majorVersion > Client.MAJOR_VERSION || version > Client.CURRENT_VERSION)
                throw new IOException(MessageFormat.format(Messages.MsgUnsupportedVersionClientFiled, version));

            // wrap with zip input stream
            ZipInputStream zipin = new ZipInputStream(decrypted);
            zipin.getNextEntry();

            Client client = new XmlSerialization().load(new InputStreamReader(zipin, StandardCharsets.UTF_8));

            // save encryption method & secret key for next save
            client.setEncryptionMethod(getEncryptionMethod());
            client.setSecret(secret);

            return client;
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorDecrypting, e.getMessage()), e);
        }
        finally
        {
            try
            {
                if (decrypted != null)
                    decrypted.close();
            }
            catch (IOException ignore)
            {
                // starting with a later jdk 1.8.0 (for example 1.8.0_25), a
                // javax.crypto.BadPaddingException
                // "Given final block not properly padded" is thrown if the
                // we do not read the complete stream
            }
        }
    }

    @Override
    public void save(Client client, final OutputStream output) throws IOException
    {
        try
        {
            // check if key length is supported
            if (!isKeyLengthSupported())
                throw new IOException(Messages.MsgKeyLengthNotSupported);

            // get or build secret key
            // if password is given, it is used (when the user chooses
            // "save as" from the menu)
            SecretKey secret = getPassword() != null ? buildSecretKey() : client.getSecret();
            if (secret == null)
                throw new IOException(Messages.MsgPasswordMissing);

            // save secret key for next save
            client.setSecret(secret);

            // write signature
            output.write(getSignature());

            // write method
            output.write(getKeyLengthFlag());

            // build cipher and stream
            Cipher cipher = Cipher.getInstance(getCipherAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            writeCipherParametersToStream(cipher, output);

            // encrypted stream
            OutputStream encrpyted = new CipherOutputStream(output, cipher);

            // write version information
            encrpyted.write(ByteBuffer.allocate(4).putInt(Client.MAJOR_VERSION).array());
            encrpyted.write(ByteBuffer.allocate(4).putInt(client.getVersion()).array());

            // wrap with zip output stream
            ZipOutputStream zipout = new ZipOutputStream(encrpyted);
            zipout.putNextEntry(new ZipEntry("data.xml")); //$NON-NLS-1$

            new XmlSerialization().save(client, zipout);

            zipout.closeEntry();
            zipout.flush();
            zipout.finish();

            // needed for AAD style ciphers
            encrpyted.flush();
            encrpyted.close();

            output.flush();
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorEncrypting, e.getMessage()), e);
        }
    }

}

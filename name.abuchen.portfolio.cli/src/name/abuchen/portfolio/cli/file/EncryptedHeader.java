package name.abuchen.portfolio.cli.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import name.abuchen.portfolio.cli.CoreVersion;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * Decrypts just the header of an encrypted client file: the content type and
 * the file version are stored inside the encrypted stream. Core only reports
 * a wrong password or a newer version as a localized message, so the CLI reads
 * both itself to report stable error codes. The layout and key derivation
 * mirror core's private implementation.
 */
record EncryptedHeader(FileFormat format, int version)
{
    private static final int SIGNATURE_LENGTH = 9; // "PORTFOLIO"
    private static final int IV_LENGTH = 16;
    private static final int BLOCK_LENGTH = 16;

    private static final byte[] SALT = new byte[] { 112, 67, 103, 107, -92, -125, -112, -95, //
                    -97, -114, 117, -56, -53, -69, -25, -28 };
    private static final int ITERATION_COUNT = 65536;

    /** Core accepts versions up to this far ahead before it assumes garbage. */
    private static final int PLAUSIBLE_VERSIONS_AHEAD = 20;

    /**
     * @throws CliException
     *             <code>incorrect-password</code> if the header does not
     *             decrypt to a plausible content type and version
     */
    static EncryptedHeader read(File file, char[] password) throws IOException, CliException
    {
        byte[] head;
        try (var input = new BufferedInputStream(new FileInputStream(file)))
        {
            head = input.readNBytes(SIGNATURE_LENGTH + 1 + IV_LENGTH + BLOCK_LENGTH);
        }
        if (head.length < SIGNATURE_LENGTH + 1 + IV_LENGTH + BLOCK_LENGTH)
            throw new IOException("the encrypted file is truncated"); //$NON-NLS-1$

        int keyBits = head[SIGNATURE_LENGTH] == 1 ? 256 : 128;
        if (!ClientFactory.isKeyLengthSupported(keyBits))
            throw new IOException("this Java runtime does not support " + keyBits + "-bit AES"); //$NON-NLS-1$ //$NON-NLS-2$

        var iv = Arrays.copyOfRange(head, SIGNATURE_LENGTH + 1, SIGNATURE_LENGTH + 1 + IV_LENGTH);
        var firstBlock = Arrays.copyOfRange(head, SIGNATURE_LENGTH + 1 + IV_LENGTH, head.length);

        ByteBuffer plain;
        try
        {
            var cipher = Cipher.getInstance("AES/CBC/NoPadding"); //$NON-NLS-1$
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, keyBits), new IvParameterSpec(iv));
            plain = ByteBuffer.wrap(cipher.doFinal(firstBlock));
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("the encrypted file header could not be decrypted", e); //$NON-NLS-1$
        }

        int contentType = plain.getInt();
        int version = plain.getInt();

        if (contentType < 1 || contentType > 2 || version < 1
                        || version > CoreVersion.MAX_SUPPORTED_FILE_VERSION + PLAUSIBLE_VERSIONS_AHEAD)
            throw CliException.failure("incorrect-password", "Incorrect password", //$NON-NLS-1$ //$NON-NLS-2$
                            "The client file could not be decrypted with the given password.", 401);

        return new EncryptedHeader(FileFormat.encrypted(contentType == 2, keyBits), version);
    }

    private static SecretKeySpec deriveKey(char[] password, int keyBits) throws GeneralSecurityException
    {
        var spec = new PBEKeySpec(password, SALT, ITERATION_COUNT, keyBits);
        try
        {
            var key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec); //$NON-NLS-1$
            return new SecretKeySpec(key.getEncoded(), "AES"); //$NON-NLS-1$
        }
        finally
        {
            spec.clearPassword();
        }
    }
}

package name.abuchen.portfolio.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.util.ProgressMonitorInputStream;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.XStreamArrayListConverter;
import name.abuchen.portfolio.util.XStreamInstantConverter;
import name.abuchen.portfolio.util.XStreamLocalDateConverter;
import name.abuchen.portfolio.util.XStreamLocalDateTimeConverter;
import name.abuchen.portfolio.util.XStreamSecurityPriceConverter;

@SuppressWarnings("deprecation")
public class ClientFactory
{
    private static class PortfolioTransactionConverter extends ReflectionConverter
    {
        public PortfolioTransactionConverter(Mapper mapper, ReflectionProvider reflectionProvider)
        {
            super(mapper, reflectionProvider);
        }

        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
        {
            return type == PortfolioTransaction.class;
        }

        @Override
        protected boolean shouldUnmarshalField(Field field)
        {
            if ("fees".equals(field.getName()) || "taxes".equals(field.getName())) //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            return super.shouldUnmarshalField(field);
        }

    }

    /* package */ static class XmlSerialization
    {
        private boolean idReferences;

        public XmlSerialization(boolean idReferences)
        {
            this.idReferences = idReferences;
        }

        private XStream makeXStream(boolean isReading)
        {
            XStream xs = isReading ? xstreamReader() : xstreamWriter();
            if (idReferences)
                xs.setMode(XStream.ID_REFERENCES);
            else
                xs.setMode(XStream.XPATH_RELATIVE_REFERENCES);
            return xs;
        }

        public Client load(Reader input) throws IOException
        {
            try
            {
                Client client = (Client) makeXStream(true).fromXML(input);

                if (client.getVersion() > Client.CURRENT_VERSION)
                    throw new IOException(MessageFormat.format(Messages.MsgUnsupportedVersionClientFiled,
                                    client.getVersion()));

                upgradeModel(client);

                return client;
            }
            catch (XStreamException e)
            {
                throw new IOException(MessageFormat.format(Messages.MsgXMLFormatInvalid, e.getMessage()), e);
            }
        }

        void save(Client client, OutputStream output) throws IOException
        {
            Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

            makeXStream(false).toXML(client, writer);

            writer.flush();
        }
    }

    interface ClientPersister
    {
        Client load(InputStream input) throws IOException;

        void save(Client client, OutputStream output) throws IOException;
    }

    private static class PlainWriter implements ClientPersister
    {
        boolean idReferences;

        public PlainWriter(boolean idReferences)
        {
            this.idReferences = idReferences;
        }

        public PlainWriter()
        {
            this(false);
        }

        @Override
        public Client load(InputStream input) throws IOException
        {
            Client client = new XmlSerialization(idReferences)
                            .load(new InputStreamReader(input, StandardCharsets.UTF_8));
            client.getSaveFlags().add(SaveFlag.XML);
            if (idReferences)
            {
                client.getSaveFlags().add(SaveFlag.ID_REFERENCES);
            }
            return client;
        }

        @Override
        public void save(Client client, OutputStream output) throws IOException
        {
            new XmlSerialization(idReferences).save(client, output);
        }
    }

    private static class PlainWriterZIP implements ClientPersister
    {
        private ClientPersister body;

        public PlainWriterZIP(ClientPersister body)
        {
            this.body = body;
        }

        @Override
        public Client load(InputStream input) throws IOException
        {
            // wrap with zip input stream
            try (ZipInputStream zipin = new ZipInputStream(input))
            {
                ZipEntry entry = zipin.getNextEntry();

                if (body == null)
                {
                    if (entry.getName().endsWith(".portfolio")) //$NON-NLS-1$
                        body = new ProtobufWriter();
                    else
                        body = new PlainWriter();
                }

                Client client = body.load(zipin);
                client.getSaveFlags().add(SaveFlag.COMPRESSED);
                return client;
            }
        }

        @Override
        public void save(Client client, OutputStream output) throws IOException
        {
            // wrap with zip output stream
            try (ZipOutputStream zipout = new ZipOutputStream(output))
            {
                zipout.setLevel(Deflater.BEST_COMPRESSION);

                String name = body instanceof ProtobufWriter ? "data.portfolio" : "data.xml"; //$NON-NLS-1$ //$NON-NLS-2$

                zipout.putNextEntry(new ZipEntry(name));
                body.save(client, zipout);
                zipout.closeEntry();
            }
        }
    }

    /**
     * Encrypts the portfolio data.
     * <p/>
     * File format:
     * 
     * <pre>
     *   signature (8 bytes, PORTFOLIO)
     *   method (1 byte, 0 = AES126, 1 = AES256)
     *   initialization vector (16 bytes)
     *   ---
     *   content type (4 bytes, 1 = XML, 2 = PROTOBUF)
     *   version (4 bytes)
     *   compressed content
     * </pre>
     */
    private static class Decryptor implements ClientPersister
    {
        private static final byte[] SIGNATURE = new byte[] { 'P', 'O', 'R', 'T', 'F', 'O', 'L', 'I', 'O' };

        private static final byte[] SALT = new byte[] { 112, 67, 103, 107, -92, -125, -112, -95, //
                        -97, -114, 117, -56, -53, -69, -25, -28 };

        private static final String AES = "AES"; //$NON-NLS-1$
        private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"; //$NON-NLS-1$
        private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1"; //$NON-NLS-1$
        private static final int ITERATION_COUNT = 65536;
        private static final int IV_LENGTH = 16;

        private static final int AES128_KEYLENGTH = 128;
        private static final int AES256_KEYLENGTH = 256;

        private ClientPersister body;
        private char[] password;
        private int keyLength;

        public Decryptor(ClientPersister body, Set<SaveFlag> flags, char[] password)
        {
            this.body = body;
            this.password = password;
            this.keyLength = flags.contains(SaveFlag.AES256) ? AES256_KEYLENGTH : AES128_KEYLENGTH;
        }

        @Override
        public Client load(final InputStream input) throws IOException
        {
            try
            {
                // check signature
                byte[] signature = new byte[SIGNATURE.length];
                int read = input.read(signature);
                if (read != SIGNATURE.length)
                    throw new IOException("tried to read " + SIGNATURE.length + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$
                if (!Arrays.equals(signature, SIGNATURE))
                    throw new IOException(Messages.MsgNotAPortfolioFile);

                // read encryption method
                int method = input.read();
                this.keyLength = method == 1 ? AES256_KEYLENGTH : AES128_KEYLENGTH;

                // check if key length is supported
                if (!isKeyLengthSupported(this.keyLength))
                    throw new IOException(Messages.MsgKeyLengthNotSupported);

                // build secret key
                SecretKey secret = buildSecretKey();

                // read initialization vector
                byte[] iv = new byte[IV_LENGTH];
                read = input.read(iv);
                if (read != IV_LENGTH)
                    throw new IOException("tried to read " + IV_LENGTH + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$

                Client client;
                // build cipher and stream
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
                try (InputStream decrypted = new CipherInputStream(input, cipher))
                {
                    // read version information
                    byte[] bytes = new byte[4];
                    read = decrypted.read(bytes); // content type
                    if (read != bytes.length)
                        throw new IOException("tried to read " + bytes.length + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$

                    int contentType = ByteBuffer.wrap(bytes).getInt();
                    read = decrypted.read(bytes); // version number
                    if (read != bytes.length)
                        throw new IOException("tried to read " + bytes.length + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$

                    int version = ByteBuffer.wrap(bytes).getInt();

                    // sanity check if the file was properly decrypted
                    if (contentType < 1 || contentType > 2 || version < 1 || version > Client.CURRENT_VERSION + 20)
                        throw new IOException(Messages.MsgIncorrectPassword);
                    if (version > Client.CURRENT_VERSION)
                        throw new IOException(MessageFormat.format(Messages.MsgUnsupportedVersionClientFiled, version));

                    if (body == null)
                    {
                        if (contentType == 2)
                            body = new ProtobufWriter();
                        else
                            body = new PlainWriter();
                    }

                    // wrap with zip input stream
                    try (ZipInputStream zipin = new ZipInputStream(decrypted))
                    {
                        zipin.getNextEntry();

                        client = body.load(zipin);
                        client.getSaveFlags().add(SaveFlag.ENCRYPTED);
                        client.getSaveFlags().add(method == 1 ? SaveFlag.AES256 : SaveFlag.AES128);

                        try // NOSONAR
                        {
                            // explicitly close the stream to force bad padding
                            // exceptions to occur inside this try-catch-block
                            decrypted.close(); // NOSONAR
                        }
                        catch (IOException ex)
                        {
                            // starting with a later jdk 1.8.0 (for example
                            // 1.8.0_25), a javax.crypto.BadPaddingException
                            // "Given final block not properly padded" is thrown
                            // if we do not read the stream - so ignore that
                            // kind of exception
                            if (!(ex.getCause() instanceof BadPaddingException))
                                throw ex;
                        }
                    }
                }

                // save secret key for next save
                client.setSecret(secret);

                return client;
            }
            catch (GeneralSecurityException e)
            {
                throw new IOException(MessageFormat.format(Messages.MsgErrorDecrypting, e.getMessage()), e);
            }
        }

        @Override
        public void save(Client client, final OutputStream output) throws IOException
        {
            try
            {
                // check if key length is supported
                if (!isKeyLengthSupported(this.keyLength))
                    throw new IOException(Messages.MsgKeyLengthNotSupported);

                // get or build secret key
                // if password is given, it is used (when the user chooses
                // "save as" from the menu)
                SecretKey secret = password != null ? buildSecretKey() : client.getSecret();
                if (secret == null)
                    throw new IOException(Messages.MsgPasswordMissing);

                // save secret key for next save
                client.setSecret(secret);

                // write signature
                output.write(SIGNATURE);

                // write method
                output.write(secret.getEncoded().length * 8 == AES256_KEYLENGTH ? 1 : 0);

                // build cipher and stream
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secret);

                // write initialization vector
                AlgorithmParameters params = cipher.getParameters();
                byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
                output.write(iv);

                // encrypted stream
                try (OutputStream encrypted = new CipherOutputStream(output, cipher))
                {
                    // write version information

                    int contentType = body instanceof ProtobufWriter ? 2 : 1;

                    encrypted.write(ByteBuffer.allocate(4).putInt(contentType).array());
                    encrypted.write(ByteBuffer.allocate(4).putInt(client.getVersion()).array());

                    // wrap with zip output stream
                    try (ZipOutputStream zipout = new ZipOutputStream(encrypted))
                    {
                        zipout.setLevel(Deflater.BEST_COMPRESSION);
                        zipout.putNextEntry(new ZipEntry("data")); //$NON-NLS-1$

                        body.save(client, zipout);
                        zipout.closeEntry();
                    }
                }
            }
            catch (GeneralSecurityException e)
            {
                throw new IOException(MessageFormat.format(Messages.MsgErrorEncrypting, e.getMessage()), e);
            }
        }

        private SecretKey buildSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException
        {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, SALT, ITERATION_COUNT, keyLength);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), AES);
        }
    }

    private static XStream xstreamReader;
    private static XStream xstreamWriter;

    public static boolean isEncrypted(File file)
    {
        try
        {
            return getFlags(file).contains(SaveFlag.ENCRYPTED);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static Set<SaveFlag> getFlags(File file) throws IOException
    {
        Set<SaveFlag> flags = EnumSet.noneOf(SaveFlag.class);

        if (file.getName().endsWith(".zip")) //$NON-NLS-1$
        {
            flags.add(SaveFlag.XML);
            flags.add(SaveFlag.COMPRESSED);
        }
        else if (file.getName().endsWith(".portfolio")) //$NON-NLS-1$
        {
            try (InputStream input = new BufferedInputStream(new FileInputStream(file)))
            {
                // read signature

                byte[] signature = new byte[Decryptor.SIGNATURE.length];
                int read = input.read(signature);
                if (read != Decryptor.SIGNATURE.length)
                    throw new IOException(
                                    "tried to read " + Decryptor.SIGNATURE.length + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$

                if (Arrays.equals(Decryptor.SIGNATURE, signature))
                {
                    flags.add(SaveFlag.ENCRYPTED);
                }
                else if (startsWith(new byte[] { 80, 75, 3, 4 }, signature))
                {
                    // https://en.wikipedia.org/wiki/List_of_file_signatures
                    flags.add(SaveFlag.COMPRESSED);
                }
            }
        }

        if (flags.isEmpty())
        {
            flags.add(SaveFlag.XML);
            try (Reader input = new InputStreamReader(new FileInputStream(file)))
            {
                char[] buffer = new char[80];
                input.read(buffer);
                if (new String(buffer).contains("<client id=")) //$NON-NLS-1$
                {
                    flags.add(SaveFlag.ID_REFERENCES);
                }
            }
        }

        return flags;
    }

    private static boolean startsWith(byte[] expected, byte[] actual)
    {
        if (actual == null || expected == null)
            return false;

        int la = actual.length;
        int le = expected.length;

        if (la < le)
            return false;

        for (int ii = 0; ii < le; ii++)
            if (actual[ii] != expected[ii])
                return false;

        return true;
    }

    public static boolean isKeyLengthSupported(int keyLength)
    {
        try
        {
            return keyLength <= Cipher.getMaxAllowedKeyLength(Decryptor.CIPHER_ALGORITHM);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorEncrypting, e.getMessage()), e);
        }
    }

    public static Client load(File file, char[] password, IProgressMonitor monitor) throws IOException
    {
        Set<SaveFlag> flags = getFlags(file);

        if (flags.contains(SaveFlag.ENCRYPTED) && password == null)
            throw new IOException(Messages.MsgPasswordMissing);

        try
        {
            // progress monitor
            long bytesTotal = file.length();
            int increment = (int) Math.min(bytesTotal / 20L, Integer.MAX_VALUE);
            monitor.beginTask(MessageFormat.format(Messages.MsgReadingFile, file.getName()), 20);
            // open an input stream for the file using a 64 KB buffer to speed
            // up reading
            try (InputStream input = new ProgressMonitorInputStream(
                            new BufferedInputStream(new FileInputStream(file), 65536), increment, monitor))
            {
                ClientPersister persister = buildPersister(flags, password);
                Client client = persister.load(input);

                PortfolioLog.info(String.format("Loaded %s with %s", file.getName(), client.getSaveFlags().toString())); //$NON-NLS-1$

                return client;
            }
        }
        catch (FileNotFoundException e)
        {
            FileNotFoundException fnf = new FileNotFoundException(
                            MessageFormat.format(Messages.MsgFileNotFound, file.getAbsolutePath()));
            fnf.initCause(e);
            throw fnf;
        }
    }

    public static Client load(Reader input) throws IOException
    {
        return load(input, false);
    }

    @VisibleForTesting
    public static Client load(Reader input, boolean useIdReferences) throws IOException
    {
        try
        {
            return new XmlSerialization(useIdReferences).load(input);
        }
        finally
        {
            if (input != null)
                input.close();
        }
    }

    @VisibleForTesting
    public static Client load(InputStream input) throws IOException
    {
        return load(input, false);
    }

    @VisibleForTesting
    public static Client load(InputStream input, boolean useIdReferences) throws IOException
    {
        return load(new InputStreamReader(input, StandardCharsets.UTF_8), useIdReferences);
    }

    public static void save(final Client client, final File file) throws IOException
    {
        Set<SaveFlag> flags = EnumSet.copyOf(client.getSaveFlags());

        if (flags.isEmpty())
            flags.add(SaveFlag.XML);

        if (flags.contains(SaveFlag.ENCRYPTED) && client.getSecret() == null)
            throw new IOException(Messages.MsgPasswordMissing);

        writeFile(client, file, null, flags, true);
    }

    public static void saveAs(final Client client, final File file, char[] password, Set<SaveFlag> flags)
                    throws IOException
    {
        if (flags.isEmpty())
            flags.add(SaveFlag.XML);

        if (flags.contains(SaveFlag.ENCRYPTED) && password == null)
            throw new IOException(Messages.MsgPasswordMissing);

        writeFile(client, file, password, flags, true);
    }

    public static void exportAs(final Client client, final File file, char[] password, Set<SaveFlag> flags)
                    throws IOException
    {
        if (flags.isEmpty())
            flags.add(SaveFlag.XML);

        if (flags.contains(SaveFlag.ENCRYPTED) && password == null)
            throw new IOException(Messages.MsgPasswordMissing);

        writeFile(client, file, password, flags, false);
    }

    private static void writeFile(final Client client, final File file, char[] password, Set<SaveFlag> flags,
                    boolean updateFlags) throws IOException
    {
        PortfolioLog.info(String.format("Saving %s with %s", file.getName(), flags.toString())); //$NON-NLS-1$

        // open an output stream for the file using a 64 KB buffer to speed up
        // writing
        try (FileOutputStream stream = new FileOutputStream(file);
                        BufferedOutputStream output = new BufferedOutputStream(stream, 65536))
        {
            // lock file while writing (apparently network-attache storage is
            // garbling up the files if it already starts syncing while the file
            // is still being written)
            FileChannel channel = stream.getChannel();
            FileLock lock = null;

            try
            {
                // On OS X fcntl does not support locking files on AFP or SMB
                // https://bugs.openjdk.org/browse/JDK-8167023
                if (!Platform.getOS().equals(Platform.OS_MACOSX))
                    lock = channel.tryLock();
            }
            catch (IOException e)
            {
                // also on some other platforms (for example reported for Linux
                // Mint, locks are not supported on SMB shares)

                PortfolioLog.warning(MessageFormat.format("Failed to aquire lock {0} with message {1}", //$NON-NLS-1$
                                file.getAbsolutePath(), e.getMessage()));
            }

            ClientPersister persister = buildPersister(flags, password);
            persister.save(client, output);

            output.flush();

            if (lock != null && lock.isValid())
                lock.release();

            if (updateFlags)
            {
                client.getSaveFlags().clear();
                client.getSaveFlags().addAll(flags);
            }
        }
    }

    private static ClientPersister buildPersister(Set<SaveFlag> flags, char[] password)
    {
        ClientPersister body = null;

        if (flags.contains(SaveFlag.BINARY))
            body = new ProtobufWriter();
        else if (flags.contains(SaveFlag.XML))
            body = new PlainWriter(flags.contains(SaveFlag.ID_REFERENCES));

        if (flags.contains(SaveFlag.ENCRYPTED))
            return new Decryptor(body, flags, password);
        else if (flags.contains(SaveFlag.COMPRESSED))
            return new PlainWriterZIP(body);

        if (body == null)
            return new PlainWriter();
        else
            return body;
    }

    /* package */ static void upgradeModel(Client client)
    {
        client.doPostLoadInitialization();

        client.setFileVersionAfterRead(client.getVersion());

        switch (client.getVersion())
        {
            case 1: // NOSONAR
                fixAssetClassTypes(client);
                addFeedAndExchange(client);
            case 2: // NOSONAR
                addDecimalPlaces(client);
            case 3:
                // do nothing --> added industry classification
            case 4: // NOSONAR
                for (Security s : client.getSecurities())
                    s.generateUUID();
            case 5:
                // do nothing --> save industry taxonomy in client
            case 6:
                // do nothing --> added WKN attribute to security
            case 7: // NOSONAR
                // new portfolio transaction types:
                // DELIVERY_INBOUND, DELIVERY_OUTBOUND
                changePortfolioTransactionTypeToDelivery(client);
            case 8:
                // do nothing --> added 'retired' property to securities
            case 9:
                // do nothing --> added 'cross entries' to transactions
            case 10: // NOSONAR
                generateUUIDs(client);
            case 11:
                // do nothing --> added 'properties' to client
            case 12: // NOSONAR
                // added investment plans
                // added security on chart as benchmark *and* performance
                fixStoredBenchmarkChartConfigurations(client);
            case 13: // NOSONAR
                // introduce arbitrary taxonomies
                addAssetClassesAsTaxonomy(client);
                addIndustryClassificationAsTaxonomy(client);
                addAssetAllocationAsTaxonomy(client);
                fixStoredClassificationChartConfiguration(client);
                setDeprecatedFieldsToNull(client);
            case 14: // NOSONAR
                // added shares to track dividends per share
                assignSharesToDividendTransactions(client);
            case 15:
                // do nothing --> added 'isRetired' property to account
            case 16:
                // do nothing --> added 'feedURL' property to account
            case 17:
                // do nothing --> added notes attribute
            case 18:
                // do nothing --> added events (stock split) to securities
            case 19:
                // do nothing --> added attribute types
            case 20:
                // do nothing --> added note to investment plan
            case 21:
                // do nothing --> added taxes to portfolio transaction
            case 22:
                // do nothing --> added 'isRetired' property to portfolio
            case 23:
                // do nothing --> added 'latestFeed' and 'latestFeedURL'
                // property to security
            case 24:
                // do nothing --> added 'TAX_REFUND' as account transaction
            case 25: // NOSONAR
                // incremented precision of shares to 6 digits after the decimal
                // sign
                incrementSharesPrecisionFromFiveToSixDigitsAfterDecimalSign(client);
            case 26:
                // do nothing --> added client settings
            case 27: // NOSONAR
                // client settings include attribute types
                fixStoredChartConfigurationToSupportMultipleViews(client);
            case 28: // NOSONAR
                // added currency support --> designate a default currency (user
                // will get a dialog to change)
                setAllCurrencies(client, CurrencyUnit.EUR);
                // bumpUpCPIMonthValue --> CPI removed anyways
                convertFeesAndTaxesToTransactionUnits(client);
            case 29: // NOSONAR
                // added decimal places to stock quotes
                addDecimalPlacesToQuotes(client);
            case 30: // NOSONAR
                // added dashboards to model
                fixStoredChartConfigurationWithNewPerformanceSeriesKeys(client);
                migrateToConfigurationSets(client);
            case 31:
                // added INTEREST_CHARGE transaction type
            case 32:
                // added AED currency
            case 33:
                // added FEES_REFUND transaction type
            case 34:
                // add optional security to FEES, FEES_REFUND, TAXES
            case 35:
                // added flag to auto-generate tx from investment plan
            case 36:
                // converted from LocalDate to LocalDateTime
            case 37:
                // added boolean attribute type
            case 38:
                // added security exchange calendar
                // added onlineId to security
            case 39:
                // removed consumer price indices
            case 40:
                // added attributes to account and portfolio
            case 41:
                // added tax units to interest transaction
            case 42:
                // added data map to classification and assignment
            case 43:
                // added LimitPrice as attribute type
            case 44: // NOSONAR
                // added weights to dashboard columns
                fixDashboardColumnWeights(client);
            case 45:
                // added custom security type NOTE
            case 46: // NOSONAR
                // added dividend payment security event
                addDefaultLogoAttributes(client);
            case 47:
                // added fees to dividend transactions
            case 48: // NOSONAR
                incrementSharesPrecisionFromSixToEightDigitsAfterDecimalSign(client);
                // add 4 more decimal places to the quote to make it 8
                addDecimalPlacesToQuotes(client);
                addDecimalPlacesToQuotes(client);
            case 49: // NOSONAR
                fixLimitQuotesWith4AdditionalDecimalPlaces(client);
            case 50: // NOSONAR
                assignTxUUIDsAndUpdateAtInstants(client);
            case 51: // NOSONAR
                permanentelyRemoveCPIData(client);
                fixDimensionsList(client);
            case 52:
                // added properties to attribute types
            case 53: // NOSONAR
                fixSourceAttributeOfTransactions(client);
            case 54: // NOSONAR
                addKeyToTaxonomyClassifications(client);
            case 55: // NOSONAR
                fixGrossValueUnits(client);
            case 56: // NOSONAR
                // migrate client filters into model (done when setting the
                // client input as we do not have access to the preferences
                // here)

                // remove obsolete MARKET properties
                removeMarketSecurityProperty(client);
            case 57: // NOSONAR
                // remove securities in watchlists which are not present in "all
                // securities", see #3452
                removeWronglyAddedSecurities(client);
            case 58: // NOSONAR
                fixDataSeriesLabelForAccumulatedTaxes(client);
            case 59: // NOSONAR
                fixNullSecurityProperties(client);
            case 60: // NOSONAR
                addInvestmentPlanTypes(client);
            case 61: // NOSONAR
                removePortfolioReportMarketProperties(client);
            case 62: // NOSONAR
                updateSecurityChartLabelConfiguration(client);
            case 63: // NOSONAR
                fixNullSecurityEvents(client);
            case 64: // NOSONAR
                assignDashboardIds(client);
            case 65: // NOSOANR
                // moved 'source' field to security event

                client.setVersion(Client.CURRENT_VERSION);
                break;
            case Client.CURRENT_VERSION:
                break;
            default:
                break;
        }
    }

    private static void fixAssetClassTypes(Client client)
    {
        for (Security security : client.getSecurities())
        {
            if ("STOCK".equals(security.getType())) //$NON-NLS-1$ // NOSONAR
                security.setType("EQUITY"); //$NON-NLS-1$ // NOSONAR
            else if ("BOND".equals(security.getType())) // NOSONAR //$NON-NLS-1$
                security.setType("DEBT"); //$NON-NLS-1$ // NOSONAR
        }
    }

    private static void addFeedAndExchange(Client client)
    {
        for (Security s : client.getSecurities())
            s.setFeed(YahooFinanceQuoteFeed.ID);
    }

    private static void addDecimalPlaces(Client client)
    {
        for (Portfolio p : client.getPortfolios())
            for (PortfolioTransaction t : p.getTransactions())
                t.setShares(t.getShares() * 100000);
    }

    private static void changePortfolioTransactionTypeToDelivery(Client client)
    {
        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getType() == Type.TRANSFER_IN)
                    t.setType(Type.DELIVERY_INBOUND);
                else if (t.getType() == Type.TRANSFER_OUT)
                    t.setType(Type.DELIVERY_OUTBOUND);
            }
        }
    }

    private static void generateUUIDs(Client client)
    {
        for (Account a : client.getAccounts())
            a.generateUUID();
        for (Portfolio p : client.getPortfolios())
            p.generateUUID();
        for (Category c : client.getRootCategory().flatten()) // NOSONAR
            c.generateUUID();
    }

    @SuppressWarnings("nls")
    private static void fixStoredBenchmarkChartConfigurations(Client client)
    {
        // Until now, the performance chart was showing *only* the benchmark
        // series, not the actual performance series. Change keys as benchmark
        // values are prefixed with '[b]'

        replace(client, "PerformanceChartView-PICKER", //
                        "Security", "[b]Security", //
                        "ConsumerPriceIndex", "[b]ConsumerPriceIndex");
    }

    private static void addAssetClassesAsTaxonomy(Client client)
    {
        TaxonomyTemplate template = TaxonomyTemplate.byId("assetclasses"); //$NON-NLS-1$
        Taxonomy taxonomy = template.buildFromTemplate();
        taxonomy.setId("assetclasses"); //$NON-NLS-1$

        int rank = 1;

        Classification cash = taxonomy.getClassificationById("CASH"); //$NON-NLS-1$
        for (Account account : client.getAccounts())
        {
            Assignment assignment = new Assignment(account);
            assignment.setRank(rank++);
            cash.addAssignment(assignment);
        }

        for (Security security : client.getSecurities())
        {
            Classification classification = taxonomy.getClassificationById(security.getType()); // NOSONAR

            if (classification != null)
            {
                Assignment assignment = new Assignment(security);
                assignment.setRank(rank++);
                classification.addAssignment(assignment);
            }
        }

        client.addTaxonomy(taxonomy);
    }

    private static void addIndustryClassificationAsTaxonomy(Client client)
    {
        String oldIndustryId = client.getIndustryTaxonomy(); // NOSONAR

        Taxonomy taxonomy = null;

        if ("simple2level".equals(oldIndustryId)) //$NON-NLS-1$
            taxonomy = TaxonomyTemplate.byId(TaxonomyTemplate.INDUSTRY_SIMPLE2LEVEL).buildFromTemplate();
        else
            taxonomy = TaxonomyTemplate.byId(TaxonomyTemplate.INDUSTRY_GICS).buildFromTemplate();

        taxonomy.setId("industries"); //$NON-NLS-1$

        // add industry taxonomy only if at least one security has been assigned
        if (assignSecurities(client, taxonomy))
            client.addTaxonomy(taxonomy);
    }

    private static boolean assignSecurities(Client client, Taxonomy taxonomy)
    {
        boolean hasAssignments = false;

        int rank = 0;
        for (Security security : client.getSecurities())
        {
            Classification classification = taxonomy.getClassificationById(security.getIndustryClassification()); // NOSONAR

            if (classification != null)
            {
                Assignment assignment = new Assignment(security);
                assignment.setRank(rank++);
                classification.addAssignment(assignment);

                hasAssignments = true;
            }
        }

        return hasAssignments;
    }

    private static void addAssetAllocationAsTaxonomy(Client client)
    {
        Category category = client.getRootCategory(); // NOSONAR

        Taxonomy taxonomy = new Taxonomy("assetallocation", Messages.LabelAssetAllocation); //$NON-NLS-1$
        Classification root = new Classification(category.getUUID(), Messages.LabelAssetAllocation);
        root.setKey(taxonomy.getId());
        taxonomy.setRootNode(root);

        buildTree(root, category);

        root.assignRandomColors();

        client.addTaxonomy(taxonomy);
    }

    private static void buildTree(Classification node, Category category) // NOSONAR
    {
        int rank = 0;

        for (Category child : category.getChildren()) // NOSONAR
        {
            Classification classification = new Classification(node, child.getUUID(), child.getName());
            classification.setWeight(child.getPercentage() * Values.Weight.factor());
            classification.setRank(rank++);
            node.addChild(classification);

            buildTree(classification, child);
        }

        for (Object element : category.getElements())
        {
            Assignment assignment = element instanceof Account account ? new Assignment(account)
                            : new Assignment((Security) element);
            assignment.setRank(rank++);

            node.addAssignment(assignment);
        }
    }

    @SuppressWarnings("nls")
    private static void fixStoredClassificationChartConfiguration(Client client)
    {
        String name = Classification.class.getSimpleName();
        replace(client, "PerformanceChartView-PICKER", //
                        "AssetClass", name, //
                        "Category", name);

        replace(client, "StatementOfAssetsHistoryView-PICKER", //
                        "AssetClass", name, //
                        "Category", name);
    }

    private static void replace(Client client, String property, String... replacements)
    {
        if (replacements.length % 2 != 0)
            throw new UnsupportedOperationException();

        String value = client.getProperty(property);
        if (value != null)
            replaceAll(client, property, value, replacements);

        int index = 0;
        while (true)
        {
            String key = property + '$' + index;
            value = client.getProperty(key);
            if (value != null)
                replaceAll(client, key, value, replacements);
            else
                break;

            index++;
        }
    }

    private static void replaceAll(Client client, String key, String value, String[] replacements)
    {
        String newValue = value;
        for (int ii = 0; ii < replacements.length; ii += 2)
            newValue = newValue.replaceAll(replacements[ii], replacements[ii + 1]);
        client.setProperty(key, newValue);
    }

    private static void setDeprecatedFieldsToNull(Client client)
    {
        client.setRootCategory(null); // NOSONAR
        client.setIndustryTaxonomy(null); // NOSONAR

        for (Security security : client.getSecurities())
        {
            security.setIndustryClassification(null); // NOSONAR
            security.setType(null); // NOSONAR
        }
    }

    private static void assignSharesToDividendTransactions(Client client)
    {
        for (Security security : client.getSecurities())
        {
            List<TransactionPair<?>> transactions = security.getTransactions(client);

            // sort by date of transaction
            Collections.sort(transactions, TransactionPair.BY_DATE);

            // count and assign number of shares by account
            Map<Account, Long> account2shares = new HashMap<>();
            for (TransactionPair<? extends Transaction> t : transactions)
            {
                if (t.getTransaction() instanceof AccountTransaction)
                {
                    AccountTransaction accountTransaction = (AccountTransaction) t.getTransaction();

                    switch (accountTransaction.getType())
                    {
                        case DIVIDENDS:
                        case INTEREST:
                            Long shares = account2shares.get(t.getOwner());
                            accountTransaction.setShares(shares != null ? shares : 0);
                            break;
                        default:
                    }
                }
                else if (t.getTransaction() instanceof PortfolioTransaction)
                {
                    PortfolioTransaction portfolioTransaction = (PortfolioTransaction) t.getTransaction();

                    // determine account: if it exists, take the cross entry.
                    // otherwise the reference account
                    Account account = null;
                    switch (portfolioTransaction.getType())
                    {
                        case BUY:
                        case SELL: // NOSONAR
                            if (portfolioTransaction.getCrossEntry() != null)
                                account = (Account) portfolioTransaction.getCrossEntry()
                                                .getCrossOwner(portfolioTransaction);
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                        default:
                            if (account == null)
                                account = ((Portfolio) t.getOwner()).getReferenceAccount();
                    }

                    long delta = 0;
                    switch (portfolioTransaction.getType())
                    {
                        case BUY:
                        case TRANSFER_IN:
                            delta = portfolioTransaction.getShares();
                            break;
                        case SELL:
                        case TRANSFER_OUT:
                            delta = -portfolioTransaction.getShares();
                            break;
                        default:
                            break;
                    }

                    Long shares = account2shares.get(account);
                    account2shares.put(account, shares != null ? shares + delta : delta);
                }
            }
        }
    }

    private static void incrementSharesPrecisionFromFiveToSixDigitsAfterDecimalSign(Client client)
    {
        for (Portfolio portfolio : client.getPortfolios())
            for (PortfolioTransaction portfolioTransaction : portfolio.getTransactions())
                portfolioTransaction.setShares(portfolioTransaction.getShares() * 10);
        for (Account account : client.getAccounts())
            for (AccountTransaction accountTransaction : account.getTransactions())
                accountTransaction.setShares(accountTransaction.getShares() * 10);
    }

    private static void fixStoredChartConfigurationToSupportMultipleViews(Client client)
    {
        @SuppressWarnings("nls")
        String[] charts = new String[] { "name.abuchen.portfolio.ui.views.DividendsPerformanceView",
                        "name.abuchen.portfolio.ui.views.StatementOfAssetsViewer",
                        "name.abuchen.portfolio.ui.views.SecuritiesTable", //
                        "PerformanceChartView-PICKER", //
                        "StatementOfAssetsHistoryView-PICKER", //
                        "ReturnsVolatilityChartView-PICKER" };

        for (String chart : charts)
        {
            String config = client.removeProperty(chart);
            if (config == null) // if other values exist, they are in order
                continue;

            List<String> values = new ArrayList<>();
            values.add("Standard:=" + config); //$NON-NLS-1$

            int index = 0;
            config = client.getProperty(chart + '$' + index);
            while (config != null)
            {
                values.add(config);
                index++;
                config = client.getProperty(chart + '$' + index);
            }

            index = 0;
            for (String va : values)
                client.setProperty(chart + '$' + index++, va);
        }
    }

    /**
     * Sets all currency codes of accounts, securities, and transactions to the
     * given currency code.
     */
    public static void setAllCurrencies(Client client, String currencyCode)
    {
        client.setBaseCurrency(currencyCode);
        client.getAccounts().stream().forEach(a -> a.setCurrencyCode(currencyCode));
        client.getSecurities().stream().forEach(s -> s.setCurrencyCode(currencyCode));

        client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .forEach(t -> t.setCurrencyCode(currencyCode));
        client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .forEach(t -> t.setCurrencyCode(currencyCode));
    }

    private static void convertFeesAndTaxesToTransactionUnits(Client client)
    {
        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                long fees = t.fees; // NOSONAR
                if (fees != 0)
                    t.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of(t.getCurrencyCode(), fees)));
                t.fees = 0; // NOSONAR

                long taxes = t.taxes; // NOSONAR
                if (taxes != 0)
                    t.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(t.getCurrencyCode(), taxes)));
                t.taxes = 0; // NOSONAR
            }
        }
    }

    private static void addDecimalPlacesToQuotes(Client client)
    {
        // previously quotes worked in cents (2 decimal places). This change
        // adds 2 decimal places to support up to 4.
        int decimalPlacesAdded = 100;

        for (Security security : client.getSecurities())
        {
            security.getPrices().stream().filter(Objects::nonNull)
                            .forEach(p -> p.setValue(p.getValue() * decimalPlacesAdded));
            if (security.getLatest() != null)
            {
                LatestSecurityPrice l = security.getLatest();
                l.setValue(l.getValue() * decimalPlacesAdded);

                if (l.getHigh() != -1)
                    l.setHigh(l.getHigh() * decimalPlacesAdded);
                if (l.getLow() != -1)
                    l.setLow(l.getLow() * decimalPlacesAdded);
                if (l.previousClose != -1) // NOSONAR
                    l.previousClose = l.previousClose * decimalPlacesAdded; // NOSONAR
            }
        }

        List<AttributeType> typesWithQuotes = client.getSettings().getAttributeTypes()
                        .filter(t -> t.getConverter() instanceof AttributeType.QuoteConverter)
                        .collect(Collectors.toList());

        client.getSecurities().stream().map(Security::getAttributes).forEach(attributes -> {
            for (AttributeType t : typesWithQuotes)
            {
                Object value = attributes.get(t);
                if (value instanceof Long l)
                    attributes.put(t, l.longValue() * decimalPlacesAdded);
            }
        });
    }

    @SuppressWarnings("nls")
    private static void fixStoredChartConfigurationWithNewPerformanceSeriesKeys(Client client)
    {
        replace(client, "PerformanceChartView-PICKER", //
                        "Client-transferals;", "Client-delta_percentage;");
    }

    @SuppressWarnings("nls")
    private static void migrateToConfigurationSets(Client client)
    {
        // charts
        migrateToConfigurationSet(client, "PerformanceChartView-PICKER");
        migrateToConfigurationSet(client, "StatementOfAssetsHistoryView-PICKER");
        migrateToConfigurationSet(client, "ReturnsVolatilityChartView-PICKER");

        // columns config
        migrateToConfigurationSet(client, "name.abuchen.portfolio.ui.views.SecuritiesPerformanceView");
        migrateToConfigurationSet(client, "name.abuchen.portfolio.ui.views.SecuritiesTable");
        migrateToConfigurationSet(client, "name.abuchen.portfolio.ui.views.StatementOfAssetsViewer");

        // up until version 30, the properties were only used for view
        // configurations (which are migrated now into configuration sets).
        // Clear all remaining properties.
        client.clearProperties();
    }

    private static void migrateToConfigurationSet(Client client, String key)
    {
        ConfigurationSet configSet = null;

        int index = 0;

        while (true)
        {
            String config = client.removeProperty(key + '$' + index);
            if (config == null)
                break;

            if (configSet == null)
                configSet = client.getSettings().getConfigurationSet(key);

            String[] split = config.split(":="); //$NON-NLS-1$
            if (split.length == 2)
                configSet.add(new ConfigurationSet.Configuration(split[0], split[1]));

            index++;
        }
    }

    private static void fixDashboardColumnWeights(Client client)
    {
        client.getDashboards().flatMap(d -> d.getColumns().stream()).forEach(c -> c.setWeight(1));
    }

    private static void addDefaultLogoAttributes(Client client)
    {
        Function<Class<? extends Attributable>, AttributeType> factory = target -> {
            AttributeType type = new AttributeType("logo"); //$NON-NLS-1$
            type.setName(Messages.AttributesLogoName);
            type.setColumnLabel(Messages.AttributesLogoColumn);
            type.setTarget(target);
            type.setType(String.class);
            type.setConverter(ImageConverter.class);
            return type;
        };

        client.getSettings().addAttributeType(factory.apply(Security.class));
        client.getSettings().addAttributeType(factory.apply(Account.class));
        client.getSettings().addAttributeType(factory.apply(Portfolio.class));
        client.getSettings().addAttributeType(factory.apply(InvestmentPlan.class));
    }

    private static void incrementSharesPrecisionFromSixToEightDigitsAfterDecimalSign(Client client)
    {
        for (Portfolio portfolio : client.getPortfolios())
            for (PortfolioTransaction portfolioTransaction : portfolio.getTransactions())
                portfolioTransaction.setShares(portfolioTransaction.getShares() * 100);
        for (Account account : client.getAccounts())
            for (AccountTransaction accountTransaction : account.getTransactions())
                accountTransaction.setShares(accountTransaction.getShares() * 100);
    }

    private static void fixLimitQuotesWith4AdditionalDecimalPlaces(Client client)
    {
        List<AttributeType> typesWithLimit = client.getSettings().getAttributeTypes()
                        .filter(t -> t.getConverter() instanceof AttributeType.LimitPriceConverter)
                        .collect(Collectors.toList());

        client.getSecurities().stream().map(Security::getAttributes).forEach(attributes -> {
            for (AttributeType t : typesWithLimit)
            {
                Object value = attributes.get(t);
                if (value instanceof LimitPrice lp)
                {
                    attributes.put(t, new LimitPrice(lp.getRelationalOperator(), lp.getValue() * 10000));
                }
            }
        });
    }

    private static void assignTxUUIDsAndUpdateAtInstants(Client client)
    {
        for (Account a : client.getAccounts())
        {
            a.setUpdatedAt(Instant.now());
            for (Transaction t : a.getTransactions())
            {
                t.setUpdatedAt(Instant.now());
                t.generateUUID();
            }
        }

        for (Portfolio p : client.getPortfolios())
        {
            p.setUpdatedAt(Instant.now());
            for (Transaction t : p.getTransactions())
            {
                t.setUpdatedAt(Instant.now());
                t.generateUUID();
            }
        }

        for (Security s : client.getSecurities())
        {
            s.setUpdatedAt(Instant.now());
        }
    }

    private static void permanentelyRemoveCPIData(Client client)
    {
        client.consumerPriceIndeces = null; // NOSONAR
    }

    private static void fixDimensionsList(Client client)
    {
        client.getTaxonomies().forEach(t -> {
            if (t.getDimensions() != null)
                t.setDimensions(new ArrayList<>(t.getDimensions()));
        });
    }

    private static void fixSourceAttributeOfTransactions(Client client)
    {
        List<Transaction> allTransactions = new ArrayList<>();
        client.getAccounts().forEach(a -> allTransactions.addAll(a.getTransactions()));
        client.getPortfolios().forEach(p -> allTransactions.addAll(p.getTransactions()));

        Pattern pattern = Pattern.compile("^((?<note>.*) \\| )?(?<file>[^ ]*\\.(pdf|csv))$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

        for (Transaction tx : allTransactions)
        {
            String note = TextUtil.trim(tx.getNote());
            if (note == null || note.length() == 0)
                continue;

            Matcher m = pattern.matcher(note);
            if (m.matches())
            {
                tx.setNote(TextUtil.trim(m.group("note"))); //$NON-NLS-1$
                tx.setSource(TextUtil.trim(m.group("file"))); //$NON-NLS-1$
            }
        }
    }

    private static void addKeyToTaxonomyClassifications(Client client)
    {
        List<TaxonomyTemplate> taxonomyTemplates = TaxonomyTemplate.list();
        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            if (Strings.isNullOrEmpty(taxonomy.getRoot().getKey()))
            {
                taxonomyTemplates.stream().filter(tt -> tt.getName().equals(taxonomy.getName())).findAny()
                                .ifPresent(template -> {
                                    copyClassificationKeys(template.build().getRoot(), taxonomy.getRoot());
                                });
            }
        }
    }

    private static void copyClassificationKeys(Classification from, Classification to)
    {
        to.setKey(from.getKey());

        Map<String, Classification> fromChildren = from.getChildren().stream()
                        .collect(Collectors.toMap(Classification::getName, Function.identity(), (r, l) -> null));

        Map<String, Classification> toChildren = to.getChildren().stream()
                        .collect(Collectors.toMap(Classification::getName, Function.identity(), (r, l) -> null));

        for (Map.Entry<String, Classification> entry : fromChildren.entrySet())
        {
            String key = entry.getKey();
            Classification fromChild = entry.getValue();
            if (toChildren.containsKey(key))
            {
                copyClassificationKeys(fromChild, toChildren.get(key));
            }
        }
    }

    private static void fixGrossValueUnits(Client client)
    {
        for (Portfolio portfolio : client.getPortfolios())
            for (PortfolioTransaction tx : portfolio.getTransactions())
                fixGrossValueUnit(tx);

        for (Account account : client.getAccounts())
            for (AccountTransaction tx : account.getTransactions())
                fixGrossValueUnit(tx);
    }

    private static void fixGrossValueUnit(Transaction tx)
    {
        Optional<Unit> unit = tx.getUnit(Unit.Type.GROSS_VALUE);

        if (unit.isEmpty())
            return;

        Unit grossValueUnit = unit.get();
        Money calculatedGrossValue = tx.getGrossValue();

        if (grossValueUnit.getAmount().equals(calculatedGrossValue))
            return;

        // check if it a rounding difference that is acceptable
        try
        {
            Unit u = new Unit(Unit.Type.GROSS_VALUE, calculatedGrossValue, grossValueUnit.getForex(),
                            grossValueUnit.getExchangeRate());

            tx.removeUnit(grossValueUnit);
            tx.addUnit(u);
            return;
        }
        catch (IllegalArgumentException ignore)
        {
            // recalculate the unit to fix the gross value
        }

        try
        {
            Money updatedGrossValue = Money.of(grossValueUnit.getForex().getCurrencyCode(),
                            BigDecimal.valueOf(calculatedGrossValue.getAmount())
                                            .divide(grossValueUnit.getExchangeRate(), Values.MC)
                                            .setScale(0, RoundingMode.HALF_EVEN).longValue());

            tx.removeUnit(grossValueUnit);
            tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, calculatedGrossValue, updatedGrossValue,
                            grossValueUnit.getExchangeRate()));
        }
        catch (IllegalArgumentException e)
        {
            // ignore in case we are still running into rounding differences
            // (for example: 4,33 EUR / 131,53 = 0,03 JPY but 0,03 JPY * 131,53
            // = 3,95 EUR) because otherwise the user cannot open the file at
            // all (and manually fix the issue)
        }
    }

    private static void removeMarketSecurityProperty(Client client)
    {
        for (Security security : client.getSecurities())
            security.removePropertyIf(p -> p == null || p.getType() == SecurityProperty.Type.MARKET);
    }

    private static void removeWronglyAddedSecurities(Client client)
    {
        client.getWatchlists() //
                        .forEach(w -> new ArrayList<>(w.getSecurities()) //
                                        .forEach(s -> {
                                            if (!client.getSecurities().contains(s))
                                            {
                                                if (s.getTransactions(client).isEmpty())
                                                    w.getSecurities().remove(s);
                                                else
                                                    client.addSecurity(s);
                                            }
                                        }));
    }

    private static void fixDataSeriesLabelForAccumulatedTaxes(Client client)
    {
        if (!client.getSettings().hasConfigurationSet("StatementOfAssetsHistoryView-PICKER")) //$NON-NLS-1$
            return;

        var configSet = client.getSettings().getConfigurationSet("StatementOfAssetsHistoryView-PICKER"); //$NON-NLS-1$

        configSet.getConfigurations() //
                        .filter(config -> config.getData() != null) //
                        .forEach(config -> config.setData(config.getData() //
                                        .replace("Client-taxes;", "Client-taxes_accumulated;"))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void fixNullSecurityProperties(Client client)
    {
        // see https://github.com/portfolio-performance/portfolio/issues/3895

        for (Security security : client.getSecurities())
        {
            var properties = security.getProperties().toList();

            for (SecurityProperty p : properties)
            {
                if (p == null)
                {
                    security.removeProperty(null);
                }
            }
        }
    }

    private static void fixNullSecurityEvents(Client client)
    {
        // see
        // https://forum.portfolio-performance.info/t/fehlermeldung-cannot-invoke-name-abuchen-portfolio-model-securityevent-gettype-because-event-is-null/29406

        for (Security security : client.getSecurities())
        {
            var events = new ArrayList<>(security.getEvents());

            for (SecurityEvent e : events)
            {
                if (e == null)
                {
                    security.removeEvent(null);
                }
            }
        }
    }

    private static void addInvestmentPlanTypes(Client client)
    {
        for (InvestmentPlan plan : client.getPlans())
        {
            if (plan.getPortfolio() != null)
            {
                plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
            }
            else
            {
                plan.setType(plan.getAmount() >= 0 ? InvestmentPlan.Type.DEPOSIT : InvestmentPlan.Type.REMOVAL);
                plan.setAmount(Math.abs(plan.getAmount()));
            }
        }
    }

    private static void removePortfolioReportMarketProperties(Client client)
    {
        // with the new Portfolio Report API, we only need the currency and do
        // not provide the markets anymore. By removing the properties, we
        // indicate to the mobile client to use the new API

        for (Security security : client.getSecurities())
        {
            security.removePropertyIf(p -> p.getType() == SecurityProperty.Type.FEED
                            && ("PORTFOLIO-REPORT-MARKETS".equals(p.getName()) //$NON-NLS-1$
                                            || "PORTFOLIO-REPORT-MARKET".equals(p.getName()))); //$NON-NLS-1$
        }
    }

    private static void updateSecurityChartLabelConfiguration(Client client)
    {
        // PR 4120 splits SHOW_DATA_LABELS into
        // SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL, SHOW_DATA_DIVIDEND_LABEL,
        // SHOW_DATA_EXTREMES_LABEL

        var propertyKey = "security-chart-details"; //$NON-NLS-1$

        var chartConfig = client.getProperty(propertyKey);
        if (chartConfig == null)
            return;

        client.setProperty(propertyKey, chartConfig.replace("SHOW_DATA_LABELS", //$NON-NLS-1$
                        "SHOW_DATA_DIVESTMENT_INVESTMENT_LABEL,SHOW_DATA_DIVIDEND_LABEL,SHOW_DATA_EXTREMES_LABEL")); //$NON-NLS-1$
    }

    private static void assignDashboardIds(Client client)
    {
        // dashboards get a unique identifier to reliably identify them in
        // configuration (say the navigation bar)
        client.getDashboards().forEach(dashboard -> dashboard.setId(UUID.randomUUID().toString()));
    }

    private static synchronized XStream xstreamReader()
    {
        if (xstreamReader == null)
            xstreamReader = xstreamFactory();

        return xstreamReader;
    }

    private static synchronized XStream xstreamWriter()
    {
        if (xstreamWriter == null)
        {
            xstreamWriter = xstreamFactory();

            // add the immutable types only when writing the file because there
            // are files out in the wild that still have referenced objects
            // (probably due to bugs in older version and/or manual manipulation
            // of files)

            // Java types which aren't multiple-referenced in PP data model, so
            // skip giving "id" attribute to these, as that adds a lot of noise
            // to the produced XML.
            xstreamWriter.addImmutableType(HashMap.class, false);
            xstreamWriter.addImmutableType(ArrayList.class, false);

            // PP's wrappers around Java types, again not multiple-reference,
            // skip adding "id" attribute for the same reason.
            xstreamWriter.addImmutableType(TypedMap.class, false);
            xstreamWriter.addImmutableType(Attributes.class, false);

            xstreamWriter.addImmutableType(Money.class, false);
            xstreamWriter.addImmutableType(ClientSettings.class, false);
            xstreamWriter.addImmutableType(Bookmark.class, false);
            xstreamWriter.addImmutableType(Transaction.Unit.class, false);
            xstreamWriter.addImmutableType(LatestSecurityPrice.class, false);
            xstreamWriter.addImmutableType(AttributeType.class, false);
            xstreamWriter.addImmutableType(SecurityPrice.class, false);
            xstreamWriter.addImmutableType(LimitPrice.class, false);
            xstreamWriter.addImmutableType(Taxonomy.class, false);
            xstreamWriter.addImmutableType(Assignment.class, false);
            xstreamWriter.addImmutableType(Dashboard.class, false);
            xstreamWriter.addImmutableType(Dashboard.Column.class, false);
            xstreamWriter.addImmutableType(Dashboard.Widget.class, false);
            xstreamWriter.addImmutableType(SecurityEvent.class, false);
            xstreamWriter.addImmutableType(ConfigurationSet.class, false);
            xstreamWriter.addImmutableType(ConfigurationSet.Configuration.class, false);
            xstreamWriter.addImmutableType(SecurityProperty.class, false);
        }

        return xstreamWriter;
    }

    @SuppressWarnings("nls")
    private static XStream xstreamFactory()
    {
        var xstream = new XStream();

        xstream.allowTypesByWildcard(new String[] { "name.abuchen.portfolio.model.**" });

        xstream.setClassLoader(ClientFactory.class.getClassLoader());

        // because we introduced LocalDate and LocalDateTime before Xstream
        // was supporting it, we must declare it referenceable for backward
        // compatibility reasons
        xstream.addImmutableType(LocalDate.class, true);
        xstream.addImmutableType(LocalDateTime.class, true);

        xstream.registerConverter(new XStreamLocalDateConverter());
        xstream.registerConverter(new XStreamLocalDateTimeConverter());
        xstream.registerConverter(new XStreamInstantConverter());
        xstream.registerConverter(new XStreamSecurityPriceConverter());
        xstream.registerConverter(
                        new PortfolioTransactionConverter(xstream.getMapper(), xstream.getReflectionProvider()));

        xstream.registerConverter(new MapConverter(xstream.getMapper(), TypedMap.class));
        xstream.registerConverter(new XStreamArrayListConverter(xstream.getMapper()));

        xstream.useAttributeFor(Money.class, "amount");
        xstream.useAttributeFor(Money.class, "currencyCode");
        xstream.aliasAttribute(Money.class, "currencyCode", "currency");

        xstream.alias("account", Account.class);
        xstream.alias("client", Client.class);
        xstream.alias("settings", ClientSettings.class);
        xstream.alias("bookmark", Bookmark.class);
        xstream.alias("portfolio", Portfolio.class);
        xstream.alias("unit", Transaction.Unit.class);
        xstream.useAttributeFor(Transaction.Unit.class, "type");
        xstream.alias("account-transaction", AccountTransaction.class);
        xstream.alias("portfolio-transaction", PortfolioTransaction.class);
        xstream.alias("security", Security.class);
        xstream.addImplicitCollection(Security.class, "properties");
        xstream.alias("latest", LatestSecurityPrice.class);
        xstream.alias("category", Category.class); // NOSONAR
        xstream.alias("watchlist", Watchlist.class);
        xstream.alias("investment-plan", InvestmentPlan.class);
        xstream.alias("attribute-type", AttributeType.class);

        xstream.alias("price", SecurityPrice.class);
        xstream.useAttributeFor(SecurityPrice.class, "date");
        xstream.aliasField("t", SecurityPrice.class, "date");
        xstream.useAttributeFor(SecurityPrice.class, "value");
        xstream.aliasField("v", SecurityPrice.class, "value");

        xstream.alias("limitPrice", LimitPrice.class);

        xstream.alias("cpi", ConsumerPriceIndex.class); // NOSONAR
        xstream.useAttributeFor(ConsumerPriceIndex.class, "year"); // NOSONAR
        xstream.aliasField("y", ConsumerPriceIndex.class, "year"); // NOSONAR
        xstream.useAttributeFor(ConsumerPriceIndex.class, "month"); // NOSONAR
        xstream.aliasField("m", ConsumerPriceIndex.class, "month"); // NOSONAR
        xstream.useAttributeFor(ConsumerPriceIndex.class, "index"); // NOSONAR
        xstream.aliasField("i", ConsumerPriceIndex.class, "index"); // NOSONAR

        xstream.alias("buysell", BuySellEntry.class);
        xstream.alias("account-transfer", AccountTransferEntry.class);
        xstream.alias("portfolio-transfer", PortfolioTransferEntry.class);

        xstream.alias("taxonomy", Taxonomy.class);
        xstream.alias("classification", Classification.class);
        xstream.alias("assignment", Assignment.class);

        xstream.alias("dashboard", Dashboard.class);
        xstream.useAttributeFor(Dashboard.class, "name");
        xstream.alias("column", Dashboard.Column.class);
        xstream.alias("widget", Dashboard.Widget.class);
        xstream.useAttributeFor(Dashboard.Widget.class, "type");

        xstream.alias("event", SecurityEvent.class);
        xstream.alias("dividendEvent", SecurityEvent.DividendEvent.class);
        xstream.alias("config-set", ConfigurationSet.class);
        xstream.alias("config", ConfigurationSet.Configuration.class);

        xstream.processAnnotations(SecurityProperty.class);

        return xstream;
    }
}

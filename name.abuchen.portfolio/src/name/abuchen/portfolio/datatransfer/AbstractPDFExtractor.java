package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/* package */abstract class AbstractPDFExtractor implements Extractor
{
    private final Client client;
    private final PDFTextStripper textStripper;
    private final List<String> bankIdentifier = new ArrayList<String>();
    private final List<DocumentType> documentTypes = new ArrayList<DocumentType>();

    private final Map<String, Security> isin2security;
    private final Map<String, Security> wkn2security;

    public AbstractPDFExtractor(Client client) throws IOException
    {
        this.client = client;

        textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);

        this.isin2security = client
                        .getSecurities()
                        .stream()
                        .filter(s -> s.getIsin() != null && !s.getIsin().isEmpty())
                        .collect(Collectors.toMap(Security::getIsin, s -> s,
                                        (l, r) -> failWith(Messages.MsgErrorDuplicateISIN, l.getIsin())));

        this.wkn2security = client
                        .getSecurities()
                        .stream()
                        .filter(s -> s.getWkn() != null && !s.getWkn().isEmpty())
                        .collect(Collectors.toMap(Security::getWkn, s -> s,
                                        (l, r) -> failWith(Messages.MsgErrorDuplicateWKN, l.getWkn())));
    }

    protected Security failWith(String message, String parameter)
    {
        throw new IllegalArgumentException(MessageFormat.format(message, parameter));
    }

    protected final void addDocumentTyp(DocumentType type)
    {
        this.documentTypes.add(type);
    }

    protected final void addBankIdentifier(String identifier)
    {
        this.bankIdentifier.add(identifier);
    }

    @Override
    public String getFilterExtension()
    {
        return "*.pdf"; //$NON-NLS-1$
    }

    @Override
    public List<Item> extract(List<File> files, List<Exception> errors)
    {
        List<Item> results = new ArrayList<Item>();
        for (File f : files)
        {
            try
            {
                String text = strip(f);
                results.addAll(extract(f.getName(), text, errors));
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }

        Set<Security> added = new HashSet<Security>();
        for (Item item : new ArrayList<Item>(results))
        {
            Security security = item.getSecurity();
            if (security != null && !client.getSecurities().contains(security) && !added.contains(security))
            {
                added.add(security);
                results.add(new SecurityItem(security));
            }
        }

        return results;
    }

    /* testing */String strip(File file) throws IOException
    {
        try (PDDocument doc = PDDocument.load(file))
        {
            return textStripper.getText(doc);
        }
    }

    private List<Item> extract(String filename, String text, List<Exception> errors)
    {
        try
        {
            checkBankIdentifier(filename, text);

            List<Item> items = new ArrayList<Item>();

            for (DocumentType type : documentTypes)
            {
                if (type.matches(text))
                    type.parse(items, text);
            }

            if (items.isEmpty())
            {
                errors.add(new UnsupportedOperationException(MessageFormat.format(
                                Messages.PDFdbMsgCannotDetermineFileType, filename)));
            }

            for (Item item : items)
                item.getSubject().setNote(filename);

            return items;
        }
        catch (IllegalArgumentException | UnsupportedOperationException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    private void checkBankIdentifier(String filename, String text)
    {
        if (bankIdentifier.isEmpty())
            bankIdentifier.add(getLabel());

        for (String identifier : bankIdentifier)
            if (text.contains(identifier))
                return;

        throw new UnsupportedOperationException( //
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, filename, getLabel()));
    }

    protected Security getOrCreateSecurity(Map<String, String> values)
    {
        Security security = buildSecurity(values);

        Security existing = isin2security.get(security.getIsin());
        if (existing != null)
            return existing;

        existing = wkn2security.get(security.getWkn());
        if (existing != null)
            return existing;

        if (security.getIsin() != null)
            isin2security.put(security.getIsin(), security);
        if (security.getWkn() != null)
            wkn2security.put(security.getWkn(), security);
        return security;
    }

    private Security buildSecurity(Map<String, String> values)
    {
        Security security = new Security();
        security.setName(values.get("name").trim()); //$NON-NLS-1$
        security.setIsin(values.get("isin")); //$NON-NLS-1$
        security.setWkn(values.get("wkn")); //$NON-NLS-1$
        security.setFeed(QuoteFeed.MANUAL);

        if (security.getIsin() == null && security.getWkn() == null)
            throw new IllegalArgumentException("Unable to construct security: " + values.toString()); //$NON-NLS-1$

        return security;
    }

    final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY); //$NON-NLS-1$

    protected long asShares(String value)
    {
        try
        {
            return Math.round(numberFormat.parse(value).doubleValue() * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /* protected */long asAmount(String value)
    {
        try
        {
            return Math.abs(Math.round(numberFormat.parse(value).doubleValue() * Values.Amount.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /* protected */Date asDate(String value)
    {
        LocalDate date = LocalDate.parse(value, DATE_FORMAT);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

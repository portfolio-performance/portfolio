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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class DeutscheBankPDFExctractor implements Extractor
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY); //$NON-NLS-1$
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);

    private final PDFTextStripper textStripper;
    private final Map<String, Security> isin2security;

    public DeutscheBankPDFExctractor(Client client) throws IOException
    {
        textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);

        this.isin2security = client.getSecurities().stream() //
                        .filter(s -> s.getIsin() != null && !s.getIsin().isEmpty()) //
                        .collect(Collectors.toMap(Security::getIsin, s -> s));
    }

    @Override
    public String getLabel()
    {
        return Messages.PDFdbLabel;
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
            try (PDDocument doc = PDDocument.load(f))
            {
                String text = textStripper.getText(doc);
                results.addAll(extract(f.getName(), text, errors));
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }
        return results;
    }

    /* protected */List<Item> extract(String filename, String text, List<Exception> errors)
    {
        try
        {
            if (!text.contains("Deutsche Bank")) //$NON-NLS-1$
                throw new UnsupportedOperationException( //
                                MessageFormat.format(Messages.PDFdbMsgFileNotSupported, filename));

            boolean isDividend = text.contains("Ertragsgutschrift"); //$NON-NLS-1$
            if (!isDividend)
                throw new UnsupportedOperationException( //
                                MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, filename));

            List<Item> items = new ArrayList<Item>();
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            transaction.setNote(filename);

            fillInSecurity(transaction, filename, lines, items);
            fillInDateAndAmount(transaction, lines);
            fillInShares(transaction, lines);

            items.add(new TransactionItem(transaction));

            return items;
        }
        catch (ParseException | UnsupportedOperationException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    private void fillInDateAndAmount(AccountTransaction transaction, String[] lines) throws ParseException
    {
        Pattern pattern = Pattern.compile("Gutschrift mit Wert (\\d+.\\d+.\\d{4}+) (\\d+,\\d+) (\\w{3}+)"); //$NON-NLS-1$
        for (int ii = 0; ii < lines.length; ii++)
        {
            Matcher matcher = pattern.matcher(lines[ii]);
            if (matcher.matches())
            {
                LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
                transaction.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                long amount = Math.round(numberFormat.parse(matcher.group(2)).doubleValue() * Values.Amount.factor());
                transaction.setAmount(amount);

                break;
            }
        }
    }

    private void fillInShares(AccountTransaction transaction, String[] lines) throws ParseException
    {
        Pattern pattern = Pattern.compile("(\\d+,\\d*) (\\S*) (\\S*)"); //$NON-NLS-1$
        for (int ii = 0; ii < lines.length; ii++)
        {
            Matcher matcher = pattern.matcher(lines[ii]);
            if (matcher.matches())
            {
                long shares = Math.round(numberFormat.parse(matcher.group(0)).doubleValue() * Values.Share.factor());
                transaction.setShares(shares);

                break;
            }
        }
    }

    private void fillInSecurity(AccountTransaction transaction, String filename, String[] lines, List<Item> items)
    {
        Security security = extractSecurity(lines);

        if (security == null)
            throw new UnsupportedOperationException(MessageFormat.format(Messages.PDFdbMsgCannotFindSecurity, filename));

        Security existing = isin2security.get(security.getIsin());
        if (existing != null)
        {
            transaction.setSecurity(existing);
        }
        else
        {
            isin2security.put(security.getIsin(), security);
            items.add(new SecurityItem(security));
            transaction.setSecurity(security);
        }
    }

    private Security extractSecurity(String[] lines)
    {
        // looking for a group of three lines
        // Stück WKN ISIN
        // (number) (wkn) (isin)
        // (name of security)

        for (int ii = 0; ii < lines.length - 2; ii++)
        {
            if ("Stück WKN ISIN".equals(lines[ii])) //$NON-NLS-1$
            {
                Pattern pattern = Pattern.compile("(\\d+,\\d*) (\\S*) (\\S*)"); //$NON-NLS-1$
                Matcher matcher = pattern.matcher(lines[ii + 1]);
                if (matcher.matches())
                {
                    Security security = new Security();
                    security.setIsin(matcher.group(3));
                    security.setWkn(matcher.group(2));
                    security.setName(lines[ii + 2]);
                    security.setFeed(QuoteFeed.MANUAL);
                    return security;
                }

                break;
            }
        }

        return null;
    }
}

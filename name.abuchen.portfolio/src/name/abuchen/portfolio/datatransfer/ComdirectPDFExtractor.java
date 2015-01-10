package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class ComdirectPDFExtractor implements Extractor
{

    private PDFTextStripper stripper;
    private DateFormat df;
    private Pattern isinPattern;
    private Matcher isinMatcher;
    private NumberFormat format;
    private List<Security> allSecurities;

    public ComdirectPDFExtractor(Client client) throws IOException
    {
        // Parsing formats rely on german style PDFs
        df = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
        format = NumberFormat.getInstance(Locale.GERMANY);

        isinPattern = Pattern.compile("[A-Z]{2}([A-Z0-9]){9}[0-9]"); //$NON-NLS-1$
        allSecurities = new ArrayList<Security>(client.getSecurities());
        stripper = new PDFTextStripper();
    }

    @Override
    public List<Item> extract(List<File> files, List<Exception> errors)
    {
        List<Item> results = new ArrayList<Item>();
        for (File f : files)
        {
            try (PDDocument doc = PDDocument.load(f))
            {
                String text = stripper.getText(doc);
                String filename = f.getName();
                results.addAll(extract(text, filename, errors));
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }
        return results;
    }

    public List<Item> extract(String text, String filename, List<Exception> errors)
    {
        if (!text.contains("comdirect bank")) //$NON-NLS-1$
        {
            errors.add(new UnsupportedOperationException(MessageFormat.format(Messages.PDFcomdirectMsgFileNotSupported,
                            filename)));
            return Collections.emptyList();
        }

        List<Item> results = new ArrayList<Item>();
        // an interest payment is identified by the topic string
        if (text.contains("Gutschrift fälliger Wertpapier-Erträge")) //$NON-NLS-1$
        {
            // No cashflow, no transaction to be generated
            if (text.contains("Ertragsthesaurierung")) { return results; } //$NON-NLS-1$
            isinMatcher = isinPattern.matcher(text);
            // Is to be used to find the security
            String isin;
            Security security;
            isinMatcher.find();
            isin = isinMatcher.group();
            security = getSecurityForISIN(isin);
            // In case the security is not present, we have to
            // a) store it for future searches
            // b) Report the creation to the user
            if (security == null)
            {
                int temp = text.indexOf("Wertpapier-Bezeichnung"); //$NON-NLS-1$
                String nameWKNLine = getNextLine(text, temp);
                String[] parts = nameWKNLine.substring(14).trim().split(" "); //$NON-NLS-1$
                String wkn = parts[0];
                StringJoiner j = new StringJoiner(" "); //$NON-NLS-1$
                for (int i = 1; i < parts.length; i++)
                    j.add(parts[i]);
                String name = j.toString();
                security = new Security(name, isin, null, QuoteFeed.MANUAL);
                security.setWkn(wkn);
                // Store
                allSecurities.add(security);
                // add to result
                SecurityItem item = new SecurityItem(security);
                results.add(item);
            }
            //The representation in the File changes with the way the account is given
            //The difference is whether or not the account is named by the IBAN
            int dateWorkOffset = 9;
            if (text.contains("Verrechnung über Konto (IBAN)")) { //$NON-NLS-1$
                dateWorkOffset = 13;
            }
            int datePos = jumpWord(text, text.indexOf("Valuta"), dateWorkOffset); //$NON-NLS-1$
            // Result Transaction
            AccountTransaction t = new AccountTransaction();
            try
            {
                Date d = df.parse(getNextWord(text, datePos));
                t.setDate(d);
            }
            catch (ParseException e)
            {
                e.printStackTrace();
                errors.add(e);
            }
            Number value = getNextNumber(text, jumpWord(text, text.indexOf("EUR", datePos), 1)); //$NON-NLS-1$
            t.setType(AccountTransaction.Type.DIVIDENDS);
            t.setAmount(Math.round(value.doubleValue() * Values.Amount.factor()));
            t.setSecurity(security);
            results.add(new TransactionItem(t));
        }
        // The buy transaction can be parsed from the name of the file
        // this requires that the user does not change the name from the
        // download
        else if (filename.contains("Wertpapierabrechnung_Kauf")) //$NON-NLS-1$
        {
            try
            {
                int tagPosition = text.indexOf("Geschäftstag"); //$NON-NLS-1$
                String tagString = getNextWord(text, getNextWhitespace(text, tagPosition));
                Date tag = df.parse(tagString);
                isinMatcher = isinPattern.matcher(text);
                String isin;
                isinMatcher.find();
                isin = isinMatcher.group();
                Security security = getSecurityForISIN(isin);
                if (security == null)
                {
                    int temp = text.indexOf("Wertpapier-Bezeichnung"); //$NON-NLS-1$
                    String nameWKNLine = getNextLine(text, temp);
                    String wkn = getLastWordInLine(nameWKNLine, 1);
                    String name = nameWKNLine.substring(0, nameWKNLine.length() - 1).trim();
                    name = name.substring(0, name.length() - wkn.length()).trim();
                    security = new Security(name, isin, null, QuoteFeed.MANUAL);
                    // Store
                    allSecurities.add(security);
                    // add to result
                    SecurityItem item = new SecurityItem(security);
                    results.add(item);
                }
                int stueckLinePos = text.indexOf("\n", text.indexOf("Zum Kurs von")); //$NON-NLS-1$ //$NON-NLS-2$
                Number stueck = getNextNumber(text, jumpWord(text, stueckLinePos, 1));
                // Fees need not be present
                // In case they are a section is present in the file
                int provPos = -1;
                provPos = text.indexOf("Provision", stueckLinePos); //$NON-NLS-1$
                BuySellEntry purchase = new BuySellEntry();
                if (provPos > 0)
                {
                    Number fee = getNextNumber(text, jumpWord(text, provPos, 3));
                    purchase.setFees(Math.round(fee.doubleValue() * Values.Amount.factor()));
                }
                int totalEURPos = text.indexOf("EUR", //$NON-NLS-1$
                                text.indexOf("EUR", text.indexOf("Zu Ihren Lasten vor Steuern")) + 3); //$NON-NLS-1$ //$NON-NLS-2$
                Number total = getNextNumber(text, jumpWord(text, totalEURPos, 1));
                purchase.setType(PortfolioTransaction.Type.BUY);
                purchase.setDate(tag);
                purchase.setSecurity(security);
                purchase.setShares(Math.round(stueck.doubleValue() * Values.Share.factor()));
                purchase.setAmount(Math.round(total.doubleValue() * Values.Amount.factor()));
                results.add(new BuySellEntryItem(purchase));
            }
            catch (ParseException e)
            {
                errors.add(e);
            }
        }
        else
        {
            errors.add(new Exception(MessageFormat.format(Messages.PDFcomdirectMsgCannotDetermineFileType, filename)));
        }
        return results;
    }

    @Override
    public String getLabel()
    {
        return Messages.PDFcomdirectLabel;
    }

    @Override
    public String getFilterExtension()
    {
        return "*.pdf"; //$NON-NLS-1$
    }

    private Security getSecurityForISIN(String isin)
    {
        for (Security sec : allSecurities)
        {
            if (sec.getIsin().equals(isin)) { return sec; }
        }
        return null;
    }

    private String getNextWord(String text, int position)
    {
        while (text.charAt(position) == ' ' || text.charAt(position) == ':')
        {
            position++;
        }
        int start = position;
        while (text.charAt(position) != ' ')
        {
            position++;
        }
        int end = position;
        return text.substring(start, end);
    }

    private int getNextWhitespace(String text, int position)
    {
        while (text.charAt(position) != ' ')
        {
            position++;
        }
        return position;
    }

    private int getNextNonWhitespace(String text, int position)
    {
        while (text.charAt(position) == ' ')
        {
            position++;
        }
        return position;
    }

    private Number getNextNumber(String text, int position)
    {
        String word = getNextWord(text, position);
        try
        {
            return format.parse(word);
        }
        catch (ParseException ignore)
        {
            return -1;
        }
    }

    private int jumpWord(String text, int position, int words)
    {
        for (int i = 0; i < words; i++)
        {
            position = getNextNonWhitespace(text, position);
            position = getNextWhitespace(text, position);
        }
        return position;
    }

    private String getLastWordInLine(String text, int position)
    {
        while (text.charAt(position) != '\n')
        {
            position++;
        }
        position--;
        while (text.charAt(position) == ' ')
        {
            position--;
        }
        String result = ""; //$NON-NLS-1$
        while (text.charAt(position) != ' ')
        {
            result = text.charAt(position) + result;
            position--;
        }
        return result;
    }

    private String getNextLine(String text, int position)
    {
        position = text.indexOf('\n', position);
        return text.substring(position + 1, text.indexOf('\n', position + 1) + 1);
    }

}

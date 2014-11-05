package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class ComdirectPDFExtractor implements Extractor
{

    PDFTextStripper stripper;
    DateFormat df;
    Pattern isinPattern;
    Matcher isinMatcher;
    NumberFormat format;
    List<Security> allSecurities;

    public ComdirectPDFExtractor(Client client)
    {
        df = new SimpleDateFormat("dd.MM.yyyy");
        isinPattern = Pattern.compile("[A-Z]{2}([A-Z0-9]){9}[0-9]");
        format = NumberFormat.getInstance(Locale.GERMANY);
        allSecurities = new ArrayList<Security>(client.getSecurities());
        try
        {
            stripper = new PDFTextStripper();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public List<Item> extract(List<File> files)
    {
        List<Item> results = new ArrayList<Item>();
        for (File f : files)
        {
            try
            {
                PDDocument doc = PDDocument.load(f);
                String text = stripper.getText(doc);
                String filename = f.toPath().getFileName().toString();
                if (text.contains("Gutschrift fälliger Wertpapier-Erträge"))
                {
                    // Thesaurierend? Do nothing!
                    if (text.contains("Ertragsthesaurierung"))
                    {
                        continue;
                    }
                    isinMatcher = isinPattern.matcher(text);
                    // Has to be used to find the security
                    String isin;
                    if (isinMatcher.find())
                    {
                        isin = isinMatcher.group();
                    }
                    else
                    {
                        throw new RuntimeException("ISIN could not be parsed");
                    }
                    int datePos = jumpWord(text, text.indexOf("Valuta"), 13);
                    Date d = df.parse(getNextWord(text, datePos));
                    Number value = getNextNumber(text, jumpWord(text, text.indexOf("EUR", datePos), 1));
                    // Result Transaction
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.INTEREST);
                    t.setDate(d);
                    t.setAmount(value.longValue()); // 1 euro = 100 cents
                    t.setNote(files.get(0).getName()); // just to test
                    t.setSecurity(getSecurityForISIN(isin));
                    // do not add to client directly -> allow user to
                    // accept/approve
                    results.add(new TransactionItem(t));
                }
                else if (filename.contains("Wertpapierabrechnung_Kauf"))
                {
                    try
                    {
                        int tagPosition = text.indexOf("Geschäftstag");
                        String tagString = getNextWord(text, getNextWhitespace(text, tagPosition));
                        Date tag = df.parse(tagString);
                        isinMatcher = isinPattern.matcher(text);
                        String isin;
                        if (isinMatcher.find())
                        {
                            isin = isinMatcher.group();
                        }
                        else
                        {
                            throw new RuntimeException("ISIN could not be parsed");
                        }
                        int stueckLinePos = text.indexOf("\n", text.indexOf("Zum Kurs von"));
                        Number stueck = getNextNumber(text, jumpWord(text, stueckLinePos, 1));
                        // Check for fees
                        int provPos = -1;
                        provPos = text.indexOf("Provision", stueckLinePos);
                        BuySellEntry purchase = new BuySellEntry();
                        if (provPos > 0)
                        {
                            Number fee = getNextNumber(text, jumpWord(text, provPos, 3));
                            purchase.setFees(Math.round(fee.doubleValue() * Values.Amount.factor()));
                        }
                        int totalEURPos = text.indexOf("EUR",
                                        text.indexOf("EUR", text.indexOf("Zu Ihren Lasten vor Steuern")) + 3);
                        Number total = getNextNumber(text, jumpWord(text, totalEURPos, 1));
                        purchase.setType(PortfolioTransaction.Type.BUY);
                        purchase.setDate(tag);
                        purchase.setSecurity(getSecurityForISIN(isin));
                        purchase.setShares(Math.round(stueck.doubleValue() * Values.Share.factor()));
                        purchase.setAmount(Math.round(total.doubleValue() * Values.Amount.factor()));
                        results.add(new BuySellEntryItem(purchase));
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    System.err.println("Could not snif type from text " + filename);
                }
                doc.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
        }
        return results;
    }

    @Override
    public String getLabel()
    {
        return "comdirect";
    }

    @Override
    public String getFilterExtension()
    {
        return "*.pdf";
    }

    private Security getSecurityForISIN(String isin)
    {
        for (Security sec : allSecurities)
        {
            if (sec.getIsin().equals(isin)) { return sec; }
        }
        throw new RuntimeException("Parsing PDF only works for Securities with known ISINs.");
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
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return -1;
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

}

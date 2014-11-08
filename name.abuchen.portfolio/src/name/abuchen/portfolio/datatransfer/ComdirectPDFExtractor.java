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

    private PDFTextStripper stripper;
    private DateFormat df;
    private Pattern isinPattern;
    private Matcher isinMatcher;
    private NumberFormat format;
    private List<Security> allSecurities;

    public ComdirectPDFExtractor(Client client)
    {
        // Parsing formats rely on german style PDFs
        df = new SimpleDateFormat("dd.MM.yyyy");
        format = NumberFormat.getInstance(Locale.GERMANY);

        isinPattern = Pattern.compile("[A-Z]{2}([A-Z0-9]){9}[0-9]");
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
    public List<Item> extract(List<File> files, List<Exception> errors)
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
                    // No cashflow, no transaction to be generated
                    if (text.contains("Ertragsthesaurierung"))
                    {
                        continue;
                    }
                    isinMatcher = isinPattern.matcher(text);
                    // Has to be used to find the security
                    String isin;
                    Security security;
                    isinMatcher.find();
                    isin = isinMatcher.group();
                    security = getSecurityForISIN(isin);
                    if (security == null)
                    {
                        int temp = text.indexOf("Wertpapier-Bezeichnung");
                        String nameWKNLine = getNextLine(text, temp);
                        String[] parts = nameWKNLine.substring(14).trim().split(" ");
                        String wkn = parts[0];
                        String name = "";
                        for (int i = 1; i < parts.length; i++)
                        {
                            name = name + parts[i] + " ";
                        }
                        name = name.trim();
                        security = new Security(name, isin, null, "MANUAL");
                        security.setWkn(wkn);
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
                    t.setSecurity(security);
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
                        isinMatcher.find();
                        isin = isinMatcher.group();
                        Security security = getSecurityForISIN(isin);
                        if (security == null)
                        {
                            int temp = text.indexOf("Wertpapier-Bezeichnung");
                            String nameWKNLine = getNextLine(text, temp);
                            String wkn = getLastWordInLine(nameWKNLine, 1);
                            String name = nameWKNLine.substring(0, nameWKNLine.length() - 1).trim();
                            name = name.substring(0, name.length() - wkn.length()).trim();
                            security = new Security(name, isin, null, "MANUAL");
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
                        purchase.setSecurity(security);
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
        String result = "";
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

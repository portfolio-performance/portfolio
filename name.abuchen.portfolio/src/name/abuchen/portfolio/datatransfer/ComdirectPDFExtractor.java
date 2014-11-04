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
                    // Datum
                    int perPosition = text.indexOf("per");
                    String datumString = text.substring(perPosition + 4, perPosition + 14);
                    Date d;
                    String eurPart = "";
                    d = df.parse(datumString);
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
                    // Loop the lines and try to find the EUR value
                    String[] lines = text.split("\r\n|\r|\n");
                    String eurLine = "";
                    boolean snap = false;
                    for (String line : lines)
                    {
                        if (snap)
                        {
                            eurLine = line;
                            break;
                        }
                        if (line.contains("Zu Ihren Gunsten vor Steuern"))
                        {
                            snap = true;
                        }
                    }
                    String[] parts = eurLine.split("EUR");
                    eurPart = parts[parts.length - 1].trim();
                    Number value = format.parse(eurPart);
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
                    int tagPosition = text.indexOf("Geschäftstag");
                    String tagString = text.substring(tagPosition + 20, tagPosition + 30);
                    try
                    {
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
                        String stText = text.substring(stueckLinePos + 6, stueckLinePos + 11);
                        Number stueck = format.parse(stText);
                        int kursEURPos = text.indexOf("EUR", stueckLinePos);
                        String kursText = text.substring(kursEURPos + 5, kursEURPos + 11);
                        Number kurs = format.parse(kursText);
                        int totalEURPos = text.indexOf("EUR", kursEURPos + 11);
                        String totalText = text.substring(totalEURPos + 18, totalEURPos + 23);
                        Number total = format.parse(totalText);
                        BuySellEntry purchase = new BuySellEntry();
                        purchase.setType(PortfolioTransaction.Type.BUY);
                        purchase.setDate(tag);
                        purchase.setSecurity(getSecurityForISIN(isin));
                        purchase.setShares(stueck.longValue() * Values.Share.factor()); // will
                                                                                        // this
                                                                                        // work
                                                                                        // for
                                                                                        // decimal
                                                                                        // shares?
                        purchase.setAmount(total.longValue() * Values.Amount.factor());
                        // TODO Fees
                        // purchase.setFees(100);
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

}

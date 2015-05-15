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
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class DABPDFExctractor implements Extractor
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY); //$NON-NLS-1$
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);

    private final PDFTextStripper textStripper;
    private final Map<String, Security> isin2security;

    public DABPDFExctractor(Client client) throws IOException
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
        return "DAB Bank"; //$NON-NLS-1$
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
            if (!text.contains("DAB Bank AG")) //$NON-NLS-1$
                throw new UnsupportedOperationException( //
                                MessageFormat.format(Messages.PDFMsgFileNotSupported, filename, "DAB Bank AG")); //$NON-NLS-1$

            List<Item> items = new ArrayList<Item>();
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            if (text.contains("Kauf")) //$NON-NLS-1$
            {
                SecurityBuyParser parser = new SecurityBuyParser(filename, lines);
                parser.parse(items);
            }
            else
            {
                throw new UnsupportedOperationException( //
                                MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, filename));
            }

            for (Item item : items)
                item.getAnnotated().setNote(filename);

            return items;
        }
        catch (ParseException | UnsupportedOperationException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    private abstract class AbstractBuySellParser
    {
        private String filename;
        private String[] lines;

        public AbstractBuySellParser(String filename, String[] lines)
        {
            this.filename = filename;
            this.lines = lines;
        }

        protected void fillInShares(BuySellEntry entry) throws ParseException
        {
            // looking for
            // Nominal Kurs
            // STK 1,4227 EUR 42,1710

            for (int ii = 0; ii < lines.length - 1; ii++)
            {
                if (lines[ii].contains("Nominal Kurs")) //$NON-NLS-1$
                {
                    // WKN
                    Pattern pattern = Pattern.compile("^STK (\\d+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$"); //$NON-NLS-1$
                    Matcher matcher = pattern.matcher(lines[ii + 1]);

                    if (matcher.matches())
                    {
                        long shares = Math.round(numberFormat.parse(matcher.group(1)).doubleValue()
                                        * Values.Share.factor());
                        entry.setShares(shares);
                        break;
                    }
                }
            }

        }

        protected void fillInAmount(BuySellEntry entry) throws ParseException
        {
            // looking for
            // Wert Konto-Nr. Betrag zu Ihren Lasten
            // 08.01.2015 8022574001 EUR 150,00

            for (int ii = 0; ii < lines.length - 1; ii++)
            {
                if (lines[ii].contains("Wert Konto-Nr. Betrag zu Ihren Lasten")) //$NON-NLS-1$
                {
                    // WKN
                    Pattern pattern = Pattern.compile("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (\\w{3}+) ([\\d.]+,\\d+)$"); //$NON-NLS-1$
                    Matcher matcher = pattern.matcher(lines[ii + 1]);

                    if (!matcher.matches())
                        continue;

                    // LocalDate date = LocalDate.parse(matcher.group(1),
                    // DATE_FORMAT);
                    // entry.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    //
                    long amount = Math.round(numberFormat.parse(matcher.group(4)).doubleValue()
                                    * Values.Amount.factor());
                    entry.setAmount(amount);
                    break;
                }
            }
        }

        protected void fillInDate(BuySellEntry entry) throws ParseException
        {
            // looking for
            // Handelstag 04.05.2015 Kurswert EUR 60,00-

            Pattern pattern = Pattern.compile("^Handelstag (\\d+.\\d+.\\d{4}+) Kurswert (\\w{3}+) ([\\d.]+,\\d+)-$"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);

                if (!matcher.matches())
                    continue;

                LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
                entry.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                break;
            }
        }

        protected void fillInSecurity(BuySellEntry entry, List<Item> items)
        {
            Security security = extractSecurity();

            if (security == null)
                throw new UnsupportedOperationException(MessageFormat.format(Messages.PDFdbMsgCannotFindSecurity,
                                filename));

            Security existing = isin2security.get(security.getIsin());
            if (existing != null)
            {
                entry.setSecurity(existing);
            }
            else
            {
                isin2security.put(security.getIsin(), security);
                items.add(new SecurityItem(security));
                entry.setSecurity(security);
            }
        }

        private Security extractSecurity()
        {
            // looking for a group of *two* lines
            // Gattungsbezeichnung ISIN
            // ARERO - Der Weltfonds Inhaber-Anteile o.N. LU0360863863

            for (int ii = 0; ii < lines.length - 1; ii++)
            {
                if (lines[ii].contains("Gattungsbezeichnung ISIN")) //$NON-NLS-1$
                {
                    // WKN
                    Pattern pattern = Pattern.compile("^(.*) ([^ ]*)$"); //$NON-NLS-1$
                    Matcher matcher = pattern.matcher(lines[ii + 1]);

                    if (!matcher.matches())
                        continue;

                    Security security = new Security();
                    security.setIsin(matcher.group(2));
                    security.setName(matcher.group(1));
                    security.setFeed(QuoteFeed.MANUAL);
                    return security;
                }
            }

            return null;
        }
    }

    private class SecurityBuyParser extends AbstractBuySellParser
    {
        public SecurityBuyParser(String filename, String[] lines)
        {
            super(filename, lines);
        }

        private void parse(List<Item> items) throws ParseException
        {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);

            fillInSecurity(entry, items);
            fillInAmount(entry);
            fillInDate(entry);
            fillInShares(entry);

            items.add(new BuySellEntryItem(entry));
        }
    }
}

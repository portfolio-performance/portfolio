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
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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

            List<Item> items = new ArrayList<Item>();
            String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

            if (text.contains("Ertragsgutschrift")) //$NON-NLS-1$
            {
                DividendParser parser = new DividendParser(filename, lines);
                parser.parse(items);
            }
            else if (text.contains("Kauf von Wertpapieren")) //$NON-NLS-1$
            {
                SecurityBuyParser parser = new SecurityBuyParser(filename, lines);
                parser.parse(items);
            }
            else if (text.contains("Verkauf von Wertpapieren")) //$NON-NLS-1$
            {
                SecuritySellParser parser = new SecuritySellParser(filename, lines);
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

    private class DividendParser
    {
        private String filename;
        private String[] lines;

        public DividendParser(String filename, String[] lines)
        {
            this.filename = filename;
            this.lines = lines;
        }

        private void parse(List<Item> items) throws ParseException
        {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);

            fillInSecurity(transaction, items);
            fillInDateAndAmount(transaction);
            fillInShares(transaction);

            items.add(new TransactionItem(transaction));
        }

        private void fillInDateAndAmount(AccountTransaction transaction) throws ParseException
        {
            // looking for:
            // Gutschrift mit Wert 15.12.2014 14,95 EUR

            Pattern pattern = Pattern.compile("Gutschrift mit Wert (\\d+.\\d+.\\d{4}+) ([\\d.]+,\\d+) (\\w{3}+)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
                    transaction.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                    long amount = Math.round(numberFormat.parse(matcher.group(2)).doubleValue()
                                    * Values.Amount.factor());
                    transaction.setAmount(amount);

                    break;
                }
            }
        }

        private void fillInShares(AccountTransaction transaction) throws ParseException
        {
            Pattern pattern = Pattern.compile("(\\d+,\\d*) (\\S*) (\\S*)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    long shares = Math
                                    .round(numberFormat.parse(matcher.group(0)).doubleValue() * Values.Share.factor());
                    transaction.setShares(shares);

                    break;
                }
            }
        }

        private void fillInSecurity(AccountTransaction transaction, List<Item> items)
        {
            Security security = extractSecurity();

            if (security == null)
                throw new UnsupportedOperationException(MessageFormat.format(Messages.PDFdbMsgCannotFindSecurity,
                                filename));

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

        private Security extractSecurity()
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

    private abstract class AbstractBuySellParser
    {
        private String filename;
        private String[] lines;

        public AbstractBuySellParser(String filename, String[] lines)
        {
            this.filename = filename;
            this.lines = lines;
        }

        protected void fillInTaxes(BuySellEntry entry) throws ParseException
        {
            // looking for
            // Kapitalertragsteuer EUR -122,94
            // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76

            Pattern pattern = Pattern.compile("Kapitalertragsteuer (\\w{3}+) ([\\d.-]+,\\d+)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    long taxes = Math.abs(Math.round(numberFormat.parse(matcher.group(2)).doubleValue()
                                    * Values.Amount.factor()));
                    entry.setTaxes(taxes);
                    break;
                }
            }

            pattern = Pattern.compile("Solidaritätszuschlag auf Kapitalertragsteuer (\\w{3}+) ([\\d.-]+,\\d+)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    long taxes = Math.abs(Math.round(numberFormat.parse(matcher.group(2)).doubleValue()
                                    * Values.Amount.factor()));
                    entry.setTaxes(entry.getPortfolioTransaction().getTaxes() + taxes);
                    break;
                }
            }
        }

        protected void fillInFees(BuySellEntry entry) throws ParseException
        {
            // looking for
            // Kurswert EUR 665,00

            Pattern pattern = Pattern.compile("Kurswert (\\w{3}+) ([\\d.]+,\\d+)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    long marketValue = Math.round(numberFormat.parse(matcher.group(2)).doubleValue()
                                    * Values.Amount.factor());
                    long totalAmount = entry.getPortfolioTransaction().getAmount();
                    long taxes = entry.getPortfolioTransaction().getTaxes();

                    switch (entry.getPortfolioTransaction().getType())
                    {
                        case BUY:
                            entry.setFees(totalAmount - taxes - marketValue);
                            break;
                        case SELL:
                            entry.setFees(marketValue - taxes - totalAmount);
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }

                    break;
                }
            }
        }

        protected void fillInShares(BuySellEntry entry) throws ParseException
        {
            // looking for
            // WKN BASF11 Nominal ST 19

            Pattern pattern = Pattern.compile("^WKN [^ ]* Nominal ST (\\d+(,\\d+)?)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    long shares = Math
                                    .round(numberFormat.parse(matcher.group(1)).doubleValue() * Values.Share.factor());
                    entry.setShares(shares);
                    break;
                }
            }
        }

        protected void fillInDateAndAmount(BuySellEntry entry) throws ParseException
        {
            // looking for
            // @formatter:off
            // Buchung auf Kontonummer 1234567 40 mit Wertstellung 08.04.2015 EUR 675,50
            // @formatter:on

            Pattern pattern = Pattern
                            .compile("Buchung auf Kontonummer [\\d ]* mit Wertstellung (\\d+.\\d+.\\d{4}+) (\\w{3}+) ([\\d.]+,\\d+)"); //$NON-NLS-1$
            for (int ii = 0; ii < lines.length; ii++)
            {
                Matcher matcher = pattern.matcher(lines[ii]);
                if (matcher.matches())
                {
                    LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
                    entry.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                    long amount = Math.round(numberFormat.parse(matcher.group(3)).doubleValue()
                                    * Values.Amount.factor());
                    entry.setAmount(amount);

                    break;
                }
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
            // looking for a group of *four* lines
            // Filialnummer Depotnummer Wertpapierbezeichnung Seite
            // 123 1234567 00 BASF SE
            // WKN BASF11 Nominal ST 19
            // ISIN DE000BASF111 Kurs EUR 35,00

            for (int ii = 0; ii < lines.length - 3; ii++)
            {
                if (lines[ii].contains("Wertpapierbezeichnung")) //$NON-NLS-1$
                {
                    // name (skip account number)
                    String name = lines[ii + 1].substring(15);

                    // WKN
                    Pattern pattern = Pattern.compile("^WKN ([^ ]*) (.*)$"); //$NON-NLS-1$
                    Matcher matcher = pattern.matcher(lines[ii + 2]);
                    String wkn = matcher.matches() ? matcher.group(1) : null;

                    // ISIN
                    pattern = Pattern.compile("^ISIN ([^ ]*) (.*)$"); //$NON-NLS-1$
                    matcher = pattern.matcher(lines[ii + 3]);
                    String isin = matcher.matches() ? matcher.group(1) : null;

                    Security security = new Security();
                    security.setIsin(isin);
                    security.setWkn(wkn);
                    security.setName(name);
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
            fillInDateAndAmount(entry);
            fillInShares(entry);
            fillInFees(entry);

            items.add(new BuySellEntryItem(entry));
        }
    }

    private class SecuritySellParser extends AbstractBuySellParser
    {
        public SecuritySellParser(String filename, String[] lines)
        {
            super(filename, lines);
        }

        private void parse(List<Item> items) throws ParseException
        {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);

            fillInSecurity(entry, items);
            fillInDateAndAmount(entry);
            fillInShares(entry);
            fillInTaxes(entry);
            fillInFees(entry);

            items.add(new BuySellEntryItem(entry));
        }
    }

}

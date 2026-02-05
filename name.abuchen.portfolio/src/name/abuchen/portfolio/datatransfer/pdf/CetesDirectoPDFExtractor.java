package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.AdditionalLocales;

/**
 * @formatter:off
 * @implNote Cetesdirecto is a Mexican government program.
 *           The currency used is MXN (Mexican Peso) represented as "$".
 *
 * @implSpec All security currencies are in MXN.
 *           The ID number corresponds to the WKN (Wertpapierkennnummer) as the ISIN is not provided.
 *           The security name is the product name followed by the series.
 *
 * @implNote The PDF file contains the following details:
 *           - Transaction date
 *           - Settlement date
 *           - ID number
 *           - Transaction type (e.g., COMPRA for purchase, AMORTIZACION for amortization, COMPSI for dividends)
 *           - Product name
 *           - Series
 *           - Number of shares
 *           - Price
 *           - Term (duration of the investment)
 *           - Interest rate (rate of return)
 *           - Charge (any fees related to the transaction)
 *           - Credit (any credits applied to the account)
 *           - Effective balance (final balance after transaction)
 *
 * Example:
 *
 * | Transaction Date | Settlement Date | ID Number     | Type    | Product | Series | Shares | Price  | Term | Rate  | Charge    | Credit | Balance    |
 * |------------------|-----------------|---------------|---------|---------|--------|--------|--------|------|-------|-----------|--------|------------|
 * | 04/01/22         | 06/01/22        | SVD147529623  | COMPRA  | CETES   | 220203 | 6,080  | 9.9573 | 2    | 5.51% | 60,540.38 | 0.00   | -60,540.10 |
 *
 * @formatter:on
 */
@SuppressWarnings("nls")
public class CetesDirectoPDFExtractor extends AbstractPDFExtractor
{
    private static final String MXN = "MXN";

    public CetesDirectoPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("contacto@cetesdirecto.com");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Cetesdirecto";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Movimientos del per", (context, lines) -> { //
            var pTaxAmountTransaction = Pattern.compile("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<wkn>[A-Z]{3}[0-9]{9})ISR.* (?<tax>[\\.,\\d]+)$");

            var taxAmountTransactionHelper = new TaxAmountTransactionHelper();
            context.putType(taxAmountTransactionHelper);

            List<TaxAmountTransactionItem> itemsToAddToFront = new ArrayList<>();

            // Add items from pTaxAmountTransaction to the beginning of the list
            taxAmountTransactionHelper.items.addAll(0, itemsToAddToFront);

            for (var i = 0; i < lines.length; i++)
            {
                var m = pTaxAmountTransaction.matcher(lines[i]);
                if (m.matches())
                {
                    var item = new TaxAmountTransactionItem();
                    item.line = i + 1;
                    item.dateTime = asDate(m.group("date"), AdditionalLocales.MEXICO);
                    item.wkn = m.group("wkn");
                    item.tax = asAmount(m.group("tax"));
                    item.currency = asCurrencyCode(MXN);

                    taxAmountTransactionHelper.items.add(item);
                }
            }
        });

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z]{3}[0-9]{9}(COMPRA|AMORTIZACION) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "AMORTIZACION" change from BUY to SELL
                        // @formatter:on
                        .section("type").optional() //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z]{3}[0-9]{9}(?<type>(COMPRA|AMORTIZACION)) .*$") //
                        .assign((t, v) -> {
                            if ("AMORTIZACION".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf(
                                        // @formatter:off
                                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "wkn", "name", "name1", "shares", "note1", "note2", "amount") //
                                                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) " //
                                                                        + "[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} " //
                                                                        + "(?<wkn>[A-Z]{3}[0-9]{9})" //
                                                                        + "COMPRA " //
                                                                        + "(?<name>.*) " //
                                                                        + "(?<name1>.*) " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "(?<note1>[\\d]+) " //
                                                                        + "(?<note2>[\\.,\\d]+) " //
                                                                        + "(?<amount>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode(MXN));
                                                            v.put("name", trim(v.get("name")) + " (" + trim(v.get("name1")) + ")");
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), AdditionalLocales.MEXICO));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setCurrencyCode(asCurrencyCode(MXN));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote("Term: " + v.get("note1") + " | Rate: " + v.get("note2"));
                                                        }),
                                        // @formatter:off
                                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "wkn", "name", "name1", "shares", "amount") //
                                                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) " //
                                                                        + "[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} " //
                                                                        + "(?<wkn>[A-Z]{3}[0-9]{9})" //
                                                                        + "AMORTIZACION " //
                                                                        + "(?<name>.*) " + "(?<name1>.*) " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<amount>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();
                                                            var amount = Money.of(asCurrencyCode(MXN), asAmount(v.get("amount")));

                                                            v.put("currency", asCurrencyCode(MXN));
                                                            v.put("name", trim(v.get("name")) + " (" + trim(v.get("name1")) + ")");
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), AdditionalLocales.MEXICO));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setMonetaryAmount(amount);

                                                            var taxAmountTransactionHelper = context.getType(TaxAmountTransactionHelper.class).orElseGet(TaxAmountTransactionHelper::new);
                                                            var item = taxAmountTransactionHelper.findItem(v.getStartLineNumber(), t.getPortfolioTransaction().getDateTime(), t.getPortfolioTransaction().getSecurity().getWkn());

                                                            if (item.isPresent())
                                                            {
                                                                var tax = Money.of(item.get().currency, item.get().tax);
                                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));

                                                                checkAndSetTax(tax, t, type.getCurrentContext());
                                                            }
                                                        }))

                        .wrap(BuySellEntryItem::new);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Movimientos del per");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z]{3}[0-9]{9}COMPSI .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
                        // @formatter:on
                        .section("date", "wkn", "name", "name1", "shares", "amount") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} " //
                                        + "(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) " //
                                        + "(?<wkn>[A-Z]{3}[0-9]{9})" //
                                        + "COMPSI " //
                                        + "(?<name>.*) " //
                                        + "(?<name1>.*) " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\d]+ [\\.,\\d]+ " //
                                        + "(?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(MXN));
                            v.put("name", trim(v.get("name")) + " (" + trim(v.get("name1")) + ")");
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date"), AdditionalLocales.MEXICO));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private static class TaxAmountTransactionHelper
    {
        private List<TaxAmountTransactionItem> items = new ArrayList<>();

        /**
         * Finds a TaxAmountTransactionItem in the list that has a line number
         * greater than or equal to the specified line and matches the given
         * date and WKN.
         *
         * @param line
         *            The line number to compare against.
         * @param dateTime
         *            The date and time to match.
         * @param wkn
         *            The WKN (Wertpapierkennnummer) to match.
         * @return An Optional containing the found item, or an empty Optional
         *         if no match is found.
         */
        public Optional<TaxAmountTransactionItem> findItem(int line, LocalDateTime dateTime, String wkn)
        {
            for (TaxAmountTransactionItem item : items)
            {
                // Skip items where the line number is less than the specified
                // line
                if (item.line < line)
                    continue;

                // Check for the matching dateTime and WKN
                if (item.dateTime.equals(dateTime) && item.wkn.equals(wkn))
                    return Optional.of(item);
            }
            return Optional.empty();
        }
    }

    private static class TaxAmountTransactionItem
    {
        int line;

        LocalDateTime dateTime;
        String wkn;
        String currency;
        long tax;

        @Override
        public String toString()
        {
            return "TaxAmountTransactionItem [line=" + line + ", dateTime=" + dateTime + ", wkn=" + wkn + ", currency=" + currency + ", tax=" + tax + "]";
        }
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "es", "MX");
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;

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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BourseDirectPDFExtractor extends AbstractPDFExtractor
{
    public BourseDirectPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bourse Direct");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bourse Direct";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Avis d.Op.ration", (context, lines) -> {
            var pTaxAmountTransaction = Pattern.compile("^.*(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})[\\s]+TAXE TRANSACT FINANCIERES[\\s]+(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]+TTF[\\s]+(?<tax>[\\d]+,[\\d]{2})$");

            var taxAmountTransactionHelper = new TaxAmountTransactionHelper();
            context.putType(taxAmountTransactionHelper);

            List<TaxAmountTransactionItem> itemsToAddToFront = new ArrayList<>();

            // Add items from pTaxAmountTransaction to the beginning of the list
            taxAmountTransactionHelper.items.addAll(0, itemsToAddToFront);

            for (var i = 0; i < lines.length; i++)
            {
                // Extract currency from header line
                if (lines[i].matches("^Date D.signation D.bit \\((?<currency>\\p{Sc})\\).*$"))
                {
                    var currencyMatcher = Pattern.compile("^Date D.signation D.bit \\((?<currency>\\p{Sc})\\).*$").matcher(lines[i]);
                    if (currencyMatcher.matches())
                    {
                        context.put("currency", asCurrencyCode(currencyMatcher.group("currency")));
                    }
                }

                var m = pTaxAmountTransaction.matcher(lines[i]);
                if (m.matches())
                {
                    var item = new TaxAmountTransactionItem();
                    item.line = i + 1;
                    item.dateTime = asDate(m.group("date"));
                    item.isin = m.group("isin");
                    item.tax = asAmount(m.group("tax"));
                    item.currency = asCurrencyCode(context.get("currency"));

                    taxAmountTransactionHelper.items.add(item);
                }
            }
        });

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}ACHAT.*$", "^.*Heure Execution:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        //  10/12/2024  ACHAT COMPTANT  FR0011550185  BNPP S&P500EUR ETF  4 978,30
                        //  05/08/2025  ACHAT ETRANGER  IE00B4BNMY34  ACCENTURE CL.A  216,93
                        // @formatter:on
                        .section("isin", "name") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}ACHAT.*[\\s]{1,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]{1,}(?<name>.*)[\\s]{2,}[\\d\\s]+,[\\d]{2}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // QUANTITE :  +173
                        // @formatter:on
                        .section("shares") //
                        .match("^.*QUANTITE :[\\s]{1,}\\+(?<shares>[\\d\\s]+(,[\\d]{2})?)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        //  10/12/2024  ACHAT COMPTANT  FR0011550185  BNPP S&P500EUR ETF  4 978,30
                        // Heure Execution: 09:04:28       Lieu: EURONEXT - EURONEXT PARIS
                        // @formatter:on
                        .section("date", "time") //
                        .match("^.*(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})[\\s]{1,}ACHAT.*$") //
                        .match("^.*Heure Execution: (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        //  10/12/2024  ACHAT COMPTANT  FR0011550185  BNPP S&P500EUR ETF  4 978,30
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}ACHAT.*[\\s]{2,}(?<amount>[\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            var taxAmountTransactionHelper = type.getCurrentContext().getType(TaxAmountTransactionHelper.class).orElseGet(TaxAmountTransactionHelper::new);
                            var item = taxAmountTransactionHelper.findItem(v.getStartLineNumber(), t.getPortfolioTransaction().getDateTime(), t.getPortfolioTransaction().getSecurity().getIsin());

                            if (item.isPresent())
                            {
                                var tax = Money.of(item.get().currency, item.get().tax);
                                checkAndSetTax(tax, t, type.getCurrentContext());
                            }
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Avis d.Op.ration", //
                        documentContext -> documentContext //
                        // @formatter:off
                        // Date Désignation Débit (€) Crédit (€)
                        // @formatter:on
                                        .section("currency") //
                                        .match("^Date D.signation D.bit \\((?<currency>\\p{Sc})\\).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}COUPONS.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        //   06/08/2025  COUPONS  NL0010273215  ASML HOLDING  4,08
                        // @formatter:on
                        .section("isin", "name") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}COUPONS[\\s]{1,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]{1,}(?<name>.*)[\\s]{2,}[\\d\\s]+,[\\d]{2}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        //  QUANTITE :  -3
                        // @formatter:on
                        .section("shares") //
                        .match("^.*QUANTITE :[\\s]{1,}\\-(?<shares>[\\d\\s]+(,[\\d]{2})?)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        //  06/08/2025  COUPONS  NL0010273215  ASML HOLDING  4,08
                        // Heure Execution: 09:04:28       Lieu: EURONEXT - EURONEXT PARIS
                        // @formatter:on
                        .section("date") //
                        .match("^.*(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})[\\s]{1,}COUPONS.*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        //  06/08/2025  COUPONS  NL0010273215  ASML HOLDING  4,08
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}COUPONS.*[\\s]{2,}(?<amount>[\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        //  06/08/2025  COUPONS  NL0010273215  ASML HOLDING  4,08
                        //  PX UN.BRUT :  +1,60072  BRUT :  +4,80
                        // @formatter:on
                        .section("amount", "gross").optional() //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}[\\s]{1,}COUPONS.*[\\s]{2,}(?<amount>[\\d\\s]+,[\\d]{2})$") //
                        .match("^.*PX UN.BRUT :[\\s]{1,}\\+[\\d\\s]+,[\\d]+[\\s]{1,}BRUT :[\\s]{1,}\\+(?<gross>[\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            var amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                            // @formatter:off
                            // we assume that the difference between gross and amount is tax
                            // @formatter:on
                            t.addUnit(new Unit(Unit.Type.TAX, gross.subtract(amount)));
                        })

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // COURTAGE :  +4,48  TVA :  +0,00
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^.*COURTAGE :[\\s]{1,}\\+(?<fee>[\\d\\s]+,[\\d]{2})[\\s]{1,}TVA :[\\s]{1,}\\+[\\d\\s]+,[\\d]{2}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private static class TaxAmountTransactionHelper
    {
        private List<TaxAmountTransactionItem> items = new ArrayList<>();

        /**
         * Finds a TaxAmountTransactionItem in the list that has a line number
         * greater than or equal to the specified line and matches the given
         * date and ISIN.
         *
         * @param line
         *            The line number to compare against.
         * @param date
         *            The date to match.
         * @param isin
         *            The ISIN to match.
         * @return An Optional containing the matching TaxAmountTransactionItem,
         *         or empty if no match is found.
         */
        public Optional<TaxAmountTransactionItem> findItem(int line, LocalDateTime date, String isin)
        {
            return items.stream()
                            .filter(item -> item.line >= line && item.dateTime.toLocalDate().isEqual(date.toLocalDate()) && item.isin.equals(isin))
                            .findFirst();
        }
    }

    private static class TaxAmountTransactionItem
    {
        public int line;
        public LocalDateTime dateTime;
        public String isin;
        public long tax;
        public String currency;

        @Override
        public String toString()
        {
            return "TaxAmountTransactionItem [line=" + line + ", dateTime=" + dateTime + ", isin=" + isin + ", currency=" + currency + ", tax=" + tax + "]";
        }
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "fr", "FR");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "fr", "FR");
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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

/**
 * @formatter:off
 * @implNote Trading 212 is a trading name of FXFlat Bank GmbH.
 *
 *           The "Aktivitätsauszug" (activity statement) is a daily statement
 *           listing executed orders, deposits, card purchases and interest on
 *           cash. All amounts are formatted in US number format.
 *
 *           The "Handelsbestätigung" (trade confirmation) is issued by
 *           Trading 212 EU GmbH per execution of an order. It exists in two
 *           variants: Format01 with US number format and leading currency
 *           symbol (€50.00), Format02 with German number format and trailing
 *           currency symbol (21,55 €).
 * @formatter:on
 */
@SuppressWarnings("nls")
public class Trading212PDFExtractor extends AbstractPDFExtractor
{
    public Trading212PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Trading 212");

        addBuySellTransaction();
        addActivityStatementTransactions();
    }

    @Override
    public String getLabel()
    {
        return "Trading 212";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Handelsbest.tigung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Auftragsdetails$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkaufen" change from BUY to SELL
                        .section("type") //
                        .match("^Anweisung (?<type>(Kaufen|Verkaufen))$") //
                        .assign((t, v) -> {
                            if ("Verkaufen".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // Only "€" and "$" are supported. Other currency symbols
                        // such as "£" are ambiguous and fail the import.
                        .oneOf( //
                                        // Format01 (US number format) is only supported
                                        // without currency conversion.
                                        // @formatter:off
                                        // Wertpapier Tencent
                                        // Symbol NNND
                                        // ISIN KYG875721634
                                        // Ausführungskurs €54.09
                                        // Wechselkurs 1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Ausf.hrungskurs (?<currency>€)[\\.,\\d]+$") //
                                                        .match("^Wechselkurs 1$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wertpapier Veea
                                        // Symbol VEEA
                                        // ISIN US6934892059
                                        // Ausführungskurs 4,99 $
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[€\\$])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Format01 (US number format)
                                        // Anzahl 0.92438528
                                        // Ausführungskurs €54.09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Anzahl (?<shares>[\\.,\\d]+)$") //
                                                        .match("^Ausf.hrungskurs \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Format02 (German number format)
                                        // Anzahl 0,712562
                                        // Ausführungskurs 33,83 $
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Anzahl (?<shares>[\\.,\\d]+)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "de", "DE"))))

                        // @formatter:off
                        // Ausführungszeitpunkt 15 Apr 2026, 06:07:41 (UTC)
                        // Ausführungszeitpunkt 05 Okt 2026, 16:41:38 (UTC)
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Ausf.hrungszeitpunkt (?<date>[\\d]{1,2} [\\p{L}]{3,4} [\\d]{4}), (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // The negative "Kosten" line on the "Rendite" page of a
                        // sale is the cost basis and must not be matched.
                        .oneOf( //
                                        // @formatter:off
                                        // Format01 (US number format)
                                        // Kosten €50.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Kosten (?<currency>€)(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Format02 (German number format)
                                        // Kosten 21,55 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Kosten (?<amount>[\\.,\\d]+) (?<currency>€)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount"), "de", "DE"));
                                                        }),
                                        // @formatter:off
                                        // Format02 (German number format)
                                        // Verkaufserlös vor Steuern 88,94 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Verkaufserl.s vor Steuern (?<amount>[\\.,\\d]+) (?<currency>€)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount"), "de", "DE"));
                                                        }))

                        // @formatter:off
                        // Wert 99,80 $
                        // Wechselkurs 1,12046704
                        // @formatter:on
                        .section("fxGross", "termCurrency", "exchangeRate").optional() //
                        .match("^Wert (?<fxGross>[\\.,\\d]+) (?<termCurrency>[€\\$])$") //
                        .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var baseCurrency = t.getPortfolioTransaction().getCurrencyCode();
                            var rate = new ExtrExchangeRate(asExchangeRate(v.get("exchangeRate")), //
                                            baseCurrency, asCurrencyCode(v.get("termCurrency")));
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross"), "de", "DE"));
                            var gross = rate.convert(baseCurrency, fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftrags-ID-Nr. 76880828286
                        // @formatter:on
                        .section("note") //
                        .match("^Auftrags\\-ID\\-Nr\\. (?<note>[\\d]+)$") //
                        .assign((t, v) -> t.setNote("Auftrags-ID-Nr.: " + v.get("note")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        addTaxReturnBlock(type);
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Auftragsdetails$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        .oneOf( //
                                        // @formatter:off
                                        // Wertpapier Tencent
                                        // Symbol NNND
                                        // ISIN KYG875721634
                                        // Ausführungskurs €54.09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Ausf.hrungskurs (?<currency>€)[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wertpapier PDS Biotechnology
                                        // Symbol PDSB
                                        // ISIN US70465T1079
                                        // Ausführungskurs 1,26 $
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[€\\$])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Ausführungszeitpunkt 05 Okt 2026, 17:02:31 (UTC)
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Ausf.hrungszeitpunkt (?<date>[\\d]{1,2} [\\p{L}]{3,4} [\\d]{4}), (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                        // A positive tax correction on a sale is a tax refund
                        // (loss offsetting) booked as a separate transaction.
                        // The exchange rate is required for the gross value of
                        // a security in foreign currency.
                        // @formatter:off
                        // Wert 126,00 $
                        // Wechselkurs 1,12029874
                        // Steuerkorrektur 0,09 €
                        // @formatter:on
                        .section("termCurrency", "exchangeRate", "amount", "currency").optional() //
                        .match("^Wert [\\.,\\d]+ (?<termCurrency>[€\\$])$") //
                        .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Steuerkorrektur (?<amount>[\\.,\\d]+) (?<currency>€)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), "de", "DE"));

                            var rate = new ExtrExchangeRate(asExchangeRate(v.get("exchangeRate")), //
                                            t.getCurrencyCode(), asCurrencyCode(v.get("termCurrency")));
                            type.getCurrentContext().putType(rate);

                            var gross = t.getMonetaryAmount();
                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftrags-ID-Nr. 58419967594
                        // @formatter:on
                        .section("note") //
                        .match("^Auftrags\\-ID\\-Nr\\. (?<note>[\\d]+)$") //
                        .assign((t, v) -> t.setNote("Auftrags-ID-Nr.: " + v.get("note")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            return null;
                        });
    }

    private void addTaxesSectionsTransaction(Transaction<BuySellEntry> transaction, DocumentType type)
    {
        transaction //

                        // Capital gains tax withheld on a sale. The amount is
                        // taken before taxes, so the tax is deducted from it.
                        // @formatter:off
                        // Steuern -0,21 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Steuern \\-(?<tax>[\\.,\\d]+) (?<currency>€)$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"), "de", "DE"));

                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                            checkAndSetTax(tax, t, type.getCurrentContext());
                        });
    }

    private void addFeesSectionsTransaction(Transaction<BuySellEntry> transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // FX Gebühr 0,03 €
                        // FX Gebühr -0,13 €
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^FX Geb.hr (\\-)?(?<fee>[\\.,\\d]+) (?<currency>€)$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"), "de", "DE"));
                            checkAndSetFee(fee, t, type.getCurrentContext());
                        });
    }

    private void addActivityStatementTransactions()
    {
        final var type = new DocumentType("Aktivit.tsauszug");
        this.addDocumentTyp(type);

        // @formatter:off
        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
        // 2026-01-06 08:12:29 VWCE IE00BK5BQT80 EUR 44466988302 Verkaufen 1.73397226 147.02 254.9286 Market OTC Reguläre Zeiten 1 EUR - - -0.07 254.93 0.01
        // 2026-01-07 11:34:48 EUNL IE00B4L5Y983 EUR 44551706884 Verkaufen 1.00035279 113.38 113.42 Market OTC Reguläre Zeiten 1 EUR - - 0.82 113.42 -0.14
        // 2026-01-13 13:45:41 CTP2 US20030N1019 EUR 44853850736 Kaufen 0.04070231 25.06 1.02 Market OTC Reguläre Zeiten 1 EUR - - - 1.02
        // @formatter:on
        var buySellBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} "
                        + "[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? [A-Z]{2}[A-Z0-9]{9}[0-9] [A-Z]{3} [\\d]+ (Kaufen|Verkaufen) .*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkaufen" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.* [A-Z]{3} [\\d]+ (?<type>(Kaufen|Verkaufen)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkaufen".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
                        // @formatter:on
                        .section("tickerSymbol", "isin", "currency") //
                        .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} "
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<currency>[A-Z]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
                        // @formatter:on
                        .section("shares") //
                        .match("^.* (Kaufen|Verkaufen) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // The columns following the transaction currency are:
                        // FX GEBÜHR | GEBÜHREN DER HANDELSPLATTFORM & STEUERN | RENDITE | WERT | STEUERN
                        //
                        // The two fee columns are matched as a literal "-": no sample document carries a
                        // value in them, and it is unknown whether the WERT column would then be gross or
                        // net of those fees. A statement carrying one fails the import instead of silently
                        // importing an understated total.
                        // @formatter:on
                        .oneOf( //
                                        // @formatter:off
                                        // With trailing tax column:
                                        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
                                        // 2026-01-06 08:12:29 VWCE IE00BK5BQT80 EUR 44466988302 Verkaufen 1.73397226 147.02 254.9286 Market OTC Reguläre Zeiten 1 EUR - - -0.07 254.93 0.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^.* (?<currency>[A-Z]{3}) \\- \\- (\\-|\\-?[\\.,\\d]+) "
                                                                        + "(?<amount>[\\.,\\d]+) (\\-|\\-?[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Without trailing tax column:
                                        // 2026-01-13 13:45:41 CTP2 US20030N1019 EUR 44853850736 Kaufen 0.04070231 25.06 1.02 Market OTC Reguläre Zeiten 1 EUR - - - 1.02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^.* (?<currency>[A-Z]{3}) \\- \\- (\\-|\\-?[\\.,\\d]+) "
                                                                        + "(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // A negative value in the trailing tax column is capital gains tax
                        // withheld on the sale. The WERT column is the gross value, so the
                        // tax is deducted from the total amount.
                        // 2026-01-07 11:34:48 EUNL IE00B4L5Y983 EUR 44551706884 Verkaufen 1.00035279 113.38 113.42 Market OTC Reguläre Zeiten 1 EUR - - 0.82 113.42 -0.14
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.* Verkaufen .* (?<currency>[A-Z]{3}) \\- \\- (\\-|\\-?[\\.,\\d]+) "
                                        + "[\\.,\\d]+ \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                            ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 2026-01-06 08:04:56 VWCE IE00BK5BQT80 EUR 44501427897 Kaufen 0.03398586 147.12 5 Market OTC Reguläre Zeiten 1 EUR - - - 5 -
                        // @formatter:on
                        .section("note") //
                        .match("^.* [A-Z]{3} (?<note>[\\d]+) (Kaufen|Verkaufen) .*$") //
                        .assign((t, v) -> t.setNote("Auftrags-ID-Nr.: " + v.get("note")))

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // A positive value in the trailing tax column of a sale is a tax refund
        // (loss offsetting) credited to the account as a separate transaction.
        // 2026-01-06 08:12:29 VWCE IE00BK5BQT80 EUR 44466988302 Verkaufen 1.73397226 147.02 254.9286 Market OTC Reguläre Zeiten 1 EUR - - -0.07 254.93 0.01
        // @formatter:on
        var taxRefundBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} "
                        + "[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? [A-Z]{2}[A-Z0-9]{9}[0-9] [A-Z]{3} [\\d]+ Verkaufen .* "
                        + "[A-Z]{3} \\- \\- (\\-|\\-?[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        .section("date", "time", "tickerSymbol", "isin", "currency", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) "
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<currency>[A-Z]{3}) (?<note>[\\d]+) Verkaufen .* "
                                        + "[A-Z]{3} \\- \\- (\\-|\\-?[\\.,\\d]+) [\\.,\\d]+ (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Auftrags-ID-Nr.: " + v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2026-01-06 02:10:08 Verzinsung von Geld EUR 1.83 -0.48 €-0.48 1.35
        // @formatter:on
        var interestBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} Verzinsung von Geld [A-Z]{3} .*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "time", "currency", "tax", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) Verzinsung von Geld "
                                        + "(?<currency>[A-Z]{3}) [\\.,\\d]+ \\-(?<tax>[\\.,\\d]+) \\p{Sc}\\-[\\.,\\d]+ (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2026-01-07 11:30:16 JP Morgan Bankeinzahlung EUR 22.33 - - 22.33
        // 2026-01-12 22:15:23 JP Morgan Bankeinzahlung EUR 0.39 €0.39
        // @formatter:on
        var depositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .*Bankeinzahlung [A-Z]{3} .*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .oneOf( //
                                        // @formatter:off
                                        // 2026-01-07 11:30:16 JP Morgan Bankeinzahlung EUR 22.33 - - 22.33
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time", "note", "currency", "amount") //
                                                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) (?<note>.*Bankeinzahlung) "
                                                                        + "(?<currency>[A-Z]{3}) [\\.,\\d]+ \\- \\- (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 2026-01-12 22:15:23 JP Morgan Bankeinzahlung EUR 0.39 €0.39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time", "note", "currency", "amount") //
                                                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) (?<note>.*Bankeinzahlung) "
                                                                        + "(?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2026-01-07 01:32:07 Kartenkauf EUR -57.35 - - -57.35
        // @formatter:on
        var removalBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} Kartenkauf [A-Z]{3} .*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "time", "currency", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) Kartenkauf "
                                        + "(?<currency>[A-Z]{3}) \\-[\\.,\\d]+ \\- \\- \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Kartenkauf");
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }
}

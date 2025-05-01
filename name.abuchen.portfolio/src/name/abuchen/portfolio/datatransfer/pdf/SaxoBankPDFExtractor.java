package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class SaxoBankPDFExtractor extends AbstractPDFExtractor
{
    public SaxoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Saxo Bank");

        addBuySellTransaction();
        addDepositTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Saxo Bank A/S";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Trade details,", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung: CHF 05-Dez-2024 - 05-Dez-2024
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung: (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Instrument .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // ISIN IE00B4L5Y983 Valuta 09-Dez-2024
                                        // Symbol SWDA:xswx Order-ID 5236807355
                                        // Ordertyp Marktorder Gehandelter Wert -5.480,43 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "tickerSymbol", "currency") //
                                                        .match("^Instrument (?<name>.*) Handelszeit.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valuta.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^Ordertyp .* \\-[\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // ISIN IE00B4L5Y983 Valuta 09-Dez-2024
                                        // Symbol SWDA:xswx Order-ID 5236807355
                                        // Ordertyp Marktorder Preis 111,8455 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "tickerSymbol", "currency") //
                                                        .match("^Instrument (?<name>.*) Handelszeit.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valuta.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^Ordertyp .* [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Eröffnung/Schluss To-Open Menge 49,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Er.ffnung\\/Schluss To-Open Menge (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // K/V Kauf Menge 49,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^K\\/V Kauf Menge (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^.*Handelszeit (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nettobetrag - - - - - -12,14 -4.869,43
                                        // Nettobetrag - - - - - 0,00 -3.057,58
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^Nettobetrag .* \\-[\\s]*[\\.,\\d]+ \\-(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // ID USD CHF CHF
                                        // Aktienbetrag 39679982249 05-Dez-2024 09-Dez-2024 -5.480,43 0,887182 -12,12 -4.862,14
                                        // Nettobetrag - - - - - -12,14 -4.869,43
                                        //
                                        // ID CHF CHF CHF
                                        // Aktienbetrag 40112732021 20-Dez-2024 24-Dez-2024 -3.050,00 1,000000 0,00 -3.050,00
                                        // Nettobetrag - - - - - 0,00 -3.057,58
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^ID (?<baseCurrency>[A-Z]{3}) [A-Z]{3} (?<termCurrency>[A-Z]{3})$") //
                                                        .match("^Aktienbetrag .* (?<exchangeRate>[\\.,\\d]+) \\-[\\.,\\d]+ \\-[\\.,\\d]+$") //
                                                        .match("^Nettobetrag .* \\-[\\s]*[\\.,\\d]+ \\-(?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Symbol SWDA:xswx Order-ID 5236807355
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-ID [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Handelsplatz Exchange Trade-ID 6093088529
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Trade\\-ID [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepositTransaction()
    {
        var type = new DocumentType("Cash Transfer Detail Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Cash Transfer Detail Report$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Bargeldtransfer
                        // CHF CHF
                        // Einlage 39482097030 28-Nov-2024 28-Nov-2024 4.600,00 1,000000 0,00 4.600,00
                        // Währung: CHF 28-Nov-2024 - 28-Nov-2024
                        // @formatter:on
                        .section("note", "date", "amount", "currency") //
                        .find("Bargeldtransfer") //
                        .match("^Einlage (?<note>[\\d]+) [\\d]{2}\\-[\\w]+\\-[\\d]{4} (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) .* (?<amount>[\\.,\\d]+)$") //
                        .match("^W.hrung: (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszugsbericht", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung : CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung : (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 26-Nov-2024 26-Nov-2024 DEPOSIT (6980803089, 6083903733) 700,00 700,00
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} (DEPOSIT) .* [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) " //
                                        + "DEPOSIT .* " //
                                        + "(?<amount>[\\.,\\d]+)" //
                                        + "[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Stempelgebühr 39683058642 05-Dez-2024 09-Dez-2024 -8,22 0,887182 -0,02 -7,29
                        // Stempelgebühr 40118366990 20-Dez-2024 24-Dez-2024 -4,58 1,000000 0,00 -4,58
                        // @formatter:on
                        .section("currencyConversionFee", "tax").optional() //
                        .documentContext("currency") //
                        .match("^Stempelgeb.hr .* (\\-)?(?<currencyConversionFee>[\\.,\\d]+) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var taxes = Money.of(v.get("currency"), asAmount(v.get("tax")));
                            var currencyConversionFee = Money.of(v.get("currency"), asAmount(v.get("currencyConversionFee")));

                            // Subtract currency conversion fee from taxes
                            taxes = taxes.subtract(currencyConversionFee);

                            checkAndSetTax(taxes, t, type.getCurrentContext());
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 4555555555 07-Apr-2025 08-Apr-2025 -1,77 0,860862 -0,01 -1,52
                        // Provision 40107905938 20-Dez-2024 24-Dez-2024 -3,00 1,000000 0,00 -3,00
                        // @formatter:on
                        .section("currencyConversionFee", "fee").optional() //
                        .documentContext("currency") //
                        .match("^Provision .* (\\-)?(?<currencyConversionFee>[\\.,\\d]+) \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var fees = Money.of(v.get("currency"), asAmount(v.get("fee")));
                            var currencyConversionFee = Money.of(v.get("currency"), asAmount(v.get("currencyConversionFee")));

                            // Add currency conversion fee from fees
                            fees = fees.add(currencyConversionFee);

                            checkAndSetFee(fees, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Stempelgebühr 39683058642 05-Dez-2024 09-Dez-2024 -8,22 0,887182 -0,02 -7,29
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Stempelgeb.hr .* \\-(?<fee>[\\.,\\d]+) \\-[\\.,\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Aktienbetrag 41308584615 05-Feb-2025 07-Feb-2025 -23.714,22 0,904466 -53,48 -21.448,69
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Aktienbetrag .* \\-(?<fee>[\\.,\\d]+) \\-[\\.,\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

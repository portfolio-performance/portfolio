package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote Targo bank provides two documents for the transaction.
 *           The security transaction and the taxes treatment.
 *           Both documents are provided as one PDF or as two PDFs.
 *
 *           The security transaction includes the fees and the withholding tax, but not the
 *           correct German taxes and the taxes treatment includes the German taxes,
 *           but not all fees and not the withholding tax.
 *
 *           Therefore, we use the documents based on their function and merge both documents, if possible, as one transaction.
 *           {@code
 *              matchTransactionPair(List<Item> transactionList,List<Item> taxesTreatmentList)
 *           }
 *
 *           The separate taxes treatment does only contain taxes in the account currency.
 *           However, if the security currency differs, we need to provide the currency conversion.
 *           {@code
 *              applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
 *           }
 *
 *           Always import the securities transaction and the taxes treatment for a correct transaction.
 *           Due to rounding differences, the correct gross amount is not always shown in the securities transaction.
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class TargobankPDFExtractor extends AbstractPDFExtractor
{
    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    private static final String ATTRIBUTE_GROSS_TAXES_TREATMENT = "gross_taxes_treatment";

    public TargobankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TARGO");
        addBankIdentifier("Targobank");
        addBankIdentifier("TARGOBANK AG");

        addBuySellTransaction();
        addDividendeTransaction_Format01();
        addDividendeTransaction_Format02();
        addTaxesTreatmentTransaction_Format01();
        addTaxesTreatmentTransaction_Format02();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Targobank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Effektenabrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Effektenabrechnung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Transaktionstyp (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wertpapier FanCy shaRe. nAmE X0-X0
                        // WKN / ISIN ABC123 / DE0000ABC123
                        // Kurs 12,34 EUR
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Kurs|Preis vom) .* (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 987,654
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag / Handelszeit 02.01.2020 / 13:01:00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 10.01.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Konto-Nr. 0101753165 1.008,91 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Rechnungsnummer: BOE-2020-0223620085-0000068
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Referenznummer 555666777888
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Referenznummer (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "Ref.-Nr.: " + trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction_Format01()
    {
        final var type = new DocumentType("(Ertragsgutschrift|Dividendengutschrift) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Ertragsgutschrift|Dividendengutschrift) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                        // WKN / ISIN A12CX1 / IE00BKX55T58
                        // Ausschüttung pro Stück 0,293466 USD
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Aussch.ttung|Dividende) pro St.ck [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 81
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbar 24.06.2020
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbar (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ex-Tag 11.06.2020
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ex\\-Tag (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gutschrift auf Ihrem Konto mit Wertstellung zum 31. August 2020
                                        // Konto-Nr. NUMMER 20,82 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag in EUR 24,49 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Bruttoertrag in [A-Z]{3} (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag 15,29 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Bruttoertrag (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Bruttoertrag 23,77 USD
                        // Devisenkurs zur Handelswährung USD/EUR 1,1223
                        // Bruttoertrag in EUR 21,18 EUR
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "currency").optional() //
                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) [A-Z]{3}$") //
                        .match("^Devisenkurs zur Handelsw.hrung (?<termCurrency>[A-Z]{3})\\/(?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Bruttoertrag in [A-Z]{3} [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // The withholding tax is already deducted from the gross income in the
                        // dividend document. The taxes treatment only credits it against the
                        // German capital gains tax.
                        //
                        // @formatter:off
                        // 15 % Ausländische Quellensteuer (US) 3,67 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^[\\.,\\d]+ % Ausl.ndische Quellensteuer( \\(.*\\))? (?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Rechnungsnummer: CPS-2020-0123456789-0001234
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction_Format02()
    {
        final var type = new DocumentType("Gutschrift mit Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        //                                                         USD                     EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^[\\s]{1,}([A-Z]{3}[\\s]{1,})?(?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Ertragsgutschrift|Dividendengutschrift)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // APPLE INC. REGISTERED SHARES O.N. WKN ISIN
                        // 865985 US0378331005
                        // Dividende pro Stück      USD         2,65000000   Zahlbar                14.02.2013
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^(?<name>.*) WKN ISIN$") //
                        .match("^(?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Aussch.ttung|Dividende) pro St.ck[\\s]{1,}(?<currency>[A-Z]{3})[\\s]{1,}[\\.,\\d]+.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 15,0000
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Dividende pro Stück      USD         2,65000000   Zahlbar                14.02.2013
                        // @formatter:on
                        .section("date") //
                        .match("^(Aussch.ttung|Dividende) pro St.ck.*Zahlbar[\\s]{1,}(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Geschäftsjahr                       2012 / 2013   Ex-Tag                 07.02.2013
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Gesch.ftsjahr.*Ex\\-Tag[\\s]{1,}(?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Gutschrift mit Wert 18.02.2013                                                25,36
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^Gutschrift mit Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // Bruttoertrag                                          39,75                   29,83
                        // Umrechnungskurs USD zu EUR        1,3324000
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate").optional() //
                        .match("^Bruttoertrag[\\s]{1,}(?<fxGross>[\\.,\\d]+)[\\s]{1,}[\\.,\\d]+$") //
                        .match("^Umrechnungskurs (?<termCurrency>[A-Z]{3}) zu (?<baseCurrency>[A-Z]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // The withholding tax is already deducted from the gross income in the
                        // dividend document. The taxes treatment only credits it against the
                        // German capital gains tax. The amount is deducted, this is indicated
                        // by the trailing minus sign.
                        .optionalOneOf( //
                                        // @formatter:off
                                        // 15,0000 % Ausländische Quellensteuer                   5,96-                   4,47-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("withHoldingTax") //
                                                        .documentContext("currency") //
                                                        .match("^[\\.,\\d]+ % Ausl.ndische Quellensteuer[\\s]{1,}[\\.,\\d]+\\-[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type)),
                                        // @formatter:off
                                        // 15,0000 % Ausländische Quellensteuer                   4,47-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("withHoldingTax") //
                                                        .documentContext("currency") //
                                                        .match("^[\\.,\\d]+ % Ausl.ndische Quellensteuer[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type)))

                        // @formatter:off
                        // Rechnungsnummer:        EE2-505-DC00-88560860962
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTreatmentTransaction_Format01()
    {
        final var type = new DocumentType("\\(Steuerbeilage\\)");
        this.addDocumentTyp(type);

        var block = new Block("^.* \\(Steuerbeilage\\) .*$");
        type.addBlock(block);

        var pdfTransaction = new Transaction<AccountTransaction>();
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .oneOf( //
                                        // @formatter:off
                                        // Wertpapier FanCy shaRe. nAmE X0-X0
                                        // WKN / ISIN ABC123 / DE0000ABC123
                                        // Kurs 12,34 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(Kurs|Preis vom) .* (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                                        // WKN / ISIN A12CX1 / IE00BKX55T58
                                        // Gesamtsumme Steuern 5,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Gesamtsumme Steuern [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Stück 987,654
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag / Handelszeit 26.05.2020 / 20:32:00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 10.01.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Belastung Ihres Kontos NUMMER mit Wertstellung zum 24. Juni 2020.
                                        // Belastung Ihres Kontos 536011111111 mit Wertstellung zum 1. Oktober 2020.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Belastung Ihres Kontos .* (?<date>[\\d]{1,2}\\. .* [\\d]{4})\\.$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Only use for dividend transactions, when the pay date is missing
                                        // Ex-Tag 11.06.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ex-Tag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Teilfreistellung (§ 20 InvStG) 30,00 % - 6,35 EUR
                                        // Erträge/Verluste 14,83 EUR
                                        // Bemessungsgrundlage 0,00 EUR
                                        // Gesamtsumme Steuern 5,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("partialExemptionTaxes", "currencypartialExemptionTaxes", //
                                                                        "grossBeforeTaxes", "currencyBeforeTaxes", //
                                                                        "grossAssessmentBasis", "currencyAssessmentBasis", //
                                                                        "deductedTaxes", "currencyDeductedTaxes") //
                                                        .match("^Teilfreistellung \\(§ 20 InvStG\\) [\\.,\\d]+ % \\- (?<partialExemptionTaxes>[\\.,\\d]+) (?<currencypartialExemptionTaxes>[A-Z]{3})$") //
                                                        .match("^Ertr.ge\\/Verluste (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[A-Z]{3})$") //
                                                        .match("^Bemessungsgrundlage (?<grossAssessmentBasis>[\\.,\\d]+) (?<currencyAssessmentBasis>[A-Z]{3})$") //
                                                        .match("^Gesamtsumme Steuern (?<deductedTaxes>[\\.,\\d]+) (?<currencyDeductedTaxes>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var partialExemptionTaxes = Money.of(asCurrencyCode(v.get("currencypartialExemptionTaxes")), asAmount(v.get("partialExemptionTaxes")));
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            grossBeforeTaxes = grossBeforeTaxes.add(partialExemptionTaxes);

                                                            var grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(v.get("grossAssessmentBasis")));
                                                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(v.get("deductedTaxes")));

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // The withholding tax is already deducted in the dividend document
                                        // and is only credited against the capital gains tax here. Therefore
                                        // only the total amount of taxes is booked.
                                        //
                                        // @formatter:off
                                        // Anrechenbare ausländische Quellensteuer 6,65 EUR
                                        // Erträge/Verluste 44,35 EUR
                                        // Anrechnung ausländischer Quellensteuer ** - 26,60 EUR
                                        // Bemessungsgrundlage 17,75 EUR
                                        // Gesamtsumme Steuern 4,68 EUR
                                        //
                                        // Erträge/Verluste 3.123,25 EUR
                                        // Bemessungsgrundlage 3.123,25 EUR
                                        // Gesamtsumme Steuern 823,76 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("grossBeforeTaxes", "currencyBeforeTaxes", //
                                                                        "grossAssessmentBasis", "currencyAssessmentBasis", //
                                                                        "deductedTaxes", "currencyDeductedTaxes") //
                                                        .match("^Ertr.ge\\/Verluste (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[A-Z]{3})$") //
                                                        .match("^Bemessungsgrundlage (?<grossAssessmentBasis>[\\.,\\d]+) (?<currencyAssessmentBasis>[A-Z]{3})$") //
                                                        .match("^Gesamtsumme Steuern (?<deductedTaxes>[\\.,\\d]+) (?<currencyDeductedTaxes>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            var grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(v.get("grossAssessmentBasis")));
                                                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(v.get("deductedTaxes")));

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);

                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // @formatter:off
                                        // We don't have to pay taxes
                                        //
                                        // Veräußerungsverlust - 1.311,10 EUR
                                        // Erträge/Verluste - 1.311,10 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("grossLoss", "currencyLoss", "grossBeforeTaxes", "currencyBeforeTaxes") //
                                                        .match("^Ver.u.erungsverlust \\- (?<grossLoss>[\\.,\\d]+) (?<currencyLoss>[A-Z]{3})$") //
                                                        .match("^Ertr.ge\\/Verluste \\- (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var grossLoss = Money.of(asCurrencyCode(v.get("currencyLoss")), asAmount(v.get("grossLoss")));
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            grossBeforeTaxes = grossLoss.subtract(grossBeforeTaxes);

                                                            // There is no taxes to pay
                                                            if (grossBeforeTaxes.isZero())
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(grossBeforeTaxes);
                                                            }
                                                        }))

                        // @formatter:off
                        // Transaktionsreferenz TR TBK14720B024746O001
                        // Transaktionsreferenz INDTBK1234567890
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Transaktionsreferenz( TR)? (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Tr.-Nr.: " + trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            // Store attribute in item data map
                            item.setData(ATTRIBUTE_GROSS_TAXES_TREATMENT, ctx.get(ATTRIBUTE_GROSS_TAXES_TREATMENT));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                ctx.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return item;
                        });
    }

    private void addTaxesTreatmentTransaction_Format02()
    {
        final var type = new DocumentType("Steuerbeilage Depot\\-Nr\\.", //
                        documentContext -> documentContext //
                                        // This document format does not contain a pay date or an ex-date,
                                        // therefore the date of the letter is used. It is printed above
                                        // the block and must be read from the document context.
                                        //
                                        // @formatter:off
                                        // Duisburg, den 15.02.2013
                                        // @formatter:on
                                        .section("date") //
                                        .match("^.*, .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        var block = new Block("^(Ertragsgutschrift|Dividendengutschrift)$");
        type.addBlock(block);

        var pdfTransaction = new Transaction<AccountTransaction>();
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        // @formatter:off
                        // APPLE INC. REGISTERED SHARES O.N. WKN ISIN
                        // 865985 US0378331005
                        // Gesamtsumme Steuern 0,00 EUR
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^(?<name>.*) WKN ISIN$") //
                        .match("^(?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Gesamtsumme Steuern [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 15,0000
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // The withholding tax is already deducted in the dividend document
                        // and is only credited against the capital gains tax here. Therefore
                        // only the total amount of taxes is booked.
                        //
                        // @formatter:off
                        // Erträge / Verluste nach §20 EStG 29,83 EUR
                        // Bemessungsgrundlage 0,00 EUR
                        // Gesamtsumme Steuern 0,00 EUR
                        // @formatter:on
                        .section("grossBeforeTaxes", "currencyBeforeTaxes", //
                                        "grossAssessmentBasis", "currencyAssessmentBasis", //
                                        "deductedTaxes", "currencyDeductedTaxes") //
                        .documentContext("date") //
                        .match("^Ertr.ge \\/ Verluste nach .20 EStG (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[A-Z]{3})$") //
                        .match("^Bemessungsgrundlage (?<grossAssessmentBasis>[\\.,\\d]+) (?<currencyAssessmentBasis>[A-Z]{3})$") //
                        .match("^Gesamtsumme Steuern (?<deductedTaxes>[\\.,\\d]+) (?<currencyDeductedTaxes>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                            var grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(v.get("grossAssessmentBasis")));
                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(v.get("deductedTaxes")));

                            t.setDateTime(asDate(v.get("date")));

                            // Calculate the taxes and store gross amount
                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                            {
                                // Store in transaction context
                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);

                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));
                            }
                            else
                            {
                                // Store in transaction context
                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                t.setMonetaryAmount(deductedTaxes);
                            }
                        })

                        // @formatter:off
                        // Transaktionsreferenz  IND00009801615D00094890632
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Transaktionsreferenz( TR)? (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Tr.-Nr.: " + trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            // Store attribute in item data map
                            item.setData(ATTRIBUTE_GROSS_TAXES_TREATMENT, ctx.get(ATTRIBUTE_GROSS_TAXES_TREATMENT));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                ctx.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return item;
                        });
    }

    private void addAccountStatementTransaction()
    {
        // @formatter:off
        // The period and the currency are valid for the whole document. Because the
        // document type already uses ranges to tell the two accounts apart, they are
        // provided as a range spanning the complete document as well.
        //
        // F I N A N Z S T A T U S vom 01.10.2025 - 31.10.2025
        // Gesamtguthaben EUR 7.480,65
        // @formatter:on
        var documentRange = new Block("^F I N A N Z S T A T U S vom .*$") //
                        .asRange(section -> section //
                                        .attributes("year", "currency") //
                                        .match("^F I N A N Z S T A T U S vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\- [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .match("^Gesamtguthaben (?<currency>[A-Z]{3}) [\\.,\\d]+$"));

        // @formatter:off
        // The document contains the bookings of the current account and of the savings
        // account in two tables of the same layout. Only the table of the current account
        // is imported, the bookings of the savings account are skipped. The tables are
        // told apart by their header line, which -- unlike the identically named line of
        // the summary at the beginning of the document -- carries no balance.
        //
        // ONLINE-KONTO                            5333333333 EUR
        // EUR TAGESGELDKONTO                      5222222250 EUR
        // @formatter:on
        var currentAccountRange = new Block("^ONLINE\\-KONTO[\\s]{1,}[\\d]+ [A-Z]{3}$", //
                        "^Dieser Finanzstatus ist gleichzeitig Ihr Kontoauszug Nr\\. [\\d]+$") //
                                        .asRange(section -> section //
                                                        .attributes("accountType") //
                                                        .match("^(?<accountType>ONLINE\\-KONTO)[\\s]{1,}[\\d]+ [A-Z]{3}$"));

        var savingsAccountRange = new Block("^EUR TAGESGELDKONTO[\\s]{1,}[\\d]+ [A-Z]{3}$") //
                        .asRange(section -> section //
                                        .attributes("accountType") //
                                        .match("^EUR (?<accountType>TAGESGELDKONTO)[\\s]{1,}[\\d]+ [A-Z]{3}$"));

        final var type = new DocumentType("F I N A N Z S T A T U S", documentRange, currentAccountRange,
                        savingsAccountRange);
        this.addDocumentTyp(type);

        // @formatter:off
        // The booking table has the columns "Belastungen", "Gutschriften" and
        // "Guthaben/Kredit", but the text extraction does not preserve the column
        // positions and no sign is printed. Every booking therefore reads as
        // "<amount> <balance>" and the direction can only be taken from the booking
        // text. Booking texts that are not listed below are skipped on purpose, a
        // booking with the wrong sign would be worse than a missing one.
        //
        // Datum Tag Buchungstext Ausgaben Einnahmen Guthaben/Kredit
        // 13.10 MO INTERNE UMBUCHUNG HABEN TARGOBANK KONTO 1.111,11 1.111,11
        // 13.10 MO INTERNE UMBUCHUNG SOLL TARGO OLB 4.436,70 0,03
        // @formatter:on
        var depositRemovalBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\.? (MO|DI|MI|DO|FR|SA|SO) INTERNE UMBUCHUNG (HABEN|SOLL) .*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "type", "amount") //
                        .documentRange("year", "currency", "accountType") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2})\\.? (MO|DI|MI|DO|FR|SA|SO) (?<note>INTERNE UMBUCHUNG (?<type>HABEN|SOLL) .*) (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            // Only the bookings of the current account are
                            // imported
                            if (!"ONLINE-KONTO".equals(v.get("accountType")))
                                v.getTransactionContext().skipTransaction(
                                                Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            // Is type --> "SOLL" change from DEPOSIT to REMOVAL
                            if ("SOLL".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(replaceMultipleBlanks(trim(v.get("note"))));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 02.10 DO Grundgebühr für September 2025 3,95 1.111,11
        // 04.08. DI Grundgebühr für Juli      2026 3,95 1.489,25
        // @formatter:on
        var feeBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.? (MO|DI|MI|DO|FR|SA|SO) Grundgeb.hr .*$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "note", "amount") //
                        .documentRange("year", "currency", "accountType") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2})\\.? (MO|DI|MI|DO|FR|SA|SO) (?<note>Grundgeb.hr .*) (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            // Only the bookings of the current account are
                            // imported
                            if (!"ONLINE-KONTO".equals(v.get("accountType")))
                                v.getTransactionContext().skipTransaction(
                                                Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(replaceMultipleBlanks(trim(v.get("note"))));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addNonImportableTransaction()
    {
        final var type = new DocumentType("Ausbuchung aus Ihrem Depot", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Tag der Übertragung 09.12.2025
                                        // @formatter:on
                                        .section("date") //
                                        .match("^Tag der .bertragung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^[\\.,\\d]+ ST .* [\\.,\\d]+ ST$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_OUTBOUND))

                        // @formatter:off
                        // 34,0000 ST MUL-AMUN ST600 BANK ETF A Girosammelverwahrung 0,0000 ST
                        // LYX01W / LU1834983477
                        // @formatter:on
                        .section("shares", "name", "wkn", "isin") //
                        .documentContext("date") //
                        .match("^(?<shares>[\\.,\\d]+) ST (?<name>.*) (Girosammelverwahrung|Streifbandverwahrung|Wertpapierrechnung) [\\.,\\d]+ ST$") //
                        .match("^(?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> {
                            v.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setAmount(0L);
                        })

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Provision 8,90 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }


    /**
     * @formatter:off
     * This method performs post-processing on a list transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions, purchase/sale transactions, and dividend transactions.
     * 2. Matches purchase/sale transactions with their corresponding taxes treatment and dividend transactions with their corresponding taxes treatment.
     * 3. Adjusts purchase/sale transactions by adding/subtracting tax amounts, adding tax units, combining source information, appending tax-related notes,
     *    and removing taxes treatment's from the list of items.
     * 4. Adjusts dividend transactions by updating the gross amount if necessary, adding/subtracting tax amounts, adding tax units,
     *    combining source information, appending taxes treatment notes, and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in purchase/sale and dividend transactions, making the transaction's more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        var taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> { //
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by buySell transactions
        var purchaseSaleTransactionList = items.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> { //
                            var type = ((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType(); //
                            return PortfolioTransaction.Type.SELL.equals(type)
                                            || PortfolioTransaction.Type.BUY.equals(type); //
                        }) //
                        .toList();

        // Filter transactions by dividend transactions
        var dividendTransactionList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .toList();

        var purchaseSaleListTaxPairs = matchTransactionPair(purchaseSaleTransactionList, taxesTreatmentList);
        var dividendTaxPairs = matchTransactionPair(dividendTransactionList, taxesTreatmentList);

        applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(purchaseSaleListTaxPairs);

        // @formatter:off
        // This loop iterates through a list of purchase/sale and tax pairs and processes them.
        //
        // For each pair, it adds/subtracts the tax amount from the purchase/sale transaction's total amount,
        // adds the tax as a tax unit to the purchase/sale transaction, combines source information if needed,
        // appends taxes treatment notes to the purchase/sale transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : purchaseSaleListTaxPairs)
        {
            var purchaseSaleTransaction = (BuySellEntry) pair.transaction.getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null && taxesTransaction.getType() == AccountTransaction.Type.TAXES)
            {
                if (purchaseSaleTransaction.getPortfolioTransaction().getType().isLiquidation())
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().subtract(taxesTransaction.getMonetaryAmount()));
                }
                else
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().add(taxesTransaction.getMonetaryAmount()));
                }

                purchaseSaleTransaction.getPortfolioTransaction()
                                .addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                purchaseSaleTransaction.setSource(
                                concatenate(purchaseSaleTransaction.getSource(), taxesTransaction.getSource(), "; "));

                purchaseSaleTransaction.setNote(
                                concatenate(purchaseSaleTransaction.getNote(), taxesTransaction.getNote(), " | "));

                ExtractorUtils.fixGrossValueBuySell().accept(purchaseSaleTransaction);

                items.remove(pair.tax());
            }
        }

        // @formatter:off
         // This loop processes a list of dividend and tax pairs, adjusting the gross amount of dividend transactions as needed.
         //
         // For each pair, it checks if there is a corresponding tax transaction. If present, it considers the gross taxes treatment
         // and sets the dividend amount to that gross amount, reduced by the withholding tax that has already been deducted
         // in the dividend document. If taxes amount is zero and gross taxes treatment is less than the dividend gross value,
         // the difference is added as an additional tax unit.
         //
         // If there is no tax transaction, it simply fixes the gross value of the dividend transaction.
         // @formatter:on
        for (TransactionTaxesPair pair : dividendTaxPairs)
        {
            var dividendTransaction = (AccountTransaction) pair.transaction().getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null)
            {
                var grossTaxesTreatment = (Money) pair.tax().getData(ATTRIBUTE_GROSS_TAXES_TREATMENT);

                if (grossTaxesTreatment != null)
                {
                    // @formatter:off
                    // The withholding tax has already been deducted from the dividend transaction
                    // and must not be counted twice when the gross amount of the taxes treatment
                    // is applied.
                    // @formatter:on
                    var withHoldingTax = dividendTransaction.getUnitSum(Unit.Type.TAX);

                    var dividendGrossValue = dividendTransaction.getGrossValue();
                    var taxesAmount = taxesTransaction.getMonetaryAmount();

                    if (taxesAmount.isZero() && grossTaxesTreatment.isLessThan(dividendGrossValue))
                    {
                        var adjustedTaxes = dividendGrossValue.subtract(grossTaxesTreatment);
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, adjustedTaxes));
                    }

                    dividendTransaction.setMonetaryAmount(grossTaxesTreatment.subtract(withHoldingTax));
                }

                ExtractorUtils.fixGrossValue().accept(dividendTransaction);

                dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount() //
                                .subtract(taxesTransaction.getMonetaryAmount()));

                dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                dividendTransaction.setSource(
                                concatenate(dividendTransaction.getSource(), taxesTransaction.getSource(), "; "));

                dividendTransaction
                                .setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

                ExtractorUtils.fixGrossValue().accept(dividendTransaction);

                items.remove(pair.tax());
            }
            else
            {
                ExtractorUtils.fixGrossValue().accept(dividendTransaction);
            }
        }
    }

    /**
     * @formatter:off
     * Matches transactions and taxes treatment's, ensuring unique pairs based on date and security.
     *
     * This method matches transactions and taxes treatment's by creating a Pair consisting of the transaction's
     * date and security. It uses a Set called 'keys' to prevent duplicates based on these Pair keys,
     * ensuring that the same combination of date and security is not processed multiple times.
     * Duplicate transactions for the same security on the same day are avoided.
     *
     * @param transactionList      A list of transactions to be matched.
     * @param taxesTreatmentList   A list of taxes treatment's to be considered for matching.
     * @return A collection of TransactionTaxesPair objects representing matched transactions and taxes treatment's.
     * @formatter:on
     */
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList,
                    List<Item> taxesTreatmentList)
    {
        // Use a Set to prevent duplicates
        Set<Pair<LocalDate, Security>> keys = new HashSet<>();
        Map<Pair<LocalDate, Security>, TransactionTaxesPair> pairs = new HashMap<>();

        // Match identified transactions and taxes treatment's
        transactionList.forEach( //
                        transaction -> {
                            var key = new Pair<>(transaction.getDate().toLocalDate(), transaction.getSecurity());

                            // Prevent duplicates
                            if (keys.add(key))
                                pairs.put(key, new TransactionTaxesPair(transaction, null));
                        } //
        );

        // Iterate through the list of taxes treatment's to match them with
        // transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date
                            // and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with
                            // this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes
                            // treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
    }

    /**
     * @formatter:off
     * Resolves missing currency conversions between taxes and purchase/sale transactions based on existing exchange rates.
     *
     * For each TransactionTaxesPair, this method checks for currency mismatches between:
     * - the monetary amount and security currency of the taxes transaction, and
     * - the monetary amount and security currency of the purchase/sale transaction.
     *
     * If either side shows a mismatch, and if the opposite side contains a valid exchange rate,
     * a corresponding GROSS_VALUE unit with the appropriate FX conversion will be added to ensure consistency.
     *
     * This helps ensure that both tax and purchase/sale transactions carry correct currency conversion data
     * when working across multi-currency portfolios.
     *
     * @param purchaseSaleTaxPairs A collection of TransactionTaxesPair objects containing associated taxes and purchase/sale transactions.
     * @formatter:on
     */
    private void applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(
                    Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
    {
        purchaseSaleTaxPairs.forEach(pair -> {
            if (pair.tax != null && pair.transaction != null)
            {
                var tax = (AccountTransaction) pair.tax.getSubject();
                var purchaseSale = (BuySellEntry) pair.transaction.getSubject();
                var purchaseSalePortfolioTx = purchaseSale.getPortfolioTransaction();

                // Determine currency of monetary amounts and associated
                // securities
                var taxCurrency = tax.getMonetaryAmount().getCurrencyCode();
                var taxSecurityCurrency = tax.getSecurity().getCurrencyCode();

                var purchaseSaleCurrency = purchaseSalePortfolioTx.getMonetaryAmount().getCurrencyCode();
                var purchaseSaleSecurityCurrency = purchaseSalePortfolioTx.getSecurity().getCurrencyCode();

                var taxHasMismatch = !taxCurrency.equals(taxSecurityCurrency);
                var purchaseSaleHasMismatch = !purchaseSaleCurrency.equals(purchaseSaleSecurityCurrency);

                // Proceed only if at least one of the transactions has a
                // currency mismatch
                if (taxHasMismatch || purchaseSaleHasMismatch)
                {
                    var taxAmount = tax.getMonetaryAmount();

                    var taxGrossValue = tax.getUnit(Unit.Type.GROSS_VALUE);
                    var purchaseSaleGrossValue = purchaseSalePortfolioTx.getUnit(Unit.Type.GROSS_VALUE);

                    // If the taxes transaction contains a usable exchange rate,
                    // apply the conversion to the sales transaction. Otherwise,
                    // if the purchase/sales transaction contains a usable
                    // exchange rate,
                    // apply the conversion to the taxes transaction.
                    if (taxGrossValue.isPresent() && taxGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(taxGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency,
                                        purchaseSalePortfolioTx.getMonetaryAmount());

                        purchaseSalePortfolioTx.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                        purchaseSalePortfolioTx.getMonetaryAmount(), fxGross, rate.getRate()));
                    }
                    else if (purchaseSaleGrossValue.isPresent()
                                    && purchaseSaleGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(purchaseSaleGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency, taxAmount);

                        tax.addUnit(new Unit(Unit.Type.GROSS_VALUE, taxAmount, fxGross, rate.getRate()));
                    }
                }
            }
        });
    }
}

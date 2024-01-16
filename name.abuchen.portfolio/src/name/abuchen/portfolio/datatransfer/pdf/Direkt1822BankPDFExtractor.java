package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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

@SuppressWarnings("nls")
public class Direkt1822BankPDFExtractor extends AbstractPDFExtractor
{
    public Direkt1822BankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("1822direkt");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
    }

    @Override
    public String getLabel()
    {
        return "1822direkt";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf|(Ausgabe|R.cknahme) Investmentfonds)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf|(Ausgabe|R.cknahme) Investmentfonds).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf|(Ausgabe|R.cknahme) Investmentfonds)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Rücknahme Investmentfonds".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                        // INHABER-ANTEILE I O.N.
                        // Börse Außerbörslich (gemäß Weisung)
                        // Ausführungskurs 40,968 EUR Auftragserteilung Online-Banking
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Börse"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag/-Zeit 01.12.2017 10:30:52 Auftraggeber Mustermann, Max
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 09.07.2021 Auftraggeber Mustermann, Max
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Ausmachender Betrag 50,00- EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs (EUR/USD) 1,1987 vom 02.03.2021
                        // Kurswert 52,50- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)\\- [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer 123456/10.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer [\\d]+\\/[\\.\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit 40,99 EUR AN
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit [\\.,\\d]+ [\\w]{3}).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Postfach.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                        // REGISTERED SHARES USD O.N.
                        // Zahlbarkeitstag 29.12.2017 Ertrag pro St. 0,123000000 USD
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("(?<nameContinued>.*)") //
                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Aussch.ttung|Dividende|Ertrag) pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 03.01.2018 zu Gunsten des Kontos xxxxxxxxxx (IBAN DExx xxxx xxxx xxxx
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 68,87+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD 1,2095
                        // Devisenkursdatum 02.01.2018
                        // Ausschüttung 113,16 USD 93,56+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^(Aussch.ttung|Dividendengutschrift) (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Abrechnungsnr. 504xxxxxxxx
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale Investmentfonds");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Postfach.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 16,0871 AIS AMUNDI DJ SWITZTIT30 LU2611732632 (ETF198)
                        // UCITS ETF DIST INH ANTEILE
                        // Zahlbarkeitstag 02.01.2024 Vorabpauschale pro St. 2,556193310 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("(?<nameContinued>.*)") //
                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Vorabpauschale pro St\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 16,0871 AIS AMUNDI DJ SWITZTIT30 LU2611732632 (ETF198)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 04.01.2024 zu Lasten des Kontos 0035254457 (IBAN DE05 5721 8171 6221 4406
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 0,63- EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abrechnungsnr. 72251346037
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 25 % auf 93,63 EUR 23,41- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+ % .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+ .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 5,22 USD 0,66- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+ .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 4,38 EUR 0,66 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+ .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 4,95- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 1,5000 % vom Kurswert 8,10- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen 1,95- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 0,11- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,11- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match(".bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsentgelt 1,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("Handelsentgelt (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kurswert 52,50- EUR
                        // Kundenbonifikation 100 % vom Ausgabeaufschlag 2,50 EUR
                        // Ausgabeaufschlag pro Anteil 5,00 %
                        // @formatter:on
                        .section("amount", "currency", "discount", "discountCurrency", "percentageFee").optional() //
                        .match("^Kurswert (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .match("^Kundenbonifikation [\\.,\\d]+ % vom Ausgabeaufschlag (?<discount>[\\.,\\d]+) (?<discountCurrency>[\\w]{3})$") //
                        .match("^Ausgabeaufschlag pro Anteil (?<percentageFee>[\\.,\\d]+) %$") //
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                            BigDecimal amount = asBigDecimal(v.get("amount"));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")),
                                            asAmount(v.get("discount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                BigDecimal fxFee = amount
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                                .add(BigDecimal.ONE), Values.MC)
                                                .multiply(percentageFee, Values.MC);

                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // fee = fee - discount
                                fee = fee.subtract(discount);

                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });
    }
}

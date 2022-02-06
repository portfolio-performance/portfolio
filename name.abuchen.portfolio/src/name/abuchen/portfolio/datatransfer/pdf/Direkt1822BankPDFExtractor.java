package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetFee;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
public class Direkt1822BankPDFExtractor extends AbstractPDFExtractor
{
    public Direkt1822BankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("1822direkt"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "1822direkt"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf|Ausgabe Investmentfonds)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf|Ausgabe Investmentfonds)(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf|Ausgabe Investmentfonds))(.*)?$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                // INHABER-ANTEILE I O.N.
                // Ausführungskurs 40,968 EUR Auftragserteilung Online-Banking
                .section("shares", "name", "isin", "wkn", "nameContinued")
                .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("(?<nameContinued>.*)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                .oneOf(
                                // Auftrag vom 05.12.2017 00:15:28 Uhr
                                section -> section.attributes("date", "time")
                                        .match("^Auftrag vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) Uhr$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Schlusstag/-Zeit 01.12.2017 10:30:52 Auftraggeber Mustermann, Max
                                section -> section.attributes("date", "time")
                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Schlusstag 09.07.2021 Auftraggeber Mustermann, Max
                                section -> section.attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // Ausmachender Betrag 50,00- EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                // REGISTERED SHARES USD O.N.
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ausmachender Betrag 68,87+ EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Den Betrag buchen wir mit Wertstellung 03.01.2018 zu Gunsten des Kontos xxxxxxxxxx (IBAN DExx xxxx xxxx xxxx
                .section("date")
                .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Devisenkurs EUR / USD 1,2095
                // Devisenkursdatum 02.01.2018
                // Ausschüttung 113,16 USD 93,56+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs .* (?<exchangeRate>[\\.,\\d]+)$")
                .match("^Aussch.ttung (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eingebuchte sonstige negative Kapitalerträge 0,02 EUR
                .section("tax", "currency").optional()
                .match("^Eingebuchte.*Kapitalertr.ge (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25 % auf 93,63 EUR 23,41- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+ % .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+ .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 15 % auf 5,22 USD 0,66-
                // EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltende Quellensteuer [\\.,\\d]+ .* (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 4,38 EUR 0,66 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+ .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 4,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen 1,95- EUR
                .section("fee", "currency").optional()
                .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 0,11- EUR
                .section("fee", "currency").optional()
                .match("Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,11- EUR
                .section("fee", "currency").optional()
                .match(".bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsentgelt 1,00- EUR
                .section("fee", "currency").optional()
                .match("Handelsentgelt (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Kurswert 52,50- EUR
                // Kundenbonifikation 100 % vom Ausgabeaufschlag 2,50 EUR
                // Ausgabeaufschlag pro Anteil 5,00 %
                .section("amount", "currency", "discount", "discountCurrency", "percentageFee").optional()
                .match("^Kurswert (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .match("^Kundenbonifikation [\\.,\\d]+ % vom Ausgabeaufschlag (?<discount>[\\.,\\d]+) (?<discountCurrency>[\\w]{3})$")
                .match("^Ausgabeaufschlag pro Anteil (?<percentageFee>[\\.,\\d]+) %$")
                .assign((t, v) -> {
                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                    {
                        // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        // fee = fee - discount
                        fee = fee.subtract(discount);

                        checkAndSetFee(fee, t, type);
                    }
                });
    }
}

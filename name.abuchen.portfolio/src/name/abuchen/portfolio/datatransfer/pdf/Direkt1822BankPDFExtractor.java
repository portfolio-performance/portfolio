package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class Direkt1822BankPDFExtractor extends AbstractPDFExtractor
{
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$

    public Direkt1822BankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("1822direkt"); //$NON-NLS-1$

        addBuySellTransaction();
        addBuySellSavePlanTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "1822direkt"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "1822direkt"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf).*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Wertpapier Abrechnung (Kauf|Verkauf).*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match(".*Abrechnung (?<type>Verkauf?).*")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                // INHABER-ANTEILE I O.N.
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^(St.ck) (?<shares>[.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("B.rse"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Auftrag vom 05.12.2017 00:15:28 Uhr
                .section("date", "time")
                .match("^(Schlusstag/-Zeit) (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 50,00- EUR
                .section("currency", "amount")
                .match("^(Ausmachender Betrag) (?<amount>[.,\\d.]+)[?(-|\\+)] (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs (EUR/USD) 1,1987 vom 02.03.2021
                .section("exchangeRate").optional()
                .match("^(Devisenkurs) \\([\\w]{3}\\/[\\w]{3}\\) (?<exchangeRate>[.,\\d]+) .*")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellSavePlanTransaction()
    {
        DocumentType type = new DocumentType(".*Wertpapier Abrechnung Ausgabe Investmentfonds.*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Wertpapier Abrechnung Ausgabe Investmentfonds.*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                // INHABER-ANTEILE I O.N.
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^(St.ck) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("B.rse"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Auftrag vom 05.12.2017 00:15:28 Uhr
                .section("date", "time")
                .match("^(Auftrag vom) (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+) Uhr")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 50,00- EUR
                .section("amount", "currency")
                .match("^(Ausmachender Betrag) (?<amount>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs (EUR/USD) 1,1987 vom 02.03.2021
                .section("exchangeRate").optional()
                .match("^(Devisenkurs) \\([\\w]{3}\\/[\\w]{3}\\) (?<exchangeRate>[.,\\d]+) .*")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                // REGISTERED SHARES USD O.N.
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^(Stück) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ausmachender Betrag 68,87+ EUR
                .section("currency", "amount")
                .match("^(Ausmachender Betrag) (?<amount>[.,\\d]+)\\+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Ex-Tag 14.12.2017 Herkunftsland Irland
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}).*")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Devisenkurs EUR / USD 1,2095
                // Devisenkursdatum 02.01.2018
                // Ausschüttung 113,16 USD 93,56+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^(Devisenkurs) .* (?<exchangeRate>[.,\\d]+)$")
                .match("^(Ausschüttung) (?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<amount>[.,\\d]+)\\+ (?<currency>[\\w]{3})$")                        
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
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
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
                .match("^(Eingebuchte.*Kapitalerträge) (?<tax>[.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25 % auf 93,63 EUR 23,41- EUR
                .section("tax", "currency").optional()
                .match("^(Kapitalertragsteuer) [.,\\d]+ % .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
                .section("tax", "currency").optional()
                .match("^(Solidaritätszuschlag) [.,\\d]+ .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 15 % auf 5,22 USD 0,66- EUR
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltende Quellensteuer [.,\\d]+ .* (?<quellensteinbeh>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer 15 % auf 4,38 EUR 0,66 EUR
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer [.,\\d]+ .* (?<quellenstanr>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 4,95- EUR
                .section("fee", "currency").optional()
                .match("^(Provision) (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen 1,95- EUR
                .section("fee", "currency").optional()
                .match("^(Eigene Spesen) (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 0,11- EUR
                .section("fee", "currency").optional()
                .match("(Transaktionsentgelt B.rse) (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,11- EUR
                .section("fee", "currency").optional()
                .match("(.bertragungs-\\/Liefergeb.hr) (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsentgelt 1,00- EUR
                .section("fee", "currency").optional()
                .match("(Handelsentgelt) (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Kurswert 52,50- EUR
                // Kundenbonifikation 100 % vom Ausgabeaufschlag 2,50 EUR
                // Ausgabeaufschlag pro Anteil 5,00 %
                .section("feeFx", "feeFy", "amountFx", "currency").optional()
                .match("^Kurswert (?<amountFx>[.,\\d]+)[-]? (?<currency>[\\w]{3})")
                .match("^Kundenbonifikation (?<feeFy>[.,\\d]+) % vom Ausgabeaufschlag [.,\\d]+ [\\w]{3}")
                .match("^Ausgabeaufschlag pro Anteil (?<feeFx>[.,\\d]+) %")
                .assign((t, v) -> {
                    // Fee in percent
                    double amountFx = Double.parseDouble(v.get("amountFx").replace(',', '.'));
                    double feeFy = Double.parseDouble(v.get("feeFy").replace(',', '.'));
                    double feeFx = Double.parseDouble(v.get("feeFx").replace(',', '.'));
                    feeFy = (amountFx / (1 + feeFx / 100)) * (feeFx / 100) * (feeFy / 100);
                    String fee =  Double.toString((amountFx / (1 + feeFx / 100)) * (feeFx / 100) - feeFy).replace('.', ',');
                    v.put("fee", fee);

                    processFeeEntries(t, v, type);
                });
    }

    private void addTax(DocumentType type, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        if (checkWithholdingTax(type, taxtype))
        {
            ((name.abuchen.portfolio.model.Transaction) t)
                    .addUnit(new Unit(Unit.Type.TAX, 
                                    Money.of(asCurrencyCode(v.get("currency")), 
                                                    asAmount(v.get(taxtype)))));
        }
    }

    private boolean checkWithholdingTax(DocumentType type, String taxtype)
    {
        if (Boolean.valueOf(type.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype))
            { 
                return false; 
            }
        }
        return true;
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}

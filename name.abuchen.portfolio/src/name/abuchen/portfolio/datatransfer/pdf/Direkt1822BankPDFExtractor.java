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
        DocumentType newType = new DocumentType(".*Abrechnung Kauf.*");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Abrechnung Kauf.*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
            // INHABER-ANTEILE I O.N.
            .section("isin", "wkn", "name", "shares", "nameContinued")
            .match("^(Stück) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Auftrag vom 05.12.2017 00:15:28 Uhr
            .section("date", "time")
            .match("^(Schlusstag/-Zeit) (?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+).*")
            .assign((t, v) -> {
                if (v.get("time") != null)
                    t.setDate(asDate(v.get("date"), v.get("time")));
                else
                    t.setDate(asDate(v.get("date")));
            })

            // Ausmachender Betrag 50,00- EUR
            .section("currency", "amount")
            .match("^(Ausmachender Betrag) (?<amount>[\\d.]+,\\d+)[?(-|\\+)] (?<currency>\\w{3}+)")
            .assign((t, v) -> {
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(v.get("currency"));
            })

            // Eingebuchte sonstige negative Kapitalerträge 0,02 EUR
            .section("tax", "currency")
            .match("^(Eingebuchte.*Kapitalerträge) (?<tax>[\\d.]+,\\d{2}) (?<currency>\\w{3})")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("tax"))))))
            
            // Provision 4,95- EUR
            .section("fee", "currency")
            .match("^(Provision) (?<fee>[\\d.-]+,\\d+)- (?<currency>\\w{3}+)")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("fee"))))))
            
            // Eigene Spesen 1,95- EUR
            .section("fee", "currency")
            .match("^(Eigene Spesen) (?<fee>[\\d.-]+,\\d+)- (?<currency>\\w{3}+)")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("fee"))))))
            .wrap(BuySellEntryItem::new);
    }

    private void addBuySellSavePlanTransaction()
    {
        DocumentType newType = new DocumentType(".*Ausgabe.*");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Ausgabe.*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            // Stück 13 COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
            // INHABER-ANTEILE I O.N.
            .section("isin", "wkn", "name", "shares", "nameContinued")
            .match("^(Stück) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Auftrag vom 05.12.2017 00:15:28 Uhr
            .section("date", "time")
            .match("^(Auftrag vom) (?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+) Uhr")
            .assign((t, v) -> {
                if (v.get("time") != null)
                    t.setDate(asDate(v.get("date"), v.get("time")));
                else
                    t.setDate(asDate(v.get("date")));
            })

            // Ausmachender Betrag 50,00- EUR
            .section("currency", "amount")
            .match("^(Ausmachender Betrag) (?<amount>[\\d.]+,\\d+)- (?<currency>\\w{3}+)")
            .assign((t, v) -> {
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(v.get("currency"));
            })

            .wrap(BuySellEntryItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType newType = new DocumentType(".*Gutschrift.*");
        this.addDocumentTyp(newType);

        Block block = new Block(".*Gutschrift.*");
        newType.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
            // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
            // REGISTERED SHARES USD O.N.
            .section("isin", "wkn", "name", "shares", "nameContinued")
            .match("^(Stück) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Ausmachender Betrag 68,87+ EUR
            .section("currency", "amount")
            .match("^(Ausmachender Betrag) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3}+)")
            .assign((t, v) -> {
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(v.get("currency"));
            })

            // Ex-Tag 14.12.2017 Herkunftsland Irland
            .section("date")
            .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}+).*")
            .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

            // Devisenkurs EUR / USD 1,2095
            // Devisenkursdatum 02.01.2018
            // Ausschüttung 113,16 USD 93,56+ EUR
            .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency")
            .match("^(Devisenkurs) .* (?<exchangeRate>[\\d.]+,\\d+)$")
            .match("^(Ausschüttung) (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})$")                        
            .assign((t, v) -> {
                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                {
                    exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                }

                newType.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

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
        
        addTaxesSectionsTransaction(pdfTransaction, newType);
        
        block.set(pdfTransaction);
    }
    
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction  
            // Kapitalertragsteuer 25 % auf 93,63 EUR 23,41- EUR
            .section("tax", "currency").optional()
            .match("^(Kapitalertragsteuer) [\\d.]+ .* (?<tax>[\\d.]+,\\d+)- (?<currency>\\w{3})$")
            .assign((t, v) -> processTaxEntries(t, v, type))
            
            // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
            .section("tax", "currency").optional()
            .match("^(Solidaritätszuschlag) [\\d.]+,\\d+ .* (?<tax>[\\d.]+,\\d+)- (?<currency>\\w{3})$")
            .assign((t, v) -> processTaxEntries(t, v, type));
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
}

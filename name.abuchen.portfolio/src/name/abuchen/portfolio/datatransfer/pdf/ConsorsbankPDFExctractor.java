package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class ConsorsbankPDFExctractor extends AbstractPDFExtractor
{
    public ConsorsbankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("Consorsbank"); //$NON-NLS-1$
        addBankIdentifier("Cortal Consors"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("KAUF");
        this.addDocumentTyp(type);

        Block block = new Block("^KAUF AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        block.set(pdfTransaction);
        pdfTransaction.subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        });

        pdfTransaction.section("wkn", "isin", "name", "currency") //
                        .find("(Wertpapier|Bezeichnung) WKN ISIN") //
                        .match("^(?<name>.*) (?<wkn>[^ ]*) (?<isin>[^ ]*)$") //
                        .match("(Kurs|Preis pro Anteil) (\\d+,\\d+) (?<currency>\\w{3}+) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Einheit Umsatz( F\\Dlligkeit)?") //
                        .match("^ST (?<shares>\\d+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Wert (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        
        addFeesSectionsTransaction(pdfTransaction);
    }
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("VERKAUF");
        this.addDocumentTyp(type);

        Block block = new Block("^VERKAUF AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        block.set(pdfTransaction);
        pdfTransaction.subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        });

        pdfTransaction.section("wkn", "isin", "name", "currency") //
                        .find("(Wertpapier|Bezeichnung) WKN ISIN") //
                        .match("^(?<name>.*) (?<wkn>[^ ]*) (?<isin>[^ ]*)$") //
                        .match("(Kurs|Preis pro Anteil) (\\d+,\\d+) (?<currency>\\w{3}+) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Einheit Umsatz( F\\Dlligkeit)?") //
                        .match("^ST (?<shares>\\d+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Wert (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        
        addFeesSectionsTransaction(pdfTransaction);
        addTaxesSectionsTransaction(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("DIVIDENDENGUTSCHRIFT");
        this.addDocumentTyp(type);

        Block block = new Block("DIVIDENDENGUTSCHRIFT.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("amount", "currency") //
                        .match("BRUTTO *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("rate", "amount", "currency").optional() //
                        .match("UMGER.ZUM DEV.-KURS *(?<rate>[\\d.]+,\\d+) *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            // replace BRUTTO (which is in foreign currency)
                            // with the value in transaction currency

                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            BigDecimal rate = asExchangeRate(v.get("rate"));

                            // forex currency has been set above ("BRUTTO")
                            BigDecimal converted = rate.multiply(BigDecimal.valueOf(amount.getAmount()));
                            Money forex = Money.of(t.getCurrencyCode(), Math.round(converted.doubleValue()));

                            BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 10, BigDecimal.ROUND_HALF_DOWN);
                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, inverseRate);

                            t.setMonetaryAmount(amount);
                            t.addUnit(grossValue);
                        })

                        .section("wkn", "name", "shares") //
                        .match("ST *(?<shares>\\d+(,\\d*)?) *WKN: *(?<wkn>\\S*) *") //
                        .match("^(?<name>.*)$") //
                        .assign((t, v) -> {
                            // reuse currency from transaction when creating a
                            // new security upon import
                            v.put("currency", t.getCurrencyCode());
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date") //
                        .match("WERT (?<date>\\d+.\\d+.\\d{4}+).*").assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new TransactionItem(t)));

        block = new Block("DIVIDENDENGUTSCHRIFT.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAXES);
                            return t;
                        })

                        .section("currency").optional()
                        .match("VERRECHNUNGSTOPF ALLGEMEIN NACH ERTRAG.*(?<currency>\\w{3}+).*")
                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                        .section("kapst", "currency").optional()
                        .match("KAPST .*(?<currency>\\w{3}+) *(?<kapst>[\\d.]+,\\d+) *").assign((t, v) -> {
                            t.setAmount(asAmount(v.get("kapst")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("solz", "currency").optional()
                        .match("SOLZ .*(?<currency>\\w{3}+) *(?<solz>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            if (currency.equals(t.getCurrencyCode()))
                                t.setAmount(t.getAmount() + asAmount(v.get("solz")));
                        })

                        .section("qust", "currency").optional() //
                        .match("QUST [\\d.]+,\\d+ *% *(?<currency>\\w{3}+) *(?<qust>[\\d.]+,\\d+) *\\w{3}+ *[\\d.]+,\\d+ *")
                        .assign((t, v) -> {
                            // if it is a foreign currency transaction and has
                            // quellensteuer, add to transaction
                            String currency = asCurrencyCode(v.get("currency"));
                            if (currency.equals(t.getCurrencyCode()))
                                t.setAmount(t.getAmount() + asAmount(v.get("qust")));
                        })

                        .section("date") //
                        .match("WERT (?<date>\\d+.\\d+.\\d{4}+).*").assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null));

    }

    private  <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax", "currency")
                        .optional()
                        .match("KAPST .*(?<currency>\\w{3}+) *(?<tax>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t)
                                                .addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("tax")))));
                            }
                        })
                        
                        .section("solz", "currency")
                        .optional()
                        .match("SOLZ .*(?<currency>\\w{3}+) *(?<solz>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t)
                                                .addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("solz")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("solz")))));
                            }
                        })
                        
                        .section("kirchenst", "currency")
                        .optional()
                        .match("KIST .*(?<currency>\\w{3}+) *(?<kirchenst>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t)
                                                .addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("kirchenst")))));
                            }
                        });
    }

    private void addFeesSectionsTransaction(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("currency", "stockfees").optional()
                        .match("(^.*)(B\\Drsenplatzgeb\\Dhr) (?<currency>\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("stockfees"))))))
                        
                        .section("currency", "brokerage").optional()
                        .match("(^.*)(Provision) (?<currency>\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("brokerage"))))))
                        
                        .section("currency", "fee").optional()
                        .match("(^.*)(Handelsentgelt) (?<currency>\\w{3}+) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))
                        
                        .section("currency", "basicfees").optional()
                        .match("(^.*)(Grundgeb\\Dhr) (?<currency>\\w{3}+) (?<basicfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("basicfees"))))))
        
                        .section("currency", "expenses").optional()
                        .match("(^.*)(Eig. Spesen) (?<currency>\\w{3}+) (?<expenses>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("expenses"))))));

    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

}

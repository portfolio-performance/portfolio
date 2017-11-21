package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

//import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class ConsorsbankPDFExctractor extends AbstractPDFExtractor
{
    public ConsorsbankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("Consorsbank"); //$NON-NLS-1$
        addBankIdentifier("Cortal Consors"); //$NON-NLS-1$

        addBuyTransaction();
        addPreemptiveBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
        addIncomeTransaction();
        addTaxAdjustmentTransaction();
    }
    
    @Override
    public String getPDFAuthor()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("KAUF");
        this.addDocumentTyp(type);

        Block block = new Block("^KAUF AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
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
                        .match("^ST (?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        
                        .section("date")
                        .match("KAUF AM (?<date>\\d+\\.\\d+\\.\\d{4}+).*")
                        .assign((t,v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency")
                        .match("Wert \\d+.\\d+.\\d{4}+ (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addPreemptiveBuyTransaction()
    {
        DocumentType type = new DocumentType("BEZUG");
        this.addDocumentTyp(type);

        Block block = new Block("^BEZUG AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
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
                        .match("^ST (?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Wert (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("VERKAUF");
        this.addDocumentTyp(type);

        Block block = new Block("^VERKAUF AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
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
                        .match("^ST (?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date")
                        .match("VERKAUF AM (?<date>\\d+\\.\\d+\\.\\d{4}+).*")
                        .assign((t,v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency")
                        .match("Wert \\d+.\\d+.\\d{4}+ (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

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
        block.set(newDividendTransaction(type));
    }

    @SuppressWarnings("nls")
    private void addIncomeTransaction()
    {
        DocumentType type = new DocumentType("ERTRAGSGUTSCHRIFT");
        this.addDocumentTyp(type);

        Block block = new Block("ERTRAGSGUTSCHRIFT.*");
        type.addBlock(block);
        block.set(newDividendTransaction(type));

    }

    @SuppressWarnings("nls")
    private Transaction<AccountTransaction> newDividendTransaction(DocumentType type)
    {
        return new Transaction<AccountTransaction>()

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

                        .section("wkn", "name", "shares") //
                        .match("ST *(?<shares>[\\d.]+(,\\d+)?) *WKN: *(?<wkn>\\S*) *") //
                        .match("^(?<name>.*)$") //
                        .assign((t, v) -> {
                            // reuse currency from transaction when creating a
                            // new security upon import
                            v.put("currency", t.getCurrencyCode());
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("rate", "amount", "currency").optional() //
                        .match("UMGER.ZUM DEV.-KURS *(?<rate>[\\d.]+,\\d+) *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            Money currentMonetaryAmount = t.getMonetaryAmount();
                            BigDecimal rate = asExchangeRate(v.get("rate"));
                            type.getCurrentContext().put("exchangeRate", rate.toPlainString());
                            BigDecimal accountMoneyValue = BigDecimal.valueOf(t.getAmount()).divide(rate,
                                            RoundingMode.HALF_DOWN);
                            String currencyCode = asCurrencyCode(v.get("currency"));
                            t.setMonetaryAmount(Money.of(currencyCode, asAmount(v.get("amount"))));
                            // transaction and security have different
                            // currencies -> add gross value
                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                Money accountMoney = Money.of(currencyCode,
                                                Math.round(accountMoneyValue.doubleValue()));
                                // replace BRUTTO (which is in foreign currency)
                                // with the value in transaction currency
                                BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 10, BigDecimal.ROUND_HALF_DOWN);
                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, accountMoney, currentMonetaryAmount,
                                                inverseRate);

                                t.addUnit(grossValue);
                            }
                        })

                        .section("kapst", "currency").optional()
                        .match("KAPST .*(?<currency>\\w{3}+) *(?<kapst>[\\d.]+,\\d+) *")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kapst"))))))

                        .section("solz", "currency").optional()
                        .match("SOLZ .*(?<currency>\\w{3}+) *(?<solz>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            if (currency.equals(t.getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX,
                                                Money.of(asCurrencyCode(currency), asAmount(v.get("solz")))));
                            }
                        })

                        .section("qust", "currency", "forexcurrency", "forex").optional() //
                        .match("QUST [\\d.]+,\\d+ *% *(?<currency>\\w{3}+) *(?<qust>[\\d.]+,\\d+) *(?<forexcurrency>\\w{3}+) *(?<forex>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            Optional<Unit> grossValueOption = t.getUnit(Type.GROSS_VALUE);
                            Money money = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("qust")));
                            if (grossValueOption.isPresent())
                            {
                                Money forex = Money.of(asCurrencyCode(v.get("forexcurrency")),
                                                asAmount(v.get("forex")));
                                t.addUnit(new Unit(Unit.Type.TAX, money, forex,
                                                grossValueOption.get().getExchangeRate()));
                            }
                            else
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, money));
                            }
                        })

                        .section("date") //
                        .match("WERT (?<date>\\d+.\\d+.\\d{4}+).*").assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("currency", "amount").optional() //
                        .match("WERT \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            String currencyCode = asCurrencyCode(v.get("currency"));
                            Money money = Money.of(currencyCode, asAmount(v.get("amount")));
                            t.setMonetaryAmount(money);
                        })

                        .section("currency", "forexpenses").optional()
                        .match("FREMDE SPESEN *(?<currency>\\w{3}+) *(?<forexpenses>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            Optional<Unit> grossValueOption = t.getUnit(Type.GROSS_VALUE);
                            long forexAmount = asAmount(v.get("forexpenses"));
                            if (grossValueOption.isPresent())
                            {
                                BigDecimal exchangeRate = grossValueOption.get().getExchangeRate();
                                long convertedMoney = Math.round(
                                                exchangeRate.multiply(BigDecimal.valueOf(forexAmount)).doubleValue());
                                Money money = Money.of(t.getCurrencyCode(), convertedMoney);
                                Money forex = Money.of(asCurrencyCode(v.get("currency")), forexAmount);
                                t.addUnit(new Unit(Unit.Type.TAX, money, forex,
                                                grossValueOption.get().getExchangeRate()));
                            }
                            else
                            {
                                BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
                                long convertedMoney = BigDecimal.valueOf(forexAmount)
                                                .divide(exchangeRate, RoundingMode.HALF_UP).longValue();
                                Money money = Money.of(t.getCurrencyCode(), convertedMoney);
                                t.addUnit(new Unit(Unit.Type.TAX, money));
                            }
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null);
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax", "currency").optional()
                        .match("KAPST .*(?<currency>\\w{3}+) *(?<tax>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX,
                                                Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("tax")))));
                            }
                        })

                        .section("solz", "currency").optional()
                        .match("SOLZ .*(?<currency>\\w{3}+) *(?<solz>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX,
                                                Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("solz")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("solz")))));
                            }
                        })

                        .section("kirchenst", "currency").optional()
                        .match("KIST .*(?<currency>\\w{3}+) *(?<kirchenst>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {
                            if (t instanceof name.abuchen.portfolio.model.Transaction)
                            {
                                ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX, Money
                                                .of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                            }
                            else
                            {
                                ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("kirchenst")))));
                            }
                        });
    }

    @SuppressWarnings("nls")
    private void addFeesSectionsTransaction(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("currency", "stockfees").optional()
                        .match("(^.*)(B\\Drsenplatzgeb\\Dhr) (?<currency>\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("stockfees"))))))

                        .section("currency", "brokerage").optional()
                        .match("(^.*)(Provision) (?<currency>\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("brokerage"))))))

                        .section("currency", "fee").optional()
                        .match("(^.*)(Handelsentgelt) (?<currency>\\w{3}+) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("currency", "basicfees").optional()
                        .match("(^.*)(Grundgeb\\Dhr) (?<currency>\\w{3}+) (?<basicfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("basicfees"))))))

                        .section("currency", "assetbasedfees").optional()
                        .match("(^.*)(Consorsbank Ausgabegeb.hr.*%) (?<currency>\\w{3}+) (?<assetbasedfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("assetbasedfees"))))))

                        .section("currency", "expenses").optional()
                        .match("(^.*)(Eig. Spesen) (?<currency>\\w{3}+) (?<expenses>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("expenses"))))));
    }
    
    @SuppressWarnings("nls")
    private void addTaxAdjustmentTransaction()
    {
        
        DocumentType type = new DocumentType("Nachträgliche Verlustverrechnung");
        this.addDocumentTyp(type);
        
        Block block = new Block(" Erstattung/Belastung \\(-\\) von Steuern");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            t.setCurrencyCode(CurrencyUnit.EUR); // nirgends im Dokument ist die Währung aufgeführt.
            return t;
        })
                        
                        // Den Steuerausgleich buchen wir mit Wertstellung 10.07.2017
                        .section("date")
                        .match(" *Den Steuerausgleich buchen wir mit Wertstellung (?<date>\\d+.\\d+.\\d{4}) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // Erstattung/Belastung (-) von Steuern
                        // Anteil                             100,00%
                        // KapSt Person 1                                 :                79,89
                        // SolZ  Person 1                                 :                 4,36
                        // KiSt  Person 1                                 :                 6,36
                        // ======================================================================
                        //                                                                 90,61
                        .section("amount")
                        .find(" *Erstattung/Belastung \\(-\\) von Steuern *")
                        .find(" *=* *")
                        .match(" *(?<amount>[\\d.]+,\\d{2}) *")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))
                        
                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

}

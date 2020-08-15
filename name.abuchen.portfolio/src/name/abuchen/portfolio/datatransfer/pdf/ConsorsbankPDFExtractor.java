package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
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

public class ConsorsbankPDFExtractor extends AbstractPDFExtractor
{
    public ConsorsbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Consorsbank"); //$NON-NLS-1$
        addBankIdentifier("Cortal Consors"); //$NON-NLS-1$

        addBuyTransaction();
        addPreemptiveBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
        addTaxAdjustmentTransaction();
        addVorabpauschaleTransaction();

        // documents since Q4 2017 look different
        addNewDividendTransaction();
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
                        .match("(Kurs|Preis pro Anteil) ([\\d.]+,\\d+) (?<currency>\\w{3}+) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Einheit Umsatz( F\\Dlligkeit)?") //
                        .match("^ST (?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "time")
                        .match("KAUF AM (?<date>\\d+\\.\\d+\\.\\d{4}+)\\s+UM (?<time>\\d+:\\d+):\\d+.*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("amount", "currency")
                        .match("Wert \\d+.\\d+.\\d{4}+ (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("forex", "exchangeRate", "currency", "amount").optional()
                        .match("umger. zum Devisenkurs *(?<forex>\\w{3}+) *(?<exchangeRate>[\\d.]+,\\d+) *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *") //
                        .assign((t, v) -> {

                            // read the forex currency, exchange rate, account
                            // currency and gross amount in account currency
                            String forex = asCurrencyCode(v.get("forex"));
                            if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                            {
                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                // gross given in account currency
                                long amount = asAmount(v.get("amount"));
                                long fxAmount = exchangeRate.multiply(BigDecimal.valueOf(amount))
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                                Money.of(asCurrencyCode(v.get("currency")), amount),
                                                Money.of(forex, fxAmount), reverseRate);

                                t.getPortfolioTransaction().addUnit(grossValue);
                            }
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
                        .match("(Kurs|Preis pro Anteil) ([\\d.]+,\\d+) (?<currency>\\w{3}+) .*")
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
                        .match("(Kurs|Preis pro Anteil) ([\\d.]+,\\d+) (?<currency>\\w{3}+) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Einheit Umsatz( F\\Dlligkeit)?") //
                        .match("^ST (?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "time")
                        .match("VERKAUF AM (?<date>\\d+\\.\\d+\\.\\d{4}+)\\s+UM (?<time>\\d+:\\d+:\\d+).*.*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

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
        DocumentType type = new DocumentType("(DIVIDENDENGUTSCHRIFT|ERTRAGSGUTSCHRIFT)");
        this.addDocumentTyp(type);

        Block block = new Block("(DIVIDENDENGUTSCHRIFT|ERTRAGSGUTSCHRIFT).*");
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
                                BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_DOWN);
                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, accountMoney, currentMonetaryAmount,
                                                inverseRate);

                                t.addUnit(grossValue);
                            }
                        })

                        .section("kapst", "currency").optional().multipleTimes()
                        .match("KAPST .*(?<currency>\\w{3}+) *(?<kapst>[\\d.]+,\\d+) *")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kapst"))))))

                        .section("solz", "currency").optional().multipleTimes()
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
                        .match("WERT (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

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

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax", "currency").optional().multipleTimes()
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

                        .section("solz", "currency").optional().multipleTimes()
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

                        .section("kirchenst", "currency").optional().multipleTimes()
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

                        .section("currency", "fee").optional()
                        .match("(^.*)(Transaktionsentgelt) (?<currency>\\w{3}+) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

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
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("expenses"))))))

                        .section("currency", "tax").optional()
                        .match("^Franzoesische Finanztransaktionssteuer .* (?<currency>\\w{3}) (?<tax>[\\d,.]+)$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))));
    }

    @SuppressWarnings("nls")
    private void addTaxAdjustmentTransaction()
    {

        DocumentType type = new DocumentType("Nachträgliche Verlustverrechnung");
        this.addDocumentTyp(type);

        Block block = new Block(" Erstattung/Belastung \\(-\\) von Steuern");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);

                            // nirgends im Dokument ist die Währung aufgeführt.
                            t.setCurrencyCode(CurrencyUnit.EUR);
                            return t;
                        })

                        // Den Steuerausgleich buchen wir mit Wertstellung
                        // 10.07.2017
                        .section("date").optional()
                        .match(" *Den Steuerausgleich buchen wir mit Wertstellung (?<date>\\d+.\\d+.\\d{4}) .*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Erstattung/Belastung (-) von Steuern
                        // Anteil                             100,00%
                        // KapSt Person 1                                 :                79,89
                        // SolZ  Person 1                                 :                 4,36
                        // KiSt  Person 1                                 :                 6,36
                        // ======================================================================
                        //                                                                 90,61
                        // @formatter:on

                        .section("amount", "sign") //
                        .find(" *Erstattung/Belastung \\(-\\) von Steuern *") //
                        .find(" *=* *") //
                        .match(" *(?<amount>[\\d.]+,\\d{2})(?<sign>-?).*") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));

                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        .wrap(t -> {
                            if (t.getDateTime() == null)
                            {
                                if (t.getAmount() == 0L)
                                    return new NonImportableItem("Erstattung/Belastung von Steuern mit 0 Euro");
                                else
                                    throw new IllegalArgumentException(Messages.MsgErrorMissingDate);
                            }
                            else
                            {
                                return new TransactionItem(t);
                            }
                        }));
    }

    @SuppressWarnings("nls")
    private void addNewDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("(Dividendengutschrift|Ertragsgutschrift).*");
        type.addBlock(block);

        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("name", "wkn", "isin", "currency") //
                        .find("Wertpapierbezeichnung WKN ISIN") //
                        .match("(?<name>.*) (?<wkn>[^ ]*) (?<isin>[^ ]*)$") //
                        .match(".*(Dividende pro St.ck|Ertragsaussch.ttung je Anteil) ([\\d.]+,\\d+) (?<currency>\\w{3}+).*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency") //
                        .match("Netto.* zugunsten IBAN (.*) (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("shares") //
                        .match("(?<shares>[\\d.]+(,\\d+)?) Stück") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional() //
                        .match("Brutto in (\\w{3}+) (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}+)")
                        .match("Devisenkurs (?<exchangeRate>[\\d.]+,\\d+) (\\w{3}+) / (\\w{3}+)") //
                        .match("Brutto in (\\w{3}+) (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            
                            // Example: Devisenkurs 1,12000 USD / EUR
                            // check which currency is transaction currency and use exchange rate accordingly
                            // if transaction currency is e.g. USD, we need to inverse the rate
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency")))) 
                            {
                                exchangeRate  = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }
                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                // check, if forex currency is transaction currency or not and swap amount, if necessary
                                Unit grossValue;
                                if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode())) 
                                {
                                    Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                    asAmount(v.get("fxAmount")));
                                    Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                                } 
                                else 
                                {
                                    Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                    asAmount(v.get("fxAmount")));
                                    Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                                }
                                t.addUnit(grossValue);
                            }
                        })

                        .section("tax", "currency").optional() //
                        .match("abzgl. Quellensteuer .* (\\w{3}+) (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type);
                        })

                        .section("tax", "currency").optional() //
                        .match("abzgl. Kapitalertragsteuer.* (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type);
                        })

                        .section("tax", "currency").optional() //
                        .match("abzgl. Solidaritätszuschlag.* (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type);
                        })

                        .section("tax", "currency").optional() //
                        .match("abzgl. Kirchensteuer.* (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type);
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null));

    }
    
    @SuppressWarnings("nls")
    private void checkAndSetTax(Money tax, AccountTransaction t, DocumentType type) 
    {
        if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
        {
            t.addUnit(new Unit(Unit.Type.TAX, tax));
        }
        else if (type.getCurrentContext().containsKey("exchangeRate"))
        {
            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

            Money txTax = Money.of(t.getCurrencyCode(),
                            BigDecimal.valueOf(tax.getAmount()).multiply(inverseRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

            // store tax value in both currencies, if
            // security's currency
            // is different to transaction currency
            if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
            {
                t.addUnit(new Unit(Unit.Type.TAX, txTax));
            }
            else
            {
                t.addUnit(new Unit(Unit.Type.TAX, txTax, tax, inverseRate));
            }
        }
    }

    @SuppressWarnings("nls")
    private void addVorabpauschaleTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Block block = new Block("^Vorabpauschale");
        type.addBlock(block);

        Transaction<AccountTransaction> vorabpauschaleTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("name", "wkn", "isin") //
                        .match("Wertpapierbezeichnung WKN ISIN") //
                        .match("(?<name>.*) (?<wkn>[^ ]*) (?<isin>[^ ]*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("tax", "currency", "date").optional()
                        .match("Netto zulasten .* (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .match("Valuta (?<date>\\d+\\.\\d+\\.\\d{4}+) .*")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null);

        block.set(vorabpauschaleTransaction);
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

}

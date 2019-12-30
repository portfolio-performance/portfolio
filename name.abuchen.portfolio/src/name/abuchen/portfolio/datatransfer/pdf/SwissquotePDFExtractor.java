package name.abuchen.portfolio.datatransfer.pdf;

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

public class SwissquotePDFExtractor extends SwissBasedPDFExtractor
{
    public SwissquotePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Swissquote"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendsTransaction();
        addFeeTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Kauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "isin", "shares", "currency", "transactionCurrency", "amount")
                        .find("Titel Ort der Ausführung") //
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .match("^Zu Ihren Lasten (?<transactionCurrency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("transactionCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("feecurrency", "fee") //
                        .optional() //
                        .match("^Kommission Swissquote Bank AG (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })

                        .section("taxcurrency", "tax") //
                        .optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .section("stockfeecurrency", "stockfee") //
                        .optional() //
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")),
                                            asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })

                        // buy foreign shares (with exchange rate)
                        .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount") //
                        .optional() //
                        .match("^Total (?<forexCurrency>\\w{3}+) (?<forexAmount>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^(?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$").assign((t, v) -> {
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                            asAmount(v.get("forexAmount")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                Unit unit;
                                // Swissquote sometimes uses scaled exchanges
                                // rates (such as DKK/CHF 15.42, instead of
                                // 0.1542,
                                // hence we try to extract and if we fail, we
                                // calculate the exchange rate
                                try
                                {
                                    unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                                }
                                catch (IllegalArgumentException e)
                                {
                                    exchangeRate = BigDecimal.valueOf(((double) gross.getAmount()) / forex.getAmount());
                                    unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                                }
                                t.getPortfolioTransaction().addUnit(unit);
                            }
                        })

                        .section("date")
                        .match("^Betrag belastet auf Kontonummer\\s+(\\d+),\\s+Valutadatum\\s(?<date>\\d+.\\d+.\\d{4}+)$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Verkauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "isin", "shares", "currency", "transactionCurrency", "amount")
                        .find("Titel Ort der Ausführung") //
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .match("^Zu Ihren Gunsten (?<transactionCurrency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("transactionCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("feecurrency", "fee").optional()
                        .match("^Kommission Swissquote Bank AG (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })

                        .section("taxcurrency", "tax").optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .section("stockfeecurrency", "stockfee").optional()
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")),
                                            asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })

                        // buy foreign shares (with exchange rate)
                        .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount") //
                        .optional() //
                        .match("^Total (?<forexCurrency>\\w{3}+) (?<forexAmount>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^(?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$").assign((t, v) -> {
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                            asAmount(v.get("forexAmount")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                Unit unit;
                                // Swissquote sometimes uses scaled exchanges
                                // rates (such as DKK/CHF 15.42, instead of
                                // 0.1542,
                                // hence we try to extract and if we fail, we
                                // calculate the exchange rate
                                try
                                {
                                    unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                                }
                                catch (IllegalArgumentException e)
                                {
                                    exchangeRate = BigDecimal.valueOf(((double) gross.getAmount()) / forex.getAmount());
                                    unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                                }
                                t.getPortfolioTransaction().addUnit(unit);
                            }
                        })

                        .section("date")
                        .match("^Betrag gutgeschrieben auf Ihrer Kontonummer\\s+(\\d+),\\s+Valutadatum\\s(?<date>\\d+.\\d+.\\d{4}+)$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendsTransaction()
    {

        Transaction<AccountTransaction> trans = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        })

                        .section("name", "isin").match("^(?<name>.*) ISIN: (?<isin>\\S*)NKN.*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match("^Ausführungsdatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("gross", "grosscurrency", "amount", "currency", "shares")
                        .match("^Anzahl (?<shares>[\\d+',.]*)$")
                        .match("^Betrag (?<grosscurrency>\\w{3}+) (?<gross>[\\d+',.]*)$")
                        .match("^Total (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("shares")));
                            t.getSecurity().setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            long gross = asAmount(v.get("gross"));
                            long tax = gross - t.getAmount();
                            Unit unit = new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax));
                            if (unit.getAmount().getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        .wrap(TransactionItem::new);

        DocumentType typeDividend = new DocumentType("Dividende");
        this.addDocumentTyp(typeDividend);

        Block blockDividend = new Block("^Dividende Unsere Referenz(.*)$");
        typeDividend.addBlock(blockDividend);
        blockDividend.set(trans);

        DocumentType typeCapitalGain = new DocumentType("Kapitalgewinn");
        this.addDocumentTyp(typeCapitalGain);

        Block blockCapitalGain = new Block("^Kapitalgewinn Unsere Referenz(.*)$");
        typeCapitalGain.addBlock(blockCapitalGain);
        blockCapitalGain.set(trans);
    }

    @SuppressWarnings("nls")
    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Depotgebühren");
        this.addDocumentTyp(type);

        Block block = new Block("^Depotgebühren Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                        .match("^Betrag belastet (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Swissquote"; //$NON-NLS-1$
    }
}

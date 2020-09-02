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

public class ViacPDFExtractor extends SwissBasedPDFExtractor
{
    public ViacPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Terzo"); //$NON-NLS-1$

        addDepositTransaction();
        addDepositTransactionEnglish();
        addBuyTransaction();
        addBuyTransactionEnglish();
        addSellTransaction();
        addSellTransactionEnglish();
        addInterestTransaction();
        addInterestTransactionEnglish();
        addFeeTransaction();
        addFeeTransactionEnglish();
        addDividendsTransaction();
        addDividendsTransactionEnglish();
        addTaxRefundTransaction();
    }

    @SuppressWarnings("nls")
    private void addDepositTransaction()
    {
        DocumentType type = new DocumentType("Einzahlung 3a");
        this.addDocumentTyp(type);

        Block block = new Block("Einzahlung 3a");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DEPOSIT);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Einzahlung 3a") //
                        .match("Gutschrift: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addDepositTransactionEnglish()
    {
        DocumentType type = new DocumentType("Deposit 3a");
        this.addDocumentTyp(type);

        Block block = new Block("Deposit 3a");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DEPOSIT);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Deposit 3a") //
                        .match("Credit: Value date (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung - Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("B.rsenabrechnung - Kauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name", "currency", "shares") //
                        .find("Order: Kauf") //
                        .match("(?<shares>[\\d+,.]*) Ant (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Kurs: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Verrechneter Betrag: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("tax", "currency").optional() //
                        .match("Stempelsteuer (?<currency>\\w{3}+) (?<tax>[\\d+',.]*)") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));

                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Betrag (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Umrechnungskurs CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addBuyTransactionEnglish()
    {
        DocumentType type = new DocumentType("Exchange Settlement - Buy");
        this.addDocumentTyp(type);

        Block block = new Block("Exchange Settlement - Buy");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name", "currency", "shares") //
                        .find("Order: Buy") //
                        .match("(?<shares>[\\d+,.]*) Qty (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Price: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Charged amount: Value date (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("tax", "currency").optional() //
                        .match("Stamp duty (?<currency>\\w{3}+) (?<tax>[\\d+',.]*)") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));

                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Amount (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Exchange rate CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("abrechnung - Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("B.rsenabrechnung - Verkauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("isin", "name", "currency", "shares") //
                        .find("Order: Verkauf") //
                        .match("(?<shares>[\\d+,.]*) Ant (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Kurs: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Verrechneter Betrag: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Betrag (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Umrechnungskurs CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransactionEnglish()
    {
        DocumentType type = new DocumentType("Exchange Settlement - Sell");
        this.addDocumentTyp(type);

        Block block = new Block("Exchange Settlement - Sell");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("isin", "name", "currency", "shares") //
                        .find("Order: Sell") //
                        .match("(?<shares>[\\d+,.]*) Qty (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Price: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Charged amount: Value date (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Amount (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Exchange rate CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("Zins");
        this.addDocumentTyp(type);

        Block block = new Block("Zins");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.INTEREST);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Zins") //
                        .match("Am (?<date>\\d+.\\d+.\\d{4}+) haben wir Ihrem Konto gutgeschrieben:") //
                        .match("Zinsgutschrift: (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addInterestTransactionEnglish()
    {
        DocumentType type = new DocumentType("Interest");
        this.addDocumentTyp(type);

        Block block = new Block("Interest");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.INTEREST);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Interest") //
                        .match("On (?<date>\\d+.\\d+.\\d{4}+) we have credited your account:") //
                        .match("Interest credit: (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Belastung");
        this.addDocumentTyp(type);

        Block block = new Block("Belastung");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Belastung") //
                        .match("Verrechneter Betrag: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeeTransactionEnglish()
    {
        DocumentType type = new DocumentType("Commission");
        this.addDocumentTyp(type);

        Block block = new Block("Commission");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .find("Commission") //
                        .match("Charged amount: Value date (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("Dividendenaussch");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendenart: Ordentliche Dividende");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("shares", "name", "isin", "currency") //
                        .match("(?<shares>[\\d+,.]*) Ant (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Aussch.ttung: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Gutgeschriebener Betrag: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Betrag (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Umrechnungskurs CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendsTransactionEnglish()
    {
        DocumentType type = new DocumentType("Dividend Payment");
        this.addDocumentTyp(type);

        Block block = new Block("Type of dividend: Ordinary dividend");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("shares", "name", "isin", "currency") //
                        .match("(?<shares>[\\d+,.]*) Qty (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Dividend payment: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Amount credited: Value date (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional() //
                        .match("Amount (?<forexCurrency>\\w{3}+) (?<forex>[\\d+',.]*)")
                        .match("Exchange rate CHF/\\w{3}+ (?<exchangeRate>[\\d+',.]*) (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)")
                        .assign((t, v) -> {

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // only add gross value with forex if the security
                            // is actually denoted in the foreign currency
                            // (often users actually have the quotes in their
                            // home country currency)
                            if (forex.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    
    @SuppressWarnings("nls")
    private void addTaxRefundTransaction()
    {
        DocumentType type = new DocumentType("Dividendenaussch");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendenart: R.ckerstattung Quellensteuer");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return transaction;
                        })

                        .section("shares", "name", "isin", "currency") //
                        .match("(?<shares>[\\d+,.]*) Ant (?<name>.*)$") //
                        .match("ISIN: (?<isin>\\S*)") //
                        .match("Aussch.ttung: (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency") //
                        .match("Gutgeschriebener Betrag: Valuta (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>-?[\\d+',.]*)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                t.setNote(t.getSecurity().getName());
                                t.setSecurity(null);
                                t.setShares(0L);
                            }
                        })

                        .wrap(TransactionItem::new));
    }


    @Override
    public String getLabel()
    {
        return "VIAC"; //$NON-NLS-1$
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Locale;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class TwinoPDFExtractor extends AbstractPDFExtractor
{
    public TwinoPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("INCOME STATEMENT");
        addStatementTransactions();
    }

    @Override
    public String getLabel()
    {
        return "Twino";
    }

    private void addStatementTransactions()
    {
        final var type = new DocumentType("INCOME STATEMENT", documentContext -> documentContext
                        .section("date")
                        .match("^for the period from [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} to (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        addDocumentTyp(type);

        // The monthly statement gives period totals. Use the period end as the
        // transaction date and ignore account balances and aggregate totals.

        // - Deposits to the investor account: EUR 2000.00
        var depositBlock = new Block("^\\s*-\\s*Deposits to the investor account: [A-Z]{3} [-+]?[\\d.,]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))
                        .section("currency", "amount")
                        .documentContext("date")
                        .match("^\\s*-\\s*Deposits to the investor account: (?<currency>[A-Z]{3}) (?<amount>[-+]?[\\d.,]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), Locale.US));
                        })
                        .wrap(TransactionItem::new));

        // - Withdrawals from the investor account: EUR 0.00
        var withdrawalBlock = new Block("^\\s*-\\s*Withdrawals from the investor account: [A-Z]{3} [-+]?[\\d.,]+$");
        type.addBlock(withdrawalBlock);
        withdrawalBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))
                        .section("currency", "amount")
                        .documentContext("date")
                        .match("^\\s*-\\s*Withdrawals from the investor account: (?<currency>[A-Z]{3}) (?<amount>[-+]?[\\d.,]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), Locale.US));
                        })
                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);
                            return new TransactionItem(t);
                        }));

        // - Inactivity fee: EUR 0.00
        var feeBlock = new Block("^\\s*-\\s*Inactivity fee: [A-Z]{3} [-+]?[\\d.,]+$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))
                        .section("currency", "amount")
                        .documentContext("date")
                        .match("^\\s*-\\s*Inactivity fee: (?<currency>[A-Z]{3}) (?<amount>[-+]?[\\d.,]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), Locale.US));
                            t.setNote("Inactivity fee");
                        })
                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);
                            return new TransactionItem(t);
                        }));

        // @formatter:off
        // - Net income from claim rights: EUR 0.00
        // • Interest income from claim rights: EUR 0.00
        // • Gain/loss from secondary market: EUR 0.00
        // • Gain/loss from currency fluctuations: EUR 0.00
        //
        // - Net income from Loan Securities: EUR 0.85
        // • Interest income from Loan Securities: EUR 0.90
        // • Tax withheld: EUR -0.05
        // • Gain/loss from secondary market: EUR 0.00
        // • Gain/loss from currency fluctuations: EUR 0.00
        //
        // - Net income from Real estate Securities: EUR 0.00
        // • Dividend income: EUR 0.00
        // • Gain/loss from secondary market: EUR 0.00
        // • Gain/loss from currency fluctuations: EUR  0.00
        // @formatter:on
        var incomeBlock = new Block("^\\s*-\\s*Net income from (claim rights|Loan Securities|Real estate Securities): [A-Z]{3} [-+]?[\\d.,]+$");
        type.addBlock(incomeBlock);
        incomeBlock.setMaxSize(5);
        incomeBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))
                        .section("source", "currency", "net", "interest")
                        .documentContext("date")
                        .match("^\\s*-\\s*Net income from (?<source>claim rights|Loan Securities|Real estate Securities): (?<currency>[A-Z]{3}) (?<net>[-+]?[\\d.,]+)$")
                        .match("^\\s*•\\s*(?:Interest income from (?:claim rights|Loan Securities)|Dividend income): [A-Z]{3} (?<interest>[-+]?[\\d.,]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setMonetaryAmount(Money.of(t.getCurrencyCode(), asAmount(v.get("net"), Locale.US)));
                            t.setNote(v.get("source"));
                        })
                        .section("tax").optional()
                        .match("^\\s*•\\s*Tax withheld: [A-Z]{3} -(?<tax>[\\d.,]+)$")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(t.getCurrencyCode(), asAmount(v.get("tax"), Locale.US)))))
                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);
                            return new TransactionItem(t);
                        }));

        // - Income from campaigns (cashback, referral): EUR 20.00
        var campaignBlock = new Block("^\\s*-\\s*Income from campaigns \\(cashback, referral\\): [A-Z]{3} [-+]?[\\d.,]+$");
        type.addBlock(campaignBlock);
        campaignBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))
                        .section("currency", "amount")
                        .documentContext("date")
                        .match("^\\s*-\\s*Income from campaigns \\(cashback, referral\\): (?<currency>[A-Z]{3}) (?<amount>[-+]?[\\d.,]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), Locale.US));
                            t.setNote("Income from campaigns (cashback, referral)");
                        })
                        .wrap(TransactionItem::new));
    }
}

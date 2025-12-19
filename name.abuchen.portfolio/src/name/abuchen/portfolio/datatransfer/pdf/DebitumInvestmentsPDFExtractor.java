package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote The currency of Debitum Investments is always EUR.
 *
 * @implSpec All account transactions are in EUR.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class DebitumInvestmentsPDFExtractor extends AbstractPDFExtractor
{
    private static final String EUR = "EUR";

    public DebitumInvestmentsPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("https://debitum.investments");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Debitum Investments";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("ACCOUNT STATEMENT", //
                        documentContext -> documentContext //
                                        .section("date") //
                                        .match("^Report date to \\(inclusive\\): (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})[\\s]*$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);


        // @formatter:off
        // DEPOSITS during reporting period: 4170.0
        // WITHDRAWALS during reporting period: 0.0
        // @formatter:on
        var depositRemovalBlock = new Block("^(DEPOSITS|WITHDRAWALS) during reporting period: [\\.\\d]+[\\s]*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("type", "amount") //
                        .documentContext("date") //
                        .match("^(?<type>(DEPOSITS|WITHDRAWALS)) during reporting period: (?<amount>[\\.\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "WITHDRAWALS" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("WITHDRAWALS".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(EUR));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> {
                            if (t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Bonus and cashback should be counted as a kind of interest as this is
        // usually based on money and taxes are withheld for that amount too.
        // Taxes are already included in the tax-section, but bonus is listed
        // extra.
        //
        // Other balance increases during reporting period: 0.0
        // Paid interest** 1.83
        // Income tax withheld -0.09
        // @formatter:on
        var interestBlock = new Block("^.*Report date to \\(inclusive\\).*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Other balance increases during reporting period: 0.0
                        // Paid interest** 1.83
                        // @formatter:on
                        .section("bonus", "amount") //
                        .documentContext("date") //
                        .match("^Other balance increases during reporting period: (?<bonus>[\\.\\d]+)[\\s]*$") //
                        .match("^Paid interest\\*\\* (?<amount>[\\.\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            var bonus = Money.of(asCurrencyCode(EUR), asAmount(v.get("bonus")));
                            var amount = Money.of(asCurrencyCode(EUR), asAmount(v.get("amount")));

                            t.setDateTime(asDate(v.get("date")));
                            t.setMonetaryAmount(amount.add(bonus));
                        })

                        // @formatter:off
                        // Income tax withheld -1.38
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Income tax withheld \\-(?<tax>[\\.\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(EUR), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }
}

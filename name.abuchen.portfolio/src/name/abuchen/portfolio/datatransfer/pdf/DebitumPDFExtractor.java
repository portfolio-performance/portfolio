package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class DebitumPDFExtractor extends AbstractPDFExtractor
{
    private static final String SECTION_AMOUNT = "amount";
    private static final String REPORT_DATE_BLOCK_REGEX = "^.*Report date to \\(inclusive\\).*$";

    public DebitumPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("SIA DN Operator");

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
                                        .section("date")
                                        .match("^Report date to \\(inclusive\\): (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$")
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        var depositBlock = new Block(REPORT_DATE_BLOCK_REGEX);
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            accountTransaction.setCurrencyCode(CurrencyUnit.EUR);
                            return accountTransaction;
                        })

                        .section(SECTION_AMOUNT) //
                        .documentContext("date") //
                        .match("^DEPOSITS during reporting period: (?<amount>[\\.\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get(SECTION_AMOUNT), "en", "US"));
                            t.setDateTime(asDate(v.get("date")));
                        }).wrap(t -> {
                            if (t.getAmount() > 0)
                                return new TransactionItem(t);
                            else
                                return null;
                        }));

        /*
         * Bonus and cashback should be counted as a kind of interest as this is
         * usually based on money and taxes are withheld for that amount too.
         * Taxes are already included in the tax-section, but bonus is listed
         * extra.
         */
        var interestsAndTaxesBlock = new Block(REPORT_DATE_BLOCK_REGEX);
        type.addBlock(interestsAndTaxesBlock);
        interestsAndTaxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            accountTransaction.setCurrencyCode(CurrencyUnit.EUR);
                            return accountTransaction;
                        })

                        .section(SECTION_AMOUNT, "bonus", "taxes") //
                        .documentContext("date") //
                        .match("^Other balance increases during reporting period: (?<bonus>[\\.\\d\\s]+)$") //
                        .match("^Paid interest\\*\\* (?<amount>[\\.\\d\\s]+)$") //
                        .match("^Income tax withheld (?<taxes>\\-[\\.\\d\\s]+)$") //
                        .assign((t, v) -> {
                            var interests = asAmount(v.get(SECTION_AMOUNT), "en", "US");
                            var bonus = asAmount(v.get("bonus"), "en", "US");
                            var taxes = asAmount(v.get("taxes"), "en", "US");

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(interests + bonus - taxes);
                            t.addUnit(new Unit(Type.TAX, Money.of(CurrencyUnit.EUR, taxes)));
                        }).wrap(TransactionItem::new));

        var withdrawlsBlock = new Block(REPORT_DATE_BLOCK_REGEX);
        type.addBlock(withdrawlsBlock);
        withdrawlsBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            accountTransaction.setCurrencyCode(CurrencyUnit.EUR);
                            return accountTransaction;
                        })

                        .section(SECTION_AMOUNT) //
                        .documentContext("date") //
                        .match("^WITHDRAWALS during reporting period: (?<amount>[\\.\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get(SECTION_AMOUNT), "en", "US"));
                        }).wrap(t -> {
                            if (t.getAmount() > 0)
                                return new TransactionItem(t);
                            else
                                return null;
                        }));

    }
}

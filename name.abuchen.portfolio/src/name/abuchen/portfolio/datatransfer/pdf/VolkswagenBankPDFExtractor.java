package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class VolkswagenBankPDFExtractor extends AbstractPDFExtractor
{
    public VolkswagenBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Volkswagen Bank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Volkswagen Bank";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug / Saldenmitteilung", //
                        documentContext -> documentContext //
                        // @formatter:off
                                        // Alter Kontostand in EUR: 1.001,29
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Alter Kontostand in (?<currency>[\\w]{3}):.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 1 11.03.2022 Gutschrift 11.03.2022 500,00
        // 1 06.01.2026 Gutschrift Echtzeit 05.01.2026 50,00
        // @formatter:on
        Block depositBlock = new Block(
                        "^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Gutschrift (Echtzeit )?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Gutschrift (Echtzeit )?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 1 21.08.2023 Telebanking Belastung 22.08.2023 -1,00
        // 3 23.08.2023 Belastung 23.08.2023 -1,00
        // @formatter:on
        Block removalBlock = new Block(
                        "^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Telebanking )?Belastung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Telebanking )?Belastung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\-(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 1 23.12.2021 Habenzinsen 25.12.2021 0,83
        // 2 23.12.2021 Solidaritätszuschlag 25.12.2021 -0,01
        // 3 23.12.2021 Kirchensteuer 25.12.2021 -0,01
        // 4 23.12.2021 Abgeltungsteuer 25.12.2021 -0,20
        // @formatter:on
        Block interestBlock = new Block(
                        "^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Habenzinsen [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Habenzinsen (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Abgeltungsteuer [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Solidarit.tszuschlag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Kirchensteuer [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));
    }
}

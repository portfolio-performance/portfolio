package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class BundesschatzPDFExtractor extends AbstractPDFExtractor
{
    public BundesschatzPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bundesschatz");

        addDepositRemovalTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bundesschatz";
    }

    private void addDepositRemovalTransaction()
    {
        final var type = new DocumentType("Ein- und Auszahlungen Bundesschatz", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung: EUR Erstellt am: 16.01.2026 22:09:40
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung: (?<currency>[A-Z]{3}) Erstellt am:.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", v.get("currency"))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 30.12.2025 Einzahlung 5.000,00 AT745170342310484364
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Einzahlung [\\.,\\d]+ .*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Einzahlung (?<amount>[\\.,\\d]+) (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 12.01.2026 Auszahlung -2.531,91 AT811215280026188132
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Auszahlung -[\\.,\\d]+ .*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Auszahlung -(?<amount>[\\.,\\d]+) (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }


    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug Bundesschatz", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung: EUR Erstellt am: 16.01.2026 22:09:40
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung: (?<currency>[A-Z]{3}) Erstellt am:.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", v.get("currency"))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 1 Monat 10.11.2025 10.12.2025 2.526,06 1,85 % 3,84 27,5% 1,06 2,78 2.528,84 ja
        // @formatter:on
        var interestBlock = new Block("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " + //
                        "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " + //
                        "[\\.,\\d]+ " + //
                        "[\\.,\\d]+[\s]*% " + //
                        "[\\.,\\d]+ " + //
                        "[\\.,\\d]+[\\s]*% " + //
                        "[\\.,\\d]+ " + //
                        "[\\.,\\d]+ " + //
                        "[\\.,\\d]+ " + //
                        "ja$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note1", "note2", "date", "tax", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note1>.*) (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " + //
                                        "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " + //
                                        "[\\.,\\d]+ " + //
                                        "[\\.,\\d]+[\\s]*% " + //
                                        "[\\.,\\d]+ " + //
                                        "[\\.,\\d]+[\\s]*% " + //
                                        "(?<tax>[\\.,\\d]+) " + //
                                        "(?<amount>[\\.,\\d]+) " + //
                                        "[\\.,\\d]+ " + //
                                        "ja$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note2") + " - " + v.get("date") + " (" + trim(v.get("note1")) + ")");

                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 12.01.2026 Auszahlung -2.531,91 AT811215280026188132
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Auszahlung -[\\.,\\d]+ .*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Auszahlung -(?<amount>[\\.,\\d]+) (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }


}

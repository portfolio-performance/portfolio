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
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 30.12.2025 Einzahlung 5.000,00 AT745170342310484364
        // 12.01.2026 Auszahlung -2.531,91 AT811215280026188132
        // @formatter:on
        var depositRemovalBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Einzahlung|Auszahlung) (\\-)?[\\.,\\d]+ .*$");
        type.addBlock(depositRemovalBlock_Format01);
        depositRemovalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount", "type", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Einzahlung|Auszahlung)(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+) (?<note>.*)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 05.05.2026 Einzahlung AT74 5170 3423 1048 4364 41 1.000,00
        // @formatter:on
        var depositRemovalBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Einzahlung|Auszahlung) .*\\s(\\-)?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock_Format02);
        depositRemovalBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount", "note", "type") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Einzahlung|Auszahlung) (?<note>.*)(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

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
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 1 Monat 10.11.2025 10.12.2025 2.526,06 1,85 % 3,84 27,5% 1,06 2,78 2.528,84 ja
        // @formatter:on
        var interestBlock = new Block("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "[\\.,\\d]+ [\\.,\\d]+[\\s]*% [\\.,\\d]+ [\\.,\\d]+[\\s]*% " //
                        + "[\\.,\\d]+ " //
                        + "[\\.,\\d]+ " //
                        + "[\\.,\\d]+ ja$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("note1", "note2", "date", "tax", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note1>.*) (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " + //
                                        "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " + //
                                        "[\\.,\\d]+ [\\.,\\d]+[\\s]*% [\\.,\\d]+ [\\.,\\d]+[\\s]*% " + //
                                        "(?<tax>[\\.,\\d]+) " + //
                                        "(?<amount>[\\.,\\d]+) " + //
                                        "[\\.,\\d]+ " + //
                                        "ja$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note2") + " - " + v.get("date") + " (" + trim(v.get("note1")) + ")");

                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));
    }


}

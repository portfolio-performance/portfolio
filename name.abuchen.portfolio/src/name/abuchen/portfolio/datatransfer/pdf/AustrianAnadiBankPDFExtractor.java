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
public class AustrianAnadiBankPDFExtractor extends AbstractPDFExtractor
{
    public AustrianAnadiBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("anadibank.at");
        addBankIdentifier("HAABAT2K");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Austrian Anadi Bank AG";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Mustermann EUR 4.348,95
                                        // @formatter:on
                                        .section("currency") //
                                        .find("^Neuer Saldo zu Ihren Gunsten.*$") //
                                        .match("^.* (?<currency>[A-Z]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))
                                        // @formatter:off
                                        // Inglitschstraße 5A, 050202-0 vom 30.05.2025
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.*[\\d]{6}\\-[\\d] vom [\\d]{2}.[\\d]{2}.(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 14.05 FTHcZZ trSlPNt, KsI 15.05 2.500,00
        // IBAN: TT40 1705 1256 3324 8502
        // Zahlungsreferenz: Festgeld
        // REF: PSSAAT140525000665510002688
        // @formatter:on
        var depositBlock_Format01 = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (?!Abschluss)(?!ABRECHNUNG ZU KONTO).* [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$");
        type.addBlock(depositBlock_Format01);
        depositBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) .* [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$") //)
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + '.' + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("note").optional() //
                        .match("^(?<note>IBAN: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 17.11 ABRECHNUNG ZU KONTO 00123456678 17.11 2.522,75
        // VERANLAGUNGSBETRAG             2.500,00
        // LAUFZEIT        2025-05-15 - 2025-11-17
        // Habenzinsen
        // AB 2025-05-15          2,4000%              30,33
        // KESt                                         7,58-
        // @formatter:on
        var depositBlock_Format02 = new Block("^[\\d]{1,2}\\.[\\d]{1,2} ABRECHNUNG ZU KONTO .* [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$");
        type.addBlock(depositBlock_Format02);
        depositBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) ABRECHNUNG ZU KONTO (?<note>[\\d]+) [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$") //
                        .match("^VERANLAGUNGSBETRAG[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> { //
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Abrechnung zu Konto: " + v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 16.12 IBAN: AT12 1234 1234 1234 1234 16.12 2.000,00-
        // Max Mustermann
        // REF: 52000251215EBP00000243646135
        //
        // 15.05 ONLINE-FESTGELD / KONTO 12345555555 15.05 2.500,00-
        //
        // 16.12 IBAN: AT12 1234 1234 1234 1234 16.12 2.000,00-
        // @formatter:on
        var removalBlock = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (?!Abschluss)(?!ABRECHNUNG ZU KONTO).* [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) .* [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + '.' + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("note").optional() //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} (?<note>(ONLINE-FESTGELD|IBAN:).*) [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+-$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.12 Abschluss 31.12 123,91
        // Habenzinsen                                165,21
        // KESt                                        41,30-
        // @formatter:on
        var interestBlock_Format01 = new Block("^[\\d]{1,2}\\.[\\d]{1,2} Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$");
        type.addBlock(interestBlock_Format01);
        interestBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>Abschluss [\\d]{1,2}\\.[\\d]{1,2}) (?<amount>[\\.,\\d]+)$") //
                        .match("^(?i)Habenzinsen[\\s]{1,}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + '.' + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note") + '.' + v.get("year"));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^(?i)KESt[\\s]{1,}(?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 17.11 ABRECHNUNG ZU KONTO 00123456678 17.11 2.522,75
        // VERANLAGUNGSBETRAG             2.500,00
        // LAUFZEIT        2025-05-15 - 2025-11-17
        // Habenzinsen
        // AB 2025-05-15          2,4000%              30,33
        // KESt                                         7,58-
        // @formatter:on
        var interestBlock_Format02 = new Block("^[\\d]{1,2}\\.[\\d]{1,2} ABRECHNUNG ZU KONTO .* [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) ABRECHNUNG ZU KONTO (?<note>[\\d]+) [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$") //
                        .find("^Habenzinsen$") //
                        .match("^AB [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} .* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> { //
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Abrechnung zu Konto: " + v.get("note"));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^(?i)KESt[\\s]{1,}(?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));

    }
}

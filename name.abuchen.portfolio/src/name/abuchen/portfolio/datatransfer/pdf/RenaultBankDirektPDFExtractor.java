package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanksAndUnderscores;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/**
 * @formatter:off
 * @implNote Renault Bank direkt
 *
 * @implSpec The importer / the Renault Bank seems to be slightly inconsistent with booking data. In the statement line
 *
 *           26.02. 30.02. Abschluss 0,52 H
 *           28.02.  Wertstellung: 30.02. Zinsen/Kontoführung                                               3,41+
 *           15.02.-30.02.   0,250% Habenzinsen                                              1,15+
 *
 *           a value date of 30.02. is provided — a date that does not exist.
 *           Because of this incorrect value date, we cannot rely on the value date (valuta date) field.
 *           Therefore, we must consistently use the posting date (booking date) instead of the value date for processing these transactions.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class RenaultBankDirektPDFExtractor extends AbstractPDFExtractor
{
    public RenaultBankDirektPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("305 200 37");
        addBankIdentifier("Renault Bank direkt");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Renault Bank direkt";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("(305 200 37|Kontoauszug)", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                    // @formatter:off
                                                    // _  _ __ _  __  _N_E_U_E__R_ K__O_NT__O_S_T_A_N_D_ _VO_M__ 2_0_._1__1_._2_0_20__ I__N_ _E_U_R _ __  _ _ _ __  __
                                                    // @formatter:on
                                                    section -> section //
                                                                    .attributes("currency") //
                                                                    .match("^.*N.*E.*U.*E.*R.*K.*O.*N.*T.*O.*S.*T.*A.*N.*D.*V.*O.*M.*I.*N* (?<currency>[A-Z_]{3,}).*$") //
                                                                    .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(stripBlanksAndUnderscores(v.get( "currency"))))),
                                                    // @formatter:off
                                                    //                        Abschlusssaldo am    30.11.2019: 14.323,94 EUR
                                                    // @formatter:on
                                                    section -> section //
                                                                    .attributes("currency") //
                                                                    .match("^[\\s]*Abschlusssaldo am .* (?<currency>[A-Z]{3}).*") //
                                                                    .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get( "currency")))),
                                                    // @formatter:off
                                                    // EUR-Konto Kontonummer 123456789
                                                    // @formatter:on
                                                    section -> section //
                                                                    .attributes("currency") //
                                                                    .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                                                    .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                    // @formatter:off
                                                    // Datum Informationen Referenz Betrag in EUR Valuta Kontostand in EUR
                                                    // @formatter:on
                                                    section -> section //
                                                                    .attributes("currency") //
                                                                    .match("^.*Kontostand in (?<currency>[A-Z]{3}).*$") //
                                                                    .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))))

                                            // @formatter:off
                                            //                                 KONTOAUSZUG  Nr. 1/2019
                                            // Renault Bank direkt - Postfach 269 - 45952 Gladbeck Kontoauszug Nr.   2/2021
                                            // @formatter:on
                                            .section("year").optional() //
                                            .match("^.*(KONTOAUSZUG|Kontoauszug)[\\s]*Nr\\.[\\s]*[\\d]{1,2}\\/(?<year>[\\d]{4}).*$") //
                                            .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 18.11.  Überweisungs-Gutschrift                                                            4.480,00+
        // 21.11.  Internet-Euro-Überweisung                                            210,00-
        // 17.02.  Dauerauftrag-Gutschrift                                                              210,00+
        // 31.08.  Wertstellung: 30.08. Internet-Euro-Überweisung                     1.500,00-
        // @formatter:on
        var depositRemovalBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\. .*(.berweisung|Gutschrift).* [\\.,\\d]+[\\-|\\+][\\s]*$");
        type.addBlock(depositRemovalBlock_Format01);
        depositRemovalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)[\\s]{1,}(.*[\\d]{2}\\.[\\d]{2}\\.)?(?<note>.*) (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+])[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 17.02. 17.02. Dauerauftragsgutschr                                                          150,00 H
        // 22.02. 22.02. Überweisungsgutschr.                                                        7.547,85 H
        // @formatter:on
        var depositRemovalBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}(?!Abschluss).* [\\.,\\d]+ [H|S][\\s]*$");
        type.addBlock(depositRemovalBlock_Format02);
        depositRemovalBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+) (?<type>[H|S])[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "S" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("S".equals(v.get("type")))
                                t.setType(Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 04.12.2021 Zahlungseingang Z12345 3.200,00 04.12.2021 25.132,68
        // 22.12.2021 Überweisung Z2342343X -5.000,00 22.12.2021 20.134,62
        // @formatter:on
        var depositRemovalBlock_Format03 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}(Zahlungseingang|.berweisung).* (\\-)?[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$");
        type.addBlock(depositRemovalBlock_Format03);
        depositRemovalBlock_Format03.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*(?<type>[\\s|\\-]{1,})(?<amount>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 29.11.  Wertstellung: 30.11. Zinsen/Kontoführung                                               2,44+
        // 30.06.  Zinsen/Kontoführung                                                                    0,21+
        // @formatter:on
        var interestBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\. .*Zinsen\\/Kontof.hrung.* [\\.,\\d]+[\\-|\\+][\\s]*$");
        type.addBlock(interestBlock_Format01);
        interestBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)[\\s]{1,}(Wertstellung: [\\s]*[\\d]{2}\\.[\\d]{2}\\.)?(?<note>.*) (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+])[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 26.02. 30.02. Abschluss                                                                       0,52 H
        //               0,25000% Habenzins                   0902       0,06H
        //               0,20000% Habenzins                   2802       0,46H
        //               Abschluss vom 01.02.2021 bis 28.02.2021
        //               für Konto  123456789
        //               Rechnung Nr.  0123456789.20210228.001
        //               USt. IdNr. - DE 812212244
        //               USt.-befreite Finanzdienstleistung
        //
        // 05.08. 31.07. Storno Abschluss                                                      3,59 S
        //
        // 26.02. 30.02. Abschluss                                                                       4,72 H
        //                0,00000% Habenzins                   2802       0,00H
        //                0,50000% Habenzins                   2802       4,72H
        //                Abschluss vom 05.02.2021 bis 28.02.2021
        //                für Konto 1234564500
        //                Rechnung Nr.  1234564500.20210228.001
        //                USt. IdNr. - DE 12342244
        //                USt.-befreite Finanzdienstleistung
        //
        // 26.02. 30.02. Solid.-Zuschlag aus                                                   0,04 S
        //               EUR        0,68-KapSt      per 25.02.2021 KT5619994500
        // 26.02. 30.02. Kapitalertragsteuer aus                                               0,68 S
        //               EUR        4,72 Habenzins  per 25.02.2021 KT5619994500
        // @formatter:on
        var interestBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}(Storno )?Abschluss.* [\\.,\\d]+ [H|S][\\s]*$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 05.08. 31.07. Storno Abschluss                                                      3,59 S
                        // @formatter:on
                        .section("type").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}(?<type>Storno) Abschluss.* [\\.,\\d]+ [H|S][\\s]*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionOrderCancellationUnsupported))

                        .section("date", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}(Storno )?Abschluss.* (?<amount>[\\.,\\d]+) (?<type>[H|S])[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "S" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // 26.02. 30.02. Kapitalertragsteuer aus                                               0,68 S
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}Kapitalertrags(s)?teuer aus[\\s]{1,}(?<tax>[\\.,\\d]+) [S].*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // 26.02. 30.02. Solid.-Zuschlag aus                                                   0,04 S
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}Solid\\.\\-Zuschlag aus[\s]{1,}(?<tax>[\\.,\\d]+) [S].*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // ???
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\.[\\s]{1,}Kirchensteuer aus[\\s]{1,}(?<tax>[\\.,\\d]+) [S].*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        //               Abschluss vom 01.02.2021 bis 28.02.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\s]*Abschluss vom (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        }));

        // @formatter:off
        // 31.12.2021 Bonuszinsen A8765ABCD 1,23 31.12.2021 20.137,08
        // @formatter:on
        var interestBlock_Format03 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}Bonuszinsen .* [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$");
        type.addBlock(interestBlock_Format03);
        interestBlock_Format03.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]{1,}(?<note>Bonuszinsen) .* (?<amount>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.12.2021 Habenzinsen A8765ABCD 2,46 31.12.2021 20.137,08
        // 28.11.2025  Habenzinsen A2511IDQC 0,01 30.11.2025 9,66
        //
        // Optional taxes for interest transactions:
        // 31.12.2021 Kapitalertragsteuer A8765ABCD -0,62 31.12.2021 20.136,46
        // 30.01.2026  Kapitalertragsteuer A2601JABQ -0,73 31.01.2026 6,44
        // @formatter:on
        var interestBlock_Format04 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}Habenzinsen .* (\\-)?[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$");
        type.addBlock(interestBlock_Format04);
        interestBlock_Format04.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*(?<type>[\\s|\\-]{1,})(?<amount>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+[\\s]*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // 31.12.2021 Kapitalertragsteuer A8765ABCD -0,62 31.12.2021 20.136,46
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}Kapitalertrags(s)?teuer .*\\-(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // ???
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}Solidarit.tszuschlag .*\\-(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // ???
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}Kirchensteuer .*\\-(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));
    }
}

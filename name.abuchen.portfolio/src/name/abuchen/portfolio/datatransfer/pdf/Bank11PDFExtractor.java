package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class Bank11PDFExtractor extends AbstractPDFExtractor
{

    public Bank11PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bank11 für Privatkunden und Handel GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bank11 für Privatkunden und Handel GmbH";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType(".*-Konto Kontonummer", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Hammer Landstr. 91, 41460 Neuss Kontoauszug Nr.  1/2022
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.*Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // EUR-Konto Kontonummer 764783800
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 19.10. 19.10. Überweisungsgutschr.                                                       18.500,00 H
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .*gutschr\\.?[\\s]{1,}[\\.,\\d]+ [H]");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>.*gutschr\\.?)[\\s]{1,}(?<amount>[\\.,\\d]+) [H]") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.10. 31.10. Überweisungsauftrag                                               3.000,00 S
        // 21.11. 21.11. Umbuchung                                                           200,00 S
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. (Umbuchung|.*berweisungsauftrag)[\\s]{1,}[\\.,\\d]+ [S]");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>(Umbuchung|.*berweisungsauftrag))[\\s]{1,}(?<amount>[\\.,\\d]+) [S]") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));


        // @formatter:off
        // 30.12. 31.12. Abschluss lt. Anlage 1                                                         52,66 H
        // 31.03. 31.03. Abschluss                                                                      56,88 H
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Abschluss.*[\\s]{1,}[\\.,\\d]+ [H]");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Abschluss.*[\\s]{1,}(?<amount>[\\.,\\d]+) [H]") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }
}

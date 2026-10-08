package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractor extends AbstractPDFExtractor
{
    public JTDirektbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("J&T Direktbank");
        addBankIdentifier("J&T BANKA");

        addAccountStatementTransaction();
        addSlovakAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "J&T Direktbank";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("J&T Direktbank", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 45952 Gladbeck Kontoauszug Nr.  1/2023
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.*Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // EUR-Konto Kontonummer 6480010
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 31.03. 31.03. Überweisungsgutschr. 3.000,00 H
        // 01.06. 01.06. Überweisungsauftrag 400,00 S
        // 18.12. 18.12. Dauerauftragsgutschr 900,00 H
        // 29.01. 29.01. Spar/Fest/Termingeld 5.000,00 S
        // 08.03. 08.03. Umbuchung  1.100,00 S
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. (.*gutschr\\.?|.berweisungsauftrag|Spar/Fest/Termingeld|Umbuchung)[\\s]{1,}[\\.,\\d]+ [S|H]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>(.*gutschr\\.?" //
                                        + "|.berweisungsauftrag" //
                                        + "|Spar/Fest/Termingeld" //
                                        + "|Umbuchung))[\\s]{1,}" //
                                        + "(?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                        .assign((t, v) -> {
                            // Is type --> "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            if ("Dauerauftragsgutschr".equals(v.get("note")))
                                v.put("note", "Dauerauftragsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 04.05. 30.04. Abschluss lt. Anlage 1 11,69 H
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Abschluss lt\\. Anlage [\\d][\\s]{1,}[\\.,\\d]+ [H]$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Abschluss lt\\. Anlage [\\d][\\s]{1,}(?<amount>[\\.,\\d]+) [H]$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 03.05. 30.04. Storno Abschluss  13,44 S
        // @formatter:on
        var interestCancellationBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Storno .*[\\s]{1,}[\\.,\\d]+ [S]$");
        type.addBlock(interestCancellationBlock);
        interestCancellationBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST_CHARGE))

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Storno .*[\\s]{1,}(?<amount>[\\.,\\d]+) [S]$") //
                        .assign((t, v) -> {
                            v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addSlovakAccountStatementTransaction()
    {
        final var type = new DocumentType("V.PIS Z ..TU", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Mena účtu: EUR
                                        // Mena účtu: EUR W. shNGPW 1n/13
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Mena .*tu: (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Aktuálna úroková sadzba (p.a.):    3,50 %
                                        // @formatter:on
                                        .section("interestRate").optional() //
                                        .match("^Aktu.lna .rokov. sadzba \\(p\\.a\\.\\):[\\s]{1,}(?<interestRate>[\\.,\\d]+ %)$") //
                                        .assign((ctx, v) -> ctx.put("interestRate", v.get("interestRate"))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 02. 01. 2024 Vysporiadanie úroku vkladu 3,74
        // 31. 12. 2023
        // 31. 12. 2023 Daň z úrokov -0,71
        //
        // 14. 05. 2024 Vysporiadanie úroku vkladu 189,48
        // 14. 05. 2024 Aktuálna úroková sadzba (p.a.): 3,80 %
        // 14. 05. 2024 Daň z úrokov -36,00
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\. [\\d]{2}\\. [\\d]{4} Vysporiadanie .roku vkladu [\\.\\d\\s]+,[\\d]{2}$");
        interestBlock.setMaxSize(3);
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("amount", "date") //
                        .documentContext("currency") //
                        .documentContextOptionally("interestRate") //
                        .match("^[\\d]{2}\\. [\\d]{2}\\. [\\d]{4} Vysporiadanie .roku vkladu (?<amount>[\\.\\d\\s]+,[\\d]{2})$") //
                        .match("^(?<date>[\\d]{2}\\. [\\d]{2}\\. [\\d]{4}).*$") //
                        .assign((t, v) -> {
                            // The value date (Valuta) is used as booking date
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));

                            if (v.get("interestRate") != null)
                                t.setNote(v.get("interestRate") + " p.a.");
                        })

                        // @formatter:off
                        // 31. 12. 2023 Daň z úrokov -0,71
                        // @formatter:on
                        .section("type", "tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\. [\\d]{2}\\. [\\d]{4} Da. z .rokov(?<type>\\s(\\-)?)(?<tax>[\\.\\d\\s]+,[\\d]{2})$") //
                        .assign((t, v) -> {
                            // Taxes are always negative in the statement,
                            // only a "-" sign reduces the interest amount
                            if ("-".equals(trim(v.get("type"))))
                            {
                                var tax = Money.of(v.get("currency"), asAmount(stripBlanks(v.get("tax"))));

                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 14. 05. 2024 Ukončenie vkladu SK80 8320 0000 0029 0004 7946   -10 153,48
        // 14. 05. 2024 Banka protiúčtu: JTBPSKBAXXX zTmbW oWWIo
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\. [\\d]{2}\\. [\\d]{4} Ukon.enie vkladu .* \\-[\\.\\d\\s]+,[\\d]{2}$");
        removalBlock.setMaxSize(2);
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("note", "amount", "date") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\. [\\d]{2}\\. [\\d]{4} (?<note>Ukon.enie vkladu) .* \\-(?<amount>[\\.\\d\\s]+,[\\d]{2})$") //
                        .match("^(?<date>[\\d]{2}\\. [\\d]{2}\\. [\\d]{4}).*$") //
                        .assign((t, v) -> {
                            // The value date (Valuta) is used as booking date
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }
}

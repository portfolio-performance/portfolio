package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NordaxBankABPDFExtractor extends AbstractPDFExtractor
{
    public NordaxBankABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Nordax Bank AB");
        addBankIdentifier("Bank Norwegian");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Nordax Bank AB";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("(Account Statement|Kontoauszug)", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 11286 SBNIRZM Währung: EUR
                                        // Währung: Euro (EUR)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.*W.hrung:.*(?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*(Zinsbuchung|Interest) [\\.,\\d]+.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf(
                                        //
                                        // @formatter:off
                                        // 24.08.2024 Zinsbuchung 403,39 10.403,39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Zinsbuchung (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        //
                                        // @formatter:off
                                        // 31.12.2024 01.01.2025 Interest 24,40
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Interest (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        })
                     )

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 24.08.2024 Übertrag des verlängerten 403,39 10.000,00
        // 24.08.2024 Übertrag des verlängerten 10.000,00 0,00
        // @formatter:on
        var transferBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .bertrag des verl.ngerten [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(transferBlock);
        transferBlock.set(new Transaction<AccountTransferEntry>()

                        .subject(() -> {
                            var accountTransferEntry = new AccountTransferEntry();
                            return accountTransferEntry;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .bertrag des verl.ngerten (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new AccountTransferItem(t, true)));

        // @formatter:off
        // 03.11.2025 03.11.2025 Zahlung an 2.000,00
        // 10.12.2024 10.12.2024 Bezahlung von 12.000,00
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Zahlung an|Bezahlung von) [\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "type", "amount", "note") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<type>(Zahlung an|Bezahlung von)) (?<amount>[\\.,\\d]+)$") //
                        .match("(?<note>.*)")
                        .assign((t, v) -> {
                            // @formatter:off
                            // When "Zahlung an" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("Zahlung an".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .section("note2").optional() //
                        .find("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*$")
                        .match("^.*$") //
                        .match("^(?<note2>.*)$") //
                        .assign((t, v) -> {
                            if (!v.get("note2").startsWith("Bank Norwegian, "))
                                t.setNote(concatenate(t.getNote(), v.get("note2"), " "));
                        })

                        .wrap(TransactionItem::new));
    }
}

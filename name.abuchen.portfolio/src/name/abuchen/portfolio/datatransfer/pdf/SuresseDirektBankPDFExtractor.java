package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SuresseDirektBankPDFExtractor extends AbstractPDFExtractor
{
    public SuresseDirektBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Suresse");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Suresse Direkt Bank";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Auszug [\\d]+([\\s]+)\\/[\\d]+", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo am 31-01-2023 : 5.011,05 EUR
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.* [\\d]{2}\\-[\\d]{2}\\-(?<year>[\\d]{4}) : ([\\-])?[\\.,\\d]+ [\\w]{3}$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 1 19-12 19-12 Uberweisung zu Ihren Gunsten 1,00 EUR
        // 2 20-12 20-12 Ihre U¨berweisung -1,00 EUR
        // @formatter:on
        Block depositBlock = new Block("^[\\d]+ [\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2} .*.berweisung.*([\\-])?[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "sign", "amount", "currency", "note") //
                        .documentContext("year") //
                        .match("^[\\d]+ (?<date>[\\d]{2}\\-[\\d]{2}) [\\d]{2}\\-[\\d]{2} .*.berweisung.*(?<sign>([\\s|\\-]))(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^(?<note>Referenz : .*)$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + "-" + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("^Z I N S A B S C H L U S S$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note1", "note2", "amount", "currency") //
                        .match("^Periode von: (?<note1>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) .* (?<note2>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) .*$") //
                        .match("^Habenzinsen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("note2")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note1").replace("-", ".") + " - " + v.get("note2").replace("-", "."));
                        })

                        .wrap(TransactionItem::new));
    }
}

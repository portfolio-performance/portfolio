package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class FordMoneyPDFExtractor extends AbstractPDFExtractor
{
    public FordMoneyPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Ford Bank GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Ford Bank GmbH / Ford Money";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Tagesgeld Kontoauszug", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match("^datum datum in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", v.get("currency"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 04.10.2024 04.10.2024 Gutschrift 1.000,00
        // 25.10.2024 25.10.2024 Überweisung -2.000,00
        // @formatter:on
        Block depositRemovalBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Gutschrift|.berweisung) ([\\-])?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "sign") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Gutschrift|.berweisung).*(?<sign>[\\s|\\-])(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.10.2024 31.10.2024 Abschluss 62,73
        // 31.10.2024 31.10.2024 Kapitalertragssteuer -37,09
        // 31.10.2024 31.10.2024 Solidaritätszuschlag -2,03
        // @formatter:on
        Block interestBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Abschluss"
                                        + "|Kapitalertragssteuer" //
                                        + "|Solidarit.tszuschlag"
                                        + "|Kirchensteuer) ([\\-])?[\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note", "sign") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Abschluss"
                                        + "|Kapitalertragssteuer" //
                                        + "|Solidarit.tszuschlag"
                                        + "|Kirchensteuer)).*(?<sign>[\\s|\\-])(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

    }
}

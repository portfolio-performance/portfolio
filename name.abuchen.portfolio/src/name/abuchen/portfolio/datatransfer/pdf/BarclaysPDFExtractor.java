package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;


@SuppressWarnings("nls")
public class BarclaysPDFExtractor extends AbstractPDFExtractor
{
    public BarclaysPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Barclays Bank Ireland PLC");
        addBankIdentifier("USt-IdNr.: DE 319 453 063");
        addBankIdentifier("Handelsregister HRB: 153530");
        addBankIdentifier("barclays.de");

        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Barclays";
    }

    private void addCreditcardStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug vom ", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match("^Beleg\\- Buchungs\\-/ Beschreibung Karte Betrag \\((?<currency>[\\w]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);
        
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.* [\\.,\\d]+\\+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>.{31,36})(.+)? +" //
                                        + "(?<amount>[\\.,\\d]+)\\+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));


        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.* [\\.,\\d]+\\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>.{36}) [A-Z]{2} Visa( B)?( A)? +" //
                                        + "(?<amount>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

}

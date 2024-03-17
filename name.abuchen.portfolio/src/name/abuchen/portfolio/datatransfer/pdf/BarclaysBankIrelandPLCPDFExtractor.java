package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;


@SuppressWarnings("nls")
public class BarclaysBankIrelandPLCPDFExtractor extends AbstractPDFExtractor
{
    public BarclaysBankIrelandPLCPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Barclays Bank Ireland PLC");

        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Barclays Bank Ireland PLC";
    }

    private void addCreditcardStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug vom ", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match("^Beleg\\- Buchungs\\-/ Beschreibung Karte Betrag \\((?<currency>[\\w]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\.,\\d]+[\\-|\\+]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>.{1,36})" //
                                        + ".* " //
                                        + "(?<amount>[\\.,\\d]+)" //
                                        + "(?<type>[\\-|\\+])$") //
                        .assign((t, v) -> {

                            // Is sign --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(replaceMultipleBlanks(v.get("note"))));
                        })

                        .wrap(TransactionItem::new));
    }
}

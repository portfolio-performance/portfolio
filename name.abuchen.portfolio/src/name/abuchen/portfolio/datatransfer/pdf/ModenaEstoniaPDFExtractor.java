package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

/**
 * @implNote Modena Estonia OÜ does not have a specific bank identifier.
 */
@SuppressWarnings("nls")
public class ModenaEstoniaPDFExtractor extends AbstractPDFExtractor
{
    public ModenaEstoniaPDFExtractor(Client client)
    {
        super(client);

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Modena Estonia OÜ";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(?m)^Income statement$");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 2025-03-13 21:39:01 Vault revenue share €12.56
                        // 2025-03-14 07:00:00 Vault signup bonus €1.42
                        // 2025-03-21 21:59:09 Vault campaign bonus €50.00
                        // @formatter:on
                        .section("type").optional() //
                        .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2} " //
                                        + "(?<type>(Vault revenue share" //
                                        + "|Vault signup bonus" //
                                        + "|Vault campaign bonus)" //
                                        + ") .*$") //
                        .assign((t, v) -> {
                            if ("Vault signup bonus".equals(v.get("type")) //
                                            || "Vault campaign bonus".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.DEPOSIT);
                        })

                        // @formatter:off
                        // 2025-03-13 21:39:01 Vault revenue share €12.56
                        // 2025-03-14 07:00:00 Vault signup bonus €1.42
                        // 2025-03-21 21:59:09 Vault campaign bonus €50.00
                        // @formatter:on
                        .section("date", "time", "note", "currency", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) " //
                                        + "(?<note>(Vault revenue share" //
                                        + "|Vault signup bonus" //
                                        + "|Vault campaign bonus)" //
                                        + ") " //
                                        + "(?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }
}

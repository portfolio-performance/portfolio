package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class GinmonPDFExtractor extends AbstractPDFExtractor
{
    public GinmonPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Ginmon Vermögensverwaltung GmbH");

        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Ginmon Vermögensverwaltung GmbH";
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("Geb.hrenabrechnung", "Kontoauszug");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Rechnungsdatum.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Rechnungsbetrag 0.04 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Rechnungsbetrag (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Rechnungsdatum / Invoice date: 31.10.2024
                        // @formatter:on
                        .section("date") //
                        .match("^Rechnungsdatum / Invoice date: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Rechnungsnummer /Invoice no: 00000002024101
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer.*: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Zeitraum vom 01.10.2024 – 31.10.2024
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zeitraum vom (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\– [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }
}

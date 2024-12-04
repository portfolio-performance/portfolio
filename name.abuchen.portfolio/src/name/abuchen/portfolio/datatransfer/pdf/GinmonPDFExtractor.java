package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class GinmonPDFExtractor extends AbstractPDFExtractor
{
    public GinmonPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Ginmon GmbH");

        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Ginmon GmbH";
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(Gebührenabrechnung)", //
                        documentContext -> documentContext //
                        // @formatter:off
                        // Rechnungsdatum / Invoice date: 31.10.2024
                        // Rechnungsnummer /Invoice no: 00000002024101
                        // @formatter:on
                                        .section("date") //
                                        .match("^Rechnungsdatum / Invoice date: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("date", v.get("date"));
                                        }));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Gebührenabrechnung) (.*)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gebührenabrechnung Oktober
                        // Invoice October
                        // Zeitraum vom 01.10.2024 – 31.10.2024
                        // Period of 01.10.2024 – 31.10.2024
                        // Position Berechungsbasis Gebühr Betrag
                        // Calculation Fee Amount
                        // Grundgebühr 57.60 € 0.7500% p.a. 0.04 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .documentContext("date") //
                        .match("^Rechnungsbetrag (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount"), "en", "US"));
                            v.getTransactionContext().put(FAILURE,
                                            Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }
}
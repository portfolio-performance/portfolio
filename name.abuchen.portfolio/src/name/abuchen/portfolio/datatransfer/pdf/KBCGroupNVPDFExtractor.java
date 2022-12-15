package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class KBCGroupNVPDFExtractor extends AbstractPDFExtractor
{
    public KBCGroupNVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("KBC BANK NV"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "KBC Group NV"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Overzicht transacties en rekeninguittreksels");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Borderel [\\d]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Uw Aankoop Online van 15 PROSUS N.V. (AS) aan 57,2 EUR 858,00 EUR
                // Waardecode NL0013654783
                .section("name", "currency", "isin")
                .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Uw Aankoop Online van 15 PROSUS N.V. (AS) aan 57,2 EUR 858,00 EUR
                .section("shares").optional()
                .match("^.* van (?<shares>[\\.,\\d]+) .* aan [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // 02/02/2022 14:50:02 Valuta 04/02/2022 Euronext A'dam
                .section("date", "time")
                .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) Valuta [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Netto debit -868,50 EUR
                .section("amount", "currency")
                .match("^Netto debit \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Borderel 275825809 Limit order
                .section("note").optional()
                .match("^(?<note>Borderel [\\d]+).*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Beurstaks 3,00 EUR
                .section("tax", "currency").optional()
                .match("^Beurstaks (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Makelaarsloon 7,50 EUR
                .section("fee", "currency").optional()
                .match("^Makelaarsloon (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

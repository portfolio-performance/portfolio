package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LGTBankPDFExtractor extends AbstractPDFExtractor
{
    public LGTBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LGT Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "LGT Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Abrechnung Kauf .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Titel A.P. Moeller - Maersk A/S
                // Namen- und Inhaber-Aktien -B-
                // ISIN DK0010244508
                // Valorennummer 906020
                // Wertpapierkennnummer 861837
                // Kurswert DKK 80'784.00
                .section("name", "nameContinued", "isin", "wkn", "currency")
                .match("^Titel (?<name>.*)$")
                .match("(?<nameContinued>.*)")
                .match("^ISIN (?<isin>[\\w]{12})$")
                .match("^Wertpapierkennnummer (?<wkn>.*)$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,'\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Anzahl 12 Stück
                .section("shares")
                .match("^Anzahl (?<shares>[\\d.,]+) St.ck$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Abschlussdatum 14.04.2020 09:00:02
                .section("date", "time")
                .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Belastung DKK Konto 0037156.021 DKK 82'452.21
                .section("currency", "amount")
                .match("^Belastung [\\w]{3} Konto .* (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Valorennummer 906020
                .section("note").optional()
                .match("^(?<note>Valorennummer .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Baraussch.ttung");
        this.addDocumentTyp(type);

        Block block = new Block("^Baraussch.ttung .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // 551 Veolia Environnement SA
                // Namen- und Inhaber-Aktien
                // ISIN: FR0000124141, Valoren-Nr.: 1098758
                // Ausschüttung EUR 0.50
                .section("name", "nameContinued", "isin", "wkn", "currency")
                .find("Stand Ihres Depots am [\\d]{2}\\. .* [\\d]{4}:")
                .match("^[\\.,\\d]+ (?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12}), Valoren\\-Nr\\.: (?<wkn>.*)$")
                .match("^Aussch.ttung (?<currency>[\\w]{3}) [\\.,'\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 551 Veolia Environnement SA
                .section("shares")
                .find("Stand Ihres Depots am [\\d]{2}\\. .* [\\d]{4}:")
                .match("^(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 14. Mai 2020
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\. .* [\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Netto EUR 198.36
                .section("currency", "amount")
                .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ausschüttungsart Ordentliche Dividende
                .section("note").optional()
                .match("^Aussch.ttungsart (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eidg. Umsatzabgabe  DKK 121.19
                .section("tax", "currency").optional()
                .match("^Eidg\\. Umsatzabgabe ([\\s]+)?(?<currency>[\\w]{3}) (?<tax>[\\.,'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer 28 % EUR -77.14
                .section("withHoldingTax", "currency").optional()
                .match("^Quellensteuer [\\d]+ % (?<currency>[\\w]{3}) \\-(?<withHoldingTax>[\\.,'\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Courtage  DKK 1'534.90
                .section("fee", "currency").optional()
                .match("^Courtage ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Broker Kommission  DKK 12.12
                .section("fee", "currency").optional()
                .match("^Broker Kommission ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

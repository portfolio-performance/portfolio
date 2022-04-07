package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class UBSAGBankingAGPDFExtractor extends AbstractPDFExtractor
{

    private final static String ISIN_PATTERN = "[A-Z]{2}[A-Z0-9]{9}[0-9]{1}"; //$NON-NLS-1$
    private final static String VALOR_PATTERN = "[0-9]{6,9}"; //$NON-NLS-1$
    private final static Map<String, Type> txTypes = Map.of("Kauf", Type.BUY, "Verkauf", Type.SELL); //$NON-NLS-1$

    public UBSAGBankingAGPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("UBS"); //$NON-NLS-1$
        addBankIdentifier("UBS Switzerland AG"); //$NON-NLS-1$
        addBankIdentifier("www.ubs.com"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UBS AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Börse (Kauf|Verkauf) Komptant");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Bewertet in: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction.section("name", "isin", "nameContinued", "currency")
                        // Whrg. Anzahl/Betrag Beschreibung Valor/ISIN Laufzeit
                        .match("^Whrg\\. Anzahl/Betrag.*$")

                        // USD 2'180 UBS (Lux) Fund Solutions - MSCI 21966836
                        .match("^(?<currency>[A-Z]{3}) [0-9'\\.]+ (?<name>.*) " + VALOR_PATTERN + "$")

                        // Emerging Markets UCITS ETF LU0950674175
                        .match("^(?<nameContinued>[\\w ]*) (?<isin>" + ISIN_PATTERN + ")$").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("currency", "amount", "date", "time", "shares", "type")
                        .match("^Abschluss (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})  (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) Börse (?<type>Kauf|Verkauf) Komptant \\-{0,1}(?<shares>[\\d.,']+) (?<currency>[\\w]{3}) [\\.',\\d]+$")

                        // Transaktionswert USD 4'890.60 4'532
                        .match("^Transaktionswert [A-Z]{3} (?<amount>[0-9'\\.]+) .*$").assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            Type txType = txTypes.getOrDefault(v.get("type"), null);
                            t.setType(txType);

                        })

                        .wrap(BuySellEntryItem::new);

        addFeeTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeeTransaction(T transaction, DocumentType type)
    {
        // Diverse USD -7.34
        transaction.section("currency", "fee").optional() //
                        .match("^Diverse (?<currency>[A-Z]{3}) (?<fee>[0-9'\\.\\-]*)")
                        .assign((t, v) -> processFeeEntries(t, v, type));

        // Courtage USD -37.01
        transaction.section("currency", "fee").optional() //
                        .match("^Courtage (?<currency>[A-Z]{3}) (?<fee>[0-9'\\.\\-]*)")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "CH");
    }

}

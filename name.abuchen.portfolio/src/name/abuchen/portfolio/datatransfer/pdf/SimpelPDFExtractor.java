package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SimpelPDFExtractor extends AbstractPDFExtractor
{
    /***
     * Information: 
     * The currency of Simpel S.A. is always EUR.
     */

    public SimpelPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Simpel S.A.");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Simpel S.A.";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^([\\s]+)?(Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^([\\s]+)?(?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                // AT0000A1QA38 10.01.2022 5.123
                .section("name", "isin")
                .match("^([\\s]+)?(Kauf|Verkauf) (?<name>.*) ['\\.\\d]+ \\p{Sc} ([\\s]+)?['\\.\\d]+ \\p{Sc} ([\\s]+)?[\\.\\d\\s]+$")
                .match("^(?<isin>[\\w]{12}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ['\\.\\d]+$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.EUR);
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                // Verkauf Standortfonds Deutschland 880.29 € 133.58 € 6.590
                .section("shares")
                .match("^([\\s]+)?(Kauf|Verkauf) .* ['\\.\\d]+ \\p{Sc} ([\\s]+)?['\\.\\d]+ \\p{Sc} ([\\s]+)?(?<shares>[\\.\\d\\s]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // AT0000A1QA38 10.01.2022 5.123
                .section("date")
                .match("^[\\w]{12} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ['\\.\\d]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Abrechnungsbetrag: 10.00 €
                // Auszahlungsbetrag: 848.68 €
                .section("amount")
                .match("^(Abrechnungsbetrag|Auszahlungsbetrag): ([\\s]+)?(?<amount>['\\.\\d]+) \\p{Sc}$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                })

                // Auftrags-Nummer: 20220106123456789000000612345
                .section("note").optional()
                .match("^(?<note>Auftrags-Nummer: [\\d]+)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Aussch.ttungsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("^.* Aussch.ttungsanzeige$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Fondsname: Standortfonds Deutschland Datum des Ertrags: 21.12.2021
                // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                .section("name", "isin")
                .match("^Fondsname: (?<name>.*) Datum .*$")
                .match("^WKN \\/ ISIN: (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.EUR);
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // There is no information about the proportions in the
                // documents. In this section we calculate the shares
                // for the respective transaction.

                // Ausschüttung je Anteil: 2.71
                // Ausschüttung gesamt: 17.58
                .section("amountPerShare", "gross")
                .match("^Aussch.ttung je Anteil: (?<amountPerShare>['\\.\\d]+)$")
                .match("^Aussch.ttung gesamt: (?<gross>['\\.\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal amountPerShare = asExchangeRate(v.get("amountPerShare"));
                    BigDecimal amountTotal = asExchangeRate(v.get("gross"));

                    int sharesPrecision = Values.Share.precision() * 2;
                    BigDecimal shares = amountTotal.divide(amountPerShare, sharesPrecision,
                                    RoundingMode.HALF_UP);

                    t.setShares(asShares(shares.toPlainString()));
                })

                // Fondsname: Standortfonds Deutschland Datum des Ertrags: 21.12.2021
                .section("date")
                .match("^Fondsname: .* Datum des Ertrags: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Zur Wiederveranlagung zur Verfügung stehend: 13.30
                .section("amount")
                .match("^Zur Wiederveranlagung zur Verf.gung stehend: (?<amount>['\\.\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                })

                // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                .section("note").optional()
                .match("^.* (?<note>Turnus: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // abgeführte Kapitalertragssteuer: 31.61 €
                .section("tax").optional()
                .match("^abgef.hrte Kapitalertragssteuer: (?<tax>['\\.\\d]+) \\p{Sc}$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragssteuer (KESt) gesamt: 4.28
                .section("tax").optional()
                .match("^Kapitalertragssteuer \\(KESt\\) gesamt: (?<tax>['\\.\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

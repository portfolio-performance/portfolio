package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Hypothekarbank Lenzburg AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractor extends AbstractPDFExtractor
{
    public HypothekarbankLenzburgAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hypothekarbank Lenzburg AG");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Hypothekarbank Lenzburg AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben am .* f.r Sie gekauft");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Transaktion .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                        //  720 Accum Shs USD Inve FTSE All Depotstelle  3500
                        // Valor: 125615212 / IE000716YHJ7
                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .find("Wir haben am .* f.r Sie gekauft") //)
                        .match("^.*[\\,'\\d]+ .* [A-Z]{3} (?<name>.*) Depotstelle .*$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                        .match("^Menge[\\s]{1,}[\\.'\\d]+ Kurs (?<currency>[\\w]{3})[\\s]{1,}[\\.'\\d]+[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                        // @formatter:on
                        .section("shares") //
                        .match("^Menge[\\s]{1,}(?<shares>[\\.'\\d]+) Kurs.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                        // @formatter:on
                        .section("date") //
                        .match("^Wir haben am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Belastung 314.391.304 Valuta 18.03.2024 CHF  3'974.14
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Belastung .* (?<currency>[\\w]{3})[\\s]{1,}(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Transaktion 61327806-0002
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Transaktion .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Umsatzabgabe CHF  5.92
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidg\\. Umsatzabgabe (?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eigene Kommission (NEON) CHF  19.74
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Eigene Kommission.* (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

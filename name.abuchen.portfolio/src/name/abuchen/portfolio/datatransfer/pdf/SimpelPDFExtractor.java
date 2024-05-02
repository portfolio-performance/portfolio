package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Simpel S.A. is a Euro-based financial services company.
 *           The currency of Simpel S.A. is always EUR.
 *
 * @implSpec There is no information about the shares in the dividend transactions in the document.
 *           These are calculated for the respective transaction.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SimpelPDFExtractor extends AbstractPDFExtractor
{

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

        Block firstRelevantLine = new Block("^.*(Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.*(?<type>(Kauf|Verkauf)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // AT0000A1QA38 10.01.2022 5.123
                        // @formatter:on
                        .section("name", "currency", "isin") //
                        .match("^.*(Kauf|Verkauf) (?<name>.*) [\\.'\\d]+ \\p{Sc}[\\s]{1,}[\\.'\\d]+ (?<currency>\\p{Sc})[\\s]{1,}[\\.'\\d]+$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // Verkauf Standortfonds Deutschland 880.29 € 133.58 € 6.590
                        // @formatter:on
                        .section("shares") //
                        .match("^.*(Kauf|Verkauf) .* [\\.'\\d]+ \\p{Sc}[\\s]{1,}[\\.'\\d]+ \\p{Sc}[\\s]{1,}(?<shares>[\\.'\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // AT0000A1QA38 10.01.2022 5.123
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Abrechnungsbetrag: 10.00 €
                        // Auszahlungsbetrag: 848.68 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Abrechnungsbetrag|Auszahlungsbetrag):[\\s]{1,}(?<amount>[\\.'\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Auftrags-Nummer: 20220106123456789000000612345
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nummer: [\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Aussch.ttungsanzeige");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.* Aussch.ttungsanzeige$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Fondsname: Standortfonds Deutschland Datum des
                        // Ertrags: 21.12.2021
                        // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Fondsname: (?<name>.*) Datum .*$") //
                        .match("^WKN \\/ ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                        .assign((t, v) -> {
                            v.put("currency", "EUR");

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Ausschüttung je Anteil: 2.71
                        // Ausschüttung gesamt: 17.58
                        // @formatter:on
                        .section("amountPerShare", "gross") //
                        .match("^Aussch.ttung je Anteil: (?<amountPerShare>[\\.'\\d]+)$") //
                        .match("^Aussch.ttung gesamt: (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            BigDecimal gross = BigDecimal.valueOf(asAmount(v.get("gross")));

                            BigDecimal shares = gross.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                        })

                        // @formatter:off
                        // Fondsname: Standortfonds Deutschland Datum des
                        // Ertrags: 21.12.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Fondsname: .* Datum des Ertrags: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zur Wiederveranlagung zur Verfügung stehend: 13.30
                        // Zur Wiederanlage/Auszahlung zur Verfügung stehend: 173.37
                        // @formatter:on
                        .section("amount") //
                        .match("^Zur (Wiederveranlagung|Wiederanlage/Auszahlung) zur Verf.gung stehend: (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode("EUR");
                        })

                        // @formatter:off
                        // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Turnus: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // abgeführte Kapitalertragssteuer: 31.61 €
                        // @formatter:on
                        .section("tax", "curreny").optional() //
                        .match("^abgef.hrte Kapitalertragssteuer: (?<tax>[\\.'\\d]+) (?<curreny>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragssteuer (KESt) gesamt: 4.28
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kapitalertragssteuer \\(KESt\\) gesamt: (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", "EUR");
                            processTaxEntries(t, v, type);
                        });
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
}

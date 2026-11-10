package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class UpvestPDFExtractor extends AbstractPDFExtractor
{
    public UpvestPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Upvest Securities GmbH");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Upvest Securities GmbH";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        Block firstRelevantLine = new Block("^Wertpapierabrechnung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //
                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section().optional() //
                        .match("^Wir haben f.r Sie verkauft.*$") //
                        .assign((t, v) -> t.setType(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // Wertpapier ISIN 
                        // XTR.II EUR OV.RATE SW. 1C LU0290358497 
                        // Nominal/Anzahl Kurs Kurswert 
                        // 2,60301 Stück 148,31 EUR 386,05 EUR 
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Wertpapier\\s+ISIN.*$") //
                        .match("^(?<name>.*)\\s+(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\s*$") //
                        .match("^[\\d,.]+\\s+St.ck\\s+[\\d,.]+\\s+[A-Z]{3}\\s+[\\d,.]+\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 2,60301 Stück 148,31 EUR 386,05 EUR 
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\d,.]+)\\s+St.ck\\s+[\\d,.]+\\s+[A-Z]{3}\\s+[\\d,.]+\\s+[A-Z]{3}\\s*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Gunsten 
                        // 04.02.2026 386,04 EUR 
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Valuta Betrag zu.*$")
                        .match("^\\d{2}\\.\\d{2}\\.\\d{4}\\s+(?<amount>[\\d,.]+)\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Handelsdatum, Uhrzeit: 02.02.2026 15:05:46
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelsdatum, Uhrzeit: (?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+(?<time>\\d{2}:\\d{2}:\\d{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Abrechnungsnummer
                        // fTZdS kenyWfNqm hZmvir 47ca082b-7818-45d4-9dda-8cb2d1174086
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Abrechnungsnummer.*$") //
                        .match("^.*\\s+(?<note>[a-z0-9\\-]+)\\s*$") //
                        .assign((t, v) -> t.setNote("Abr.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

        // @formatter:off
                        // Steuererrechnung
                        // Kapitalertragssteuer: 0,01 EUR 
                        // Solidaritätszuschlag: 0,00 EUR 
                        // Kirchensteuer: 0,00 EUR 
                        // @formatter:on
                        .section("tax", "currency").multipleTimes().optional() //
                        .match("^(Kapitalertragssteuer|Solidarit.tszuschlag|Kirchensteuer): (?<tax>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount,
                        ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }
}

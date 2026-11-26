package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
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
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Upvest Securities GmbH";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Wir haben für Sie (gekauft|verkauft)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wertpapierabrechnung.*$");
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
                        .match("^[\\.,\\d]+\\s+St.ck\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s+[\\.,\\d]+\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 2,60301 Stück 148,31 EUR 386,05 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+)\\s+St.ck\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Gunsten
                        // 04.02.2026 386,04 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Valuta Betrag zu.*$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\s+(?<amount>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Handelsdatum, Uhrzeit: 02.02.2026 15:05:46
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelsdatum, Uhrzeit: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})\\s+(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handelsdatum: 01.11.2026
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelsdatum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Abrechnungsnummer
                        // fTZdS kenyWfNqm hZmvir 47ca082b-7818-45d4-9dda-8cb2d1174086
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Abrechnungsnummer.*$") //
                        .match("^.*\\s+(?<note>[A-Za-z0-9\\-]+)\\s*$") //
                        .assign((t, v) -> t.setNote("Abr.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Sie haben eine Ausschüttung erhalten");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wertpapierabrechnung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //
                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapier ISIN
                        // AEFS-A.EUR.STR.I.RNXTEOD LU3227875377
                        // Positionsgröße / Anteile Auszahlung pro Stück Gesamteinnahme
                        // 685,05 Stück 0,1526 EUR 104,53 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Wertpapier\\s+ISIN.*$") //
                        .match("^(?<name>.*)\\s+(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\s*$") //
                        .match("^[\\.,\\d]+\\s+Stück\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s+[\\.,\\d]+\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 685,05 Stück 0,1526 EUR 104,53 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+)\\s+Stück\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s+[\\.,\\d]+\\s+[A-Z]{3}\\s*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wertstellung Betrag zu Ihren Gunsten
                        // 25.11.2026 76,97 EUR
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .match("^Wertstellung Betrag zu.*$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})\\s+(?<amount>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3})\\s*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer
                        // OgbtsxP MkitQ 203n8904-a515-4500-a2e5-5446BNC0hQ60
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Auftragsnummer.*$") //
                        .match("^.*\\s+(?<note>[A-Za-z0-9\\-]+)\\s*$") //
                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note"))))

                        .wrap(TransactionItem::new);

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
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Gezahlte ausländische Quellensteuer (0 %): 0,00 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Gezahlte ausländische Quellensteuer \\([\\.,\\d]+ %\\): (?<withHoldingTax>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare ausländische Quellensteuer: 0,00 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare ausländische Quellensteuer: (?<creditableWithHoldingTax>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Einbehaltene deutsche Kapitalertragsteuer: 26,13 EUR
                        // Einbehaltener deutscher Solidaritätszuschlag: 1,43 EUR
                        // Einbehaltene deutsche Kirchensteuer: 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").multipleTimes().optional() //
                        .match("^Einbehaltene(r)? deutsche(r)? (Kapitalertragsteuer|Solidaritätszuschlag|Kirchensteuer): (?<tax>[\\.,\\d]+)\\s+(?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount,
                        ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }
}

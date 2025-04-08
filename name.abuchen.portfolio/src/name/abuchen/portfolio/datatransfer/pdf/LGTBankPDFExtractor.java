package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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
public class LGTBankPDFExtractor extends AbstractPDFExtractor
{
    public LGTBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LGT Bank");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "LGT Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Abrechnung (Kauf|Verkauf|Zeichnung)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnung (Kauf|Verkauf|Zeichnung).*$", "^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
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
                        .match("^Abrechnung (?<type>(Kauf|Verkauf|Zeichnung)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel A.P. Moeller - Maersk A/S
                                        // Namen- und Inhaber-Aktien -B-
                                        // ISIN DK0010244508
                                        // Valorennummer 906020
                                        // Wertpapierkennnummer 861837
                                        // Kurswert DKK 80'784.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "wkn", "currency") //
                                                        .match("^Titel (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Wertpapierkennnummer (?<wkn>.*)$") //
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Titel JPMorgan Funds SICAV - Emerging Markets Sustainable Equity
                                        // Fund
                                        // Namen-Anteile -C- / Class USD
                                        // ISIN LU2051469208
                                        // Valorennummer 50139326
                                        // Kurswert USD 50'520.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^Titel (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Anzahl 12 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl (?<shares>[\\.,\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschlussdatum 14.04.2020 09:00:02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Abschlussdatum 04.07.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Abschlussdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zeichnungstag (NAV) 08.06.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zeichnungstag .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Belastung DKK Konto 0037156.021 DKK 82'452.21
                        // Belastung CHF Freizügigkeitskonto 2026457.031 CHF 48'502.01
                        // Gutschrift USD Konto 2026457.055 USD 8'332.19
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Belastung|Gutschrift) [\\w]{3} .* (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 210796978 Kundenportfolio
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: .*) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Valorennummer 906020
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Valorennummer .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Baraussch.ttung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Baraussch.ttung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 551 Veolia Environnement SA
                        // Namen- und Inhaber-Aktien
                        // ISIN: FR0000124141, Valoren-Nr.: 1098758
                        // Ausschüttung EUR 0.50
                        // @formatter:on
                        .section("name", "nameContinued", "isin", "wkn", "currency") //
                        .find("Stand Ihres Depots am [\\d]{1,2}\\. .* [\\d]{4}:") //
                        .match("^[\\.,\\d]+ (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.*ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]), Valoren\\-Nr\\.: (?<wkn>.*)$") //
                        .match("^Aussch.ttung (?<currency>[\\w]{3}) [\\.,'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 551 Veolia Environnement SA
                        // @formatter:on
                        .section("shares") //
                        .find("Stand Ihres Depots am [\\d]{1,2}\\. .* [\\d]{4}:") //
                        .match("^(?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 14. Mai 2020
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto EUR 198.36
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 330401346 Kontonummer: 1234567.031
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Ausschüttungsart Ordentliche Dividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Aussch.ttungsart (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Umsatzabgabe  DKK 121.19
                        // Eidg. Umsatzabgabe  USD -12.51
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Eidg\\. Umsatzabgabe ([\\s]+)?(?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Verrechnungssteuer 35 % CHF -851.20
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Verrechnungssteuer [\\d]+ % (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 28 % EUR -77.14
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer [\\d]+ % (?<currency>[\\w]{3}) (\\-)?(?<withHoldingTax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage  DKK 1'534.90
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage[\\s]{1,}(?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SIX Meldegebühr  CHF 0.20
                        // SIX Meldegebühr  CHF -0.20
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.* Meldegeb.hr[\\s]{1,}(?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SIX Börsengebühr  CHF -8.80
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.* B.rsengeb.hr[\\s]{1,}(?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Broker Kommission  DKK 12.12
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Broker Kommission[\\s]{1,}(?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
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

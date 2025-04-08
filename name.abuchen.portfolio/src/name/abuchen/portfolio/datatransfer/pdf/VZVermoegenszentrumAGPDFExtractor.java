package name.abuchen.portfolio.datatransfer.pdf;

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

/**
 * @formatter:off
 * @implNote VZ Holding / VZ Vermögenszentrum
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class VZVermoegenszentrumAGPDFExtractor extends AbstractPDFExtractor
{
    public VZVermoegenszentrumAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("VZ Depotbank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "VZ Holding / VZ Vermögenszentrum";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung \\- (Kauf|Emission)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Auftragsnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

//                        // Is type --> "Verkauf" change from BUY to SELL
//                        .section("type").optional() //
//                        .match("^Abrechnung (?<type>(Kauf|Verkauf|Zeichnung)).*$") //
//                        .assign((t, v) -> {
//                            if ("Verkauf".equals(v.get("type")))
//                                t.setType(PortfolioTransaction.Type.SELL);
//                        })

                        // @formatter:off
                        // Wir haben für Sie am 31.01.2022 gekauft.
                        // 20 Namen-Aktie
                        // Calida Holding AG
                        // Valor: 12663946
                        // ISIN: CH0126639464
                        // Total Kurswert CHF -986.00
                        // @formatter:on
                        .section("name1", "name", "wkn", "isin", "currency") //
                        .find("Wir haben f.r Sie am.*")
                        .match("^[\\.'\\d]+ (?<name1>.*)$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Total Kurswert (?<currency>[\\w]{3}) \\-[\\.'\\d]+$") //
                        .assign((t, v) -> {
                            v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Wir haben für Sie am 31.01.2022 gekauft.
                        // 20 Namen-Aktie
                        // @formatter:on
                        .section("shares") //
                        .find("Wir haben f.r Sie am.*")
                        .match("^(?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wir haben für Sie am 31.01.2022 gekauft.
                        // Ausführung Menge Preis Kurswert
                        // 10:19:24 20 49.30 CHF -986.00
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Wir haben f.r Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .find("Ausf.hrung Menge Preis Kurswert")
                        .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) [\\.'\\d]+ [\\.'\\d]+ [\\w]{3} \\-[\\.'\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Netto CHF -1'027.30
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[\\w]{3}) \\-(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer AUF221133-43219876
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividende|Aussch.ttung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Unsere Referenz.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wir beziehen uns auf die in Ihrem Wertschriftendepot verwahrten Titel und rechnen die Ausschüttung wie folgt ab:
                        // Anteile -(CHF) A-dis- Ex Datum: 08.09.2023
                        // UBS ETF (CH) - SMIM (R) Zahlbar Datum: 13.09.2023
                        // Valor: 11176253
                        // ISIN: CH0111762537
                        // Bestand: 38 zu CHF 5.99
                        // @formatter:on
                        .section("name1", "name", "wkn", "isin", "currency") //
                        .find("Wir beziehen uns auf die in.*")
                        .match("^(?<name1>.*) Ex Datum:.*$") //
                        .match("^(?<name>.*) Zahlbar Datum:.*$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Bestand: [\\.'\\d]+ zu (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> {
                            v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Bestand: 38 zu CHF 5.99
                        // @formatter:on
                        .section("shares") //
                        .match("^Bestand: (?<shares>[\\.,\\d]+) zu [\\w]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // UBS ETF (CH) - SMIM (R) Zahlbar Datum: 13.09.2023
                        // @formatter:on
                        .section("date") //
                        .match("^.*Zahlbar Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto CHF 147.95
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Zug, 14.09.2023 Unsere Referenz CA20237768/20888    / PJD
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Referenz .*\\/[\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Umsatz Stempel CHF -0.75
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidg\\. Umsatz Stempel (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Verrechnungssteuer 35% CHF -79.65
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer [\\.'\\d]+% (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Börsengebühren CHF -1.55
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hren (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage * CHF -39.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Courtage \\* (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$") //
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

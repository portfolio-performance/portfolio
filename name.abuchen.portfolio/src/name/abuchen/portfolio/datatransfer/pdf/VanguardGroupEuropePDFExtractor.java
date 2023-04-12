package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class VanguardGroupEuropePDFExtractor extends AbstractPDFExtractor
{
    public VanguardGroupEuropePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Vanguard Group Europe GmbH"); //$NON-NLS-1$^

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Vanguard Group Europe GmbH"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung: Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapierabrechnung: Kauf$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Wertpapierbezeichnung FDBL Vanguard LifeStrategy 60% Equity UCITS ETF
                // (EUR) Distributing
                // ISIN IE00BMVB5Q68
                // Kurs (Stückpreis) EUR 25,0900
                .section("name", "name1", "isin", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)")
                .match("^(?<name1>.*)$")
                .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^Kurs \\(.*\\) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("ISIN"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominal / Stück 11,956954
                .section("shares")
                .match("^Nominal \\/ St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungstag / -zeit 03.02.2023 um 11:11:17 Uhr
                .section("date", "time")
                .match("^Ausf.hrungstag \\/ \\-zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                .oneOf(
                                // Kurswert EUR 300,00
                                // EUR 300,00
                                // Abrechnungskonto DE01234567891234567890
                                section -> section
                                        .attributes("currency", "amount")
                                        .find("Kurswert [\\w]{3} [\\.,\\d]+$")
                                        .match("^(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .match("^Abrechnungskonto .*$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // Abbuchungsbetrag EUR 300,00
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Abbuchungsbetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Referenznummer 12345678
                .section("note").optional()
                .match("^(?<note>Referenznummer .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);
    }
}

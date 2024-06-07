package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class RaisinBankAGPDFExtractor extends AbstractPDFExtractor
{
    public RaisinBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raisin Bank AG");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Raisin Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapierabrechnung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Wertpapier ISIN
                        // VANG.FTSE D.A.P.X.J.DLD IE00B9F5YL18
                        // 0,18854 Stück 24,08 EUR 4,54 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                        .match("^[\\.,\\d]+ St.ck [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 0,18854 Stück 24,08 EUR 4,54 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) St.ck [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Handelsdatum, Uhrzeit: 23.05.2024 12:10:43
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelsdatum, Uhrzeit: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Lasten
                        // 27.05.2024 4,54 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("Valuta Betrag zu Ihren Lasten.*")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abrechnungsnummer
                        // 95a8535f-0aa7-4197-85ae-3f7337d9232d
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Abrechnungsnummer).*")
                        .match("^(?<note2>[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+).*$") //
                        .assign((t, v) -> t.setNote(concatenate(trim(v.get("note1")), trim(v.get("note2")), ": ")))

                        .wrap(BuySellEntryItem::new);
    }
}

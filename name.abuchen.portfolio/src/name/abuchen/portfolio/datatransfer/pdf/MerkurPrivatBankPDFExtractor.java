package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractor extends AbstractPDFExtractor
{

    public MerkurPrivatBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Umsatzsteuer-ID DE198159260");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MERKUR PRIVATBANK KGaA";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // @formatter:off
        // Wertpapier Abrechnung Kauf
        // @formatter:on
        Block firstRelevantLine = new Block("^Am Marktplatz.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction // @formatter:off
                        // Wertpapier Abrechnung Verkauf
                        // or
                        // Wertpapier Abrechnung Kauf
                        // @formatter:on
                        .section("type").match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*").assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // REGISTERED SHARES 1C O.N.
                        // @formatter:on
                        .section("name", "name1", "isin", "wkn", "currency")
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                        .match("^(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$").assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 09.05.2023 09:26:37 Auftraggeber jvDDiy QfmZL LloZHJy
                        // @formatter:on
                        .section("date", "time")
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 47.811,46- EUR
                        // @formatter:on
                        .section("amount", "currency")
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Auftragsnummer 284722/61.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);

    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction // @formatter:off
                        // Provision 25,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,06- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));

    }
}

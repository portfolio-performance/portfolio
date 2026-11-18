package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class CaisseEpargnePDFExtractor extends AbstractPDFExtractor
{
    public CaisseEpargnePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CAISSE D'EPARGNE");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Caisse d'Epargne";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("NATURE DE L.OPERATION : (ACHAT|VENTE) AU COMPTANT");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^NATURE DE L.OPERATION : (ACHAT|VENTE) AU COMPTANT$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "VENTE" change from BUY to SELL
                        .section("type").optional() //
                        .match("^NATURE DE L.OPERATION : (?<type>(ACHAT|VENTE)) AU COMPTANT$") //
                        .assign((t, v) -> {
                            if ("VENTE".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // LIBELLE DE LA VALEUR : CARREFOUR
                        // CODE VALEUR : FR0000120172
                        // QUANTITE : 15
                        // COURS : 16,06 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^LIBELLE DE LA VALEUR : (?<name>.*)$") //
                        .match("^CODE VALEUR : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^COURS : [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // DATE ET HEURE D'EXECUTION : 27/05/2026 - 12:17:41
                        // @formatter:on
                        .section("date", "time") //
                        .match("^DATE ET HEURE D.EXECUTION : (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) \\- (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // QUANTITE : 15
                        // @formatter:on
                        .section("shares") //
                        .match("^QUANTITE : (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // NET PASSE SUR VOTRE COMPTE NUMERAIRE 12345 12345 12345678910 EUR 242,70
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^NET PASS. SUR VOTRE COMPTE NUMERAIRE .* (?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesSectionsTransaction(Transaction<BuySellEntry> transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // TAXE SUR LES ACQUISITIONS DE TITRES 0,96
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^TAXE SUR LES ACQUISITIONS DE TITRES (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // the tax line has no currency, the currency of
                            // the transaction was set from the net amount
                            v.put("currency", t.getPortfolioTransaction().getCurrencyCode());
                            processTaxEntries(t, v, type);
                        });
    }

    private void addFeesSectionsTransaction(Transaction<BuySellEntry> transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // COMMISSION 0,84
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^COMMISSION (?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // the commission line has no currency, the currency
                            // of the transaction was set from the net amount
                            v.put("currency", t.getPortfolioTransaction().getCurrencyCode());
                            processFeeEntries(t, v, type);
                        });
    }
}
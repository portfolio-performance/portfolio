package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CreditMutuelAllianceFederalePDFExtractor extends AbstractPDFExtractor
{
    public CreditMutuelAllianceFederalePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Suravenir");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Crédit Mutuel Alliance Fédérale (Suravenir)";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Objet : Confirmation de versement");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Objet : Confirmation de versement$", "^Total.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Montant brut (frais inclus) : 3 000,00 €
                        // LYXOR STOXX50 DR UCITS ETF DIST FR0007054358 05/06/2020 33,26 90,1984 3 000,00
                        // @formatter:on
                        .section("currency", "name", "isin") //
                        .match("^Montant brut \\(frais inclus\\) : [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}.*") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // LYXOR STOXX50 DR UCITS ETF DIST FR0007054358 05/06/2020 33,26 90,1984 3 000,00
                        // @formatter:on
                        .section("shares") //
                        .match("^.* [A-Z]{2}[A-Z0-9]{9}[0-9] [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d\\s]+,[\\d]{2} (?<shares>[\\d\\s]+,[\\d]{1,}) [\\d\\s]+,[\\d]{2}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // ü Date du versement : 04/06/2020
                        // @formatter:on
                        .section("date") //
                        .match("^.*Date du versement : (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Montant net : 3 000,00 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Montant net : (?<amount>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "fr", "FR");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "fr", "FR");
    }
}

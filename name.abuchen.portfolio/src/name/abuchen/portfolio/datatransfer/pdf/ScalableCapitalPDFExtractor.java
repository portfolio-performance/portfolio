package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class ScalableCapitalPDFExtractor extends AbstractPDFExtractor
{
    public ScalableCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scalable Capital GmbH");

        addPurchaseTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scalable Capital GmbH";
    }

    private void addPurchaseTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^f.r (Kundenauftrag|Sparplanausf.hrung) .*$");
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
                        .match("^(?<type>(Kauf|Verkauf)) .* [\\.,\\d]+ Stk\\. .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                        // IE0008T6IUX0
                        // @formatter:on
                        .section("name", "currency", "isin") //
                        .match("^(Kauf|Verkauf) (?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(Kauf|Verkauf) .* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ausführung 12.12.2024 13:12:51 Geschäft 36581526
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Ausf.hrung (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total 19,49 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Typ LIMIT Order SCALsin78vS5CYz
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Order (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ordergebühren +0,99 EUR
                        // Ordergebühren -0,99 EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Ordergeb.hren [\\-|\\+](?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

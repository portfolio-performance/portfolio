package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class MeDirectBankPlcPDFExtractor extends AbstractPDFExtractor
{
    public MeDirectBankPlcPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("MeDirect Bank");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MeDirect Bank (Malta) PLC";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Transactiebevestiging");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^In overeenstemming met uw instructies hebben wij de.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        type.getCurrentContext();

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkoop" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>Verkoop) .* [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                        .assign((t, v) -> {
                            if ("Verkoop".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Verkoop UT0001234508 FR0010135103 CARMIGNAC 1.03 € 667,16 € 687,17
                        // Koop UT0003175054 LU0432616737 INVESCO FUNDS - 38.879 € 17,49 € 679,99
                        // PATRIMOINE A EUR
                        // @formatter:on
                        .section("name", "isin", "currency", "nameContinued") //
                        .match("^(Koop|Verkoop) .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) [\\.,\\d]+ (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc} [\\.,\\d]+$") //
                        .match("(?<nameContinued>.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Verkoop UT0001234508 FR0010135103 CARMIGNAC 1.03 € 667,16 € 687,17
                        // Koop UT0003175054 LU0432616737 INVESCO FUNDS - 38.879 € 17,49 € 679,99
                        // @formatter:on
                        .section("shares").optional() //
                        .match("^(Koop|Verkoop) .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* (?<shares>[\\.,\\d]+) \\p{Sc} [\\.,\\d]+ \\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 18-02-2020 - 09:06:19.000000 (UTC) XOFF N/A
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(?<date>[\\d]{2}-[\\d]{2}-[\\d]{4}) \\- (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Totaal € 684,46
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Totaal (?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Verkoop UT0001234508 FR0010135103 CARMIGNAC 1.03 € 667,16 € 687,17
                        // Koop UT0003175054 LU0432616737 INVESCO FUNDS - 38.879 € 17,49 € 679,99
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Koop|Verkoop) (?<note>.*) [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // MeDirect Commissie € 0,00
                        // @formatter:on
                        .section("currency", "fee").optional()
                        .match("^MeDirect Commissie (?<currency>\\p{Sc}) (?<fee>[\\.,\\d]+)$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Wisselkosten en belastingen € 2,71
                        // @formatter:on
                        .section("currency", "fee").optional()
                        .match("^Wisselkosten en belastingen (?<currency>\\p{Sc}) (?<fee>[\\.,\\d]+)$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class OpenBankSAPDFExtractor extends AbstractPDFExtractor
{
    public OpenBankSAPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("OPEN BANK");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Open Bank S.A.";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("TRANSAKTIONSABRECHNUNG");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRANSAKTIONSABRECHNUNG$");
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
                        .match("^.* TRANSAKTION .* (?<type>(ZEICHNUNG|R.CKERSTATTUNG|MAXIMAL VERF.GBARE ERSTATTUNG))$") //
                        .assign((t, v) -> {
                            if ("RÜCKERSTATTUNG".equals(v.get("type")) //
                                            || "MAXIMAL VERFÜGBARE ERSTATTUNG".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        //                                                                             A N G E G E B EN  IN :      EUR
                        //  D A T U M :    15-09-2022    ISHARES EURO GOVT INF I'EUR                         W E C H S E LK U R S :        1,00
                        //  I SI N :      IE00BD0NC144
                        //     NETTOINVENTARWERT                    10,00000 EUR
                        // @formatter:on
                        .section("currency", "name", "isin") //
                        .match("^.*A([\\s]+)?N([\\s]+)?G([\\s]+)?E([\\s]+)?G([\\s]+)?E([\\s]+)?B([\\s]+)?E([\\s]+)?N[\\s]{1,}I([\\s]+)?N[\\s]{1,}:[\\s]{1,}" //
                                        + "(?<currency>[\\w]{3})$") //
                        .match("^.*D([\\s]+)?A([\\s]+)?T([\\s]+)?U([\\s]+)?M([\\s]+)?:[\\s]{1,}[\\d]{2}\\-[\\d]{2}-[\\d]{4}[\\s]{1,}" //
                                        + "(?<name>.*)[\\s]{1,}" //
                                        + "W([\\s]+)?E([\\s]+)?C([\\s]+)?H([\\s]+)?S([\\s]+)?E([\\s]+)?L([\\s]+)?K([\\s]+)?U([\\s]+)?R([\\s]+)?S([\\s]+)?:[\\s]{1,}[\\.,\\d]+$") //
                        .match("^.*I([\\s]+)?S([\\s]+)?I([\\s]+)?N([\\s]+)?:[\\s]{1,}" //
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        //     BETEILIGUNGEN                       1,0000000
                        // @formatter:on
                        .section("shares") //
                        .match("^.*BETEILIGUNGEN[\\s]{1,}(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        //     DATUM NETTOINVENTARWERT    13.09.2022
                        // @formatter:on
                        .section("date") //
                        .match("^.*DATUM NETTOINVENTARWERT[\\s]{1,}(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        //     NETTOBETRAG                             10,00 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^.*NETTOBETRAG[\\s]{1,}(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        //      T R A  N S A K T I O N S N U  M M E R  :   0  0  0  0 5  0  0  0 0  0    PERSÖNLICHES KONTO
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*: (?<note>[\\d\\s]+)[\\s]{1,}PERS.NLICHES KONTO$") //
                        .assign((t, v) -> t.setNote("Transaktion-Nr.: " + stripBlanks(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
    }
}

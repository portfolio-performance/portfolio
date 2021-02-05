package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class LGTBankPDFExtractor extends AbstractPDFExtractor
{
    public LGTBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LGT Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "LGT Bank AG"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "LGT Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType newType = new DocumentType(".*Abrechnung Kauf.*");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Abrechnung Kauf.*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            // Titel A.P. Moeller - Maersk A/S
            // Namen- und Inhaber-Aktien -B-
            // ISIN DK0010244508
            // Valorennummer 906020
            // Wertpapierkennnummer 861837
            .section("isin", "wkn", "name")
            .match("^(Titel) (?<name>.*)$")
            .match("^(ISIN) (?<isin>[\\w]{12}.*)$")
            .match(".*")
            .match("^(Wertpapierkennnummer) (?<wkn>.*)$")
            .assign((t, v) -> {
                t.setSecurity(getOrCreateSecurity(v));
            })

            // Abschlussdatum 14.04.2020 09:00:02
            .section("date", "time")
            .match("^(Abschlussdatum) (?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+)$")
            .assign((t, v) -> {
                if (v.get("time") != null)
                    t.setDate(asDate(v.get("date"), v.get("time")));
                else
                    t.setDate(asDate(v.get("date")));
            })
            
            // Anzahl 12 Stück
            .section("shares")
            .match("^(Anzahl) (?<shares>[\\d.,]+) (Stück)$")
            .assign((t, v) -> {
                t.setShares(asShares(v.get("shares")));
            })

            // Belastung DKK Konto 0037156.021 DKK 82'452.21
            .section("currency", "amount")
            .match("^(Belastung.* Konto) .* (?<currency>[\\w]{3}) (?<amount>[\\d('|.)]+(,|.)\\d+)$")
            .assign((t, v) -> {
                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                t.setAmount(asAmount(ConvertAmount(v.get("amount"))));
            })

            // Eidg. Umsatzabgabe  DKK 121.19
            .section("tax", "currency")
            .match("^(Eidg. Umsatzabgabe)\\s+(?<currency>[\\w]{3}) (?<tax>[\\d('|.)]+(,|.)\\d+)$")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(ConvertAmount(v.get("tax")))))))

            // Courtage  DKK 1'534.90
            .section("fee", "currency")
            .match("^(Courtage)\\s+(?<currency>[\\w]{3}) (?<fee>[\\d('|.)]+(,|.)\\d+)$")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(ConvertAmount(v.get("currency"))),
                                                            asAmount(ConvertAmount(v.get("fee")))))))

            // Broker Kommission  DKK 12.12
            .section("fee", "currency")
            .match("^(Broker Kommission)\\s+(?<currency>[\\w]{3}) (?<fee>[\\d('|.)]+(,|.)\\d+)$")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(ConvertAmount(v.get("currency"))),
                                                            asAmount(ConvertAmount(v.get("fee")))))))

            .wrap(BuySellEntryItem::new);
    }

    private String ConvertAmount(String inputAmount)
    {
        String amount;
        
        amount = inputAmount.replace("'", "");
        
        return amount.replace(".", ",");
    }
}

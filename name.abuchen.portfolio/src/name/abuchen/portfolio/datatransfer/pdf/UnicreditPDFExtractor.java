package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class UnicreditPDFExtractor extends AbstractPDFExtractor
{

    public UnicreditPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UniCredit Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();

    }

    @Override
    public String getLabel()
    {
        return "UniCredit Bank AG / HypoVereinsbank (HVB)"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("K a u f|V e r k a u f");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(
                        "^W e r t p a p i e r - A b r e c h n u n g ([\\s]+)?(K a u f| V e r k a u f).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional()
                        .match("^W e r t p a p i e r - A b r e c h n u n g ([\\s]+)?(?<type>V e r k a u f).*$")
                        .assign((t, v) -> {
                            if (v.get("type").equals("V e r k a u f"))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                            }
                        })

                        // Nennbetrag Wertpapierbezeichnung
                        // Wertpapierkennnummer/ISIN
                        // SANOFI S.A. 920657
                        // ST 22
                        // ACTIONS PORT. EO 2 FR0000120578
                        // Kurswert EUR 1.547,26
                        .section("name", "wkn", "isin", "shares", "currency").optional()
                        .find("^Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer\\/ISIN.*$")
                        .match("^(?<name>.*) (?<wkn>[^ ]*)$") //
                        .match("^ST *(?<shares>[.,\\d]+)$") //
                        .match("^.* (?<isin>[\\w]{12})$") //
                        .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+.*$") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // Nennbetrag Wertpapierbezeichnung
                        // Wertpapierkennnummer/ISIN
                        // ST 25 FIRST EAGLE AMUNDI-INTERNATIO. A1JQVV ACTIONS
                        // NOM. AE-C O.N. LU0565135745
                        // Kurswert EUR 4.803,50
                        .section("name", "name2", "wkn", "isin", "shares").optional()
                        .find("^Nennbetrag Wertpapierbezeichnung ([\\s]+)?Wertpapierkennnummer\\/ISIN.*$")
                        .match("^ST (?<shares>[.,\\d]+) (?<name>.*) ([\\s]+)?(?<wkn>[\\w]{6}) ([\\s]+)?(?<name2>.*) (?<isin>[\\w]{12})$")
                        .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+.*$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("name").trim() + " " + v.get("name2").trim());

                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // Zum Kurs von Ausf}hrungstag/Zeit Ausf}hrungsort
                        // Verwahrart
                        // 15.02.2016
                        .section("date").optional() //
                        .find("^Zum Kurs von .*$") //
                        .match("^(?<date>\\d+.\\d+.\\d{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // Zum Kurs von Ausführungstag/Zeit Ausführungsort
                        // Verwahrart
                        // EUR 192,14 20.04.2021 03.53.15 WP-Rechnung GS
                        .section("date", "time").optional() //
                        .find("^Zum Kurs von .*$")
                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?(?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // Belastung (vor Steuern) EUR 1.560,83
                        // Gutschrift (vor Steuern) EUR 8.175,91
                        .section("currency", "amount")
                        .match("^(Belastung|Gutschrift) \\(vor Steuern\\) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)(.*)?$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // Brokerkommission* EUR 0,27
                        .section("fee", "currency").optional()
                        .match("^Brokerkommission\\* (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)(.*)?$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // Transaktionsentgelt* EUR 3,09
                        .section("fee", "currency").optional()
                        .match("^Transaktionsentgelt\\* (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)(.*)?$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // Provision EUR 10,21
                        .section("fee", "currency").optional()
                        .match("^Provision (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)(.*)?$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // Wertpapierprovision* EUR 159,96
                        .section("fee", "currency").optional()
                        .match("^Wertpapierprovision\\* (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)(.*)?$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

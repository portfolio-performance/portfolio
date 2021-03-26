package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class SBrokerPDFExtractor extends AbstractPDFExtractor
{
    public SBrokerPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("S Broker AG & Co. KG"); //$NON-NLS-1$
        addBankIdentifier("Sparkasse"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf .*|Verkauf .*|Wertpapier Abrechnung Ausgabe Investmentfonds)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        
        Block firstRelevantLine = new Block("(Kauf .*|Verkauf .*|Wertpapier Abrechnung Ausgabe Investmentfonds)");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                    // Is type --> "Verkauf" change from BUY to SELL
                    .section("type").optional()
                    .match("(?<type>Verkauf?) .*")
                    .assign((t, v) -> {
                        if (v.get("type").equals("Verkauf"))
                        {
                            t.setType(PortfolioTransaction.Type.SELL);
                        }
                    })

                    // Gattungsbezeichnung ISIN
                    // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                    .section("isin", "name").optional()
                    .match("Gattungsbezeichnung ISIN")
                    .match("(?<name>.*)\\W+(?<isin>.+)")
                    .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                    // Nominale Wertpapierbezeichnung ISIN (WKN)
                    // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                    .section("shares", "name", "isin", "wkn").optional()
                    .match("^Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                    .match("^St.ck (?<shares>[.,\\d]+) (?<name>.*) (?<isin>\\w{12}) \\((?<wkn>.*)\\)")
                    .assign((t, v) -> {
                        t.setShares(asShares(v.get("shares")));
                        t.setSecurity(getOrCreateSecurity(v));
                    })

                    // STK 16,000 EUR 120,4000
                    .section("shares").optional()
                    .match("^STK (?<shares>[.,\\d]+) .*")
                    .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                    // Auftrag vom 27.02.2021 01:31:42 Uhr
                    .section("date", "time").optional()
                    .match("^Auftrag vom (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+).*")
                    .assign((t, v) -> {
                        if (v.get("time") != null)
                            t.setDate(asDate(v.get("date"), v.get("time")));
                        else
                            t.setDate(asDate(v.get("date")));
                    })

                    // Ausmachender Betrag 500,00- EUR
                    .section("currency", "amount").optional()
                    .match("^Ausmachender Betrag (?<amount>[.,\\d]+)[-]? (?<currency>\\w{3})")
                    .assign((t, v) -> {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(v.get("currency"));
                    })

                    // Wert Konto-Nr. Betrag zu Ihren Lasten
                    // 01.10.2014 10/0000/000 EUR 1.930,17
                    .section("date", "amount", "currency").optional()
                    .match(".* zu Ihren (Gunsten|Lasten).*")
                    .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}\\/\\d{4}\\/\\d{3} (?<currency>\\w{3}) (?<amount>[.,\\d]+)")
                    .assign((t, v) -> {
                        t.setDate(asDate(v.get("date")));
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    })
                    
                    // Handelszeit 09:02 Orderentgelt                EUR 10,90-
                    .section("fee", "currency").optional()
                    .match(".* Orderentgelt\\W+(?<currency>\\w{3}+) (?<fee>[.,\\d]+)-")
                    .assign((t, v) -> t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("fee"))))))

                    // Börse Stuttgart Börsengebühr EUR 2,29-
                    .section("fee", "currency").optional()
                    .match(".* B.rsengeb.hr (?<currency>\\w{3}+) (?<fee>[.,\\d]+)-")
                    .assign((t, v) -> t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("fee"))))))

                    // Kurswert 509,71- EUR
                    // Kundenbonifikation 40 % vom Ausgabeaufschlag 9,71 EUR
                    // Ausgabeaufschlag pro Anteil 5,00 %
                    .section("feeFx", "feeFy", "amountFx", "currency").optional()
                    .match("^Kurswert (?<amountFx>[.,\\d]+)[-]? (?<currency>\\w{3})")
                    .match("^Kundenbonifikation (?<feeFy>[.,\\d]+) % vom Ausgabeaufschlag [.,\\d]+ \\w{3}")
                    .match("^Ausgabeaufschlag pro Anteil (?<feeFx>[.,\\d]+) %")
                    .assign((t, v) -> {
                        // Fee in percent
                        double amountFx = Double.parseDouble(v.get("amountFx").replace(',', '.'));
                        double feeFy = Double.parseDouble(v.get("feeFy").replace(',', '.'));
                        double feeFx = Double.parseDouble(v.get("feeFx").replace(',', '.'));
                        feeFy = (amountFx / (1 + feeFx / 100)) * (feeFx / 100) * (feeFy / 100);
                        String fee =  Double.toString((amountFx / (1 + feeFx / 100)) * (feeFx / 100) - feeFy).replace('.', ',');                        
                        v.put("fee", fee);

                        t.getPortfolioTransaction()
                                .addUnit(new Unit(Unit.Type.FEE,
                                                Money.of(asCurrencyCode(v.get("currency")),
                                                                asAmount(v.get("fee")))));
                    })

                    .wrap(BuySellEntryItem::new);

    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Erträgnisgutschrift.*EMAILVERSAND.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("isin", "name")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*)\\W+(?<isin>.+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares")
                        .match("^STK (?<shares>\\d+,\\d+?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .find("Wert Konto-Nr. (Devisenkurs )?Betrag zu Ihren Gunsten")
                        .match("(?<date>\\d+.\\d+.\\d{4}).*(?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "S Broker AG & Co. KG / Sparkasse"; //$NON-NLS-1$
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }
}

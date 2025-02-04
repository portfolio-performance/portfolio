package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDateTime;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.AdditionalLocales;

@SuppressWarnings("nls")
public class CetesDirectoPDFExtractor extends AbstractPDFExtractor
{

    public CetesDirectoPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CETES");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "CETES";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Movimientos del per");
        this.addDocumentTyp(type);
        
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block(
                        "^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z0-9]+(COMPRA|COMPSI|AMORTIZACION).*$");
        //Block firstRelevantLine = new Block("^Saldo inicial+.*$", "^Saldo final+.*$"); // This does not work
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //
                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })
                        
                        .oneOf(
                                        // @formatter:off
                                        // Registration | Settlement | ID+Type | Product | Series | Shares | Price | Term | Rate | Charge | Credit | Balance
                                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
                                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
                                        // @formatter:on
                                        section -> section
                                        .attributes("date", "id", "name", "series", "shares", "term", "rate", "amount") //
                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) (?<id>[A-Z0-9]+)(?<type>COMPRA|COMPSI) (?<name>[A-Z]+) (?<series>[\\dA-Z]+) (?<shares>[\\d,\\.]+) (?<price>[\\d,\\.]+) (?<term>[\\d]+) (?<rate>[\\d\\.]+) (?<amount>[\\d\\,\\.]+) .*$") //
                                        .assign((t, v) -> {
                                            v.put("currency", CurrencyUnit.MXN);
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDate(asDate(v.get("date")));
                                            t.setShares(asShares(v.get("shares")));
                                            t.setCurrencyCode(CurrencyUnit.MXN);
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setNote("ID:" + v.get("id") + " Series:" + v.get("series") + " Term:" + v.get("term") + " Rate:" + v.get("rate"));
                                                        }),

                                        // @formatter:off
                                        // Registration | Settlement | ID+Type | Product | Series | Shares | Term | Charge | Credit | Balance
                                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
                                        // Registration | Settlement | ID+Type | Product | Series | Term | Charge | Credit | Balance
                                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
                                        // @formatter:on
                                        section -> section.attributes("date", "id", "type", "name", "series", "shares", "term", "amount", "tax") //
                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) (?<id>[A-Z0-9]+)(?<type>AMORTIZACION) (?<name>[A-Z]+) (?<series>[\\dA-Z]+) (?<shares>[\\d,\\.]+) (?<term>[\\d,\\.]+) (?<charge>[\\d,\\.]+) (?<amount>[\\d\\,\\.]+).*$") //
                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z0-9]+ISR [A-Z]+ [\\dA-Z]+ [\\d]+ (?<tax>[\\d\\,\\.]+).*$") //
                                        .assign((t, v) -> {
                                            v.put("currency", CurrencyUnit.MXN);
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDate(asDate(v.get("date")));
                                            t.setShares(asShares(v.get("shares")));
                                            t.setCurrencyCode(CurrencyUnit.MXN);
                                            long amount = asAmount(v.get("amount"));
                                            long tax = asAmount(v.get("tax"));
                                            // Tax discounted after the amount.
                                            t.setAmount(amount-tax);
                                            Money moneyTax = Money.of(CurrencyUnit.MXN, tax);
                                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, moneyTax));
                                            if ("AMORTIZACION".equals(v.get("type")))
                                                t.setType(PortfolioTransaction.Type.SELL);
                                            t.setNote("ID:" + v.get("id") + " Series:" + v.get("series") + " Term:" + v.get("term"));
                                                        })

                        .wrap(BuySellEntryItem::new));

        //addTaxesSectionsTransaction(pdfTransaction, type); // Not needed apparently, after adding the TAX Unit.
        // addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) (?<id>[A-Z0-9]+)(?<type>ISR) (?<name>[A-Z]+) (?<series>[\\dA-Z]+) (?<term>[\\d]+) (?<tax>[\\d\\,\\.]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type));

    }

    @Override
    protected long asAmount(String value)
    {
        return asAmount(value, "es", "MX");
    }

    private LocalDateTime asDate(String date)
    {
        return asDate(date, AdditionalLocales.MEXICO);
    }

    @Override
    protected long asShares(String value)
    {
        return asShares(value, "es", "MX");
    }

}

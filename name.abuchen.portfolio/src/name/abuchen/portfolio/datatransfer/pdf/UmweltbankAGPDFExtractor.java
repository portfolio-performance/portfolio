package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

/**
 * @formatter:off
 * @implNote UmweltBank AG provides customer bonuses (Kundenbonifikation) that reduce issue fees.
 *
 *           Example from Kauf02:
 *           Kurswert 13.040,15 EUR
 *           Entgelt 247,76 EUR,
 *           Kundenbonifikation 100% vom Ausgabeaufschlag 380,70 EUR
 *           --> Final amount: 13.040,15 + 247,76 - 380,70 = 12.907,21 EUR
 *
 *           If bonus were 80%: Customer pays 380,70 * 0,20 = 76,14 EUR of the issue fee
 *           --> Final amount would be: 13.040,15 + 247,76 + 76,14 = 13.364,05 EUR
 * @formatter:on
 */

@SuppressWarnings("nls")
public class UmweltbankAGPDFExtractor extends AbstractPDFExtractor
{
    public UmweltbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UmweltBank AG");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UmweltBank AG";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Stück 500 UMWELTBANK ETF-GL SDG FOCUS        LU2679277744 (A3EV2A)
                                        // ACT.PORT. P EUR ACC. ON
                                        // Ausführungskurs 10,422 EUR Auftragserteilung/ -ort Online-Banking
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Stück 500 UMWELTBANK ETF-GL SDG FOCUS        LU2679277744 (A3EV2A)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag/-Zeit 12.06.2025 09:04:17 Auftraggeber wkRmoM OWCDHzpI qPtTDs
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 06.05.2025 Auftraggeber wkRmoM OWCDHzpI qPtTDs
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Ausmachender Betrag 5.268,32- EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kurswert 13.040,15- EUR
                        // Kundenbonifikation 100 % vom Ausgabeaufschlag 380,70 EUR
                        // @formatter:on
                        .section("gross", "grossCurrency", "fee", "feeCurrency").optional() //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)\\- (?<grossCurrency>[\\w]{3})$") //
                        .match("^Kundenbonifikation 100 % vom Ausgabeaufschlag (?<fee>[\\.,\\d]+) (?<feeCurrency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            var gross = Money.of(asCurrencyCode(v.get("grossCurrency")), asAmount(v.get("gross")));
                            var fee = Money.of( asCurrencyCode(v.get("feeCurrency")), asAmount(v.get("fee")));

                            t.setMonetaryAmount(gross.subtract(fee));
                        })


                        // @formatter:off
                        //  Auftragsnummer 715355/24.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 1,1000 % vom Kurswert 57,32- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision [\\.,\\d]+ % vom Kurswert (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kurswert 13.040,15- EUR
                        // Entgelt 247,76- EUR
                        // Kundenbonifikation 100 % vom Ausgabeaufschlag 380,70 EUR
                        // @formatter:on
                        .section("gross", "grossCurrency", "fee", "feeCurrency", "discount", "discountCurrency").optional() //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)\\- (?<grossCurrency>[A-Z]{3})$") //
                        .match("^Entgelt (?<fee>[\\.,\\d]+)\\- (?<feeCurrency>[A-Z]{3})$") //
                        .match("^Kundenbonifikation [\\.,\\d]+ % vom Ausgabeaufschlag (?<discount>[\\.,\\d]+) (?<discountCurrency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var gross = Money.of(asCurrencyCode(v.get("grossCurrency")), asAmount(v.get("gross")));
                            var fee = Money.of(asCurrencyCode(v.get("feeCurrency")), asAmount(v.get("fee")));
                            var discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            // portion of the issue fee actually applied
                            var appliedFeePortion = gross.subtract(gross.subtract(discount));

                            // @formatter:off
                            // totalFees = fixed fee + (applied portion - discount)
                            // @formatter:on
                            var totalFees = fee.add(appliedFeePortion.subtract(discount));

                            checkAndSetFee(totalFees, t, type.getCurrentContext());
                        });
    }
}

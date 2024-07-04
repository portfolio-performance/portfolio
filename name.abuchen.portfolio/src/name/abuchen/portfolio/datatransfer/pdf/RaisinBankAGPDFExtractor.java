package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class RaisinBankAGPDFExtractor extends AbstractPDFExtractor
{
    public RaisinBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raisin Bank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Raisin Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie (gekauft|verkauft)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Kauf|Verkauf).*$");
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
                        .match("^(?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // VANG.FTSE D.A.P.X.J.DLD IE00B9F5YL18
                        // 0,18854 Stück 24,08 EUR 4,54 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                        .match("^[\\.,\\d]+ St.ck [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 0,18854 Stück 24,08 EUR 4,54 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) St.ck [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf(
                                        // @formatter:off
                                        // Handelsdatum: 24.05.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelsdatum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Handelsdatum, Uhrzeit: 23.05.2024 12:10:43
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelsdatum, Uhrzeit: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Lasten
                        // 27.05.2024 4,54 EUR
                        //
                        // Valuta Betrag zu Ihren Gunsten
                        // 04.07.2024 0,88 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("Valuta Betrag zu Ihren (Lasten|Gunsten).*$")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abrechnungsnummer
                        // 95a8535f-0aa7-4197-85ae-3f7337d9232d
                        //
                        // Abrechnungsnummer
                        // IzFqnHk IyBqtsnat XKGoEM bf7448fe-2a12-4c8e-bc98-e6b7411fb56b
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Abrechnungsnummer).*$")
                        .match("^(.* )?(?<note2>[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+).*$") //
                        .find("Wir haben f.r.*") //
                        .assign((t, v) -> t.setNote(concatenate(trim(v.get("note1")), trim(v.get("note2")), ": ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("Aus(s)?ch.ttung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Aus(s)?ch.ttung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // VANG.FTSE DEV.EU.UETF EOD IE00B945VV12
                        // 154,9996 Stück 0,7397 EUR 114,66 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                        .match("^[\\.,\\d]+ St.ck [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 0,18854 Stück 24,08 EUR 4,54 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) St.ck [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Gunsten
                        // 26.06.2024 92,21 EUR
                        // @formatter:on
                        .section("date") //
                        .find("Valuta Betrag zu Ihren Gunsten.*")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}.*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Valuta Betrag zu Ihren Gunsten
                        // 26.06.2024 92,21 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .find("Valuta Betrag zu Ihren Gunsten.*")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 6,38096 Stück 0,4874 USD 3,11 USD
                        // Devisenkurs: 1,0704 EUR/USD
                        // @formatter:on
                        .section("fxGross", "exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^[\\.,\\d]+ St.ck [\\.,\\d]+ [\\w]{3} (?<fxGross>[\\.,\\d]+) [\\w]{3}.*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer
                        // 680ZY015-5cfb-4240-961c-865127t5147j
                        //
                        // Auftragsnummer
                        // eZqTLXa ODRtHoPVn XjThGc 237dcf5e-bbe9-4127-9eef-4153d9c85ccf
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Auftragsnummer).*")
                        .match("^(.* )?(?<note2>[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+\\-[\\w]+).*$") //
                        .assign((t, v) -> t.setNote(concatenate(trim(v.get("note1")), trim(v.get("note2")), ": ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Einbehaltene deutsche Kapitalertragsteuer: 19,63 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Einbehaltene deutsche Kapitalertrags(s)?teuer: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragssteuer: 0,04 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene deutsche Kirchensteuer: 1,76 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Einbehaltene deutsche Kirchensteuer: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag: 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltener deutscher Solidaritätszuschlag: 1,07 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Einbehaltener deutscher Solidarit.tszuschlag: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer: 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer: (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}

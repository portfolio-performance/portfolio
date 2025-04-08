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
public class GladbacherBankAGPDFExtractor extends AbstractPDFExtractor
{
    public GladbacherBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Gladbacher Bank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Gladbacher Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gladbacher Bank AG.*$");
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
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 28 CHURCH & DWIGHT CO. INC.           US1713401024 (864371)
                        // REGISTERED SHARES DL 1
                        // Handels-/Ausführungsplatz Tradegate (gemäß Weisung)
                        // Ausführungskurs 85,00 EUR Auftragserteilung/ -ort Online-Banking
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 28 CHURCH & DWIGHT CO. INC.           US1713401024 (864371)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 18.12.2023 18:14:20 Auftraggeber Max Mustermann
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 2.395,10- EUR
                        // Ausmachender Betrag 3.698,50 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Rechnungsnummer W07512-0000013677/23
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Market-Order
                        // Limit billigst
                        // @formatter:on
                        .section("note").optional() //
                        .find("Market-Order") //
                        .match("^(?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gladbacher Bank AG.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 52 AMETEK INC. US0311001004 (908668)
                        // REGISTERED SHARES DL -,01
                        // Zahlbarkeitstag 22.12.2023 Dividende pro Stück 0,25 USD
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*)[\\s]{1,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Zahlbarkeitstag .* Dividende pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 52 AMETEK INC. US0311001004 (908668)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 28.12.2023 zu Gunsten des Kontos 123456789 (IBAN DE65 3106 0181 2345 6789
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 8,71+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD  1,1109
                        // Dividendengutschrift 13,00 USD 11,70+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Dividendengutschrift (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        //  Abrechnungsnr. 86148344230
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.*(?<note1>Abrechnungsnr\\.) ([\\s]+)?(?<note2>[\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        // @formatter:off
                        // Ex-Tag 07.12.2023 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 25 % auf 4,66 EUR 1,17- EUR
                        // Kapitalertragsteuer 25,00% auf 291,57 EUR 72,89- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s])?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
                        // Solidaritätszuschlag 5,50% auf 72,89 EUR 4,00- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s])?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 5,5 % auf 23,41 EUR 1,28- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s])?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 13,00 USD 1,76- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s])?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 35,23- EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer (?<creditableWithHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 11,70 EUR 1,76 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s])?% .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 15,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,10- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

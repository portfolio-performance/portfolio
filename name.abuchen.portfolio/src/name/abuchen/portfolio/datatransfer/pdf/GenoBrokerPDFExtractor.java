package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

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
public class GenoBrokerPDFExtractor extends AbstractPDFExtractor
{
    public GenoBrokerPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("GENO Broker GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "GENO Broker GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                        // @formatter:off
                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                        // Stück 30 Carbios SA Anrechte Aktie FR001400IRI9 (A3EJEH)
                        // 1Ausführungskurs 30,88 EUR Auftraggeber Mustermann
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^.*Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 30 Carbios SA Anrechte Aktie FR001400IRI9 (A3EJEH)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 30.06.2023 09:57:4 Fällig am 07.07.2023
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 967,47 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 210796978 Kundenportfolio
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.*(?<note1>Auftragsnummer).*$") //
                        .match(".* (?<note2>[\\d]+) .* Datum.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        // @formatter:off
                        // Limit billigst
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> {
                            if (t.getNote() != null)
                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                            else
                                t.setNote(trim(v.get("note")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^.* Kundenservice .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                        // @formatter:off
                        // Stück 1.000                              CROPENERGIES AG
                        // INHABER-AKTIEN O.N.
                        // DE000A0LAUP1    (A0LAUP)
                        // Dividende pro Stück                      0,60         EUR
                        // @formatter:on
                        .section("name", "nameContinued", "isin", "wkn", "currency") //
                        .match("^St.ck [\\.,\\d]+ ([\\s]+)?(?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\s]+)?\\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^Dividende pro St.ck .* (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 1.000                              CROPENERGIES AG
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbarkeitstag                         14.07.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbarkeitstag ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag        445,94+   EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag ([\\s]+)?(?<amount>[\\.,\\d]+)\\+ ([\\s]+)?(?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs                             EUR / CAD  1,4915
                        // Dividendengutschrift                                   14.133,00     CAD        9.475,70+   EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional() //
                        .match("^Devisenkurs ([\\s]+)?(?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Dividendengutschrift ([\\s]+)?(?<fxGross>[\\.,\\d]+) ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<gross>[\\.,\\d]+)\\+ ([\\s]+)?(?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        //      Abrechnungsnr.      60007000
                        // Abrechnungsnr.        000000000
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.*(?<note1>Abrechnungsnr\\.) ([\\s]+)?(?<note2>[\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Einbehaltene Quellensteuer 25 % auf 14.133,00 CAD     2.368,93-    EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} .* (?<withHoldingTax>[\\.,\\d]+)\\- ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 9.475,70 EUR             1.421,36     EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} .* (?<creditableWithHoldingTax>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Verrechnete anrechenbare ausländische Quellensteuer
                        // (Verhältnis 100/25) auf 3,96 EUR    15,87  -  EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .find("Verrechnete anrechenbare ausl.ndische Quellensteuer") //
                        .match("^\\(Verh.ltnis .*\\) auf [\\.,\\d]+ [\\w]{3} ([\\s]+)?(?<creditableWithHoldingTax>[\\.,\\d]+) ([\\s]+)?(\\-)? ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Kapitalertragsteuer 25 % auf 584,13 EUR      146,03-    EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} .* (?<tax>[\\.,\\d]+)\\- ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 146,03 EUR     8,03-    EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} .* (?<tax>[\\.,\\d]+)\\- ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 5,5 % auf 146,03 EUR     8,03-    EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} .* (?<tax>[\\.,\\d]+)\\- ([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Provision 32,95-EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\-([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 5,60- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\-([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsentgelt 2,52- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Handelsentgelt (?<fee>[\\.,\\d]+)\\-([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

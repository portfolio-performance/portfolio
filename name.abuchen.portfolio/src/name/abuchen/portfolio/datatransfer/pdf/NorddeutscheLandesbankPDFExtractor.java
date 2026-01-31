package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class NorddeutscheLandesbankPDFExtractor extends AbstractPDFExtractor
{
    public NorddeutscheLandesbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Norddeutsche Landesbank");

        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Norddeutsche Landesbank";
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("(Dividendengutschrift)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Norddeutsche Landesbank Girozentrale.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 140 NOVARTIS AG CH0012005267 (904278)
                                        // NAMENS-AKTIEN SF 0,49
                                        // Zahlbarkeitstag 13.03.2025 Dividende pro Stück 3,50 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Dividende pro St.ck [\\.,\\d]+ (?<currency>[A-Z]{3})$") //)
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Stück 140 NOVARTIS AG CH0012005267 (904278)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Den Betrag buchen wir mit Wertstellung 17.03.2025 zu Gunsten des Kontos 8220092118 (IBAN Ex76 3069 2250 0086
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag 330,80+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)[\\+\\-]? (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs EUR / CHF 0,9628
                                        // Devisenkursdatum 13.03.2025
                                        // Dividendengutschrift 490,00 CHF 508,93+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .match("^Dividendengutschrift [\\.,\\d]+ [A-Z]{3} (?<gross>[\\.,\\d]+)(\\+) [A-Z]{3}$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Abrechnungsnr. 39001696226
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Abrechnungsnr\\. .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))


                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Einbehaltene Quellensteuer 35 % auf 490,00 CHF 178,13- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+.* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 508,93 EUR 76,34 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+.* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }
}

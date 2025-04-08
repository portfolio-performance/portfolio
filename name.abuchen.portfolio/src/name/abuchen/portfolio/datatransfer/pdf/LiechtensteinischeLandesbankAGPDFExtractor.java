package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Liechtensteinische Landesbank AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class LiechtensteinischeLandesbankAGPDFExtractor extends AbstractPDFExtractor
{
    public LiechtensteinischeLandesbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Liechtensteinische Landesbank");

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Liechtensteinische Landesbank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung \\- Ihr (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^B.rsenabrechnung \\- Ihr (Kauf|Verkauf).*$");
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
                        .match("^B.rsenabrechnung \\- Ihr (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Auftragsnummer XXXXXXXXX
                        // Ant Plenum CAT Bond Fund Klasse -P CHF-
                        // Kurs CHF 104.42
                        // ISIN LI0290349492
                        // Valorennummer 29034949
                        // @formatter:on
                        .section("name", "currency", "isin", "wkn") //
                        .find("Auftragsnummer .*") //
                        .match("^(?<name>.*)$") //
                        .match("^Kurs (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Anzahl / Nominal 1.394011
                        // @formatter:on
                        .section("shares")
                        .match("^Anzahl \\/ Nominal (?<shares>[\\.'\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 22. November 2023 CHF 145.56
                        // @formatter:on
                        .section("date") //
                        .match("^Zu Ihren (Lasten|Gunsten) Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) [\\w]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 22. November 2023 CHF 145.56
                        // Zu Ihren Gunsten Valuta 7. November 2023 CHF 55.10
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren (Lasten|Gunsten) Valuta [\\d]{1,2}\\. .* [\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bruttobetrag EUR 48.47
                        // Umrechnungskurs EUR/CHF 0.964296
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional()
                        .match("^Bruttobetrag [\\w]{3} (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Umrechnungskurs (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer XXXXXXXXX
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Bardividende \\(Ordentliche Dividende\\)" //
                        + "|Wahldividende)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Bardividende \\(Ordentliche Dividende\\)" //
                        + "|Wahldividende)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Auftragsnummer 623950393
                                        // Reg Shs Healthpeak Pptys Inc
                                        // ISIN US42250P1030
                                        // Valorennummer 50880191
                                        // Zahlungswert USD 0.30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^Zahlungswert (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Auftragsnummer XXXXXXXXX
                                        // Reg Shs Pearson PLC
                                        // ISIN GB0006776081
                                        // Zahlungswert GBP 0.07 pro Stück
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .find("Auftragsnummer .*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Valorennummer (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^Zahlungswert (?<currency>[\\w]{3}) [\\.'\\d]+ pro St.ck$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Ihr Bestand per 06.11.2023 25.114744 Stück
                        // @formatter:on
                        .section("shares")
                        .match("^Ihr Bestand per .* (?<shares>[\\.'\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 20. November 2023 CHF 5.65
                        // @formatter:on
                        .section("date") //
                        .match("^Zu Ihren Gunsten Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) [\\w]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 20. November 2023 CHF 5.65
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren Gunsten Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bruttobetrag USD 7.53
                        // Umrechnungskurs USD/CHF 0.882477
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional()
                        .match("^Bruttobetrag [\\w]{3} (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Umrechnungskurs (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer 623950393
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug in", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug in EUR 01.12.2023 - 31.12.2023
                                        // @formatter:on
                                        .section("currency", "year") //
                                        .match("^Kontoauszug in (?<currency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("currency", asCurrencyCode(v.get("currency")));
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        // @formatter:off
        // 01.02. Gutschrift 01.02. 465.86 10'522.58
        // XXXXX XXXXX
        // Auftragsnummer: XXXXXXXXX
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}. Gutschrift [\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}. Gutschrift (?<date>[\\d]{2}\\.[\\d]{2}.) (?<amount>[\\.'\\d]+) [\\.'\\d]+$") //
                        .match("^(?<note>Auftragsnummer: .*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Per 31. Dezember 2023
        // Abrechnungsperiode 30.09.2023-31.12.2023
        // Habenzins 456.60
        // @formatter:on
        Block interestBlock = new Block("^Per [\\d]{1,2}\\. .* [\\d]{4}$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note1", "note2", "amount") //
                        .documentContext("currency") //
                        .match("^Per (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                        .match("^Abrechnungsperiode (?<note1>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})\\-(?<note2>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .match("^Habenzins (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note1") + " - " + v.get("note2"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidgenössische Stempelsteuer CHF 0.04
                        // Eidgenössische Stempelsteuer CHF -0.01
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidgen.ssische Stempelsteuer (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Finanztransaktionssteuer Spanien EUR 0.10
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Finanztransaktionssteuer .* (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 15 % Quellensteuer USD -1.13
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^[\\d]+ % Quellensteuer (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class UBSAGBankingAGPDFExtractor extends AbstractPDFExtractor
{
    public UBSAGBankingAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UBS"); //$NON-NLS-1$
        addBankIdentifier("UBS Switzerland AG"); //$NON-NLS-1$
        addBankIdentifier("www.ubs.com"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UBS AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rse (Kauf|Verkauf) Komptant");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Bewertet in: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^.* B.rse (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // USD 2'180 UBS (Lux) Fund Solutions - MSCI 21966836
                // Emerging Markets UCITS ETF LU0950674175
                .section("currency", "name", "wkn", "nameContinued", "isin")
                .match("^(?<currency>[\\w]{3}) [\\.'\\d]+ (?<name>.*) (?<wkn>[0-9]{6,9})$")
                .match("^(?<nameContinued>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                .section("shares")
                .match("^.* Komptant (\\-)?(?<shares>[\\.',\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                .section("date", "time")
                .match("^Abschluss (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Abrechnungsbetrag USD -4'919.95
                // Abrechnungsbetrag USD 11'050.04
                .section("currency", "amount")
                .match("^Abrechnungsbetrag (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Buchung 10.03.2022 XXXXXXXXXXXXXXXX 0.9267
                // 15:02:13 450 USD 10.87
                // Abrechnungsdetails Bewertet in  CHF
                // Transaktionswert USD 4'890.60 4'532
                .section("exchangeRate", "baseCurrency", "termCurrency", "currency", "gross").optional()
                .match("^Buchung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<exchangeRate>[\\.'\\d]+)$")
                .match("^Abrechnungsdetails Bewertet in .*(?<termCurrency>[\\w]{3})$")
                .match("^Transaktionswert (?<currency>[\\w]{3}) (?<gross>[\\.'\\d]+) [\\.'\\d]+$")
                .match("^Abrechnungsbetrag (?<baseCurrency>[\\w]{3}) (\\-)?[\\.'\\d]+$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Gutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("DIVIDENDENZAHLUNG$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // STUECKZAHL VALOR 957150 ISIN US6541061031 ANSATZ
                // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                // BRUTTO USD 6.10
                .section("wkn", "isin", "name", "tickerSymbol", "currency")
                .match("^STUECKZAHL VALOR (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.',\\d]+ (?<name>.*) \\((?<tickerSymbol>.*)\\) BRUTTO$")
                .match("^BRUTTO (?<currency>[\\w]{3}) [\\.'\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                .section("shares")
                .match("^(?<shares>[\\.',\\d]+) .* \\(.*\\) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                .section("date")
                .match("^GUTSCHRIFT KONTO .* VALUTA (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.'\\d]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                .section("currency", "amount")
                .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // BRUTTO USD 6.10
                // UMRECHNUNGSKURS USD/CHF 0.901639
                .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional()
                .match("^BRUTTO (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.'\\d]+)$")
                .match("^UMRECHNUNGSKURS (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+)$")
                .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.'\\d]+$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // STEUERABZUG 30.00% USD -1.83
                .section("currency", "tax").optional()
                .match("^STEUERABZUG [\\.'\\d]+% (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Diverse USD -7.34
                .section("currency", "fee").optional()
                .match("^Diverse (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Courtage USD -22.01
                .section("currency", "fee").optional()
                .match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }


    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

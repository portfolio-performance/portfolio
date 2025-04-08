package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
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

@SuppressWarnings("nls")
public class FindependentAGPDFExtractor extends AbstractPDFExtractor
{
    public FindependentAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Findependent AG");

        addBuySellTransaction();
        addDividendsTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Findependent AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("ETF\\-(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^ETF\\-(Kauf|Verkauf)$", "^Erstellt am:.*$");
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
                        .match("^ETF\\-(?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // ETF-Name Vanguard FTSE All World ETF
                        // ISIN IE00B3RBWM25
                        // Preis pro Anteil CHF 100.040
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^ETF-Name (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Preis pro Anteil (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Anzahl Anteile 2
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl Anteile (?<shares>[\\',\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 01.12.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Verrechneter Betrag CHF 200.43
                        // Gutgeschriebener Betrag CHF 700.02
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Verrechneter|Gutgeschriebener) Betrag (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Kaufpreis total USD 912.50
                        // Wechselkurs CHF/USD 0.87555
                        // Kaufpreis CHF CHF 798.94
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Kaufpreis total [\\w]{3} (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Wechselkurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .match("^Kaufpreis [\\w]{3} [\\w]{3} (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("Ertragsaussch.ttung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Ertragsaussch.ttung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // ETF-Name UBS SXI Real Estate Funds ETF
                        // ISIN CH0105994401
                        // Bruttoertrag pro Anteil CHF 0.1200
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^ETF-Name (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Bruttoertrag|Ertrag) pro Anteil (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Anzahl Anteile 58
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl Anteile (?<shares>[\\.'\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 13.09.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ertrag total CHF CHF 4.52
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ertrag total [\\w]{3} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("Deinem Konto wurde (gut(ge)?schrieben|belastet):");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Einzahlung" //
                        + "|Willkommensbonus von findependent" //
                        + "|Depotgeb.hren an" //
                        + "|Verwaltungsgeb.hren an" //
                        + "|Geb.hrenerstattung von).*$"); //
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Depotgebühren" or "Verwaltungsgebühren" change from DEPOSIT to FEES
                        // Is type --> "Gebührenerstattung" change from DEPOSIT to FEES_REFUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>(Depotgeb.hren|Verwaltungsgeb.hren|Geb.hrenerstattung)) (an|von).*$") //
                        .assign((t, v) -> {
                            if ("Depotgebühren".equals(v.get("type")) || "Verwaltungsgebühren".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES);

                            if ("Gebührenerstattung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES_REFUND);
                        })

                        // @formatter:off
                        // Valuta 06.11.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Betrag CHF 5'100.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Betrag (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Depotgebühren CHF 1.95
                                        // Verwaltungsgebühren CHF 2.45
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(Depotgeb.hren|Verwaltungsgeb.hren) (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Zahlungsgrund SE-2
                        // Zahlungsgrund findependent Gutschrift Q3
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zahlungsgrund (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")))) //

                        // @formatter:off
                        // Depotgebühren CHF 1.95
                        // Periode 01.07.2023 - 30.09.2023
                        //
                        // Verwaltungsgebühren CHF 2.45
                        // Periode 01.10.2023 - 31.12.2023
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>(Depotgeb.hren|Verwaltungsgeb.hren)) [\\w]{3} [\\.'\\d]+$") //
                        .match("^Periode (?<note2>.*)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), trim(v.get("note1")), " "));
                            t.setNote(concatenate(t.getNote(), trim(v.get("note2")), " "));
                        })

                .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Verrechnungssteuer (35%) CHF -2.44
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer \\([\\d]+%\\) (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Stempelabgaben CHF 0.30
                        // Stempelabgaben CHF -1.05
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Stempelabgaben (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Börsenabgaben CHF 0.05
                        // Börsenabgaben CHF -0.05
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsenabgaben (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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

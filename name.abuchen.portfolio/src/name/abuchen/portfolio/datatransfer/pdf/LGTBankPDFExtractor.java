package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LGTBankPDFExtractor extends AbstractPDFExtractor
{
    /**
     * Documents of the Austrian branch use the German number format (1.234,56)
     * instead of the Swiss number format (1'234.56). Amounts always have two
     * decimal places and are detected automatically, shares and exchange rates
     * depend on this flag.
     */
    private static final String IS_AUSTRIAN_BRANCH = "isAustrianBranch";

    public LGTBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LGT Bank");

        addBuySellTransaction();
        addDividendeTransaction();
        addFundAccumulationTransaction();
        addForeignExchangeTransaction();
        addCallMoneyInterestTransaction();
        addCallMoneyReductionTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "LGT Bank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Abrechnung (Kauf|Verkauf|Zeichnung)", this::addAustrianBranchContext);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Abrechnung (Kauf|Verkauf|Zeichnung).*$", "^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Abrechnung (?<type>(Kauf|Verkauf|Zeichnung)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel A.P. Moeller - Maersk A/S
                                        // Namen- und Inhaber-Aktien -B-
                                        // ISIN DK0010244508
                                        // Valorennummer 906020
                                        // Wertpapierkennnummer 861837
                                        // Kurswert DKK 80'784.00
                                        //
                                        // Titel  New York Times Co
                                        // Namen-Aktien -A-
                                        // ISIN US6501111073
                                        // Valorennummer 956840
                                        // Wertpapierkennnummer  857534
                                        // Kurswert USD 10.769,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "wkn", "currency") //
                                                        .match("^Titel (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]*$") //
                                                        .match("^Wertpapierkennnummer (?<wkn>.*)$") //
                                                        .match("^Kurswert (?<currency>[A-Z]{3}) [\\.,'\\d]+[\\s]*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Titel JPMorgan Funds SICAV - Emerging Markets Sustainable Equity
                                        // Fund
                                        // Namen-Anteile -C- / Class USD
                                        // ISIN LU2051469208
                                        // Valorennummer 50139326
                                        // Kurswert USD 50'520.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^Titel (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]*$") //
                                                        .match("^Kurswert (?<currency>[A-Z]{3}) [\\.,'\\d]+[\\s]*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Anzahl 12 Stück
                        // Anzahl  168 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl[\\s]+(?<shares>[\\.,'\\d]+) St.ck[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), type)))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschlussdatum 14.04.2020 09:00:02
                                        // Abschlussdatum  07.10.2026 22:00:03
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Abschlussdatum[\\s]+(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})[\\s]*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Abschlussdatum 04.07.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Abschlussdatum[\\s]+(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zeichnungstag (NAV) 08.06.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zeichnungstag .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Belastung DKK Konto 0037156.021 DKK 82'452.21
                        // Belastung CHF Freizügigkeitskonto 2026457.031 CHF 48'502.01
                        // Gutschrift USD Konto 2026457.055 USD 8'332.19
                        // Gutschrift USD Konto 9332814.677 USD 10.164,42
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Belastung|Gutschrift) [A-Z]{3} .* (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 210796978 Kundenportfolio
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Valorennummer 906020
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Valorennummer .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Baraussch.ttung", this::addAustrianBranchContext);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Baraussch.ttung .*$", "^Valuta [\\d]{1,2}\\. .* [\\d]{4}[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Stand Ihres Depots am 12. Mai 2020:
                        // 551 Veolia Environnement SA
                        // Namen- und Inhaber-Aktien
                        // ISIN: FR0000124141, Valoren-Nr.: 1098758
                        // Ausschüttung EUR 0.50
                        //
                        // Ihr Bestand am 31. August 2026:
                        // 100 Union Pacific Corp
                        //  Namen-Aktien
                        //  ISIN: US9078181081, Valoren-Nr.: 979840
                        // Ausschüttung USD 1,42
                        // @formatter:on
                        .section("name", "nameContinued", "isin", "wkn", "currency") //
                        .find("(Stand Ihres Depots|Ihr Bestand) am [\\d]{1,2}\\. .* [\\d]{4}:[\\s]*") //
                        .match("^[\\.,'\\d]+ (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.*ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]), Valoren\\-Nr\\.: (?<wkn>.*)$") //
                        .match("^Aussch.ttung (?<currency>[A-Z]{3}) [\\.,'\\d]+[\\s]*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 551 Veolia Environnement SA
                        // @formatter:on
                        .section("shares") //
                        .find("(Stand Ihres Depots|Ihr Bestand) am [\\d]{1,2}\\. .* [\\d]{4}:[\\s]*") //
                        .match("^(?<shares>[\\.,'\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), type)))

                        // @formatter:off
                        // Valuta 14. Mai 2020
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ex-Datum 12. Mai 2020
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ex\\-Datum (?<exDate>[\\d]{1,2}\\. .* [\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Netto EUR 198.36
                        // Netto USD 102,94
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 330401346 Kontonummer: 1234567.031
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Ausschüttungsart Ordentliche Dividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Aussch.ttungsart (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addFundAccumulationTransaction()
    {
        final var type = new DocumentType("Fondsthesaurierung", this::addAustrianBranchContext);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Fondsthesaurierung .*$", "^Valuta [\\d]{1,2}\\. .* [\\d]{4}[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        // A tax charge on accumulated income (ausschüttungsgleicher
                        // Ertrag) has no ex-date
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        // @formatter:off
                        // Ihr Bestand am 28. Juli 2026:
                        // 228,889 LGT PB AIF SICAV - LGT Global Private Credit (LGPC) Feeder Fund
                        //  Inhaber-Anteile -C- / Class EUR
                        //  ISIN: LI1286282879, Valoren-Nr.: 128628287
                        // Wir rechnen folgende Erträge unter Eingangsvorbehalt ab:
                        // Fondsergebnis der Meldeperiode EUR 6,2315
                        // @formatter:on
                        .section("shares", "name", "nameContinued", "isin", "wkn", "currency") //
                        .find("Ihr Bestand am [\\d]{1,2}\\. .* [\\d]{4}:[\\s]*") //
                        .match("^(?<shares>[\\.,'\\d]+) (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.*ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]), Valoren\\-Nr\\.: (?<wkn>.*)$") //
                        .match("^Fondsergebnis der Meldeperiode (?<currency>[A-Z]{3}) [\\.,'\\d]+[\\s]*$") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares"), type));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Valuta 28. August 2026
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\. .* [\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Lasten EUR 323,35
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren Lasten (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 197707898 Kontonummer: 3852893.339
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(trim(v.get("note")), "Fondsthesaurierung", " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addForeignExchangeTransaction()
    {
        final var type = new DocumentType("Devisenkassagesch.ft", "Kontoauszug", this::addAustrianBranchContext);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransferEntry>();

        var firstRelevantLine = new Block("^Devisenkassagesch.ft[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(AccountTransferEntry::new)

                        // @formatter:off
                        // Transaktionsdatum  08.10.2026  09:09:52
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Transaktionsdatum[\\s]+(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]+(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})[\\s]*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Belastung SGD Konto 3287370.268 SGD 90,85
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Belastung [A-Z]{3} .* (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> t.getSourceTransaction()
                                        .setMonetaryAmount(Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")))))

                        // @formatter:off
                        // Gutschrift USD Konto 3894370.206 USD 70,22
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift [A-Z]{3} .* (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> t.getTargetTransaction()
                                        .setMonetaryAmount(Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")))))

                        // @formatter:off
                        // Kassakurs  1,293876
                        // @formatter:on
                        .section("exchangeRate") //
                        .match("^Kassakurs[\\s]+(?<exchangeRate>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            var source = t.getSourceTransaction().getMonetaryAmount();
                            var target = t.getTargetTransaction().getMonetaryAmount();
                            var exchangeRate = asExchangeRate(v.get("exchangeRate"), type);

                            // The spot rate is quoted by market convention
                            // without currency pair, so it can be expressed in
                            // either direction
                            if (!Unit.isWithinRoundingTolerance(source, target, exchangeRate))
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                            t.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, source, target, exchangeRate));
                        })

                        // @formatter:off
                        // Auftragsnummer: 204939646 Portfolio: 5982370.181
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> new AccountTransferItem(t, true));
    }

    private void addCallMoneyInterestTransaction()
    {
        final var type = new DocumentType("Zinsabrechnung Callgeld");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Zinsabrechnung Callgeld .*$", "^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        // @formatter:off
                        // Valuta 30.09.2026
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift EUR Konto 2005875.935 437,95
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift (?<currency>[A-Z]{3}) Konto .* (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer: 203545881 Portfolio: 3569975.224
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Zinsabrechnung Callgeld EUR Callgeld AT.850340
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zinsabrechnung Callgeld [A-Z]{3} (?<note>Callgeld .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addCallMoneyReductionTransaction()
    {
        final var type = new DocumentType("Reduktion Callgeld");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Reduktion Callgeld .*$", "^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        // @formatter:off
                        // Valuta 18.09.2026
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // The money is only moved from the call money account to
                        // the current account, therefore it is not imported
                        //
                        // @formatter:off
                        // Gutschrift EUR Konto 3254470.096 EUR 1.000,00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift [A-Z]{3} .* (?<currency>[A-Z]{3}) (?<amount>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            v.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);
                        })

                        // @formatter:off
                        // Auftragsnummer: 202616273 Portfolio: 7962388.454
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Reduktion Callgeld EUR Callgeld AT.1115562
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Reduktion Callgeld [A-Z]{3} (?<note>Callgeld .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug [A-Z]{3} Konto", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug EUR Konto
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Kontoauszug (?<currency>[A-Z]{3}) Konto[\\s]*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 30.06.2026 Saldovortrag   26.286,50
        // 07.08.2026 Auftrag 198827910  07.08.2026   804,64   30.404,49
        // @formatter:on
        var balanceLine = Pattern.compile("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*[\\s](\\-)?[\\.\\d]+,[\\d]{2}[\\s]*$");

        // @formatter:off
        // 28.08.2026 Steuerkorrektur 28.08.2026   98,59   30.305,90
        // 17.09.2026 Quartalsgebühren 30.09.2026   1.146,34   30.114,49
        // @formatter:on
        var bookingLine = Pattern.compile("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Steuerkorrektur|Quartalsgeb.hren) .*$");

        // Debit and credit amounts cannot be distinguished by their column,
        // therefore each booking is combined with the previous balance line
        var bookingSplittingStrategy = (SplittingStrategy) lines -> {
            var spans = new ArrayList<LineSpan>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (!bookingLine.matcher(lines[ii]).matches())
                    continue;

                for (var jj = ii - 1; jj >= 0; jj--)
                {
                    if (balanceLine.matcher(lines[jj]).matches())
                    {
                        spans.add(new LineSpan(jj, ii));
                        break;
                    }
                }
            }

            return spans;
        };

        var bookingBlock = new Block(bookingSplittingStrategy);
        type.addBlock(bookingBlock);
        bookingBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        // @formatter:off
                        // 22.07.2026 Auftrag 197135683 22.07.2026   261,43   30.424,68
                        // Bardividende N Enel
                        // 05.08.2026 Steuerkorrektur 05.08.2026   187,17   30.611,85
                        // @formatter:on
                        .section("previousBalance", "date", "note", "amount", "balance") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*[\\s](?<previousBalance>(\\-)?[\\.\\d]+,[\\d]{2})[\\s]*$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>(Steuerkorrektur|Quartalsgeb.hren)) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]+(?<amount>[\\.\\d]+,[\\d]{2})[\\s]+(?<balance>(\\-)?[\\.\\d]+,[\\d]{2})[\\s]*$") //
                        .assign((t, v) -> {
                            var isCredit = asBalance(v.get("balance")).compareTo(asBalance(v.get("previousBalance"))) > 0;

                            if ("Steuerkorrektur".equals(v.get("note")))
                                t.setType(isCredit ? AccountTransaction.Type.TAX_REFUND : AccountTransaction.Type.TAXES);
                            else
                                t.setType(isCredit ? AccountTransaction.Type.FEES_REFUND : AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Umsatzabgabe  DKK 121.19
                        // Eidg. Umsatzabgabe  USD -12.51
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Eidg\\. Umsatzabgabe ([\\s]+)?(?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Verrechnungssteuer 35 % CHF -851.20
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Verrechnungssteuer [\\d]+ % (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KESt   USD -604,36
                        // KESt USD -17,76
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt[\\s]+(?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 28 % EUR -77.14
                        // Quellensteuer 15 % USD -21,30
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer [\\d]+ % (?<currency>[A-Z]{3}) (\\-)?(?<withHoldingTax>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage  DKK 1'534.90
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage[\\s]{1,}(?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SIX Meldegebühr  CHF 0.20
                        // SIX Meldegebühr  CHF -0.20
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.* Meldegeb.hr[\\s]{1,}(?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SIX Börsengebühr  CHF -8.80
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.* B.rsengeb.hr[\\s]{1,}(?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühren   USD -0,23
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^B.rsengeb.hren[\\s]+(?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,'\\d]+)[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Broker Kommission  DKK 12.12
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Broker Kommission[\\s]{1,}(?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void addAustrianBranchContext(Transaction<DocumentContext> documentContext)
    {
        documentContext //

                        // @formatter:off
                        // Zweigniederlassung Österreich
                        // @formatter:on
                        .section().optional() //
                        .match("^Zweigniederlassung .sterreich[\\s]*$") //
                        .assign((ctx, v) -> ctx.putBoolean(IS_AUSTRIAN_BRANCH, true));
    }

    private Locale numberLocale(DocumentType type)
    {
        return type.getCurrentContext().getBoolean(IS_AUSTRIAN_BRANCH) ? Locale.GERMANY : Locale.of("de", "CH");
    }

    private long asShares(String value, DocumentType type)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, numberLocale(type));
    }

    private BigDecimal asExchangeRate(String value, DocumentType type)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, numberLocale(type));
    }

    private BigDecimal asBalance(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Amount, ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, ExtractorUtils.guessNumberLocale(value, Locale.of("de", "CH")));
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

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
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
public class ScalableCapitalPDFExtractor extends AbstractPDFExtractor
{
    public ScalableCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scalable Capital GmbH");
        addBankIdentifier("Scalable Capital Bank GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
        addInterestTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scalable Capital Bank GmbH";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(Wertpapierabrechnung|Contract note|Transactiebevestiging|Nota contrattuale )");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(f.r|voor|per) " //
                        + "(Kundenauftrag" //
                        + "|Sparplanausf.hrung" //
                        + "|savings plan order" //
                        + "|client order" //
                        + "|Auftrag" //
                        + "|beleggingsplanorder" //
                        + "|l.ordine del piano di accumulo).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Buy|Kopen|Acquisto|Verkauf|Sell)) .* [\\.,\\d]+ (Stk|pc)\\..*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Sell".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Acquisto SPDR MSCI All Country World Investable Market (Acc) 1.071122 unità 233.40 EUR 250.00 EUR
                                        // IE00B3YLTY66
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^Acquisto (?<name>.*) [\\.,\\d]+ unit. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kopen IncomeShares S&P 500 Options 0.935016 stk. 5.3475 EUR 5.00 EUR
                                        // XS2875106242
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^Kopen (?<name>.*) [\\.,\\d]+ stk\\. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                                        // IE0008T6IUX0
                                        //
                                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                                        // LU2903252349
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(Kauf|Verkauf) (?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Buy Amundi Stoxx Europe 600 (Acc) 22.650225 pc. 919.45 EUR 2,788.00 EUR
                                        // LU0908500753
                                        //
                                        // Sell ASML Holding 19.00 pc. 786.90 EUR 14,951.10 EUR
                                        // NL0010273215
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(Buy|Sell) (?<name>.*) [\\.,\\d]+ pc\\. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Acquisto SPDR MSCI All Country World Investable Market (Acc) 1.071122 unità 233.40 EUR 250.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Acquisto .* (?<shares>[\\.,\\d]+) unit. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Kopen IncomeShares S&P 500 Options 0.935016 stk. 5.3475 EUR 5.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Kopen .* (?<shares>[\\.,\\d]+) stk\\. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Kauf|Verkauf) .* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Buy Amundi Stoxx Europe 600 (Acc) 22.650225 pc. 919.45 EUR 2,788.00 EUR
                                        // Sell ASML Holding 19.00 pc. 786.90 EUR 14,951.10 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Buy|Sell) .* (?<shares>[\\.,\\d]+) pc\\. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))))

                        // @formatter:off
                        // Ausführung 12.12.2024 13:12:51 Geschäft 36581526
                        // Execution 05.05.2025 12:25:16 Exchange ID 86828W28lTD45195
                        // Uitvoering 07.05.2025 11:28:30 Exchange-ID 347993001
                        // Esecuzione 25.08.2025 11:07:44 ID di scambio 0782210
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(Ausf.hrung|Execution|Uitvoering|Esecuzione) (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total 19,49 EUR
                        // Gutschrift 472,00 EUR
                        // Belastung 200,00 EUR
                        // Debit 85.65 EUR
                        // Credit 14,951.10 EUR
                        // Totaal 5.00 EUR
                        // Addebito 250.00 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Total|Gutschrift|Belastung|Debit|Credit|Totaal|Addebito) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Tipo LIMIT ID ordine tRFkKlxFUnG7LmN
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Tipo .* ID ordine (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("ID ordine: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // Type LIMIT Order-ID SCALcbFSrNzHRn3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Type .* Order\\-ID (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Order-ID: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // Typ LIMIT Order SCALsin78vS5CYz
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Typ .* Order (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // Type LIMIT Order ID BZzUZDlXBK5mNIc
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Type .* Order ID (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Order ID: " + trim(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("(Dividende|Zinszahlung|Dividend)", //
                        "(Kauf|Buy|Kopen|Acquisto|Verkauf|Sell)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*(Seite|Pagina) 1 \\/ [\\d]$");
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
                                        // Effect met recht VanEck Mstr.DM Dividend.UC.ETF
                                        // ISIN NL0011683594
                                        // 03.06.2025 11.06.2025 Krediet 0.90 EUR 2.769834 2.49 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Effect met recht (?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Krediet [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Berechtigtes Wertpapier AGNC Investment Corp.
                                        // ISIN US00123Q1040
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Berechtigtes Wertpapier (?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Rechthebbende hoeveelheid 2.769834
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Rechthebbende hoeveelheid (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Berechtigte Anzahl 0,663129
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Berechtigte Anzahl (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 03.06.2025 11.06.2025 Krediet 0.90 EUR 2.769834 2.49 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) Krediet [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [\\.,\\d]+ [A-Z]{3}.*$")
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) Gutschrift [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [\\.,\\d]+ [A-Z]{3}.*$")
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag 0,07 EUR
                                        // Totaal 2.12 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(Gesamtbetrag|Totaal) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // USD / EUR 1,0269
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ (?<gross>[\\.,\\d]+) [A-Z]{3}.*$") //
                                                        .match("^(?<termCurrency>[A-Z]{3}) \\/ (?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        final var type = new DocumentType("Rechnungsabschluss");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Deutschland Seite 1.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gesamt 13,69 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gesamt (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kontostand am 31.03.2025 726,58 EUR (Soll- & Habenzinsen berücksichtigt)
                        // @formatter:on
                        .section("date") //
                        .match("^Kontostand am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zeitraum 01.01.2025 - 31.03.2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zeitraum (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("(Verrechnungskonto|Cash Account Statement)");
        this.addDocumentTyp(type);

        // @formatter:off
        // 07.04.2025 07.04.2025 Überweisung +29.715,63 EUR
        // 14.08.2025 15.08.2025 Lastschrift +558,52 EUR
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(.berweisung" //
                        + "|Lastschrift" //
                        + "|Direct debit) " //
                        + "\\+[\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>(.berweisung" //
                                        + "|Lastschrift"
                                        + "|Direct debit)) " //
                                        + "\\+(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 03.04.2025 04.04.2025 Prime-Abonnementgebühr -4,99 EUR
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Prime\\-Abonnementgeb.hr) \\-[\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>(Prime\\-Abonnementgeb.hr)) \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01.07.2025 30.06.2025 Abgezogener oder erstatteter Solidaritätszuschlag auf Kundenebene -0,77 EUR
        // 01.07.2025 30.06.2025 Auf Kundenebene einbehaltene oder erstattete Kapitalertragssteuer -14,07 EUR
        // @formatter:on
        var taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (.*oder erstattete.*) [\\-|\\+][\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + ".*" //
                                        + "(?<note>(Solidarit.tszuschlag|Kapitalertrag(s)?steuer)).*" //
                                        + "(?<type>[\\-|\\+])(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                         // Is type --> "-" change from TAXES to TAX_REFUND
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01.07.2025 30.06.2025 Erhaltene Zinsen +56,27 EUR
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Erhaltene Zinsen) [\\-|\\+][\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "type", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*" //
                                        + "(?<type>[\\-|\\+])(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                         v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionAlternativeDocumentRequired);

                         // Is type --> "-" change from INTEREST to INTEREST_CHARGE
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 2,48 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 0,14 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 0,19 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Finanztransaktionssteuer +2,40 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Finanztransaktionssteuer \\+(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Belastingen op financiële transacties 0.00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Belastingen op financi.le transacties \\+(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Ausländische Quellensteuer -0,01 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Ausl.ndische Quellensteuer (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Buitenlandse bronbelasting -0.37 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Buitenlandse bronbelasting (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Verrechnung anrechenbarer Quellensteuer -0,12 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Verrechnung anrechenbarer Quellensteuer (\\-)?(?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // CA25039N4084
                        // +0,99
                        // Ordergebühren
                        // EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .find("[A-Z]{2}[A-Z0-9]{9}[0-9]") //
                        .match("^[\\-|\\+](?<fee>[\\.,\\d]+)$") //
                        .match("^Ordergeb.hren$") //
                        .match("^(?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Ordergebühren +0,99 EUR
                        // Ordergebühren -0,99 EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Ordergeb.hren [\\-|\\+](?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Order fees +0.99 EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Order fees [\\-|\\+](?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        var language = "de";
        var country = "DE";

        var lastDot = value.lastIndexOf(".");
        var lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        var language = "de";
        var country = "DE";

        var lastDot = value.lastIndexOf(".");
        var lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }
}

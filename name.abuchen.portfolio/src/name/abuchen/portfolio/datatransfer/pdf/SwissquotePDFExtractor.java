package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SwissquotePDFExtractor extends AbstractPDFExtractor
{
    public SwissquotePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Swissquote Bank AG");
        addBankIdentifier("Swissquote Bank Ltd");

        addBuySellTransaction();
        addBuySellCryptoTransaction();
        addDividendsTransaction();
        addPaymentTransaction();
        addInterestTransaction();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Swissquote Bank AG / Yuh (powerd by Swissquote Bank AG)";
    }

    record Data(String currency) {
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("((B.rsen|Derivate)transaktion: |Stock-Exchange Transaction: )?(Kauf|Verkauf|Sell|Buy)", //
                        "Anzahl W.hrung Rate");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^((B.rsen|Derivate)transaktion: |Stock-Exchange Transaction: )?(Kauf|Verkauf|Sell|Buy).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^((B.rsen|Derivate)transaktion: |Stock-Exchange Transaction: )?(?<type>(Kauf|Verkauf|Sell|Buy)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || ("Sell".equals(v.get("type"))))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // APPLE ORD ISIN: US0378331005 NASDAQ New York
                                        // 15 193 USD 2'895.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d]+ [\\.'\\d]+ (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Bezeichnung Anzahl Kontraktwährung Preis
                                        // SPY JUL24 527C 1.00 USD 11.6
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency") //
                                                        .find("Bezeichnung Anzahl Kontraktw.hrung Preis")
                                                        .match("^(?<name>.*) [\\.'\\d]+ (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 15 193 USD 2'895.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.'\\d]+) [\\.'\\d]+ [A-Z]{3} [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Bezeichnung Anzahl Kontraktwährung Preis
                                        // SPY JUL24 530C 5.00 USD 9.52
                                        // Kontraktgrösse Multiplikator Ausübungspreis Fälligkeit Börsenplatz
                                        // 100 100 527 31.07.2024 International Securities
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("multiplicator", "shares") //
                                                        .match("^.* (?<shares>[\\.'\\d]+) [A-Z]{3} [\\.'\\d]+$") //
                                                        .match("^[\\.'\\d]+ (?<multiplicator>[\\.'\\d]+) [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                                                        .assign((t, v) -> {
                                                            var shares = new BigDecimal(v.get("shares"));
                                                            var multiplicator = new BigDecimal(v.get("multiplicator"));

                                                            t.setShares(shares.multiply(multiplicator, Values.MC)
                                                                            .setScale(Values.Share.precision(),
                                                                                            RoundingMode.HALF_UP)
                                                                            .movePointRight(Values.Share.precision())
                                                                            .longValue());
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Gemäss Ihrem Kaufauftrag vom 05.08.2019 haben wir folgende Transaktionen vorgenommen:
                                        // Gemäss Ihrem Verkaufsauftrag vom 05.02.2018 haben wir folgende Transaktionen vorgenommen:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Gem.ss Ihrem (Kauf|Verkaufs)auftrag vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // In accordance with your buy order of 06.02.2025, we have carried out the following transactions:
                                        // In accordance with your sell order of 21.10.2025, we have carried out the following transactions:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^In accordance with your (buy|sell) order of (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Am 15.11.2024 haben wir folgende Transaktionen vorgenommen:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) haben wir folgende Transaktionen vorgenommen:$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Ihren Lasten USD 2'900.60
                                        // Zu Ihren Gunsten CHF 8'198.70
                                        // Total gutgeschrieben USD 1'159.55
                                        // Total belastet USD 3'144.75
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(Zu Ihren (Lasten|Gunsten)|Total (gutgeschrieben|belastet)) (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // To your debit CHF 390.45
                                        // To your credit CHF 548.20
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^To your (debit|credit) (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Total DKK 35'410.5
                        // Wechselkurs 14.9827
                        // CHF 5'305.45
                        // @formatter:on
                        .section("fxCurrency", "fxGross", "exchangeRate", "currency", "gross").optional() //
                        .match("^Total (?<fxCurrency>[A-Z]{3}) (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Wechselkurs (?<exchangeRate>[\\.'\\d]+)$") //
                        .match("^(?<currency>[A-Z]{3}) (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            var exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            if (t.getPortfolioTransaction().getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                            {
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }
                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            var fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            // @formatter:off
                            // Swissquote sometimes uses scaled exchanges rates (such as DKK/CHF 15.42),
                            // instead of 0.1542, hence we try to extract and if we fail,
                            // we calculate the exchange rate
                            // @formatter:on
                            if (fxGross.getCurrencyCode().equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                try
                                {
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, exchangeRate));
                                }
                                catch (IllegalArgumentException e)
                                {
                                    exchangeRate = BigDecimal.valueOf(((double) gross.getAmount()) / fxGross.getAmount());
                                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, exchangeRate));
                                }
                            }
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Börsentransaktion: Kauf Unsere Referenz: 32484929
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* (?<note>Referenz: .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Stock-Exchange Transaction: Buy Our reference: 123456789
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* Our reference:(?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellCryptoTransaction()
    {
        final var type = new DocumentType("(B.rsentransaktion: )?(Kauf|Verkauf)", //
                        "Anzahl Preis Betrag");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(B.rsentransaktion: )?(Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Anzahl Währung Rate
                        // 0.02 BTC 63'643.9
                        // Bruttobetrag USD  1'272.88
                        // @formatter:on
                        .section("name", "currency") //
                        .find("Anzahl W.hrung Rate")
                        .match("^[\\.'\\d]+ (?<name>[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?) [\\.'\\d]+$") //
                        .match("^Bruttobetrag (?<currency>[A-Z]{3})[\\s]{1,}[\\.'\\d]+$") //
                        .assign((t, v) -> {
                            v.put("tickerSymbol", v.get("name"));

                            t.setSecurity(getOrCreateCryptoCurrency(v));
                        })

                        // @formatter:off
                        // Anzahl Währung Rate
                        // 0.02 BTC 63'643.9
                        // @formatter:on
                        .section("shares") //
                        .find("Anzahl W.hrung Rate")
                        .match("^(?<shares>[\\.'\\d]+) [A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})? [\\.'\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Am 19.03.2024 haben wir folgende Transaktionen auf SQX vorgenommen:
                        // @formatter:on
                        .section("date") //
                        .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) haben wir folgende Transaktionen.*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Total USD 1'285.61
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kauf Unsere Referenz: 535993271
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendsTransaction()
    {
        final var type = new DocumentType("(Dividende|Kapitalgewinn)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Dividende|Kapitalgewinn) Unsere Referenz:.*$");
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
                                        // HARVEST CAPITAL CREDIT ORD ISIN: US41753F1093NKN: 350
                                        // Dividende 0.08 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^(Dividende|Kapitalgewinn) [\\.'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // SIMPLIFY BITCOIN STGY INC ETF ISIN: US82889N6739
                                        // Dividende pro Aktie USD 0.1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^Dividende pro Aktie (?<currency>[A-Z]{3}) [\\.'\\d]+.*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Anzahl 350
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl (?<shares>[\\.'\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valutadatum 27.06.2019
                        // @formatter:on
                        .section("date") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Total USD 23.88 CHF 21.48
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Total (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+) [A-Z]{3} [\\.'\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Total USD 19.60
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Total (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Betrag SEK 656.25
                                        // Wechselkurs 0.08462
                                        // Total CHF 38.87
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^Betrag (?<baseCurrency>[A-Z]{3}) (?<fxGross>[\\.'\\d]+)$") //
                                                        .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .match("^Total (?<termCurrency>[A-Z]{3}) [\\.'\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getTermCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Betrag USD 28.10 CHF 25.28
                                        // Wechselkurs USD / CHF : 0.89959
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^Betrag [A-Z]{3} (?<gross>[\\.'\\d]+) [A-Z]{3} [\\.'\\d]+.*$") //
                                                        .match("^Wechselkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) : (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Betrag EUR 134.40
                                        // Wechselkurs EUR / CHF : 0.94515
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^Betrag [A-Z]{3} (?<gross>[\\.'\\d]+)$") //
                                                        .match("^Wechselkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) : (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Dividende Unsere Referenz: 32484929
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addPaymentTransaction()
    {
        final var type = new DocumentType("(Zahlungsverkehr|Depotgeb.hren Unsere Referenz:)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Zahlungsverkehr \\- (Gutschrift|Belastung)|Depotgeb.hren Unsere Referenz:) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Belastung" change from DEPOSIT to REMOVAL
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Zahlungsverkehr \\- (?<type>(Gutschrift|Belastung)) .*$") //
                        .assign((t, v) -> {
                            if ("Belastung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);
                        })

                        // @formatter:off
                        // Is type --> "Depotgebühren" change from DEPOSIT to FEES
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Depotgeb.hren) Unsere Referenz: .*$") //
                        .assign((t, v) -> {
                            if ("Depotgebühren".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES);
                        })

                        // @formatter:off
                        // Valutadatum 27.10.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Total EUR 1'000.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Total (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Betrag belastet CHF 28.55
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Betrag belastet (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Zahlungsverkehr - Gutschrift Unsere Referenz: 312345678
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Depotgebühren Unsere Referenz: 32484929
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Depotgeb.hren) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addInterestTransaction()
    {
        final var type = new DocumentType("Zinsabrechnung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^IBAN : .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 30.12.2022 1.36 0.00 1.36 0.00 1.36
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // IBAN : CH3308781000123456700 - Währung : CHF
                        // Total 1.36 0.00 1.36 0.00 1.36
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^IBAN : .* W.hrung : (?<currency>[A-Z]{3})$") //
                        .match("^Total .* (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Zinsabrechnung vom 05.09.2022 bis zum 31.12.2022
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>Zinsabrechnung) vom (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<note3>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            t.setNote(v.get("note1") + " " + v.get("note2") + " - " + v.get("note3"));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        var baseCurrencyRange = new Block("^KONTOAUSZUG in (?<baseCurrency>[A-Z]{3})$") //
                        .asRange(section -> section //
                                        .attributes("baseCurrency") //
                                        .match("^KONTOAUSZUG in (?<baseCurrency>[A-Z]{3})$"));

        final var type = new DocumentType("KONTOAUSZUG", baseCurrencyRange);
        this.addDocumentTyp(type);

        // @formatter:off
        // 31.03.2023 Depotgebühren 20.00 31.03.2023 -20.00
        // 29.09.2023 Depotgebühren 20.00 29.09.2023 -5'791.30
        // @formatter:on
        var feeBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Depotgeb.hren [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.'\\d]+$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date") //
                        .documentRange("baseCurrency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<note>Depotgeb.hren) " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(\\-)?[\\.'\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("baseCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // 29.12.2023 Sollzinsen 127.85 31.12.2023 -7'589.89
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Sollzinsen [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?[\\.'\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date") //
                        .documentRange("baseCurrency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<note>Sollzinsen) " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(\\-)?[\\.'\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("baseCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private void addNonImportableTransaction()
    {
        final var type = new DocumentType("((B.rsen|Derivate)transaktion: )?Verfall", //
                        "Anzahl W.hrung Rate");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^((B.rsen|Derivate)transaktion: )?Verfall.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Bezeichnung Anzahl Kontraktwährung Preis
                        // QQQ APR24 447C 1.00 USD -
                        // @formatter:on
                        .section("name", "currency") //
                        .find("Bezeichnung Anzahl Kontraktw.hrung Preis")
                        .match("^(?<name>.*) [\\.'\\d]+ (?<currency>[A-Z]{3}) \\-$") //
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setAmount(0L);
                        })

                        // @formatter:off
                        // Bezeichnung Anzahl Kontraktwährung Preis
                        // QQQ APR24 447C 1.00 USD -
                        // Kontraktgrösse Multiplikator Ausübungspreis Fälligkeit Börsenplatz
                        // 100 100 447 05.04.2024 International Securities
                        // @formatter:on
                        .section("multiplicator", "shares") //
                        .match("^.* (?<shares>[\\.'\\d]+) [A-Z]{3} \\-$") //
                        .match("^[\\.'\\d]+ (?<multiplicator>[\\.'\\d]+) [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> {
                            var shares = new BigDecimal(v.get("shares"));
                            var multiplicator = new BigDecimal(v.get("multiplicator"));

                            t.setShares(shares.multiply(multiplicator, Values.MC)
                                            .setScale(Values.Share.precision(),
                                                            RoundingMode.HALF_UP)
                                            .movePointRight(Values.Share.precision())
                                            .longValue());
                        })

                        // @formatter:off
                        // Am 08.04.2024 haben wir folgende Transaktionen vorgenommen:
                        // @formatter:on
                        .section("date") //
                        .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Derivatetransaktion: Verfall Unsere Referenz: 549183576
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Abgabe (Eidg. Stempelsteuer) USD 4.75
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Abgabe \\(Eidg\\. Stempelsteuer\\) (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 15.00% (US) USD 4.20
                        // Quellensteuer 15.00% (US) USD 4.22 CHF 3.80
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Quellensteuer [\\.'\\d]+% \\(.*\\)( [A-Z]{3} [\\.'\\d]+)? (?<currency>[A-Z]{3}) (?<withHoldingTax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Zusätzlicher Steuerrückbehalt 15% USD 4.20
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Zus.tzlicher Steuerr.ckbehalt [\\.'\\d]+% (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Verrechnungssteuer 35% (CH) CHF 63.88
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer [\\.'\\d]+% \\(.*\\) (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Tax (Federal stamp duty) CHF 0.60
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Tax \\(Federal stamp duty\\) (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kommission Swissquote Bank AG USD 0.85
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kommission Swissquote Bank AG (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühren CHF 1.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hren (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kommission CHF 1.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kommission (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Stock exchange fee CHF 2.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Stock exchange fee (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+).*$") //
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

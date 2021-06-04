package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ErsteBankPDFExtractor extends AbstractPDFExtractor
{
    public ErsteBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BROKERJET"); //$NON-NLS-1$
        addBankIdentifier("Brokerjet Bank AG"); //$NON-NLS-1$
        addBankIdentifier("ERSTE BANK GRUPPE"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Erste Bank Gruppe / BrokerJet"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(IHR (KAUF|VERKAUF)|KAUF|VERKAUF).*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("(IHR (KAUF|VERKAUF)|KAUF|VERKAUF).*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match(".*(?<type>VERKAUF)")
                .assign((t, v) -> {
                    if (v.get("type").equals("VERKAUF"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Wertpapier: MORGAN ST., DEAN W. FX-Kommission: USD 3,75-
                // DL-01 WP-Kommission: USD 22,67-
                // WP-Kenn-Nr. : US6174464486
                .section("name", "name1", "isin").optional()
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .match("(?<name1>.*) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("WP-Kommission"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                // O.N.
                // Gesamtbetrag: EUR 3.217,22
                // WP-Kenn-Nr.: AT0000937503
                .section("name", "name1", "isin").optional()
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .match("(?<name1>.*)$")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Gesamtbetrag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier: EVN STAMMAKTIEN O.N. WP-Kommission: EUR 9,99
                // WP-Kenn-Nr. : AT0000741053
                .section("name", "isin").optional()
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD 5,24
                // WP-Kenn-Nr. : US1491231015 WP-Kommission: USD 25,62
                .section("name", "isin").optional()
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12}) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier: TELEKOM AUSTRIA AKT.
                // O.N.
                // WP-Kenn-Nr. : AT0000720008 Gesamtbetrag: EUR 1.049,40
                .section("name", "name1", "isin").optional()
                .match("^Wertpapier: (?<name>.*)$")
                .match("(?<name1>.*)$")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12}) (Tradinggeb.hren|WP-Kommission|Gesamtbetrag|Devisenprovision|FX-Kommission): [\\w]{3} [.,\\d]+[-]?$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("WP-Kenn-Nr.*"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier: DWS TOP 50 WELT
                // WP-Kenn-Nr. : DE0009769794
                .section("name", "isin").optional()
                .match("^Wertpapier: (?<name>.*)$")
                .match("^WP-Kenn-Nr.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück: 105,00
                // Stück: 90 Gesamtbetrag: EUR 1.142,19
                .section("shares")
                .match("^St.ck: (?<shares>[.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Ausführungsdatum: 23.09.2015 Ausführungszeit: 09:02:20
                .section("date", "time").optional()
                .match("^Ausführungsdatum: (?<date>\\d+.\\d+.\\d{4}) Ausführungszeit: (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausführungsdatum: 05. Oktober 2009 Ausführungszeit: 12:39:27
                .section("date", "time").optional()
                .match("^Ausführungsdatum: (?<date>\\d+\\. .* \\d{4}) Ausführungszeit: (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    // Formate the date from 05. Oktober 2009 to 05.10.2009
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date"), DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))));
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Gesamtbetrag: EUR 3.217,22
                .section("currency", "amount").optional()
                .match("^(.*)?Gesamtbetrag: (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Börse: New York Stock Exchange Kurswert: USD 2.069,90
                // Valutatag: 08. Dezember 2009 Devisenkurs: 0,6654265
                .section("currency", "amount", "fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^.* Kurswert: (?<fxcurrency>[\\w]{3}) (?<fxamount>[.,\\d]+)[-]?$")
                .match("^(.*)?Devisenkurs: (?<exchangeRate>[.,\\d]+)$")
                .match("^(.*)?Gesamtbetrag[\\s+]?: (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                    
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxcurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxamount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // Valutatag: 08. Dezember 2009 Devisenkurs: 0,6654265
                .section("exchangeRate").optional()
                .match("^(.*)?Devisenkurs: (?<exchangeRate>[.,\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("BARDIVIDENDE|FONDS-AUSSCH.TTUNG|FONDS - AUSSCH.TTUNG");
        this.addDocumentTyp(type);

        Block block = new Block("(BARDIVIDENDE|FONDS-AUSSCH.TTUNG|FONDS - AUSSCH.TTUNG)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // ISIN : CA0679011084
                // Wertpapierbezeichnung : BARRICK GOLD CORP.
                .section("isin", "name").optional()
                .match("^ISIN : (?<isin>[\\w]{12})$")
                .match("^Wertpapierbezeichnung : (?<name>.*)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier : MUENCH.RUECKVERS.VNA O.N. Dividende Brutto : EUR 201,25
                // Wertpapier : DWS TOP 50 WELT Ausschüttung Brutto : EUR 5,64
                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                .section("isin", "name").optional()
                .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : [\\w]{3} [.,\\d]+$")
                .match("^WP-Kenn-Nr.* : (?<isin>[\\w]{12}) (Fremde Steuer|KESt) : [\\w]{3} [.,\\d]+$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapier : RAIFF.ETHIK-AKTIEN (R) Ausschüttung Brutto : EUR 19,07
                // WP-Kenn-Nr. : AT0000677901
                .section("isin", "name").optional()
                .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : [\\w]{3} [.,\\d]+$")
                .match("^WP-Kenn-Nr.* : (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Anspruchsberechtigter : 35
                // Anspruchsberechtigter : 77.638
                .section("shares").optional()
                .match("^Anspruchsberechtigter : (?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setShares(asShares(convertAmount(v.get("shares"))));
                })

                // WP-Bestand : 35,000 Dividendenbetrag : EUR 148,17
                // WP-Bestand : 20,286
                .section("shares").optional()
                .match("^WP-Bestand : (?<shares>[.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex-Tag : 27.08.2015
                .section("date").optional()
                .match("^Ex-Tag : (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                // Ex-Tag : 15. Juni 2011
                .section("date").optional()
                .match("^Ex-Tag : (?<date>\\d+\\. .* \\d{4})(.*)?")
                .assign((t, v) -> {
                    // Formate the date from 29. April 2010 to 29.05.2010
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date"), DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Gesamtbetrag (in : EUR 0.4
                .section("currency", "amount").optional()
                .match("^Gesamtbetrag .in : (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })

                // Dividende Netto : EUR 127,54
                .section("currency", "amount").optional()
                .match("^(.*)?Dividende Netto : (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })

                // Auszahlungsbetrag : EUR 3,84
                .section("currency", "amount").optional()
                .match("^(.*)?Auszahlungsbetrag : (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })

                // Brutto-Betrag : USD 0.7
                // Devisenkurs : 0.888889
                // Gesamtbetrag (in : EUR 0.4
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Brutto-Betrag : (?<fxCurrency>[\\w]{3}) (?<fxAmount>['.,\\d]+)$")
                .match("^Devisenkurs : (?<exchangeRate>[.,\\d]+)$")
                .match("^Gesamtbetrag .in : (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(convertExchangeRate(v.get("exchangeRate")));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(convertAmount(v.get("fxAmount"))));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(convertAmount(v.get("amount"))));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), 
                                            asAmount(convertAmount(v.get("fxAmount"))));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), 
                                            asAmount(convertAmount(v.get("amount"))));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                // Wertpapier : MORGAN ST., DEAN W. DL-01 Dividende Brutto : USD 3,00
                // Verwahrart : WR Devisenkurs : 1,2859000
                // Dividende Netto : EUR 1,73
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^.* Dividende Brutto : (?<fxCurrency>[\\w]{3}) (?<fxAmount>['.,\\d]+)$")
                .match("^.* Devisenkurs : (?<exchangeRate>[.,\\d]+)$")
                .match("^Dividende Netto : (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));

                    BigDecimal exchangeRate = asExchangeRate(convertExchangeRate(v.get("exchangeRate")));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(convertAmount(v.get("fxAmount"))));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(convertAmount(v.get("amount"))));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), 
                                            asAmount(convertAmount(v.get("fxAmount"))));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), 
                                            asAmount(convertAmount(v.get("amount"))));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Steuer : Quellensteuer
                // Steuern : USD 0.18
                .section("currency", "tax").optional()
                .match("^Steuer : Quellensteuer")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                })

                // Steuer : KESt1
                // Steuern : USD 0.18
                .section("currency", "tax").optional()
                .match("^Steuer : KESt1")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                })

                // Steuer : KESt2
                // Steuern : EUR 1.97
                .section("currency", "tax").optional()
                .match("^Steuer : KESt2")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                })

                // Steuer : KESt3
                // Steuern : EUR 0.00
                .section("currency", "tax").optional()
                .match("^Steuer : KESt3")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                })

                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                .section("currency", "tax").optional()
                .match(".* Fremde Steuer : (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Dividende : EUR 5,750000 KESt : EUR 20,13
                .section("currency", "tax").optional()
                .match(".* KESt : (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ausschüttung : EUR 0,160000 KESt II : EUR 1,41
                .section("currency", "tax").optional()
                .match(".* KESt II : (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt II : EUR 1,22
                .section("currency", "tax").optional()
                .match("KESt II : (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt III : EUR 0,00
                .section("currency", "tax").optional()
                .match("KESt III : (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                .section("currency", "fee").optional()
                .match(".* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* Tradinggeb.hren: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD 5,24
                .section("currency", "fee").optional()
                .match(".* FX-Kommission: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: INVESCO ASIAN INF. A Devisenprovision: USD 0,44
                .section("currency", "fee").optional()
                .match(".* Devisenprovision: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private String convertAmount(String inputAmount)
    {
        String amount = inputAmount.replace("'", "");
        return amount.replace(".", ",");
    }

    private String convertExchangeRate(String inputExchangeRate)
    {
        // 0.888889    --> 0,888889
        return inputExchangeRate.replace(".", ",");
    }
}

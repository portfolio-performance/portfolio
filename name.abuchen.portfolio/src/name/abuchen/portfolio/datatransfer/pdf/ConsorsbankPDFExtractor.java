package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class ConsorsbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isjointaccount"; //$NON-NLS-1$

    BiConsumer<Map<String, String>, String[]> isJointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("^(abzgl. Kapitalertragssteuer|KAPST) anteilig 50,00.*$"); //$NON-NLS-1$
        Boolean bJointAccount = false;
        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(IS_JOINT_ACCOUNT, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(IS_JOINT_ACCOUNT, Boolean.FALSE.toString());
    };

    public ConsorsbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Consorsbank"); //$NON-NLS-1$
        addBankIdentifier("POSTFACH 17 43"); //$NON-NLS-1$
        addBankIdentifier("Cortal Consors"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addEncashmentTransaction();
        addAdvanceTaxTransaction();
        addTaxAdjustmentTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("KAUF|Kauf|BEZUG|Bezug|VERKAUF|Verkauf|VERK. TEIL-/BEZUGSR.", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^([\\s]+)?(KAUF|Kauf|BEZUG|Bezug|VERKAUF|Verkauf|VERK. TEIL-\\/BEZUGSR.) ([\\s]+)?AM .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "VERKAUF" change from BUY to SELL
                .section("type").optional()
                .match("^([\\s]+)?(?<type>VERKAUF|Verkauf|VERK. TEIL-\\/BEZUGSR.) ([\\s]+)?AM .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("VERKAUF") 
                        || v.get("type").equals("Verkauf")
                        || v.get("type").equals("VERK. TEIL-/BEZUGSR."))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // COMS.-MSCI WORL.T.U.ETF I ETF110 LU0392494562
                // Kurs 37,650000 EUR P.ST. NETTO  
                // Preis pro Anteil 25,640000 EUR  
                .section("name", "wkn", "isin", "currency").optional()
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^(Kurs|Preis pro Anteil) [\\.,\\d]+ (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // ST 15,75243 WKN: 625952
                // GARTMORE - CONT. EUROP. FUND
                // ACTIONS NOM. A O.N.
                // KURS 4,877300 P.ST. NETTO
                // KURSWERT EUR 76,83
                .section("wkn", "name", "nameContinued", "currency").optional()
                .match("^ST [\\.,\\d]+ WKN:(?<wkn>.*)$")
                .match("^(?<name>.*)$")
                .match("^(?<nameContinued>.*)")
                .match("^KURS [\\.,\\d]+ .*$")
                .match("^KURSWERT (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // ST 334,00000 WKN: A0MZBE
                // AHOLD, KON. EO-,30
                // Kurs 9,890000 EUR P.ST. FRANCO COURTAGE
                // Kurswert EUR 3.303,26
                .section("wkn", "name", "currency").optional()
                .match("^ST [\\.,\\d]+ WKN:(?<wkn>.*)$")
                .match("^(?<name>.*)$")
                .match("^Kurs [\\.,\\d]+ .*$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                //       ST                        50,00000               WKN: 851144
                //                 GENERAL ELECTRIC CO.
                //                 SHARES DL -,06
                //       KURSWERT                                      EUR               1.917,50
                .section("wkn", "name", "nameContinued", "currency").optional()
                .match("^[\\s]+ ST [\\s]+[\\.,\\d]+ [\\s]+WKN:(?<wkn>.*)$")
                .match("^[\\s]+(?<name>.*)$")
                .match("^[\\s]+(?<nameContinued>.*)")
                .match("^[\\s]+KURSWERT [\\s]+(?<currency>[\\w]{3}) [\\s]+[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // ST 11,87891 WKN: 625952
                // GARTMORE-CONT. EUROP. A
                // Preis pro Anteil 6,467700 EUR
                .section("wkn", "name", "currency").optional()
                .match("^ST [\\.,\\d]+ WKN:(?<wkn>.*)$")
                .match("^(?<name>.*)$")
                .match("^Preis pro Anteil [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // ST 132,80212
                // ST 15,75243 WKN: 625952
                // ST 1.000,00000 Letzte Fälligkeit 01.09.2021
                //       ST                        50,00000               WKN: 851144
                .section("shares")
                .match("^([\\s]+)?ST ([\\s]+)?(?<shares>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // KAUF AM 15.01.2015  UM 08:13:35 MUENCHEN NR. 12345670.001
                .section("time").optional()
                .match("^(KAUF|Kauf|BEZUG|Bezug|VERKAUF|Verkauf|VERK. TEIL-/BEZUGSR.) .* UM (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Kauf AM 17.10.2005 IN SPARPLAN NR.2424880.001
                //              KAUF                 AM 18.09.2001 IN FRANKFURT          NR. 6201999.001
                // KAUF AM 15.01.2015  UM 08:13:35 MUENCHEN NR. 12345670.001
                .section("date")
                .match("^([\\s]+)?(KAUF|Kauf|BEZUG|Bezug|VERKAUF|Verkauf|VERK. TEIL-/BEZUGSR.) ([\\s]+)?AM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Wert 19.01.2015 EUR 5.000,00
                // WERT 20.05.2008 EUR 3.290,05
                //       WERT  20.09.2001                              EUR               1.928,74
                .section("currency", "amount").optional()
                .match("^([\\s]+)?(Wert|WERT) ([\\s]+)?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // EUR 75,00
                // WERT 21.10.2005
                //                                                     EUR                 691,31
                //       WERT  24.03.2005
                .section("currency", "amount").optional()
                .match("^([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                .match("^([\\s]+)?WERT ([\\s]+)?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}([\\s]+)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // zulasten Konto-Nr. 0860101888 7.659,37 EUR
                .section("amount", "currency").optional()
                .match("^(zulasten|zugunsten) Konto-Nr\\. [\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // umger. zum Devisenkurs USD 1,077900 EUR 500,97
                //       UMGER. ZUM DEVISENKURS  USD        0,882100   EUR                  56,68
                .section("forex", "exchangeRate", "currency", "amount").optional()
                .match("^([\\s]+)?(umger. zum Devisenkurs|UMGER. ZUM DEVISENKURS) ([\\s]+)?(?<forex>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("forex"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in account currency
                        long amount = asAmount(v.get("amount"));
                        long fxAmount = exchangeRate.multiply(BigDecimal.valueOf(amount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(asCurrencyCode(v.get("currency")), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // Kurswert 343,75 USD
                // Devisenkurs 1,174000 EUR / USD
                .section("fxCurrency", "fxAmount", "exchangeRate").optional()
                .match("^Kurswert (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) [\\w]{3} \\/ [\\w]{3}$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // Limitkurs  5,500000 EUR
                .section("note").optional()
                .match("^(?<note>Limitkurs .*)")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("DIVIDENDENGUTSCHRIFT|Dividendengutschrift|ERTRAGSGUTSCHRIFT|Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(DIVIDENDENGUTSCHRIFT|Dividendengutschrift|ERTRAGSGUTSCHRIFT|Ertragsgutschrift)(.*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // ST                    1.370,00000          WKN:  ETF110                 
                //            COMS.-MSCI WORL.T.U.ETF I                                    
                //            Namens-Aktien o.N.                                           
                .section("shares", "wkn", "name", "nameContinued", "currency").optional()
                .match("^ST ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?WKN: ([\\s]+)?(?<wkn>.*)(.*)?$")
                .match("^(?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ZINS-\\/DIVIDENDENSATZ|ERTRAGSAUSSCHUETTUNG P. ST.) .* ([\\s]+)?(?<currency>[\\w]{3}) SCHLUSSTAG PER [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}(.*)?$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // OMNICOM GROUP INC. Registered Shares DL -,15 871706 US6819191064
                // 25 Stück
                // Dividende pro Stück 0,60 USD Schlusstag 17.12.2017
                .section("name", "wkn", "isin", "shares", "currency").optional()
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^(?<shares>[\\.,\\d]+) St.ck$")
                .match("^(Steuerfreie )?(Dividende pro St.ck|Ertragsaussch.ttung je Anteil) [\\.,\\d]+ (?<currency>[\\w]{3}) Schlusstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // UMGER.ZUM DEV.-KURS                 1,093000  EUR                285,60 
                // WERT 11.01.2016  
                .section("date", "currency", "amount").optional()
                .match("^UMGER.ZUM DEV.-KURS [\\s]+[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) [\\s]+(?<amount>[\\.,\\d]+)+(.*)?$")
                .match("^WERT (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // WERT 08.05.2015                               EUR                326,90 
                .section("date", "currency", "amount").optional()
                .match("^WERT (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Netto zugunsten IBAN DE00 0000 0000 0000 0000 00 9,34 EUR
                // Valuta 09.01.2018 BIC CSDBDE71XXX
                .section("date", "currency", "amount").optional()
                .match("^Netto zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Netto in USD zugunsten IBAN DE12 3456 3456 3456 3456 78 6,46 USD
                // Valuta 01.01.2020 BIC AAAAAA11XXX
                .section("date", "currency", "amount").optional()
                .match("^Netto in [\\w]{3} zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // BRUTTO                                        USD                180,00 
                // UMGER.ZUM DEV.-KURS                 1,104300  EUR                138,55 
                .section("fxAmountGross", "fxCurrency", "exchangeRate").optional()
                .match("^BRUTTO ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxAmountGross>[\\.,\\d]+)(.*)?$")
                .match("^UMGER.ZUM DEV.-KURS ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(.*)?$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    // create gross value unit only, 
                    // if transaction currency is different to security currency
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        // create a Unit only, 
                        // if security and transaction currency are different
                        if (!t.getCurrencyCode().equalsIgnoreCase(asCurrencyCode(v.get("fxCurrency"))))
                        {
                            // get exchange rate (in Fx/EUR) and
                            // calculate inverse exchange rate (in EUR/Fx)
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                            RoundingMode.HALF_DOWN);

                            // get gross amount and calculate equivalent in EUR
                            Money fxAmountGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmountGross")));
                            BigDecimal amount = BigDecimal.valueOf(fxAmountGross.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN);

                            Money fxAmount = Money.of(t.getCurrencyCode(), amount.longValue());
                            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, fxAmount, fxAmountGross,
                                            inverseRate));
                        }
                    }
                })

                // Brutto in USD 15,00 USD
                // Devisenkurs 1,195900 USD / EUR
                // Brutto in EUR 12,54 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Brutto in [\\w]{3} (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) [\\w]{3} \\/ [\\w]{3}$")
                .match("^Brutto in [\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    // Example: Devisenkurs 1,249900 USD / EUR
                    // check which currency is transaction currency and
                    // use exchange rate accordingly
                    // if transaction currency is e.g. USD, we need to
                    // inverse the rate
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
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
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
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

    @SuppressWarnings("nls")
    private void addEncashmentTransaction()
    {
        DocumentType type = new DocumentType("Einl.sung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^Einl.sung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Wertpapierbezeichnung WKN ISIN
                // Lang & Schwarz AG LS846N DE000LS846N5
                .section("wkn", "isin", "name", "name1")
                .find("Wertpapierbezeichnung WKN ISIN")
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Einheit"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 1.000 20.05.2021
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) [\\d]+.[\\d]+.[\\d]{4}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Einlösung zu 0,001 EUR Schlusstag 20.05.2021
                .section("date")
                .match("^Einl.sung .* Schlusstag (?<date>[\\d]+.[\\d]+.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Netto zugunsten IBAN DExxxxxxxxxxxxxxxxxxxx 1,00 EUR
                .section("amount", "currency")
                .match("^Netto zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Vorabpauschale$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAXES);
                    return t;
                })

                // Wertpapierbezeichnung WKN ISIN
                // L&G-L&G R.Gbl Robot.Autom.UETF Bearer Shares (Dt. Zert.) o.N. A12GJD DE000A12GJD2
                .section("wkn", "isin", "name")
                .match("^Wertpapierbezeichnung WKN ISIN$")
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Bestand
                // 106 Stück
                .section("shares")
                .match("^Bestand$")
                .match("^(?<shares>[\\.,\\d]+) St.ck$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Netto zulasten IBAN DE73 7603 0080 0123 4567 89 0,73 EUR
                // Valuta 02.01.2020 BIC CSDBDE71XXX
                .section("currency", "amount", "date")
                .match("^Netto zulasten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    @SuppressWarnings("nls")
    private void addTaxAdjustmentTransaction()
    {

        DocumentType type = new DocumentType("Nachtr.gliche Verlustverrechnung");
        this.addDocumentTyp(type);

        Block block = new Block("^([\\s]+)?Erstattung/Belastung \\(-\\) von Steuern$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);

                    // nirgends im Dokument ist die Währung aufgeführt.
                    t.setCurrencyCode(CurrencyUnit.EUR);
                    return t;
                })

                // Den Steuerausgleich buchen wir mit Wertstellung
                // 10.07.2017
                .section("date").optional()
                .match(" *Den Steuerausgleich buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Erstattung/Belastung (-) von Steuern
                // Anteil                             100,00%
                // KapSt Person 1                                 :                79,89
                // SolZ  Person 1                                 :                 4,36
                // KiSt  Person 1                                 :                 6,36
                // ======================================================================
                //                                                                 90,61
                // @formatter:on

                .section("amount", "sign") //
                .find(" *Erstattung/Belastung \\(-\\) von Steuern *") //
                .find(" *=* *") //
                .match(" *(?<amount>[\\.,\\d]+)(?<sign>-?).*") //
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));

                    if ("-".equals(v.get("sign")))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                .wrap(t -> {
                    if (t.getDateTime() == null)
                    {
                        if (t.getAmount() == 0L)
                            return new NonImportableItem("Erstattung/Belastung von Steuern mit 0 Euro");
                        else
                            throw new IllegalArgumentException(Messages.MsgErrorMissingDate);
                    }
                    else
                    {
                        return new TransactionItem(t);
                    }
                }));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Franzoesische Finanztransaktionssteuer 0,30% EUR 0,07
                .section("tax", "currency").optional()
                .match("^Franzoesische Finanztransaktionssteuer [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // QUST 15,00000  %   EUR                 24,45  USD                 27,00
                .section("tax", "currency").optional()
                .match("^QUST [\\.,\\d]+ ([\\s]+)?% ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+) .*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer in EUR 1,88 EUR
                .section("tax", "currency").optional()
                .match("^Quellensteuer in [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer (Account)
                // KAPST                                 25,00 % EUR                111,00 
                .section("tax", "currency").optional()
                .match("^KAPST ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragsteuer 25,00 % von 5,02 EUR 1,26 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kapitalertragsteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragssteuer 25,00% 97,47 EUR 24,37 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kapitalertragssteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragsteuer 2,06 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kapitalertragsteuer (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer (Account)
                // KAPST 24,45% EUR 198,08
                .section("tax", "currency").optional()
                .match("^KAPST [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalerstragsteuer (Joint Account)
                // KAPST anteilig 50,00% 25,00% EUR 0,50
                // KAPST anteilig 50,00% 25,00% EUR 0,50
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^KAPST anteilig [\\.,\\d]+% [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KAPST anteilig [\\.,\\d]+% [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalerstragsteuer (Joint Account)
                // abzgl. Kapitalertragssteuer anteilig 50,00% 25,00% 208,72 EUR 52,18 EUR
                // abzgl. Kapitalertragssteuer anteilig 50,00% 25,00% 208,72 EUR 52,18 EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl. Kapitalertragssteuer anteilig [\\.,\\d]+% [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl. Kapitalertragssteuer anteilig [\\.,\\d]+% [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Account)
                // SOLZ                                   5,50 % EUR                  6,10 
                .section("tax", "currency").optional()
                .match("^SOLZ ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 5,50 % von 1,26 EUR 0,06 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Solidarit.tszuschlag [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 5,50% 24,37 EUR 1,34 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 0,10 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Solidarit.tszuschlag (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Account)
                // SOLZ 5,50% EUR 10,89
                .section("tax", "currency").optional()
                .match("^SOLZ [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Joint Account)
                // SOLZ 5,50% EUR 0,02
                // SOLZ 5,50% EUR 0,02
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^SOLZ [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^SOLZ [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag (Joint Account)
                // abzgl. Solidaritätszuschlag 5,50% 52,18 EUR 2,86 EUR
                // abzgl. Solidaritätszuschlag 5,50% 52,18 EUR 2,86 EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // KIST                                   5,50 % EUR                  6,10 
                .section("tax", "currency").optional()
                .match("^KIST ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // abzgl. Kirchensteuer 9,00 % von 8,93 EUR 0,80 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kirchensteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // abzgl. Kirchensteuer 5,50% 24,37 EUR 1,34 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // KIST 9,00% EUR 17,82
                .section("tax", "currency").optional()
                .match("^KIST [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Joint Account)
                // KIST 9,00% EUR 1,00
                // KIST 9,00% EUR 1,00
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^KIST [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KIST [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Joint Account)
                // abzgl. Kirchensteuer 9% 52,18 EUR 2,86 EUR
                // abzgl. Kirchensteuer 9% 52,18 EUR 2,86 EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                });
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Börsenplatzgebühr EUR 2,95
                .section("fee", "currency").optional()
                .match("^(abzgl. )?B.rsenplatzgeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsenplatzgebühr 2,50 EUR
                // abzgl. Börsenplatzgebühr 1,50 EUR
                .section("fee", "currency").optional()
                .match("^(abzgl. )?B.rsenplatzgeb.hr (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsentgelt EUR 3,00
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Handelsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision EUR 5,00
                // PROVISION EUR 8,26
                .section("fee", "currency").optional()
                .match("^(abzgl. )?(Provision|PROVISION) (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision 13,54 EUR
                // abzgl. Provision 5,00 EUR
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Provision (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Grundgebühr EUR 4,95
                // GRUNDGEBUEHR EUR 4,95
                .section("fee", "currency").optional()
                .match("^(abzgl. )?(Grundgeb.hr|GRUNDGEBUEHR) (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Grundgebühr 3,95 EUR
                // abzgl. Grundgebühr 4,95 EUR
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Grundgeb.hr (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Consorsbank Ausgabegeb�hr 2,50% EUR 0,61
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Consorsbank Ausgabegeb.hr [\\.,\\d]+% (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt EUR 11,54
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt 5,96 EUR
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Transaktionsentgelt (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                //       COURTAGE                                      EUR                   1,53
                .section("fee", "currency").optional()
                .match("^[\\s]+ COURTAGE [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                //       PROVISION                                     EUR                   5,11
                .section("fee", "currency").optional()
                .match("^[\\s]+ PROVISION [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // BONIFIKAT. 2,38100 % EUR 1,83
                .section("fee", "currency").optional()
                .match("^BONIFIKAT. [\\.,\\d]+ % (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                //       EIG.SPESEN                                    EUR                   4,60
                .section("fee", "currency").optional()
                .match("^[\\s]+ EIG.SPESEN [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // abz. CortalConsors Discount 2,38100 % EUR 1,83
                .section("fee", "currency").optional()
                .match("^abz. CortalConsors Discount [\\.,\\d]+ % (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eig. Spesen EUR 1,95
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Eig. Spesen (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // FREMDE SPESEN                                 USD                  1,91 
                .section("fee", "currency").optional()
                .match("^(abzgl. )?FREMDE SPESEN [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // abzgl. Fremde Spesen 0,07 USD
                .section("fee", "currency").optional()
                .match("^(abzgl. )?Fremde Spesen (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @SuppressWarnings("nls")
    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
    
    @SuppressWarnings("nls")
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
}
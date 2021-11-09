package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.TextUtil;

@SuppressWarnings("nls")
public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    /***
     * Attention:
     * For dividend transactions, 
     * post-processing will be performed once the dividend transaction 
     * and tax treatment are in two separate documents.
     * 
     * @Override
     * public List<Item> postProcessing(List<Item> items)
     */

    public ComdirectPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("comdirect"); //$NON-NLS-1$

        addBuySellTransaction();
        addSellWithNegativeAmountTransaction();
        addDividendeTransaction();
        addTaxTreatmentForDividendeTransaction();
        addAdvanceTaxTransaction();
        addFinancialReport();
        addDepositoryFeeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Comdirect Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(\\*[\\s]+)?(Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug)(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(\\*[\\s]+)?(?<type>(Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug))(.*)?$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Wertpapierverkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }

                    /***
                     * If we have multiple entries in the document,
                     * then the "negative" flag must be removed.
                     */
                    type.getCurrentContext().remove("negative");
                })

                // Wertpapier-Bezeichnung                                               WPKNR/ISIN 
                // BASF                                           BASF11                           
                // Inhaber-Anteile                                                    DE000BASF111 
                // St.  1,000                EUR  1,000                                            
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00 
                .section("name", "wkn", "nameContinued", "isin", "shares", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,})(.*)?$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12})(.*)?$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelszeit       : 09:04 Uhr (MEZ/MESZ)                  (Kommissionsgeschäft) 
                .section("time").optional()
                .match("Handelszeit ([\\s]+)?: ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}) Uhr.*")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Geschäftstag      : 01.01.2000        Ausführungsplatz  : XETRA 
                .section("date")
                .match("^Gesch.ftstag ([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                //        Umrechn. zum Dev. kurs 1,080600 vom 17.04.2020 : EUR            4.425,22 
                .section("exchangeRate").optional()
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                /***
                 * If the type of transaction is "SELL" 
                 * and the amount is negative, 
                 * then the gross amount set.
                 * 
                 * Fees are processed in a separate transaction
                 */
                .section("negative").optional()
                .match("^.* \\d+.\\d+.[\\d]{4} ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(?<negative>[-])(.*)?$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getType().isLiquidation())
                    {
                        type.getCurrentContext().put("negative", "X");
                    }
                })

                // IBAN                                  Valuta        Zu Ihren Gunsten vor Steuern 
                // DE09 9999 9999 9999 9999 00   EUR     01.01.2010        EUR           10.111,11
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // EUR     30.12.2020        EUR            1.430,30 
                .section("amount", "currency").optional()
                .match("^.* Zu Ihren (Lasten|Gunsten)( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {                   
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                //                           Kurswert                    : USD            4.768,00 
                //        Umrechn. zum Dev. kurs 1,080600 vom 17.04.2020 : EUR            4.425,22 
                .section("fxCurrency", "fxAmount", "exchangeRate").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxAmount>[\\.,\\d]+)(.*)?$")
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
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
                    }
                })

                //                           Kurswert                    : EUR                3,54 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,86- 
                .section("amount", "currency", "fxCurrency").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .match("^.* Zu Ihren (Lasten|Gunsten)( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+(.*)?$")
                .assign((t, v) -> {                   
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        String forex = asCurrencyCode(v.get("fxCurrency"));
                        if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                        {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }
                    }
                })

                //  Summe        St.  570                EUR  37,379473    EUR           21.306,30  
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                9,61- 
                .section("amount", "currency", "fxCurrency").optional()
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)(.*)?$")
                .match("^.* Zu Ihren Lasten( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+(.*)?$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        String forex = asCurrencyCode(v.get("fxCurrency"));
                        if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                        {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }
                    }
                })

                //                           Kurswert                    : USD                3,54 
                //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR                2,28 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern                                                    
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR               10,12- 
                .section("amount", "currency", "exchangeRate", "fxCurrency").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+(.*)?$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        // read the forex currency, exchange rate and gross
                        // amount in forex currency
                        String forex = asCurrencyCode(v.get("currency"));
                        if (!t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                        {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                            RoundingMode.HALF_DOWN);

                            // gross given in forex currency
                            long fxAmount = asAmount(v.get("amount"));
                            long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                            .setScale(0, RoundingMode.HALF_DOWN).longValue();

                            // set amount in account currency
                            String amountFX =  Double.toString((double)amount / 100.0).replace('.', ',');
                            t.setAmount(asAmount(amountFX));
                            t.setCurrencyCode(asCurrencyCode(t.getPortfolioTransaction().getCurrencyCode()));

                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                            Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                            Money.of(v.get("fxCurrency"), fxAmount), reverseRate);

                            t.getPortfolioTransaction().addUnit(grossValue);
                        }
                    }
                })

                /***
                 * If the taxes are negative, 
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If the currency of the tax differs from the amount, 
                 * it will be converted and reset.
                 */

                // a b g e f ü h rt e S t e u er n                   E_ U_ R_ _ _ _ _ _ _ _  _ _ __ _ _-1__1,_1_ 1_ 
                .section("taxRefund", "currency").optional()
                .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e"
                                + " ([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n"
                                + " ([\\s_]+)?(?<currency>[\\w\\s_]+)"
                                + " ([\\s_]+)?-(?<taxRefund>[.,\\d\\s_]+)?$")
                .assign((t, v) -> {
                    v.put("taxRefund", stripBlanksAndUnderscores(v.get("taxRefund")));
                    v.put("currency", stripBlanksAndUnderscores(v.get("currency")));

                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift)(.*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // p e r  0 9  . 11 . 2 0 1 0                          U n il  e ve r  N . V  .                           A0  J M Z B
                // S T K            1 . 9 0 0 , 0  0 0                C e r t . v .A a n d e  l e n  E  O -, 1 6            NL  0 00  0 00  9 3 5 5
                // EUR 0,208      Dividende pro Stück für Geschäftsjahr        01.01.10 bis 31.12.10
                .section("name", "wkn", "shares", "nameContinued", "isin", "currency").optional()
                .match("^([\\s]+)?(p([\\s]+)?e([\\s]+)?r) ([\\s]+)?[\\.\\d\\s]+ ([\\s]+)?(?<name>.*)[\\s]{3,}(?<wkn>.*)$")
                .match("^([\\s]+)?(S([\\s]+)?T([\\s]+)?K) ([\\s]+)?(?<shares>[\\.,\\d\\s]+) (?<nameContinued>.*)[\\s]{3,}(?<isin>.*)$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ ([\\s]+)?(Dividende|Aussch.ttung) pro St.ck .*$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setShares(asShares(stripBlanks(v.get("shares"))));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // p e  r  0 3. 1  2 .2  0 20            v  a r ia  b el       SA N H A  G m b  H &   C o.  K  G                     A 1 T NA  7
                // E U R             5. 0 0  0 ,0 0 0                 ST  Z- A n l e  ih e  v  .2  0 1 3 ( 2 3 / 2 6)         D  E0  00  A1 T N A 7 0 
                .section("name", "wkn", "shares", "nameContinued", "isin", "currency").optional()
                .match("^([\\s]+)?(p([\\s]+)?e([\\s]+)?r) ([\\s]+)?[\\.\\d\\s]+ [\\s\\w]{3,} [\\s]{3,}(?<name>.*)[\\s]{3,}(?<wkn>.*)$")
                .match("^(?<currency>[A-Z\\s]+) (?<shares>[\\.,\\d\\s]+) ST ([\\s]+)?(?<nameContinued>.*)[\\s]{3,}(?<isin>[\\w\\s]+)$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("shares", stripBlanks(v.get("shares")));
                    v.put("currency", stripBlanks(v.get("currency")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    // Workaround for bonds
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 000000000  EUR            00000000      15.12.2010         EUR             335,92
                .section("date")
                .match("^.* Zu Ihren Gunsten vor Steuern(.*)?$")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\s]+[\\w]{3} [\\s]+[\\.,\\d]+(.*)?$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Verrechnung über Konto                  Valuta       Zu Ihren Gunsten vor Steuern
                // 0000000 00     EUR                      27.04.2009         EUR           1.546,13
                .section("currency", "amount")
                .match("^.* Zu Ihren Gunsten vor Steuern(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Bruttobetrag:                     USD              16,00
                //     zum Devisenkurs: EUR/USD      1,167800                 EUR              11,65 
                // Bruttobetrag                     USD              10,50                           
                //     zum Devisenkurs EURUSD      1,185400                 EUR               7,52   
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Bruttobetrag([:])? ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxAmount>[\\.,\\d]+)(.*)?$")
                .match("^(.*)?zum Devisenkurs([:])? [\\w]{3}([\\/])?[\\w]{3} ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long localAmount = exchangeRate.multiply(BigDecimal.valueOf(fxAmount.getAmount()))
                                            .longValue();
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), localAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long forexAmount = exchangeRate.multiply(BigDecimal.valueOf(amount.getAmount()))
                                            .longValue();
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), forexAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        // remove existing unit to replace with new one
                        Optional<Unit> grossUnit = t.getUnit(Unit.Type.GROSS_VALUE);
                        if (grossUnit.isPresent())
                        {
                            t.removeUnit(grossUnit.get());
                        }
                        t.addUnit(grossValue);
                    }
                })

                //              zum Devisenkurs: EUR/USD      1,167800                 EUR              11,65 
                //     zum Devisenkurs EURUSD      1,185400                 EUR               7,52   
                .section("exchangeRate").optional()
                .match("^(.*)?zum Devisenkurs([:])? [\\w]{3}([\\/])?[\\w]{3} ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(.*)?$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // zahlbar ab 19.03.2020                 Quartalsdividende                            
                // zahlbar ab 15.12.2010                 Zwischendividende
                // zahlbar ab 19.10.2017                 monatl. Dividende                            
                .section("note").optional()
                .match("^zahlbar ab [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)(?<note>.*)$")
                .assign((t, v) -> t.setNote(TextUtil.strip(v.get("note"))))
                
                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        
        block.set(pdfTransaction);
    }

    private void addTaxTreatmentForDividendeTransaction()
    {
        DocumentType type = new DocumentType("Steuerliche Behandlung: ((Aus|In)l.ndische (Dividende|Investment-Aussch.ttung)|Zinsen .*)");
        this.addDocumentTyp(type);

        Block block = new Block("^Steuerliche Behandlung: .*$", "^Die Gutschrift erfolgt mit Valuta .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Stk.             518 PROCTER GAMBLE , WKN / ISIN: 852062  / US7427181091           
                // EUR           5.000 SANHA ANL 13/26 STZ , WKN / ISIN: A1TNA7  / DE000A1TNA70
                // Z u  Ih r e n G u n s t e n n a c h S t e u er n :       U S D             126,3 2 
                .section("shares", "name", "wkn", "isin", "currency")
                .match("^[\\w]{3}. ([\\s]+)?(?<shares>[\\.,\\d]+) (?<name>.*), WKN \\/ ISIN: (?<wkn>.*) \\/ (?<isin>[\\w]{12})(.*)?$")
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currency>[\\w\\s]+)"
                                + " ([\\s]+)?[\\.,\\d\\s]+([\\W]+)?$")
                .assign((t, v) -> {
                    v.put("currency", stripBlanks(v.get("currency")));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Die Gutschrift erfolgt mit Valuta 20.02.2020 auf Konto EUR mit der IBAN DE12 3456 7890 1234 5678 00   
                .section("date")
                .match("^Die Gutschrift erfolgt mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Z u  Ih r e n G u n s t e n n a c h S t e u er n :              E U R             302,5 5   
                .section("currency", "amount")
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currency>[\\w\\s]+)"
                                + " ([\\s]+)?(?<amount>[\\.,\\d\\s]+)([\\W]+)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                })

                //  Zu  Ih r e n G u n s t e n v o r S te u e r n :              E U R             302,5 5   
                //  S te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g                  E  U   R                             3   5  5 , 9 5                          
                .section("currency", "gross1", "gross2").optional()
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currency>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<gross1>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e"
                                + "([\\s]+)?"
                                + "(v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g)?"
                                + "([\\s]+)?(\\(([\\s]+)?1([\\s]+)?\\))?"
                                + " ([\\s]+)?(?<currency>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<gross2>[\\.,\\d\\s]+)([\\W]+)?$")
                .assign((t, v) -> {
                    long amount = t.getAmount();
                    long gross1 = asAmount(stripBlanks(v.get("gross1")));
                    long gross2 = asAmount(stripBlanks(v.get("gross2")));
                    long tax = 0;

                    if (gross1 > gross2)
                        // before taxes > Tax base
                        tax = gross1 - amount;
                    else
                        // before taxes < tax base
                        tax = gross2 - amount;

                    if (tax > 0)
                        t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(stripBlanks(v.get("currency"))), tax)));
                })

                //  Zu  Ih r e n G u n s t e n v o r S te u e r n :              E U R             302,5 5   
                //  S te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g                  E  U   R                             3   5  5 , 9 5
                // Umrechnungen zum Devisenkurs       1,189700                                                                             
                .section("currency1", "gross1", "currency2", "gross2", "exchangeRate").optional()
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?[A-Z\\s]+"
                                + " ([\\s]+)?[.,\\d\\s]+"
                                + " ([\\s]+)?(?<currency1>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<gross1>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e"
                                + "([\\s]+)?"
                                + "(v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g)?"
                                + "([\\s]+)?(\\(([\\s]+)?1([\\s]+)?\\))?"
                                + " ([\\s]+)?(?<currency2>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<gross2>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    v.put("currency1", stripBlanks(v.get("currency1")));
                    v.put("gross1", stripBlanks(v.get("gross1")));
                    v.put("currency2", stripBlanks(v.get("currency2")));
                    v.put("gross2", stripBlanks(v.get("gross2")));
                    v.put("exchangeRate", stripBlanks(v.get("exchangeRate")));

                    long tax = 0;
                    long amount = t.getAmount();
                    long gross1 = asAmount(v.get("gross1"));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("currency1"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (t.getCurrencyCode().contentEquals(t.getSecurity().getCurrencyCode()))
                    {
                        if (!t.getCurrencyCode().equalsIgnoreCase(asCurrencyCode(v.get("currency2"))))
                        {
                            Money fxGross2 = Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("gross2")));
                            BigDecimal fxAmount2 = BigDecimal.valueOf(fxGross2.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN);
                            
                            if (gross1 > fxAmount2.longValue())
                            {
                                // before taxes > Tax base
                                tax = gross1 - amount;
                            }
                            else
                            {
                                // before taxes < tax base
                                tax = fxAmount2.longValue() - amount;
                            }

                            if (tax > 0)
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency1")), tax)));
                        }
                    }
                })

                .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^Steuerliche Behandlung: Vorabpauschale .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Stk.              11,486 ISIV-MSCI FRAN. U.ETF EOA , WKN / ISIN: A12ATD  / IE00BP3QZJ36   
                .section("shares", "name", "wkn", "isin")
                .match("^Stk. ([\\s]+)?(?<shares>[\\.,\\d]+) (?<name>.*) , (WKN \\/ ISIN:) (?<wkn>.*) \\/ (?<isin>[\\w]{12})(.*)?$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setShares(asShares(stripBlanks(v.get("shares"))));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Die Belastung erfolgt mit Valuta 14.01.2020 auf Konto EUR mit der IBAN XXXXXX         
                .section("date")
                .match("^Die Belastung erfolgt mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // VERRECHNUNGSKONTO VALUTA BETRAG
                // DE12345678912345678912 04.01.2021 -0,32 EUR
                .section("currency", "amount")
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + " ([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + " ([\\s]+)?L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + " ([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h"
                                + " ([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " [\\s]+(?<currency>[\\w\\s]+)"
                                + " ([\\s]+)?(?<amount>[-\\.,\\d\\s]+)$")
                .assign((t, v) -> {
                    v.put("amount", stripBlanksAndUnderscores(v.get("amount")));
                    v.put("currency", stripBlanksAndUnderscores(v.get("currency")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> new TransactionItem(t));
    }

    private void addSellWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierverkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^(\\*[\\s]+)?Wertpapierverkauf(.*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return t;
        });

        pdfTransaction
                // Wertpapier-Bezeichnung                                               WPKNR/ISIN 
                // BASF                                           BASF11                           
                // Inhaber-Anteile                                                    DE000BASF111 
                // St.  1,000                EUR  1,000                                            
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00 
                .section("name", "wkn", "nameContinued", "isin", "shares", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,})(.*)?$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12})(.*)?$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelszeit       : 09:04 Uhr (MEZ/MESZ)                  (Kommissionsgeschäft) 
                .section("time").optional()
                .match("Handelszeit ([\\s]+)?: ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}) Uhr.*")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Geschäftstag      : 01.01.2000        Ausführungsplatz  : XETRA     
                .section("date")
                .match("^Gesch.ftstag ([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                .section("negative").optional()
                .match("^.* Zu Ihren Lasten( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(?<negative>[-])(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        type.getCurrentContext().put("negative", "X");
                    }
                })

                //        Umrechn. zum Dev. kurs 1,080600 vom 17.04.2020 : EUR            4.425,22 
                .section("exchangeRate").optional()
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                //                           Kurswert                    : EUR                3,54 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern                                                    
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                9,61- 
                .section("fxCurrency", "fxAmount", "currency", "amount").optional()
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)(.*)?$")
                .match("^.* Zu Ihren Lasten( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("fxAmount")) + asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00  
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // DExx xxxx xxxx xxxx xxxx xx   EUR     24.11.2016        EUR            1.431,40  
                .section("fxCurrency", "fxAmount", "currency", "amount").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxAmount>[\\.,\\d]+)(.*)?$")
                .match("^.* Zu Ihren Lasten( vor Steuern)?(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("fxAmount")) + asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                //                           Kurswert                    : USD                3,54 
                //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR                2,28 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR               10,12- 
                .section("amount", "currency", "exchangeRate", "fxCurrency", "amount2").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)(.*)?$")
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount2>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        // read the forex currency, exchange rate and gross
                        // amount in forex currency
                        String forex = asCurrencyCode(v.get("currency"));
                        if (!t.getSecurity().getCurrencyCode().equals(forex))
                        {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                            RoundingMode.HALF_DOWN);

                            // gross given in forex currency
                            long fxAmount = asAmount(v.get("amount"));
                            long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                            .setScale(0, RoundingMode.HALF_DOWN).longValue();

                            // set amount in account currency
                            String amountFX =  Double.toString((double)amount / 100.0).replace('.', ',');
                            t.setAmount(asAmount(amountFX) + asAmount(v.get("amount2")));
                            t.setCurrencyCode(asCurrencyCode(t.getCurrencyCode()));

                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                            Money.of(t.getCurrencyCode(), amount),
                                            Money.of(v.get("fxCurrency"), fxAmount), reverseRate);

                            t.addUnit(grossValue);
                        }
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addDepositoryFeeTransaction()
    {
        DocumentType type = new DocumentType("Verwahrentgelt");
        this.addDocumentTyp(type);

        Block block = new Block("^.* Verwahrentgelt .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.FEES);
                return entry;
            });

        pdfTransaction
                    // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.01.2020
                    .section("name", "wkn", "date")
                    .match("^.*Verwahrentgelt (?<name>.*), WKN (?<wkn>.*) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                    .assign((t, v) -> {
                        v.put("wkn", stripBlanks(v.get("wkn")));

                        t.setDateTime(asDate(v.get("date")));
                        t.setSecurity(getOrCreateSecurity(v));
                    })

                    // Die Buchung von 0,01 Euro für den vorherigen Monat erfolgte über das Abrechnungskonto für
                    // den vorherigen Monat mit einem Entgelt in Höhe von 123,45 Euro. Das entspricht 0,0298 %
                    .section("currency", "amount")
                    .match("^.* (Buchung|H.he) von (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                    .assign((t, v) -> {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                    })

                    .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the buy/sell transaction function must be adjusted.
         * addBuySellTransaction();
         */
        Block block = new Block("^(\\*[\\s]+)?(Wertpapierkauf|Wertpapierverkauf)(.*)?$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Wertpapier-Bezeichnung                                               WPKNR/ISIN 
                // BASF                                           BASF11                           
                // Inhaber-Anteile                                                    DE000BASF111 
                // St.  1,000                EUR  1,000                                            
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00 
                .section("name", "wkn", "nameContinued", "isin", "shares", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,})(.*)?$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12})(.*)?$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelszeit       : 09:04 Uhr (MEZ/MESZ)                  (Kommissionsgeschäft) 
                .section("time").optional()
                .match("Handelszeit *: (?<time>[\\d]{2}:[\\d]{2}) Uhr.*")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Geschäftstag      : 01.01.2000        Ausführungsplatz  : XETRA     
                .section("date")
                .match("^Gesch.ftstag [\\s]+: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                //        Umrechn. zum Dev. kurs 1,080600 vom 17.04.2020 : EUR            4.425,22 
                .section("exchangeRate").optional()
                .match("^.* Umrechn. zum Dev. kurs (?<exchangeRate>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {                   
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // IBAN                                  Valuta        Zu Ihren Gunsten vor Steuern 
                // DE09 9999 9999 9999 9999 00   EUR     01.01.2010        EUR           10.111,11
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // EUR     30.12.2020        EUR            1.430,30 
                // e r s ta t te t e S t e ue r n     E_ _U R_ _ _ _ _ _ _ _ _  _ _ __ _ 7_1__,7_ 3_
                .section("currency", "amount").optional()
                .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n"
                                + " ([\\s]+)?(?<currency>[\\w\\s_]+)"
                                + " ([\\s_]+)?(?<amount>[-\\.,\\d\\s_]+)(.*)?$")
                .assign((t, v) -> {
                    v.put("amount", stripBlanksAndUnderscores(v.get("amount")));
                    v.put("currency", stripBlanksAndUnderscores(v.get("currency")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    /***
                     * If the currency of the security 
                     * differs from the account currency
                     */
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        Money amount = Money.of(t.getCurrencyCode(), asAmount(v.get("amount")));

                        // convert gross to security currency using exchangeRate
                        long gross = exchangeRate.multiply(BigDecimal.valueOf(asAmount(v.get("amount")))
                                        .setScale(0, RoundingMode.HALF_DOWN)).longValue();

                        Money fxAmount = Money.of(t.getSecurity().getCurrencyCode(), gross);

                        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate));
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addFinancialReport()
    {
        DocumentType type = new DocumentType("Finanzreport", (context, lines) -> {
            Pattern pBaseCurrency = Pattern.compile("^(Kontow.hrung) ([\\w]{3})$");
            Pattern pForeignCurrencyAccount = Pattern.compile("^(W.hrungsanlagekonto) \\(([\\w]{3})\\) .*$");
            Pattern pStartForeignCurrency = Pattern.compile("^(W.hrungsanlagekonto) \\(([\\w]{3})\\)$");
            Pattern pEndForeignCurrency = Pattern.compile("^Neuer Saldo .*$");

            Boolean ForeignCurrencyAccount = false;

            // read the current context here
            for (int i = 0; i < lines.length; i++)
            {
                // Ihre aktuellen Salden IBAN Saldo in
                // EUR
                if (lines[i].compareTo("Ihre aktuellen Salden IBAN Saldo in") == 0)
                {
                    context.put("currency", lines[i+1]);
                }
                // Ihre aktuellen Salden Saldo in
                // IBAN EUR
                if ((lines[i].compareTo("Ihre aktuellen Salden Saldo in") == 0) && (lines[i+1].substring(0,4).compareTo("IBAN") == 0))
                {
                    context.put("currency", lines[i+1].substring(5, 8));
                }
                // Kontowährung EUR
                Matcher m = pBaseCurrency.matcher(lines[i]);
                if (m.matches())
                {
                    context.put("currency", m.group(2));
                }
                // Währungsanlagekonto (USD) DE31 2004 1155 1234 5678 05 +554,83 +487,76
                m = pForeignCurrencyAccount.matcher(lines[i]);
                if (m.matches())
                {
                    context.put("foreignCurrency", m.group(2));
                }
                
                // Sets the start and end line of the foreign currency transactions
                m = pStartForeignCurrency.matcher(lines[i]);
                if (m.matches())
                {
                    context.put("startInForeignCurrency", Integer.toString(i));
                    ForeignCurrencyAccount = true;
                }
                m = pEndForeignCurrency.matcher(lines[i]);
                if (m.matches() && ForeignCurrencyAccount)
                {
                    context.put("endInForeignCurrency", Integer.toString(i));
                    ForeignCurrencyAccount = false;
                }
            }
        });
        this.addDocumentTyp(type);

        Block removalblock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Konto.bertrag|.bertrag|Lastschrift|Visa-Umsatz|Auszahlung|Barauszahlung|Kartenverf.gun|Guthaben.bertr|Wechselgeld-Sparen).* [-]([\\.,\\d]+)$");
        type.addBlock(removalblock);
        removalblock.setMaxSize(3);
        removalblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Konto.bertrag|.bertrag|Lastschrift|Visa-Umsatz|Auszahlung|Barauszahlung|Kartenverf.gun|Guthaben.bertr|Wechselgeld-Sparen).* [-](?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));     
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));

                            // When we recognize a foreign currency account,
                            // we change from currency to foreign currency

                            boolean hasForeignCurrencyBlock = context.containsKey("startInForeignCurrency")
                                            && context.containsKey("endInForeignCurrency");

                            if (hasForeignCurrencyBlock
                                            && v.getStartLineNumber() >= Integer
                                                            .parseInt(context.get("startInForeignCurrency"))
                                            && v.getEndLineNumber() <= Integer
                                                            .parseInt(context.get("endInForeignCurrency")))
                            {
                                    t.setCurrencyCode(context.get("foreignCurrency"));
                            }

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block depositblock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Konto.bertrag|.bertrag|Guthaben.bertr|Gutschrift|Bar|Visa-Kartenabre|Korrektur Barauszahlung).* [+]([\\.,\\d]+)$");
        type.addBlock(depositblock);
        depositblock.setMaxSize(3);
        depositblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Konto.bertrag|.bertrag|Guthaben.bertr|Gutschrift|Bar|Visa-Kartenabre|Korrektur Barauszahlung).* [+](?<amount>[\\.,\\d]+)$")
                        .match("(^|^A)(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));

                            // When we recognize a foreign currency account,
                            // we change from currency to foreign currency

                            boolean hasForeignCurrencyBlock = context.containsKey("startInForeignCurrency")
                                            && context.containsKey("endInForeignCurrency");

                            if (hasForeignCurrencyBlock
                                            && v.getStartLineNumber() >= Integer
                                                            .parseInt(context.get("startInForeignCurrency"))
                                            && v.getEndLineNumber() <= Integer
                                                            .parseInt(context.get("endInForeignCurrency")))
                            {
                                    t.setCurrencyCode(context.get("foreignCurrency"));
                            }

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block feeblock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Kontoabschluss Kontof.hrung|Geb.hren\\/Spesen|Geb.hr Barauszahlung|Entgelte|Auslandsentgelt).* [-]([\\.,\\d]+)$");
        type.addBlock(feeblock);
        feeblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.FEES);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Kontoabschluss Kontof.hrung|Geb.hren\\/Spesen|Geb.hr Barauszahlung|Entgelte|Auslandsentgelt).* [-](?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block interestblock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Kontoabschluss Abschluss Zinsen).* [+]([\\.,\\d]+)$");
        type.addBlock(interestblock);
        interestblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Kontoabschluss Abschluss Zinsen).* [+](?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .section("kest", "soli", "kestcur", "solicur", "note1", "note2").optional()
                        .match("^(?<note1>Kapitalertragsteuer) (?<kest>[\\.,\\d]+)- (?<kestcur>[\\w]{3})$")
                        .match("^(?<note2>Solidarit.tszuschlag) (?<soli>[\\.,\\d]+)([-])? (?<solicur>[\\w]{3})$")
                        .assign((t, v) -> {
                            Money kest = Money.of(asCurrencyCode(v.get("kestcur")), asAmount(v.get("kest")));
                            if (kest.getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(new Unit(Unit.Type.TAX, kest));

                            Money soli = Money.of(asCurrencyCode(v.get("solicur")), asAmount(v.get("soli")));
                            if (soli.getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(new Unit(Unit.Type.TAX, soli));

                            t.setNote(v.get("note1") + "/" + v.get("note2"));
                        })

                        .wrap(TransactionItem::new));
        
        Block interestchargeblock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Kontoabschluss Abschluss Zinsen).* [-]([\\.,\\d]+)$");
        type.addBlock(interestchargeblock);
        interestchargeblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Kontoabschluss Abschluss Zinsen).* [-](?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // a b g e f ü h rt e S t e u er n       E_ U_ R_ _ _ _ _ _ _ _  _ _ __ _ _  __0,_0_ 0_ 
                .section("currency", "tax").optional()
                .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n"
                                + " [\\s]+(?<currency>[\\w\\s_]+)"
                                + " ([\\s]+)?(?<tax>[-\\.,\\d\\s_]+)$")
                .assign((t, v) -> {
                    v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                    v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                    processTaxEntries(t, v, type);  
                })

                // 15,000 % Quellensteuer                                     EUR              59,28 -
                .section("currency", "tax").optional()
                .match("^([\\s]+)?[\\.,\\d]+ % Quellensteuer ([\\s]+)?(?<currency>[\\w]+) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // 25,000 % Kapitalertragsteuer auf  EUR           2.100,00   EUR             525,00 -
                .section("currency", "tax").optional()
                .match("^([\\s]+)?[\\.,\\d]+ % Kapitalertragsteuer .* ([\\s]+)?(?<currency>[\\w]+) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                //  5,500 % Solidaritätszuschl. auf  EUR             525,00   EUR              28,87 -
                .section("currency", "tax").optional()
                .match("^([\\s]+)?[\\.,\\d]+ % Solidarit.tszuschl. .* ([\\s]+)?(?<currency>[\\w]+) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                //  5,500 % Kirchensteuer auf  EUR             1,00   EUR              1,00 -
                .section("currency", "tax").optional()
                .match("^([\\s]+)?[\\.,\\d]+ % Kirchensteuer .* ([\\s]+)?(?<currency>[\\w]+) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                //                           Provision                   : EUR               12,10 
                .section("currency", "fee").optional()
                .match("^.* Provision ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                           Gesamtprovision             : EUR                9,90 
                .section("currency", "fee").optional()
                .match("^.* Gesamtprovision ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                           Börsenplatzabhäng. Entgelt  : EUR                1,50 
                .section("currency", "fee").optional()
                .match("^.* B.rsenplatzabh.ng. Entgelt ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                           Umschreibeentgelt           : EUR                0,60
                .section("currency", "fee").optional()
                .match("^.* Umschreibeentgelt ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                           Abwickl.entgelt Clearstream : EUR                2,90
                .section("currency", "fee").optional()
                .match("^.* Abwickl.entgelt Clearstream ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                }) 

                //                           Abwickl.entgelt Clearstream : EUR                2,90
                .section("currency", "fee").optional()
                .match("^.* Variable B.rsenspesen ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                  0,08000% Maklercourtage              : EUR                0,88- 
                .section("currency", "fee").optional()
                .match("^.* Maklercourtage ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                //                           Fremde Spesen               : USD               13,90 
                .section("currency", "fee").optional()
                .match("^.* Fremde Spesen ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                });
    }

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

    /***
     * In some cases, two documents are created for a dividend transaction. 
     * Once the dividend payment and once the tax treatment.
     * 
     * If both are imported at the same time, 
     * then the taxes are recalculated.
     */
    @Override
    public List<Item> postProcessing(List<Item> items)
    {
        // group dividends into tax + nontax
        Map<LocalDateTime, Map<Security, List<Item>>> dividends = items.stream()
                        .filter(TransactionItem.class::isInstance)
                        .map(TransactionItem.class::cast)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> AccountTransaction.Type.DIVIDENDS
                                        .equals(((AccountTransaction) i.getSubject()).getType()))
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        Iterator<Item> iterator = items.iterator();

        while (iterator.hasNext())
        {
            Item i = iterator.next();

            if (isDividendTransaction(i))
            {
                List<Item> similarTransactions = dividends.get(i.getDate()).get(i.getSecurity());

                // Are there multiple dividend transactions?
                if (similarTransactions.size() == 2)
                {
                    AccountTransaction a1 = (AccountTransaction) similarTransactions.get(0).getSubject();
                    AccountTransaction a2;
                    int ownIndex = 0;

                    // a1 = self, a2 = other
                    if (i.equals(similarTransactions.get(0)))
                    {
                        a2 = (AccountTransaction) similarTransactions.get(1).getSubject();
                    }
                    else
                    {
                        a1 = (AccountTransaction) similarTransactions.get(1).getSubject();
                        a2 = (AccountTransaction) similarTransactions.get(0).getSubject();
                        ownIndex = 1;
                    }
                    // if tax of a1 < tax of a2
                    if (!a1.getUnit(Type.TAX).isPresent() || a2.getUnit(Type.TAX).get().getAmount().isGreaterOrEqualThan(a1.getUnit(Type.TAX).get().getAmount()))
                    {
                        // store potential gross unit
                        Optional<Unit> unitGross = a1.getUnit(Unit.Type.GROSS_VALUE);
                        if (unitGross.isPresent())
                        {
                            a2.addUnit(unitGross.get());
                        }
                    }

                    // remove self and own divTransaction
                    iterator.remove();
                    dividends.get(i.getDate()).get(i.getSecurity()).remove(ownIndex);
                }
            }
        }

        return items;
    }

    private boolean isDividendTransaction(Item i)
    {
        if (i instanceof TransactionItem)
        {
            Annotated s = ((TransactionItem) i).getSubject();
            if (s instanceof AccountTransaction)
            {
                AccountTransaction a = (AccountTransaction) s;
                return AccountTransaction.Type.DIVIDENDS.equals(a.getType());
            }
        }
        return false;
    }

    private String stripBlanksAndUnderscores(String input)
    {
        return input.replaceAll("[\\s_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll("[\\s]", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }
}

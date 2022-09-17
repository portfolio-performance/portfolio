package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanksAndUnderscores;
import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    /**
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
        DocumentType type = new DocumentType("(Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug|Wertpapierumtausch)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(\\*[\\s]+)?(Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug|Wertpapierumtausch).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(\\*[\\s]+)?(?<type>(Wertpapierkauf|Wertpapierverkauf|Wertpapierbezug|Wertpapierumtausch)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Wertpapierverkauf") || v.get("type").equals("Wertpapierumtausch"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Wertpapier-Bezeichnung                                               WPKNR/ISIN 
                // BASF                                           BASF11                           
                // Inhaber-Anteile                                                    DE000BASF111 
                // St.  1,000                EUR  1,000                                            
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00 
                .section("name", "wkn", "nameContinued", "isin", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,}).*$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12}).*$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?([\\s]+)?St\\. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.  1,000                EUR  1,000                                            
                .section("shares")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?([\\s]+)?St\\. ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?[\\w]{3}.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeit       : 09:04 Uhr (MEZ/MESZ)                  (Kommissionsgeschäft) 
                .section("time").optional()
                .match("^Handelszeit ([\\s]+)?: ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}) Uhr.*$")
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

                // If the type of transaction is "SELL" and the amount
                // is negative, then the gross amount set.
                // Fees are processed in a separate transaction
                .section("negative").optional()
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(?<negative>\\-).*$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getType().isLiquidation())
                        type.getCurrentContext().put("negative", "X");
                })

                // IBAN                                  Valuta        Zu Ihren Gunsten vor Steuern 
                // DE09 9999 9999 9999 9999 00   EUR     01.01.2010        EUR           10.111,11
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // EUR     30.12.2020        EUR            1.430,30 
                .section("amount", "currency")
                .match("^.* Zu Ihren (Lasten|Gunsten)( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {                   
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                //                           Kurswert                    : EUR                3,54 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,86- 
                .section("amount", "currency", "fxCurrency").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                .match("^.* Zu Ihren (Lasten|Gunsten)( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
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
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .match("^.* Zu Ihren Lasten( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
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

                //                           Kurswert                    : USD            1.573,75 
                //                           Ausmachender Betrag         : USD            1.559,85 
                //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR                2,28 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR               10,12- 
                .section("fxCurrency", "fxGross", "termCurrency", "exchangeRate", "baseCurrency", "currency").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .match("^.* Ausmachender Betrag ([\\s]+)?: ([\\s]+)?(?<termCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .match("^.* (Umrechn\\. zum Dev\\. kurs|Umrechnung zum Devisenkurs) (?<exchangeRate>[\\.,\\d]+).* : (?<baseCurrency>[\\w]{3}).*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));
                    
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    type.getCurrentContext().putType(asExchangeRate(v));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                //  Summe        St.  720                USD  40,098597    USD           28.870,99 
                //                           Ausmachender Betrag           USD           28.898,89 
                //        Umrechn. zum Dev. kurs 1,120800 vom 12.03.2020 : EUR           25.784,17 
                .section("fxCurrency", "fxGross", "termCurrency", "exchangeRate", "baseCurrency", "currency").optional()
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .match("^.* Ausmachender Betrag ([\\s]+)?(?<termCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .match("^.* (Umrechn\\. zum Dev\\. kurs|Umrechnung zum Devisenkurs) (?<exchangeRate>[\\.,\\d]+).* : (?<baseCurrency>[\\w]{3}).*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));
                    
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    type.getCurrentContext().putType(asExchangeRate(v));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // If the taxes are negative, this is a tax refund
                // transaction and we subtract this from the amount and
                // reset this.
                // If the currency of the tax differs from
                // the amount, it will be converted and reset.

                // a b g e f ü h rt e S t e u er n                   E_ U_ R_ _ _ _ _ _ _ _  _ _ __ _ _-1__1,_1_ 1_ 
                .section("taxRefund", "currency").optional()
                .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e"
                                + " ([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n"
                                + " ([\\s_]+)?(?<currency>[A-Z\\s_]+)"
                                + " ([\\s_]+)?\\-(?<taxRefund>[.,\\d\\s_]+)?$")
                .assign((t, v) -> {
                    Money taxRefund = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currency"))), asAmount(stripBlanksAndUnderscores(v.get("taxRefund"))));

                    if (t.getPortfolioTransaction().getCurrencyCode().equals(stripBlanksAndUnderscores(v.get("currency"))))
                        t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                })
                
                .conclude(PDFExtractorUtils.fixGrossValueBuySell())

                .wrap(t -> {
                    // If we have multiple entries in the document,
                    // then the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // p e r  0 9  . 11 . 2 0 1 0                          U n il  e ve r  N . V  .                           A0  J M Z B
                // S T K            1 . 9 0 0 , 0  0 0                C e r t . v .A a n d e  l e n  E  O -, 1 6            NL  0 00  0 00  9 3 5 5
                // EUR 0,208      Dividende pro Stück für Geschäftsjahr        01.01.10 bis 31.12.10
                .section("name", "wkn", "nameContinued", "isin", "currency").optional()
                .match("^([\\s]+)?(p([\\s]+)?e([\\s]+)?r) ([\\s]+)?[\\.\\d\\s]+ ([\\s]+)?(?<name>.*)[\\s]{3,}(?<wkn>.*)$")
                .match("^([\\s]+)?(S([\\s]+)?T([\\s]+)?K) ([\\s]+)?[\\.,\\d\\s]+ (?<nameContinued>.*)[\\s]{3,}(?<isin>.*)$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ ([\\s]+)?(Dividende|Aussch.ttung) pro St.ck .*$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // p e  r  0 3. 1  2 .2  0 20            v  a r ia  b el       SA N H A  G m b  H &   C o.  K  G                     A 1 T NA  7
                // E U R             5. 0 0  0 ,0 0 0                 ST  Z- A n l e  ih e  v  .2  0 1 3 ( 2 3 / 2 6)         D  E0  00  A1 T N A 7 0 
                .section("name", "wkn", "nameContinued", "isin", "currency").optional()
                .match("^([\\s]+)?(p([\\s]+)?e([\\s]+)?r) ([\\s]+)?[\\.\\d\\s]+ [\\s\\w]{3,} [\\s]{3,}(?<name>.*)[\\s]{3,}(?<wkn>.*)$")
                .match("^(?<currency>[A-Z\\s]+) [\\.,\\d\\s]+ ST ([\\s]+)?(?<nameContinued>.*)[\\s]{3,}(?<isin>[\\w\\s]+)$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("currency", stripBlanks(v.get("currency")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // S T K            1 . 9 0 0 , 0  0 0                C e r t . v .A a n d e  l e n  E  O -, 1 6            NL  0 00  0 00  9 3 5 5
                                section -> section
                                        .attributes("shares")
                                        .match("^([\\s]+)?(S([\\s]+)?T([\\s]+)?K) ([\\s]+)?(?<shares>[\\.,\\d\\s]+) .*$")
                                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))
                                ,
                                // E U R             5. 0 0  0 ,0 0 0                 ST  Z- A n l e  ih e  v  .2  0 1 3 ( 2 3 / 2 6)         D  E0  00  A1 T N A 7 0 
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z\\s]+ (?<shares>[\\.,\\d\\s]+) ST .*$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            t.setShares(asShares(stripBlanks(v.get("shares"))) / 100);
                                        })
                        )

                // 000000000  EUR            00000000      15.12.2010         EUR             335,92
                // Gutschrift auf Konto                    Valuta             Zu Ihren Gunsten        
                .section("date")
                .match("^.* Zu Ihren Gunsten( vor Steuern)?.*$")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+.*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Verrechnung über Konto                  Valuta       Zu Ihren Gunsten vor Steuern
                // 0000000 00     EUR                      27.04.2009         EUR           1.546,13
                // Gutschrift auf Konto                    Valuta             Zu Ihren Gunsten        
                // 1111111 11     EUR                      15.05.2008         EUR             126,24  
                .section("currency", "amount")
                .match("^.* Zu Ihren Gunsten( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Bruttobetrag:                     USD              16,00
                //     zum Devisenkurs: EUR/USD      1,167800                 EUR              11,65 
                // Bruttobetrag                     USD              10,50                           
                //     zum Devisenkurs EURUSD      1,185400                 EUR               7,52
                .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional()
                .match("^Bruttobetrag(:)? ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .match("^.*zum Devisenkurs(:)? (?<baseCurrency>[\\w]{3})(\\/)?(?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // In this section we calculate the taxes. If the gross
                // value is in foreign currency, it will be converted to
                // the posting currency. Otherwise we subtract the net
                // amount from the gross amount
                 
                // Bruttobetrag:                     USD              22,60
                // Bruttobetrag                     USD              10,50  
                .section("currency", "gross").optional()
                .match("^Bruttobetrag(:)? ([\\s]+)?(?<currency>\\w{3}) ([\\s]+)?(?<gross>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                    Optional<PDFExchangeRate> exchangeRate = type.getCurrentContext().getType(PDFExchangeRate.class);

                    if (!t.getCurrencyCode().equals(gross.getCurrencyCode()) && exchangeRate.isPresent())
                        gross = exchangeRate.get().convert(t.getCurrencyCode(), gross);

                    Money tax = gross.subtract(t.getMonetaryAmount());

                    checkAndSetTax(tax, t, type);
                })

                // zahlbar ab 19.03.2020                 Quartalsdividende                            
                // zahlbar ab 15.12.2010                 Zwischendividende
                // zahlbar ab 19.10.2017                 monatl. Dividende                            
                .section("note").optional()
                .match("^zahlbar ab [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<note>(?i).*dividende).*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))
                
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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stk.             518 PROCTER GAMBLE , WKN / ISIN: 852062  / US7427181091           
                // EUR           5.000 SANHA ANL 13/26 STZ , WKN / ISIN: A1TNA7  / DE000A1TNA70
                // Z u  Ih r e n G u n s t e n n a c h S t e u er n :       U S D             126,3 2 
                .section("name", "wkn", "isin", "currency")
                .match("^[\\w]{3}(\\.)? ([\\s]+)?[\\.,\\d]+ (?<name>.*), WKN \\/ ISIN: (?<wkn>.*) \\/ (?<isin>[\\w]{12}).*$")
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currency>[A-Z\\s_]+)"
                                + " ([\\s]+)?[\\.,\\d\\s]+([\\W]+)?$")
                .assign((t, v) -> {
                    v.put("currency", stripBlanks(v.get("currency")));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // // Stk.             518 PROCTER GAMBLE , WKN / ISIN: 852062  / US7427181091           
                .section("shares")
                .match("^[\\w]{3}(\\.)? ([\\s]+)?(?<shares>[\\.,\\d]+) (?<name>.*), WKN \\/ ISIN: .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Die Gutschrift erfolgt mit Valuta 20.02.2020 auf Konto EUR mit der IBAN DE12 3456 7890 1234 5678 00   
                .section("date")
                .match("^Die Gutschrift erfolgt mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Z u  Ih r e n G u n s t e n n a c h S t e u er n :              E U R             302,5 5   
                .section("currency", "amount")
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currency>[A-Z\\s_]+)"
                                + " ([\\s]+)?(?<amount>[\\.,\\d\\s]+)([\\W]+)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                })

                //  Zu  Ih r e n G u n s t e n v o r S te u e r n :              E U R             302,5 5   
                //  S te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g                  E  U   R                             3   5  5 , 9 5                          
                .section("currencyBeforeTaxes", "grossBeforeTaxes", "currencyAssessmentBasis", "grossAssessmentBasis").optional()
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?:"
                                + " ([\\s]+)?(?<currencyBeforeTaxes>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<grossBeforeTaxes>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e"
                                + "([\\s]+)?"
                                + "(v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g)?"
                                + "([\\s]+)?(\\(([\\s]+)?1([\\s]+)?\\))?"
                                + " ([\\s]+)?(?<currencyAssessmentBasis>[A-Z\\s]+)"
                                + " ([\\s]+)?(?<grossAssessmentBasis>[\\.,\\d\\s]+)([\\W]+)?$")
                .assign((t, v) -> {
                    Money grossValueBeforeTaxes = Money.of(asCurrencyCode(stripBlanks(v.get("currencyBeforeTaxes"))), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                    Money taxAssessmentBasis = Money.of(asCurrencyCode(stripBlanks(v.get("currencyAssessmentBasis"))), asAmount(stripBlanks(v.get("grossAssessmentBasis"))));

                    // Use value which is greater:
                    // The tax assessment basis can include foreign withholding taxes which
                    // have been deducted from the gross value before taxes value.
                    Money tax = null;
                    if (grossValueBeforeTaxes.isGreaterOrEqualThan(taxAssessmentBasis))
                        tax = grossValueBeforeTaxes.subtract(t.getMonetaryAmount());
                    else
                        tax = taxAssessmentBasis.subtract(t.getMonetaryAmount());

                    checkAndSetTax(tax, t, type);
                })

                //  Zu  Ih r e n G u n s t e n v o r S te u e r n :              E U R             302,5 5   
                //  S te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g                  E  U   R                             3   5  5 , 9 5
                // Umrechnungen zum Devisenkurs       1,189700                                                                             
                .section("currencyBeforeTaxes", "grossBeforeTaxes", "currencyAssessmentBasis", "grossAssessmentBasis", "exchangeRate").optional()
                .match("^([\\s]+)?Z([\\s]+)?u"
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n"
                                + "([\\s]+)?v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: "
                                + "([\\s]+)?[A-Z\\s]+ "
                                + "([\\s]+)?[.,\\d\\s]+ "
                                + "([\\s]+)?(?<currencyBeforeTaxes>[A-Z\\s]+) "
                                + "([\\s]+)?(?<grossBeforeTaxes>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e"
                                + "([\\s]+)?"
                                + "(v([\\s]+)?o([\\s]+)?r"
                                + "([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g)?"
                                + "([\\s]+)?(\\(([\\s]+)?1([\\s]+)?\\))? "
                                + "([\\s]+)?(?<currencyAssessmentBasis>[A-Z\\s]+) "
                                + "([\\s]+)?(?<grossAssessmentBasis>[\\.,\\d\\s]+)([\\W]+)?$")
                .match("^([\\s]+)?Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Money grossValueBeforeTaxes = Money.of(asCurrencyCode(stripBlanks(v.get("currencyBeforeTaxes"))), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                    Money taxAssessmentBasis = Money.of(asCurrencyCode(stripBlanks(v.get("currencyAssessmentBasis"))), asAmount(stripBlanks(v.get("grossAssessmentBasis"))));

                    if (!t.getCurrencyCode().equals(grossValueBeforeTaxes.getCurrencyCode()))
                        return;

                    if (!grossValueBeforeTaxes.getCurrencyCode().equals(taxAssessmentBasis.getCurrencyCode()))
                    {
                        PDFExchangeRate exchangeRate = new PDFExchangeRate(
                                        asExchangeRate(stripBlanks(v.get("exchangeRate"))),
                                        taxAssessmentBasis.getCurrencyCode(),
                                        grossValueBeforeTaxes.getCurrencyCode());
                        taxAssessmentBasis = exchangeRate.convert(grossValueBeforeTaxes.getCurrencyCode(),
                                        taxAssessmentBasis);
                    }

                    // Use value which is greater:
                    // The tax assessment basis can include foreign withholding taxes which
                    // have been deducted from the gross value before taxes value.
                    Money tax = null;
                    if (grossValueBeforeTaxes.isGreaterOrEqualThan(taxAssessmentBasis))
                        tax = grossValueBeforeTaxes.subtract(t.getMonetaryAmount());
                    else
                        tax = taxAssessmentBasis.subtract(t.getMonetaryAmount());

                    checkAndSetTax(tax, t, type);
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
            return entry;
        });

        Block firstRelevantLine = new Block("^Steuerliche Behandlung: Vorabpauschale .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Stk.              11,486 ISIV-MSCI FRAN. U.ETF EOA , WKN / ISIN: A12ATD  / IE00BP3QZJ36   
                .section("name", "wkn", "isin")
                .match("^Stk\\. ([\\s]+)?[\\.,\\d]+ (?<name>.*) , (WKN \\/ ISIN:) (?<wkn>.*) \\/ (?<isin>[\\w]{12}).*$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stk.              11,486 ISIV-MSCI FRAN. U.ETF EOA , WKN / ISIN: A12ATD  / IE00BP3QZJ36   
                .section("shares")
                .match("^Stk\\. ([\\s]+)?(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Die Belastung erfolgt mit Valuta 14.01.2020 auf Konto EUR mit der IBAN XXXXXX         
                .section("date")
                .match("^Die Belastung erfolgt mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // VERRECHNUNGSKONTO VALUTA BETRAG
                // DE12345678912345678912 04.01.2021 -0,32 EUR
                .section("currency", "amount")
                .match("^([\\s]+)?Z([\\s]+)?u "
                                + "([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n "
                                + "([\\s]+)?L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n "
                                + "([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h "
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: "
                                + "[\\s]+(?<currency>[A-Z\\s_]+) "
                                + "([\\s]+)?(?<amount>[-\\.,\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(stripBlanksAndUnderscores(v.get("amount"))));
                    t.setCurrencyCode(asCurrencyCode(stripBlanksAndUnderscores(v.get("currency"))));
                })

                .wrap(TransactionItem::new);
    }

    private void addSellWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierverkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^(\\*[\\s]+)?Wertpapierverkauf.*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        });

        pdfTransaction
                // Wertpapier-Bezeichnung                                               WPKNR/ISIN 
                // BASF                                           BASF11                           
                // Inhaber-Anteile                                                    DE000BASF111 
                // St.  1,000                EUR  1,000                                            
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00 
                .section("name", "wkn", "nameContinued", "isin", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,}).*$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12}).*$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St\\. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.  1,000                EUR  1,000                                            
                .section("shares")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?St\\. ([\\s]+)?(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

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
                .match("^.* Zu Ihren Lasten( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+(?<negative>\\-).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        type.getCurrentContext().put("negative", "X");
                })

                //                           Kurswert                    : EUR                3,54 
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern                                                    
                // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                9,61- 
                //  Summe        St.  20                 EUR  71,00        EUR            1.420,00  
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // DExx xxxx xxxx xxxx xxxx xx   EUR     24.11.2016        EUR            1.431,40  
                .section("fxCurrency", "fxAmount", "currency", "amount").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxAmount>[\\.,\\d]+).*$")
                .match("^.* Zu Ihren Lasten( vor Steuern)?.*$")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("fxAmount")) + asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        pdfTransaction
                // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.01.2020
                .section("name", "wkn", "date")
                .match("^.* Verwahrentgelt (?<name>.*), WKN (?<wkn>.*) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
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
        Block block = new Block("^(\\*[\\s]+)?(Wertpapierkauf|Wertpapierverkauf).*$");
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
                .section("name", "wkn", "nameContinued", "isin", "currency")
                .match("^Wertpapier-Bezeichnung .*$")
                .match("^(?<name>([\\S]{1,}[\\s]{1})+) [\\s]{3,}(?<wkn>[\\w]{1,}).*$")
                .match("^(?<nameContinued>.*) ([\\s]+)?(?<isin>[\\w]{12}).*$")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?([\\s]+)?St\\. ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.  1,000                EUR  1,000                                            
                .section("shares")
                .match("^(([\\s]+)?Summe ([\\s]+)?)?([\\s]+)?St\\. ([\\s]+)?(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeit       : 09:04 Uhr (MEZ/MESZ)                  (Kommissionsgeschäft) 
                .section("time").optional()
                .match("Handelszeit ([\\s]+)?: (?<time>[\\d]{2}:[\\d]{2}) Uhr.*")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Geschäftstag      : 01.01.2000        Ausführungsplatz  : XETRA     
                .section("date")
                .match("^Gesch.ftstag ([\\s]+)?: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                // IBAN                                  Valuta        Zu Ihren Gunsten vor Steuern 
                // DE09 9999 9999 9999 9999 00   EUR     01.01.2010        EUR           10.111,11
                // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern 
                // EUR     30.12.2020        EUR            1.430,30 
                // e r s ta t te t e S t e ue r n     E_ _U R_ _ _ _ _ _ _ _ _  _ _ __ _ 7_1__,7_ 3_
                .section("currency", "amount").optional()
                .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n "
                                + "([\\s]+)?(?<currency>[A-Z\\s_]+) "
                                + "([\\s_]+)?(?<amount>[\\.,\\d\\s_]+).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(stripBlanksAndUnderscores(v.get("amount"))));
                    t.setCurrencyCode(asCurrencyCode(stripBlanksAndUnderscores(v.get("currency"))));
                })

                // e r s ta t te t e S t e ue r n           E  U   R            3  .  5   3  9 , 5 8  U_ S_ D_ _ _ _ _ _ _ _  _ __ 3_ ._ _ 9_9 _ 9, _3 _ 7_   
                .section("currency", "amount").optional()
                .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n "
                                + "([\\s]+)?[A-Z\\s_]+ ([\\s_]+)?[\\.,\\d\\s_]+ "
                                + "([\\s]+)?(?<currency>[A-Z\\s_]+) ([\\s_]+)?(?<amount>[-\\.,\\d\\s_]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(stripBlanksAndUnderscores(v.get("amount"))));
                    t.setCurrencyCode(asCurrencyCode(stripBlanksAndUnderscores(v.get("currency"))));
                })

                //                           Kurswert                    : USD            1.573,75 
                //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR            1.275,95 
                //  er s ta t te t e S t e ue r n    E_ U_ R_ _ _ _ _ _ _ _ _  __  _ __ _10_,_8_ 4_
                .section("termCurrency", "exchangeRate", "baseCurrency", "currency", "gross").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<termCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .match("^.* (Umrechn\\. zum Dev\\. kurs|Umrechnung zum Devisenkurs) (?<exchangeRate>[\\.,\\d]+).* : (?<baseCurrency>[\\w]{3}).*$")
                .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e"
                                + "([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n "
                                + "([\\s]+)?(?<currency>[A-Z\\s_]+) "
                                + "([\\s_]+)?(?<gross>[\\.,\\d\\s_]+).*$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        PDFExchangeRate rate = asExchangeRate(v);
                        type.getCurrentContext().putType(asExchangeRate(v));

                        Money gross = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currency"))), asAmount(stripBlanksAndUnderscores(v.get("gross"))));
                        Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                        checkAndSetGrossUnit(gross, fxGross, t, type);
                    }
                })

                // e r s ta t te t e S t e ue r n            E  U   R           3  .  5   3  9 , 5 8  U_ S_ D_ _ _ _ _ _ _ _  _ __ 3_ ._ _ 9_9 _ 9, _3 _ 7_   
                // Umrechnungen zum Devisenkurs       1,129900                                            
                .section("fxCurrency", "fxGross", "currency", "gross", "exchangeRate").optional()
                .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<termCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n "
                                + "([\\s]+)?(?<fxCurrency>[A-Z\\s_]+) ([\\s_]+)?(?<fxGross>[\\.,\\d\\s_]+) "
                                + "([\\s]+)?(?<currency>[A-Z\\s_]+) ([\\s_]+)?(?<gross>[\\.,\\d\\s_]+)$")
                .match("^^Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        type.getCurrentContext().putType(asExchangeRate(v));

                        Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                        Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                        checkAndSetGrossUnit(gross, fxGross, t, type);
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
            Pattern pAccountingBillDate = Pattern.compile("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Kontoabschluss .*$");

            Boolean ForeignCurrencyAccount = false;

            // read the current context here
            for (int i = 0; i < lines.length; i++)
            {
                // Ihre aktuellen Salden IBAN Saldo in
                // EUR
                if (lines[i].compareTo("Ihre aktuellen Salden IBAN Saldo in") == 0)
                    context.put("currency", lines[i+1]);

                // Ihre aktuellen Salden Saldo in
                // IBAN EUR
                if ((lines[i].compareTo("Ihre aktuellen Salden Saldo in") == 0) && (lines[i+1].substring(0,4).compareTo("IBAN") == 0))
                    context.put("currency", lines[i+1].substring(5, 8));

                // Kontowährung EUR
                Matcher m = pBaseCurrency.matcher(lines[i]);
                if (m.matches())
                    context.put("currency", m.group(2));

                // Währungsanlagekonto (USD) DE31 2004 1155 1234 5678 05 +554,83 +487,76
                m = pForeignCurrencyAccount.matcher(lines[i]);
                if (m.matches())
                    context.put("foreignCurrency", m.group(2));

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

                m = pAccountingBillDate.matcher(lines[i]);
                if (m.matches())
                    context.put("accountingBillDate", m.group("date"));
            }
        });
        this.addDocumentTyp(type);

        Block removalBlock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Konto.bertrag"
                        + "|.bertrag"
                        + "|Lastschrift"
                        + "|Visa\\-Umsatz"
                        + "|Auszahlung"
                        + "|Barauszahlung"
                        + "|Kartenverf.gun"
                        + "|Guthaben.bertr"
                        + "|Wechselgeld\\-).* "
                        + "\\-[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(3);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("note1", "note2", "amount", "date")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note1>Konto.bertrag"
                                        + "|.bertrag"
                                        + "|Lastschrift"
                                        + "|Visa\\-Umsatz"
                                        + "|Auszahlung"
                                        + "|Barauszahlung"
                                        + "|Kartenverf.gun"
                                        + "|Guthaben.bertr"
                                        + "|Wechselgeld\\-)"
                                        + "(?<note2>.*) "
                                        + "\\-(?<amount>[\\.,\\d]+)$")
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
                            
                            // Formatting some notes
                            if (v.get("note1").startsWith("Kartenverfügun"))
                                v.put("note", "Kartenverfügung Kartenzahlung");
                            else if (v.get("note2").matches("^(?i:(.* )?Wechselgeld\\-.*)$"))
                                v.put("note", "Wechselgeld-Sparen");
                            else if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Girokonto)$"))
                                v.put("note", "Übertrag auf Girokonto");
                            else if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Tagesgeld PLUS\\-Konto)$"))
                                v.put("note", "Übertrag auf Tagesgeld PLUS-Konto");
                            else if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Visa\\-Karte)$"))
                                v.put("note", "Übertrag auf Visa-Karte");
                            else
                                v.put("note", v.get("note1"));

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block depositBlock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Konto.bertrag"
                        + "|.bertrag"
                        + "|Guthaben.bertr"
                        + "|Gutschrift"
                        + "|Bar"
                        + "|Visa\\-Kartenabre"
                        + "|Korrektur Barauszahlung).* "
                        + "\\+[\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(3);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("note1", "note2", "amount", "date")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note1>Konto.bertrag"
                                        + "|.bertrag"
                                        + "|Guthaben.bertr"
                                        + "|Gutschrift"
                                        + "|Bar"
                                        + "|Visa\\-Kartenabre"
                                        + "|Korrektur Barauszahlung)"
                                        + "(?<note2>.*) "
                                        + "\\+(?<amount>[\\.,\\d]+)$")
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

                            // Formatting some notes
                            if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Girokonto)$"))
                                v.put("note", "Übertrag auf Girokonto");
                            else if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Tagesgeld PLUS\\-Konto)$"))
                                v.put("note", "Übertrag auf Tagesgeld PLUS-Konto");
                            else if (v.get("note2").matches("^(?i:(.* )?Uebertrag auf Visa\\-Karte)$"))
                                v.put("note", "Übertrag auf Visa-Karte");
                            else if (v.get("note2").matches("^(?i:(.* )?Bargeldeinzahlung Karte .*)$"))
                                v.put("note", "Bargeldeinzahlung Karte");
                            else if (v.get("note2").matches("^(?i:(.* )?Gutschrift aus Bonus\\-Sparen)$"))
                                v.put("note", "Gutschrift aus Bonus-Sparen");
                            else if (v.get("note2").matches("^(?i:(.* )?Gutschr\\. Wechselgeld\\-Sparen)$"))
                                v.put("note", "Gutschrift Wechselgeld-Sparen");
                            else if (v.get("note1").matches("^(?i:Visa\\-Kartenabre.*)$"))
                                v.put("note", "Visa-Kartenabrechnung");
                            else
                                v.put("note", v.get("note1"));

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Geb.hren\\/Spesen"
                        + "|Geb.hr Barauszahlung"
                        + "|Entgelte"
                        + "|Auslandsentgelt).* "
                        + "\\-[\\.,\\d]+$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.FEES);
                            return entry;
                        })

                        .section("date", "amount", "note")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note>Geb.hren\\/Spesen"
                                        + "|Geb.hr Barauszahlung"
                                        + "|Entgelte"
                                        + "|Auslandsentgelt).* "
                                        + "\\-(?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block accountingBillFeeBlock = new Block("^Versandpauschale [\\.,\\d]+\\- [\\w]{3}$");
        type.addBlock(accountingBillFeeBlock);
        accountingBillFeeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.FEES);
                            return entry;
                        })

                        .section("note", "amount", "currency")
                        .match("^(?<note>Versandpauschale) (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(context.get("accountingBillDate")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
        
        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Kontoabschluss Abschluss Zinsen.* (\\+|\\-)[\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST);
                            return entry;
                        })

                        .section("note", "type", "amount", "date")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Kontoabschluss Abschluss Zinsen).* (?<type>(\\+|\\-))(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            // Is sign --> "-" change from INTEREST to INTEREST_CHARGE
                            if (v.get("type").equals("-"))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block taxesBlock = new Block("^(Kapitalertragsteuer|Solidarit.tszuschlag|Kirchensteuer) [\\.,\\d]+(\\+|\\-) [\\w]{3}$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("note", "amount", "type", "currency")
                        .match("^(?<note>(Kapitalertragsteuer|Solidarit.tszuschlag|Kirchensteuer)) (?<amount>[\\.,\\d]+)(?<type>(\\+|\\-)) (?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            // Is sign --> "-" change from TAXES to TAX_REFUND
                            if (v.get("type").equals("-"))
                                t.setType(AccountTransaction.Type.TAXES);

                            t.setDateTime(asDate(context.get("accountingBillDate")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                //                           Transaktionssteuer          : GBP              213,60 
                .section("tax", "currency").optional()
                .match("^.* Transaktionssteuer ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                //                           Provision                   : EUR               12,10 
                .section("currency", "fee").optional()
                .match("^.* Provision ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Gesamtprovision             : EUR                9,90 
                .section("currency", "fee").optional()
                .match("^.* Gesamtprovision ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Börsenplatzabhäng. Entgelt  : EUR                1,50 
                .section("currency", "fee").optional()
                .match("^.* B.rsenplatzabh.ng\\. Entgelt ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Umschreibeentgelt           : EUR                0,60
                .section("currency", "fee").optional()
                .match("^.* Umschreibeentgelt ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Abwickl.entgelt Clearstream : EUR                2,90
                .section("currency", "fee").optional()
                .match("^.* Abwickl.entgelt Clearstream ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Variable Börsenspesen       : EUR                3,00
                .section("currency", "fee").optional()
                .match("^.* Variable B.rsenspesen ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                  0,08000% Maklercourtage              : EUR                0,88- 
                .section("currency", "fee").optional()
                .match("^.* Maklercourtage ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                //                           Fremde Spesen               : USD               13,90 
                .section("currency", "fee").optional()
                .match("^.* Fremde Spesen ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                });
    }

    /**
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
                    if (!a1.getUnit(Type.TAX).isPresent() || a2.getUnit(Type.TAX).get().getAmount()
                                    .isGreaterOrEqualThan(a1.getUnit(Type.TAX).get().getAmount()))
                    {
                        // store potential gross unit
                        Optional<Unit> unitGross = a1.getUnit(Unit.Type.GROSS_VALUE);
                        if (unitGross.isPresent())
                            a2.addUnit(unitGross.get());

                        // combine notes and source
                        a2.setNote(concat(a2.getNote(), a1.getNote()));
                        a2.setSource(concat(a2.getSource(), a1.getSource()));

                        // remove self and own divTransaction
                        iterator.remove();
                        dividends.get(i.getDate()).get(i.getSecurity()).remove(ownIndex);
                    }
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

    private String concat(String first, String second)
    {
        if (first == null && second == null)
            return null;

        if (first != null && second == null)
            return first;

        return first == null ? second : first + "; " + second; //$NON-NLS-1$
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class ConsorsbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isjointaccount"; //$NON-NLS-1$

    BiConsumer<DocumentContext, String[]> isJointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("^(abzgl\\. Kapitalertragssteuer|KAPST) anteilig 50,00.*$"); //$NON-NLS-1$
        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.putBoolean(IS_JOINT_ACCOUNT, true);
                break;
            }
        }
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
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(?i)(Kauf"
                        + "|Bezug"
                        + "|Verkauf"
                        + "|VERK. TEIL\\-/BEZUGSR\\."
                        + "|VERKAUF KAPITALMA.*)", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^.*(?i)(Kauf"
                        + "|Bezug"
                        + "|Verkauf"
                        + "|VERK\\. TEIL\\-\\/BEZUGSR\\."
                        + "|VERKAUF KAPITALMA.*) ([\\s]+)?AM .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "VERKAUF" change from BUY to SELL
                .section("type").optional()
                .match("^.*(?i)(?<type>Verkauf"
                                + "|VERK\\. TEIL\\-\\/BEZUGSR\\."
                                + "|VERKAUF KAPITALMA.*) ([\\s]+)?AM .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("VERKAUF") 
                        || v.get("type").equals("Verkauf")
                        || v.get("type").equals("VERK. TEIL-/BEZUGSR.")
                        || v.get("type").equals("VERKAUF KAPITALMAßN.")
                        || v.get("type").equals("VERKAUF KAPITALMASSN."))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                .oneOf(
                                // @formatter:off
                                // COMS.-MSCI WORL.T.U.ETF I ETF110 LU0392494562
                                // Kurs 37,650000 EUR P.ST. NETTO  
                                // Preis pro Anteil 25,640000 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("name", "wkn", "isin", "currency")
                                        .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^(Kurs|Preis pro Anteil) [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // ST 15,75243 WKN: 625952
                                // GARTMORE - CONT. EUROP. FUND
                                // ACTIONS NOM. A O.N.
                                // KURS 4,877300 P.ST. NETTO
                                // KURSWERT EUR 76,83
                                // @formatter:on
                                section -> section
                                        .attributes("wkn", "name", "nameContinued", "currency")
                                        .match("^ST [\\.,\\d]+ WKN: (?<wkn>[A-Z0-9]{6})$")
                                        .match("^(?<name>.*)$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^KURS [\\.,\\d]+ .*$")
                                        .match("^KURSWERT (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // ST 334,00000 WKN: A0MZBE
                                // AHOLD, KON. EO-,30
                                // Kurs 9,890000 EUR P.ST. FRANCO COURTAGE
                                // Kurswert EUR 3.303,26
                                // @formatter:on
                                section -> section
                                        .attributes("wkn", "name", "currency")
                                        .match("^ST [\\.,\\d]+ WKN: (?<wkn>[A-Z0-9]{6})$")
                                        .match("^(?<name>.*)$")
                                        .match("^Kurs [\\.,\\d]+ .*$")
                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                //       ST                        50,00000               WKN: 851144
                                //                 GENERAL ELECTRIC CO.
                                //                 SHARES DL -,06
                                //       KURSWERT                                      EUR               1.917,50
                                // @formatter:on
                                section -> section
                                        .attributes("wkn", "name", "nameContinued", "currency")
                                        .match("^[\\s]+ ST [\\s]+[\\.,\\d]+ [\\s]+WKN: (?<wkn>[A-Z0-9]{6}).*$")
                                        .match("^[\\s]+ (?<name>.*)$")
                                        .match("^[\\s]+ (?<nameContinued>.*)$")
                                        .match("^[\\s]+ KURSWERT [\\s]+(?<currency>[\\w]{3}) [\\s]+[\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // ST 11,87891 WKN: 625952
                                // GARTMORE-CONT. EUROP. A
                                // Preis pro Anteil 6,467700 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("wkn", "name", "currency")
                                        .match("^ST [\\.,\\d]+ WKN: (?<wkn>[A-Z0-9]{6})$")
                                        .match("^(?<name>.*)$")
                                        .match("^Preis pro Anteil [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // ST 132,80212
                // ST 15,75243 WKN: 625952
                // ST 1.000,00000 Letzte Fälligkeit 01.09.2021
                //       ST                        50,00000               WKN: 851144
                // @formatter:on
                .section("shares")
                .match("^([\\s]+)?ST ([\\s]+)?(?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // KAUF AM 15.01.2015  UM 08:13:35 MUENCHEN NR. 12345670.001
                // @formatter:on
                .section("time").optional()
                .match("^(?i)([\\s]+)?(Kauf"
                                + "|Bezug"
                                + "|Verkauf"
                                + "|VERK\\. TEIL\\-\\/BEZUGSR\\."
                                + "|VERKAUF KAPITALMA.*) .* UM (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // @formatter:off
                // Kauf AM 17.10.2005 IN SPARPLAN NR.2424880.001
                //              KAUF                 AM 18.09.2001 IN FRANKFURT          NR. 6201999.001
                // KAUF AM 15.01.2015  UM 08:13:35 MUENCHEN NR. 12345670.001
                // @formatter:on
                .section("date")
                .match("^(?i)([\\s]+)?(Kauf"
                                + "|Bezug"
                                + "|Verkauf"
                                + "|VERK\\. TEIL\\-\\/BEZUGSR\\."
                                + "|VERKAUF KAPITALMA.*) ([\\s]+)?AM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                .oneOf(
                                // @formatter:off
                                // Wert 13.05.2020 EUR 525,92
                                // Wert 19.01.2015 EUR 5.000,00
                                // WERT 20.05.2008 EUR 3.290,05
                                //       WERT  20.09.2001                              EUR               1.928,74
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(?i)([\\s]+)?Wert ([\\s]+)?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // EUR 75,00
                                // WERT 21.10.2005
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                                        .match("^(?i)([\\s]+)?WERT ([\\s]+)?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}([\\s]+)?$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // zulasten Konto-Nr. 0860101888 7.659,37 EUR
                                // ZU LASTEN  KONTO-NR.   546956980              EUR               1.928,74
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(?i)(zulasten|ZU LASTEN|zugunsten|ZU GUNSTEN) ([\\s]+)?Konto\\-Nr\\. ([\\s]+)?[\\d]+ ([\\s]+)?(?<amount>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)?$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Kurswert USD 540,00
                                // umger. zum Devisenkurs USD 1,077900 EUR 500,97
                                //       UMGER. ZUM DEVISENKURS  USD        0,882100   EUR                  56,68
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "gross", "exchangeRate", "baseCurrency", "termCurrency")
                                        .match("^Kurswert (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$")
                                        .match("^([\\s]+)?(umger\\. zum Devisenkurs|UMGER\\. ZUM DEVISENKURS) ([\\s]+)?(?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?(?<baseCurrency>[\\w]{3}) ([\\s]+)?(?<gross>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("baseCurrency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Kurswert 343,75 USD
                                // Kurswert in EUR 292,80 EUR
                                // Devisenkurs 1,174000 EUR / USD
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "gross", "currency", "baseCurrency", "termCurrency", "exchangeRate")
                                        .match("^Kurswert (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Kurswert in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Nettoinventarwert 52,84 USD
                                // Kurswert in EUR 49,50 EUR
                                // Devisenkurs 1,067400 EUR / USD
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "gross", "currency", "baseCurrency", "termCurrency", "exchangeRate")
                                        .match("^Nettoinventarwert (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Kurswert in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Kurswert 1.020.000,00 JPY
                                // Börsenplatzgebühr 7.760,00 JPY
                                // Devisenkurs 141,090000 EUR / JPY
                                // Zwischensumme 7.284,43 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "baseCurrency", "termCurrency", "exchangeRate")
                                        .match("^Kurswert (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(t.getAccountTransaction().getCurrencyCode(), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                // @formatter:off
                // Limitkurs  5,500000 EUR
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Limitkurs .*)")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // Ursprungs-WKN 549532
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ursprungs-WKN .*)")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(?i)(Dividendengutschrift|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(?i)(Dividendengutschrift|Ertragsgutschrift).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Storno wegen geänderten steuerrelevanten Daten. Neuabrechnung folgt.
                .section("type").optional()
                .match("^(?<type>Storno) .*$")
                .assign((t, v) -> v.getTransactionContext().put(FAILURE,
                                Messages.MsgErrorOrderCancellationUnsupported))

                .oneOf(
                                // @formatter:off
                                // ST                    1.370,00000          WKN:  ETF110                 
                                //            COMS.-MSCI WORL.T.U.ETF I                                    
                                //            Namens-Aktien o.N.                                           
                                // ZINS-/DIVIDENDENSATZ            1,200000  EUR SCHLUSSTAG PER 07.05.2015
                                // @formatter:on
                                section -> section
                                        .attributes("wkn", "name", "nameContinued", "currency")
                                        .match("^ST ([\\s]+)?[\\.,\\d]+ ([\\s]+)?WKN: ([\\s]+)?(?<wkn>[A-Z0-9]{6}).*$")
                                        .match("^(?<name>.*)$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^(?i)(ZINS-\\/DIVIDENDENSATZ|ERTRAGSAUSSCHUETTUNG P\\. ST\\.) .* ([\\s]+)?(?<currency>[\\w]{3}) SCHLUSSTAG PER [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // OMNICOM GROUP INC. Registered Shares DL -,15 871706 US6819191064
                                // 25 Stück
                                // Dividende pro Stück 0,60 USD Schlusstag 17.12.2017
                                // @formatter:on
                                section -> section
                                        .attributes("name", "wkn", "isin", "currency")
                                        .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^(Steuerfreie )?(Dividende pro St.ck|Ertragsaussch.ttung je Anteil) [\\.,\\d]+ (?<currency>[\\w]{3}) Schlusstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                .oneOf(
                                // @formatter:off
                                // ST                    1.370,00000          WKN:  ETF110
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^ST ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?WKN: .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // 25 Stück
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) St.ck$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // WERT 11.01.2016
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^(?i)WERT (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Valuta 09.01.2018 BIC CSDBDE71XXX
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^(?i)Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // @formatter:off
                                // WERT 08.05.2015                               EUR                326,90
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(?i)WERT (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Netto zugunsten IBAN DE00 0000 0000 0000 0000 00 9,34 EUR
                                // Netto in USD zugunsten IBAN DE12 3456 3456 3456 3456 78 6,46 USD
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Netto( in [\\w]{3})? (zugunsten|zulasten) .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // UMGER.ZUM DEV.-KURS                 1,093000  EUR                285,60
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(?i)UMGER\\.ZUM DEV\\.\\-KURS ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // BRUTTO                                        USD                180,00 
                                // UMGER.ZUM DEV.-KURS                 1,104300  EUR                138,55
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^BRUTTO ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                                        .match("^UMGER\\.ZUM DEV\\.\\-KURS ([\\s]+)?(?<exchangeRate>[\\.,\\d]+) ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                                        .assign((t, v) -> {
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Brutto in USD 15,00 USD
                                // Devisenkurs 1,195900 USD / EUR
                                // Brutto in EUR 12,54 EUR
                                // Netto zugunsten IBAN DE00 0000 0000 0000 0000 00 9,34 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "gross", "currency", "baseCurrency", "termCurrency")
                                        .match("^Brutto in [\\w]{3} (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3})$")
                                        .match("^Brutto in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("Netto zugunsten IBAN .* [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Brutto in USD 10,00 USD
                                // Devisenkurs 1,12000 USD / EUR
                                // Brutto in EUR 8,93 EUR
                                // Netto in EUR 5,77 EUR
                                // Netto in USD zugunsten IBAN DE12 3456 3456 3456 3456 78 6,46 USD
                                // @formatter:on
                                section -> section
                                        .attributes("gross", "currency", "exchangeRate", "fxGross", "fxCurrency", "termCurrency", "baseCurrency")
                                        .match("^Brutto in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) [\\w]{3} \\/ [\\w]{3}$")
                                        .match("^Brutto in [\\w]{3} (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("Netto in (?<baseCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                                        .match("Netto in (?<termCurrency>[\\w]{3}) zugunsten IBAN .* [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap((t, ctx) -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                    {
                        TransactionItem item = new TransactionItem(t);
                        item.setFailureMessage(ctx.getString(FAILURE));
                        return item;
                    }
                    return null;
                });

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
                // @formatter:off
                // Wertpapierbezeichnung WKN ISIN
                // Lang & Schwarz AG LS846N DE000LS846N5
                // @formatter:on
                .section("wkn", "isin", "name", "name1")
                .find("Wertpapierbezeichnung WKN ISIN")
                .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Einheit"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Stück 1.000 20.05.2021
                // @formatter:on
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) [\\d]+.[\\d]+.[\\d]{4}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Einlösung zu 0,001 EUR Schlusstag 20.05.2021
                // @formatter:on
                .section("date")
                .match("^Einl.sung .* Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // Netto zugunsten IBAN DExxxxxxxxxxxxxxxxxxxx 1,00 EUR
                // @formatter:on
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

                // @formatter:off
                // Wertpapierbezeichnung WKN ISIN
                // L&G-L&G R.Gbl Robot.Autom.UETF Bearer Shares (Dt. Zert.) o.N. A12GJD DE000A12GJD2
                // @formatter:on
                .section("wkn", "isin", "name")
                .match("^Wertpapierbezeichnung WKN ISIN$")
                .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Bestand
                // 106 Stück
                // @formatter:on
                .section("shares")
                .match("^Bestand$")
                .match("^(?<shares>[\\.,\\d]+) St.ck$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valuta 02.01.2020 BIC CSDBDE71XXX
                // @formatter:on
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Netto zulasten IBAN DE73 7603 0080 0123 4567 89 0,73 EUR
                // Valuta 02.01.2020 BIC CSDBDE71XXX
                // @formatter:on
                .section("currency", "amount")
                .match("^Netto zulasten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
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

        Block block = new Block("^([\\s]+)?Erstattung\\/Belastung \\(\\-\\) von Steuern$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);

                    // Set currency
                    t.setCurrencyCode(CurrencyUnit.EUR);
                    return t;
                })

                // @formatter:off
                // Den Steuerausgleich buchen wir mit Wertstellung 10.07.2017
                // @formatter:on
                .section("date")
                .match("([\\s]+)?Den Steuerausgleich buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*")
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

                .section("amount", "sign")
                .find("^([\\s]+)?Erstattung\\/Belastung \\(\\-\\) von Steuern.*")
                .match("([\\s]+)?(?<amount>[\\.,\\d]+)(?<sign>(\\-)?).*")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));

                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                .wrap(t -> {
                    TransactionItem item = new TransactionItem(t);
                    if (t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                }));
    }

    @SuppressWarnings("nls")
    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^Kontow.hrung (?<currency>[\\w]{3})$");
            Pattern pYear = Pattern.compile("^Datum [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{2}) .*$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));
            }
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^(GUTSCHRIFT|D\\-GUTSCHRIFT|EURO\\-UEBERW\\.)( .*)? [\\d]{2}\\.[\\d]{2}\\. [\\d]+ [\\d]{2}\\.[\\d]{2}\\. [\\.,\\d]+\\+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                // @formatter:off
                // GUTSCHRIFT NR.99999999992 21.08. 8401 21.08. 6.500,00+
                // D-GUTSCHRIFT NR.99999999999 03.09. 7998 03.09. 4.900,00+
                // EURO-UEBERW. 11.08. 8420 11.08. 1.000,00+
                // @formatter:on
                .section("note", "date", "amount")
                .match("^(?<note>GUTSCHRIFT|D\\-GUTSCHRIFT|EURO\\-UEBERW\\.)( .*)? [\\d]{2}\\.[\\d]{2}\\. [\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.,\\d]+)\\+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date") + context.get("year")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));

                    // Formatting some notes
                    if (v.get("note").equals("GUTSCHRIFT"))
                        v.put("note", "Gutschrift");

                    if (v.get("note").equals("D-GUTSCHRIFT"))
                        v.put("note", "D-Gutschrift");

                    if (v.get("note").equals("EURO-UEBERW."))
                        v.put("note", "Euro-Überweisung");

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block removalBlock = new Block("^(UEBERWEISUNG|EURO\\-UEBERW\\.|DAUERAUFTRAG)( .*)? [\\d]{2}\\.[\\d]{2}\\. [\\d]+ [\\d]{2}\\.[\\d]{2}\\. [\\.,\\d]+\\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                // @formatter:off
                // UEBERWEISUNG NR.99999999991 21.08. 8401 21.08. 25.308,00-
                // EURO-UEBERW. 21.12. 8420 21.12. 6.000,00-
                // @formatter:on
                .section("note", "date", "amount")
                .match("^(?<note>(UEBERWEISUNG|EURO\\-UEBERW\\.|DAUERAUFTRAG))( .*)? [\\d]{2}\\.[\\d]{2}\\. [\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date") + context.get("year")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));

                    // Formatting some notes
                    if (v.get("note").equals("UEBERWEISUNG"))
                        v.put("note", "Überweisung");

                    if (v.get("note").equals("EURO-UEBERW."))
                        v.put("note", "Euro-Überweisung");

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feeBlock = new Block("^ABSCHLUSS [\\d]{2}\\.[\\d]{2}\\. [\\d]+ [\\d]{2}\\.[\\d]{2}\\. [\\.,\\d]+\\-$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                // @formatter:off
                // ABSCHLUSS 31.12. 8800 31.12. 1,22-
                // @formatter:on
                .section("note", "date", "amount")
                .match("^(?<note>ABSCHLUSS) [\\d]{2}\\.[\\d]{2}\\. [\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.) (?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date") + context.get("year")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));

                    // Formatting some notes
                    if (v.get("note").equals("ABSCHLUSS"))
                        v.put("note", "Abschluss");

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Franzoesische Finanztransaktionssteuer 0,30% EUR 0,07
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Franzoesische Finanztransaktionssteuer [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // QUST 15,00000  %   EUR                 24,45  USD                 27,00
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^QUST [\\.,\\d]+ ([\\s]+)?% ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<withHoldingTax>[\\.,\\d]+) .*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // abzgl. Quellensteuer 15,00 % von 29,60 EUR 4,44 EUR
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^abzgl\\. Quellensteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Anrechenbare Quellensteuer 15,00 % von 29,60 EUR 4,44 EUR
                // @formatter:on
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // @formatter:off
                // Kapitalertragsteuer (Account)
                // KAPST                                 25,00 % EUR                111,00
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KAPST ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragsteuer 25,00 % von 5,02 EUR 1,26 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kapitalertragsteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragssteuer 25,00% 97,47 EUR 24,37 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kapitalertragssteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer (Account)
                // abzgl. Kapitalertragsteuer 2,06 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kapitalertragsteuer (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer (Account)
                // KAPST 24,45% EUR 198,08
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KAPST [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalerstragsteuer (Joint Account)
                // KAPST anteilig 50,00% 25,00% EUR 0,50
                // KAPST anteilig 50,00% 25,00% EUR 0,50
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^KAPST anteilig [\\.,\\d]+% [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KAPST anteilig [\\.,\\d]+% [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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

                // @formatter:off
                // Kapitalerstragsteuer (Joint Account)
                // abzgl. Kapitalertragssteuer anteilig 50,00% 25,00% 208,72 EUR 52,18 EUR
                // abzgl. Kapitalertragssteuer anteilig 50,00% 25,00% 208,72 EUR 52,18 EUR
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl\\. Kapitalertragssteuer anteilig [\\.,\\d]+% [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl\\. Kapitalertragssteuer anteilig [\\.,\\d]+% [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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

                // @formatter:off
                // Solitaritätszuschlag (Account)
                // SOLZ                                   5,50 % EUR                  6,10
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^SOLZ ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 5,50 % von 1,26 EUR 0,06 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Solidarit.tszuschlag [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 5,50% 24,37 EUR 1,34 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Account)
                // abzgl. Solidaritätszuschlag 0,10 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Solidarit.tszuschlag (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Account)
                // SOLZ 5,50% EUR 10,89
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^SOLZ [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solitaritätszuschlag (Joint Account)
                // SOLZ 5,50% EUR 0,02
                // SOLZ 5,50% EUR 0,02
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^SOLZ [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^SOLZ [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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

                // @formatter:off
                // Solidaritätszuschlag (Joint Account)
                // abzgl. Solidaritätszuschlag 5,50% 52,18 EUR 2,86 EUR
                // abzgl. Solidaritätszuschlag 5,50% 52,18 EUR 2,86 EUR
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl\\. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl\\. Solidarit.tszuschlag [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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

                // @formatter:off
                // Kirchensteuer (Account)
                // KIST                                   5,50 % EUR                  6,10
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KIST ([\\s]+)?[\\.,\\d]+ % ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer (Account)
                // abzgl. Kirchensteuer 9,00 % von 8,93 EUR 0,80 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kirchensteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer (Account)
                // abzgl. Kirchensteuer 5,50% 24,37 EUR 1,34 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer (Account)
                // KIST 9,00% EUR 17,82
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KIST [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer (Joint Account)
                // KIST 9,00% EUR 1,00
                // KIST 9,00% EUR 1,00
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^KIST [\\.,\\d]+% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KIST [\\.,\\d]+% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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

                // @formatter:off
                // Kirchensteuer (Joint Account)
                // abzgl. Kirchensteuer 9% 52,18 EUR 2,86 EUR
                // abzgl. Kirchensteuer 9% 52,18 EUR 2,86 EUR
                // @formatter:on
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^abzgl\\. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+) (?<currency1>[\\w]{3})$")
                .match("^abzgl\\. Kirchensteuer [\\.,\\d]+% [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+) (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
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
                // @formatter:off
                // Börsenplatzgebühr EUR 2,95
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?B.rsenplatzgeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Börsenplatzgebühr 2,50 EUR
                // abzgl. Börsenplatzgebühr 1,50 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?B.rsenplatzgeb.hr (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Handelsentgelt EUR 3,00
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?Handelsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // abzgl. Handelsentgelt 2,61 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Handelsentgelt (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision EUR 5,00
                // PROVISION EUR 8,26
                //       PROVISION                                     EUR                   5,11
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)([\\s]+)?(abzgl\\. )?Provision ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision 13,54 EUR
                // abzgl. Provision 5,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Provision (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Grundgebühr EUR 4,95
                // GRUNDGEBUEHR EUR 4,95
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?(Grundgeb.hr|GRUNDGEBUEHR) (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Grundgebühr 3,95 EUR
                // abzgl. Grundgebühr 4,95 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Grundgeb.hr (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Consorsbank Ausgabegeb.hr 2,50% EUR 0,61
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?Consorsbank Ausgabegeb.hr [\\.,\\d]+% (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Transaktionsentgelt EUR 11,54
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Transaktionsentgelt 5,96 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Transaktionsentgelt (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                //       COURTAGE                                      EUR                   1,53
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)([\\s]+)?COURTAGE ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // BONIFIKAT. 2,38100 % EUR 1,83
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)BONIFIKAT\\. [\\.,\\d]+ % (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                //       EIG.SPESEN                                    EUR                   4,60
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)([\\s]+)?EIG\\.SPESEN ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // abz. CortalConsors Discount 2,38100 % EUR 1,83
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)abz\\. CortalConsors Discount [\\.,\\d]+ % (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Eig. Spesen EUR 1,95
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?Eig\\. Spesen (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Eig. Spesen 1,95 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Eig\\. Spesen (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // FREMDE SPESEN                                 USD                  1,91
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^(?i)(abzgl\\. )?FREMDE SPESEN [\\s]+(?<currency>[\\w]{3}) [\\s]+(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // abzgl. Fremde Spesen 0,07 USD
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)(abzgl\\. )?Fremde Spesen (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Courtage EUR 2,71
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)Courtage (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Handelsplatzkosten 1,95 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)Handelsplatzkosten (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Consorsbank Ausgabegebühr 1,00% 0,50 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^(?i)Consorsbank Ausgabegeb.hr [\\.,\\d]+% (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FinTechGroupBankPDFExtractor extends AbstractPDFExtractor
{

    public FinTechGroupBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("biw AG"); //$NON-NLS-1$
        addBankIdentifier("FinTech Group Bank AG"); //$NON-NLS-1$
        addBankIdentifier("flatex Bank AG"); //$NON-NLS-1$
        addBankIdentifier("flatexDEGIRO Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addSummaryStatementBuySellTransaction();
        addDividendeTransaction();
        addDividendeWithNegativeAmountTransaction();
        addAccountStatementTransaction();
        addTransferOutTransaction();
        addTransferInTransaction();
        addAdvanceTaxTransaction();
    }

    @Override
    public String getLabel()
    {
        return "flatexDEGIRO Bank AG / FinTech Group Bank AG / biw AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung (Kauf|Verkauf)( Fonds\\/Zertifikate)?");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^.* Auftragsdatum .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Storno Wertpapierabrechnung Kauf Fonds/Zertifikate
                // Stornierung Wertpapierabrechnung Kauf Fonds
                .section().optional()
                .match("^(Storno|Stornierung) Wertpapierabrechnung.*$")
                .assign((t, v) -> {
                    throw new IllegalArgumentException(Messages.MsgErrorOrderCancellationUnsupported);
                })

                // Nr.121625906/1     Kauf        IS C.MSCI EMIMI U.ETF DLA (IE00BKM4GZ66/A111X9)
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Nr.76443716/1  Verkauf           UBS AG LONDON 14/16 RWE (DE000US9RGR9/US9RGR)
                // Kurs           29,0000 %               Kurswert       EUR             7.250,00
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([\\s]+)? ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Ausgeführt     25.000,00000 EUR
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                .section("shares", "notation")
                .match("^Ausgef.hrt ([:\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("St."))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // An den  Ausführungszeit   13:59 Häusern 5 
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("time").optional()
                .match("^.* Ausf.hrungszeit ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Max Mustermann Schlusstag        03.12.2015o
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("date")
                .match("^.* (Handelstag|Schlusstag) ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })
                
                // Devisenkurs   :        1,000000         Eigene Spesen :               0,00 EUR
                .section("exchangeRate").optional()
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .oneOf(
                                // Endbetrag      EUR               -50,30
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                //        Endbetrag                   -52,50 EUR
                                // Endbetrag     :            -760,09 EUR
                                // Gewinn/Verlust -267,59 EUR             Endbetrag      EUR            16.508,16
                                //                                        Endbetrag      EUR                 0,95
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^(.* )?Endbetrag ([:\\s]+)?(\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                /***
                 * If the taxes are negative, 
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If the currency of the tax differs from the amount, 
                 * it will be converted and reset.
                 * 
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addTaxReturnBlock(type);
                 */
                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundSameCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundDifferentCurrency)

                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundDifferentCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundDifferentCurrency)

                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundSameCurrency)
                
                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundDifferentCurrency)

                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundSameCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundDifferentCurrency)

                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundSameCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundDifferentCurrency)

                //                                     ***Einbeh. SichSt EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundSameCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                //                                     ***Einbeh. SichSt EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+)$")
                .assign(this::extractTaxRefundDifferentCurrency)

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void extractTaxRefundDifferentCurrency(BuySellEntry t, Map<String, String> v)
    {
        if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
        {
            Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
            exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

            taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(),
                            BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

            v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));
            addTaxRefund(t, v);
        }
    }

    private void extractTaxRefundSameCurrency(BuySellEntry t, Map<String, String> v)
    {
        if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
        {
            addTaxRefund(t, v);
        }
    }

    private void addTaxRefund(BuySellEntry t, Map<String, String> v)
    {
        // For SELL transaction we need to subtract refunded taxes.
        // For BUY transactions we need to add refunded taxes.
        int negative = t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.SELL ? -1 : 1;
        t.setAmount(t.getPortfolioTransaction().getAmount() + negative * asAmount(v.get("taxRefund")));
    }

    private void addSummaryStatementBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung \\(Wertpapierkauf\\/-verkauf\\)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Nr.60796942/1  Kauf               BAYWA AG VINK.NA. O.N. (DE0005194062/519406)
                // Kurs         : 39,2480 EUR             Kurswert       :           5.887,20 EUR
                .section("name", "isin", "wkn", "currency")
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 4.550,00 St.            Schlusstag    :  01.11.2017, 14:41 Uhr
                .section("shares")
                .match("^davon ausgef\\.([:\\s]+)? (?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                .section("date", "time")
                .match("^.* Schlusstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), (?<time>[\\d]{2}:[\\d]{2}{2}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Devisenkurs   : 1,195010                Provision     :               5,90 EUR
                .section("exchangeRate").optional()
                .match("^Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                .section("amount", "currency")
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?(\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                /***
                 * If the taxes are negative, 
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If the currency of the tax differs from the amount, 
                 * it will be converted and reset.
                 * 
                 * Example:
                 * **Einbeh. Steuer:           0,84 EUR
                 * Endbetrag     :             955,98 USD
                 * 
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addSummaryStatementTaxReturnBlock(type);
                 */
                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundSameCurrency)

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([\\s]+)?: (?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign(this::extractTaxRefundDifferentCurrency)

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addSummaryStatementTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift)( .*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);

                /***
                 * If we have multiple entries in the document,
                 * with taxes and tax refunds,
                 * then the "negative" flag must be removed.
                 */
                type.getCurrentContext().remove("negative");

                return entry;
            });

        pdfTransaction
                // Nr.716759781                   HANN.RUECK SE NA O.N.     (DE0008402215/840221)
                // Extag           :  08.05.2014          Bruttodividende
                // Zahlungstag     :  08.05.2014          pro Stück       :       3,0000 EUR
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+ ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\).*$")
                .find(".* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)")
                .match("^.* ([\\s]+)?pro St.ck([\\s]+)?: ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Nr.111111111                   ISH.FOOBAR 12345666 x.EFT (DE1234567890/AB1234)
                // Zinstermin      :  28.04.2016          Zinsbetrag      :        73,75 EUR
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+ ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\).*$")
                .match("^.* ([\\s]+)?Zinsbetrag([\\s]+)?: ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.             :         360
                .section("shares")
                .match("^(St\\.|St\\.\\/Nominale) ([\\s]+)?: ([\\s]+)?(?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .section("date")
                .match("Valuta ([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                //                                        Endbetrag       :       795,15 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,

                                /***
                                 * If we have a negative amount, 
                                 * we first post the dividends received 
                                 * and then the tax charge
                                 */
                                // Extag           :    07.10.2021        Bruttothesaurierung:        78,81 USD
                                // Zuflusstag      :    08.10.2021        Devisenkurs        :         1,156200
                                //                                        Endbetrag          :       -15,24 EUR
                                section -> section
                                        .attributes("fxAmount", "fxCurrency", "exchangeRate", "currency")
                                        .match("^.* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)([\\s]+)?: ([\\s]+)?(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^(.* )?Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?\\-[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negative", "X");

                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                            if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                                            {
                                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                                            }
                                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                                            // Calculate gross amount in account currency
                                            Money gross = Money.of(v.get("currency"), 
                                                            Math.round(fxAmount.getAmount() / exchangeRate.doubleValue()));

                                            // Set amount
                                            t.setAmount(gross.getAmount());

                                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                            {
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                                RoundingMode.HALF_DOWN);

                                                // check, if forex currency is transaction
                                                // currency or not and swap amount, if necessary
                                                Unit grossValue;
                                                if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                                                {
                                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, gross, fxAmount, inverseRate);
                                                }
                                                else
                                                {
                                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, fxAmount, gross, inverseRate);
                                                }
                                                t.addUnit(grossValue);
                                            }
                                        })
                                ,
                                // Extag           :    20.01.2020        Bruttothesaurierung:        23,19 EUR
                                //                                        Endbetrag          :        -8,26 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)([\\s]+)?: ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?\\-[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negative", "X");

                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Extag : 08.08.2017 Bruttodividende : 26,25 USD
                // Devisenkurs     :    1,180800         *Einbeh. Steuer  :         1,11 EUR
                .section("fxAmount", "fxCurrency", "exchangeRate").optional()
                .match("^(.* )?(Bruttoaussch.ttung|Bruttodividende|Bruttothesaurierung)([\\s]+)?: ([\\s]+)?(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^(.* )?Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
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
                            Money fxAmountGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            BigDecimal amount = BigDecimal.valueOf(fxAmountGross.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN);

                            Money fxAmount = Money.of(t.getCurrencyCode(), amount.longValue());
                            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, fxAmount, fxAmountGross,
                                            inverseRate));
                        }
                    }
                })

                //   unter der Transaktion-Nr.: 132465978
                .section("note").optional()
                .match("^.* (?<note>Transaktion-Nr\\.: [\\d]+)(.*)?$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDividendeWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("Ertragsmitteilung \\- (aussch.ttender\\/teil)?thesaurierender( transparenter)? Fonds");
        this.addDocumentTyp(type);

        Block block = new Block("^Ertragsmitteilung \\- (aussch.ttender\\/teil)?thesaurierender( transparenter)? Fonds$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.TAXES);

                /***
                 * If we have multiple entries in the document,
                 * with taxes and tax refunds,
                 * then the "negative" flag must be removed.
                 */
                type.getCurrentContext().remove("negative");

                return entry;
            });

        pdfTransaction
                // Nr.1784953069                  PICTET-GL.MEGAT.SEL.P EO  (LU0386882277/A0RLJD)
                // St.             :          10,0        Bruttothesaurierung
                //                                        pro Stück          :       2,3189 EUR
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+ ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\).*$")
                .find(".* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)")
                .match("^.* ([\\s]+)?pro St.ck([\\s]+)?: ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.             :         168,9        Bruttothesaurierung
                .section("shares")
                .match("^(St\\.|St\\.\\/Nominale) ([\\s]+)?: ([\\s]+)?(?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .section("date")
                .match("^Valuta ([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                //                                       Endbetrag          :        -8,26 EUR
                .section("amount", "currency").optional()
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?\\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Extag           :    07.10.2021        Bruttothesaurierung:        78,81 USD
                // Zuflusstag      :    08.10.2021        Devisenkurs        :         1,156200
                //                                        Endbetrag          :       -15,24 EUR
                .section("fxCurrency", "exchangeRate", "currency", "amount").optional()
                .match("^.* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)([\\s]+)?: ([\\s]+)?[\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .match("^(.* )?Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?\\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getSecurity().getCurrencyCode().equals(forex))
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

                        t.addUnit(grossValue);
                    }
                })

                //   unter der Transaktion-Nr.: 132465978
                .section("note").optional()
                .match("^.* (?<note>Transaktion-Nr\\.: [\\d]+)(.*)?$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("^Kontoauszug Nr: ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4}).*$");
            Pattern pCurrency = Pattern.compile("^Kontow.hrung: ([:\\s]+)?(?<currency>[\\w]{3})$");

            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group("year"));
                }

                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        // 29.01.     29.01.  Überweisung                                       1.100,00+
        Block block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?.berweisung ([\\s]+)?[\\-\\.,\\d]+[\\+|\\-]$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("date", "note", "amount", "sign")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>.berweisung) ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)(?<sign>[\\+|\\-])$")
                .assign((t, v) -> {
                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.REMOVAL);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // 01.10.     01.10.  EINZAHLUNG 4 FLATEX / 0/16765097                  2.000,00+
        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(EINZAHLUNG|AUSZAHLUNG) .* ([\\s]+)?+[\\-\\.,\\d]+[\\+|\\-]$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("date", "note", "amount", "sign")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>(EINZAHLUNG|AUSZAHLUNG)) .* ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)(?<sign>[\\+|\\-])$")
                .assign((t, v) -> {
                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.REMOVAL);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // 19.11.     19.11.  R-Transaktion                                       -53,00-
        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+R-Transaktion  ([\\s]+)?+[\\-\\.,\\d]+\\-$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                .section("date", "note", "amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>R-Transaktion)  ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // Added "Lastschrift" as DEPOSIT option
        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+Lastschrift ([\\s]+)?+[\\-\\.,\\d]+[\\+|\\-]$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("date", "note", "amount", "sign")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>Lastschrift) ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)(?<sign>[\\+|\\-])$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> new TransactionItem(t)));

        // 19.07.     20.07.  Depotgebühren 01.04.2020 - 30.04.2020,                0,26-
        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+Depotgeb.hren ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\- [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, ([\\s]+)?+[\\-\\.,\\d]+\\-$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("date", "note", "amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>Depotgeb.hren[ ]+[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\- [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // 30.12.     31.12.  Zinsabschluss   01.10.2014 - 31.12.2014               7,89+
        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+Zinsabschluss .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    return t;
                })

                .section("date", "note", "amount", "sign")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>Zinsabschluss ([\\s]+)?+([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\- ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) ([\\s]+)?+(?<amount>[\\-\\.,\\d]+)(?<sign>[\\+|\\-])$")
                .assign((t, v) -> {
                    // Is sign --> "+" change from INTEREST_CHARGE to INTEREST
                    if (v.get("sign").equals("+"))
                    {
                        t.setType(AccountTransaction.Type.INTEREST);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        block = new Block("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+Steuertopfoptimierung .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                .section("date", "note", "amount", "sign")
                .match("^[\\d]{2}\\.[\\d]{2}\\. ([\\s]+)?+(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\s]+)?+(?<note>Steuertopfoptimierung ([\\s]+)?+([\\d]{4})) ([\\s]+)?+(?<amount>[\\.,\\d\\-]+)(?<sign>[\\+|\\-])$")
                .assign((t, v) -> {
                    // Is sign --> "-" change from TAX_REFUND to TAXES
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.TAXES);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("date") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("date") + context.get("year")));
                    }

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTransferOutTransaction()
    {
        final DocumentType type = new DocumentType("(Depotausgang|Bestandsausbuchung|Gutschrifts- \\/ Belastungsanzeige)", (context, lines) -> {
            Pattern pDate = Pattern.compile("^([\\s]+)?Frankfurt( am Main)?, ([\\s]+)?(den )?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");

            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("date", m.group("date"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            String date = type.getCurrentContext().get("date");
            if (date == null) throw new IllegalArgumentException("document date is null, parsing must have failed");
            entry.setDate(asDate(date));
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Depotausgang|Bestandsausbuchung|Gutschrifts\\- \\/ Belastungsanzeige)(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Depotausgang                          COMMERZBANK INLINE09EO/SF (DE000CM31SV9)
                // Rückzahlungsgrund: Ablauf der Optionsfrist     Betrag/Stk: 10,000000000000 EUR
                // Bestandsausbuchung                       COMMERZBANK PUT10 EOLS (DE000CB81KN1)
                .section("name", "isin", "currency").optional()
                .match("^(Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Depotausgang                          COMS.-MSCI WORL.T.U.ETF I (LU0392494562)
                //                                          Geldgegenwert***:           0,00 EUR
                .section("name", "isin", "currency").optional()
                .match("^Depotausgang ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .match("^.* Geldgegenwert([:\\*\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                .section("wkn", "isin", "name", "shares", "currency").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>.*) ([\\s]+)?(?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*) ([\\s]+)?(?<shares>[\\.,\\d]+)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // St./Nominale      :      310,000000 St.  Bemessungs-
                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("shares", "notation").optional()
                .match("^Stk?\\.\\/Nominale([\\*\\s]+)?: ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3})(.*)$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !v.get("notation").toLowerCase().startsWith("st"))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // Fälligkeitstag   : 02.12.2009                  Letzter Handelstag:  20.11.2009
                // Fälligkeitstag                                                  25.06.2021
                .section("date").optional()
                .match("^F.lligkeitstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Verwahrart      : GS-Verwahrung        Geldgegenwert***:              0,20 EUR
                //                                           Geldgegenwert*  :         111,22 EUR
                // Geldgegenwert                                                       393,73 EUR
                .section("amount", "currency").optional()
                .match("^(.* )?Geldgegenwert([:\\*\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // **oben genannten Bestand haben wir wertlos ausgebucht
                .section().optional()
                .match("^.* Bestand haben wir wertlos ausgebucht.*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                    t.setAmount(0L);
                    t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                    t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                })

                /***
                 * If the taxes are negative,
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addTransferInOutTaxReturnBlock(type);
                 */
                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:           -382,12 EUR
                //                                           Einbeh. Steuer**:         -10,00 EUR
                .section("taxRefund").optional()
                .match("^.* Einbeh\\. Steuer[\\*\\s]+: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                })

                // Einbeh. Steuer**                                                    -88,53 EUR
                .section("taxRefund").optional()
                .match("^Einbeh\\. Steuer[\\*\\s]+ \\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTransferInOutTaxReturnBlock(type);
    }

    private void addTransferInTransaction()
    {
        DocumentType type = new DocumentType("Gutschrifts-\\/Belastungsanzeige");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^Depoteingang(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Depoteingang DEKAFONDS CF (DE0008474503)
                // Kurs           : 105,769231 EUR         Devisenkurs    :          1,000000
                .section("isin", "name", "currency")
                .match("^Depoteingang ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stk./Nominale   : 25.000,000000 EUR    Einbeh. Steuer* :              0,00 EUR
                .section("shares", "notation")
                .match("^Stk\\.\\/Nominale([\\s]+)?: ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("Stk"))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // Datum : 16.03.2015
                .section("date")
                .match("Datum([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Kurs           : 30,070360 EUR          Devisenkurs    :          1,000000
                .section("amount", "currency")
                .match("^Kurs([\\s]+)?: ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")) * t.getShares() / Values.Share.factor());
                })

                /***
                 * If the taxes are negative,
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addTransferInOutTaxReturnBlock(type);
                 */
                //                                           Einbeh. Steuer**:          -1,00 EUR
                .section("taxRefund").optional()
                .match("^.* Einbeh\\. Steuer[\\*\\s]+: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() - asAmount(v.get("taxRefund")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTransferInOutTaxReturnBlock(type);
    }

    private void addAdvanceTaxTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung Vorabpauschale", (context, lines) -> {
            Pattern pDate = Pattern.compile("^Buchungsdatum ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$");
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                    context.put("date", m.group("date"));
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("^Wertpapierabrechnung Vorabpauschale .*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAXES);
                    return t;
                })

                // ISHS MSCI EM USD-AC                 IE00BKM4GZ66/A111X9    
                .section("wkn", "isin", "name")
                .match("^(?<name>.*) (?<isin>[\\w]{12})\\/(?<wkn>.*) *$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                .section("shares")
                .match("^Gesamtbestand ([\\s]+)?(?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Buchungsdatum        11.01.2020
                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                .section("date")
                .match("Gesamtbestand .* zum ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(type.getCurrentContext().get("date") != null ? type.getCurrentContext().get("date") : v.get("date")));
                })

                //                                  ** Einbeh. Steuer                    4,69 EUR
                .section("amount", "currency")
                .match("^.* Einbeh\\. Steuer ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode("currency"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the buy/sell transaction function must be adjusted.
         * addBuySellTransaction();
         */
        Block block = new Block("^.* Auftragsdatum .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Nr.123441244/1  Kauf            C.S.-MSCI PACIF.T.U.ETF I (LU0392495023/ETF114)
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Nr.76443716/1  Verkauf           UBS AG LONDON 14/16 RWE (DE000US9RGR9/US9RGR)
                // Kurs           29,0000 %               Kurswert       EUR             7.250,00
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([\\s]+)? ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Ausgeführt     25.000,00000 EUR
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                .section("shares", "notation")
                .match("^Ausgef.hrt ([:\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|[\\w]{3})).*$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("St."))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // An den  Ausführungszeit   13:59 Häusern 5 
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("time").optional()
                .match("^.* Ausf.hrungszeit ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Max Mustermann Schlusstag        03.12.2015o
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("date")
                .match("^.* (Handelstag|Schlusstag) ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                /***
                * If the currency of the tax differs from the amount,
                * it will be converted and reset.
                */
                .oneOf(
                                // Endbetrag      EUR               -50,30
                                section -> section
                                        .attributes("currency")
                                        .match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?[\\.,\\d]+$")
                                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))
                                ,
                                //        Endbetrag                   -52,50 EUR
                                // Endbetrag     :            -760,09 EUR
                                section -> section
                                        .attributes("currency")
                                        .match("^(.* )?Endbetrag([:\\s]+)? (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))
                        )

                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([:\\s]+)? (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                    {
                        Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                        type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                        fxAmount = Money.of(v.get("fxCurrency"),
                                        BigDecimal.valueOf(fxAmount.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("fxCurrency", t.getCurrencyCode());
                        v.put("fxAmount", BigDecimal.valueOf(fxAmount.getAmount(), 2).toString().replace('.', ','));

                        t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                        t.setAmount(asAmount(v.get("fxAmount")));
                    }
                })

                //                                     ***Einbeh. SichSt EUR                 -1,00
                .section("amount", "currency").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt([:\\s]+)? (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                //                                      ***Einbeh. SichSt EUR                 -1,00
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*\\*Einbeh\\. SichSt([:\\s]+)? (?<fxCurrency>[\\w]{3}) ([\\s]+)?\\-(?<fxAmount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                    {
                        Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                        type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                        fxAmount = Money.of(v.get("fxCurrency"), 
                                        BigDecimal.valueOf(fxAmount.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("fxCurrency", t.getCurrencyCode());
                        v.put("fxAmount", BigDecimal.valueOf(fxAmount.getAmount(), 2).toString().replace('.', ','));
                        t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                        t.setAmount(asAmount(v.get("fxAmount")));
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addSummaryStatementTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the buy/sell transaction function must be adjusted.
         * addSummaryStatementBuySellTransaction();
         */
        Block block = new Block("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Nr.60797017/1  Verkauf             HANN.RUECK SE NA O.N. (DE0008402215/840221)
                // Kurs         : 59,4890 EUR             Kurswert       :           5.948,90 EUR
                .section("name", "isin", "wkn", "currency")
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^.* Kurswert([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                .section("shares")
                .match("^davon ausgef\\.([:\\s]+)? (?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                .section("date", "time")
                .match("^.* Schlusstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                /***
                * If the currency of the tax differs from the amount, 
                * it will be converted and reset.
                */
                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                .section("currency")
                .match("^.* Endbetrag([:\\s]+)? (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([:\\s]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })
                
                // Devisenkurs   : 1,192200(x)             Provision     :
                // Valuta        : 02.12.2020            **Einbeh. Steuer:              -0,84 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([\\s]+)?: (?<exchangeRate>[\\.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([:\\s]+)? \\-(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                    {
                        Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                        type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                        fxAmount = Money.of(v.get("fxCurrency"), 
                                        BigDecimal.valueOf(fxAmount.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("fxCurrency", t.getCurrencyCode());
                        v.put("fxAmount", BigDecimal.valueOf(fxAmount.getAmount(), 2).toString().replace('.', ','));

                        t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                        t.setAmount(asAmount(v.get("fxAmount")));
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTransferInOutTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the transaction function must be adjusted.
         * addTransferOutTransaction();
         * addTransferInTransaction();
         */
        Block block = new Block("^(Depoteingang|Depotausgang|Bestandsausbuchung|Gutschrifts\\- \\/ Belastungsanzeige)(.*)?$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Depotausgang                          COMMERZBANK INLINE09EO/SF (DE000CM31SV9)
                // Bestandsausbuchung                       COMMERZBANK PUT10 EOLS (DE000CB81KN1)
                // Depoteingang                            UBS AG LONDON 14/16 RWE (DE000US9RGR9)
                .section("name", "isin", "currency").optional()
                .match("^(Depoteingang|Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                .section("wkn", "isin", "name", "shares", "currency").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>.*) ([\\s]+)?(?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*) ([\\s]+)?(?<shares>[\\.,\\d]+)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("shares", "notation").optional()
                .match("^Stk\\.\\/Nominale([:\\s]+)? (?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|[\\w]{3})) .*$")
                .assign((t, v) -> {
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("Stk"))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // Fälligkeitstag   : 02.12.2009                  Letzter Handelstag:  20.11.2009
                // Datum          : 24.11.2015
                // Fälligkeitstag                                                  25.06.2021
                // Fälligkeitstag: 18.07.2011 letzter Handel am: 11.07.2011
                .section("date").optional()
                .match("^(F.lligkeitstag|Datum)([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:           -382,12 EUR
                //                                           Einbeh. Steuer**:         -10,00 EUR
                .section("amount", "currency").optional()
                .match("^.* Einbeh\\. Steuer([:\\s\\*]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Einbeh. Steuer**                                                    -88,53 EUR
                .section("amount", "currency").optional()
                .match("^Einbeh\\. Steuer([:\\s\\*]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        /***
         * If the currency of the tax differs from the amount,
         * it will be converted and reset.
         * 
         * Example:
         * **Einbeh. Steuer:           0,84 EUR
         * Endbetrag     :             955,98 USD
         */
        transaction
                // Lagerland    : Deutschland           **Einbeh. Steuer :               0,00 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {

                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })

                // Lagerland    : Deutschland           **Einbeh. Steuer EUR               1,00
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })

                // Lagerland    : Deutschland           **Einbeh. Steuer                0,00 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })
                
                // Lagerland    : Deutschland           **Einbeh. KESt                0,00 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })

                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :             305,85 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })

                //              ***Einbeh. SichSt EUR                1,00
                .section("tax", "currency").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                        {
                            Money taxFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            taxFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(taxFX.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                            v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                            v.put("tax", BigDecimal.valueOf(taxFX.getAmount(), 2).toString().replace('.', ','));
                        }
                        processTaxEntries(t, v, type);
                    }
                })

                //                                      *Einbeh. Steuer  :       284,85 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Quellenst.-satz :           15,00 %    Gez. Quellenst. :            1,15 USD
                .section("withHoldingTax", "currency").optional()
                .match("^.* Gez\\. Quellenst\\.([\\s]+)?: ([\\s]+)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                    }
                })

                // Quellenst.-satz :            15,00 %  Gez. Quellensteuer :           18,28 USD
                .section("withHoldingTax", "currency").optional()
                .match("^.* Gez\\. Quellensteuer([\\s]+)?: ([\\s]+)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                    }
                })

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("withHoldingTax", "currency").optional()
                .match("^.* Einbeh\\. Steuer\\*([\\s]+)?: ([\\s]+)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                    }
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        /***
         * If the currency of the provision fee differs from the amount,
         * it will be converted and reset.
         */
        transaction
                // Devisenkurs  :                         Provision      :               3,90 EUR
                .section("fee", "currency").optional()
                .match("^.* Provision([\\s]+)?: ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        Money feeFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                        BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                        feeFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                        BigDecimal.valueOf(feeFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                        v.put("fee", BigDecimal.valueOf(feeFX.getAmount(), 2).toString().replace('.', ','));
                    }
                    processFeeEntries(t, v, type);
                })

                // Devisenkurs                            Provision      EUR                 5,90
                .section("fee", "currency").optional()
                .match("^.* Provision ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        Money feeFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                        BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                        feeFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                        BigDecimal.valueOf(feeFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                        v.put("fee", BigDecimal.valueOf(feeFX.getAmount(), 2).toString().replace('.', ','));
                    }
                    processFeeEntries(t, v, type);
                })

                // Kurs                 20,835000 EUR      Provision                     5,90 EUR
                .section("fee", "currency").optional()
                .match("^.* Provision ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        Money feeFX = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                        BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                        feeFX = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                        BigDecimal.valueOf(feeFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("currency", ((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode());
                        v.put("fee", BigDecimal.valueOf(feeFX.getAmount(), 2).toString().replace('.', ','));
                    }
                    processFeeEntries(t, v, type);
                })

                // Bew-Faktor   : 1,0000                  Eigene Spesen  :               1,00 EUR
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen([\\s]+)?: ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart     Wertpapierrechnung      Eigene Spesen                 2,71 EUR
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Bew-Faktor   : 1,0000                  Eigene Spesen  EUR                 1,00
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  :               1,00 EUR
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen([\\s]+)?: ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  EUR                 1,00
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart     Wertpapierrechnung      *Fremde Spesen                 2,71 EUR
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen ([\\s]+)?(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        addAccountTransaction();
        addTransferOutTransaction();
        addTransferInTransaction();
        addAdvanceFeeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
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
            return entry;
        });

        Block firstRelevantLine = new Block("^.* Auftragsdatum .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Nr.121625906/1     Kauf        IS C.MSCI EMIMI U.ETF DLA (IE00BKM4GZ66/A111X9)
                .section("name", "isin", "wkn").optional()
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Ausgeführt     25.000,00000 EUR
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                .section("shares", "notation")
                .match("^Ausgef.hrt ([:\\s]+)?(?<shares>[.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3}).*$")
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

                // Max Mustermann Schlusstag        03.12.2015o
                // An den  Ausführungszeit   13:59 Häusern 5
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                .section("date", "time").optional()
                .match("^(.*)?(Handelstag|Schlusstag) ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).*$")
                .match("^(.*)?Ausf.hrungszeit ([\\s]+)?(?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("date", "time").optional()
                .match("^(.*)?Schlusstag ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).* Ausf.hrungszeit ([\\s]+)?(?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })
                
                // Devisenkurs   :        1,000000         Eigene Spesen :               0,00 EUR
                .section("exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Endbetrag      EUR               -50,30
                .section("amount", "currency").optional()
                .match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[-]?(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                //        Endbetrag                   -52,50 EUR
                .section("amount", "currency").optional()
                .match("^.* Endbetrag ([\\s]+)?[-]?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Endbetrag     :            -760,09 EUR
                .section("amount", "currency").optional()
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?[-]?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addTaxReturnBlock(type);
                 */
                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());
                        
                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                //                                     ***Einbeh. SichSt EUR                -1,00
                .section("taxRefund", "currency").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                //                                     ***Einbeh. SichSt EUR                -1,00
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<taxRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
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

        Block firstRelevantLine = new Block("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Nr.60796942/1  Kauf               BAYWA AG VINK.NA. O.N. (DE0005194062/519406)
                .section("name", "isin", "wkn")
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 4.550,00 St.            Schlusstag    :  01.11.2017, 14:41 Uhr
                .section("shares")
                .match("^davon ausgef\\.([\\s]+)?: (?<shares>[.,\\d]+) St\\. .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                .section("date", "time")
                .match("^.* Schlusstag([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}), (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Devisenkurs   : 1,195010                Provision     :               5,90 EUR
                .section("exchangeRate").optional()
                .match("^Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                .section("amount", "currency").optional()
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?[-]?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                .section("taxRefund", "currency", "exchangeRate").optional()
                .match("^Devisenkurs ([\\s]+)?: (?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                    {
                        Money taxRefundFX = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                        taxRefundFX = Money.of(t.getPortfolioTransaction().getCurrencyCode(), 
                                        BigDecimal.valueOf(taxRefundFX.getAmount()).multiply(exchangeRate)
                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                        v.put("taxRefund", BigDecimal.valueOf(taxRefundFX.getAmount(), 2).toString().replace('.', ','));

                        t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addSummaryStatementTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift)( .*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Nr.716759781                   HANN.RUECK SE NA O.N.     (DE0008402215/840221)
                .section("name", "isin", "wkn")
                .match("^Nr\\.\\d+ ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // St.             :         360
                .section("shares")
                .match("^(St\\.|St\\.\\/Nominale) ([\\s]+)?: ([\\s]+)?(?<shares>[.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .section("date")
                .match("Valuta ([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                //                                        Endbetrag       :       795,15 EUR
                .section("amount", "currency").optional()
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                /***
                 * If dividends amount is negative 
                 * then we switch the transaction to taxes, 
                 * because there is reinvesting and only the taxes 
                 * must be paid from the dividend transaction
                 */
                //                                       Endbetrag          :        -8,26 EUR
                .section("amount", "currency").optional()
                .match(".* Endbetrag([\\s]+)?: ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setType(AccountTransaction.Type.TAXES);

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Extag : 08.08.2017 Bruttodividende : 26,25 USD
                // Devisenkurs     :    1,180800         *Einbeh. Steuer  :         1,11 EUR
                .section("fxAmountGross", "fxCurrency", "exchangeRate").optional()
                .match("^.* (Bruttoaussch.ttung|Bruttodividende) ([\\s]+)?: ([\\s]+)?(?<fxAmountGross>[.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs([\\s]+)?: ([\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
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

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");

            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }

                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        // 29.01.     29.01.  �berweisung                                       1.100,00+
        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+.berweisung[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("valuta", "amount", "note", "sign")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>.berweisung)[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                .assign((t, v) -> {
                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.REMOVAL);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // 01.10.     01.10.  EINZAHLUNG 4 FLATEX / 0/16765097                  2.000,00+
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+(EINZAHLUNG|AUSZAHLUNG) .* +[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("valuta", "amount", "note", "sign")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>(EINZAHLUNG|AUSZAHLUNG)) .* +(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                .assign((t, v) -> {
                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.REMOVAL);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // Added "Lastschrift" as DEPOSIT option
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Lastschrift[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("valuta", "amount", "note", "sign")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>Lastschrift)[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> new TransactionItem(t)));

        // 11.11.     12.11.  Gebühr Kapitaltransaktion Ausland                     4,56-
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Geb.hr Kapitaltransaktion Ausland[ ]+[\\d.-]+,\\d+[-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("valuta", "amount", "isin", "note")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>Geb.hr Kapitaltransaktion Ausland)[ ]+(?<amount>[\\d.-]+,\\d+)[-]")
                .match("\\s*(?<isin>\\w{12})").assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
                    }
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // 19.07.     20.07.  Depotgebühren 01.04.2020 - 30.04.2020,                0,26-
        block = new Block("\\d+.\\d+.[ ]+\\d+.\\d+.[ ]+Depotgeb.hren[ ]+\\d{2}.\\d{2}.\\d{4}[ -]+\\d{2}.\\d{2}.\\d{4},[ ]+[\\d.-]+,\\d+[-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("valuta", "amount", "note")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>Depotgeb.hren[ ]+\\d{2}.\\d{2}.\\d{4}[ -]+\\d{2}.\\d{2}.\\d{4}),[ ]+(?<amount>[\\d.-]+,\\d+)[-]")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
                    }
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // 30.12.     31.12.  Zinsabschluss   01.10.2014 - 31.12.2014               7,89+
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Zinsabschluss[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    return t;
                })

                .section("valuta", "amount", "note", "sign")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>Zinsabschluss[ ]+(\\d+.\\d+.\\d{4})(\\s+)-(\\s+)(\\d+.\\d+.\\d{4}))(\\s+)(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                .assign((t, v) -> {
                    // Is sign --> "+" change from INTEREST_CHARGE to INTEREST
                    if (v.get("sign").equals("+"))
                    {
                        t.setType(AccountTransaction.Type.INTEREST);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
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

        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Steuertopfoptimierung[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                .section("valuta", "amount", "note", "sign")
                .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<note>Steuertopfoptimierung[ ]+(\\d{4}))(\\s+)(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                .assign((t, v) -> {
                    // Is sign --> "-" change from TAX_REFUND to TAXES
                    if (v.get("sign").equals("-"))
                    {
                        t.setType(AccountTransaction.Type.TAXES);
                    }

                    Map<String, String> context = type.getCurrentContext();
                    if (v.get("valuta") != null)
                    {
                        // create a long date from the year in the context
                        t.setDateTime(asDate(v.get("valuta") + context.get("year")));
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
        DocumentType type = new DocumentType("Depotausgang|Bestandsausbuchung|Gutschrifts- \\/ Belastungsanzeige");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Depotausgang|Bestandsausbuchung|Gutschrifts- \\/ Belastungsanzeige)(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Depotausgang                          COMMERZBANK INLINE09EO/SF (DE000CM31SV9)
                // Bestandsausbuchung                       COMMERZBANK PUT10 EOLS (DE000CB81KN1)
                .section("isin", "name").optional()
                .match("^(Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                .section("wkn", "isin", "name", "shares").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>.*) ([\\s]+)?(?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*) ([\\s]+)?(?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("shares", "notation").optional()
                .match("^Stk\\.\\/Nominale([\\*\\s]+)?: ([\\s]+)?(?<shares>[.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3})(.*)$")
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
                .section("date")
                .match("^F.lligkeitstag([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Verwahrart      : GS-Verwahrung        Geldgegenwert***:              0,20 EUR
                //                                           Geldgegenwert*  :         111,22 EUR
                .section("amount", "currency").optional()
                .match("^(.*) Geldgegenwert\\*.*([\\s]+)?: ([\\s]+)?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})(.*)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // **oben genannten Bestand haben wir wertlos ausgebucht
                .section().optional()
                .match("(.*)Bestand haben wir wertlos ausgebucht(.*)")
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
                .match("^.* Einbeh\\. Steuer\\*.*([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) [\\w]{3}$")
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
                .section("isin", "name")
                .match("^Depoteingang ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stk./Nominale   : 25.000,000000 EUR    Einbeh. Steuer* :              0,00 EUR
                .section("shares", "notation")
                .match("^Stk\\.\\/Nominale([\\s]+)?: ([\\s]+)?(?<shares>[.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3}).*$")
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
                .match("Datum([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.\\d{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Kurs           : 30,070360 EUR          Devisenkurs    :          1,000000
                .section("amount", "currency")
                .match("^Kurs([\\s]+)?: ([\\s]+)?(?<amount>[.,\\d]+) (?<currency>[\\w]{3}).*$")
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
                .match("^.* Einbeh\\. Steuer\\*.*([\\s]+)?: ([\\s]+)?-(?<taxRefund>[.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() - asAmount(v.get("taxRefund")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTransferInOutTaxReturnBlock(type);
    }

    private void addAdvanceFeeTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung Vorabpauschale", (context, lines) -> {
            Pattern pDate = Pattern.compile("Buchungsdatum +(?<date>\\d+.\\d+.\\d{4}+) *");
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                    context.put("buchungsdatum", m.group("date"));
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
                .match("^(?<name>.*) +(?<isin>[\\w]{12})/(?<wkn>.*) *$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                .section("shares")
                .match("^Gesamtbestand * (?<shares>[\\d,.]+) St.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Buchungsdatum        11.01.2020
                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                .section("date")
                .match("Gesamtbestand .*zum +(?<date>\\d+.\\d+.\\d{4}).*$")
                .assign((t, v) -> {
                    // prefer "buchungsdatum" over "date" if available
                    t.setDateTime(asDate(type.getCurrentContext().get("buchungsdatum") != null ? type.getCurrentContext().get("buchungsdatum") : v.get("date")));
                })

                //                                  ** Einbeh. Steuer                    4,69 EUR
                .section("amount", "currency")
                .match("^.* Einbeh\\. Steuer ([\\s]+)?(?<amount>[.,\\d]+) (?<currency>[\\w]{3}).*$")
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
                .section("name", "isin", "wkn")
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                .section("shares").optional()
                .match("^Ausgef.hrt([:\\s]+)?(?<shares>[.,\\d]+) St\\..*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Max Mustermann Schlusstag        03.12.2015o
                // An den  Ausführungszeit   13:59 Häusern 5
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                .section("date", "time").optional()
                .match("^(.*)?(Handelstag|Schlusstag) ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).*$")
                .match("^(.*)?Ausf.hrungszeit ([\\s]+)?(?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                .section("date", "time").optional()
                .match("^(.*)?Schlusstag ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).* Ausf.hrungszeit ([\\s]+)?(?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                /***
                * If the currency of the tax differs from the amount,
                * it will be converted and reset.
                */
                // Endbetrag      EUR               -50,30
                .section("currency").optional()
                .match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[-]?[.,\\d]+$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                //        Endbetrag                   -52,50 EUR
                .section("currency").optional()
                .match("^.* Endbetrag ([\\s]+)?[-]?[.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Endbetrag     :            -760,09 EUR
                .section("currency").optional()
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?[-]?[.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                .section("currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?-(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
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

                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
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

                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                 -1,00
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                 -1,00
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?-(?<fxAmount>[.,\\d]+)$")
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

                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                // Devisenkurs   : 1,192200(x)             Provision     :
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency").optional()
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?-(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
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
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?-(?<amount>[.,\\d]+)$")
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
                .match("^Devisenkurs ([:\\s]+)?(?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?-(?<fxAmount>[.,\\d]+)$")
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
        Block block = new Block("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Nr.60797017/1  Verkauf             HANN.RUECK SE NA O.N. (DE0008402215/840221)
                .section("name", "isin", "wkn", "currency")
                .match("^Nr\\.\\d+\\/\\d+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[\\w]{12})\\/(?<wkn>.*)\\)$")
                .match("^Kurs ([\\s]+)?: [.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                .section("shares")
                .match("^davon ausgef\\.([\\s]+)?: (?<shares>[.,\\d]+) St\\. .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                .section("date", "time")
                .match("^.* Schlusstag([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}), (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                /***
                * If the currency of the tax differs from the amount, 
                * it will be converted and reset.
                */
                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                .section("currency")
                .match("^.* Endbetrag([\\s]+)?: ([\\s]+)?[-]?[.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                .section("amount", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^Devisenkurs ([\\s]+)?: (?<exchangeRate>[.,\\d]+).*$")
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?-(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
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
        Block block = new Block("^(Depoteingang|Depotausgang|Bestandsausbuchung|Gutschrifts- \\/ Belastungsanzeige)(.*)?$");
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
                .section("isin", "name").optional()
                .match("^(Depoteingang|Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[\\w]{12})\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                .section("wkn", "isin", "name", "shares").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>.*) ([\\s]+)?(?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*) ([\\s]+)?(?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("shares", "notation").optional()
                .match("^Stk\\.\\/Nominale([\\*\\s]+)?: ([\\s]+)?(?<shares>[.,\\d]+) ([\\s]+)?(?<notation>St\\.|[\\w]{3})(.*)$")
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
                .section("date")
                .match("^(F.lligkeitstag|Datum)([\\s]+)?: ([\\s]+)?(?<date>\\d+.\\d+.\\d{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:           -382,12 EUR
                //                                           Einbeh. Steuer**:         -10,00 EUR
                .section("amount", "currency").optional()
                .match("^.* Einbeh\\. Steuer\\*(.*)([\\s]+)?: ([\\s]+)?-(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^.* \\*\\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
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
                })

                // Lagerland    : Deutschland           **Einbeh. Steuer EUR               1,00
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
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
                })

                // Lagerland    : Deutschland           **Einbeh. Steuer                0,00 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. Steuer ([\\s]+)?(?<currency>[\\w]{3}) (?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
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
                })
                
                // Lagerland    : Deutschland           **Einbeh. KESt                0,00 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
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
                })

                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :             305,85 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. KESt([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
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
                })

                //              ***Einbeh. SichSt EUR                1,00
                .section("tax", "currency").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
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
                })

                //                                      *Einbeh. Steuer  :       284,85 EUR
                .section("tax", "currency").optional()
                .match("^.* \\*Einbeh\\. Steuer([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Quellenst.-satz :           15,00 %    Gez. Quellenst. :            1,15 USD
                .section("tax", "currency").optional()
                .match("^.* Gez\\. Quellenst\\.([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Quellenst.-satz :            15,00 %  Gez. Quellensteuer :           18,28 USD
                .section("tax", "currency").optional()
                .match("^.* Gez\\. Quellensteuer([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                .section("tax", "currency").optional()
                .match("^.* Einbeh\\. Steuer\\*([\\s]+)?: ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
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
                .match("^.* Provision([\\s]+)?: ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^.* Provision([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[.,\\d]+)$")
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
                .match("^.* Provision ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^.* Eigene Spesen([\\s]+)?: ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart     Wertpapierrechnung      Eigene Spesen                 2,71 EUR
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Bew-Faktor   : 1,0000                  Eigene Spesen  EUR                 1,00
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  :               1,00 EUR
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen([\\s]+)?: ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  EUR                 1,00
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart     Wertpapierrechnung      *Fremde Spesen                 2,71 EUR
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen ([\\s]+)?(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
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
}

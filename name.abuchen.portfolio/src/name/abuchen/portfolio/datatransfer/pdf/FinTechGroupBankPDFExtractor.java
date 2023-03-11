package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Storno Wertpapierabrechnung Kauf Fonds/Zertifikate
                // Stornierung Wertpapierabrechnung Kauf Fonds
                .section("type").optional()
                .match("^(?<type>(Storno|Stornierung)) Wertpapierabrechnung .*$")
                .assign((t, v) -> v.getTransactionContext().put(FAILURE,
                                Messages.MsgErrorOrderCancellationUnsupported))

                // @formatter:off
                // Nr.121625906/1     Kauf        IS C.MSCI EMIMI U.ETF DLA (IE00BKM4GZ66/A111X9)
                // Kurs          :       25,920000 EUR     Provision     :               5,90 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf)([\\s]+)? (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Nr.76443716/1  Verkauf           UBS AG LONDON 14/16 RWE (DE000US9RGR9/US9RGR)
                // Kurs           29,0000 %               Kurswert       EUR             7.250,00
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf)([\\s]+)? (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ % ([\\s]+)?Kurswert([\\s]+)? (?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Ausgeführt     25.000,00000 EUR
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                // @formatter:on
                .section("shares", "notation")
                .match("^Ausgef.hrt([:\\s]+)? (?<shares>[\\.,\\d]+)([\\s]+)? (?<notation>(St\\.|Stk|[\\w]{3})).*$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").startsWith("St"))
                    {
                        BigDecimal shares = asBigDecimal(v.get("shares"));
                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // @formatter:off
                // An den  Ausführungszeit   13:59 Häusern 5 
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                // @formatter:on
                .section("time").optional()
                .match("^.* Ausf.hrungszeit([\\s]+)? (?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // @formatter:off
                // Max Mustermann Schlusstag        03.12.2015o
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                // @formatter:on
                .section("date")
                .match("^.* (Handelstag|Schlusstag)([\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // @formatter:off
                // Devisenkurs   :        1,000000         Eigene Spesen :               0,00 EUR
                // @formatter:on
                .section("exchangeRate").optional()
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .oneOf(
                                // @formatter:off
                                // Endbetrag      EUR               -50,30
                                // Gewinn/Verlust -267,59 EUR             Endbetrag      EUR            16.508,16
                                //                                        Endbetrag      EUR                 0,95
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.* Endbetrag([\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (\\-)?(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                //        Endbetrag                   -52,50 EUR
                                // Endbetrag     :            -760,09 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^(.* )?Endbetrag([:\\s]+)? (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // @formatter:off
                // Ausgeführt : 80 St. Kurswert : 4.216,61 EUR
                // Kurs : 55,080000 USD Provision : 0,00 EUR
                // Devisenkurs : 1,045010 Eigene Spesen : 0,00 EUR
                // @formatter:on
                .section("gross", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^.* Kurswert([:\\s]+)? (?<gross>[\\.,\\d]+)([\\s]+)? (?<currency>[\\w]{3})$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) .*$")
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // If the taxes are negative, this is a tax refund transaction
                // and we subtract this from the amount and reset this.
                //
                // If the currency of the tax differs from the amount,
                // it will be converted and reset.
                // @formatter:on

                .optionalOneOf(
                                // @formatter:off
                                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                                //                                     ***Einbeh. SichSt EUR                -1,00
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "taxRefund")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt|SichSt)([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? \\-(?<taxRefund>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                            {
                                                type.getCurrentContext().put("negativeTax", "X");

                                                Money taxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));
                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR                -1,00
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                //                                     ***Einbeh. SichSt EUR                -1,00
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "currency", "taxRefund")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt|SichSt)([\\s]+)? (?<currency>[\\w]{3})([\\s]+)? \\-(?<taxRefund>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && !t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                            {
                                                type.getCurrentContext().put("negativeTax", "X");

                                                Money fxTaxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money taxRefund = Money.of(t.getPortfolioTransaction().getCurrencyCode(), BigDecimal.valueOf(fxTaxRefund.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer               -1,00 EUR
                                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("taxRefund", "currency")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                            {
                                                type.getCurrentContext().put("negativeTax", "X");

                                                Money taxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));
                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust:            0,00 EUR   **Einbeh. Steuer:              -1,00 EUR
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("taxRefund", "currency", "exchangeRate")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && !t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                            {
                                                type.getCurrentContext().put("negativeTax", "X");

                                                Money fxTaxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money taxRefund = Money.of(t.getPortfolioTransaction().getCurrencyCode(), BigDecimal.valueOf(fxTaxRefund.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() == null)
                                                t.setNote(trim(v.get("note")));
                                        })
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> {
                                            if (t.getNote() == null)
                                                t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2")));
                                        })
                        )

                .wrap((t, ctx) -> {
                    // If the taxes are negative, then this is a tax
                    // refund and has been marked so. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negativeTax");

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                    {
                        BuySellEntryItem item = new BuySellEntryItem(t);
                        item.setFailureMessage(ctx.getString(FAILURE));
                        return item;
                    }
                    return null;
                });

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

        Block firstRelevantLine = new Block("^Nr\\.[\\d]+\\/[\\d]+([\\s]+)? (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+([\\s]+)? (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // Nr.60796942/1  Kauf               BAYWA AG VINK.NA. O.N. (DE0005194062/519406)
                // Kurs         : 39,2480 EUR             Kurswert       :           5.887,20 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+([\\s]+)? (Kauf|Verkauf)([\\s]+)? (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 4.550,00 St.            Schlusstag    :  01.11.2017, 14:41 Uhr
                // @formatter:on
                .section("shares").optional()
                .match("^davon ausgef\\.([:\\s]+)? (?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                // @formatter:on
                .section("date", "time").optional()
                .match("^.* Schlusstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), (?<time>[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // @formatter:off
                // Devisenkurs   : 1,192200(x)             Provision     :
                // Devisenkurs   : 1,195010                Provision     :               5,90 EUR
                // @formatter:on
                .section("exchangeRate").optional()
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // @formatter:off
                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                //                                         Endbetrag     :           4.773,36 USD
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^.* Endbetrag([:\\s]+)? (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Kurs          : 114,4700 USD            Kurswert      :             528,10 EUR
                // Devisenkurs   : 1,083790                Provision     :               5,90 EUR
                .section("gross", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) .* Kurswert([:\\s]+)? (?<gross>[\\.,\\d]+)([\\s]+)? (?<currency>[\\w]{3})$")
                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // If the total amount is negative and the market value
                // is less than the fees, then we post the gross value.
                // The fees are processed in a separate transaction.
                //
                // If the currency of the fees differs from the final
                // amount, they are converted and reset.
                // @formatter:on

                .optionalOneOf(
                                section -> section
                                .attributes("gross", "fxCurrency", "exchangeRate")
                                .match("^.* Kurswert([:\\s]+)? (?<gross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                                .assign((t, v) -> {
                                    if (t.getPortfolioTransaction().getType().isLiquidation() && !t.getPortfolioTransaction().getCurrencyCode().equals(v.get("fxCurrency")))
                                    {
                                        type.getCurrentContext().put("negative", "X");

                                        Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("gross")));

                                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                        Money gross = Money.of(t.getPortfolioTransaction().getCurrencyCode(), BigDecimal.valueOf(fxGross.getAmount())
                                                        .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                        t.setMonetaryAmount(gross);
                                    }
                                    else if (t.getPortfolioTransaction().getType().isLiquidation() && t.getPortfolioTransaction().getCurrencyCode().equals(v.get("fxCurrency")))
                                    {
                                        type.getCurrentContext().put("negative", "X");

                                        t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                        t.setAmount(asAmount(v.get("gross")));
                                    }
                                })
                                ,
                                // @formatter:off
                                // Kurs          : 0,0010 EUR              Kurswert      :               1,19 EUR
                                // Devisenkurs   :                         Provision     :               5,90 EUR
                                //                                         Endbetrag     :              -4,71 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("gross", "fxCurrency")
                                        .match("^.* Kurswert([:\\s]+)? (?<gross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && t.getPortfolioTransaction().getCurrencyCode().equals(v.get("fxCurrency")))
                                            {
                                                type.getCurrentContext().put("negative", "X");

                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                t.setAmount(asAmount(v.get("gross")));
                                            }
                                        })
                        )

                // @formatter:off
                // If the taxes are negative, 
                // this is a tax refund transaction
                // and we subtract this from the amount and reset this.
                //  
                // If the currency of the tax differs from the amount, 
                // it will be converted and reset.
                // @formatter:on

                .optionalOneOf(
                                section -> section
                                .attributes("exchangeRate", "taxRefund", "currency")
                                .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+) \\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                .assign((t, v) -> {
                                    if (t.getPortfolioTransaction().getType().isLiquidation() && !t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                    {
                                        type.getCurrentContext().put("negativeTax", "X");

                                        Money fxTaxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));

                                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                        Money taxRefund = Money.of(t.getPortfolioTransaction().getCurrencyCode(), BigDecimal.valueOf(fxTaxRefund.getAmount())
                                                        .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                        t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                    }
                                })
                                ,
                                // @formatter:off
                                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("taxRefund", "currency")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? \\-(?<taxRefund>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getType().isLiquidation() && t.getPortfolioTransaction().getCurrencyCode().equals(v.get("currency")))
                                            {
                                                type.getCurrentContext().put("negativeTax", "X");

                                                Money taxRefund = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxRefund")));
                                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(taxRefund));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // If the taxes are negative, then this is a tax
                    // refund and has been marked so. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negativeTax");

                    // The final amount is negative. The fees incurred
                    // are processed in a separate transaction. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negative");

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addSummaryStatementTaxReturnBlock(type);
        addSummaryStatementFeesBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsmitteilung|Zinsgutschrift)( .*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Nr.716759781                   HANN.RUECK SE NA O.N.     (DE0008402215/840221)
                // Extag           :  08.05.2014          Bruttodividende
                // Zahlungstag     :  08.05.2014          pro Stück       :       3,0000 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+([\\s]+)? (?<name>.*)([\\s]+)? \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\).*$")
                .find(".* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)")
                .match("^.* pro St.ck([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Nr.111111111                   ISH.FOOBAR 12345666 x.EFT (DE1234567890/AB1234)
                // Zinstermin      :  28.04.2016          Zinsbetrag      :        73,75 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+([\\s]+)? (?<name>.*)([\\s]+)? \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\).*$")
                .match("^.* Zinsbetrag([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // St.             :         360
                // @formatter:on
                .section("shares")
                .match("^(St\\.|St\\.\\/Nominale)([:\\s]+)? (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valuta          :      17.06.2022      grundlage       :            0,00 EUR
                // @formatter:on
                .section("date")
                .match("Valuta([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                //                                        Endbetrag       :       795,15 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* Endbetrag([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Extag           :    07.10.2021        Bruttothesaurierung:        78,81 USD
                                // Zuflusstag      :    08.10.2021        Devisenkurs        :         1,156200
                                //                                        Endbetrag          :       -15,24 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("type", "fxGross", "fxCurrency", "exchangeRate", "currency")
                                        .match("^.* (?<type>(Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung))([:\\s]+)? (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^(.* )?Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negative", "X");

                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            // If we have a negative amount and no gross reinvestment,
                                            // we first book the dividends received and then the tax charge
                                            if (v.get("type").equals("Bruttothesaurierung"))
                                            {
                                                t.setAmount(0L);
                                            }
                                            else
                                            {
                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                                                {
                                                    exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                                                }
                                                type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                                Money gross = Money.of(asCurrencyCode(v.get("currency")),
                                                                BigDecimal.valueOf(fxGross.getAmount()).multiply(inverseRate)
                                                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(gross);
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Extag           :    20.01.2020        Bruttothesaurierung:        23,19 EUR
                                //                                        Endbetrag          :        -8,26 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("type", "amount", "currency")
                                        .match("^.* (?<type>(Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung))([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negative", "X");

                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            // If we have a negative amount and no gross reinvestment,
                                            // we first book the dividends received and then the tax charge
                                            if (v.get("type").equals("Bruttothesaurierung"))
                                                t.setAmount(0L);
                                            else
                                                t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Extag           :      20.05.2020      Bruttodividende :           25,50 USD
                                //                                       *Einbeh. Steuer  :            2,37 EUR
                                // Devisenkurs     :        1,134800
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "currency", "exchangeRate")
                                        .match("^(.* )?(Bruttoaussch.ttung|Bruttodividende|Bruttothesaurierung)([:\\s]+)? (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .match("^(.* )?Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Extag           :  08.08.2017          Bruttodividende :        26,25 USD
                                // Devisenkurs     :    1,180800         *Einbeh. Steuer  :         1,11 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "currency")
                                        .match("^(.* )?(Bruttoaussch.ttung|Bruttodividende|Bruttothesaurierung)([:\\s]+)? (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                                        .match("^(.* )?Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+) ([\\*\\s]+)?Einbeh\\. Steuer([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // The final amount is negative. The taxes incurred
                    // are processed in a separate transaction. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negative");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Nr.1784953069                  PICTET-GL.MEGAT.SEL.P EO  (LU0386882277/A0RLJD)
                // St.             :          10,0        Bruttothesaurierung
                //                                        pro Stück          :       2,3189 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+([\\s]+)? (?<name>.*)([\\s]+)? \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\).*$")
                .find(".* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)")
                .match("^.* pro St.ck([:\\s]+) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // St.             :         168,9        Bruttothesaurierung
                // @formatter:on
                .section("shares")
                .match("^(St\\.|St\\.\\/Nominale)([:\\s]+)? (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valuta          :      17.06.2022      grundlage       :            0,00 EUR
                // @formatter:on
                .section("date")
                .match("^Valuta([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // If the currency of the tax differs from the amount, 
                // it will be converted and reset.

                // @formatter:off
                //                                       Endbetrag          :        -8,26 EUR
                // @formatter:on
                .section("currency").optional()
                .match("^.* Endbetrag([:\\s]+)? (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                // @formatter:off
                // Devisenkurs     :         1,110600   *Einbeh. Steuer     :           99,39 EUR
                //                                       Endbetrag          :        -8,26 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    type.getCurrentContext().put("negative", "X");

                    if (t.getCurrencyCode().contentEquals(v.get("currency")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                .optionalOneOf(
                                // @formatter:off
                                // Extag           :       17.02.2022    Bruttoausschüttung :           34,66 USD
                                // Valuta          :       02.03.2022
                                //                                       Bemessungsgrundlage:          361,42 EUR
                                // Devisenkurs     :         1,110600   *Einbeh. Steuer     :           99,39 EUR
                                //                                       Endbetrag          :          -68,18 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "exchangeRate", "gross", "currency")
                                        .match("^.* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)([:\\s]+)? [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).* [\\*]+Einbeh\\. Steuer([:\\s]+)? (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Extag           :    07.10.2021        Bruttothesaurierung:        78,81 USD
                                // Valuta          :    08.10.2021       *Einbeh. Steuer     :        15,24 EUR
                                // Zuflusstag      :    08.10.2021        Devisenkurs        :         1,156200
                                //                                        Endbetrag          :       -15,24 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "gross", "currency", "exchangeRate")
                                        .match("^.* (Bruttodividende|Bruttoaussch.ttung|Bruttothesaurierung)([:\\s]+)? [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^(.* )?Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* Endbetrag([:\\s]+)? \\-[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // The final amount is negative. The taxes incurred
                    // are processed in a separate transaction. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negative");

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
                    context.put("year", m.group("year"));

                m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // 29.01.     29.01.  Überweisung                                       1.100,00+
        // @formatter:on
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
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.REMOVAL);
                    
                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // @formatter:off
        // 01.10.     01.10.  EINZAHLUNG 4 FLATEX / 0/16765097                  2.000,00+
        // @formatter:on
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
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.REMOVAL);

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // @formatter:off
        // 19.11.     19.11.  R-Transaktion                                       -53,00-
        // @formatter:on
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

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        // @formatter:off
        // 19.11.     19.11.  Lastschrift                                          50,00+
        // @formatter:on
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

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> new TransactionItem(t)));

        // @formatter:off
        // 19.07.     20.07.  Depotgebühren 01.04.2020 - 30.04.2020,                0,26-
        // @formatter:on
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

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // 30.12.     31.12.  Zinsabschluss   01.10.2014 - 31.12.2014               7,89+
        // @formatter:on
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
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "+" change from INTEREST_CHARGE to INTEREST
                    if (v.get("sign").equals("+"))
                        t.setType(AccountTransaction.Type.INTEREST);

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // 31.12.     31.12.  Steuertopfoptimierung 2016                            4,94+
        // @formatter:on
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
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from TAX_REFUND to TAXES
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.TAXES);

                    // create a long date from the year in the context
                    if (v.get("date") != null)
                        t.setDateTime(asDate(v.get("date") + context.get("year")));

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
                    context.put("date", m.group("date"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setDate(asDate(type.getCurrentContext().get("date").toString()));
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Depotausgang|Bestandsausbuchung|Gutschrifts\\- \\/ Belastungsanzeige).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Depotausgang                          COMMERZBANK INLINE09EO/SF (DE000CM31SV9)
                // Rückzahlungsgrund: Ablauf der Optionsfrist     Betrag/Stk: 10,000000000000 EUR
                // Bestandsausbuchung                       COMMERZBANK PUT10 EOLS (DE000CB81KN1)
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .match("^(Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Depotausgang                          COMS.-MSCI WORL.T.U.ETF I (LU0392494562)
                //                                          Geldgegenwert***:           0,00 EUR
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .match("^Depotausgang ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$")
                .match("^.* Geldgegenwert([:\\*\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                // @formatter:on
                .section("wkn", "isin", "name", "currency").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>[A-Z0-9]{6}) ([\\s]+)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\s]+)?(?<name>.*) ([\\s]+)?[\\.,\\d]+$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // @formatter:off
                                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                                // St./Nominale      :      310,000000 St.  Bemessungs-
                                // Stk./Nominale**: 2.000,000000 Stk       Einbeh. Steuer*:              0,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares", "notation")
                                        .match("^(St|Stk|Stck)\\.\\/Nominale([*:\\s]+)? (?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|Stk|[\\w]{3})) .*$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            if (v.get("notation") != null && !v.get("notation").startsWith("St"))
                                            {
                                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                            }
                                            else
                                            {
                                                t.setShares(asShares(v.get("shares")));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Stck./Nominale:          83,000000        Betrag/Stck.    :           1,34 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(Stk|Stck)\\.\\/Nominale([:\\s]+)? (?<shares>[\\.,\\d]+) .* Betrag\\/Stck\\. .* [\\.,\\d]+ [\\w]{3}$$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // @formatter:off
                // Fälligkeitstag   : 02.12.2009                  Letzter Handelstag:  20.11.2009
                // Fälligkeitstag                                                  25.06.2021
                // @formatter:on
                .section("date").optional()
                .match("^F.lligkeitstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // Verwahrart      : GS-Verwahrung        Geldgegenwert***:              0,20 EUR
                //                                           Geldgegenwert*  :         111,22 EUR
                // Geldgegenwert                                                       393,73 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^(.* )?Geldgegenwert([:\\*\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // **oben genannten Bestand haben wir wertlos ausgebucht
                // @formatter:on
                .section().optional()
                .match("^.* Bestand haben wir wertlos ausgebucht.*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                    t.setAmount(0L);
                    t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                    t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                })

                // If the taxes are negative,
                // this is a tax refund transaction
                // and we subtract this from the amount and reset this.

                // @formatter:off
                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:           -382,12 EUR
                //                                           Einbeh. Steuer**:         -10,00 EUR
                // @formatter:on
                .section("taxRefund").optional()
                .match("^.* Einbeh\\. Steuer[\\*\\s]+: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund"))))

                // @formatter:off
                // Einbeh. Steuer**                                                    -88,53 EUR
                // @formatter:on
                .section("taxRefund").optional()
                .match("^Einbeh\\. Steuer[\\*\\s]+ \\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund"))))

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

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

        Block firstRelevantLine = new Block("^Depoteingang.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Depoteingang DEKAFONDS CF (DE0008474503)
                // Kurs           : 105,769231 EUR         Devisenkurs    :          1,000000
                // @formatter:on
                .section("isin", "name", "currency")
                .match("^Depoteingang ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Stk./Nominale   : 25.000,000000 EUR    Einbeh. Steuer* :              0,00 EUR
                // @formatter:on
                .section("shares", "notation")
                .match("^Stk\\.\\/Nominale([\\s]+)?: ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|Stk|[\\w]{3})).*$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").startsWith("St"))
                    {
                        BigDecimal shares = asBigDecimal(v.get("shares"));
                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // @formatter:off
                // Datum : 16.03.2015
                // @formatter:on
                .section("date")
                .match("Datum([\\s]+)?: ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Kurs           : 30,070360 EUR          Devisenkurs    :          1,000000
                // @formatter:on
                .section("amount", "currency")
                .match("^Kurs([\\s]+)?: ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")) * t.getShares() / Values.Share.factor());
                })

                // If the taxes are negative,
                // this is a tax refund transaction
                // and we subtract this from the amount and reset this.

                // @formatter:off
                //                                           Einbeh. Steuer**:          -1,00 EUR
                // @formatter:on
                .section("taxRefund").optional()
                .match("^.* Einbeh\\. Steuer[\\*\\s]+: ([\\s]+)?\\-(?<taxRefund>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> t.setAmount(t.getAmount() - asAmount(v.get("taxRefund"))))

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

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

                // @formatter:off
                // ISHS MSCI EM USD-AC                 IE00BKM4GZ66/A111X9
                // @formatter:on
                .section("wkn", "isin", "name")
                .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6}) *$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                // @formatter:on
                .section("shares")
                .match("^Gesamtbestand ([\\s]+)?(?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Buchungsdatum        11.01.2020
                // Gesamtbestand       476,000000 St.  zum                             31.12.2019
                // @formatter:on
                .section("date")
                .match("Gesamtbestand .* zum ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(type.getCurrentContext().get("date") != null ? type.getCurrentContext().get("date") : v.get("date")));
                })

                // @formatter:off
                //                                  ** Einbeh. Steuer                    4,69 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^.* Einbeh\\. Steuer ([\\s]+)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode("currency"));
                })

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    if (t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^.* Auftragsdatum .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // @formatter:off
                // Nr.123441244/1  Kauf            C.S.-MSCI PACIF.T.U.ETF I (LU0392495023/ETF114)
                // Kurs           4,424000 EUR           Kurswert       EUR             44,24
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Nr.76443716/1  Verkauf           UBS AG LONDON 14/16 RWE (DE000US9RGR9/US9RGR)
                // Kurs           29,0000 %               Kurswert       EUR             7.250,00
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?(Kauf|Verkauf) ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^.* Kurswert([\\s]+)? ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Ausgeführt     25.000,00000 EUR
                // Ausgeführt    :              29 St.     Kurswert      :             751,68 EUR
                // Ausgeführt     10 St.
                // Ausgeführt     19,334524 St.           Kurswert       EUR             1.050,00
                // @formatter:on
                .section("shares", "notation")
                .match("^Ausgef.hrt ([:\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|Stk|[\\w]{3})).*$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").startsWith("St"))
                    {
                        BigDecimal shares = asBigDecimal(v.get("shares"));
                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                // @formatter:off
                // An den  Ausführungszeit   13:59 Häusern 5 
                // ZZZZ ZZ Ausführungszeit    17:30 Uhr. 
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                // @formatter:on
                .section("time").optional()
                .match("^.* Ausf.hrungszeit ([\\s]+)?(?<time>[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // @formatter:off
                // Max Mustermann Schlusstag        03.12.2015o
                // ZZZZZ ZZZZ Handelstag         10.04.2019xan
                // Max Mu Stermann Schlusstag        17.01.2019u Ausführungszeit   17:52 Uhr
                // @formatter:on
                .section("date")
                .match("^.* (Handelstag|Schlusstag) ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                // If the currency of the tax differs from the amount,
                // it will be converted and reset.
                .oneOf(
                                // @formatter:off
                                // Endbetrag      EUR               -50,30
                                // @formatter:on
                                section -> section
                                        .attributes("currency")
                                        .match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?[\\.,\\d]+$")
                                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))
                                ,
                                // @formatter:off
                                //        Endbetrag                   -52,50 EUR
                                // Endbetrag     :            -760,09 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("currency")
                                        .match("^(.* )?Endbetrag([:\\s]+)? (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))
                        )

                .optionalOneOf(
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? \\-(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(amount);
                                            }
                                            else
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                t.setAmount(asAmount(v.get("fxAmount")));
                                            }
                                })
                                ,
                                // @formatter:off
                                // Gewinn/Verlust 0,00 EUR              **Einbeh. Steuer EUR                -1,00
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                t.setAmount(asAmount(v.get("amount")));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                                // Devisenkurs   :        1,000000         Eigene Spesen :               0,00 EUR
                                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :             -305,85 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(amount);
                                            }
                                            else
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                t.setAmount(asAmount(v.get("fxAmount")));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Gewinn/Verlust            0,00 EUR    **Einbeh. Steuer                -1,00 EUR
                                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :            -305,85 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* [\\*]+Einbeh\\. (Steuer|KESt)([:\\s]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                t.setAmount(asAmount(v.get("amount")));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // Since it is a tax refund, we record it. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negativeTax");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addSummaryStatementTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?Verkauf ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // @formatter:off
                // Nr.60797017/1  Verkauf             HANN.RUECK SE NA O.N. (DE0008402215/840221)
                // Kurs         : 59,4890 EUR             Kurswert       :           5.948,90 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?Verkauf ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // @formatter:on
                .section("shares").optional()
                .match("^davon ausgef\\.([:\\s]+)? (?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                // @formatter:on
                .section("date", "time").optional()
                .match("^.* Schlusstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                // If the currency of the tax differs from the amount, 
                // it will be converted and reset.

                // @formatter:off
                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                // @formatter:on
                .section("currency").optional()
                .match("^.* Endbetrag([:\\s]+)? (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :
                                // Valuta        : 02.12.2020            **Einbeh. Steuer:              -0,84 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs ([\\s]+)?: (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? \\-(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (!t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(amount);
                                            }
                                            else
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                t.setAmount(asAmount(v.get("fxAmount")));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Lagerland    : Deutschland           **Einbeh. Steuer :            -100,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* [\\*]+Einbeh\\. Steuer([:\\s]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("negativeTax", "X");

                                            if (t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                t.setAmount(asAmount(v.get("amount")));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // Since it is a tax refund, we record it. 
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negativeTax");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addSummaryStatementFeesBlock(DocumentType type)
    {
        Block block = new Block("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?Verkauf ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                // @formatter:off
                // Nr.60797017/1  Verkauf             HANN.RUECK SE NA O.N. (DE0008402215/840221)
                // Kurs         : 59,4890 EUR             Kurswert       :           5.948,90 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency").optional()
                .match("^Nr\\.[\\d]+\\/[\\d]+ ([\\s]+)?Verkauf ([\\s]+)?(?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6})\\)$")
                .match("^Kurs([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // @formatter:on
                .section("shares").optional()
                .match("^davon ausgef\\.([:\\s]+)? (?<shares>[\\.,\\d]+) St\\. .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // davon ausgef.: 150,00 St.              Schlusstag     :  28.01.2014, 12:50 Uhr
                // davon ausgef. : 540,00 St.              Schlusstag    :      09.04.2019, 16:52
                // @formatter:on
                .section("date", "time").optional()
                .match("^.* Schlusstag([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                // If the currency of the fee differs from the amount, 
                // it will be converted and reset.

                // @formatter:off
                // Valuta       : 30.01.2014              Endbetrag      :          -5.893,10 EUR
                // @formatter:on
                .section("negative", "currency").optional()
                .match("^.* Endbetrag([:\\s]+)? (?<negative>(\\-)?[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    if (v.get("negative").startsWith("-"))
                        type.getCurrentContext().put("negative", "X");
                })

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :              5,90 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).* Provision([:\\s]+)? (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(amount);
                                            }
                                            else if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                t.setAmount(asAmount(v.get("fxAmount")));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Devisenkurs   :                         Provision     :               5,90 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* Provision([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                t.setAmount(asAmount(v.get("amount")));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision      EUR                 5,90
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxCurrency", "fxAmount")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).* Provision([:\\s]+)? (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(amount);
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Devisenkurs                            Provision      EUR                 5,90
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.* Provision([:\\s]+)? (?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                t.setAmount(asAmount(v.get("amount")));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :              5,90 EUR
                                // Verwahrart    : GS-Verwahrung           Eigene Spesen :               1,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* Eigene Spesen([:\\s]+)? (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                            else if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Verwahrart    : GS-Verwahrung           Eigene Spesen :               1,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* Eigene Spesen([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision  EUR                    5,90
                                // Verwahrart    : GS-Verwahrung           Eigene Spesen  EUR                1,00
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxCurrency", "fxAmount")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* Eigene Spesen([:\\s]+)? (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                            else if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Verwahrart    : GS-Verwahrung            Eigene Spesen  EUR                1,00
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.* Eigene Spesen([:\\s]+)? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision     :              5,90 EUR
                                // Verwahrart    : GS-Verwahrung          *Fremde Spesen :               1,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxAmount", "fxCurrency")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* \\*Fremde Spesen([:\\s]+)? (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                            else if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Verwahrart    : GS-Verwahrung          *Fremde Spesen :               1,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* \\*Fremde Spesen([:\\s]+)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs   : 1,192200(x)             Provision  EUR                    5,90
                                // Verwahrart    : GS-Verwahrung          *Fremde Spesen  EUR                1,00
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "fxCurrency", "fxAmount")
                                        .match("^Devisenkurs([:\\s]+)? (?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^.* \\*Fremde Spesen([:\\s]+)? (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && !t.getCurrencyCode().contentEquals(v.get("fxCurrency")))
                                            {
                                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));

                                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                Money amount = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(fxAmount.getAmount())
                                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                            else if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Verwahrart    : GS-Verwahrung          *Fremde Spesen   EUR                1,00
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.* \\*Fremde Spesen([:\\s]+)? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")) && t.getCurrencyCode().contentEquals(v.get("currency")))
                                            {
                                                Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                            }
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    // Because the final amount was negative, we record
                    // the fees separately in this transaction.
                    // Finally, we remove the flag.
                    type.getCurrentContext().remove("negative");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTransferInOutTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^(Depoteingang|Depotausgang|Bestandsausbuchung|Gutschrifts\\- \\/ Belastungsanzeige).*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // @formatter:off
                // Depotausgang                          COMMERZBANK INLINE09EO/SF (DE000CM31SV9)
                // Bestandsausbuchung                       COMMERZBANK PUT10 EOLS (DE000CB81KN1)
                // Depoteingang                            UBS AG LONDON 14/16 RWE (DE000US9RGR9)
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .match("^(Depoteingang|Depotausgang|Bestandsausbuchung) ([\\s]+)?(?<name>.*) ([\\s]+)?\\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // WKN ISIN Wertpapierbezeichnung Anzahl
                // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                // @formatter:on
                .section("wkn", "isin", "name", "currency").optional()
                .find("^WKN ([\\s]+)?ISIN ([\\s]+)?Wertpapierbezeichnung ([\\s]+)?Anzahl$")
                .match("^(?<wkn>[A-Z0-9]{6}) ([\\s]+)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\s]+)?(?<name>.*) ([\\s]+)?[\\.,\\d]+$")
                .match("^.* Betrag\\/(Stk|Stck\\.)([:\\s]+)? [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // @formatter:off
                                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                                // St./Nominale      :      310,000000 St.  Bemessungs-
                                // Stk./Nominale**: 2.000,000000 Stk       Einbeh. Steuer*:              0,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares", "notation")
                                        .match("^(St|Stk|Stck)\\.\\/Nominale([*:\\s]+)? (?<shares>[\\.,\\d]+) ([\\s]+)?(?<notation>(St\\.|Stk|[\\w]{3})) .*$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            if (v.get("notation") != null && !v.get("notation").startsWith("St"))
                                            {
                                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                            }
                                            else
                                            {
                                                t.setShares(asShares(v.get("shares")));
                                            }
                                        })
                                ,
                                // @formatter:off
                                // Stck./Nominale:          83,000000        Betrag/Stck.    :           1,34 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(Stk|Stck)\\.\\/Nominale([:\\s]+)? (?<shares>[\\.,\\d]+) .* Betrag\\/Stck\\. .* [\\.,\\d]+ [\\w]{3}$$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // @formatter:off
                // Fälligkeitstag   : 02.12.2009                  Letzter Handelstag:  20.11.2009
                // Datum          : 24.11.2015
                // Fälligkeitstag                                                  25.06.2021
                // Fälligkeitstag: 18.07.2011 letzter Handel am: 11.07.2011
                // @formatter:on
                .section("date").optional()
                .match("^(F.lligkeitstag|Datum)([:\\s]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:           -382,12 EUR
                //                                           Einbeh. Steuer**:         -10,00 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^.* Einbeh\\. Steuer([:\\s\\*]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Einbeh. Steuer**                                                    -88,53 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Einbeh\\. Steuer([:\\s\\*]+)? \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .optionalOneOf(
                                // @formatter:off
                                //   unter der Transaktion-Nr.: 132465978
                                //   unter der Transaktion-Nr. : 1111111111
                                // Transaktionsnummer: 921414163
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(.* )?(?<note>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)? [\\d]+).*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // @formatter:off
                                //     Evtl. Details dazu finden Sie im Steuerreport unter der Transaktion-Nr.:
                                // 1301138113.
                                // @formatter:on
                                section -> section
                                        .attributes("note1", "note2")
                                        .match("^.* (?<note1>(Transaktion\\-Nr\\.|Transaktionsnummer)([:\\s]+)?)$")
                                        .match("^([\\s]+)?(?<note2>[\\d]+)\\.$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))
                        )

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // @formatter:off
        // If the currency of the provision tax differs from the amount, it will
        // be converted and reset.
        //
        // Example:
        // **Einbeh. Steuer:           0,84 EUR
        // Endbetrag     :             955,98 USD
        // @formatter:on
        transaction
                // @formatter:off
                // Quellenst.-satz :           15,00 %    Gez. Quellenst. :            1,15 USD
                // Quellenst.-satz :            15,00 %  Gez. Quellensteuer :           18,28 USD
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^.* Gez\\. (Quellenst\\.|Quellensteuer)([:\\s]+)? (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                })

                // @formatter:off
                // Lagerland    : Deutschland           **Einbeh. Steuer :               0,00 EUR
                // Lagerland    : Deutschland           **Einbeh. Steuer                0,00 EUR
                // Gewinn/Verlust:        1.112,18 EUR   **Einbeh. KESt  :             305,85 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* \\*\\*Einbeh\\. (Steuer|KESt)([:\\s]+)? (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                    {
                        Money tax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                        if (!tax.getCurrencyCode().equals(((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().getCurrencyCode()))
                        {
                            Money fxTax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            tax = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(fxTax.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());
                        }

                        checkAndSetTax(tax, t, type.getCurrentContext());
                    }
                })

                // @formatter:off
                // Lagerland    : Deutschland           **Einbeh. Steuer EUR               1,00
                // Gewinn/Verlust 0,00 EUR              **Einbeh. KESt   EUR               1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^.* \\*\\*Einbeh\\. (Steuer|KESt)([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                    {
                        Money tax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                        if (!tax.getCurrencyCode().equals(((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().getCurrencyCode()))
                        {
                            Money fxTax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            tax = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(fxTax.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());
                        }

                        checkAndSetTax(tax, t, type.getCurrentContext());
                    }
                })

                // @formatter:off
                //              ***Einbeh. SichSt EUR                1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^.* \\*\\*\\*Einbeh\\. SichSt([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                    {
                        Money tax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                        if (!tax.getCurrencyCode().equals(((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().getCurrencyCode()))
                        {
                            Money fxTax = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("tax")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            tax = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(fxTax.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());
                        }

                        checkAndSetTax(tax, t, type.getCurrentContext());
                    }
                })

                // @formatter:off
                //                                      *Einbeh. Steuer  :       284,85 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* \\*Einbeh\\. Steuer([:\\s]+)? (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Stk./Nominale  : 325,000000 Stk         Einbeh. Steuer*:            382,12 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* Einbeh\\. Steuer\\*([:\\s]+)? (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negativeTax")) && !"X".equals(type.getCurrentContext().get("negative")))
                        processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        // If the currency of the provision fee differs from the amount, it will
        // be converted and reset.
        transaction
                // @formatter:off
                // Devisenkurs  :                         Provision      :               3,90 EUR
                // Kurs                 20,835000 EUR      Provision                     5,90 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Provision([:\\s]+)? (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        Money fee = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                        if (!fee.getCurrencyCode().equals(((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().getCurrencyCode()))
                        {
                            Money fxFee = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            fee = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(fxFee.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());
                        }

                        checkAndSetFee(fee, t, type.getCurrentContext());   
                    }
                })

                // @formatter:off
                // Devisenkurs                            Provision      EUR                 5,90
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Provision([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        Money fee = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                        if (!fee.getCurrencyCode().equals(((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().getCurrencyCode()))
                        {
                            Money fxFee = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("fee")));

                            BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));

                            fee = Money.of(((BuySellEntry) t).getPortfolioTransaction().getCurrencyCode(),
                                            BigDecimal.valueOf(fxFee.getAmount()).multiply(exchangeRate)
                                                .setScale(0, RoundingMode.HALF_UP).longValue());
                        }

                        checkAndSetFee(fee, t, type.getCurrentContext());   
                    }
                })

                // @formatter:off
                // Bew-Faktor   : 1,0000                  Eigene Spesen  :               1,00 EUR
                // Verwahrart     Wertpapierrechnung      Eigene Spesen                 2,71 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Eigene Spesen([:\\s]+)? (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Bew-Faktor   : 1,0000                  Eigene Spesen  EUR                 1,00
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Eigene Spesen([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  :               1,00 EUR
                // Verwahrart     Wertpapierrechnung      *Fremde Spesen                 2,71 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* \\*Fremde Spesen([:\\s]+)? (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Verwahrart   : GS-Verwahrung          *Fremde Spesen  EUR                 1,00
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* \\*Fremde Spesen([:\\s]+)? (?<currency>[\\w]{3})([\\s]+)? (?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Fremdgeb./Stück :            0,02 USD  Gesamtgebühr    :            0,10 USD
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Gesamtgeb.hr([:\\s]+)? (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                });
    }
}

package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractor extends AbstractPDFExtractor
{
    public TradeRepublicPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TRADE REPUBLIC"); //$NON-NLS-1$

        addBuySellTransaction();
        addSellWithNegativeAmountTransaction();
        addLiquidationTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
        addTaxStatementTransaction();
        addInterestStatementTransaction();
        addAdvanceTaxTransaction();
        addCaptialReductionTransaction();
        addDeliveryInOutBoundTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Trade Republic Bank GmbH"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(((Limit|Stop\\-Market|Market)\\-Order )?"
                        + "(Kauf"
                        + "|Verkauf"
                        + "|Sparplanausf.hrung"
                        + "|Ex.cution de l.investissement programm.) .*"
                        + "|REINVESTIERUNG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("(((Limit|Stop\\-Market|Market)\\-Order )?"
                        + "(Kauf"
                        + "|Verkauf"
                        + "|Sparplanausf.hrung"
                        + "|Ex.cution de l.investissement programm.) .*"
                        + "|REINVESTIERUNG)");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^((Limit|Stop\\-Market|Market)\\-Order )?(?<type>(Kauf|Verkauf|Sparplanausf.hrung|Ex.cution de l.investissement programm.)) .*$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                // Registered Shares o.N.
                                // AU000000CUV3
                                // ISIN: DE000A3H23V7
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin", "nameContinued")
                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,

                                // This is for the reinvestment of dividends
                                // We pick the second 

                                // @formatter:off
                                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                // 2 Reinvestierung Vodafone Group PLC 22 Stk.
                                // Registered Shares DL 0,2095238
                                // GB00BH4HKS39
                                // 1 Bruttoertrag 26,80 GBP
                                // 2 Barausgleich 0,37 GBP
                                // @formatter:on
                                section -> section
                                        .attributes("name", "nameContinued", "isin", "currency")
                                        .match("^[\\d] Reinvestierung .* [\\.,\\d]+ Stk\\.$")
                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^[\\d] Bruttoertrag [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                .oneOf(
                                // @formatter:off
                                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                // Tencent Holdings Ltd. 0,3773 titre(s) 53,00 EUR 20,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)) .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                // 2 Reinvestierung Vodafone Group PLC 22 Stk.
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                                        .match("^[\\d] Reinvestierung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                                // Verkauf am 26.02.2021, um 11:44 Uhr.
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^((Limit|Stop\\-Market|Market)\\-Order )?(Kauf|Verkauf) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Exécution de l'investissement programmé le 17/01/2022 sur le Lang & Schwarz Exchange.
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Ex.cution de l.investissement programm. .* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Sparplanausführung am 18.11.2019 an der Lang & Schwarz Exchange.
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Sparplanausf.hrung .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,

                                // This is for the reinvestment of
                                // dividends

                                // @formatter:off
                                // DE40110101001234567890 06.08.2021 0,44 GBP
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // If the type of transaction is "SELL" and the amount
                // is negative, then the gross amount set. Fees are
                // processed in a separate transaction.

                // @formatter:off
                // GESAMT 1.395,60 EUR
                // GESAMT -1.396,60 EUR
                // @formatter:on
                .section("negative").optional()
                .match("GESAMT (\\-)?[\\.,\\d]+ [\\w]{3}")
                .match("GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getType().isLiquidation())
                        type.getCurrentContext().put("negative", "X");
                })

                .oneOf(
                                // @formatter:off
                                // There might be two lines with "GESAMT" or "TOTAL"
                                // - one for gross
                                // - one for the net value 
                                // we pick the second
                                // @formatter:on

                                // @formatter:off
                                // GESAMT 1.825,60 EUR
                                // GESAMT 1.792,29 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency", "gross", "grossCurrency")
                                        .match("^(GESAMT|TOTAL) (\\-)?(?<gross>[\\.,\\d]+) (?<grossCurrency>[\\w]{3})$")
                                        .match("^(GESAMT|TOTAL) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if ("X".equals(type.getCurrentContext().get("negative")))
                                            {
                                                t.setAmount(asAmount(v.get("gross")));
                                                t.setCurrencyCode(asCurrencyCode(v.get("grossCurrency")));
                                            }
                                            else
                                            {
                                                t.setAmount(asAmount(v.get("amount")));
                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));                                                
                                            }
                                        })
                                ,
                                // In case there is no tax,
                                // only one line with "GESAMT"
                                // exists and we need to grab data from that line

                                // @formatter:off
                                // GESAMT 1.792,29 EUR
                                // TOTAL 20,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^(GESAMT|TOTAL) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // This is for the reinvestment of dividends. We
                // subtract the second amount from the first amount and
                // set this.

                // @formatter:off
                // 1 Bruttoertrag 26,80 GBP
                // 2 Barausgleich 0,37 GBP
                // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                // @formatter:on
                .section("gross", "grossCurrency", "cashCompensation", "cashCompensationCurrency", "exchangeRate", "baseCurrency", "fxCurrency", "currency").optional()
                .match("^[\\d] Bruttoertrag (?<gross>[\\.,\\d]+) (?<grossCurrency>[\\w]{3})$")
                .match("^[\\d] Barausgleich (?<cashCompensation>[\\.,\\d]+) (?<cashCompensationCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<fxCurrency>[\\w]{3}) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    Money grossValueBasis = Money.of(asCurrencyCode(v.get("grossCurrency")), asAmount(v.get("gross")));
                    Money cashCompensationValue = Money.of(asCurrencyCode(v.get("cashCompensationCurrency")), asAmount(v.get("cashCompensation")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = grossValueBasis.subtract(cashCompensationValue);
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    t.setAmount(gross.getAmount());
                    t.setCurrencyCode(asCurrencyCode(gross.getCurrencyCode()));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // If the tax is optimized, this is a tax refund
                // transaction and we subtract this from the amount and
                // reset this.

                // @formatter:off
                // Kapitalertragssteuer Optimierung 20,50 EUR
                // Kapitalertragsteuer Optimierung 4,56 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Kapitalertrag(s)?steuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                })

                // @formatter:off
                // Solidaritätszuschlag Optimierung 1,13 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                })

                // @formatter:off
                // Kirchensteuer Optimierung 9,84 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                })

                .wrap(t -> {
                    // If we have multiple entries in the document,
                    // then the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addBuySellTaxReturnBlock(type);
    }

    private void addLiquidationTransaction()
    {
        DocumentType type = new DocumentType("TILGUNG");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^TILGUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // TurboC O.End Linde
                // DE000TT22GS8
                // 1 Kurswert 0,70 EUR
                // @formatter:on
                .section("name", "isin", "nameContinued")
                .match("^[\\d] Tilgung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^[\\d] Kurswert [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // @formatter:on
                .section("shares")
                .match("^[\\d] Tilgung .* (?<shares>[\\.,\\d]+) Stk\\.$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // DE0000000000000000000 02.10.2020 33,89 EUR
                // @formatter:on
                .section("date")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // SUMME 0,70 EUR
                // GESAMT 0,25 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^(SUMME|GESAMT) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addLiquidationTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(AUSSCH.TTUNG|DIVIDENDE|REINVESTIERUNG|STORNO DIVIDENDE)");
        this.addDocumentTyp(type);

        Block block = new Block("^(AUSSCH.TTUNG|DIVIDENDE|REINVESTIERUNG|STORNO DIVIDENDE)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Storno der Dividende mit dem Ex-Tag 29.11.2019.
                // @formatter:on
                .section("type").optional()
                .match("^(?<type>Storno) der Dividende .*$")
                .assign((t, v) -> v.getTransactionContext().put(FAILURE,
                                Messages.MsgErrorOrderCancellationUnsupported))

                .oneOf(
                                // @formatter:off
                                // iShsV-EM Dividend UCITS ETF 10 Stk. 0,563 USD 5,63 USD
                                // Registered Shares USD o.N.
                                // IE00B652H904
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin", "nameContinued")
                                        .match("^(?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                // Registered Shares DL 0,2095238
                                // GB00BH4HKS39
                                // 1 Bruttoertrag 26,80 GBP
                                // @formatter:on
                                section -> section
                                        .attributes("name", "nameContinued", "isin", "currency")
                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^[\\d] Reinvestierung .*$")
                                        .match("^[\\d] Bruttoertrag [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                .oneOf(
                                // @formatter:off
                                // iShsV-EM Dividend UCITS ETF 10 Stk. 0,563 USD 5,63 USD
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^.* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[\\d] Reinvestierung .* (?<shares>[\\.,\\d]+) Stk\\.$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // @formatter:off
                // DExxxxxx 25.09.2019 4,18 EUR
                // @formatter:on
                .section("date")
                .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                // 1 Bruttoertrag 26,80 GBP
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^[\\d] Reinvestierung .*$")
                                        .match("^[\\d] Bruttoertrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // There might be two lines with "GESAMT"
                                // - one for gross
                                // - one for the net value 
                                // we pick the second
                                // @formatter:on

                                // @formatter:off
                                // GESAMT 3,83 EUR
                                // GESAMT 2,83 EUR
                                // GESAMT -30,17 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^GESAMT [\\.,\\d]+ [\\w]{3}$")
                                        .match("^GESAMT (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // GESAMT 1,630 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^GESAMT (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // GESAMT 5,63 USD
                                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "baseCurrency", "termCurrency", "currency")
                                        .match("^GESAMT (\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // 1 Bruttoertrag 26,80 GBP
                                // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                                // GESAMT 0,44 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "baseCurrency", "termCurrency", "currency")
                                        .match("^[\\d] Bruttoertrag (\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                                        .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            t.setAmount(gross.getAmount());
                                            t.setCurrencyCode(asCurrencyCode(gross.getCurrencyCode()));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (ctx.getString(FAILURE) != null)
                        item.setFailureMessage(ctx.getString(FAILURE));

                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    
                    return item;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addCaptialReductionTransaction()
    {
        DocumentType type = new DocumentType("KAPITALREDUKTION");
        this.addDocumentTyp(type);

        Block block = new Block("^KAPITALREDUKTION$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                // Registered Shares o.N.
                // CA0679011084
                // @formatter:on
                .section("name", "nameContinued", "isin", "currency")
                .match("^[\\d] Kapitalmaßnahme (?<name>.*) [\\.,\\d]+ Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^[\\d] Barausgleich [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                // @formatter:on
                .section("shares")
                .match("^[\\d] Kapitalmaßnahme .* (?<shares>[\\.,\\d]+) Stk\\.$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // DE12345689234567671 15.06.2021 0,71 EUR
                // @formatter:on
                .section("date")
                .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // GESAMT 1,630 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^GESAMT (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // 1 Barausgleich 1,18 USD
                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                // @formatter:on
                .section("fxGross", "fxCurrency","exchangeRate", "baseCurrency", "termCurrency", "gross", "currency").optional()
                .match("^[\\d] Barausgleich (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }
    
    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("(UMTAUSCH\\/BEZUG"
                        + "|FUSION"
                        + "|KAPITALERH.HUNG GEGEN BAR"
                        + "|VERGLEICHSVERFAHREN"
                        + "|DEPOTÜBERTRAG EINGEHEND"
                        + "|TITELUMTAUSCH)", (context, lines) -> {
            Pattern pDate = Pattern.compile("^(.*) DATUM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            Pattern pSkipTransaction = Pattern.compile("^ABRECHNUNG$");
            Pattern pTransactionPosition = Pattern.compile("^(?<transactionPosition>[\\d]) (Barausgleich|Kurswert) (\\-)?[\\.,\\d]+ [\\w]{3}$");
            context.put("skipTransaction", Boolean.FALSE.toString());
            context.put("transactionPosition", "0");

            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                    context.put("date", m.group("date"));

                // If we have a "ABRECHNUNG", then this is not a
                // delivery in/outbond.
                m = pSkipTransaction.matcher(line);
                if (m.matches())
                    context.put("skipTransaction", Boolean.TRUE.toString());

                m = pTransactionPosition.matcher(line);
                if (m.matches() && Boolean.parseBoolean(context.get("skipTransaction")))
                    context.put("transactionPosition", m.group("transactionPosition"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction1 = new Transaction<>();
        pdfTransaction1.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("(^|^[\\d] )(Umtausch\\/Bezug"
                        + "|Einbuchung"
                        + "|Ausbuchung"
                        + "|Kapitalerh.hung gegen Bar"
                        + "|VERGLEICHSVERFAHREN"
                        + "|Depotübertrag eingehend"
                        + "|TITELUMTAUSCH) .* "
                        + "([\\.,\\d]+ Stk\\.|am [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction1);

        pdfTransaction1
                // Is type --> "Einbuchung" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                .section("type").optional()
                .match("(^|^[\\d] )(?<type>"
                                + "(Umtausch\\/Bezug"
                                + "|Einbuchung"
                                + "|Ausbuchung"
                                + "|Kapitalerh.hung gegen Bar"
                                + "|VERGLEICHSVERFAHREN"
                                + "|Depotübertrag eingehend"
                                + "|TITELUMTAUSCH)"
                                + ") .* ([\\.,\\d]+ Stk\\.|am [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.)$")
                .assign((t, v) -> {
                    if ("Einbuchung".equals(v.get("type")) || "Kapitalerhöhung gegen Bar".equals(v.get("type")) || "Depotübertrag eingehend".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                })

                // @formatter:off
                // 1 Umtausch/Bezug Nordex SE 47 Stk.
                // Inhaber-Aktien o.N.
                // DE000A0D6554
                // @formatter:on
                .section("position", "name", "shares", "nameContinued", "isin").optional()
                .match("^(?<position>[\\d]) (Umtausch\\/Bezug|Einbuchung|Ausbuchung|Depotübertrag eingehend) (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    context.put("position", v.get("position"));
                    t.setDateTime(asDate(context.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                    t.setAmount(0L);

                    if (v.get("position").equals(context.get("transactionPosition")))
                    {                       
                        type.getCurrentContext().put("name", v.get("name"));
                        type.getCurrentContext().put("nameContinued", v.get("nameContinued"));
                        type.getCurrentContext().put("isin", v.get("isin"));
                        type.getCurrentContext().put("shares", v.get("shares"));
                    }
                })

                .section("name", "nameContinued", "date", "currency", "isin", "shares").optional()
                .match("^VERH.LTNIS3 VERH.LTNIS4 PREIS5 FRIST6$")
                .match("^(?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^ISIN: [\\w]{12} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\– [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\– [\\.,\\d]+ Stk\\. [\\.,\\d]+ : [\\.,\\d]+ [\\.,\\d]+ : [\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^Bezugsrecht: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^Anzahl eingebuchter $")
                .match("^Bezugsrechte: (?<shares>[\\.,\\d]+) Stk\\.$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                .wrap(t -> {
                    // If we have a "ABRECHNUNG", then this is not a
                    // delivery in/outbond. We skip this transaction.
                    Map<String, String> context = type.getCurrentContext();
                    boolean skipTransactions = Boolean.parseBoolean(context.get("skipTransaction"));

                    if (skipTransactions && context.get("transactionPosition").equals(context.get("position")))
                        return null;
                    else
                        return new TransactionItem(t);
                });

        // If we have a "ABRECHNUNG".
        Transaction<BuySellEntry> pdfTransaction2 = new Transaction<>();
        pdfTransaction2.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block buySellBlock = new Block("^ABRECHNUNG$");
        type.addBlock(buySellBlock);
        buySellBlock.set(pdfTransaction2);

        pdfTransaction2
                // Is type --> "Barausgleich" change from BUY to SELL
                .section("type").optional()
                .match("^[\\d] (?<type>Barausgleich|Kurswert) (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if ("Barausgleich".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // 1 Kurswert -643,90 EUR
                // DE00000000000000000000 15.07.2021 -648,90 EUR
                // 1 Barausgleich 267,90 USD
                // DEXXXXXXXXXXXXXXXXXXXX 22.07.2021 163,68 EUR
                // @formatter:on
                .section("position", "amount", "currency", "date")
                .match("^(?<position>[\\d]) (Barausgleich|Kurswert) (\\-)?[\\.,\\d]+ [\\w]{3}$")
                .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    // Check if the position are equal
                    if (v.get("position").equals(context.get("transactionPosition")))
                    {
                        v.put("name", context.get("name"));
                        v.put("nameContinued", context.get("nameContinued"));
                        v.put("isin", context.get("isin"));
                        t.setSecurity(getOrCreateSecurity(v));

                        t.setDate(asDate(v.get("date")));
                        t.setShares(asShares(context.get("shares")));
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() > 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addTaxesSectionsTransaction(pdfTransaction2, type);
        addFeesSectionsTransaction(pdfTransaction2, type);
    }

    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern currency = Pattern.compile("BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN (?<currency>[\\w]{3})");

            for (String line : lines)
            {
                Matcher m = currency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (Accepted PayIn|Einzahlung akzeptiert).*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (Accepted PayIn|Einzahlung akzeptiert):.* (to|auf).* (?<amount>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                        })

                        .wrap(TransactionItem::new));

        Block taxRefundBlock = new Block("([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Steueroptimierung.*");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}-[\\d]{2}-[\\d]{2})) Steueroptimierung.* (?<amount>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addTaxStatementTransaction()
    {
        DocumentType type = new DocumentType("STEUERABRECHNUNG");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAX_REFUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^STEUERABRECHNUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Kapitalertragsteuer Optimierung 3,75 EUR
                // @formatter:on
                .section("amount", "currency", "date")
                .match("^Kapitalertrags(s)?teuer Optimierung [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .find("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                .wrap(TransactionItem::new);
    }

    private void addInterestStatementTransaction()
    {
        DocumentType type = new DocumentType("ABRECHNUNG ZINSEN");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        Block firstRelevantLine = new Block("^ABRECHNUNG ZINSEN$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // zum 31.01.2023
                // @formatter:on
                .section("date")
                .match("^zum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // DE10123456789123456789 01.02.2023 0,88 EUR
                // @formatter:on
                .section("currency", "amount")
                .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
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

        Block firstRelevantLine = new Block("^VORABPAUSCHALE$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // iShs Core MSCI EM IMI U.ETF 173,3905 Stk.
                // Registered Shares o.N.
                // ISIN: IE00BKM4GZ66
                // @formatter:on
                .section("name", "isin", "shares", "nameContinued")
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // @formatter:off
                // VERRECHNUNGSKONTO VALUTA BETRAG
                // DE12345678912345678912 04.01.2021 -0,32 EUR
                // @formatter:on
                .section("date", "amount", "currency")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap((t) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    
                    return item;
                });
    }

    private void addBuySellTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^((Limit|Stop\\-Market|Market)\\-Order )?(Kauf|Verkauf) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // @formatter:off
                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // Registered Shares o.N.
                // AU000000CUV3
                // @formatter:on
                .section("name", "currency", "nameContinued", "isin")
                .match("^(?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // @formatter:on
                .section("shares")
                .match("^.* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                // Verkauf am 26.02.2021, um 11:44 Uhr.
                // @formatter:on
                .section("date", "time")
                .match("^((Limit|Stop\\-Market|Market)\\-Order )?(Kauf|Verkauf) .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                // @formatter:off
                // Kapitalertragssteuer Optimierung 20,50 EUR
                // Kapitalertragsteuer Optimierung 4,56 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Kapitalertrag(s)?steuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Solidaritätszuschlag Optimierung 1,13 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                })

                // @formatter:off
                // Kirchensteuer Optimierung 9,84 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addLiquidationTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^TILGUNG$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // @formatter:off
                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // TurboC O.End Linde
                // DE000TT22GS8
                // @formatter:on
                .section("name", "nameContinued", "isin")
                .match("^[\\d] Tilgung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // @formatter:on
                .section("shares")
                .match("^[\\d] Tilgung .* (?<shares>[\\.,\\d]+) Stk\\.$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // DE0000000000000000000 02.10.2020 33,89 EUR
                // @formatter:on
                .section("date")
                .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // 2 Kapitalertragssteuer Optimierung 29,24 EUR
                // 2 Kapitalertragsteuer Optimierung 1,00 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^([\\d] )?Kapitalertrag(s)?steuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Solidaritätszuschlag Optimierung 1,61 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                })

                // @formatter:off
                // Kirchensteuer Optimierung 2,34 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addSellWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^(.*\\-Order )?Verkauf.*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        });

        pdfTransaction
                // @formatter:off
                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // Registered Shares o.N.
                // AU000000CUV3
                // ISIN: DE000A3H23V7
                // @formatter:on
                .section("name", "currency", "nameContinued", "isin")
                .match("^(?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // @formatter:on
                .section("shares")
                .match("^.* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                // Verkauf am 26.02.2021, um 11:44 Uhr.
                // @formatter:on
                .section("date", "time")
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                // @formatter:off
                // GESAMT 0,34 EUR
                // GESAMT -0,66 EUR
                // @formatter:on
                .section("negative").optional()
                .match("^GESAMT [\\.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> type.getCurrentContext().put("negative", "X"))

                // @formatter:off
                // GESAMT -0,66 EUR
                // @formatter:on
                .section("negative").optional()
                .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        type.getCurrentContext().put("negative", "X");
                })

                // @formatter:off
                // Fremdkostenzuschlag -1,00 EUR
                // @formatter:on
                .section("currency", "amount").optional()
                .match("^Fremdkostenzuschlag \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                .wrap(t -> {
                    // If we have multiple entries in the document,
                    // then the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Quellensteuer DE für US-Emittent -7,56 USD
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^([\\d] )?Quellensteuer .* (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Quellensteuer -12,00 USD
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^([\\d] )?Quellensteuer (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Kapitalertragssteuer -30,63 EUR
                // Kapitalertragsteuer -8,36 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^([\\d] )?Kapitalertrags(s)?teuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Solidaritätszuschlag -1,68 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^([\\d] )?Solidarit.tszuschlag (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kirchensteuer -1,68 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^([\\d] )?Kirchensteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Frz. Finanztransaktionssteuer -3,00 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* Finanztransaktionssteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kapitalertragsteuer Optimierung 0,360 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kapitalertrag(s)?steuer Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (v.getTransactionContext().get(FAILURE) != null)
                        processTaxEntries(t, v, type);     
                })

                // @formatter:off
                // Solidaritätszuschlag Optimierung 0,360 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (v.getTransactionContext().get(FAILURE) != null)
                        processTaxEntries(t, v, type);     
                })

                // @formatter:off
                // Kirchensteuer Optimierung 0,360 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (v.getTransactionContext().get(FAILURE) != null)
                        processTaxEntries(t, v, type);     
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Fremdkostenzuschlag -1,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Fremdkostenzuschlag \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Gebühr Kundenweisung -5,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Geb.hr Kundenweisung \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Fremde Spesen -0,12 USD
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Fremde Spesen \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
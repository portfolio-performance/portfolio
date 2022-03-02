package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
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
        DocumentType type = new DocumentType("(((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf|Sparplanausf.hrung) .*|REINVESTIERUNG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf|Sparplanausf.hrung) .*|REINVESTIERUNG)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^((Limit|Stop-Market|Market)-Order )?(?<type>(Kauf|Verkauf|Sparplanausf.hrung)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }

                    /***
                     * If we have multiple entries in the document,
                     * then the "negative" flag must be removed.
                     */
                    type.getCurrentContext().remove("negative");
                })

                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // Registered Shares o.N.
                // AU000000CUV3
                // ISIN: DE000A3H23V7
                .section("name", "shares", "currency", "isin", "nameContinued").optional()
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                /***
                 * This is for the reinvestment of dividends
                 * We pick the second 
                 */

                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                // 2 Reinvestierung Vodafone Group PLC 22 Stk.
                // Registered Shares DL 0,2095238
                // GB00BH4HKS39
                // 1 Bruttoertrag 26,80 GBP
                // 2 Barausgleich 0,37 GBP
                .section("name", "shares", "nameContinued", "isin", "amount1", "amount2", "currency").optional()
                .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$")
                .match("^[\\d] Reinvestierung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12})$")
                .match("^[\\d] Bruttoertrag (?<amount1>[\\.,\\d]+) [\\w]{3}$")
                .match("^[\\d] Barausgleich (?<amount2>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));

                    t.setAmount(asAmount(v.get("amount1")) - asAmount(v.get("amount2")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .oneOf(
                                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                                // Verkauf am 26.02.2021, um 11:44 Uhr.
                                section -> section
                                        .attributes("date", "time")
                                        .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Sparplanausführung am 18.11.2019 an der Lang & Schwarz Exchange.
                                section -> section
                                        .attributes("date")
                                        .match("^Sparplanausf.hrung .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                /***
                                 * This is for the reinvestment of dividends
                                 */
                                // DE40110101001234567890 06.08.2021 0,44 GBP
                                section -> section
                                        .attributes("date")
                                        .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                /***
                 * If the type of transaction is "SELL" 
                 * and the amount is negative, 
                 * then the gross amount set.
                 * 
                 * Fees are processed in a separate transaction
                 */
                .section("negative").optional()
                .match("GESAMT (\\-)?[\\.,\\d]+ [\\w]{3}")
                .match("GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}")
                .assign((t, v) -> {
                    if (t.getPortfolioTransaction().getType().isLiquidation())
                    {
                        type.getCurrentContext().put("negative", "X");
                    }
                })

                /***
                 * There might be two lines with "GESAMT"
                 * - one for gross
                 * - one for the net value 
                 * we pick the second
                 */

                // GESAMT 1.825,60 EUR
                // GESAMT 1.792,29 EUR
                .section("fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^GESAMT (\\-)?(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^GESAMT (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setAmount(asAmount(v.get("fxAmount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                    }
                })

                /***
                 * in case there is no tax,
                 * only one line with "GESAMT"
                 * exists and we need to grab data from that line
                 */

                // GESAMT 1.792,29 EUR
                .section("amount", "currency")
                .match("^GESAMT (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    // if amount is already set, we do nothing
                    if (t.getPortfolioTransaction().getAmount() == 0L)
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                /***
                 * This is for the reinvestment of dividends
                 * 
                 * We subtract the second amount from 
                 * the first amount and set this,
                 */

                // 1 Bruttoertrag 26,80 GBP
                // 2 Barausgleich 0,37 GBP
                // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                .section("amount1", "amount2", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^[\\d] Bruttoertrag (?<amount1>[\\.,\\d]+) [\\w]{3}$")
                .match("^[\\d] Barausgleich (?<amount2>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})\\/[\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                    RoundingMode.HALF_DOWN);

                    // gross given in forex currency
                    long gross = asAmount(v.get("amount1")) - asAmount(v.get("amount2"));
                    long amount = reverseRate.multiply(BigDecimal.valueOf(gross))
                                    .setScale(0, RoundingMode.HALF_DOWN).longValue();

                    // set amount in account currency
                    Money fxAmount = Money.of(forex, amount);

                    t.setAmount(fxAmount.getAmount());
                    t.setCurrencyCode(asCurrencyCode(forex));

                    // create a Unit only, 
                    // if security and transaction currency are different
                    if (!t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(forex, amount),
                                        Money.of(asCurrencyCode(v.get("currency")), gross), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                /***
                 * If the tax is optimized, 
                 * this is a tax refund transaction
                 * and we subtract this from the amount and reset this.
                 * 
                 * If changes are made in this area, 
                 * the tax refund function must be adjusted.
                 * addBuySellTaxReturnBlock(type);
                 */

                // Kapitalertragssteuer Optimierung 20,50 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragssteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Kapitalertragsteuer Optimierung 4,56 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragsteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Solidaritätszuschlag Optimierung 1,13 EUR
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Kirchensteuer Optimierung 9,84 EUR
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

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

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^TILGUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // TurboC O.End Linde
                // DE000TT22GS8
                .section("name", "isin", "shares", "nameContinued")
                .match("^[\\d] Tilgung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // DE0000000000000000000 02.10.2020 33,89 EUR
                .section("date")
                .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // 1 Kurswert 0,70 EUR
                // SUMME 0,70 EUR
                // 1 Kurswert 0,25 EUR
                // GESAMT 0,25 EUR
                .section("amount", "currency")
                .match("^[\\d] Kurswert .*$")
                .match("^(SUMME|GESAMT) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        /***
         * If the tax is optimized, 
         * this is a tax refund transaction.
         * 
         * If changes are made in this area, 
         * the tax refund function must be adjusted.
         * addLiquidationTaxReturnBlock(type);
         */

        addLiquidationTaxReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(AUSSCH.TTUNG|DIVIDENDE|REINVESTIERUNG)");
        this.addDocumentTyp(type);

        Block block = new Block("^(AUSSCH.TTUNG|DIVIDENDE|REINVESTIERUNG)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        pdfTransaction
                // iShsV-EM Dividend UCITS ETF 10 Stk. 0,563 USD 5,63 USD
                // Registered Shares USD o.N.
                // IE00B652H904
                .section("name", "shares", "currency", "isin", "nameContinued").optional()
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                // Registered Shares DL 0,2095238
                // GB00BH4HKS39
                // 1 Bruttoertrag 26,80 GBP
                .section("name", "shares", "nameContinued", "isin", "amount", "currency").optional()
                .match("^[\\d] Reinvestierung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12})$")
                .match("^[\\d] Reinvestierung .*$")
                .match("^[\\d] Bruttoertrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // DExxxxxx 25.09.2019 4,18 EUR
                .section("date")
                .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // GESAMT 1,630 EUR
                .section("amount", "currency").optional()
                .match("^GESAMT (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    // if amount is already set, we do nothing
                    if (t.getAmount() == 0L)
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                })

                /***
                 * There might be two lines with "GESAMT"
                 * - one for gross
                 * - one for the net value 
                 * we pick the second
                 */

                // GESAMT 3,83 EUR
                // GESAMT 2,83 EUR
                .section("amount", "currency").optional()
                .match("^GESAMT [\\.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // GESAMT 4,60 USD
                // Quellensteuer DE für US-Emittent -0,690 USD
                // Zwischensumme 3,91 USD
                // Zwischensumme 1,106 EUR/USD 3,54 EUR
                .section("exchangeRate", "fxAmount", "fxTaxAmount", "fxTaxCurrency", "fxCurrency", "amount", "currency").optional()
                .match("^GESAMT (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Quellensteuer( DE für US\\-Emittent)? \\-(?<fxTaxAmount>[\\.,\\d]+) (?<fxTaxCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) [\\w]{3}\\/[\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                    Money tax = Money.of(asCurrencyCode(v.get("fxTaxCurrency")), asAmount(v.get("fxTaxAmount")));
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    // Calculate gross amount + tax in account currency
                    gross = Money.of(asCurrencyCode(v.get("currency")),
                                    Math.round(tax.getAmount() / exchangeRate.doubleValue())).add(gross);

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

                // GESAMT 5,63 USD
                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^GESAMT (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) [\\w]{3}\\/[\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    // if not gross is present
                    Optional<Unit> grossUnit = t.getUnit(Unit.Type.GROSS_VALUE);
                    if (!grossUnit.isPresent())
                    {
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
                    }
                })

                // 1 Bruttoertrag 26,80 GBP
                // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                // GESAMT 0,44 EUR
                .section("fxAmount", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^[\\d] Bruttoertrag (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3})\\/[\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                    
                    // if transaction currency is different to security currency
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        // if security and transaction currency are different
                        if (!t.getCurrencyCode().equalsIgnoreCase(asCurrencyCode(v.get("currency"))))
                        {
                            // get gross amount and calculate equivalent in EUR
                            Money gross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            BigDecimal amount = BigDecimal.valueOf(gross.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN);

                            // set amount in account currency
                            Money fxAmount = Money.of(v.get("currency"), amount.longValue());

                            t.setAmount(fxAmount.getAmount());
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }
                    }
                    else
                    {
                        // create a Unit only, 
                        // if security and transaction currency are different
                        if (t.getCurrencyCode().equalsIgnoreCase(asCurrencyCode(v.get("fxCurrency"))))
                        {
                            // get exchange rate (in Fx/EUR) and
                            // calculate inverse exchange rate (in EUR/Fx)
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                            RoundingMode.HALF_DOWN);

                            // get gross amount and calculate equivalent in EUR
                            Money gross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            BigDecimal amount = BigDecimal.valueOf(gross.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN);

                            // set amount in account currency
                            Money fxAmount = Money.of(v.get("currency"), amount.longValue());

                            t.setAmount(fxAmount.getAmount());
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, fxAmount, gross,
                                            inverseRate));
                        }
                    }
                })

                .wrap(TransactionItem::new);

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

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        pdfTransaction
                // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                // Registered Shares o.N.
                // CA0679011084
                .section("name", "shares", "nameContinued", "isin", "currency")
                .match("^[\\d] Kapitalmaßnahme (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12})$")
                .match("^[\\d] Barausgleich [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // DE12345689234567671 15.06.2021 0,71 EUR
                .section("date")
                .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // GESAMT 1,630 EUR
                .section("amount", "currency").optional()
                .match("^GESAMT (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // GESAMT 5,63 USD
                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                // GESAMT 4,18 EUR
                .section("forexCurrency", "exchangeRate", "amount", "currency").optional()
                .match("^Zwischensumme [\\.,\\d]+ (?<forexCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                    RoundingMode.HALF_DOWN);
                    BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                    Math.round(t.getAmount() / inverseRate.doubleValue()));
                    Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, inverseRate);
                    if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                        t.addUnit(unit);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }
    
    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("(UMTAUSCH\\/BEZUG|FUSION|KAPITALERH.HUNG GEGEN BAR)", (context, lines) -> {
            Pattern pDate = Pattern.compile("^(.*) DATUM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            Pattern pSkipTransaction = Pattern.compile("^ABRECHNUNG$");
            Pattern pTransactionPosition = Pattern.compile("^(?<transactionPosition>[\\d]) (Barausgleich|Kurswert) (\\-)?[\\.,\\d]+ [\\w]{3}$");
            context.put("skipTransaction", Boolean.FALSE.toString());
            context.put("transactionPosition", "0");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("date", m.group("date"));
                }

                /***
                 * If we have a "ABRECHNUNG",
                 * then this is not a delivery in/outbond.
                 */
                m = pSkipTransaction.matcher(line);
                if (m.matches())
                {
                    context.put("skipTransaction", Boolean.TRUE.toString());
                }

                m = pTransactionPosition.matcher(line);
                if (m.matches() && Boolean.parseBoolean(context.get("skipTransaction")))
                {
                    context.put("transactionPosition", m.group("transactionPosition"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction1 = new Transaction<>();
        pdfTransaction1.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("(^|^[\\d] )(Umtausch\\/Bezug|Einbuchung|Ausbuchung|Kapitalerh.hung gegen Bar) .* ([\\.,\\d]+ Stk\\.|am [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction1);

        pdfTransaction1
                // Is type --> "Einbuchung" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                .section("type").optional()
                .match("(^|^[\\d] )(?<type>Umtausch\\/Bezug|Einbuchung|Ausbuchung|Kapitalerh.hung gegen Bar) .* ([\\.,\\d]+ Stk\\.|am [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.)$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Einbuchung") || v.get("type").equals("Kapitalerhöhung gegen Bar"))
                    {
                        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                    }
                })

                // 1 Umtausch/Bezug Nordex SE 47 Stk.
                // Inhaber-Aktien o.N.
                // DE000A0D6554
                .section("position", "name", "shares", "nameContinued", "isin").optional()
                .match("^(?<position>[\\d]) (Umtausch\\/Bezug|Einbuchung|Ausbuchung) (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12})$")
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
                .match("^Bezugsrecht: (?<isin>[\\w]{12}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^Anzahl eingebuchter $")
                .match("^Bezugsrechte: (?<shares>[\\.,\\d]+) Stk\\.$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                /***
                 * If we have a "ABRECHNUNG",
                 * then this is not a delivery in/outbond.
                 * We skip this transaction.
                 */
                .wrap(t -> {
                            Map<String, String> context = type.getCurrentContext();
                            boolean skipTransactions = Boolean.parseBoolean(context.get("skipTransaction"));

                            if (skipTransactions && context.get("transactionPosition").equals(context.get("position")))
                                return null;
                            else
                                return new TransactionItem(t);
                });

        /***
         * If we have a "ABRECHNUNG".
         */
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
                    if (v.get("type").equals("Barausgleich"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 1 Kurswert -643,90 EUR
                // DE00000000000000000000 15.07.2021 -648,90 EUR
                // 1 Barausgleich 267,90 USD
                // DEXXXXXXXXXXXXXXXXXXXX 22.07.2021 163,68 EUR
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

                // Zwischensumme 1,1785 EUR/USD 227,32 EUR
                .section("exchangeRate").optional()
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ [\\w]{3}")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
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
            // read the current context here
            for (String line : lines)
            {
                Matcher m = currency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
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

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^STEUERABRECHNUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Kapitalertragssteuer Optimierung 3,75 EUR
                .section("amount", "currency", "date").optional()
                .match("^Kapitalertragssteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .find("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Kapitalertragsteuer Optimierung 3,75 EUR
                .section("amount", "currency", "date").optional()
                .match("^Kapitalertragsteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .find("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Solidaritätszuschlag Optimierung 0,21 EUR
                .section("amount", "currency", "date")
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .find("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Kirchensteuer Optimierung 0,30 EUR
                .section("amount", "currency", "date").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .find("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                .wrap(TransactionItem::new);
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

        Block firstRelevantLine = new Block("^VORABPAUSCHALE$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // iShs Core MSCI EM IMI U.ETF 173,3905 Stk.
                // Registered Shares o.N.
                // ISIN: IE00BKM4GZ66
                .section("name", "isin", "shares", "nameContinued")
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // VERRECHNUNGSKONTO VALUTA BETRAG
                // DE12345678912345678912 04.01.2021 -0,32 EUR
                .section("date", "amount", "currency")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> new TransactionItem(t));
    }

    private void addBuySellTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the buy/sell transaction function must be adjusted.
         * addBuySellTransaction();
         */
        Block block = new Block("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);

                    /***
                     * If we have multiple entries in the document,
                     * then the "negative" flag must be removed.
                     */
                    type.getCurrentContext().remove("negative");

                    return t;
                })

                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // Registered Shares o.N.
                // AU000000CUV3
                .section("name", "shares", "currency", "nameContinued", "isin")
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                // Verkauf am 26.02.2021, um 11:44 Uhr.
                .section("date", "time")
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                // Kapitalertragssteuer Optimierung 20,50 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragssteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kapitalertragsteuer Optimierung 4,56 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragsteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Solidaritätszuschlag Optimierung 1,13 EUR
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kirchensteuer Optimierung 9,84 EUR
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addLiquidationTaxReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the liquidation transaction function must be adjusted.
         * addLiquidationTransaction();
         */
        Block block = new Block("^TILGUNG$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);

                    /***
                     * If we have multiple entries in the document,
                     * then the "negative" flag must be removed.
                     */
                    type.getCurrentContext().remove("negative");

                    return t;
                })

                // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                // TurboC O.End Linde
                // DE000TT22GS8
                .section("name", "shares", "nameContinued", "isin")
                .match("^[\\d] Tilgung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // DE0000000000000000000 02.10.2020 33,89 EUR
                .section("date")
                .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // 2 Kapitalertragssteuer Optimierung 29,24 EUR
                .section("amount", "currency").optional()
                .match("^([\\d] )?Kapitalertragssteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // 2 Kapitalertragsteuer Optimierung 1,00 EUR
                .section("amount", "currency").optional()
                .match("^([\\d] )?Kapitalertragsteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Solidaritätszuschlag Optimierung 1,61 EUR
                .section("amount", "currency").optional()
                .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kirchensteuer Optimierung 2,34 EUR
                .section("amount", "currency").optional()
                .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
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

            /***
             * If we have multiple entries in the document,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return t;
        });

        pdfTransaction
                // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                // Registered Shares o.N.
                // AU000000CUV3
                // ISIN: DE000A3H23V7
                .section("name", "shares", "currency", "nameContinued", "isin")
                .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                // Verkauf am 26.02.2021, um 11:44 Uhr.
                .section("date", "time").optional()
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})), um (?<time>[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                .section("negative").optional()
                .match("^GESAMT [\\.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> type.getCurrentContext().put("negative", "X"))

                .section("negative").optional()
                .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        type.getCurrentContext().put("negative", "X");
                    }
                })

                // Fremdkostenzuschlag -1,00 EUR
                .section("currency", "amount").optional()
                .match("^Fremdkostenzuschlag -(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {                            
                    if ("X".equals(type.getCurrentContext().get("negative")))
                    {
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        t.setAmount(asAmount(v.get("amount")));
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Quellensteuer DE für US-Emittent -7,56 USD
                .section("withHoldingTax", "currency").optional()
                .match("^([\\d] )?Quellensteuer .* \\-(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Quellensteuer -12,00 USD
                .section("withHoldingTax", "currency").optional()
                .match("^([\\d] )?Quellensteuer \\-(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Kapitalertragssteuer -30,63 EUR
                .section("tax", "currency").optional()
                .match("^([\\d] )?Kapitalertragssteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Kapitalertragsteuer -8,36 EUR
                .section("tax", "currency").optional()
                .match("^([\\d] )?Kapitalertragsteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag -1,68 EUR
                .section("tax", "currency").optional()
                .match("^([\\d] )?Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Kirchensteuer -1,68 EUR
                .section("tax", "currency").optional()
                .match("^([\\d] )?Kirchensteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Frz. Finanztransaktionssteuer -3,00 EUR
                .section("tax", "currency").optional()
                .match("^.* Finanztransaktionssteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Fremdkostenzuschlag -1,00 EUR
                .section("fee", "currency").optional()
                .match("^Fremdkostenzuschlag \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                // Gebühr Kundenweisung -5,00 EUR
                .section("fee", "currency").optional()
                .match("^Geb.hr Kundenweisung \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
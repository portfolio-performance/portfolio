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
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Trade Republic Bank GmbH"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf|Sparplanausführung)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf|Sparplanausf.hrung) .*$");
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
                .section("name", "shares", "currency", "isin", "nameContinued")
                .match("^(?<name>.*) (?<shares>[.,\\d]+) Stk. [.,\\d]+ (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3}$")
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
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+), um (?<time>\\d+:\\d+) .*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Sparplanausführung am 18.11.2019 an der Lang & Schwarz Exchange.
                .section("date").optional()
                .match("^Sparplanausf.hrung .* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                /***
                 * If the type of transaction is "SELL" 
                 * and the amount is negative, 
                 * then the gross amount set.
                 * 
                 * Fees are processed in a separate transaction
                 */
                .section("negative").optional()
                .match("GESAMT ([-])?[.,\\d]+ [\\w]{3}")
                .match("GESAMT (?<negative>[-])[.,\\d]+ [\\w]{3}")
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
                .match("^GESAMT ([-])?(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^GESAMT ([-])?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^GESAMT ([-])?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    // if amount is already set, we do nothing
                    if (t.getPortfolioTransaction().getAmount() == 0L)
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
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
                .match("^Kapitalertragssteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Kapitalertragsteuer Optimierung 4,56 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragsteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Solidaritätszuschlag Optimierung 1,13 EUR
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("amount")));
                })

                // Kirchensteuer Optimierung 9,84 EUR
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^[\\d]+ Tilgung (?<name>.*) (?<shares>[.,\\d]+) Stk.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // DE0000000000000000000 02.10.2020 33,89 EUR
                .section("date")
                .match("^[\\w]+ (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // 1 Kurswert 0,70 EUR
                // SUMME 0,70 EUR
                // 1 Kurswert 0,25 EUR
                // GESAMT 0,25 EUR
                .section("amount", "currency")
                .match("^[\\d]+ Kurswert .*$")
                .match("^(SUMME|GESAMT) (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
        DocumentType type = new DocumentType("(AUSSCHÜTTUNG|DIVIDENDE)");
        this.addDocumentTyp(type);

        Block block = new Block("^(AUSSCH.TTUNG|DIVIDENDE)$");
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
                .section("name", "shares", "currency", "isin", "nameContinued")
                .match("^(?<name>.*) (?<shares>[.,\\d]+) Stk. [.,\\d]+ (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3}$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // DExxxxxx 25.09.2019 4,18 EUR
                .section("date")
                .match("^[\\w]+ (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d+]+ [\\w]{3}$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // GESAMT 1,630 EUR
                .section("amount", "currency").optional()
                .match("^GESAMT (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
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
                .match("^GESAMT [.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // GESAMT 5,63 USD
                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                // GESAMT 4,18 EUR
                .section("forexCurrency", "exchangeRate", "amount", "currency").optional()
                .match("^GESAMT [.,\\d]+ (?<forexCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[.,\\d]+) [\\w]{3}\\/[\\w]{3} [.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .match("^[\\d]+ Kapitalmaßnahme (?<name>.*) (?<shares>[.,\\d]+) Stk\\.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(?<isin>[\\w]{12})$")
                .match("^[\\d]+ Barausgleich [.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // DE12345689234567671 15.06.2021 0,71 EUR
                .section("date")
                .match("^[\\w]+ (?<date>\\d+.\\d+.\\d{4}) [.,\\d+]+ [\\w]{3}$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // GESAMT 1,630 EUR
                .section("amount", "currency").optional()
                .match("^GESAMT (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // GESAMT 5,63 USD
                // Zwischensumme 1,102 EUR/USD 5,11 EUR
                // GESAMT 4,18 EUR
                .section("forexCurrency", "exchangeRate", "amount", "currency").optional()
                .match("^Zwischensumme [.,\\d]+ (?<forexCurrency>[\\w]{3})$")
                .match("^Zwischensumme (?<exchangeRate>[.,\\d]+) [\\w]{3}\\/[\\w]{3} [.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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

    private void addAccountStatementTransaction()
    {

        DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern currency = Pattern.compile("BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN (?<currency>\\w{3})");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = currency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        Block payInBlock = new Block("\\d+.\\d+.\\d{4} (Accepted PayIn|Einzahlung akzeptiert).*");
        type.addBlock(payInBlock);
        payInBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) (Accepted PayIn|Einzahlung akzeptiert):.* (to|auf).* (?<amount>[\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                        })

                        .wrap(TransactionItem::new));

        Block taxRefundBlock = new Block("\\d+.\\d+.\\d{4} Steueroptimierung.*");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) Steueroptimierung null \\d{16} (?<amount>[\\d+,.]*)")
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
                .match("Kapitalertragssteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match(".* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Kapitalertragsteuer Optimierung 3,75 EUR
                .section("amount", "currency", "date").optional()
                .match("Kapitalertragsteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match(".* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Solidaritätszuschlag Optimierung 0,21 EUR
                .section("amount", "currency", "date")
                .match("Solidarit.tszuschlag Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match(".* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Kirchensteuer Optimierung 0,30 EUR
                .section("amount", "currency", "date").optional()
                .match("Kirchensteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                .match(".* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+")
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
                .match("^(?<name>.*) (?<shares>[.,\\d]+) Stk.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // VERRECHNUNGSKONTO VALUTA BETRAG
                // DE12345678912345678912 04.01.2021 -0,32 EUR
                .section("date", "amount", "currency")
                .match("^.* (?<date>\\d{2}.\\d{2}.\\d{4}) -(?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> new TransactionItem(t));

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
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
                .section("name", "shares", "currency", "isin", "nameContinued")
                .match("^(?<name>.*) (?<shares>[.,\\d]+) Stk. [.,\\d]+ (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3}$")
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
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+), um (?<time>\\d+:\\d+) .*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                // Kapitalertragssteuer Optimierung 20,50 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragssteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kapitalertragsteuer Optimierung 4,56 EUR
                .section("amount", "currency").optional()
                .match("^Kapitalertragsteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Solidaritätszuschlag Optimierung 1,13 EUR
                .section("amount", "currency").optional()
                .match("^Solidarit.tszuschlag Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kirchensteuer Optimierung 9,84 EUR
                .section("amount", "currency").optional()
                .match("^Kirchensteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .section("name", "isin", "shares", "nameContinued")
                .match("^[\\d]+ Tilgung (?<name>.*) (?<shares>[.,\\d]+) Stk.$")
                .match("^(?<nameContinued>.*)$")
                .match("^(ISIN: )?(?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // DE0000000000000000000 02.10.2020 33,89 EUR
                .section("date")
                .match("^[\\w]+ (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+) [.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // 2 Kapitalertragssteuer Optimierung 29,24 EUR
                .section("amount", "currency").optional()
                .match("^([\\d]+ )?Kapitalertragssteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // 2 Kapitalertragsteuer Optimierung 1,00 EUR
                .section("amount", "currency").optional()
                .match("^([\\d]+ )?Kapitalertragsteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Solidaritätszuschlag Optimierung 1,61 EUR
                .section("amount", "currency").optional()
                .match("^([\\d]+ )?Solidarit.tszuschlag Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kirchensteuer Optimierung 2,34 EUR
                .section("amount", "currency").optional()
                .match("^([\\d]+ )?Kirchensteuer Optimierung (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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

        Block block = new Block("^(.*-Order )?Verkauf.*$");
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
                .section("name", "shares", "currency", "isin", "nameContinued")
                .match("^(?<name>.*) (?<shares>[.,\\d]+) Stk. [.,\\d]+ (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3}$")
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
                .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>\\d+.\\d+.\\d{4}|\\d{4}-\\d+-\\d+), um (?<time>\\d+:\\d+) .*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDateTime(asDate(v.get("date")));
                })

                .section("negative").optional()
                .match("^GESAMT [.,\\d]+ [\\w]{3}$")
                .match("^GESAMT (?<negative>[-])[.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    type.getCurrentContext().put("negative", "X");
                })

                .section("negative").optional()
                .match("^GESAMT (?<negative>[-])[.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        type.getCurrentContext().put("negative", "X");
                    }
                })

                // Fremdkostenzuschlag -1,00 EUR
                .section("currency", "amount").optional()
                .match("^Fremdkostenzuschlag -(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
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
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Quellensteuer .* -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Quellensteuer -12,00 USD
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Quellensteuer -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Kapitalertragssteuer -30,63 EUR
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Kapitalertragssteuer -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Kapitalertragsteuer -8,36 EUR
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Kapitalertragsteuer -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag -1,68 EUR
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Solidarit.tszuschlag -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Kirchensteuer -1,68 EUR
                .section("tax", "currency").optional()
                .match("^([\\d]+ )?Kirchensteuer -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Frz. Finanztransaktionssteuer -3,00 EUR
                .section("tax", "currency").optional()
                .match("^.* Finanztransaktionssteuer -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Fremdkostenzuschlag -1,00 EUR
                .section("fee", "currency").optional()
                .match("^Fremdkostenzuschlag -(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
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
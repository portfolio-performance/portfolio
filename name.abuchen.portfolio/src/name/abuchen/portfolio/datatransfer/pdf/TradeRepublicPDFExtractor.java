package name.abuchen.portfolio.datatransfer.pdf;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
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
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractor extends AbstractPDFExtractor
{
    public TradeRepublicPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TRADE REPUBLIC");

        addBuySellTransaction();
        addBuySellCryptoTransaction();
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
        return "Trade Republic Bank GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(WERTPAPIERABRECHNUNG" //
                        + "|WERTPAPIERABRECHNUNG SPARPLAN" //
                        + "|SECURITIES SETTLEMENT SAVINGS PLAN" //
                        + "|SECURITIES SETTLEMENT" //
                        + "|REINVESTIERUNG" //
                        + "|INVESTISSEMENT"
                        + "|REGOLAMENTO TITOLI)", //
                        "(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?" //
                                        + "(?<type>(Kauf" //
                                        + "|Buy"
                                        + "|Acquisto" //
                                        + "|Verkauf" //
                                        + "|Sparplanausf.hrung" //
                                        + "|SAVINGS PLAN" //
                                        + "|Ex.cution de l.investissement programm." //
                                        + "|REINVESTIERUNG))" //
                                        + " .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Registered Shares o.N.
                                        // AU000000CUV3
                                        // ISIN: DE000A3H23V7
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        // We pick the second
                                        //
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 2 Reinvestierung Vodafone Group PLC 22 Stk.
                                        // Registered Shares DL 0,2095238
                                        // GB00BH4HKS39
                                        // 1 Bruttoertrag 26,80 GBP
                                        // 2 Barausgleich 0,37 GBP
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] Reinvestierung .* [\\.,\\d]+ Stk\\.$") //
                                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^[\\d] Bruttoertrag [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Tencent Holdings Ltd. 0,3773 titre(s) 53,00 EUR 20,00 EUR
                                        // zjBAM Corp. 125 Pz. 29,75 EUR 3.718,75 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Berkshire Hathaway Inc. 0.3367 Pcs. 297.00 EUR 100.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 2 Reinvestierung Vodafone Group PLC 22 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$") //
                                                        .match("^[\\d] Reinvestierung (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                                        // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                                        // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                                        // Limit-Order Buy on 28.04.2023 at 11:13 (Europe/Berlin).
                                        // Verkauf am 26.02.2021, um 11:44 Uhr.
                                        // Market-OrderAcquisto il 01.06.2023 alle 10:46 (Europe/Berlin) su Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?(Buy|Acquisto|Kauf|Verkauf) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}))(,)? (um|at|alle) (?<time>[\\d]{2}:[\\d]{2}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Exécution de l'investissement programmé le 17/01/2022 sur le Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ex.cution de l.investissement programm. .* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Sparplanausführung am 18.11.2019 an der Lang & Schwarz Exchange.
                                        // Savings plan execution on 16.05.2023 on the Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Sparplanausf.hrung|Savings plan) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),

                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        //
                                        // DE40110101001234567890 06.08.2021 0,44 GBP
                                        // @formatter:on
                                        section -> section.attributes("date") //
                                                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // If the type of transaction is "SELL" and the amount
                        // is negative, then the gross amount set.
                        // Fees are processed in a separate transaction.
                        //
                        // GESAMT 1.395,60 EUR
                        // GESAMT -1.396,60 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("(GESAMT|TOTAL|TOTALE) (\\-)?[\\.,\\d]+ [\\w]{3}") //
                        .match("(GESAMT|TOTAL|TOTALE) (?<negative>\\-)[\\.,\\d]+ [\\w]{3}") //
                        .assign((t, v) -> {
                            if (t.getPortfolioTransaction().getType().isLiquidation())
                                type.getCurrentContext().putBoolean("negative", true);
                        })

                        .oneOf( //
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
                                        section -> section //
                                                        .attributes("amount", "currency", "gross", "grossCurrency") //
                                                        .match("^(GESAMT|TOTAL|TOTALE) (\\-)?(?<gross>[\\.,\\d]+) (?<grossCurrency>[\\w]{3})$") //
                                                        .match("^(GESAMT|TOTAL|TOTALE) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                t.setAmount(asAmount(v.get("gross")));
                                                                t.setCurrencyCode(asCurrencyCode(v.get("grossCurrency")));
                                                            }
                                                            else
                                                            {
                                                                t.setAmount(asAmount(v.get("amount")));
                                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            }
                                                        }),
                                        // In case there is no tax,
                                        // only one line with "GESAMT"
                                        // exists and we need to grab data from
                                        // that line

                                        // @formatter:off
                                        // GESAMT 1.792,29 EUR
                                        // TOTAL 20,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(GESAMT|TOTAL|TOTALE) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // This is for the reinvestment of dividends.
                        // We subtract the second amount from the first amount and set this.
                        //
                        // 1 Bruttoertrag 26,80 GBP
                        // 2 Barausgleich 0,37 GBP
                        // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                        // @formatter:on
                        .section("gross", "currency", "cashCompensation", "cashCompensationCurrency", "exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^[\\d] Bruttoertrag (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^[\\d] Barausgleich (?<cashCompensation>[\\.,\\d]+) (?<cashCompensationCurrency>[\\w]{3})$") //
                        .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            Money grossValueBasis = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            Money cashCompensationValue = Money.of(asCurrencyCode(v.get("cashCompensationCurrency")), asAmount(v.get("cashCompensation")));

                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = grossValueBasis.subtract(cashCompensationValue);
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

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
                        .section("amount", "currency").optional() //
                        .match("^Kapitalertrag(s)?steuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 1,13 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 9,84 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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

    private void addBuySellCryptoTransaction()
    {
        DocumentType type = new DocumentType("(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^((Limit|Stop\\-Market|Market)\\-Order )?" //
                                        + "(?<type>(Kauf" //
                                        + "|Buy" //
                                        + "|Verkauf" //
                                        + "|Sparplanausf.hrung" //
                                        + "|SAVINGS PLAN" //
                                        + "|Ex.cution de l.investissement programm." //
                                        + "|REINVESTIERUNG))" //
                                        + " .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Ethereum (ETH) 0,0878 Stk. 3.415,35 EUR 299,87 EUR
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency") //
                        .match("^(?<name>.*) \\((?<tickerSymbol>[A-Z]*)\\) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateCryptoCurrency(v)))

                        // @formatter:off
                        // Ethereum (ETH) 0,0878 Stk. 3.415,35 EUR 299,87 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^.* \\([A-Z]*\\) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Market-Order Kauf am 03.01.2022, um 12:32 Uhr (Europe/Berlin) im außerbörslichen Handel (Bankhaus
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), .* (?<time>[\\d]{2}:[\\d]{2}) .*$") //
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Sparplanausführung am 16.05.2023 im außerbörslichen Handel Bankhaus Scheich.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Sparplanausf.hrung .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // VERRECHNUNGSKONTO VALUTA BETRAG
                                        // DE40110101008224044621 03.01.2022 -300,87 EUR
                                        //
                                        // VERRECHNUNGSKONTO VALUTA BETRAG
                                        // DE99012345670123456789 10.01.2022 261,85 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("VERRECHNUNGSKONTO VALUTA BETRAG") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                                        // DE00111122223333444455 16.05.2023 -24,99 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // SPARPLAN y646-a753
                        // @formatter:on
                        .section("note").optional() //
                        .match("^SPARPLAN (?<note>.*\\-.*)$") //
                        .assign((t, v) -> t.setNote("Sparplan: " + trim(v.get("note"))))

                        // @formatter:off
                        // xxxxxxxxxxxxxxxxxxxxx ORDER 2dc3-a410
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* ORDER (?<note>.*\\-.*)$") //
                        .assign((t, v) -> {
                            if (t.getNote() != null)
                                t.setNote(t.getNote() + " | Order: " + trim(v.get("note")));
                            else
                                t.setNote("Order: " + trim(v.get("note")));
                        })

                        // @formatter:off
                        // AUSFÜHRUNG ce15-0e37
                        // 92540 Berlin AUSFÜHRUNG K7Y2-2e37
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*AUSF.HRUNG (?<note>.*\\-.*)$") //
                        .assign((t, v) -> {
                            if (t.getNote() != null)
                                t.setNote(t.getNote() + " | Ausführung: " + trim(v.get("note")));
                            else
                                t.setNote("Ausführung: " + trim(v.get("note")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addLiquidationTransaction()
    {
        DocumentType type = new DocumentType("TILGUNG");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TILGUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    BuySellEntry portfolioTransaction = new BuySellEntry();
                    portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                    return portfolioTransaction;
                })

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
        DocumentType type = new DocumentType("(AUSSCH.TTUNG" //
                        + "|DIVIDENDE" //
                        + "|REINVESTIERUNG" //
                        + "|STORNO DIVIDENDE" //
                        + "|DIVIDENDO" //
                        + "|DISTRIBUZIONE)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$"); //
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Storno der Dividende mit dem Ex-Tag 29.11.2019.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Storno) der Dividende .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // iShsV-EM Dividend UCITS ETF 10 Stk. 0,563 USD 5,63 USD
                                        // Registered Shares USD o.N.
                                        // IE00B652H904
                                        //
                                        // Enbridge Inc. 20,971565 Pz. 0,8875 CAD 18,61 CAD
                                        // Registered Shares o.N.
                                        // ISIN: CA29250N1050
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|Pz\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // Registered Shares DL 0,2095238
                                        // GB00BH4HKS39
                                        // 1 Bruttoertrag 26,80 GBP
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] Reinvestierung (?<name>.*) [\\.,\\d]+ Stk\\.$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^[\\d] Reinvestierung .*$") //
                                                        .match("^[\\d] Bruttoertrag [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // iShsV-EM Dividend UCITS ETF 10 Stk. 0,563 USD 5,63 USD
                                        // Enbridge Inc. 20,971565 Pz. 0,8875 CAD 18,61 CAD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|Pz\\.) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Reinvestierung .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // DExxxxxx 25.09.2019 4,18 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 1 Bruttoertrag 26,80 GBP
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^[\\d] Reinvestierung .*$") //
                                                        .match("^[\\d] Bruttoertrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // There might be two lines with "GESAMT"
                                        // - one for gross
                                        // - one for the net value
                                        // we pick the second
                                        //
                                        // GESAMT 45,40 USD
                                        // GESAMT -30,17 EUR
                                        //
                                        // TOTALE 18,61 CAD
                                        // TOTALE 9,56 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(GESAMT|TOTALE) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(GESAMT|TOTALE) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // GESAMT 1,630 EUR
                                        // TOTALE 9,56 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(GESAMT|TOTALE) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf(
                                        // @formatter:off
                                        // GESAMT 5,63 USD
                                        // Zwischensumme 1,102 EUR/USD 5,11 EUR
                                        //
                                        // Subtotale 13,96 CAD
                                        // Subtotale 1,46055 EUR/CAD 9,56 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "exchangeRate", "baseCurrency", "termCurrency") //
                                                        .match("^(GESAMT|TOTALE) (\\-)?(?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^(Zwischensumme|Subtotale) (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 1 Bruttoertrag 26,80 GBP
                                        // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                                        // GESAMT 0,44 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "exchangeRate", "baseCurrency", "termCurrency") //
                                                        .match("^[\\d] Bruttoertrag (\\-)?(?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            t.setAmount(gross.getAmount());
                                                            t.setCurrencyCode(asCurrencyCode(gross.getCurrencyCode()));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

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
    }

    private void addCaptialReductionTransaction()
    {
        DocumentType type = new DocumentType("KAPITALREDUKTION");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^KAPITALREDUKTION$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                    return accountTransaction;
                })

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
                .section("fxGross", "exchangeRate", "baseCurrency", "termCurrency", "gross").optional()
                .match("^[\\d] Barausgleich (?<fxGross>[\\.,\\d]+) [\\w]{3}$")
                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) [\\w]{3}$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                    Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
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
            context.putBoolean("skipTransaction", false);
            context.put("transactionPosition", "0");

            for (String line : lines)
            {
                Matcher mDate = pDate.matcher(line);
                if (mDate.matches())
                    context.put("date", mDate.group("date"));

                // If we have a "ABRECHNUNG", then this is not a
                // delivery in/outbond.
                Matcher mSkipTransaction = pSkipTransaction.matcher(line);
                if (mSkipTransaction.matches())
                    context.putBoolean("skipTransaction", true);

                Matcher mTransactionPosition = pTransactionPosition.matcher(line);
                if (mTransactionPosition.matches() && context.getBoolean("skipTransaction"))
                    context.put("transactionPosition", mTransactionPosition.group("transactionPosition"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction1 = new Transaction<>();

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

        pdfTransaction1 //

                .subject(() -> {
                    PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                    portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    return portfolioTransaction;
                })

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
                    if (type.getCurrentContext().getBoolean("skipTransaction")
                                    && type.getCurrentContext().get("transactionPosition").equals(type.getCurrentContext().get("position")))
                        return null;
                    else
                        return new TransactionItem(t);
                });

        // If we have a "ABRECHNUNG".
        Transaction<BuySellEntry> pdfTransaction2 = new Transaction<>();

        firstRelevantLine = new Block("^ABRECHNUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction2);

        pdfTransaction2 //

                .subject(() -> {
                    BuySellEntry portfolioTransaction = new BuySellEntry();
                    portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                    return portfolioTransaction;
                })

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
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // // BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN (?<currency>[\\w]{3})$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                        // @formatter:off
                                                        // WERTSTELLUNG BUCHUNGSTEXT BETRAG IN EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^WERTSTELLUNG BUCHUNGSTEXT BETRAG IN (?<currency>[\\w]{3})$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))));

        this.addDocumentTyp(type);

        Block depositBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) "
                        + "(Accepted PayIn"
                        + "|Einzahlung akzeptiert"
                        + "|Customer).*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) " //
                                        + "(Accepted PayIn" //
                                        + "|Einzahlung akzeptiert" //
                                        + "|Customer .* inpayed net):.* (?<amount>[\\.,\\d]+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        Block taxRefundBlock = new Block("([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Steueroptimierung.*");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}-[\\d]{2}-[\\d]{2})) Steueroptimierung.* (?<amount>[\\.,\\d]+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Your interest payment.*");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}-[\\d]{2}-[\\d]{2})) Your interest payment (?<amount>[\\.,\\d]+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addTaxStatementTransaction()
    {
        DocumentType type = new DocumentType("STEUERABRECHNUNG");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^STEUERABRECHNUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                    return accountTransaction;
                })

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
        DocumentType type = new DocumentType("(ABRECHNUNG ZINSEN" //
                        + "|RESOCONTO INTERESSI MATURATI" //
                        + "|INTEREST INVOICE)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // zum 31.01.2023
                        // alla data 31.05.2023
                        // as of 30.09.2023
                        // @formatter:on
                        .section("date") //
                        .match("^(zum|alla data|as of) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // IBAN BUCHUNGSDATUM GUTSCHRIFT NACH STEUERN
                        // DE10123456789123456789 01.02.2023 0,88 EUR
                        //
                        // IBAN DATA EMISSIONE TOTALE
                        // DE93752109007837402856 01.06.2023 0,12 EUR
                        //
                        // IBAN BOOKING DATE TOTAL
                        // DE27502109007011534672 02.10.2023 1,47 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("IBAN (BUCHUNGSDATUM|DATA EMISSIONE|BOOKING DATE) (GUTSCHRIFT NACH STEUERN|TOTALE|TOTAL)") //
                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Cash Zinsen 4,00% 01.10.2023 - 31.10.2023 152,67 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.* (Zinsen|Interessi|Interest) (?<note1>[\\.,\\d]+%) (?<note2>.*[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note2")) + " (" + trim(v.get("note1")) + ")")),
                                        // @formatter:off
                                        // Cash Zinsen 2,00% 2,58 EUR
                                        // Liquidità Interessi 2,00% 0,12 EUR
                                        // Cash Interest 2,00% 1,47 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* (Zinsen|Interessi|Interest) (?<note>[\\.,\\d]+%).*$")
                                                        .assign((t, v) -> t.setNote(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^VORABPAUSCHALE$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.TAXES);
                    return accountTransaction;
                })

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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^((Limit|Stop\\-Market|Market)\\-Order )?(Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                    return accountTransaction;
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
                });
    }

    private void addLiquidationTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TILGUNG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                    return accountTransaction;
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
                });
    }

    private void addSellWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("(WERTPAPIERABRECHNUNG" //
                        + "|WERTPAPIERABRECHNUNG SPARPLAN" //
                        + "|SECURITIES SETTLEMENT SAVINGS PLAN" //
                        + "|SECURITIES SETTLEMENT" //
                        + "|REINVESTIERUNG" //
                        + "|INVESTISSEMENT)", //
                        "(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(.*\\-Order )?Verkauf.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                        // Registered Shares o.N.
                        // AU000000CUV3
                        // ISIN: DE000A3H23V7
                        // @formatter:on
                        .section("name", "currency", "nameContinued", "isin") //
                        .match("^(?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^.* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                        // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                        // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                        // Verkauf am 26.02.2021, um 11:44 Uhr.
                        // @formatter:on
                        .section("date", "time") //
                        .match("^((Limit|Stop-Market|Market)-Order )?(Kauf|Verkauf) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})), um (?<time>[\\d]{2}:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // GESAMT 0,34 EUR
                        // GESAMT -0,66 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("^GESAMT [\\.,\\d]+ [\\w]{3}$") //
                        .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("negative", true))

                        // @formatter:off
                        // GESAMT -0,66 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("^GESAMT (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                type.getCurrentContext().putBoolean("negative", true);
                        })

                        // @formatter:off
                        // Fremdkostenzuschlag -1,00 EUR
                        // @formatter:on
                        .section("currency", "amount").optional() //
                        .match("^Fremdkostenzuschlag \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean("negative"))
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
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Quellensteuer DE für US-Emittent -7,56 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^([\\d] )?Quellensteuer .* (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Ritenuta alla fonte -4,65 CAD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Ritenuta alla fonte (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Quellensteuer -12,00 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^([\\d] )?Quellensteuer (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Kapitalertragssteuer -30,63 EUR
                        // Kapitalertragsteuer -8,36 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^([\\d] )?Kapitalertrags(s)?teuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag -1,68 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer -1,68 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^([\\d] )?Kirchensteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Frz. Finanztransaktionssteuer -3,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.* Finanztransaktionssteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer Optimierung 0,360 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrag(s)?steuer Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (v.getTransactionContext().get(FAILURE) != null)
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 0,360 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (v.getTransactionContext().get(FAILURE) != null)
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 0,360 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer Optimierung (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (v.getTransactionContext().get(FAILURE) != null)
                                processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Fremdkostenzuschlag -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremdkostenzuschlag \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Gebühr Kundenweisung -5,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Geb.hr Kundenweisung \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Fremde Spesen -0,12 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Spesen \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // External cost surcharge -1.00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^External cost surcharge \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Supplemento spese di terzi -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Supplemento spese di terzi \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        });
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
package name.abuchen.portfolio.datatransfer.pdf;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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
        addSellWithNegativeAmountTransaction();
        addBuySellCryptoTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
        addTaxesStatementTransaction();
        addTaxesCorrectionStatementTransaction();
        addDepositStatementTransaction();
        addInterestStatementTransaction();
        addNonImportableTransaction();
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
                        + "|WERTPAPIERABRECHNUNG ROUND UP" //
                        + "|WERTPAPIERABRECHNUNG SAVEBACK" //
                        + "|SECURITIES SETTLEMENT SAVINGS PLAN" //
                        + "|SECURITIES SETTLEMENT" //
                        + "|REINVESTIERUNG" //
                        + "|INVESTISSEMENT"
                        + "|REGOLAMENTO TITOLI"
                        + "|ZWANGS.BERNAHME"
                        + "|TILGUNG)", //
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
                                        + "|REINVESTIERUNG" //
                                        + "|ZWANGS.BERNAHME"
                                        + "|TILGUNG))" //
                                        + ".*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "ZWANGSÜBERNAHME".equals(v.get("type")) //
                                            || "TILGUNG".equals(v.get("type")))
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
                                        // 1 Ausbuchung Quantafuel AS 17 Stk.
                                        // Navne-Aksjer NK -,01
                                        // NO0010785967
                                        // 1 Barausgleich 108,46 NOK
                                        //
                                        // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                                        // TurboC O.End Linde
                                        // DE000TT22GS8
                                        // 1 Kurswert 0,70 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung) (?<name>.*) [\\.,\\d]+ Stk\\.$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        // 1 Ausbuchung Quantafuel AS 17 Stk.
                                        // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
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
                                        // Ausführung von Round up am 09.02.2024 an der Lang & Schwarz Exchange.
                                        // Ausführung von Saveback am 04.03.2024 an der Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ausf.hrung von (Round up|Saveback) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
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
                                        // @formatter:off
                                        // SUMME 0,70 EUR
                                        // SUMME 33,19 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^SUMME (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .match("^SUMME [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                                t.setAmount(asAmount(v.get("amount")));
                                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
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
                                                        .match("^(GESAMT|TOTAL|TOTALE|) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // This is for the reinvestment of dividends.
                                        // We subtract the second amount from the first amount and set this.
                                        //
                                        // 1 Bruttoertrag 26,80 GBP
                                        // 2 Barausgleich 0,37 GBP
                                        // Zwischensumme 0,85267 EUR/GBP 0,44 EUR
                                        // @formatter:on
                                        section -> section
                                                .attributes("gross", "currency", "cashCompensation", "cashCompensationCurrency", "exchangeRate", "baseCurrency", "termCurrency")
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
                                                }),
                                        // @formatter:off
                                        // 1 Barausgleich 108,46 NOK
                                        // Zwischensumme 11,370137 EUR/NOK 9,54 EUR
                                        // @formatter:on
                                        section -> section
                                                .attributes("exchangeRate", "baseCurrency", "termCurrency", "gross")
                                                .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) [\\w]{3}$")
                                                .assign((t, v) -> {
                                                    ExtrExchangeRate rate = asExchangeRate(v);
                                                    type.getCurrentContext().putType(rate);

                                                    Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                    Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                }))

                        // @formatter:off
                        // If the tax is optimized, this is a tax refund
                        // transaction and we subtract this from the amount and reset this.
                        // @formatter:on

                        // @formatter:off
                        // Kapitalertragssteuer Optimierung 20,50 EUR
                        // Kapitalertragsteuer Optimierung 4,56 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kapitalertrags(s)?teuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^(GESAMT|TOTAL|TOTALE|) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 1,13 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^(GESAMT|TOTAL|TOTALE|) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 9,84 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^(GESAMT|TOTAL|TOTALE|) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(tax));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // D 12345 Stadt ORDER dead-beef
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*ORDER (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Order: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 23537 DCrFCrYea AUSFÜHRUNG 5437-f7f5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*AUSF.HRUNG (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Ausführung: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // [ZIP CODE] [CITY] EXÉCUTION cee1-2d00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*EXÉCUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Exécution : " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 131 56 rwMMPGwX EXECUTION d008-0f58
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*EXECUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Execution: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 51670 cyuzKxpHr ORDINE cY43-6m6l
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*ORDINE (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Ordine: " + trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // SAVEBACK B2C4-n64q
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SAVEBACK (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Saveback: "))),
                                        // @formatter:off
                                        // SPARPLAN y646-a753
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SPARPLAN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Sparplan: "))),
                                        // @formatter:off
                                        // SAVINGS PLAN 6af7-5be3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SAVINGS PLAN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Savings plan: "))),
                                        // @formatter:off
                                        // ROUND UP 42c2-50a7
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^ROUND UP (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Round Up: "))),
                                        // @formatter:off
                                        // PROGRAMMÉ eea2-4c8b
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^PROGRAMM. (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Programmé : "))),
                                        // @formatter:off
                                        // EXÉCUTION 4A66-g597
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^EXÉCUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Exécution : "))),
                                        // @formatter:off
                                        // EXECUTION 4A66-g597
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^EXECUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Execution: "))),
                                        // @formatter:off
                                        // ESECUZIONE V711-7789
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^ESECUZIONE (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Esecuzione: "))),
                                        // @formatter:off
                                        // AUSFÜHRUNG 4019-2100
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^AUSF.HRUNG (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Ausführung: "))))

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
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
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

                        .optionalOneOf( //
                                        // @formatter:off
                                        // xxxxxxxxxxxxxxxxxxxxx ORDER 2dc3-a410
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*ORDER (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Order: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 23537 DCrFCrYea AUSFÜHRUNG 5437-f7f5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*AUSF.HRUNG (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Ausführung: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // [ZIP CODE] [CITY] EXÉCUTION cee1-2d00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*EXÉCUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Exécution : " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 131 56 rwMMPGwX EXECUTION d008-0f58
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*EXECUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Execution: " + trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // SAVEBACK B2C4-n64q
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SAVEBACK (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Saveback: "))),
                                        // @formatter:off
                                        // SPARPLAN y646-a753
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SPARPLAN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Sparplan: "))),
                                        // @formatter:off
                                        // SAVINGS PLAN 6af7-5be3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^SAVINGS PLAN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Savings plan: "))),
                                        // @formatter:off
                                        // ROUND UP 42c2-50a7
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^ROUND UP (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Round Up: "))),
                                        // @formatter:off
                                        // PROGRAMMÉ eea2-4c8b
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^PROGRAMM. (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Programmé : "))),
                                        // @formatter:off
                                        // EXÉCUTION 4A66-g597
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^EXÉCUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Exécution : "))),
                                        // @formatter:off
                                        // EXECUTION 4A66-g597
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^EXECUTION (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Execution: "))),
                                        // @formatter:off
                                        // ESECUZIONE V711-7789
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^ESECUZIONE (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Esecuzione: "))),
                                        // @formatter:off
                                        // AUSFÜHRUNG ce15-0e37
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^AUSF.HRUNG (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Ausführung: "))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(AUSSCH.TTUNG" //
                        + "|DIVIDENDE" //
                        + "|REINVESTIERUNG" //
                        + "|STORNO DIVIDENDE" //
                        + "|DIVIDEND" //
                        + "|DIVIDENDO" //
                        + "|DISTRIBUZIONE"
                        + "|KAPITALREDUKTION)");
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
                                        //
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // Registered Shares o.N.
                                        // ISIN: US0378331005
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|Pz\\.|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // Registered Shares o.N.
                                        // CA0679011084
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] Kapitalmaßnahme (?<name>.*) [\\.,\\d]+ Stk\\.$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^[\\d] Barausgleich [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|Pz\\.) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Reinvestierung|Kapitalmaßnahme) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
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
                                                        .match("^(GESAMT|TOTALE|TOTAL) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(GESAMT|TOTALE|TOTAL) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // GESAMT 1,630 EUR
                                        // TOTALE 9,56 EUR
                                        // TOTAL 0.02 EU
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(GESAMT|TOTALE|TOTAL) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf(
                                        // @formatter:off
                                        // GESAMT 5,63 USD
                                        // Zwischensumme 1,102 EUR/USD 5,11 EUR
                                        //
                                        // TOTALE 18,61 CAD
                                        // Subtotale 1,46055 EUR/CAD 9,56 EUR
                                        //
                                        // TOTAL 0.02 USD
                                        // Subtotal 1.0715 EUR/USD 0.02 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "exchangeRate", "baseCurrency", "termCurrency") //
                                                        .match("^(GESAMT|TOTALE|TOTAL) (\\-)?(?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^(Zwischensumme|Subtotale|Subtotal) (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
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
                                                        }),
                                        // @formatter:off
                                        // 1 Barausgleich 1,18 USD
                                        // Zwischensumme 1,218702 EUR/USD 0,97 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "exchangeRate", "baseCurrency", "termCurrency", "gross") //
                                                        .match("^[\\d] Barausgleich (?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Zwischensumme (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))


                        // @formatter:off
                        // Kapitalertragssteuer Optimierung -0,090 EUR
                        // Kapitalertragsteuer Optimierung 5,15 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kapitalertrags(s)?teuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (v.getTransactionContext().getString(FAILURE) == null)
                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 0,28 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (v.getTransactionContext().getString(FAILURE) == null)
                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 9,84 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (v.getTransactionContext().getString(FAILURE) == null)
                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

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
        addDividendeTaxReturnBlock(type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
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
                        //
                        // MUF-Amundi MSCI World II U.E. 5,598 Stück
                        // Actions au Port.Dist o.N.
                        // FR0010315770
                        // @formatter:on
                        .section("name", "nameContinued", "isin") //
                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|St.ck)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // iShs Core MSCI EM IMI U.ETF 173,3905 Stk.
                        // MUF-Amundi MSCI World II U.E. 5,598 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|St.ck)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // DE12345678912345678912 04.01.2021 -0,32 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\-[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // hxTZALjiAGgX. 6 DATUM 15.01.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.* DATUM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // VERRECHNUNGSKONTO VALUTA BETRAG
                        // DE12345678912345678912 04.01.2021 -0,32 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Steuerpflichtige Vorabpauschale 1,73 EUR
                        // @formatter:on
                        .section("note", "amount", "currency").optional() //
                        .match("^Steuerpflichtige (?<note>Vorabpauschale (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}))$") //
                        .assign((t, v) -> {

                            if (t.getCurrencyCode() == null && t.getAmount() == 0)
                            {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setAmount(0L);
                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                            }
                            t.setNote(v.get("note"));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN EUR
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

        Block depositBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                        + "(Accepted PayIn" //
                        + "|Einzahlung akzeptiert" //
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

        Block removalBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                        + "(Auszahlung an Referenzkonto).*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) " //
                                        + "(Auszahlung an Referenzkonto) \\-(?<amount>[\\.,\\d]+)") //
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

    private void addTaxesStatementTransaction()
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
                        .section("amount", "currency", "date") //
                        .match("^Kapitalertrags(s)?teuer Optimierung [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .find("VERRECHNUNGSKONTO VALUTA BETRAG") //
                        .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addTaxesCorrectionStatementTransaction()
    {
        DocumentType type = new DocumentType("STEUERKORREKTUR");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^STEUERKORREKTUR$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // W.P. Carey Inc. 38,4597 Stk.
                        // Registered Shares DL -,01
                        // ISIN: US92936U1097
                        // @formatter:on
                        .section("name", "shares", "nameContinued", "isin") //
                        .match("^(?<name>.*) (?<shares>[\\.,\\d]+) (Stk\\.|St.ck)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Quellensteuer für US-Emittent -6,94 USD
                        // @formatter:on
                        .section("amount", "currency", "date") //
                        .find("^Quellensteuer f.r US\\-Emittent \\-[\\.,\\d]+ ([\\w]{3})$") //
                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                        .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addDepositStatementTransaction()
    {
        DocumentType type = new DocumentType("ABRECHNUNG EINZAHLUNG");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                        // DEXXxxxxxxxxxxXXXXXXXXX 07.01.2024 49,00 EUR
                        // @formatter:on
                        .section("date") //
                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                        // DEXXxxxxxxxxxxXXXXXXXXX 07.01.2024 49,00 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
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

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(SPLIT" //
                        + "|FUSION" //
                        + "|DEPOT.BERTRAG EINGEHEND" //
                        + "|TITELUMTAUSCH"
                        + "|VERGLEICHSVERFAHREN"
                        + "|KAPITALERH.HUNG GEGEN BAR"
                        + "|SPIN\\-OFF"
                        + "|UMTAUSCH\\/BEZUG"
                        + "|STEUERLICHER UMTAUSCH)", //
                        documentContext -> documentContext //
                                        .section("transaction") //
                                        .match("^(?<transaction>(SPLIT" //
                                                        + "|FUSION" //
                                                        + "|DEPOT.BERTRAG EINGEHEND"
                                                        + "|TITELUMTAUSCH"
                                                        + "|VERGLEICHSVERFAHREN"
                                                        + "|KAPITALERH.HUNG GEGEN BAR"
                                                        + "|SPIN\\-OFF"
                                                        + "|UMTAUSCH\\/BEZUG"
                                                        + "|STEUERLICHER UMTAUSCH))$") //
                                        .assign((ctx, v) -> ctx.put("transaction", v.get("transaction")))

                                        // @formatter:off
                                        // Straße 1 DATUM 27.07.2021
                                        // @formatter:on
                                        .section("date") //
                                        .match("^.*DATUM (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^([\\d] )?(Einbuchung" //
                        + "|Ausbuchung"
                        + "|Umtausch\\/Bezug" //
                        + "|Depot.bertrag .*"
                        + "|Kapitalerh.hung gegen Bar .*) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 1 Einbuchung Amazon.com Inc. 17,4743 Stk.
                                        // Registered Shares DL -,01
                                        // US0231351067
                                        //
                                        // 1 Ausbuchung Slack Technologies Inc. 10 Stk.
                                        // Registered Shs Cl.A o.N.
                                        // US83088V1026
                                        //
                                        // 1 Umtausch/Bezug Nordex SE 47 Stk.
                                        // Inhaber-Aktien o.N.
                                        // DE000A0D6554
                                        //
                                        // 1 Depotübertrag eingehend Knorr-Bremse AG 13 Stk.
                                        // Inhaber-Aktien o.N.
                                        // DE000KBX1006
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type", "name", "shares", "nameContinued", "isin") //
                                                        .documentContext("date", "transaction") //
                                                        .match("^[\\d] (?<type>(Einbuchung|Ausbuchung|Umtausch\\/Bezug|Depot.bertrag eingehend)) (?<name>.*) (?<shares>[\\.,\\d]+) Stk\\.$")
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type --> "-" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                                            // @formatter:on
                                                            if ("Ausbuchung".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                                                            if ("SPLIT".equals(v.get("transaction")))
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                                                            else
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // VERHÄLTNIS3 VERHÄLTNIS4 PREIS5 FRIST6
                                        // Nordex SE
                                        // Inhaber-Aktien o.N.
                                        // ISIN: DE000A0D6554 02.07.2021 – 02.07.2021 – 130 Stk. 1 : 1 2,75 : 1,00 13,70 EUR
                                        // Bezugsrechte: 130 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "date", "currency", "isin", "shares") //
                                                        .find("VERH.*")
                                                        .match("^(?<name>.*)$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<currency>[\\w]{3})$")
                                                        .match("^Bezugsrechte: (?<shares>[\\.,\\d]+) Stk\\.$")
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addBuySellTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
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
                                        // 1 Ausbuchung Quantafuel AS 17 Stk.
                                        // Navne-Aksjer NK -,01
                                        // NO0010785967
                                        // 1 Barausgleich 108,46 NOK
                                        //
                                        // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                                        // TurboC O.End Linde
                                        // DE000TT22GS8
                                        // 1 Kurswert 0,70 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung) (?<name>.*) [\\.,\\d]+ Stk\\.$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        // 1 Ausbuchung Quantafuel AS 17 Stk.
                                        // 1 Tilgung HSBC Trinkaus & Burkhardt AG 700 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
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
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Exécution de l'investissement programmé le 17/01/2022 sur le Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ex.cution de l.investissement programm. .* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Sparplanausführung am 18.11.2019 an der Lang & Schwarz Exchange.
                                        // Savings plan execution on 16.05.2023 on the Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Sparplanausf.hrung|Savings plan) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Ausführung von Round up am 09.02.2024 an der Lang & Schwarz Exchange.
                                        // Ausführung von Saveback am 04.03.2024 an der Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ausf.hrung von (Round up|Saveback) .* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        //
                                        // DE40110101001234567890 06.08.2021 0,44 GBP
                                        // @formatter:on
                                        section -> section.attributes("date") //
                                                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // GESAMT 3.615,63 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(GESAMT|TOTAL|TOTALE|SUMME) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // For tax optimizations in sales,
                            // we set the amount to 0.00 and then add the optimized taxes.
                            // @formatter:on

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(0L);
                        })

                        // @formatter:off
                        // Kapitalertragssteuer Optimierung 20,50 EUR
                        // Kapitalertragsteuer Optimierung 4,56 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kapitalertrags(s)?teuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 1,13 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 9,84 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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

    private void addDividendeTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
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
                                        //
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // Registered Shares o.N.
                                        // ISIN: US0378331005
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|Pz\\.|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN: )?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // Registered Shares o.N.
                                        // CA0679011084
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] Kapitalmaßnahme (?<name>.*) [\\.,\\d]+ Stk\\.$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^[\\d] Barausgleich [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|Pz\\.) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Reinvestierung|Kapitalmaßnahme) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // DExxxxxx 25.09.2019 4,18 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // DE99012345670123456789 10.01.2024 68,74 EUR
                        // @formatter:on
                        .section("currency") //
                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(0L);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer Optimierung 5,15 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kapitalertrags(s)?teuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                        })

                        // @formatter:off
                        // Solidaritätszuschlag Optimierung 0,28 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                        })

                        // @formatter:off
                        // Kirchensteuer Optimierung 9,84 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                        })

                        // @formatter:off
                        // Zwischensumme 1,095514 EUR/USD 63,31 EUR
                        // @formatter:on
                        .section("exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^(Zwischensumme|Subtotale|Subtotal) (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ [\\w]{3}$")
                        .assign((t, v) -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                            {
                                ExtrExchangeRate rate = asExchangeRate(v);
                                type.getCurrentContext().putType(rate);

                                Money fxGross = rate.convert(rate.getTermCurrency(), t.getMonetaryAmount());

                                checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                            }
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return null;

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
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
                        .assign((t, v) -> processTaxEntries(t, v, type));
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
                        })

                        // @formatter:off
                        // Gebühr für Einzahlung via Lastschrift 0,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Geb.hr f.r Einzahlung via Lastschrift \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
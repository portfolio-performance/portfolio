package name.abuchen.portfolio.datatransfer.pdf;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractor extends AbstractPDFExtractor
{
    public TradeRepublicPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TRADE REPUBLIC");
        addBankIdentifier("Trade Republic Bank GmbH");

        addBuySellTransaction();
        addSellWithNegativeAmountTransaction();
        addBuySellCryptoTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction_Format01();
        addAccountStatementTransaction_Format02();
        addTaxesStatementTransaction();
        addTaxesCorrectionStatementTransaction();
        addDepositStatementTransaction();
        addInterestStatementTransaction();
        addFeeStatementTransaction();
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
                        + "|LIQUIDACI.N DE VALORES" //
                        + "|REINVESTIERUNG" //
                        + "|CONFIRMATION DE L.INVESTISSEMENT PROGRAMM." //
                        + "|CONFIRMATION D.EX.CUTION" //
                        + "|RELEV. DE TRANSACTION" //
                        + "|REGOLAMENTO TITOLI" //
                        + "|ZWANGS.BERNAHME" //
                        + "|TILGUNG" //
                        + "|REPAYMENT" //
                        + "|AUS.BUNG VON OPTIONSSCHEINEN)", //
                        "(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
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
                        .match("^(?i)((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?" //
                                        + "(?<type>(Kauf" //
                                        + "|Buy" //
                                        + "|Achat" //
                                        + "|Acquisto" //
                                        + "|Verkauf" //
                                        + "|Sell" //
                                        + "|Compra" //
                                        + "|Venta" //
                                        + "|Vente" //
                                        + "|Sparplanausf.hrung" //
                                        + "|SAVINGS PLAN" //
                                        + "|Ex.cution de l.investissement programm." //
                                        + "|REINVESTIERUNG" //
                                        + "|ZWANGS.BERNAHME" //
                                        + "|TILGUNG" //
                                        + "|REPAYMENT))" //
                                        + ".*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equalsIgnoreCase(v.get("type")) //
                                            || "Sell".equalsIgnoreCase(v.get("type")) //
                                            || "Venta".equalsIgnoreCase(v.get("type")) //
                                            || "Vente".equalsIgnoreCase(v.get("type")) //
                                            || "ZWANGSÜBERNAHME".equalsIgnoreCase(v.get("type")) //
                                            || "TILGUNG".equalsIgnoreCase(v.get("type")) //
                                            || "REPAYMENT".equalsIgnoreCase(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // Is type --> "Ausbuchung" change from BUY to SELL
                        .section("type").optional() //
                        .match("^[\\d] (?<type>Ausbuchung) .*[A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                        .assign((t, v) -> {
                            if ("Ausbuchung".equalsIgnoreCase(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 1 Ausbuchung Long @6.07 € TUI AG Best TurboDE000SU34220 2053 Stücke
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin") //
                                                        .match("^[\\d] Ausbuchung .*(?<currency>\\p{Sc}) (?<name>.*)(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 1 Repayment Société Générale Effekten GmbH 500 Pcs.
                                        // MiniL O.End CBOE VIX 14,94
                                        // DE000SQ728J8
                                        // 1 Market value 35.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung|Repayment) (?<name>.*) [\\.,\\d]+ (Stk|Pcs)\\.$")
                                                        .match("^(?<nameContinued>.*) [\\w]{4} [\\w]{3,4} [\\.,\\d]+$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert|Market value) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung|Repayment) (?<name>.*) [\\.,\\d]+ (Stk|Pcs)\\.$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert|Market value) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // Bundesobl.Ser.180 v.2019(24)
                                        // ISIN: DE0001141802
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "nameContinued", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
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
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Société Générale Effekten GmbH 490 Pcs. 0.886 EUR 434.14 EUR
                                        // MiniL O.End CBOE VIX 11,49
                                        // ISIN: DE000SQ6QKU9
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*) [\\w]{4} [\\w]{3,4} [\\.,\\d]+$")
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 3M Co. 7 Stk. 139,40 EUR 975,80 EUR
                                        // Registered Shares DL -,01
                                        // ISIN: US88579Y1010
                                        //
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Registered Shares o.N.
                                        // AU000000CUV3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kontron 23 Stk. 20,90 EUR 480,70 EUR
                                        // ISIN: AT0000A0E9W5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 1 Ausbuchung Long @6.07 € TUI AG Best TurboDE000SU34220 2053 Stücke
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Ausbuchung .*\\p{Sc} .*[A-Z]{2}[A-Z0-9]{9}[0-9] (?<shares>[\\.,\\d]+) St.cke$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Tencent Holdings Ltd. 0,3773 titre(s) 53,00 EUR 20,00 EUR
                                        // zjBAM Corp. 125 Pz. 29,75 EUR 3.718,75 EUR
                                        // Vonovia SE 0,781379 tít. 24,06 EUR 18,80 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.|t.t\\.) .*$") //
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
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Repayment Société Générale Effekten GmbH 500 Pcs.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Repayment .* (?<shares>[\\.,\\d]+) Pcs\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
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
                                        // Market-Order Sell on 02.05.2023 at 18:18 (Europe/Berlin).
                                        // Market-Order Achat le 10/04/2024 à 17:33 (Europe/Berlin).
                                        // Market-OrderCompra el día 01.12.2022 a las 11:56 (Europe/Berlin).
                                        // Market-OrderVenta a día 15.02.2024, a las 16:15 (Europe/Berlin) en Lang & Schwarz Exchange.
                                        // Market-Order Vente avec le numéro d'ordre 185e-c098 a été exécuté le 16/11/2023 à 16:41 (Europe/Berlin) sur la place de négociation
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(?i)((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?(Buy|Achat|Acquisto|Kauf|Verkauf|Sell|Compra|Venta|Vente) .*" //
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}" //
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}" //
                                                                        + "|[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}))" //
                                                                        + "(,)? (um|at|alle|.|a las) (?<time>[\\d]{2}:[\\d]{2}).*$") //
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
                                        // Saveback execution on 02.05.2024 on the Lang & Schwarz Exchange.
                                        // Ejecución del plan de inversión el día 02.05.2024 en Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Sparplanausf.hrung"
                                                                        + "|Ejecuci.n del plan de inversi.n"
                                                                        + "|(Savings plan|Saveback) execution) .* "
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Ausführung von Round up am 09.02.2024 an der Lang & Schwarz Exchange.
                                        // Ausführung von Saveback am 04.03.2024 an der Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ausf.hrung von (Round up|Saveback) .* "
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // DE40110101001234567890 06.08.2021 0,44 GBP
                                        // DE71100123450999999601 30.07.2024 2.05 EUR
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
                                                        .match("^(GESAMT|TOTAL|TOTALE) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // VERRECHNUNGSKONTO DATUM DER ZAHLUNG
                                        // DE71100123450999999601 30.07.2024 2.05 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("VERRECHNUNGSKONTO DATUM DER ZAHLUNG")
                                                        .match("^[\\w]+ ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // TITRE QUANTITÉ PRIX ESTIMATION DU MONTANT DE VENTE
                                        // WisdomTree Multi Ass.Iss.PLC 0,477676 titre(s) 112,427 EUR 53,703679652 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("TITRE QUANTITÉ PRIX ESTIMATION DU MONTANT DE VENTE")
                                                        .match("^.* [\\.,\\d]+ titre\\(s\\) [\\.,\\d]+ [\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
                                        // 12345 CIudad ORDEN 1b03-784c
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*ORDEN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Orden: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // 75000 Paris ORDRE 69da-1c6f
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*ORDRE (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Ordre : " + trim(v.get("note")))),
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
                                        // 12345 CIudad EJECUCIÓN ff4d-982a
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*EJECUCI.N (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote("Ejecución: " + trim(v.get("note")))),
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
                                        // PLAN DE INVERSIÓN 21ef-595a
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^PLAN DE INVERSIÓN (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Plan de Invesión: "))),
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
                                        // EJECUCIÓN d4e7-9ecc
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^EJECUCI.N (?<note>.*\\-.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | Ejecución: "))),
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
                        + "|SECURITIES SETTLEMENT" //
                        + "|REGOLAMENTO TITOLI" //
                        + "|LIQUIDACI.N DE VALORES" //
                        + "|CONFIRMATION D.EX.CUTION)", //
                        "(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(.*\\-Order )?(Verkauf|Sell|Venta|Vente).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
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
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // Bundesobl.Ser.180 v.2019(24)
                                        // ISIN: DE0001141802
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "nameContinued", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kontron 23 Stk. 20,90 EUR 480,70 EUR
                                        // ISIN: AT0000A0E9W5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Tencent Holdings Ltd. 0,3773 titre(s) 53,00 EUR 20,00 EUR
                                        // zjBAM Corp. 125 Pz. 29,75 EUR 3.718,75 EUR
                                        // Vonovia SE 0,781379 tít. 24,06 EUR 18,80 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.|t.t\\.) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Berkshire Hathaway Inc. 0.3367 Pcs. 297.00 EUR 100.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Market-Order Verkauf am 18.06.2019, um 17:50 Uhr an der Lang & Schwarz Exchange.
                        // Stop-Market-Order Verkauf am 10.06.2020, um 11:42 Uhr.
                        // Limit-Order Verkauf am 21.07.2020, um 09:30 Uhr an der Lang & Schwarz Exchange.
                        // Verkauf am 26.02.2021, um 11:44 Uhr.
                        // Market-Order Sell on 02.05.2023 at 18:18 (Europe/Berlin).
                        // Market-OrderVenta a día 15.02.2024, a las 16:15 (Europe/Berlin) en Lang & Schwarz Exchange.
                        // Market-Order Vente avec le numéro d'ordre 185e-c098 a été exécuté le 16/11/2023 à 16:41 (Europe/Berlin) sur la place de négociation
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(?i)((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?(Verkauf|Sell|Venta|Vente) .*" //
                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}" //
                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}" //
                                        + "|[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}))" //
                                        + "(,)? (um|at|alle|.|a las) (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // GESAMT 0,34 EUR
                        // GESAMT -0,66 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("^(GESAMT|TOTAL|TOTALE) [\\.,\\d]+ [\\w]{3}$") //
                        .match("^(GESAMT|TOTAL|TOTALE) (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("negative", true))

                        // @formatter:off
                        // GESAMT -0,66 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("^(GESAMT|TOTAL|TOTALE) (?<negative>\\-)[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                type.getCurrentContext().putBoolean("negative", true);
                        })

                        // @formatter:off
                        // Fremdkostenzuschlag -1,00 EUR
                        // External cost surcharge -1.00 EUR
                        // Frais externes -1,00 EUR
                        // @formatter:on
                        .section("currency", "amount").optional() //
                        .match("^(Fremdkostenzuschlag|External cost surcharge|Frais externes) \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
        DocumentType type = new DocumentType("(ABRECHNUNG CRYPTOGESCH.FT|CRYPTO SPARPLAN|ABRECHNUNG CRYPTO SAVEBACK)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$", "^Diese Abrechnung wird maschinell erstellt.*$");
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
                                        // Saveback Ausführung am 02.09.2024 im außerbörslichen Handel Bankhaus Scheich.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Sparplanausf.hrung|Saveback) .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
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
                        + "|DISTRIBUZIONE" //
                        + "|Distribution" //
                        + "|KAPITALREDUKTION)", //
                        "ABRECHNUNG ZINSEN");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$"); //
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .optionalOneOf(
                                        // @formatter:off
                                        // Storno der Dividende mit dem Ex-Tag 29.11.2019.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Storno) der Dividende .*$") //
                                                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported)),
                                        // @formatter:off
                                        // STORNIERUNG DER DIVIDENDE
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>STORNIERUNG) DER DIVIDENDE$") //
                                                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported)))

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
                                        //
                                        // Apple Inc. 1,92086 titre(s) 0,25 USD 0,48 USD
                                        // Registered Shares o.N.
                                        // ISIN : US0378331005
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pz\\.|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
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
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Euro iSTOXX ex FIN Dividend+ EUR (Dist) 206.651869 Stücke 0.48 EUR 99.19 EURDE000ETFL482
                                        //
                                        // POSITION QUANTITY YIELD AMOUNT
                                        // Vici Properties 30.000000 Pcs. 0.415 USD 12.45 USDUS9256521090
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .find("POSITION (ANZAHL|QUANTITY) (Ertrag|ERTRAG|ERTR.GNIS|YIELD) (BETRAG|AMOUNT)")
                                                        .match("^(?<name>.*) [\\.,\\d]+ (St.cke|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // MSCI World USD (Dist) 123.45 USD
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        //
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Euro iSTOXX ex FIN 0.33 EUR
                                        // DE000ETFL482 1.525759 Stücke -0.215 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*) [\\.,\\d]+ [\\w]{3}")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Developed Markets Dividend Leaders EUR (Dist)
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*)")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // NVIDIA 0.32 USD
                                        // 32.000000 Stücke 0.01 USD
                                        // US67066G1040
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*) [\\.,\\d]+ [\\w]{3}")
                                                        .match("[\\.,\\d]+ St.cke [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION QUANTITY YIELD AMOUNT
                                        // Mondelez
                                        // US6092071058 2.000000 Pcs. 0.425 USD
                                        //
                                        // POSITION QUANTITÉ TAUX MONTANT
                                        // Microsoft
                                        // US5949181045 0.561914 unit. 0.75 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION (QUANTITY|QUANTIT.) (YIELD|TAUX) (AMOUNT|MONTANT)")
                                                        .match("^(?<name>.*)")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ (Pcs|unit)\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        // NL0011683594 90.537929 Stücke 0.87 EUR
                                        // Euro iSTOXX ex FIN Dividend+ EUR (Dist) 206.651869 Stücke 0.48 EUR 99.19 EURDE000ETFL482
                                        // Vici Properties 30.000000 Pcs. 0.415 USD 12.45 USDUS9256521090
                                        // DE000ETFL482 1.525759 Stücke -0.215 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (St.cke|Pcs\\.) (\\-)?[\\.,\\d]+ [\\w]{3}.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // US6092071058 2.000000 Pcs. 0.425 USD
                                        // US5949181045 0.561914 unit. 0.75 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Pcs|unit)\\. [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 32.000000 Stücke 0.01 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) St.cke [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Reinvestierung|Kapitalmaßnahme) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // DExxxxxx 25.09.2019 4,18 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        //
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // DE98502109007017811111 16/05/2024 0,38 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

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
                                                        }),
                                        // @formatter:off
                                        // GESAMT 5,63 USD
                                        // Zwischensumme 1,102 EUR/USD 5,11 EUR
                                        //
                                        // TOTALE 18,61 CAD
                                        // Subtotale 1,46055 EUR/CAD 9,56 EUR
                                        //
                                        // TOTAL 0.02 USD
                                        // Subtotal 1.0715 EUR/USD 0.02 EUR
                                        //
                                        // TOTAL 0,48 USD
                                        // Sous-total 1,0802 EUR/USD 0,38 EUR
                                        //
                                        // Zwischensumme 21.42 USD
                                        // Zwischensumme 1.073 USD/EUR 19.97 EUR
                                        //
                                        // Sub Total 0.72 USD
                                        // Sub Total 1.0855 USD/EUR 0.66 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "baseCurrency", "termCurrency", "gross", "currency") //
                                                        .match("^(Zwischensumme|GESAMT|TOTALE|TOTAL|Sub Total) (\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$") //
                                                        .match("^(Zwischensumme|Subtotale|Subtotal|Sous\\-total|Sub Total) (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?(?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!asCurrencyCode(v.get("currency")).equals(asCurrencyCode(v.get("baseCurrency"))))
                                                            {
                                                                v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                                                v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));
                                                            }

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

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

    private void addAccountStatementTransaction_Format01()
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

        // @formatter:off
        // 15.04.2020 Accepted PayIn:DE00000000000000 to DE0000000000000000 150,00
        // 28.01.2021 Einzahlung akzeptiert: DE12345678912345678912 auf 123,45
        // 01.04.2023 Customer c3e9411b-b0ab-4820-a3b3-48cd72f6963b inpayed net: 700,00
        // @formatter:on
        Block depositBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                        + "(Accepted PayIn" //
                        + "|Einzahlung akzeptiert" //
                        + "|Customer).*$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(1);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) " //
                                        + "(Accepted PayIn" //
                                        + "|Einzahlung akzeptiert" //
                                        + "|Customer .* inpayed net):.* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01.01.2023 Auszahlung an Referenzkonto -11.111,11
        // @formatter:on
        Block removalBlock = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Auszahlung an Referenzkonto.*$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(1);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) Auszahlung an Referenzkonto \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 04.11.2020 Steueroptimierung null 0000000000000000 4,20
        // @formatter:on
        Block taxRefundBlock = new Block("([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Steueroptimierung.*");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.setMaxSize(1);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}-[\\d]{2}-[\\d]{2})) Steueroptimierung.* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01.04.2023 Your interest payment 00,01
        // @formatter:on
        Block interestBlock = new Block("([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Your interest payment.*");
        type.addBlock(interestBlock);
        interestBlock.setMaxSize(1);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}-[\\d]{2}-[\\d]{2})) Your interest payment (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;
                            return null;
                        }));
    }

    private void addAccountStatementTransaction_Format02()
    {
        final DocumentType type = new DocumentType("(KONTO.BERSICHT" //
                        + "|RESUMEN DE ESTADO DE CUENTA" //
                        + "|SYNTH.SE DU RELEV. DE COMPTE" //
                        + "|ACCOUNT STATEMENT SUMMARY)", (context, lines) -> { //
            Pattern pAccountAmountTransaction_Format01 = Pattern.compile("^(?!(Depotkonto|Cuenta de valores|Compte titres)).*[\\.,\\d]+ \\p{Sc} (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$");
            Pattern pAccountAmountTransaction_Format02 = Pattern.compile("^(?!(Securities Account)).*\\p{Sc}[\\.,\\d]+ (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+).*$");

            Pattern pAccountInitialSaldoTransaction_Format01 = Pattern.compile("^(Depotkonto|Cuenta de valores|Compte titres) (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc}$");
            Pattern pAccountInitialSaldoTransaction_Format02 = Pattern.compile("^(Securities Account) (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+ \\p{Sc}[\\.,\\d]+ \\p{Sc}[\\.,\\d]+$");

            AccountAmountTransactionHelper accountAmountTransactionHelper = new AccountAmountTransactionHelper();
            context.putType(accountAmountTransactionHelper);

            List<AccountAmountTransactionItem> itemsToAddToFront = new ArrayList<>();

            for (int i = 0; i < lines.length; i++)
            {
                Matcher m = pAccountInitialSaldoTransaction_Format01.matcher(lines[i]);
                if (m.matches())
                {
                    AccountAmountTransactionItem item = new AccountAmountTransactionItem();
                    item.line = i + 1;
                    item.currency = asCurrencyCode(m.group("currency"));
                    item.amount = asAmount(m.group("amount"));

                    itemsToAddToFront.add(item);
                }

                m = pAccountInitialSaldoTransaction_Format02.matcher(lines[i]);
                if (m.matches())
                {
                    AccountAmountTransactionItem item = new AccountAmountTransactionItem();
                    item.line = i + 1;
                    item.currency = asCurrencyCode(m.group("currency"));
                    item.amount = asAmount(m.group("amount"));

                    itemsToAddToFront.add(item);
                }
            }

            // Add items from pAccountInitialSaldoTransaction to the beginning of the list
            accountAmountTransactionHelper.items.addAll(0, itemsToAddToFront);

            for (int i = 0; i < lines.length; i++)
            {
                Matcher m = pAccountAmountTransaction_Format01.matcher(lines[i]);
                if (m.matches())
                {
                    AccountAmountTransactionItem item = new AccountAmountTransactionItem();
                    item.line = i + 1;
                    item.currency = asCurrencyCode(m.group("currency"));
                    item.amount = asAmount(m.group("amount"));

                    accountAmountTransactionHelper.items.add(item);
                }

                m = pAccountAmountTransaction_Format02.matcher(lines[i]);
                if (m.matches())
                {
                    AccountAmountTransactionItem item = new AccountAmountTransactionItem();
                    item.line = i + 1;
                    item.currency = asCurrencyCode(m.group("currency"));
                    item.amount = asAmount(m.group("amount"));

                    accountAmountTransactionHelper.items.add(item);
                }
            }
        });
        this.addDocumentTyp(type);

        Block depositRemovalBlock_Format01 = new Block("^[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?( [\\d]{4})? " //
                        + "(.berweisung" //
                        + "|Transfer" //
                        + "|Referral Refund" //
                        + "|Kartentransaktion) .*$");
        type.addBlock(depositRemovalBlock_Format01);
        depositRemovalBlock_Format01.setMaxSize(1);
        depositRemovalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 02 Apr. Überweisung Einzahlung akzeptiert: DE7243872432 auf 2024 DE7243872432 1.200,00 € 51.352,41 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?) .berweisung Einzahlung .* auf (?<year>[\\d]{4}) .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 16 Apr. 2024 Überweisung Einzahlung akzeptiert: DE5987654321 auf DE12334567658 3.500,00 € 16.347,54 €
                                        // 23 Juli 2024 Überweisung Incoming transfer from KLEslAT zxcWeqg 1.100,00 € 56.457,39 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) .berweisung (Einzahlung|Incoming) .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 24 Sep 2024 Transfer Deposit accepted: DE12345678901234567890 to DE12345678901234567890 €100.00 €103.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) Transfer Deposit .* (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 26 Sep 2024 Transfer PayOut to transit €15.99 €0.00
                                        // 24 Sep 2024 Referral Refund for your gift €10.01 €113.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) (Transfer PayOut|Referral Refund) .* (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 23 Apr. 2024 Kartentransaktion ALDI SAGT DANKE 37,82 € 46.424,80 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) Kartentransaktion (?<note>.*) (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 01 Apr. 2024 Überweisung PayOut to transit 172,23 € 50.000,00 €
                                        // 26 Sep 2024 Transfer PayOut to transit €15.99 €0.00
                                        // 22 Juli 2024 Überweisung Outgoing transfer for EMYRMzk QpSHhzd 200,00 € 55.357,39 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) .berweisung (PayOut|Outgoing).* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 20 Sep 2024 Transfer Google Pay Top up €100.00 €100.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) Transfer (?<note>.*) (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})(?<amountAfter>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(asCurrencyCode(v.get("currencyAfter")), asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class).orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.REMOVAL);
                                                            }

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new));

        Block depositRemovalBlock_Format02 = new Block("^[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?[\\s].*$");
        type.addBlock(depositRemovalBlock_Format02);
        depositRemovalBlock_Format02.setMaxSize(4);
        depositRemovalBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 20 may Transacción BACKBLAZE INC, 7,38 $, exchange rate: 0,9227642, ECB rate: 0,9221689414, markup:
                                        // 2024 con tarjeta 0,06454984 % 6,81 € 8.204,96 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "note", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]Transacci.n (?<note>.*), [\\.,\\d]+ \\p{Sc}.*$") //
                                                        .match("^(?<year>[\\d]{4}) con tarjeta .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (?<amountAfter>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(asCurrencyCode(v.get("currencyAfter")), asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class).orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.REMOVAL);
                                                            }

                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 07 Apr.
                                        // 2024 Kartentransaktion Backerei XAXRs 798 2,50 € 51.194,91 €
                                        //
                                        // 16 Apr.
                                        // 2024 Kartentransaktion Visa Geld zurueck Aktion 0,08 € 18.584,55 €
                                        //
                                        // 02 may Transacción
                                        // 2024 con tarjeta WATSON RESTAURANTS 30,99 € 18.261,16 €
                                        //
                                        // 28 juil.
                                        // 2024 Virement Apple Pay Top up 100,00 € 104,30 €
                                        //
                                        // 16 oct.
                                        // 2023 Parrainage Remboursement de votre cadeau 25,57 € 111,58 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "note", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s].*$") //
                                                        .match("^(?<year>[\\d]{4}) " //
                                                                        + "(Kartentransaktion|con tarjeta|Virement|Parrainage) " //
                                                                        + "(?<note>(?!(Einzahlung|Ingreso|Paiement)).*) " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) " //
                                                                        + "(?<amountAfter>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(asCurrencyCode(v.get("currencyAfter")), asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class).orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.REMOVAL);
                                                            }

                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 30 Apr.
                                        // 2024 Überweisung
                                        // Einzahlung akzeptiert: DE00000000000000000000 auf
                                        // DE00000000000000000000 1.200,00 € 18.514,32 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$") //
                                                        .match("^(?<year>[\\d]{4}) .berweisung$") //
                                                        .match("^Einzahlung akzeptiert: .*$") //
                                                        .match("^.* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 02 Okt.
                                        // 2023 Überweisung Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000 1.200,00 € 5.427,30 €
                                        //
                                        // 02 Mai
                                        // 2024 Prämie Your Saveback payment 4,94 € 18.524,98 €
                                        //
                                        // 02 may
                                        // 2024 Transferencia Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000 2.600,00 € 18.292,15 €
                                        //
                                        // 02 may
                                        // 2024 Recompensa Your Saveback payment 3,89 € 18.265,05 €
                                        //
                                        // 30 Juli
                                        // 2024 Überweisung Incoming transfer from Vorname Nachname 2.000,00 € 17.796,12 €
                                        //
                                        // 01 juil.
                                        // 2024 Virement Paiement accepté: DE98200411330722961000 à DE21502109007019521081 500,00 € 897,24 €
                                        //
                                        // 02 sept.
                                        // 2024 Virement Paiement accepté: FR7634047446300402044310454 à DE55402154005012458754 100,00 € 131,09 €
                                        //
                                        // 08 abr
                                        // 2024 Recomendación Reembolso por tu regalo 9,96 € 6.337,75 €
                                        //
                                        // 30 März
                                        // 2020 Überweisung Accepted PayIn:DE74500400480142038900 to DE30110101008889827581 4.000,00 € 4.000,00 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$") //
                                                        .match("^(?<year>[\\d]{4}) " //
                                                                        + "(.berweisung" //
                                                                        + "|Pr.mie" //
                                                                        + "|Transferencia" //
                                                                        + "|Recompensa" //
                                                                        + "|Recomendaci.n" //
                                                                        + "|Virement) " //
                                                                        + "(Einzahlung akzeptiert:" //
                                                                        + "|Accepted PayIn:" //
                                                                        + "|Ingreso aceptado:" //
                                                                        + "|Paiement accept.:" //
                                                                        + "|Your Saveback" //
                                                                        + "|Your Saveback payment" //
                                                                        + "|Incoming transfer from" //
                                                                        + "|Reembolso)" //
                                                                        + ".* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 01 août
                                        // 2024 Virement Paiement accepté: DE98200411330722961000 à DE21502109007019521081 500,00 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$") //
                                                        .match("^(?<year>[\\d]{4}) " //
                                                                        + "(.berweisung" //
                                                                        + "|Pr.mie" //
                                                                        + "|Transferencia" //
                                                                        + "|Recompensa" //
                                                                        + "|Virement) " //
                                                                        + "(Einzahlung akzeptiert:" //
                                                                        + "|Ingreso aceptado:" //
                                                                        + "|Paiement accept.:" //
                                                                        + "|Your Saveback" //
                                                                        + "|Your Saveback payment" //
                                                                        + "|Incoming transfer from)" //
                                                                        + ".* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 20 Mai
                                        // 2024 Überweisung PayOut to transit 18.085,60 € 18.939,80 €
                                        //
                                        // 03 may
                                        // 2024 Transferencia PayOut to transit 8.000,00 € 10.265,05 €
                                        //
                                        // 26 Juli
                                        // 2024 Überweisung Outgoing transfer for Vorname Nachname 22.000,00 € 15.825,42 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$") //
                                                        .match("^(?<year>[\\d]{4}) " //
                                                                        + "(.berweisung" //
                                                                        + "|Transferencia) " //
                                                                        + "(PayOut to transit" //
                                                                        + "|Outgoing transfer for.*) " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) " //
                                                                        + "[\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block depositRemovalBlock_Format03 = new Block("^[\\d]{2}[\\s]$");
        type.addBlock(depositRemovalBlock_Format03);
        depositRemovalBlock_Format03.setMaxSize(4);
        depositRemovalBlock_Format03.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 28
                                        // Apr. Kartentransaktion Hornbach Baumarkt AG FIL. 2,40 € 1.902,38 €
                                        // 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "year", "note", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$") //
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) Kartentransaktion " //
                                                                        + "(?<note>.*) " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (?<amountAfter>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})$") //
                                                        .match("^(?<year>[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(asCurrencyCode(v.get("currencyAfter")), asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class).orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.REMOVAL);
                                                            }

                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 04
                                        // Apr. Überweisung Einzahlung akzeptiert: DE34120300001066107218 auf DE00000000000000000000 2.000,00 € 3.030,97 €
                                        // 2024
                                        //
                                        // 03
                                        // Juni Prämie Your Saveback payment 5,18 € 3.484,00 €
                                        // 2024
                                        //
                                        // 29
                                        // Aug. Überweisung Incoming transfer from Vorname Nachname 2.500,00 € 19.885,07 €
                                        // 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$") //
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) " //
                                                                        + "(.berweisung Einzahlung akzeptiert:" //
                                                                        + "|.berweisung Incoming transfer from" //
                                                                        + "|Pr.mie Your Saveback)" //
                                                                        + ".* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .match("^(?<year>[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 02
                                        // Aug. Überweisung Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000 1.200,00 € 9.205,89 €2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$") //
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) " //
                                                                        + ".berweisung Einzahlung akzeptiert:.* " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}(?<year>[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 20
                                        // Sept. Überweisung Einzahlung akzeptiert: DE00000000000000000000 auf
                                        // 2024 DE00000000000000000000
                                        // 889,77 € 964,18 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$") //
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) .berweisung Einzahlung akzeptiert: .*") //
                                                        .match("^(?<year>[\\d]{4}) .*$") //
                                                        .match("(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (?<amountAfter>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(asCurrencyCode(v.get("currencyAfter")), asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class).orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.REMOVAL);
                                                            }

                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // 14
                                        // Juni Überweisung PayOut to transit 6.500,00 € 50.860,76 €
                                        // 2024
                                        //
                                        // 19
                                        // Aug. Überweisung Outgoing transfer for Vorname Nachname 900,00 € 17.385,07 €
                                        // 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$") //
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) " //
                                                                        + ".berweisung (PayOut to transit|Outgoing transfer for).* " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .match("^(?<year>[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // 03 Apr.
        // 2024 Gebühren Trade Republic Card 5,00 € 49.997,41 €
        //
        // 03 abr
        // 2024 Comisión Trade Republic Card 5,00 € 6.402,79 €
        // @formatter:on
        Block feesBlock_Format01 = new Block("^[\\d]{2} [\\w]{3,4}([\\.]{1})?[\\s]$");
        type.addBlock(feesBlock_Format01);
        feesBlock_Format01.setMaxSize(2);
        feesBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("date", "year", "note", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$") //
                                                        .match("^(?<year>[\\d]{4}) .* (?<note>Trade Republic Card) " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // 16 Apr. 2024 Gebühren Trade Republic Card 5,00 € 46.462,62 €
        // @formatter:on
        Block feesBlock_Format02 = new Block("^[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?( [\\d]{4})? .* Trade Republic Card [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc}$");
        type.addBlock(feesBlock_Format02);
        feesBlock_Format02.setMaxSize(1);
        feesBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("date", "note", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4}) .* (?<note>Trade Republic Card) " //
                                                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));


        // @formatter:off
        // 01 Apr. 2024 Zinszahlung Your interest payment 172,23 € 50.172,23 €
        // @formatter:on
        Block interestBlock_Format01 = new Block("^[\\d]{2} [\\p{L}]{3,4}([\\.]{1})? [\\d]{4} Zinszahlung .*$");
        type.addBlock(interestBlock_Format01);
        interestBlock_Format01.setMaxSize(1);
        interestBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("date", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} ([\\p{L}]{3,4}([\\.]{1})?) [\\d]{4}) (Zinszahlung|intereses|d.int.r.ts) .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionAlternativeDocumentRequired);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;
                            return null;
                        }));

        // @formatter:off
        // 01 Apr.
        // 2024 Zinszahlung Your interest payment 147,34 € 50.152,41 €
        //
        // 01 may Pago de
        // 2024 intereses Your interest payment 26,13 € 15.692,15 €
        //
        // 01 juil. Paiement
        // 2024 d'intérêts Your interest payment 1,24 € 397,24 €
        //
        // 01 août Paiement
        // 2024 d'intérêts Your interest payment 1,30 € 605,60 €
        // @formatter:on
        Block interestBlock_Format02 = new Block("^[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?[\\s]((Pago|Paiement).*)?$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.setMaxSize(2);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]((Pago|Paiement).*)?$")
                                                        .match("^(?<year>[\\d]{4}) (Zinszahlung|intereses|d.int.r.ts) Your interest payment (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionAlternativeDocumentRequired);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;
                            return null;
                        }));

        // @formatter:off
        // 01
        // Feb. Zinszahlung Your interest payment 33,37 € 10.671,44 €
        // 2024
        // @formatter:on
        Block interestBlock_Format03 = new Block("^[\\d]{2}[\\s]$");
        type.addBlock(interestBlock_Format03);
        interestBlock_Format03.setMaxSize(3);
        interestBlock_Format03.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$")
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) (Zinszahlung|intereses|d.int.r.ts) Your interest payment (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .match("^(?<year>[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month")
                                                                            + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionAlternativeDocumentRequired);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;
                            return null;
                        }));

        // @formatter:off
        // 02 Aug.
        // 2024 Steuern Tax Optimisation 6,54 € 55.938,40 €
        // @formatter:on
        Block taxesBlock_Format01 = new Block("^[\\d]{2} [\\w]{3,4}([\\.]{1})?[\\s]$");
        type.addBlock(taxesBlock_Format01);
        taxesBlock_Format01.setMaxSize(2);
        taxesBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("date", "year", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2} [\\p{L}]{3,4}([\\.]{1})?)[\\s]$")
                                                        .match("^(?<year>[\\d]{4}) Steuern (Steueroptimierung |Tax Optimisation|Kapitalertragssteueroptimierung ).* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc}$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // 19
        // Feb. Steuern Steueroptimierung null 0000000000000000 78,17 € 715,35 €
        // 2024
        //
        // 04
        // Juni Steuern Kapitalertragssteueroptimierung Depot Direktverkauf IE00BGJWWY63 INVESCOM2
        // 2024 EUR GOVB1-3Y A 5008723420240604
        //
        // 14
        // Juni Steuern Tax Optimisation 0,01 € 50.860,75 €
        // 2024
        // @formatter:on
        Block taxesBlock_Format02 = new Block("^[\\d]{2}[\\s]$");
        type.addBlock(taxesBlock_Format02);
        taxesBlock_Format02.setMaxSize(3);
        taxesBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("day", "month", "year", "amount", "currency", "amountAfter", "currencyAfter") //
                                                        .match("^(?<day>[\\d]{2})[\\s]$")
                                                        .match("^(?<month>[\\p{L}]{3,4}([\\.]{1})?) Steuern (Steueroptimierung |Tax Optimisation|Kapitalertragssteueroptimierung ).* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (?<amountAfter>[\\.,\\d]+) (?<currencyAfter>\\p{Sc})$") //
                                                        .match("^(?<year>[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();
                                                            Money amountAfter = Money.of(
                                                                            asCurrencyCode(v.get("currencyAfter")),
                                                                            asAmount(v.get("amountAfter")));

                                                            AccountAmountTransactionHelper accountAmountTransactionHelper = context.getType(AccountAmountTransactionHelper.class)
                                                                            .orElseGet(AccountAmountTransactionHelper::new);
                                                            Optional<AccountAmountTransactionItem> item = accountAmountTransactionHelper.findItem(v.getStartLineNumber(), amountAfter);

                                                            if (item.isPresent())
                                                            {
                                                                Money amountBefore = Money.of(item.get().currency, item.get().amount);

                                                                if (amountBefore.isGreaterThan(amountAfter))
                                                                    t.setType(AccountTransaction.Type.TAXES);
                                                            }

                                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private void addTaxesStatementTransaction()
    {
        final DocumentType type = new DocumentType("(STEUERABRECHNUNG" //
                        + "|STEUERLICHE OPTIMIERUNG" //
                        + "|ABRECHNUNG ZINSEN)", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // IBAN BUCHUNGSDATUM GUTSCHRIFT NACH STEUERN
                                        // DE10123456789123456789 01.02.2023 0,88 EUR
                                        //
                                        // VERRECHNUNGSKONTO VALUTA BETRAG
                                        // DE00000000000000000000 23.11.2020 4,26
                                        //
                                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                                        // DE30501108019081234567 11.04.2024 2,21 EUR
                                        //
                                        // VERRECHNUNGSKONTO DATUM DER ZAHLUNG BETRAG
                                        // DE12345678912345678912 28.09.2024 -0.27 EUR
                                        //
                                        // IBAN BUCHUNGSDATUM GESAMT
                                        // DE12321546856552266333 01.07.2024 74,08 EUR
                                        //
                                        // @formatter:on
                                        .section("date") //
                                        .find("(IBAN BUCHUNGSDATUM (GUTSCHRIFT NACH STEUERN|GESAMT)|VERRECHNUNGSKONTO (VALUTA|WERTSTELLUNG|DATUM DER ZAHLUNG) BETRAG)") //
                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\-)?[\\.,\\d]+( [\\w]{3})?$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("date", v.get("date"));
                                            ctx.putBoolean("multipleTransactionInterests", false);
                                            ctx.putBoolean("multipleTransactionDividende", false);
                                            ctx.putBoolean("otherTaxesTransaction", false);
                                        })

                                        // @formatter:off
                                        // Cash Zinsen 4,00% 01.06.2024 - 11.06.2024 61,11 EUR
                                        // Cash Zinsen 3,75% 12.06.2024 - 30.06.2024 99,39 EUR
                                        // @formatter:on
                                        .section("multipleTransactionInterests").optional() //
                                        .match("^(?<multipleTransactionInterests>Cash) Zinsen [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .match("^Cash Zinsen [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .assign((ctx, v) -> ctx.putBoolean("multipleTransactionInterests", true))

                                        // @formatter:off
                                        // Geldmarkt Dividende 3,75% 01.09.2024 - 17.09.2024 8,05 EUR
                                        // Geldmarkt Dividende 3,50% 18.09.2024 - 30.09.2024 8,03 EUR
                                        // @formatter:on
                                        .section("multipleTransactionDividende").optional() //
                                        .match("^(?<multipleTransactionDividende>Geldmarkt) Dividende [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .match("^Geldmarkt Dividende [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .assign((ctx, v) -> ctx.putBoolean("multipleTransactionDividende", true))

                                        // @formatter:off
                                        // HSBC Trinkaus & Burkhardt GmbH 400 Stk. 2,21 EUR
                                        // TurboC O.End salesfor
                                        // ISIN: DE000HS33QJ0
                                        // @formatter:on
                                        .section("name", "shares", "nameContinued", "isin").optional() //
                                        .match("^(?<name>.*) (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [\\w]{3}$") //
                                        .match("^(?<nameContinued>.*)$") //
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("name", v.get("name"));
                                            ctx.put("shares", v.get("shares"));
                                            ctx.put("nameContinued", v.get("nameContinued"));
                                            ctx.put("isin", v.get("isin"));
                                        }));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^ABRECHNUNG( \\- (ZINSEN|DIVIDENDE))?$", "^(?i)Gesamt (\\-)?[\\.,\\d]+( [\\w]{3})?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ABRECHNUNG - ZINSEN
                                        // POSITION BETRAG
                                        // Besteuerungsgrundlage 160,50 EUR
                                        // Kapitalertragssteuer -40,13 EUR
                                        // Solidaritätszuschlag -2,20 EUR
                                        // Gesamt 118,17 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "gross", "amount", "currency") //
                                                        .documentContext("date")
                                                        .find("ABRECHNUNG( \\- ZINSEN)?")
                                                        .match("^Besteuerungsgrundlage (?<note>(?<gross>[\\.,\\d]+) [\\w]{3})$") //
                                                        .match("^(?i)Gesamt (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setMonetaryAmount(gross.subtract(amount));
                                                            t.setNote("Abrechnung Zinsen (" + v.get("note") + ")");
                                                        }),
                                        // @formatter:off
                                        // ABRECHNUNG - DIVIDENDE
                                        // POSITION BETRAG
                                        // Besteuerungsgrundlage 16,08 EUR
                                        // Kapitalertragssteuer -4,02 EUR
                                        // Solidaritätszuschlag -0,22 EUR
                                        // Gesamt 11,84 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "gross", "amount", "currency") //
                                                        .documentContext("date")
                                                        .find("ABRECHNUNG( \\- DIVIDENDE)?")
                                                        .match("^Besteuerungsgrundlage (?<note>(?<gross>[\\.,\\d]+) [\\w]{3})$") //
                                                        .match("^(?i)Gesamt (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setMonetaryAmount(gross.subtract(amount));
                                                            t.setNote("Abrechnung Dividende (" + v.get("note") + ")");
                                                        }),
                                        // @formatter:off
                                        // Steuerliche Optimierung am 28.09.2024
                                        // ABRECHNUNG
                                        // POSITION BETRAG
                                        // Kapitalertragssteuer -0.26 EUR
                                        // Solidaritätszuschlag -0.01 EUR
                                        // GESAMT -0.27 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .documentContext("date")
                                                        .match("^Kapitalertrags(s)?teuer( Optimierung)? \\-[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .match("^(?i)Gesamt \\-(?<amount>[\\.,\\d]+)( [\\w]{3})?$") //
                                                        .assign((t, v) -> {
                                                            type.getCurrentContext().putBoolean("otherTaxesTransaction", true);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Steuerliche Optimierung am 18.07.2024
                                        // ABRECHNUNG
                                        // POSITION BETRAG
                                        // Kapitalertragssteuer 735.67 EUR
                                        // Solidaritätszuschlag 40.46 EUR
                                        // GESAMT 776.13 EUR
                                        //
                                        // STEUERABRECHNUNG
                                        // ABRECHNUNG
                                        // POSITION BETRAG
                                        // Kapitalertragsteuer Optimierung 3,75 EUR
                                        // Solidaritätszuschlag Optimierung 0,21 EUR
                                        // Kirchensteuer Optimierung 0,30 EUR
                                        // GESAMT 4,26
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .documentContext("date")
                                                        .match("^Kapitalertrags(s)?teuer( Optimierung)? [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .match("^(?i)Gesamt (?<amount>[\\.,\\d]+)( [\\w]{3})?$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAX_REFUND);
                                                            type.getCurrentContext().putBoolean("otherTaxesTransaction", true);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // ABRECHNUNG
                                        // POSITION BETRAG
                                        // Erstattung US-Quellensteuer 2,21 EUR
                                        // GESAMT 2,21 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .documentContext("date", "name", "shares", "nameContinued", "isin")
                                                        .match("^(?i)Gesamt (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAX_REFUND);
                                                            type.getCurrentContext().putBoolean("otherTaxesTransaction", true);

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (type.getCurrentContext().getBoolean("multipleTransactionInterests"))
                            {
                                // If we have multiple entries in the document,
                                // then the flag must be removed.
                                type.getCurrentContext().remove("multipleTransactionInterests");
                                return item;
                            }

                            if (type.getCurrentContext().getBoolean("multipleTransactionDividende"))
                            {
                                // If we have multiple entries in the document,
                                // then the flag must be removed.
                                type.getCurrentContext().remove("multipleTransactionDividende");
                                return item;
                            }

                            if (type.getCurrentContext().getBoolean("otherTaxesTransaction"))
                            {
                                // If we have multiple entries in the document,
                                // then the flag must be removed.
                                type.getCurrentContext().remove("otherTaxesTransaction");
                                return item;
                            }

                            return null;
                        });
    }

    private void addTaxesCorrectionStatementTransaction()
    {
        DocumentType type = new DocumentType("STEUERKORREKTUR");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(STORNO )?STEUERKORREKTUR$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // STORNO STEUERKORREKTUR
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>STORNO) STEUERKORREKTUR$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

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
                        // Quellensteuer für US-Emittent 6,94 USD
                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                        // DE00000000000000000000 02.11.2023 6,30 EUR
                        //
                        // Quellensteuer für US-Emittent -6,94 USD
                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                        // DE00000000000000000000 02.11.2023 -6,30 EUR
                        // @formatter:on
                        .section("amount", "currency", "date") //
                        .find("^Quellensteuer f.r US\\-Emittent (\\-)?[\\.,\\d]+ ([\\w]{3})$") //
                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                        .match("^.* (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addDepositStatementTransaction()
    {
        DocumentType type = new DocumentType("(ABRECHNUNG EINZAHLUNG" //
                        + "|R.GLEMENT DU VERSEMENT)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                                        // DEXXxxxxxxxxxxXXXXXXXXX 07.01.2024 49,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // COMPTE-ESPÈCES DATE DE VALEUR MONTANT
                                        // DE13502109007011547146 24/05/2023 1000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .find("COMPTE\\-ESP.CES DATE DE VALEUR MONTANT") //
                                                        .match("^.* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // VERRECHNUNGSKONTO WERTSTELLUNG BETRAG
                                        // DEXXxxxxxxxxxxXXXXXXXXX 07.01.2024 49,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("VERRECHNUNGSKONTO WERTSTELLUNG BETRAG") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // COMPTE-ESPÈCES DATE DE VALEUR MONTANT
                                        // DE13502109007011547146 24/05/2023 1000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("COMPTE\\-ESP.CES DATE DE VALEUR MONTANT") //
                                                        .match("^.* [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestStatementTransaction()
    {
        final DocumentType type = new DocumentType("(ABRECHNUNG ZINSEN" //
                        + "|RESOCONTO INTERESSI MATURATI" //
                        + "|INTEREST INVOICE"
                        + "|RAPPORT D.INT.R.TS)", //
                        documentContext -> documentContext //

                                        .oneOf(
                                                        // @formatter:off
                                                        // IBAN BUCHUNGSDATUM GUTSCHRIFT NACH STEUERN
                                                        // DE10123456789123456789 01.02.2023 0,88 EUR
                                                        //
                                                        // IBAN BUCHUNGSDATUM GESAMT
                                                        // DE12321546856552266333 01.07.2024 74,08 EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("date") //
                                                                        .find("IBAN (BUCHUNGSDATUM|DATA EMISSIONE|BOOKING DATE|DATE) (GUTSCHRIFT NACH STEUERN|GESAMT|TOTALE|TOTAL|D.EFFET TOTAL)") //
                                                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\w]{3}$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("date", v.get("date"));
                                                                            ctx.putBoolean("multipleTransactionInterests", false);
                                                                        }),
                                                        // @formatter:off
                                                        // IBAN DATE D'EFFET TOTAL
                                                        // DE21111111111111111111 01/02/2024 0,09 EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("date") //
                                                                        .find("IBAN DATE D.EFFET TOTAL") //
                                                                        .match("^.* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\.,\\d]+ [\\w]{3}$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("date", v.get("date"));
                                                                            ctx.putBoolean("multipleTransactionInterests", false);
                                                                        }))

                                        // @formatter:off
                                        // Cash Zinsen 4,00% 01.06.2024 - 11.06.2024 61,11 EUR
                                        // Cash Zinsen 3,75% 12.06.2024 - 30.06.2024 99,39 EUR
                                        // @formatter:on
                                        .section("multipleTransactionInterests").optional() //
                                        .match("^(?<multipleTransactionInterests>(Cash|Liquidit.)) (Zinsen|Interessi|Interest) [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .match("^(Cash|Liquidit.) (Zinsen|Interessi|Interest) [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .assign((ctx, v) -> ctx.putBoolean("multipleTransactionInterests", true))

                                        // @formatter:off
                                        // Geldmarkt Dividende 3,75% 01.09.2024 - 17.09.2024 8,05 EUR
                                        // Geldmarkt Dividende 3,50% 18.09.2024 - 30.09.2024 8,03 EUR
                                        // @formatter:on
                                        .section("multipleTransactionDividende").optional() //
                                        .match("^(?<multipleTransactionDividende>Geldmarkt) Dividende [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .match("^Geldmarkt Dividende [\\.,\\d]+% .*[\\d]{4} [\\.,\\d]+ [\\w]{3}$") //
                                        .assign((ctx, v) -> ctx.putBoolean("multipleTransactionDividende", true))

                                        // @formatter:off
                                        // ABRECHNUNG - ZINSEN
                                        // POSITION BETRAG
                                        // Besteuerungsgrundlage 160,50 EUR
                                        // Kapitalertragssteuer -40,13 EUR
                                        // Solidaritätszuschlag -2,20 EUR
                                        // Gesamt 118,17 EUR
                                        // ABRECHNUNG - DIVIDENDE
                                        // @formatter:on
                                        .section("amountInterest", "currencyInterest").optional() //
                                        .find("ABRECHNUNG( \\- ZINSEN)?")
                                        .match("^Gesamt (?<amountInterest>[\\.,\\d]+) (?<currencyInterest>[\\w]{3})$") //
                                        .match("^(ABRECHNUNG \\- DIVIDENDE|BUCHUNG)$")
                                        .assign((ctx, v) -> {
                                            ctx.put("amountInterest", v.get("amountInterest"));
                                            ctx.put("currencyInterest", asCurrencyCode(v.get("currencyInterest")));
                                        })

                                        // @formatter:off
                                        // ABRECHNUNG - DIVIDENDE
                                        // POSITION BETRAG
                                        // Besteuerungsgrundlage 3,76 EUR
                                        // Kapitalertragssteuer -0,94 EUR
                                        // Solidaritätszuschlag -0,05 EUR
                                        // Gesamt 2,77 EUR
                                        // BUCHUNG
                                        // @formatter:on
                                        .section("amountDividende", "currencyDividende").optional() //
                                        .find("ABRECHNUNG \\- DIVIDENDE")
                                        .match("^Gesamt (?<amountDividende>[\\.,\\d]+) (?<currencyDividende>[\\w]{3})$") //
                                        .match("^BUCHUNG$")
                                        .assign((ctx, v) -> {
                                            ctx.put("amountDividende", v.get("amountDividende"));
                                            ctx.put("currencyDividende", asCurrencyCode(v.get("currencyDividende")));
                                        }));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Geldmarkt|Cash|Liquidit.|Esp.ces) (Zinsen|Interessi|Interest|Dividende|Int.r.ts) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Geldmarkt Dividende 3,75% 01.09.2024 - 17.09.2024 8,05 EUR
                                        // Geldmarkt Dividende 3,50% 18.09.2024 - 30.09.2024 8,03 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3", "gross", "currency") //
                                                        .documentContext("date", "amountDividende", "currencyDividende", "multipleTransactionDividende") //
                                                        .match("^Geldmarkt (?<note1>Dividende) (?<note2>[\\.,\\d]+%) (?<note3>.*[\\d]{4}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                                                            t.setDateTime(asDate(v.get("date")));

                                                            if (type.getCurrentContext().getBoolean("multipleTransactionDividende"))
                                                            {
                                                                t.setMonetaryAmount(gross);
                                                            }
                                                            else
                                                            {
                                                                Money amount = Money.of(asCurrencyCode(v.get("currencyDividende")), asAmount(v.get("amountDividende")));
                                                                t.setMonetaryAmount(amount);
                                                                t.addUnit(new Unit(Unit.Type.TAX, gross.subtract(amount)));
                                                            }

                                                            t.setNote(trim(v.get("note1")) + " " + trim(v.get("note3")) + " (" + v.get("note2") + ")");
                                                        }),
                                        // @formatter:off
                                        // Geldmarkt Dividende 3,75% 26.06.2024 - 30.06.2024 3,76 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3", "gross", "currency") //
                                                        .documentContext("date", "amountDividende", "currencyDividende") //
                                                        .match("^Geldmarkt (?<note1>Dividende) (?<note2>[\\.,\\d]+%) (?<note3>.*[\\d]{4}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currencyDividende")), asAmount(v.get("amountDividende")));
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setMonetaryAmount(amount);
                                                            t.setNote(trim(v.get("note1")) + " " + trim(v.get("note3")) + " (" + v.get("note2") + ")");
                                                            t.addUnit(new Unit(Unit.Type.TAX, gross.subtract(amount)));
                                                        }),
                                        // @formatter:off
                                        // Cash Zinsen 4,00% 01.06.2024 - 11.06.2024 61,11 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3", "gross", "currency") //
                                                        .documentContext("date", "amountInterest", "currencyInterest", "multipleTransactionInterests") //
                                                        .match("^(Cash|Liquidit.|Esp.ces) (?<note1>(Zinsen|Interessi|Interest|Int.r.ts)) (?<note2>[\\.,\\d]+%) (?<note3>.*[\\d]{4}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                                                            t.setDateTime(asDate(v.get("date")));

                                                            if (type.getCurrentContext().getBoolean("multipleTransactionInterests"))
                                                            {
                                                                t.setMonetaryAmount(gross);
                                                            }
                                                            else
                                                            {
                                                                Money amount = Money.of(asCurrencyCode(v.get("currencyInterest")), asAmount(v.get("amountInterest")));
                                                                t.setMonetaryAmount(amount);
                                                                t.addUnit(new Unit(Unit.Type.TAX, gross.subtract(amount)));
                                                            }

                                                            t.setNote(trim(v.get("note1")) + " " + trim(v.get("note3")) + " (" + v.get("note2") + ")");
                                                        }),
                                        // @formatter:off
                                        // Cash Zinsen 2,00% 2,58 EUR
                                        // Liquidità Interessi 2,00% 0,12 EUR
                                        // Cash Interest 2,00% 1,47 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "gross", "currency") //
                                                        .documentContext("date", "amountInterest", "currencyInterest", "multipleTransactionInterests") //
                                                        .match("^(Cash|Liquidit.|Esp.ces) (?<note1>(Zinsen|Interessi|Interest|Int.r.ts)) (?<note2>[\\.,\\d]+%) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));

                                                            t.setDateTime(asDate(v.get("date")));

                                                            if (type.getCurrentContext().getBoolean("multipleTransactionInterests"))
                                                            {
                                                                t.setMonetaryAmount(gross);
                                                            }
                                                            else
                                                            {
                                                                Money amount = Money.of(asCurrencyCode(v.get("currencyInterest")), asAmount(v.get("amountInterest")));
                                                                t.setMonetaryAmount(amount);
                                                                t.addUnit(new Unit(Unit.Type.TAX, gross.subtract(amount)));
                                                            }

                                                            t.setNote(v.get("note1") + " (" + v.get("note2") + ")");
                                                        }),
                                        // @formatter:off
                                        // Cash Interest 4,00% 01.06.2024 - 11.06.2024 7,10 EUR
                                        // Cash Interest 3,75% 12.06.2024 - 30.06.2024 11,56 EUR
                                        // Espèces Intérêts 4,00% 23/01/2024 - 31/01/2024 0,09 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3", "amount", "currency") //
                                                        .documentContext("date") //
                                                        .match("^(Cash|Liquidit.|Esp.ces) (?<note1>(Zinsen|Interessi|Interest|Int.r.ts)) (?<note2>[\\.,\\d]+%) (?<note3>.*[\\d]{4}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(trim(v.get("note1")) + " " + trim(v.get("note3")) + " (" + v.get("note2") + ")");
                                                        }),
                                        // @formatter:off
                                        // Cash Zinsen 2,00% 2,58 EUR
                                        // Liquidità Interessi 2,00% 0,12 EUR
                                        // Cash Interest 2,00% 1,47 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "amount", "currency") //
                                                        .documentContext("date") //
                                                        .match("^(Cash|Liquidit.|Esp.ces) (?<note1>(Zinsen|Interessi|Interest|Int.r.ts)) (?<note2>[\\.,\\d]+%) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note1") + " (" + v.get("note2") + ")");
                                                        }))

                        .wrap(TransactionItem::new);
    }

    private void addFeeStatementTransaction()
    {
        DocumentType type = new DocumentType("PAIEMENTS PAR CARTE TRADE REPUBLIC");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // POSITION COMMANDÉ LE QUANTITÉ
                        // Carte Trade Republic 19.04.2024 1
                        // @formatter:on
                        .section("date") //
                        .find("POSITION COMMAND. LE QUANTIT.") //
                        .match("^Carte Trade Republic (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Total -50,00 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .find("POSITION MONTANT") //
                        .match("^Total \\-(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(SPLIT" //
                        + "|FUSION" //
                        + "|DEPOT.BERTRAG EINGEHEND" //
                        + "|TITELUMTAUSCH" //
                        + "|VERGLEICHSVERFAHREN" //
                        + "|KAPITALERH.HUNG GEGEN BAR" //
                        + "|SPIN\\-OFF" //
                        + "|UMTAUSCH\\/BEZUG" //
                        + "|STEUERLICHER UMTAUSCH)", //
                        documentContext -> documentContext //
                                        .section("transaction") //
                                        .match("^(?<transaction>(SPLIT" //
                                                        + "|FUSION" //
                                                        + "|DEPOT.BERTRAG EINGEHEND" //
                                                        + "|TITELUMTAUSCH" //
                                                        + "|VERGLEICHSVERFAHREN" //
                                                        + "|KAPITALERH.HUNG GEGEN BAR" //
                                                        + "|SPIN\\-OFF" //
                                                        + "|UMTAUSCH\\/BEZUG" //
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

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
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
                                        // 1 Ausbuchung Long @6.07 € TUI AG Best TurboDE000SU34220 2053 Stücke
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin") //
                                                        .match("^[\\d] Ausbuchung .*(?<currency>\\p{Sc}) (?<name>.*)(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 1 Repayment Société Générale Effekten GmbH 500 Pcs.
                                        // MiniL O.End CBOE VIX 14,94
                                        // DE000SQ728J8
                                        // 1 Market value 35.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung|Repayment) (?<name>.*) [\\.,\\d]+ (Stk|Pcs)\\.$")
                                                        .match("^(?<nameContinued>.*) [\\w]{4} [\\w]{3,4} [\\.,\\d]+$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert|Market value) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
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
                                        section -> section //
                                                        .attributes("name", "nameContinued", "isin", "currency") //
                                                        .match("^[\\d] (Ausbuchung|Tilgung|Repayment) (?<name>.*) [\\.,\\d]+ (Stk|Pcs)\\.$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^[\\d] (Barausgleich|Kurswert|Market value) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // Bundesobl.Ser.180 v.2019(24)
                                        // ISIN: DE0001141802
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "nameContinued", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .match("^(?<nameContinued>.*)$")
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
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
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Société Générale Effekten GmbH 490 Pcs. 0.886 EUR 434.14 EUR
                                        // MiniL O.End CBOE VIX 11,49
                                        // ISIN: DE000SQ6QKU9
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*) [\\w]{4} [\\w]{3,4} [\\.,\\d]+$")
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 3M Co. 7 Stk. 139,40 EUR 975,80 EUR
                                        // Registered Shares DL -,01
                                        // ISIN: US88579Y1010
                                        //
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Registered Shares o.N.
                                        // AU000000CUV3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kontron 23 Stk. 20,90 EUR 480,70 EUR
                                        // ISIN: AT0000A0E9W5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pcs\\.|Pz\\.|t.t\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 1 Ausbuchung Long @6.07 € TUI AG Best TurboDE000SU34220 2053 Stücke
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Ausbuchung .*\\p{Sc} .*[A-Z]{2}[A-Z0-9]{9}[0-9] (?<shares>[\\.,\\d]+) St.cke$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Clinuvel Pharmaceuticals Ltd. 80 Stk. 22,82 EUR 1.825,60 EUR
                                        // Tencent Holdings Ltd. 0,3773 titre(s) 53,00 EUR 20,00 EUR
                                        // zjBAM Corp. 125 Pz. 29,75 EUR 3.718,75 EUR
                                        // Vonovia SE 0,781379 tít. 24,06 EUR 18,80 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.|t.t\\.) .*$") //
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
                                        // 1 Repayment Société Générale Effekten GmbH 500 Pcs.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] Repayment .* (?<shares>[\\.,\\d]+) Pcs\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Bundesrep.Deutschland 1.019 EUR 98,05 % 999,13 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+ % [\\.,\\d]+ [\\w]{3}$")
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
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
                                        // Market-Order Achat le 10/04/2024 à 17:33 (Europe/Berlin).
                                        // Market-OrderCompra el día 01.12.2022 a las 11:56 (Europe/Berlin).
                                        // Market-OrderVenta a día 15.02.2024, a las 16:15 (Europe/Berlin) en Lang & Schwarz Exchange.
                                        // Market-Order Vente avec le numéro d'ordre 185e-c098 a été exécuté le 16/11/2023 à 16:41 (Europe/Berlin) sur la place de négociation
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(?i)((Limit|Stop\\-Market|Market)\\-Order(\\s)?)?(Buy|Achat|Acquisto|Kauf|Verkauf|Sell|Compra|Venta|Vente) .* " //
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}" //
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}" //
                                                                        + "|[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}))" //
                                                                        + "(,)? (um|at|alle|.|a las) (?<time>[\\d]{2}:[\\d]{2}).*$") //
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
                                        // Saveback execution on 02.05.2024 on the Lang & Schwarz Exchange.
                                        // Ejecución del plan de inversión el día 02.05.2024 en Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Sparplanausf.hrung"
                                                                        + "|Ejecuci.n del plan de inversi.n"
                                                                        + "|(Savings plan|Saveback) execution) .* "
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Ausführung von Round up am 09.02.2024 an der Lang & Schwarz Exchange.
                                        // Ausführung von Saveback am 04.03.2024 an der Lang & Schwarz Exchange.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ausf.hrung von (Round up|Saveback) .* "
                                                                        + "(?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"
                                                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // DE40110101001234567890 06.08.2021 0,44 GBP
                                        // DE71100123450999999601 30.07.2024 2.05 EUR
                                        // @formatter:on
                                        section -> section.attributes("date") //
                                                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // GESAMT 3.615,63 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
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

        Block firstRelevantLine = new Block("^TRADE REPUBLIC BANK GMBH.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .optionalOneOf(
                                        // @formatter:off
                                        // Storno der Dividende mit dem Ex-Tag 29.11.2019.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Storno) der Dividende .*$") //
                                                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported)),
                                        // @formatter:off
                                        // STORNIERUNG DER DIVIDENDE
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>STORNIERUNG) DER DIVIDENDE$") //
                                                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported)))

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
                                        //
                                        // Apple Inc. 1,92086 titre(s) 0,25 USD 0,48 USD
                                        // Registered Shares o.N.
                                        // ISIN : US037833100
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "nameContinued") //
                                                        .match("^(?<name>.*) [\\.,\\d]+ (Stk\\.|titre\\(s\\)|Pz\\.|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(ISIN([\\s])?:([\\s])?)?(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
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
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Euro iSTOXX ex FIN Dividend+ EUR (Dist) 206.651869 Stücke 0.48 EUR 99.19 EURDE000ETFL482
                                        //
                                        // POSITION QUANTITY YIELD AMOUNT
                                        // Vici Properties 30.000000 Pcs. 0.415 USD 12.45 USDUS9256521090
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .find("POSITION (ANZAHL|QUANTITY) (Ertrag|ERTRAG|ERTR.GNIS|YIELD) (BETRAG|AMOUNT)")
                                                        .match("^(?<name>.*) [\\.,\\d]+ (St.cke|Pcs\\.) [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // MSCI World USD (Dist) 123.45 USD
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        //
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Euro iSTOXX ex FIN 0.33 EUR
                                        // DE000ETFL482 1.525759 Stücke -0.215 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*) [\\.,\\d]+ [\\w]{3}")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // Developed Markets Dividend Leaders EUR (Dist)
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*)")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ St.cke [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION QUANTITY YIELD AMOUNT
                                        // Mondelez
                                        // US6092071058 2.000000 Pcs. 0.425 USD
                                        // @formatter:on
                                        //
                                        // POSITION QUANTITÉ TAUX MONTANT
                                        // Microsoft
                                        // US5949181045 0.561914 unit. 0.75 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION (QUANTITY|QUANTIT.) (YIELD|TAUX) (AMOUNT|MONTANT)")
                                                        .match("^(?<name>.*)")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ (Pcs|unit)\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // POSITION ANZAHL ERTRAG BETRAG
                                        // NVIDIA 0.32 USD
                                        // 32.000000 Stücke 0.01 USD
                                        // US67066G1040
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("POSITION ANZAHL (Ertrag|ERTRAG|ERTR.GNIS) BETRAG")
                                                        .match("^(?<name>.*) [\\.,\\d]+ [\\w]{3}")
                                                        .match("[\\.,\\d]+ St.cke [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
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
                                        // Apple Inc. 1,92086 titre(s) 0,25 USD 0,48 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Stk\\.|titre\\(s\\)|Pz\\.) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // IE00BK1PV551 123 Stücke 0.36 USD
                                        // NL0011683594 90.537929 Stücke 0.87 EUR
                                        // Euro iSTOXX ex FIN Dividend+ EUR (Dist) 206.651869 Stücke 0.48 EUR 99.19 EURDE000ETFL482
                                        // Vici Properties 30.000000 Pcs. 0.415 USD 12.45 USDUS9256521090
                                        // DE000ETFL482 1.525759 Stücke -0.215 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (St.cke|Pcs\\.) (\\-)?[\\.,\\d]+ [\\w]{3}.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // Apple Inc. 0.0929 Pcs. 0.24 USD 0.02 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) Pcs\\. [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // US6092071058 2.000000 Pcs. 0.425 USD
                                        // US5949181045 0.561914 unit. 0.75 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* (?<shares>[\\.,\\d]+) (Pcs|unit)\\. [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 32.000000 Stücke 0.01 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) St.cke [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))),
                                        // @formatter:off
                                        // 1 Reinvestierung Vodafone Group PLC 699 Stk.
                                        // 1 Kapitalmaßnahme Barrick Gold Corp. 8,4226 Stk.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\d] (Reinvestierung|Kapitalmaßnahme) .* (?<shares>[\\.,\\d]+) Stk\\.$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // DExxxxxx 25.09.2019 4,18 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        //
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // DE98502109007017811111 16/05/2024 0,38 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]+ (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (\\-)?[\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // DE99012345670123456789 10.01.2024 68,74 EUR
                        // DE98502109007017811111 16/05/2024 0,38 EUR
                        // @formatter:on
                        .section("currency") //
                        .match("^[\\w]+ (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"
                                        + "|[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}"
                                        + "|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(0L);
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Kapitalertragsteuer Optimierung 5,15 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^([\\d] )?Kapitalertrags(s)?teuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }),
                                        // @formatter:off
                                        // Kapitalertragssteuer -13.33 EUR
                                        // Kapitalertragssteuer 13.33 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Kapitalertrags(s)?teuer \\-[\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^Kapitalertrags(s)?teuer (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }))
                        .optionalOneOf( //
                                        // @formatter:off
                                        // Solidaritätszuschlag Optimierung 0,28 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^([\\d] )?Solidarit.tszuschlag Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }),
                                        // @formatter:off
                                        // Solidaritätszuschlag -0.73 EUR
                                        // Solidaritätszuschlag 0.73 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Solidarit.tszuschlag \\-[\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^Solidarit.tszuschlag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Kirchensteuer Optimierung 9,84 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^([\\d] )?Kirchensteuer Optimierung (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }),
                                        // @formatter:off
                                        //
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Kirchensteuer \\-[\\.,\\d]+ [\\w]{3}$") //
                                                        .match("^Kirchensteuer (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(amount));
                                                        }))

                        // @formatter:off
                        // Zwischensumme 1,095514 EUR/USD 63,31 EUR
                        // Sous-total 1,0802 EUR/USD 0,38 EUR
                        // Sub Total 1.0855 USD/EUR 0.66 EUR
                        // @formatter:on
                        .section("exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^(Zwischensumme|Subtotale|Subtotal|Sous\\-total|Sub Total) (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (\\-)?[\\.,\\d]+ [\\w]{3}$")
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
                        // Withholding Tax for US issuer -0.13 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^([\\d] )?(Quellensteuer|Withholding Tax) .* (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Impôt à la source pour les émetteurs américains -0,07 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Imp.t . la source pour les .metteurs am.ricains (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
                        // Kapitalertragssteuer 1.0894 EUR/USD -11.11 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+ [\\w]{3}\\/[\\w]{3} \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag -1,68 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^([\\d] )?Solidarit.tszuschlag (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 1.0894 EUR/USD -1.11 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+ [\\w]{3}\\/[\\w]{3} \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer -1,68 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 1.0894 EUR/USD -1.11 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+ [\\w]{3}\\/[\\w]{3} \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Frz. Finanztransaktionssteuer -3,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.* Finanztransaktionssteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Finanztransaktionssteuer -0,08 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Finanztransaktionssteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
                        // Frais externes -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Frais externes \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
                        // Tarifa plana por costes del servicio de ejecución de terceros -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Tarifa plana por costes del servicio de ejecuci.n de terceros \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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

    private static class AccountAmountTransactionItem
    {
        int line;

        String currency;
        long amount;

        @Override
        public String toString()
        {
            return "AccountTransactionItem [line=" + line + ", currency=" + currency + ", amount=" + amount + "]";
        }
    }

    private static class AccountAmountTransactionHelper
    {
        private List<AccountAmountTransactionItem> items = new ArrayList<>();

        // Finds an AccountAmountTransactionItem in the list that has a line
        // number less than or equal to the specified line
        public Optional<AccountAmountTransactionItem> findItem(Integer line, Money money)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (int i = items.size() - 1; i >= 0; i--) // NOSONAR
            {
                AccountAmountTransactionItem item = items.get(i);

                if (item.line > line)
                    continue;

                if (!item.currency.equals(money.getCurrencyCode()))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
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
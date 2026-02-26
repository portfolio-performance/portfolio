package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.replaceSingleBlank;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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

/**
 * @formatter:off
 * @implNote Liberty Vorsorge AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class LibertyVorsorgeAGPDFExtractor extends AbstractPDFExtractor
{
    public LibertyVorsorgeAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Liberty Vorsorge AG");
        addBankIdentifier("Liberty");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Liberty Vorsorge AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("B[\\s]*.[\\s]*r[\\s]*s[\\s]*e[\\s]*n[\\s]*a[\\s]*b[\\s]*r[\\s]*e[\\s]*c[\\s]*h[\\s]*n[\\s]*u[\\s]*n[\\s]*g[\\s]*\\-[\\s]*([\\s]*Z[\\s]*e[\\s]*i[\\s]*c[\\s]*h[\\s]*n[\\s]*u[\\s]*n[\\s]*g[\\s]*)?([\\s]*K[\\s]*a[\\s]*u[\\s]*f[\\s]*|[\\s]*V[\\s]*e[\\s]*r[\\s]*k[\\s]*a[\\s]*u[\\s]*f[\\s]*)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^A[\\s]*u[\\s]*f[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*s[\\s]*n[\\s]*u[\\s]*m[\\s]*m[\\s]*e[\\s]*r.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "verkauft" change from BUY to SELL
                        .section("type").optional() //
                        .match("^W[\\s]*i[\\s]*r[\\s]*h[\\s]*a[\\s]*b[\\s]*e[\\s]*n[\\s]*f.[\\s]*r[\\s]*S[\\s]*i[\\s]*e[\\s]*a[\\s]*m.*" //
                                        + "(?<type>v[\\s]*e[\\s]*r[\\s]*k[\\s]*a[\\s]*u[\\s]*f[\\s]*t).*") //
                        .assign((t, v) -> {
                            if ("verkauft".equals(stripBlanks(v.get("type"))))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben für Sie am 05.06.2023 gezeichnet
                                        // 0.201 Anteile -QBH CHF-
                                        // Credit Suisse Index Fund (CH) II Umbrella
                                        // - CSIF (CH) II Gold Blue
                                        // Valor: 35276539
                                        // ISIN: CH0352765397
                                        // Menge/Nominal Börsenplatz Preis
                                        // 0.201 UBS Funds 1'423.47
                                        // Total Kurswert CHF -286.12
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "wkn", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am.*") //
                                                        .match("^[\\.'\\d]+ Anteile .*$") //
                                                        .match("^(?<name>.*)$")
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .find("Menge\\/Nominal B.rsenplatz Preis") //
                                                        .match("^Total Kurswert (?<currency>[A-Z]{3}) .*$") //)
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // W ir ha ben für Sie a m  06.03.2025 v erk a u ft
                                        // 8 Anla gefonds U SD
                                        // Fra nk lin Tem pleton ICAV  - Fra nk lin FTSE India  U CITS
                                        // V a lor:46325074
                                        // ISIN:IE00BH Z RQ Z 17
                                        // M enge/Nom ina l Börsenpla tz Preis
                                        // 8 SIX  Sw iss Ex cha nge U SD 40.1483
                                        // Tota lK u rsw ert U SD 321.19
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "currency") //
                                                        .find("W[\\s]*i[\\s]*r[\\s]*h[\\s]*a[\\s]*b[\\s]*e[\\s]*n[\\s]*f.[\\s]*r[\\s]*S[\\s]*i[\\s]*e[\\s]*a[\\s]*m.*") //
                                                        .find("[\\.'\\d]+ A[\\s]*n[\\s]*l[\\s]*a[\\s]*g[\\s]*e[\\s]*f[\\s]*o[\\s]*n[\\s]*d[\\s]*s.*") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^V[\\s]*a[\\s]*l[\\s]*o[\\s]*r[\\s]*:[\\s]*(?<wkn>.*)$") //
                                                        .match("^I[\\s]*S[\\s]*I[\\s]*N[\\s]*:[\\s]*(?<isin>.*)$") //
                                                        .match("^T[\\s]*o[\\s]*t[\\s]*a[\\s]*l[\\s]*K[\\s]*u[\\s]*r[\\s]*s[\\s]*w[\\s]*e[\\s]*r[\\s]*t[\\s]*(?<currency>[A-Z\\s]{3,}) .*$") //
                                                        .assign((t, v) ->
                                                        {
                                                            v.put("name", trim(replaceMultipleBlanks(replaceSingleBlank(v.get("name")))));
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("currency", stripBlanks(v.get("currency")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben für Sie am 05.06.2023 gezeichnet
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Wir haben für Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // W ir ha ben für Sie a m  06.03.2025 v erk a u ft
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("W[\\s]*i[\\s]*r[\\s]*h[\\s]*a[\\s]*b[\\s]*e[\\s]*n[\\s]*f.[\\s]*r[\\s]*S[\\s]*i[\\s]*e[\\s]*a[\\s]*m[\\s]*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Menge/Nominal Börsenplatz Preis
                                        // 0.201 UBS Funds 1'423.47
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Menge\\/Nominal B.rsenplatz Preis") //
                                                        .match("^(?<shares>[\\.'\\d]+) .*$") //"
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // M enge/Nom ina l Börsenpla tz Preis
                                        // 8 SIX  Sw iss Ex cha nge U SD 40.1483
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("M[\s]*e[\\s]*n[\\s]*g[\\s]*e[\\s]*\\/[\\s]*N[\\s]*o[\\s]*m[\\s]*i[\\s]*n[\\s]*a[\\s]*l[\\s]*B[\\s]*.[\\s]*r[\\s]*s[\\s]*e[\\s]*n[\\s]*p[\\s]*l[\\s]*a[\\s]*t[\\s]*z[\\s]*P[\\s]*r[\\s]*e[\\s]*i[\\s]*s[\\s]*") //
                                                        .match("^(?<shares>[\\.'\\d]+) .*$") //"
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Netto CHF -10'142.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Netto (?<currency>[A-Z]{3}) (\\-)?(?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Netto U SD 320.66
                                        // Cha nge U SD /CH F 0.885050 CH F 283.80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^C[\\s]*h[\\s]*a[\s]*n[\\s]*g[\\s]*e[\\s]*[A-Z\\s]{3,}[\\s]*\\/[\\s]*[A-Z\\s]{3,} [\\.,\\d]+[\\s]*(?<currency>[A-Z\\s]{3,})[\\s]*(\\-)?(?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Tota lK u rsw ert U SD 321.19
                                        // Cha nge U SD /CH F 0.885050 CH F 283.80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "baseCurrency", "exchangeRate", "fxGross") //
                                                        .match("^T[\\s]*o[\\s]*t[\\s]*a[\\s]*l[\\s]*K[\\s]*u[\\s]*r[\\s]*s[\\s]*w[\\s]*e[\\s]*r[\\s]*t[\\s]*(?<currency>[A-Z\\s]{3,}) (?<fxGross>[\\.,\\d]+)$") //
                                                        .match("^C[\\s]*h[\\s]*a[\s]*n[\\s]*g[\\s]*e[\\s]*(?<baseCurrency>[A-Z\\s]{3,})[\\s]*\\/[\\s]*(?<termCurrency>[A-Z\\s]{3,}) (?<exchangeRate>[\\.,\\d]+) [A-Z\\s]{3,} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            v.put("termCurrency", stripBlanks(v.get("termCurrency")));
                                                            v.put("baseCurrency", stripBlanks(v.get("baseCurrency")));

                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getTermCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Auftragsnummer AUF1191526
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Auftragsnummer .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Au ftra gsnu m m er AU F250312-8240903
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^(?<note1>A[\\s]*u[\\s]*f[\\s]*[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*s[\\s]*n[\\s]*u[\\s]*m[\\s]*m[\\s]*e[\\s]*r) (?<note2>.*)$") //
                                                        .assign((t, v) -> t.setNote(stripBlanks(v.get("note1")) + " " + stripBlanks(v.get("note2")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("A[\\s]*u[\\s]*s[\\s]*s[\\s]*c[\\s]*h[\\s]*.[\\s]*t[\\s]*t[\\s]*u[\\s]*[\\s]*n[\\s]*g");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Referenz .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // W ir bez iehen u ns a u f die in Ihrem  D epot liegenden W erte u nd rechnen die Au sschüttu ng w ie folgt a b:
                                        // Anteile -FA CH F- Ex  D a tu m : 17.06.2025
                                        // Sw issca nto (CH ) Index  Fu nd V Z a hlba r D a tu m : 20.06.2025
                                        // - Index  Bond Fu nd Corp.CH F Responsible
                                        // V a lor:111719600
                                        // ISIN:CH 1117196001
                                        // Besta nd: 1.739 z u  CH F 1.15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "wkn", "isin", "currency") //
                                                        .find("W[\\s]*i[\\s]*r[\\s]*b[\\s]*e[\\s]*z[\\s]*i[\\s]*e[\\s]*h[\\s]*e[\\s]*n[\\s]*u[\\s]*n[\\s]*s.*") //
                                                        .match("^(?<name>.*) Z[\\s]*a[\\s]*h[\\s]*l[\\s]*b[\\s]*a[\\s]*r.*$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^V[\\s]*a[\\s]*l[\\s]*o[\\s]*r[\\s]*:[\\s]*(?<wkn>.*)$") //
                                                        .match("^I[\\s]*S[\\s]*I[\\s]*N[\\s]*:[\\s]*(?<isin>.*)$") //
                                                        .match("^B[\\s]*e[\\s]*s[\\s]*t[\\s]*a[\\s]*n[\\s]*d[\\s]*:[\\s]*[\\.'\\d]+ z[\\s]*u[\\s]*(?<currency>[A-Z\\s]{3,}).*$") //
                                                        .assign((t, v) ->
                                                        {
                                                            v.put("name", trim(replaceMultipleBlanks(replaceSingleBlank(v.get("name")))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("currency", stripBlanks(v.get("currency")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Besta nd: 1.739 z u  CH F 1.15
                        // @formatter:on
                        .section("shares") //
                        .match("^B[\\s]*e[\\s]*s[\\s]*t[\\s]*a[\\s]*n[\\s]*d[\\s]*:[\\s]*(?<shares>[\\.'\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // U nsere G u tschrift erfolgt a u f K onto 1.2886.4114-199 per 20.06.2025
                        // @formatter:on
                        .section("date") //
                        .match("^U[\\s]*n[\\s]*s[\\s]*e[\\s]*r[\\s]*e[\\s]*G[\\s]*u[\\s]*t[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*i[\\s]*f[\\s]*t.*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //"
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto CH F 1.30
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^N[\\s]*e[\\s]*t[\\s]*t[\\s]*o[\\s]*(?<currency>[A-Z\\s]{3,})[\\s]*(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Schw y z ,25.06.2025 U nsere Referenz CA20250624/95977
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Referenz (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note")))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // 35%  V errechnu ngssteu er CH F -0.70
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^[\\d]+%[\\s]*V[\\s]*e[\\s]*r[\\s]*r[\\s]*e[\\s]*c[\\s]*h[\\s]*n[\\s]*u[\\s]*n[\\s]*g[\\s]*s[\\s]*s[\\s]*t[\\s]*e[\\s]*u[\\s]*e[\\s]*r[\\s]*(?<currency>[A-Z\\s]{3,}) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", stripBlanks(v.get("currency")));

                            processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Stem pel U SD -0.48
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^S[\\s]*t[\\s]*e[\\s]*m[\\s]*p[\\s]*e[\\s]*l[\\s]*(?<currency>[A-Z\\s]{3,}) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", stripBlanks(v.get("currency")));

                            processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Andere Spesen U SD -0.05
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^A[\\s]*n[\\s]*d[\\s]*e[\\s]*r[\\s]*e[\\s]*S[\\s]*p[\\s]*e[\\s]*s[\\s]*e[\\s]*n[\\s]*(?<currency>[A-Z\\s]{3,}) \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", stripBlanks(v.get("currency")));
                            v.put("fee", stripBlanks(v.get("fee")));

                            processFeeEntries(t, v, type);
                        });
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

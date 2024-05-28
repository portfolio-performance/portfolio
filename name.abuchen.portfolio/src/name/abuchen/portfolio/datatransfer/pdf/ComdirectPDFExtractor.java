package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanksAndUnderscores;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote ComDirect provides two documents for the transaction.
 *           The security transaction and the taxes treatment.
 *           Both documents are provided as one PDF or as two PDFs.
 *
 *           The security transaction includes the fees, but not the correct taxes
 *           and the taxes treatment includes all taxes (including withholding tax),
 *           but not all fees.
 *
 *           Therefore, we use the documents based on their function and merge both documents, if possible, as one transaction.
 *           {@code
 *              matchTransactionPair(List<Item> transactionList,List<Item> taxesTreatmentList)
 *           }
 *
 *           The separate taxes treatment does only contain taxes in the account currency.
 *           However, if the security currency differs, we need to provide the currency conversion.
 *           {@code
 *              fixMissingCurrencyConversionForSaleTaxesTransactions(Collection<TransactionTaxesPair>)
 *           }
 *
 *           Always import the securities transaction and the taxes treatment for a correct transaction.
 *           Due to rounding differences, the correct gross amount is not always shown in the securities transaction.
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    private static final String ATTRIBUTE_GROSS_TAXES_TREATMENT = "gross_taxes_treatment";

    public ComdirectPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("comdirect");

        addBuySellTransaction();
        addSellWithNegativeAmountTransaction();
        addDividendeTransaction();
        addDepositoryFeeTransaction();
        addTaxesTreatmentTransaction();
        addFinancialReport();
    }

    @Override
    public String getLabel()
    {
        return "Comdirect Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Wertpapierkauf" //
                        + "|Wertpapierverkauf" //
                        + "|Verkauf .* B.rsenplatz" //
                        + "|Wertpapierumtausch)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(?!Steuerliche Behandlung:).*" //
                        + "(Wertpapierkauf" //
                        + "|Wertpapierverkauf"
                        + "|Wertpapierumtausch" //
                        + "|Sie folgendes Gesch.ft ausgef.hrt).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Wertpapierverkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.*(?<type>(Wertpapierkauf|Wertpapierverkauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Wertpapierverkauf".equals(v.get("type")) || "Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Wertpapier-Bezeichnung                                               WPKNR/ISIN
                                        // ARERO   Der Weltfonds - ESG                                              DWS26Y
                                        // Inhaber-Anteile LC o.N.                                            LU2114851830
                                        //                           Kurswert                    : EUR              999,90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wertpapier\\-Bezeichnung .*") //
                                                        .match("^(?<name>.*)[\\s]{2,}(?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*)[\\s]{2,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Wertpapier-Bezeichnung                                               WPKNR/ISIN
                                        // Medtronic PLC                                                            A14M2J
                                        // Registered Shares DL -,0001                                        IE00BTN1Y115
                                        //  Summe        St.  20                 EUR  71,00        EUR            1.420,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wertpapier\\-Bezeichnung .*") //
                                                        .match("^(?<name>.*)[\\s]{2,}(?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*)[\\s]{2,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?Summe[\\s]{1,}St\\.[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Wertpapier-Bezeichnung                                               WPKNR/ISIN
                                        // LVMH Moët Henn. L. Vuitton SA                                            853292
                                        // Actions Port. (C.R.) EO 0,3                                        FR0000121014
                                        //               St.  100                EUR  86,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wertpapier\\-Bezeichnung .*") //
                                                        .match("^(?<name>.*)[\\s]{2,}(?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*)[\\s]{2,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?St\\.[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // zum Kurs von : EUR  33,47
                                        //                           Wertpapierbezeichnung          WPK-Nr.
                                        // St. 16                    iShares PLC-MSCI Wo.UC.ETF DIS A0HGV0
                                        //                           Registered Shares o.N.         IE00B0M62Q58
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "wkn", "nameContinued", "isin") //
                                                        .match("^zum Kurs von : (?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+ .*$") //
                                                        .find(".* Wertpapierbezeichnung .* WPK\\-Nr\\. .*") //
                                                        .match("^St\\. [\\.,\\d]+ (?<name>.*) (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // St.  8,544                EUR  117,03
                                        //               St.  100                EUR  86,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^([\\s]+)?St\\.[\\s]{1,}(?<shares>[\\.,\\d]+)[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        //  Summe        St.  20                 EUR  71,00        EUR            1.420,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^([\\s]+)?Summe[\\s]{1,}St\\.[\\s]{1,}(?<shares>[\\.,\\d]+)[\\s]{1,}[\\w]{3}.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // St. 16                    iShares PLC-MSCI Wo.UC.ETF DIS A0HGV0
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^St\\. (?<shares>[\\.,\\d]+) .* [A-Z0-9]{6}.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // EUR  1.000                100%
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3}[\\s]{1,}(?<shares>[\\.,\\d]+)[\\s]{1,}[\\.,\\d]+%.*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Handelszeit       : 20:06 Uhr (MEZ/MESZ)
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit[\\s]{1,}: (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Geschäftstag      : 02.01.2023        Ausführung          comdirect
                        // @formatter:on
                        .section("date") //
                        .match("^Gesch.ftstag[\\s]{1,}: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // If the type of transaction is "SELL" and the amount negative, then the gross amount set.
                        // Fees are processed in a separate transaction.
                        //
                        //                           Kurswert                    : EUR                3,54
                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                        // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,86-
                        // @formatter:on
                        .section("negative").optional() //
                        .find(".* Zu Ihren Lasten.*") //
                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}[\\w]{3} [\\s]{2,}[\\.,\\d]+(?<negative>\\-).*$") //
                        .assign((t, v) -> {
                            if (t.getPortfolioTransaction().getType().isLiquidation())
                                type.getCurrentContext().putBoolean("negative", true);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // zum Kurs von : EUR  33,47
                                        // St. 16                    iShares PLC-MSCI Wo.UC.ETF DIS A0HGV0
                                        // wird über IBAN DE36 6026 6370 0968 8056 79  ( EUR ) gebucht.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyPerShare", "amountPerShare", "shares", "currency") //
                                                        .match("^zum Kurs von : (?<currencyPerShare>[\\w]{3})[\\s]{1,}(?<amountPerShare>[\\.,\\d]+) .*$") //
                                                        .match("^St\\. (?<shares>[\\.,\\d]+) .* [A-Z0-9]{6}.*$") //
                                                        .match("^wird .ber .* \\( (?<currency>[\\w]{3}) \\) gebucht\\..*$") //
                                                        .assign((t, v) -> {
                                                            Money amountPerShare = Money.of(asCurrencyCode(v.get("currencyPerShare")), asAmount(v.get("amountPerShare")));
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));

                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), //
                                                                            BigDecimal.valueOf(amountPerShare.getAmount()) //
                                                                                            .multiply(shares, Values.MC) //
                                                                                            .setScale(0, RoundingMode.HALF_UP).longValue());

                                                            t.setMonetaryAmount(amount);
                                                        }),
                                        // @formatter:off
                                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                                        // DE02 0588 9054 7570 3258 87   EUR     04.01.2023        EUR              999,90
                                        //
                                        // IBAN                                  Valuta        Zu Ihren Gunsten vor Steuern
                                        // DE36 4148 7227 3022 3056 95   EUR     10.02.2020        EUR              616,18
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find(".* Zu Ihren (Lasten|Gunsten).*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<amount>[\\.,\\d]+)([\\s]+)?$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechnung zum Devisenkurs (?<exchangeRate>[\\.,\\d]+)[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}[\\w]{3} [\\s]{2,}[\\.,\\d]+\\-.*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            t.setMonetaryAmount(gross);

                                                            checkAndSetGrossUnit(t.getPortfolioTransaction().getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        //                          Kurswert                    : USD                2,90
                                        //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR                5,73-
                                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                                        //                XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,23-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechn\\. zum Dev\\. kurs (?<exchangeRate>[\\.,\\d]+) .*[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}[\\w]{3} [\\s]{2,}[\\.,\\d]+\\-.*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            t.setMonetaryAmount(gross);

                                                            checkAndSetGrossUnit(t.getPortfolioTransaction().getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        //                                      Kurswert                    : EUR                3,54
                                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                                        // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,86-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "gross", "baseCurrency") //
                                                        .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<gross>[\\.,\\d]+).*$")
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+\\-.*$") //
                                                        .assign((t, v) -> {
                                                            String forex = asCurrencyCode(v.get("baseCurrency"));
                                                            if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                                                            {
                                                                t.setAmount(asAmount(v.get("gross")));
                                                                t.setCurrencyCode(asCurrencyCode(v.get("fxCurrency")));
                                                            }
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        //                           Kurswert                    : USD               28,74
                                        //            Umrechnung zum Devisenkurs 1,123400        : EUR               24,97
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechnung zum Devisenkurs (?<exchangeRate>[\\.,\\d]+)[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            if (!type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                ExtrExchangeRate rate = asExchangeRate(v);
                                                                type.getCurrentContext().putType(rate);

                                                                Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                                Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }
                                                        }),
                                        // @formatter:off
                                        //                           Kurswert                    : USD            4.768,00
                                        //        Umrechn. zum Dev. kurs 1,080600 vom 17.04.2020 : EUR            4.425,22
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechn\\. zum Dev\\. kurs (?<exchangeRate>[\\.,\\d]+) .*[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            if (!type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                ExtrExchangeRate rate = asExchangeRate(v);
                                                                type.getCurrentContext().putType(rate);

                                                                Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                                Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }
                                                        }),
                                        // @formatter:off
                                        //  Summe        St.  720                USD  40,098597    USD           28.870,99
                                        //        Umrechn. zum Dev. kurs 1,120800 vom 12.03.2020 : EUR           25.784,17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") // //
                                                        .match("^([\\s]+)?Summe[\\s]{1,}St\\.[\\s]{1,}[\\.,\\d]+[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<termCurrency>[\\w]{3})[\\s]{1,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechn\\. zum Dev\\. kurs (?<exchangeRate>[\\.,\\d]+) .*[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            if (!type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                ExtrExchangeRate rate = asExchangeRate(v);
                                                                type.getCurrentContext().putType(rate);

                                                                Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                                Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Ordernummer       : 000117637940-001  Rechnungsnummer   : 508104401078D295
                                        //                     592581219254      Rechnungsnummer   : 878649826981vsP4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* (?<note>[\\-\\d]+) [\\s]{1,}Rechnungsnummer.*$") //
                                                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + v.get("note"))),
                                        // @formatter:off
                                        // Geschäftstag : 08.06.2015         Order-Nr.   : 71871368321 / 001
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* Order\\-Nr\\.[\\s]{1,}: (?<note>[\\/\\d\\s]+).*$")
                                                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note")))))

                        // @formatter:off
                        //                     592581219254      Rechnungsnummer   : 878649826981vsP4
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Rechnungsnummer[\\s]{1,}: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "R.-Nr.: " + trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(t -> {
                            // If we have multiple entries in the document,
                            // then the "negative" flag must be removed.
                            type.getCurrentContext().remove("negative");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellWithNegativeAmountTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierverkauf");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(?!Steuerliche Behandlung:).*Wertpapierverkauf.*$");
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
                                        // Wertpapier-Bezeichnung                                               WPKNR/ISIN
                                        // ARERO   Der Weltfonds - ESG                                              DWS26Y
                                        // Inhaber-Anteile LC o.N.                                            LU2114851830
                                        //                           Kurswert                    : EUR              999,90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wertpapier\\-Bezeichnung .*") //
                                                        .match("^(?<name>.*)[\\s]{2,}(?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*)[\\s]{2,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Wertpapier-Bezeichnung                                               WPKNR/ISIN
                                        // Medtronic PLC                                                            A14M2J
                                        // Registered Shares DL -,0001                                        IE00BTN1Y115
                                        //  Summe        St.  20                 EUR  71,00        EUR            1.420,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wertpapier\\-Bezeichnung .*") //
                                                        .match("^(?<name>.*)[\\s]{2,}(?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<nameContinued>.*)[\\s]{2,}(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?Summe[\\s]{1,}St\\.[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // St.  8,544                EUR  117,03
                                        //               St.  100                EUR  86,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^([\\s]+)?St\\.[\\s]{1,}(?<shares>[\\.,\\d]+)[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        //  Summe        St.  20                 EUR  71,00        EUR            1.420,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^([\\s]+)?Summe[\\s]{1,}St\\.[\\s]{1,}(?<shares>[\\.,\\d]+)[\\s]{1,}[\\w]{3}.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Handelszeit       : 20:06 Uhr (MEZ/MESZ)
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit[\\s]{1,}: (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Geschäftstag      : 02.01.2023        Ausführung          comdirect
                        // @formatter:on
                        .section("date") //
                        .match("^Gesch.ftstag[\\s]{1,}: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDateTime(asDate(v.get("date")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        //                           Kurswert                    : USD                2,90
                                        //        Umrechn. zum Dev. kurs 1,222500 vom 16.12.2020 : EUR                5,73-
                                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                                        // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,23-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency", "currency", "amount") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechn\\. zum Dev\\. kurs (?<exchangeRate>[\\.,\\d]+) .*[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<amount>[\\.,\\d]+)\\-.*$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            t.setMonetaryAmount(gross.add(amount));

                                                            gross = Money.of(rate.getBaseCurrency(), t.getAmount());
                                                            fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency", "currency", "amount") //
                                                        .match("^.* Kurswert[\\s]{1,}: (?<termCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* Umrechnung zum Devisenkurs (?<exchangeRate>[\\.,\\d]+)[\\s]{1,}: (?<baseCurrency>[\\w]{3}) [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<amount>[\\.,\\d]+)\\-.*$") //
                                                        .assign((t, v) -> {
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            t.setMonetaryAmount(gross.add(amount));

                                                            gross = Money.of(rate.getBaseCurrency(), t.getAmount());
                                                            fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        //                                      Kurswert                    : EUR                3,54
                                        // IBAN                                  Valuta         Zu Ihren Lasten vor Steuern
                                        // XXXX XXXX XXXX XXXX XXXX XX   EUR     27.08.2020        EUR                8,86-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxCurrency", "fxGross", "currency", "amount") //
                                                        .match("^.* Kurswert ([\\s]+)?: ([\\s]+)?(?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                                                        .find(".* Zu Ihren Lasten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<amount>[\\.,\\d]+)\\-.*$") //
                                                        .assign((t, v) -> {
                                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            t.setMonetaryAmount(fxGross.add(amount));
                                                        }))

                        // @formatter:off
                        // Ordernummer       : 000117637940-001  Rechnungsnummer   : 508104401078D295
                        //                     592581219254      Rechnungsnummer   : 878649826981vsP4
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>[\\-\\d]+) [\\s]{1,}Rechnungsnummer.*$") //
                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + v.get("note")))

                        // @formatter:off
                        //                     592581219254      Rechnungsnummer   : 878649826981vsP4
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Rechnungsnummer[\\s]{1,}: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "R.-Nr.: " + trim(v.get("note")), " | ")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift" //
                        + "|Ertragsgutschrift" //
                        + "|Zinsgutschrift" //
                        + "|Ertragsthesaurierung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividendengutschrift" //
                        + "|Ertragsgutschrift" //
                        + "|Zinsgutschrift" //
                        + "|Ertragsthesaurierung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // p e r   1 7.  01 .  20 1 8                         P r  oc t  e r  & G a m b l e  C o  .,   T he                85 2 0 6  2
                                        // ST K                0  , 3 1 6                R e gi  st  er  ed  S  ha r  e s  o .N  .            U S 7 4 27  18  10 9  1
                                        // USD 0,6896     Dividende pro Stück für Geschäftsjahr        01.07.17 bis 30.06.18
                                        //
                                        // p e r  1 3 . 0 5.  20 2  0                         U n i l ev e  r  N.  V .                           A 0 JM  Q 9
                                        // S T  K              1  3 , 94 4                 Aa  nd e l e  n  o p n  aa  m E O   -,  16          N  L0  00 0 3  88 6  1 9
                                        // EUR 0,4104     Dividende pro Stück für Geschäftsjahr        01.01.20 bis 31.12.20
                                        //
                                        // p e r  2  5.  02  .2  0 1 5                        i  S ha r  e s P L C  - M SC I  W  o . U C . E T F D  IS           A 0H  GV 0
                                        // S T K                11  ,9  71                 R eg i  s t er  e d  S ha  re  s o  .N  .            IE 0  0B 0 M  6 2 Q5 8
                                        // EUR 0,4104     Dividende pro Stück für Geschäftsjahr        01.01.20 bis 31.12.20
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?[\\.,\\d\\s]+ [\\s]{2,}(?<name>.*) [\\s]{2,}(?<wkn>[A-Z0-9\\s]+)$") //
                                                        .match("^([\\s]+)?S([\\s]+)?T([\\s]+)?K [\\s]{2,}[\\.,\\d\\s]+ [\\s]{2,}(?<nameContinued>.*) [\\s]{2,}(?<isin>[A-Z0-9\\s]+)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+[\\s]{1,}Dividende pro St.ck .*$") //
                                                        .assign((t, v) -> {
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // p e r  2  5.  02  .2  0 1 5                        i  S ha r  e s P L C  - M SC I  W  o . U C . E T F D  IS           A 0H  GV 0
                                        // S T K                11  ,9  71                 R eg i  s t er  e d  S ha  re  s o  .N  .            IE 0  0B 0 M  6 2 Q5 8
                                        // USD 0,0947     Ausschüttung pro Stück für Geschäftsjahr     01.03.14 bis 28.02.15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?[\\.,\\d\\s]+ [\\s]{2,}(?<name>.*) [\\s]{2,}(?<wkn>[A-Z0-9\\s]+)$") //
                                                        .match("^([\\s]+)?S([\\s]+)?T([\\s]+)?K [\\s]{2,}[\\.,\\d\\s]+ [\\s]{2,}(?<nameContinued>.*) [\\s]{2,}(?<isin>[A-Z0-9\\s]+)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+[\\s]{1,}Aussch.ttung pro St.ck .*$") //
                                                        .assign((t, v) -> {
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // p e r   3 1. 0  8 . 2 01 7                          Ly  x o r  F T S E A T  HE  X  L a . C ap   U.  ET  F           L Y X 0 BF
                                        // S T  K           3 . 1  0 0, 0 0  0               A  ct i  o ns   au  P o  rt e  u r o . N .           F R 0 0 1 0 4 0  5 4 3 1
                                        // EUR 0,00       Thesaurierung pro Stück für Geschäftsjahr    01.09.16 bis 31.08.17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "nameContinued", "isin", "currency") //
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?[\\.,\\d\\s]+ [\\s]{2,}(?<name>.*) [\\s]{2,}(?<wkn>[A-Z0-9\\s]+)$") //
                                                        .match("^([\\s]+)?S([\\s]+)?T([\\s]+)?K [\\s]{2,}[\\.,\\d\\s]+ [\\s]{2,}(?<nameContinued>.*) [\\s]{2,}(?<isin>[A-Z0-9\\s]+)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+[\\s]{1,}Thesaurierung pro St.ck .*$") //
                                                        .assign((t, v) -> {
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                                                        }),
                                        // @formatter:off
                                        // Zinsgutschrift
                                        // p  e r 2  2 .0  6 . 20  16             6 ,0  0          Co m  me  r zb  a nk   A G                          CN  2 V KP
                                        // E  U R           1 .  0 0 0, 0  0 0                AA L   P R OT E C  T 0  6 . 16  P  AH 3             D E 0 0 0C N  2 V K P 9
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "currency", "nameContinued", "isin") //
                                                        .find("Zinsgutschrift.*")
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?[\\.,\\d\\s]+ [\\s]{2,}(?<name>.*) [\\s]{2,}(?<wkn>[A-Z0-9\\s]+)$") //
                                                        .match("^(?<currency>\\s*[A-Z]\\s*[A-Z]\\s*[A-Z])[\\s]{2,}[\\.,\\d\\s]+[\\s]{1,}(?<nameContinued>.*) [\\s]{2,}(?<isin>[A-Z0-9\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            v.put("wkn", stripBlanks(v.get("wkn")));
                                                            v.put("isin", stripBlanks(v.get("isin")));
                                                            v.put("currency", stripBlanks(v.get("currency")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("nameContinued", trim(replaceMultipleBlanks(v.get("nameContinued"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // ST K                0  , 3 1 6                R e gi  st  er  ed  S  ha r  e s  o .N  .            U S 7 4 27  18  10 9  1
                        // E  U R           1 .  0 0 0, 0  0 0                AA L   P R OT E C  T 0  6 . 16  P  AH 3             D E 0 0 0C N  2 V K P 9
                        // @formatter:on
                        .section("notation", "shares") //
                        .match("^(?<notation>\\s*[A-Z]\\s*[A-Z]\\s*[A-Z])[\\s]{2,}(?<shares>[\\.,\\d\\s]+) [\\s]{2,}.*$") //
                        .assign((t, v) -> {
                            v.put("notation", stripBlanks(v.get("notation")));
                            v.put("shares", stripBlanks(v.get("shares")));

                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"STK".equalsIgnoreCase(v.get("notation")))
                            {
                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Verrechnung über Konto (IBAN)           Valuta       Zu Ihren Gunsten vor Steuern
                                        // DE36 3095 7353 0890 1239 79   EUR       19.02.2018         EUR               0,15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .find(".* Zu Ihren Gunsten.*") //
                                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\s]{2,}[\\w]{3} [\\s]{2,}[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // p e r   3 1. 0  8 . 2 01 7                          Ly  x o r  F T S E A T  HE  X  L a . C ap   U.  ET  F           L Y X 0 BF
                                        // EUR 0,00       Thesaurierung pro Stück für Geschäftsjahr    01.09.16 bis 31.08.17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?(?<date>[\\.,\\d\\s]+) [\\s]{2,}.* [\\s]{2,}[A-Z0-9\\s]+$") //
                                                        .match("^([\\s]+)?S([\\s]+)?T([\\s]+)?K [\\s]{2,}[\\.,\\d\\s]+ [\\s]{2,}.* [\\s]{2,}[A-Z0-9\\s]+$") //
                                                        .match("^[\\w]{3} [\\.,\\d]+[\\s]{1,}Thesaurierung pro St.ck .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date"))))))

                        .oneOf( //
                                        // @formatter:off
                                        // Verrechnung über Konto (IBAN)           Valuta       Zu Ihren Gunsten vor Steuern
                                        // DE36 3095 7353 0890 1239 79   EUR       19.02.2018         EUR               0,15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find(".* Zu Ihren Gunsten.*") //
                                                        .match("^.* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<amount>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // p e r   3 1. 0  8 . 2 01 7                          Ly  x o r  F T S E A T  HE  X  L a . C ap   U.  ET  F           L Y X 0 BF
                                        // EUR 0,00       Thesaurierung pro Stück für Geschäftsjahr    01.09.16 bis 31.08.17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^([\\s]+)?p([\\s]+)?e([\\s]+)?r ([\\s]+)?[\\.,\\d\\s]+ [\\s]{2,}.* [\\s]{2,}[A-Z0-9\\s]+$") //
                                                        .match("^([\\s]+)?S([\\s]+)?T([\\s]+)?K [\\s]{2,}[\\.,\\d\\s]+ [\\s]{2,}.* [\\s]{2,}[A-Z0-9\\s]+$") //
                                                        .match("^(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)[\\s]{1,}Thesaurierung pro St.ck .*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(0L);
                                                        }))

                        .optionalOneOf(
                                        // @formatter:off
                                        // Bruttobetrag:                     USD               0,22
                                        //     zum Devisenkurs: EUR/USD      1,250200                 EUR               0,15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag: [\\s]{2,}[\\w]{3} [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^.* zum Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) [\\s]{2,}(?<exchangeRate>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttobetrag:                     USD               0,22
                                        // 15,000 % Quellensteuer            USD               0,03 -
                                        //     zum Devisenkurs: EUR/USD      1,084100                 EUR               4,65
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxCurrency", "fxGross", "currencyWithHoldingTax", "withHoldingTax", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag: [\\s]{2,}(?<fxCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^[\\.,\\d]+ % Quellensteuer [\\s]{2,}(?<currencyWithHoldingTax>[\\w]{3}) [\\s]{2,}(?<withHoldingTax>[\\.,\\d]+) \\-.*$") //
                                                        .match("^.* zum Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) [\\s]{2,}(?<exchangeRate>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxWithHoldingTax = Money.of(asCurrencyCode(v.get("currencyWithHoldingTax")), asAmount(v.get("withHoldingTax")));
                                                            Money withHoldingTax = rate.convert(rate.getBaseCurrency(), fxWithHoldingTax);

                                                            // @formatter:off
                                                            // Add withHolding tax and set correct gross amount
                                                            // @formatter:on
                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(withHoldingTax));

                                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 15,000 % Quellensteuer                                     EUR               0,86 -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "withHoldingTax") //
                                                        .match("^[\\.,\\d]+ % Quellensteuer [\\s]{2,}(?<currency>[\\w]{3}) [\\s]{2,}(?<withHoldingTax>[\\.,\\d]+) \\-.*$") //
                                                        .assign((t, v) -> {
                                                            Money withHoldingTax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("withHoldingTax")));

                                                            // @formatter:off
                                                            // Add withHolding tax and set correct gross amount
                                                            // @formatter:on
                                                            if (t.getMonetaryAmount().getCurrencyCode().equals(withHoldingTax.getCurrencyCode()))
                                                                t.setMonetaryAmount(t.getMonetaryAmount().add(withHoldingTax));
                                                        }))

                        // @formatter:off
                        // Bruttobetrag:                     USD              50,00
                        //     zum Devisenkurs: EUR/USD      1,552700                 EUR              27,37
                        // Die nicht erstattungsfähige Quellensteuer in Höhe von      USD               7,50
                        // @formatter:on
                        .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currencyWithHoldingTax", "withHoldingTax").optional() //
                        .match("^Bruttobetrag: [\\s]{2,}(?<fxCurrency>[\\w]{3}) [\\s]{2,}(?<fxGross>[\\.,\\d]+).*$") //
                        .match("^.* zum Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) [\\s]{2,}(?<exchangeRate>[\\.,\\d]+) .*$") //
                        .match("^.* nicht erstattungsf.hige Quellensteuer .* [\\s]{2,}(?<currencyWithHoldingTax>[\\w]{3}) [\\s]{2,}(?<withHoldingTax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxWithHoldingTax = Money.of(rate.getTermCurrency(), asAmount(v.get("withHoldingTax")));
                            Money withHoldingTax = rate.convert(rate.getBaseCurrency(), fxWithHoldingTax);

                            // @formatter:off
                            // Subtract Non-refundable withholding tax and set correct gross amount
                            // @formatter:on
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(withHoldingTax));

                            Money fxGross = rate.convert(rate.getTermCurrency(), t.getMonetaryAmount());

                            checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // (Referenz-Nr. 0SID3MHIVFT000ZN).
                        // @formatter:on
                        .section("note").optional() //
                        .match("^\\(Referenz\\-Nr\\. (?<note>.*)\\).*$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // zahlbar ab 15.02.2018                 Quartalsdividende
                        // zahlbar ab 22.04.2013                 Zwischendividende
                        // zahlbar ab 21.03.2023                 Schlussdividende
                        // zahlbar ab 19.10.2017                 monatl. Dividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^zahlbar ab [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\s]{2,}" //
                                        + "(?<note>(Quartalsdividende" //
                                        + "|Zwischendividende" //
                                        + "|Schlussdividende" //
                                        + "|monatl\\. Dividende))" //
                                        + ".*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTreatmentTransaction()
    {
        DocumentType type = new DocumentType("Steuerliche Behandlung: " //
                        + "(Wertpapierkauf"
                        + "|Verkauf" //
                        + "|Wertpapierverkauf"
                        + "|Inl.ndische Dividende" //
                        + "|Inl.ndische Investment\\-Aussch.ttung" //
                        + "|Ausl.ndische Dividende" //
                        + "|Ausl.ndische Investment\\-Aussch.ttung" //
                        + "|Einl.sung" //
                        + "|Einbuchung Sachaussch.ttung" //
                        + "|Vorabpauschale)");
        this.addDocumentTyp(type);

        Block block = new Block("^Kundennr\\. .*$");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Stk.             400 IQIYI INC. ADR  DL-,00001 , WKN / ISIN: A2JGN8  / US46267X1081
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                        E  U   R                         9  .  1   1  0 , 3 5  U S D          1  0.  86 1 , 3  6
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Stk\\. [\\s]{2,}(\\-)?[\\.,\\d]+ (?<name>.*), WKN \\/ ISIN: (?<wkn>[A-Z0-9]{6}) .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?"
                                                                        + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)"
                                                                        + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{2,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{2,}[\\.,\\d\\s]+ [\\s]{1,}(?<currency>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{2,}[\\.,\\d\\s]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", stripBlanks(v.get("currency")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stk.               8 BAYER AG NA O.N. , WKN / ISIN: BAY001  / DE000BAY0017
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R             616,1 8
                                        //
                                        // Stk.               8,544 ARERO-WELTFDS-ESG LC , WKN / ISIN: DWS26Y  / LU2114851830
                                        //  Zu  Ih r e n L a s te n  v o r  S te u e r n:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Stk\\. [\\s]{2,}(\\-)?[\\.,\\d]+ (?<name>.*), WKN \\/ ISIN: (?<wkn>[A-Z0-9]{6}) .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?"
                                                                        + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)"
                                                                        + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{2,}(?<currency>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{2,}[\\.,\\d\\s]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", stripBlanks(v.get("currency")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // EUR           5.000 COBA CAM.PART.-ANL.09/15 , WKN / ISIN: CB89VM  / DE000CB89VM3
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R           6.584,4 5
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "wkn", "isin") //
                                                        .match("^(?<currency>[\\w]{3}) [\\s]{2,}(\\-)?(?<shares>[\\.,\\d]+) (?<name>.*), WKN \\/ ISIN: (?<wkn>[A-Z0-9]{6}) .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?"
                                                                        + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)"
                                                                        + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{2,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{2,}[\\.,\\d\\s]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", stripBlanks(v.get("currency")));
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Stk.               8 BAYER AG NA O.N. , WKN / ISIN: BAY001  / DE000BAY0017
                                        // Stk.              -0,049 GOOGLE INC.C      DL-,001 , WKN / ISIN: A110NH  / US38259P7069
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Stk\\. [\\s]{2,}(\\-)?(?<shares>[\\.,\\d]+) .*, WKN \\/ ISIN: [A-Z0-9]{6} .* [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // EUR           5.000 COBA CAM.PART.-ANL.09/15 , WKN / ISIN: CB89VM  / DE000CB89VM3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} [\\s]{2,}(\\-)?(?<shares>[\\.,\\d]+) .*, WKN \\/ ISIN: [A-Z0-9]{6} .* [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                                                        .assign((t, v) -> {
                                                            // Percentage quotation, workaround for bonds
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Steuerliche Behandlung: Wertpapierkauf Nr. 71112853 vom 02.01.2023
                                        // Steuerliche Behandlung: Wertpapierverkauf Nr. 91023920 vom 06.02.2020
                                        // Steuerliche Behandlung: Verkauf Investmentfonds Nr. 91000684 vom 05.06.2018
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Steuerliche Behandlung: (Wertpapierkauf|Verkauf|Wertpapierverkauf) .* (?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Die Gutschrift erfolgt mit Valuta 10.02.2020 auf Konto EUR mit der IBAN DE36 9902 2167 9152 6272 27
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Die Gutschrift erfolgt mit Valuta (?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Die Belastung erfolgt mit Valuta 14.01.2020 auf Konto EUR mit der IBAN XXXXXX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Die Belastung erfolgt mit Valuta (?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Steuerliche Behandlung: Vorabpauschale Ausland vom 04.01.2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Steuerliche Behandlung: Vorabpauschale .* (?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R               5,0 3
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g                  E  U   R                                  6 , 7 1
                                        // a b g e f ü h rt e S t e u er n                                                                                                                    E_ U_ _R _ _ _ _ _ _ _  __ _  _ __ _ _0_,_0 0_
                                        //
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R             616,1 8
                                        //  S te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g  ( 1 )                 E    U     R                                         -   2  4 , 8 7
                                        // a b g e f ü h rt e S t e u er n                                                                                                                    E_ _U R_ _ _ _ _ _ _ _ _  __  _ __  __0_,_0 0_
                                        //
                                        // Z u  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R              31,0 0
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e v o r V e r lu s tv e r re c h n u n g  ( 1 ) (2 2 )     E   U  R                                  0 , 3 1
                                        //  ab g e f ü h rt e S t e u er n                                                                                                                    _E _U R_ _ _ _ _ _ _ _  _ _ _ _ __  __0_,_0 _0
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "currencyTaxesBaseBeforeLossOffset", "sign", "grossTaxesBaseBeforeLossOffset", "currencyDeductedTaxes", "deductedTaxes") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?" //
                                                                        + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)" //
                                                                        + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}(?<currencyBeforeTaxes>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{1,}(\\-)?(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\(\\s\\d\\)]+)? [\\s]{1,}(?<currencyTaxesBaseBeforeLossOffset>[A-Z]\\s*[A-Z]\\s*[A-Z]) (?<sign>[\\-\\s]{1,})(?<grossTaxesBaseBeforeLossOffset>[\\.,\\d\\s]+).*$") //
                                                        .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}(?<currencyDeductedTaxes>[A-Z_\\s]+) [\\-_\\s]{1,}(?<deductedTaxes>[\\.,\\d_\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            Money grossBeforeTaxes = Money.of(asCurrencyCode(stripBlanks(v.get("currencyBeforeTaxes"))), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            Money grossTaxesBaseBeforeLossOffset = Money.of(asCurrencyCode(stripBlanks(v.get("currencyTaxesBaseBeforeLossOffset"))), asAmount(stripBlanks(v.get("grossTaxesBaseBeforeLossOffset"))));
                                                            Money deductedTaxes = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyDeductedTaxes"))), asAmount(stripBlanksAndUnderscores(v.get("deductedTaxes"))));

                                                            // Calculate the taxes
                                                            if (!grossBeforeTaxes.isZero() && grossTaxesBaseBeforeLossOffset.isGreaterThan(grossBeforeTaxes) && !"-".equals(trim(v.get("sign"))))
                                                            {
                                                                t.setMonetaryAmount(grossTaxesBaseBeforeLossOffset.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossTaxesBaseBeforeLossOffset);
                                                            }
                                                            else
                                                            {
                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                    // @formatter:off
                                    //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                        E  U   R                         9  .  1   1  0 , 3 5  U S D          1  0.  86 1 , 3  6
                                    // S  te u e rb e m  e ss u n g s g r u n d la g e ( 1 )                                                     E  U   R                         3  .  0   4  7 , 7 1
                                    //  ab g e f ü h rt e S t e u er n                                                                        E  U   R                           -  8   0  3 , 8 3  U_ _S D_ _ _ _ _ _ _ _  _ _ _ _- _9_ 5_  8__,  3_ 4_
                                    // @formatter:on
                                    section -> section //
                                                    .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "fxCurrencyAssessmentBasis", "fxGrossAssessmentBasis", "currencyDeductedTaxes", "deductedTaxes", "exchangeRate") //
                                                    .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?"
                                                                    + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)"
                                                                    + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{2,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{2,}[\\.,\\d\\s]+ [\\s]{1,}(?<currencyBeforeTaxes>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{2,}(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                    .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e(([\\s]+)?\\(([\\s]+)?[\\d\\s]+\\))? [\\s]{1,}(?<fxCurrencyAssessmentBasis>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{1,}(?<fxGrossAssessmentBasis>[\\.,\\d\\s]+).*$") //
                                                    .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}[A-Z_\\s]+ [\\-_\\s]{1,}[\\.,\\d_\\s]+ [\\s]{1,}(?<currencyDeductedTaxes>[A-Z_\\s]+) [\\-_\\s]{1,}(?<deductedTaxes>[\\.,\\d_\\s]+).*$") //
                                                    .match("^Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$") //
                                                    .assign((t, v) -> {
                                                        Money grossBeforeTaxes = Money.of(asCurrencyCode(stripBlanks(v.get("currencyBeforeTaxes"))), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                        Money fxGrossAssessmentBasis = Money.of(asCurrencyCode(stripBlanks(v.get("fxCurrencyAssessmentBasis"))), asAmount(stripBlanks(v.get("fxGrossAssessmentBasis"))));
                                                        Money deductedTaxes = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyDeductedTaxes"))), asAmount(stripBlanksAndUnderscores(v.get("deductedTaxes"))));

                                                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                        Money grossAssessmentBasis = Money.of(grossBeforeTaxes.getCurrencyCode(), BigDecimal.valueOf(fxGrossAssessmentBasis.getAmount())
                                                                        .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                        // Calculate the taxes and store gross amount
                                                        if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                        {
                                                            t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                            // Store in transaction context
                                                            v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                        }
                                                        else
                                                        {
                                                            // Store in transaction context
                                                            v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                            t.setMonetaryAmount(deductedTaxes);
                                                        }
                                                    }),
                                        // @formatter:off
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R               4,6 5
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e                                                            E  U   R                                  5 , 4 7
                                        // a b g e f ü h rt e S t e u er n                                                                                                                    _E U_ _R _ _ _ _ _ _ _  __ _ _  _ __-_0_,5_ _8
                                        //
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R             115,8 6
                                        //  S te u e rb e m  e ss u n g s g r u n d la g e ( 1 )                                                     E  U   R                                8  1 , 1 0
                                        // a b g e f ü h rt e S t e u er n                                                                                                                    E_ U_ _R _ _ _ _ _ _ _  __ _ _ _ -__2_1,__3 9_
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "currencyAssessmentBasis", "grossAssessmentBasis", "currencyDeductedTaxes", "deductedTaxes") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?" //
                                                                        + "(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)" //
                                                                        + "([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}(?<currencyBeforeTaxes>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{1,}(\\-)?(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e(([\\s]+)?\\(([\\s]+)?[\\d\\s]+\\))? [\\s]{1,}(?<currencyAssessmentBasis>[A-Z]\\s*[A-Z]\\s*[A-Z]) [\\s]{1,}(?<grossAssessmentBasis>[\\.,\\d\\s]+).*$") //
                                                        .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}(?<currencyDeductedTaxes>[A-Z_\\s]+) [\\-_\\s]{1,}(?<deductedTaxes>[\\.,\\d_\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            Money grossBeforeTaxes = Money.of(asCurrencyCode(stripBlanks(v.get("currencyBeforeTaxes"))), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            Money grossAssessmentBasis = Money.of(asCurrencyCode(stripBlanks(v.get("currencyAssessmentBasis"))), asAmount(stripBlanks(v.get("grossAssessmentBasis"))));
                                                            Money deductedTaxes = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyDeductedTaxes"))), asAmount(stripBlanksAndUnderscores(v.get("deductedTaxes"))));

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                            // @formatter:off
                                            // Z u  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R           1.263,0 5
                                            // S  te u e rb e m  e ss u n g s g r u n d la g e                                                            E  U   R                             -   3  8 , 7 2
                                            //  er s ta t te t e S t e ue r n                                                                                                                     E_ U_ R_ _ _ _ _ _ _ _ _  __  _ __ _10_,_8_ 4_
                                            //
                                            // Z  u     I h  r e   n    G  u   n   s  t e   n   v  o  r    S  t e  u   e   r n  :                                                         E  U   R                      1   0  .  5   8  3 , 9 9  U S D           11  .9 5 8 ,  8 5
                                            // S  te u e rb e m  e ss u n g s g r u n d la g e                                                            E  U   R                    -  1   2  .  6   4  4 , 1 7
                                            // e r s ta t te t e S t e ue r n                                                                         E  U   R                         3  .  5   3  9 , 5 8  U_ S_ D_ _ _ _ _ _ _ _  _ __ 3_ ._ _ 9_9 _ 9, _3 _ 7_
                                            // @formatter:on
                                            section -> section //
                                                            .attributes("currencyRefundedTaxes", "refundedTaxes") //
                                                            .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+.*$") //
                                                            .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e(([\\s]+)?\\(([\\s]+)?[\\d\\s]+\\))? [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+.*$") //
                                                            .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}[A-Z_\\s]+ [\\-_\\s]{1,}[\\.,\\d_\\s]+ [\\s]{1,}(?<currencyRefundedTaxes>[A-Z_\\s]+) [\\-_\\s]{1,}(?<refundedTaxes>[\\.,\\d_\\s]+)$") //
                                                            .assign((t, v) -> {
                                                                t.setType(AccountTransaction.Type.TAX_REFUND);

                                                                t.setCurrencyCode(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyRefundedTaxes"))));
                                                                t.setAmount(asAmount(stripBlanksAndUnderscores(v.get("refundedTaxes"))));
                                                        }),
                                        // @formatter:off
                                        // Z u  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R               1,4 0
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e n a c h V e r lu s t ve r r ec h n u n g  (8 )         E  U  R                                 -  0 , 1 0
                                        // K  ap i ta le r tr a gs t e ue r  ( 8 )                                                                   E  U   R                                  0 , 0 3
                                        // S ol id a ri tä t sz u s c hl a g                                                                      E  U   R                                  0 , 0 0
                                        // K irc h e n s te u e r                                                                              _E  _U   R_  _  _  _   _  _  _   _  _  _  _   _  _  _   _  0_ _, 0_ 0_
                                        // e r s ta t te t e S t e ue r n                                                                                                                     _E _U _R _ _ _ _ _ _ _  __  __  _ _ _0_,__0 _3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyRefundedTaxes", "refundedTaxes") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?(G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+.*$") //
                                                        .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e([\\s]+)?n([\\s]+)?a([\\s]+)?c([\\s]+)?h([\\s]+)?V([\\s]+)?e([\\s]+)?r([\\s]+)?l([\\s]+)?u([\\s]+)?s([\\s]+)?t([\\s]+)?v([\\s]+)?e([\\s]+)?r([\\s]+)?r([\\s]+)?e([\\s]+)?c([\\s]+)?h([\\s]+)?n([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\(\\s\\d\\)]+)? [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\-\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}[A-Z_\\s]+ [\\-_\\s]{1,}[\\.,\\d_\\s]+ [\\s]{1,}(?<currencyRefundedTaxes>[A-Z_\\s]+) [\\-_\\s]{1,}(?<refundedTaxes>[\\.,\\d_\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAX_REFUND);

                                                            t.setCurrencyCode(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyRefundedTaxes"))));
                                                            t.setAmount(asAmount(stripBlanksAndUnderscores(v.get("refundedTaxes"))));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Z  u     I h  r e   n    G  u   n   s  t e   n   v  o  r    S  t e  u   e   r n  :                                                         E  U   R                      1   0  .  5   8  3 , 9 9  U S D           11  .9 5 8 ,  8 5
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e                                                            E  U   R                    -  1   2  .  6   4  4 , 1 7
                                        // e r s ta t te t e S t e ue r n                                                                         E  U   R                         3  .  5   3  9 , 5 8  U_ S_ D_ _ _ _ _ _ _ _  _ __ 3_ ._ _ 9_9 _ 9, _3 _ 7_
                                        // Umrechnungen zum Devisenkurs       1,192200
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxRefundedTaxes", "baseCurrency", "refundedTaxes", "exchangeRate") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+ [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+$") //
                                                        .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e(([\\s]+)?\\(([\\s]+)?[\\d\\s]+\\))? [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+.*$") //
                                                        .match("^([\\s]+)?e([\\s]+)?r([\\s]+)?s([\\s]+)?t([\\s]+)?a([\\s]+)?t([\\s]+)?t([\\s]+)?e([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}(?<termCurrency>[A-Z_\\s]+) [\\-_\\s]{1,}(?<fxRefundedTaxes>[\\.,\\d_\\s]+) [\\s]{1,}(?<baseCurrency>[A-Z_\\s]+) [\\-_\\s]{1,}(?<refundedTaxes>[\\.,\\d_\\s]+)$") //
                                                        .match("^Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("baseCurrency", asCurrencyCode(stripBlanksAndUnderscores(v.get("baseCurrency"))));
                                                            v.put("termCurrency", asCurrencyCode(stripBlanksAndUnderscores(v.get("termCurrency"))));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(stripBlanksAndUnderscores(v.get("refundedTaxes"))));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(stripBlanksAndUnderscores(v.get("fxRefundedTaxes"))));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        //  Zu  Ih r e n G u n s t e n v o r S te u e r n :                                                        E  U   R                         9  .  1   1  0 , 3 5  U S D          1  0.  86 1 , 3  6
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e ( 1 )                                                     E  U   R                         3  .  0   4  7 , 7 1
                                        //  ab g e f ü h rt e S t e u er n                                                                        E  U   R                           -  8   0  3 , 8 3  U_ _S D_ _ _ _ _ _ _ _  _ _ _ _- _9_ 5_  8__,  3_ 4_
                                        // Umrechnungen zum Devisenkurs       1,192200
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxRefundedTaxes", "baseCurrency", "refundedTaxes", "exchangeRate") //
                                                        .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?I([\\s]+)?h([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n([\\s]+)?v([\\s]+)?o([\\s]+)?r([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n([\\s]+)?: [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+ [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+$") //
                                                        .match("^([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?b([\\s]+)?e([\\s]+)?m([\\s]+)?e([\\s]+)?s([\\s]+)?s([\\s]+)?u([\\s]+)?n([\\s]+)?g([\\s]+)?s([\\s]+)?g([\\s]+)?r([\\s]+)?u([\\s]+)?n([\\s]+)?d([\\s]+)?l([\\s]+)?a([\\s]+)?g([\\s]+)?e(([\\s]+)?\\(([\\s]+)?[\\d\\s]+\\))? [\\s]{1,}[A-Z]\\s*[A-Z]\\s*[A-Z] [\\s]{1,}(\\-)?[\\.,\\d\\s]+.*$") //
                                                        .match("^([\\s]+)?a([\\s]+)?b([\\s]+)?g([\\s]+)?e([\\s]+)?f([\\s]+)?.([\\s]+)?h([\\s]+)?r([\\s]+)?t([\\s]+)?e([\\s]+)?S([\\s]+)?t([\\s]+)?e([\\s]+)?u([\\s]+)?e([\\s]+)?r([\\s]+)?n [\\s]{1,}(?<termCurrency>[A-Z_\\s]+) [\\-_\\s]{1,}(?<fxRefundedTaxes>[\\.,\\d_\\s]+) [\\s]{1,}(?<baseCurrency>[A-Z_\\s]+) [\\-_\\s]{1,}(?<refundedTaxes>[\\.,\\d_\\s]+)$") //
                                                        .match("^Umrechnungen zum Devisenkurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("baseCurrency", asCurrencyCode(stripBlanksAndUnderscores(v.get("baseCurrency"))));
                                                            v.put("termCurrency", asCurrencyCode(stripBlanksAndUnderscores(v.get("termCurrency"))));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(stripBlanksAndUnderscores(v.get("refundedTaxes"))));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(stripBlanksAndUnderscores(v.get("fxRefundedTaxes"))));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // XXXXX XXXXX               R   e  f e  r e  n  z  - N   u mmer:     0 8  I F  L  C  B   P  Y 1000J7B
                        // Steuerliche Behandlung: Vorabpauschale Ausland vom 02.01.2020
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*R([\\s]+)?e([\\s]+)?f([\\s]+)?e([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?z([\\s]+)?\\-([\\s]+)?N([\\s]+)?u([\\s]+)?m([\\s]+)?m([\\s]+)?e([\\s]+)?r([\\s]+)?:[\\s]{1,}(?<note>.*)$")
                        .match("^Steuerliche Behandlung: Vorabpauschale .*$") //
                        .assign((t, v) -> t.setNote("Vorabpauschale | Ref.-Nr.: " + stripBlanks(v.get("note")).substring(0, 16)))

                        // @formatter:off
                        // 643  R  e  f e  r e  n  z  - N   u mmer:    2 G   I G   7  N   0  V  BSQ00112
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*R([\\s]+)?e([\\s]+)?f([\\s]+)?e([\\s]+)?r([\\s]+)?e([\\s]+)?n([\\s]+)?z([\\s]+)?\\-([\\s]+)?N([\\s]+)?u([\\s]+)?m([\\s]+)?m([\\s]+)?e([\\s]+)?r([\\s]+)?:[\\s]{1,}(?<note>.*)$")
                        .assign((t, v) -> {
                            if (t.getType().isCredit())
                                t.setNote(concatenate(t.getNote(), "Ref.-Nr.: " + stripBlanks(v.get("note")).substring(0, 16), " | "));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            // Store attribute in item data map
                            item.setData(ATTRIBUTE_GROSS_TAXES_TREATMENT, ctx.get(ATTRIBUTE_GROSS_TAXES_TREATMENT));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addDepositoryFeeTransaction()
    {
        DocumentType type = new DocumentType("Verwahrentgelt");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.* Verwahrentgelt .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.01.2020
                        // @formatter:on
                        .section("name", "wkn") //
                        .match("^.* Verwahrentgelt (?<name>.*), WKN (?<wkn>[A-Z0-9]{6}) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setShares(0L);
                        })

                        // @formatter:off
                        // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.01.2020
                        // @formatter:on
                        .section("date") //
                        .match("^.* Verwahrentgelt (?<name>.*), WKN [A-Z0-9]{6} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Die Buchung von 0,01 Euro für den vorherigen Monat erfolgte über das Abrechnungskonto für
                        // den vorherigen Monat mit einem Entgelt in Höhe von 123,45 Euro. Das entspricht 0,0298 %
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^.* (Buchung|H.he) von (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf(
                                        // @formatter:off
                                        // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.05.2019
                                        // den vorherigen Monat mit einem Entgelt in Höhe von 123,45 Euro. Das entspricht 0,0298 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.* (?<note1>Verwahrentgelt .*), WKN [A-Z0-9]{6} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^.* (?<note2>[\\.,\\d]+ %)(?! MwSt\\.).*$") //
                                                        .assign((t, v) -> t.setNote(
                                                                        v.get("note1") + " (" + v.get("note2") + ")")),
                                        // @formatter:off
                                        // Abrechnung Verwahrentgelt Xetra Gold, WKN A0S9GB 06.01.2020
                                        // Entgelt von 0,0298 % (0,025 % zzgl. 19 % MwSt.) auf Ihren Wert in der Gattung.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.* (?<note1>Verwahrentgelt .*), WKN [A-Z0-9]{6} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^.* (?<note2>[\\.,\\d]+ %).*$") //
                                                        .assign((t, v) -> t.setNote(v.get("note1") + " (" + v.get("note2") + ")")))

                        .wrap(TransactionItem::new);
    }

    private void addFinancialReport()
    {
        DocumentType type = new DocumentType("Finanzreport", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^Kontow.hrung (?<currency>[\\w]{3})$");
            Pattern pForeignCurrencyAccount = Pattern.compile("^W.hrungsanlagekonto \\((?<foreignCurrency>[\\w]{3})\\) .*$");
            Pattern pAccountingBillDate = Pattern.compile("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Kontoabschluss .*$");
            Pattern pStartForeignCurrency = Pattern.compile("^W.hrungsanlagekonto \\([\\w]{3}\\)$");
            Pattern pEndForeignCurrency = Pattern.compile("^Neuer Saldo .*$");

            boolean foreignCurrencyAccount = false;
            int startInForeignCurrencyIndex = -1;
            int endInForeignCurrencyIndex = -1;

            for (int i = 0; i < lines.length; i++)
            {
                String line = lines[i];

                switch (line)
                {
                    case "Ihre aktuellen Salden IBAN Saldo in":
                        context.put("currency", lines[i + 1]);
                        break;

                    case "Ihre aktuellen Salden Saldo in":
                        context.put("currency", lines[i + 1].substring(5, 8));
                        break;

                    default:
                        Matcher mCurrency = pCurrency.matcher(line);
                        if (mCurrency.matches())
                            context.put("currency", mCurrency.group("currency"));

                        Matcher mForeignCurrencyAccount = pForeignCurrencyAccount.matcher(line);
                        if (mForeignCurrencyAccount.matches())
                            context.put("foreignCurrency", mForeignCurrencyAccount.group("foreignCurrency"));

                        Matcher mStartForeignCurrency = pStartForeignCurrency.matcher(line);
                        if (mStartForeignCurrency.matches())
                        {
                            startInForeignCurrencyIndex = i;
                            foreignCurrencyAccount = true;
                        }

                        Matcher mEndForeignCurrency = pEndForeignCurrency.matcher(line);
                        if (mEndForeignCurrency.matches() && foreignCurrencyAccount)
                        {
                            endInForeignCurrencyIndex = i;
                            foreignCurrencyAccount = false;
                        }

                        Matcher mAccountingBillDate = pAccountingBillDate.matcher(line);
                        if (mAccountingBillDate.matches())
                            context.put("accountingBillDate", mAccountingBillDate.group("date"));

                        break;
                }
            }

            if (startInForeignCurrencyIndex != -1)
                context.put("startInForeignCurrency", Integer.toString(startInForeignCurrencyIndex));

            if (endInForeignCurrencyIndex != -1)
                context.put("endInForeignCurrency", Integer.toString(endInForeignCurrencyIndex));
        });
        this.addDocumentTyp(type);

        Block removalBlock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Konto.bertrag"
                        + "|.bertrag"
                        + "|Lastschrift"
                        + "|Visa\\-Umsatz"
                        + "|Auszahlung"
                        + "|Barauszahlung"
                        + "|Kartenverf.gun"
                        + "|Guthaben.bertr"
                        + "|Wechselgeld\\-).* "
                        + "\\-[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(3);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("note1", "note2", "amount", "date")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note1>Konto.bertrag"
                                        + "|.bertrag"
                                        + "|Lastschrift"
                                        + "|Visa\\-Umsatz"
                                        + "|Auszahlung"
                                        + "|Barauszahlung"
                                        + "|Kartenverf.gun"
                                        + "|Guthaben.bertr"
                                        + "|Wechselgeld\\-)"
                                        + "(?<note2>.*) "
                                        + "\\-(?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));

                            // When we recognize a foreign currency account,
                            // we change from currency to foreign currency

                            boolean hasForeignCurrencyBlock = context.containsKey("startInForeignCurrency")
                                            && context.containsKey("endInForeignCurrency");

                            if (hasForeignCurrencyBlock
                                            && v.getStartLineNumber() >= Integer
                                                            .parseInt(context.get("startInForeignCurrency"))
                                            && v.getEndLineNumber() <= Integer
                                                            .parseInt(context.get("endInForeignCurrency")))
                            {
                                t.setCurrencyCode(context.get("foreignCurrency"));
                            }

                            // Formatting some notes
                            if (v.get("note1").startsWith("Kartenverfügun"))
                                v.put("note", "Kartenverfügung Kartenzahlung");
                            else if (v.get("note2").matches("^(?i).*Wechselgeld\\-.*$"))
                                v.put("note", "Wechselgeld-Sparen");
                            else if (v.get("note2").matches("^(?i).*Uebertrag auf Girokonto.*$"))
                                v.put("note", "Übertrag auf Girokonto");
                            else if (v.get("note2").matches("^(?i).*Uebertrag auf Tagesgeld PLUS\\-Konto.*$"))
                                v.put("note", "Übertrag auf Tagesgeld PLUS-Konto");
                            else if (v.get("note2").matches("^(?i).*Uebertrag auf Visa\\-Karte.*$"))
                                v.put("note", "Übertrag auf Visa-Karte");
                            else
                                v.put("note", v.get("note1"));

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block depositBlock = new Block("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Konto.bertrag"
                        + "|.bertrag"
                        + "|Guthaben.bertr"
                        + "|Gutschrift"
                        + "|Bar"
                        + "|Visa\\-Kartenabre"
                        + "|Korrektur Barauszahlung).* "
                        + "\\+[\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(3);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note1", "note2", "amount", "date")
                        .match("(^|^A)[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note1>Konto.bertrag"
                                        + "|.bertrag"
                                        + "|Guthaben.bertr"
                                        + "|Gutschrift"
                                        + "|Bar"
                                        + "|Visa\\-Kartenabre"
                                        + "|Korrektur Barauszahlung)"
                                        + "(?<note2>.*) "
                                        + "\\+(?<amount>[\\.,\\d]+)$")
                        .match("(^|^A)(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));

                            // When we recognize a foreign currency account,
                            // we change from currency to foreign currency

                            boolean hasForeignCurrencyBlock = context.containsKey("startInForeignCurrency")
                                            && context.containsKey("endInForeignCurrency");

                            if (hasForeignCurrencyBlock
                                            && v.getStartLineNumber() >= Integer
                                                            .parseInt(context.get("startInForeignCurrency"))
                                            && v.getEndLineNumber() <= Integer
                                                            .parseInt(context.get("endInForeignCurrency")))
                            {
                                t.setCurrencyCode(context.get("foreignCurrency"));
                            }

                            // Formatting some notes
                            if (v.get("note2").matches("^(?i).*Uebertrag auf Girokonto.*$"))
                                v.put("note", "Übertrag auf Girokonto");
                            else if (v.get("note2").matches("^(?i).*Uebertrag auf Tagesgeld PLUS\\-Konto.*$"))
                                v.put("note", "Übertrag auf Tagesgeld PLUS-Konto");
                            else if (v.get("note2").matches("^(?i).*Uebertrag auf Visa\\-Karte.*$"))
                                v.put("note", "Übertrag auf Visa-Karte");
                            else if (v.get("note2").matches("^(?i).*Bargeldeinzahlung Karte.*$"))
                                v.put("note", "Bargeldeinzahlung Karte");
                            else if (v.get("note2").matches("^(?i).*Gutschrift aus Bonus\\-Sparen.*$"))
                                v.put("note", "Gutschrift aus Bonus-Sparen");
                            else if (v.get("note2").matches("^(?i).*Gutschr\\. Wechselgeld\\-Sparen.*$"))
                                v.put("note", "Gutschrift Wechselgeld-Sparen");
                            else if (v.get("note1").matches("^(?i).*Visa\\-Kartenabre.*$"))
                                v.put("note", "Visa-Kartenabrechnung");
                            else
                                v.put("note", v.get("note1"));

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(Geb.hren\\/Spesen"
                        + "|Geb.hr Barauszahlung"
                        + "|Entgelte"
                        + "|Kontof.hrungse"
                        + "|Auslandsentgelt).* "
                        + "\\-[\\.,\\d]+$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note")
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                        + "(?<note>Geb.hren\\/Spesen"
                                        + "|Geb.hr Barauszahlung"
                                        + "|Entgelte"
                                        + "|Kontof.hrungse"
                                        + "|Auslandsentgelt).* "
                                        + "\\-(?<amount>[\\.,\\d]+)$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));

                            // Formatting some notes
                            if (v.get("note").startsWith("Kontoführungse"))
                                v.put("note", "Kontoführungsentgelt");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block accountingBillFeeBlock = new Block("^Versandpauschale [\\.,\\d]+\\- [\\w]{3}$");
        type.addBlock(accountingBillFeeBlock);
        accountingBillFeeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note", "amount", "currency")
                        .match("^(?<note>Versandpauschale) "
                                        + "(?<amount>[\\.,\\d]+)\\- "
                                        + "(?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(context.get("accountingBillDate")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Kontoabschluss Abschluss Zinsen.* [\\-|\\+][\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "type", "amount", "date")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                        + "(?<note>Kontoabschluss Abschluss Zinsen).* "
                                        + "(?<type>[\\-|\\+])"
                                        + "(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            // Is sign --> "-" change from INTEREST to INTEREST_CHARGE
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(context.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block taxesBlock = new Block("^(Kapitalertragsteuer|Solidarit.tszuschlag|Kirchensteuer) [\\.,\\d]+[\\-|\\+] [\\w]{3}$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type", "currency")
                        .match("^(?<note>(Kapitalertragsteuer"
                                        + "|Solidarit.tszuschlag"
                                        + "|Kirchensteuer)) "
                                        + "(?<amount>[\\.,\\d]+)"
                                        + "(?<type>[\\-|\\+]) "
                                        + "(?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            // Is sign --> "-" change from TAXES to TAX_REFUND
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAXES);

                            t.setDateTime(asDate(context.get("accountingBillDate")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        //                           Transaktionssteuer          : GBP              213,60
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.* Transaktionssteuer[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Die nicht erstattungsfähige Quellensteuer in Höhe von      USD               7,50
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.* nicht erstattungsf.hige Quellensteuer in H.he von[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 25,000 % Kapitalertragsteuer auf  EUR           2.100,00   EUR             525,00 -
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.* Kapitalertragsteuer auf[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        //  5,500 % Solidaritätszuschl. auf  EUR             525,00   EUR              28,87 -
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.* Solidarit.tszuschl\\. auf[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // fremde Spesen                     USD               0,08 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^fremde Spesen[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Fremde Spesen               : USD               27,90
                        //                           Fremde Spesen               : USD               13,90-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Fremde Spesen[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                  0,08000% Maklercourtage              : EUR                0,88-
                        //                           Maklercourtage              : EUR                0,75-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Maklercourtage[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Abwickl.entgelt Clearstream : EUR                2,90-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Abwickl\\.entgelt Clearstream[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Variable Börsenspesen       : EUR                3,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Variable B.rsenspesen[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Gesamtprovision             : EUR                9,90
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Gesamtprovision[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Börsenplatzabhäng. Entgelt  : EUR                1,50-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* B.rsenplatzabh.ng\\. Entgelt[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Umschreibeentgelt           : EUR                0,95
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Umschreibeentgelt[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        //                           Provision                   : EUR                9,90-
                        //                           Provision                   : EUR                0,37
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Provision[\\s]{1,}: (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processFeeEntries(t, v, type);
                        });
    }

    /**
     * @formatter:off
     * This method performs post-processing on a list transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions, sale transactions, and dividend transactions.
     * 2. Matches sale transactions with their corresponding taxes treatment and dividend transactions with their corresponding taxes treatment.
     * 3. Adjusts sale transactions by subtracting tax amounts, adding tax units, combining source information, appending tax-related notes,
     *    and removing taxes treatment's from the list of items.
     * 4. Adjusts dividend transactions by updating the gross amount if necessary, subtracting tax amounts, adding tax units,
     *    combining source information, appending taxes treatment notes, and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in sale and dividend transactions, making the transaction's more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        List<Item> taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> { //
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by buySell transactions
        List<Item> saleTransactionList = items.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> PortfolioTransaction.Type.SELL //
                                        .equals((((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType()))) //
                        .toList();

        // Filter transactions by dividend transactions
        List<Item> dividendTransactionList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .toList();

        var saleTaxPairs = matchTransactionPair(saleTransactionList, taxesTreatmentList);
        var dividendTaxPairs = matchTransactionPair(dividendTransactionList, taxesTreatmentList);

        fixMissingCurrencyConversionForSaleTaxesTransactions(saleTaxPairs);

        // @formatter:off
        // This loop iterates through a list of sale and tax pairs and processes them.
        //
        // For each pair, it subtracts the tax amount from the sale transaction's total amount,
        // adds the tax as a tax unit to the sale transaction, combines source information if needed,
        // appends taxes treatment notes to the sale transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : saleTaxPairs)
        {
            var saleTransaction = (BuySellEntry) pair.transaction.getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null && taxesTransaction.getType() == AccountTransaction.Type.TAXES)
            {
                saleTransaction.setMonetaryAmount(saleTransaction.getPortfolioTransaction().getMonetaryAmount()
                                .subtract(taxesTransaction.getMonetaryAmount()));

                saleTransaction.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                saleTransaction.setSource(concatenate(saleTransaction.getSource(), taxesTransaction.getSource(), "; "));

                saleTransaction.setNote(concatenate(saleTransaction.getNote(), taxesTransaction.getNote(), " | "));

                items.remove(pair.tax());
            }
        }

         // @formatter:off
         // This loop processes a list of dividend and tax pairs, adjusting the gross amount of dividend transactions as needed.
         //
         // For each pair, it checks if there is a corresponding tax transaction. If present, it considers the gross taxes treatment,
         // adjusting the gross amount of the dividend transaction if necessary. If taxes amount is zero and gross taxes treatment
         // is less than the dividend amount, it adjusts the taxes and sets the dividend amount to the gross taxes treatment.
         // If taxes amount is not zero, it sets the dividend amount to the gross taxes treatment and fixes the gross value.
         //
         // If there is no tax transaction, it simply fixes the gross value of the dividend transaction.
         // @formatter:on
        for (TransactionTaxesPair pair : dividendTaxPairs)
        {
            var dividendTransaction = (AccountTransaction) pair.transaction().getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null)
            {
                Money grossTaxesTreatment = (Money) pair.tax().getData(ATTRIBUTE_GROSS_TAXES_TREATMENT);

                if (grossTaxesTreatment != null)
                {
                    Money dividendAmount = dividendTransaction.getMonetaryAmount();
                    Money taxesAmount = taxesTransaction.getMonetaryAmount();

                    if (taxesAmount.isZero() && grossTaxesTreatment.isLessThan(dividendAmount))
                    {
                        Money adjustedTaxes  = dividendAmount.subtract(grossTaxesTreatment);
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, adjustedTaxes ));
                        dividendTransaction.setMonetaryAmount(grossTaxesTreatment);
                    }
                    else
                    {
                        dividendTransaction.setMonetaryAmount(grossTaxesTreatment);
                    }
                }

                ExtractorUtils.fixGrossValue().accept(dividendTransaction);

                dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount() //
                                .subtract(taxesTransaction.getMonetaryAmount()));

                dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                dividendTransaction.setSource(concatenate(dividendTransaction.getSource(), taxesTransaction.getSource(), "; "));

                dividendTransaction.setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

                items.remove(pair.tax());
            }
            else
            {
                ExtractorUtils.fixGrossValue().accept(dividendTransaction);
            }
        }
    }

    /**
     * @formatter:off
     * Matches transactions and taxes treatment's, ensuring unique pairs based on date and security.
     *
     * This method matches transactions and taxes treatment's by creating a Pair consisting of the transaction's
     * date and security. It uses a Set called 'keys' to prevent duplicates based on these Pair keys,
     * ensuring that the same combination of date and security is not processed multiple times.
     * Duplicate transactions for the same security on the same day are avoided.
     *
     * @param transactionList      A list of transactions to be matched.
     * @param taxesTreatmentList   A list of taxes treatment's to be considered for matching.
     * @return A collection of TransactionTaxesPair objects representing matched transactions and taxes treatment's.
     * @formatter:on
     */
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList, List<Item> taxesTreatmentList)
    {
        // Use a Set to prevent duplicates
        Set<Pair<LocalDate, Security>> keys = new HashSet<>();
        Map<Pair<LocalDate, Security>, TransactionTaxesPair> pairs = new HashMap<>();

        // Match identified transactions and taxes treatment's
        transactionList.forEach( //
                        transaction -> {
                            var key = new Pair<>(transaction.getDate().toLocalDate(), transaction.getSecurity());

                            // Prevent duplicates
                            if (keys.add(key))
                                pairs.put(key, new TransactionTaxesPair(transaction, null));
                        } //
        );

        // Iterate through the list of taxes treatment's to match them with transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
    }

    /**
     * @formatter:off
     * This method fixes missing currency conversion for taxes transactions.
     *
     * It iterates through a collection of TransactionTaxesPair objects and performs the necessary currency conversions
     * if required based on the currency codes of the involved transactions.
     *
     * @param saleTaxPairs A collection of TransactionTaxesPair objects containing taxes and sale transactions.
     * @formatter:on
     */
    private void fixMissingCurrencyConversionForSaleTaxesTransactions(Collection<TransactionTaxesPair> saleTaxPairs)
    {
        saleTaxPairs.forEach( //
                        pair -> { //
                            if (pair.tax != null)
                            {
                                // Get the taxes treatment from the SaleTaxPair
                                var tax = (AccountTransaction) pair.tax.getSubject();

                                // Check if currency conversion is needed
                                if (!tax.getSecurity().getCurrencyCode().equals(tax.getMonetaryAmount().getCurrencyCode()))
                                {
                                    // Get the sale transaction from the SaleTaxPair
                                    var sale = (BuySellEntry) pair.transaction.getSubject();

                                    // Check if we have an exchange rate available from the sale transaction
                                    var grossValue = sale.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE);

                                    if (grossValue.isPresent() && grossValue.get().getExchangeRate() != null)
                                    {
                                        // Create and set the required grossUnit to the taxes treatment
                                        var rate = new ExtrExchangeRate(grossValue.get().getExchangeRate(),
                                                        sale.getPortfolioTransaction().getSecurity().getCurrencyCode(),
                                                        tax.getCurrencyCode());

                                        String termCurrency = sale.getPortfolioTransaction().getSecurity().getCurrencyCode();
                                        Money fxGross = rate.convert(termCurrency, tax.getMonetaryAmount());

                                        // Add the converted gross value unit to the taxes transaction
                                        tax.addUnit(new Unit(Unit.Type.GROSS_VALUE, tax.getMonetaryAmount(), fxGross, rate.getRate()));
                                    }
                                }
                            }
                        } //
        );
    }
}

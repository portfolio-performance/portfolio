package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class INGDiBaPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isJointAccount";

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("KapSt anteilig [\\d]{2},[\\d]{2} %.*");

        for (String line : lines)
        {
            if (pJointAccount.matcher(line).matches())
            {
                context.putBoolean(IS_JOINT_ACCOUNT, true);
                break;
            }
        }
    };

    public INGDiBaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("ING-DiBa AG");
        addBankIdentifier("ING BANK NV");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa AG / ING Groep N.V.";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Wertpapierabrechnung " //
                        + "(Kauf" //
                        + "|Kauf Einmalanlage" //
                        + "|Kauf aus Sparplan" //
                        + "|Kauf aus Wiederanlage Fondsaussch.ttung" //
                        + "|Kauf Zeichnung" //
                        + "|Bezug" //
                        + "|Verkauf" //
                        + "|Verkauf aus Kapitalmaßnahme" //
                        + "|Verk\\. Teil\\-\\/Bezugsr\\.)" //
                        + "|R.ckzahlung" //
                        + "|Einl.sung"
                        + "|Operaciones Cuenta de Valores)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("(Wertpapierabrechnung " //
                        + "(Kauf" //
                        + "|Kauf Einmalanlage" //
                        + "|Kauf aus Sparplan" //
                        + "|Kauf aus Wiederanlage Fondsaussch.ttung" //
                        + "|Kauf Zeichnung" //
                        + "|Bezug" //
                        + "|Verkauf" //
                        + "|Verkauf aus Kapitalmaßnahme" //
                        + "|Verk\\. Teil\\-\\/Bezugsr\\.)" //
                        + "|R.ckzahlung" //
                        + "|Einl.sung"
                        + "|Operaciones Cuenta de Valores)");
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
                        .match("^(Wertpapierabrechnung )?" //
                                        + "(?<type>(Kauf" //
                                        + "|Kauf Einmalanlage" //
                                        + "|Kauf aus Sparplan" //
                                        + "|Kauf aus Wiederanlage Fondsaussch.ttung" //
                                        + "|Kauf Zeichnung" //
                                        + "|Bezug" //
                                        + "|Verkauf" //
                                        + "|Verkauf aus Kapitalmaßnahme" //
                                        + "|Verk. Teil\\-\\/Bezugsr\\.)" //
                                        + "|R.ckzahlung" //
                                        + "|Einl.sung"
                                        + "|Venta)$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "Verkauf aus Kapitalmaßnahme".equals(v.get("type")) //
                                            || "Verk. Teil-/Bezugsr.".equals(v.get("type")) //
                                            || "Rückzahlung".equals(v.get("type")) //
                                            || "Einlösung".equals(v.get("type")))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                            }
                        })

                        // Is type --> "Venta" change from BUY to SELL
                        .section("type").optional() //
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* (?<type>(Compra|Venta)) .*$") //
                        .assign((t, v) -> {
                            if ("Venta".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ISIN (WKN) DE0002635307 (263530)
                                        // Wertpapierbezeichnung iSh.STOXX Europe 600 U.ETF DE
                                        // Inhaber-Anteile
                                        // Kurswert EUR 4.997,22
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "name1", "currency") //
                                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominale") && !v.get("name1").startsWith("Zinstermin"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                                        // 30 DIAGEO GB0002374006 LONDRES Compra 25,19 GBP 894,48 EUR 4,47 EUR 4,52 EUR 0,00 EUR 4,47 EUR 907,94 EUR
                                        // 125 EBRO FOODS ES0112501012 M.CONTINUO Venta 16,66 EUR 2.082,50 EUR 5,08 EUR 3,17 EUR 0,00 EUR 2.074,25 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^[\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .* (Compra|Venta) [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominale Stück 14,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Nominale St.ck (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominale 11,00 Stück
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Nominale (?<shares>[\\.,\\d]+) St.ck$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominale EUR 1.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Nominale [\\w]{3} (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // Nominale 2.000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Nominale (?<shares>[\\.,\\d]+) [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Ausführungstag / -zeit 17.11.2015 um 16:17:32 Uhr
                        // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                        // @formatter:on
                        .section("time").optional() //
                        .match("^(Ausf.hrungstag|Schlusstag) \\/ \\-zeit [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // 14/08/2023 14/08/2023 15:37 67 Limitada 15,01 EUR 14/08/2023 12:21 1005,67 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Ausführungstag 15.12.2015
                                        // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                                        // Fälligkeit 25.05.2017
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date").multipleTimes() //
                                                        .match("^(Ausf.hrungstag|Schlusstag|F.lligkeit)( \\/ -zeit)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("[\\.,\\d]+ .* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Endbetrag zu Ihren Lasten EUR 533,39
                                        // Endbetrag zu Ihren Gunsten EUR 1.887,64
                                        // Endbetrag EUR 256,66
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(Ausf.hrungstag|Schlusstag|F.lligkeit)( \\/ -zeit)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$") //
                                                        .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Zwischensumme USD 1.503,75
                                        // umger. zum Devisenkurs EUR 1.311,99 (USD = 1,146163)
                                        // Endbetrag zu Ihren Lasten EUR 1.335,07
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "gross", "termCurrency", "exchangeRate", "baseCurrency") //
                                                        .match("^Zwischensumme [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                                                        .match("^.* Devisenkurs [\\w]{3} (?<gross>[\\.,\\d]+) \\((?<termCurrency>[\\w]{3}) = (?<exchangeRate>[\\.,\\d]+)\\)$") //
                                                        .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<baseCurrency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 07/06/2024 19/06/2024 09:00 30 Limitada 25,19 GBP 07/06/2024 16:15 1,190 EUR 899,28 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "exchangeRate", "baseCurrency", "gross") //
                                                        .match("^.* (?<termCurrency>[\\w]{3}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}:[\\d]{2} (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Ordernummer 12345678.001
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Ordernummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Diese Order wurde mit folgendem Limit / -typ erteilt: 38,10 EUR
                                        // Diese Order wurde mit folgendem Limit / -typ erteilt: 57,00 EUR / Dynamisches Stop / Abstand 6,661 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^Diese Order wurde mit folgendem (?<note1>Limit) .*: (?<note2>[\\.,\\d]+ [\\w]{3})( .*)?$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + ": " + v.get("note2"), " | "))),
                                        // @formatter:off
                                        // Rückzahlung
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>R.ckzahlung)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Stückzinsen EUR 0,10 (Zinsvaluta 17.11.2022 357 Tage)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>St.ckzinsen .*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Dividendengutschrift" //
                        + "|Ertragsgutschrift" //
                        + "|Zinsgutschrift"
                        + "|Abono de Dividendos)", //
                        jointAccount);
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividendengutschrift" //
                        + "|Ertragsgutschrift" //
                        + "|Zinsgutschrift"
                        + "|Abono de Dividendos).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Sie erhalten eine neue Abrechnung.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Sie erhalten eine neue Abrechnung\\.)$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // ISIN (WKN) US5801351017 (856958)
                                        // Wertpapierbezeichnung McDonald's Corp.
                                        // Registered Shares DL-,01
                                        // Zins-/Dividendensatz 0,94 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "name1", "currency") //
                                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Zins\\-\\/Dividendensatz|(Ertragsaussch.ttung|Vorabpauschale) per St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominale"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // ISIN (WKN) DE000A1PGUT9 (A1PGUT)
                                        // Wertpapierbezeichnung 7,25000% posterXXL AG
                                        // Inh.-Schv.v.2012(2015/2017)
                                        // Nominale 1.000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "nameContinued", "currency") //
                                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Nominale [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // ISIN (WKN) DE000A3MP4P9 (A3MP4P)
                                        // Wertpapierbezeichnung 4,00000% PCC SE Inh.-Teilschuldv. v.21(22/26)
                                        // Nominale 5.000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "currency") //
                                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                                                        .match("^Nominale [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Valor: ADMIRAL GROUP PLC(ADM) Mercado: LONDRES
                                        // Importe por título: 0,61 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency") //
                                                        .match("^Valor: (?<name>.*) Mercado: .*$") //
                                                        .match("^Importe por t.tulo: [\\.,\\d]+ (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale 66,00 Stück
                                        // Nominale 1.000,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "notation") //
                                                        .match("^Nominale (?<shares>[\\.,\\d]+) (?<notation>(St.ck|[\\w]{3}))$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
                                                            {
                                                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                            }
                                                            else
                                                            {
                                                                t.setShares(asShares(v.get("shares")));
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Número de títulos: 103
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^N.mero de t.tulos: (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Valuta 15.12.2016
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 07/06/2024 1465 0100 32 5120505073 51,25 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag zu Ihren Gunsten EUR 44,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gesamtbetrag zu Ihren Gunsten (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // If the total amount is negative, then we change the
                                        // transaction type from DIVIDENDS to TAXES.
                                        //
                                        // Gesamtbetrag zu Ihren Lasten EUR - 20,03
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) \\- (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            if (v.getTransactionContext().get(FAILURE) == null)
                                                                t.setType(AccountTransaction.Type.TAXES);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Importe total neto: 51,25 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Importe total neto: (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Brutto USD 62,04
                        // Umg. z. Dev.-Kurs (1,049623) EUR 50,24
                        //
                        // Brutto USD - 54,00
                        // Umg. z. Dev.-Kurs (1,084805) EUR - 37,33
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional() //
                        .match("^Brutto (?<termCurrency>[\\w]{3}) (\\- )?(?<fxGross>[\\.,\\d]+)$") //
                        .match("^Umg\\. z\\. Dev\\.\\-Kurs \\((?<exchangeRate>[\\.,\\d]+)\\) (?<baseCurrency>[\\w]{3}) (\\- )?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        final DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Vorabpauschale$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // ISIN (WKN) IE00BKPT2S34 (A2P1KU)
                        // Wertpapierbezeichnung iShsIII-Gl.Infl.L.Gov.Bd U.ETF
                        // Reg. Shs HGD EUR Acc. oN
                        // Nominale 378,00 Stück
                        // Vorabpauschale per Stück 0,00245279 EUR
                        // @formatter:on
                        .section("isin", "wkn", "name", "name1", "currency") //
                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Vorabpauschale per St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominale"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Nominale 378,00 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominale (?<shares>[\\.,\\d]+) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 04.01.2021
                        // Zahltag 02.01.2024
                        // @formatter:on
                        .section("date") //
                        .match("^(Valuta|Zahltag) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gesamtbetrag zu Ihren Lasten EUR - 0,16
                        // Gesamtbetrag zu Ihren Gunsten EUR 0,00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gesamtbetrag zu Ihren (Lasten|Gunsten) (?<currency>[\\w]{3}) (\\- )?(?<amount>[.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug .*[\\d]{4}", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // Valuta Vorgang Euro
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Buchung Buchung \\/ Verwendungszweck Betrag \\((?<currency>[\\w]{3})\\)$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("currency", asCurrencyCode("EUR"));
                                                                        }),
                                                        // @formatter:off
                                                        // Valuta Vorgang Euro
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Valuta Vorgang (?<currency>Euro)$") //
                                                                        .assign((ctx, v) -> {
                                                                            if ("Euro".equals(v.get("currency")))
                                                                                ctx.put("currency", asCurrencyCode("EUR"));
                                                                        })));

        this.addDocumentTyp(type);

        // @formatter:off
        // 13.07.2016 Ueberweisung Mustermann -5.000,00
        // 13.02.2020 Gutschrift/Dauerauftrag Max Mustermann 1,01
        // 16.02.2020 Lastschrift XYZ GmbH -10,00
        // 06.03.2023 Kontolöschung -1.161,10
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Ueberweisung" //
                        + "|Dauerauftrag\\/Terminueberw\\." //
                        + "|Lastschrift" //
                        + "|Kontol.schung)" //
                        + ".* \\-[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Ueberweisung" //
                                        + "|Dauerauftrag\\/Terminueberw\\." //
                                        + "|Lastschrift" //
                                        + "|Kontol.schung)" //
                                        + ".* \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Ueberweisung".equals(v.get("note")))
                                v.put("note", "Überweisung");

                            if ("Dauerauftrag/Terminueberw.".equals(v.get("note")))
                                v.put("note", "Dauerauftrag/Terminüberweisung");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 27.06.2016 Gutschrift Max Mustermann 10.000,00
        // 14.02.2020 Dauerauftrag/Terminueberw. Max Mustermann -30,00
        // 29.04.2021 Gehalt/Rente Hauptkasse des Freistaates Sachsen 806,83
        // 13.10.2020 Gutschrift-VWL 40,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Gutschrift-VWL" //
                        + "|Gutschrift\\/Dauerauftrag" //
                        + "|Gehalt\\/Rente" //
                        + "|Gutschrift)" //
                        + ".* [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Gutschrift\\-VWL" //
                                        + "|Gutschrift\\/Dauerauftrag" //
                                        + "|Gehalt\\/Rente" //
                                        + "|Gutschrift)" //
                                        + ".* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (bis|Zinsen|Zinsgutschrift)( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})?.* [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 01.01.2016 bis 14.06.2016 0,50%  Zins 0,40
                                        // 15.06.2016 bis 31.12.2016 0,35%  Zins 5,22
                                        // 16.12.2023 bis 31.12.2023 3,750%  bis 250.000 Euro für das 1. Extra-Konto 0,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "date", "note2", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^(?<note1>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) " //
                                                                        + "(?<note2>[\\.,\\d]+%) " //
                                                                        + ".* (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(v.get("note1") + " (" + v.get("note2") + ")");
                                                        }),
                                        // @formatter:off
                                        // 01.01.2022 bis 05.12.2022 keine Zinsen erwirtschaftet 0,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "keine Zinsen erwirtschaftet " //
                                                                        + "(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                        }),
                                        // @formatter:off
                                        // 31.12.2020 Zinsgutschrift 0,02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "date", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(?<note1>Zinsgutschrift) " //
                                                                        + "(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(v.get("note1"));
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        }));

        // @formatter:off
        // 30.12.2016 Kapitalertragsteuer -1,38
        // 30.12.2016 Solidaritätszuschlag -0,07
        // 30.12.2016 Kirchensteuer -0,11
        // @formatter:on
        Block taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Kapitalertrags(s)?teuer" //
                        + "|Solidarit.tszuschlag" //
                        + "|Kirchensteuer) \\-[\\.,\\d]+$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Kapitalertrags(s)?teuer" //
                                        + "|Solidarit.tszuschlag" //
                                        + "|Kirchensteuer) " //
                                        + "\\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 03.05.2023 Entgelt EgumoUc -0,99
        // @formatter:on
        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Entgelt .* \\-[\\.,\\d]+$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Entgelt .* \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(Wertpapierabrechnung zum steuerrelevanten Umtausch" //
                        + "|Umtausch Eingang" //
                        + "|Umtausch Ausgang)");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Wertpapierabrechnung zum steuerrelevanten Umtausch" //
                        + "|Umtausch Eingang" //
                        + "|Umtausch Ausgang)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Ausgang" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Umtausch (?<type>(Eingang|Ausgang))$") //
                        .assign((t, v) -> {
                            if ("Ausgang".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ISIN (WKN) LU1291109293 (A2ACQY)
                                        // Wertpapierbezeichnung BNP P.Easy-ECPI Gl ESG Infra.
                                        // Nam.-Ant.UCITS ETF CAP EUR o.N
                                        // Nominale Stück 4,00
                                        // Kurs EUR 64,7182
                                        // Ausführungstag 03.11.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "nameContinued", "shares", "currency", "date") //
                                                        .match("^ISIN \\(WKN\\) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Nominale St.ck (?<shares>[\\.,\\d]+)$") //
                                                        .match("^Kurs (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .match("^Ausf.hrungstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // 16,0648 Stück Kenvue Inc. 25.08.2023 0021740090
                                        // Registered Shares DL -,001
                                        // ISIN (WKN): US49177J1025 (A3EEHU)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "date", "nameContinued", "isin", "wkn") //
                                                        .match("^(?<shares>[\\.,\\d]+) St.ck (?<name>.*) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^ISIN \\(WKN\\): (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }))

                        // @formatter:off
                        // im Verhältnis 1:8,0324 in die WKN A3EEHU umgetauscht.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^.*im (?<type>Verh.ltnis) .* WKN [A-Z0-9]{6} .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe
                        //     operación  operación de ING bolsa   total
                        //
                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe")
                        .find(".*operación  operación de ING bolsa   total")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Franz. Transaktionssteuer 0,30% EUR 2,52
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Franz\\. Transaktionssteuer [\\.,\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer (Account)
                        // Kapitalertragsteuer 25,00 % EUR 18,32
                        // Kapitalertragsteuer 25,00% EUR 5,91
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalerstragsteuer (Joint Account)
                        // KapSt anteilig 50,00 % 24,45% EUR 79,46
                        // KapSt anteilig 50,00 % 24,45 % EUR 79,46
                        // @formatter:on
                        .section("currency1", "tax1", "currency2", "tax2").optional() //
                        .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$") //
                        .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                            {
                                // Account 1
                                v.put("currency", v.get("currency1"));
                                v.put("tax", v.get("tax1"));
                                processTaxEntries(t, v, type);

                                // Account 2
                                v.put("currency", v.get("currency2"));
                                v.put("tax", v.get("tax2"));
                                processTaxEntries(t, v, type);
                            }
                        })

                        // @formatter:off
                        // Solidaritätszuschlag (Account)
                        // Solidarit‰tszuschlag 5,50 % EUR 1,00
                        // Solidaritätszuschlag 5,50% EUR 0,32
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solitaritätszuschlag (Joint Account)
                        // Solidaritätszuschlag 5,50% EUR 4,37
                        // Solidaritätszuschlag 5,50 % EUR 4,37
                        // @formatter:on
                        .section("currency1", "tax1", "currency2", "tax2").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$") //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                            {
                                // Account 1
                                v.put("currency", v.get("currency1"));
                                v.put("tax", v.get("tax1"));
                                processTaxEntries(t, v, type);

                                // Account 2
                                v.put("currency", v.get("currency2"));
                                v.put("tax", v.get("tax2"));
                                processTaxEntries(t, v, type);
                            }
                        })

                        // @formatter:off
                        // Kirchensteuer (Account)
                        // Kirchensteuer 5,50 % EUR 1,00
                        // Kirchensteuer 5,50% EUR 0,32
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer (Joint Account)
                        // Kirchensteuer 9,00 % EUR 7,15
                        // Kirchensteuer 9,00% EUR 7,15
                        // @formatter:on
                        .section("currency1", "tax1", "currency2", "tax2").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$") //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                            {
                                // Account 1
                                v.put("currency", v.get("currency1"));
                                v.put("tax", v.get("tax1"));
                                processTaxEntries(t, v, type);

                                // Account 2
                                v.put("currency", v.get("currency2"));
                                v.put("tax", v.get("tax2"));
                                processTaxEntries(t, v, type);
                            }
                        })

                        // @formatter:off
                        // QuSt 15,00 % (EUR 8,87) USD 9,31
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^QuSt [\\.,\\d]+([\\s]+)?% \\((?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)\\) [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // QuSt 30,00 % EUR 16,50
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^QuSt [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Retención: 12,02 €
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Retenci.n: (?<withHoldingTax>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Retención en origen: 9,05 €
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Retenci.n en origen: (?<withHoldingTax>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Retención en destino: 9,74 €
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Retenci.n en destino: (?<withHoldingTax>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe
                        //     operación  operación de ING bolsa   total
                        //
                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe")
                        .find(".*operación  operación de ING bolsa   total")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe
                        //     operación  operación de ING bolsa   total
                        //
                        // 67 RED ELECTRICA ES0173093024 M.CONTINUO Compra 15,01 EUR 1.005,67 EUR 4,01 EUR 0,33 EUR 2,01 EUR 1.012,02 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos  Importe")
                        .find(".*operación  operación de ING bolsa   total")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe
                        //     operación  operación de ING bolsa  cambio   total
                        //           de divisa
                        // 30 DIAGEO GB0002374006 LONDRES Compra 25,19 GBP 894,48 EUR 4,47 EUR 4,52 EUR 0,00 EUR 4,47 EUR 907,94 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe")
                        .find(".*operación  operación de ING bolsa  cambio   total")
                        .find(".*de divisa")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe
                        //     operación  operación de ING bolsa  cambio   total
                        //           de divisa
                        // 30 DIAGEO GB0002374006 LONDRES Compra 25,19 GBP 894,48 EUR 4,47 EUR 4,52 EUR 0,00 EUR 4,47 EUR 907,94 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe")
                        .find(".*operación  operación de ING bolsa  cambio   total")
                        .find(".*de divisa")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Nº Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe
                        //     operación  operación de ING bolsa  cambio   total
                        //           de divisa
                        // 30 DIAGEO GB0002374006 LONDRES Compra 25,19 GBP 894,48 EUR 4,47 EUR 4,52 EUR 0,00 EUR 4,47 EUR 907,94 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Título  Valor ISIN Mercado Tipo Precio  Importe Comisión Gastos de Impuestos Comisión  Importe")
                        .find(".*operación  operación de ING bolsa  cambio   total")
                        .find(".*de divisa")
                        .match("^[\\.,\\d]+ .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsplatzgebühr EUR 2,50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Handelsplatzgeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision EUR 9,90
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsentgelt EUR 3,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Handelsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsenentgelt EUR 0,39
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsenentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Variables Transaktionsentgelt EUR 2,82
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Variables Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage EUR 3,20
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Courtage (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kurswert EUR 52,63
                        // Rabatt EUR - 2,63
                        // Der regul.re Ausgabeaufschlag von 5,263% ist im Kurs enthalten.
                        // @formatter:on
                        .section("currency", "amount", "discountCurrency", "discount", "percentageFee").optional() //
                        .match("^Kurswert (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .match("^Rabatt (?<discountCurrency>[\\w]{3}) \\- (?<discount>[\\.,\\d]+)$") //
                        .match("^Der regul.re Ausgabeaufschlag von (?<percentageFee>[\\.,\\d]+)% .*$") //
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                            BigDecimal amount = asBigDecimal(v.get("amount"));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                BigDecimal fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC); //

                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // fee = fee - discount
                                fee = fee.subtract(discount);

                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });
    }
}
package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote DKB Bank has redesigned their PDF documents as of September 2023.
 *
 *           We therefore separate the functions with
 *              XXX_Format01
 *           and
 *              XXX_Format02
 * @formatter:on
 */

@SuppressWarnings("nls")
public class DkbPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isJointAccount";

    BiConsumer<DocumentContext, String[]> isJointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("^Anteilige Berechnungsgrundlage f.r \\([\\d]{2},[\\d]{2}([\\s]+)?%\\).*$");

        for (String line : lines)
        {
            if (pJointAccount.matcher(line).matches())
            {
                context.putBoolean(IS_JOINT_ACCOUNT, true);
                break;
            }
        }
    };

    public DkbPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DKB");
        addBankIdentifier("Deutsche Kreditbank");
        addBankIdentifier("10919 Berlin");

        addBuySellTransaction();
        addDividendeTransaction();
        addTransferOutTransaction();
        addAdvanceTaxTransaction();
        addBuyTransactionFundsSavingsPlan();
        addAccountStatementTransaction_Format01();
        addAccountStatementTransaction_Format02();
        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Kreditbank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf" //
                        + "|Kauf Direkthandel" //
                        + "|Ausgabe" //
                        + "|Ausgabe Investmentfonds" //
                        + "|Verkauf" //
                        + "|Verkauf Direkthandel" //
                        + "|Verkauf aus Kapitalmaßnahme" //
                        + "|R.cknahme Investmentfonds" //
                        + "|Gesamtk.ndigung" //
                        + "|Teilr.ckzahlung mit Nennwert.nderung" //
                        + "|Teilliquidation mit Nennwertreduzierung)", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Wertpapier Abrechnung )?" //
                        + "(Kauf" //
                        + "|Kauf Direkthandel" //
                        + "|Ausgabe" //
                        + "|Ausgabe Investmentfonds" //
                        + "|Verkauf" //
                        + "|Verkauf Direkthandel" //
                        + "|Verkauf aus Kapitalmaßnahme" //
                        + "|R.cknahme Investmentfonds" //
                        + "|Gesamtk.ndigung" //
                        + "|Teilr.ckzahlung mit Nennwert.nderung" //
                        + "|Teilliquidation mit Nennwertreduzierung)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        // Map for tax lost adjustment transaction
        Map<String, String> context = type.getCurrentContext();

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(Wertpapier Abrechnung )?" //
                                        + "(?<type>(Kauf" //
                                        + "|Kauf Direkthandel" //
                                        + "|Ausgabe" //
                                        + "|Ausgabe Investmentfonds" //
                                        + "|Verkauf" //
                                        + "|Verkauf Direkthandel" //
                                        + "|Verkauf aus Kapitalmaßnahme" //
                                        + "|R.cknahme Investmentfonds" + "|Gesamtk.ndigung"
                                        + "|Teilr.ckzahlung mit Nennwert.nderung"
                                        + "|Teilliquidation mit Nennwertreduzierung))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "Verkauf Direkthandel".equals(v.get("type")) //
                                            || "Verkauf aus Kapitalmaßnahme".equals(v.get("type")) //
                                            || "Rücknahme Investmentfonds".equals(v.get("type")) //
                                            || "Gesamtkündigung".equals(v.get("type")) //
                                            || "Teilrückzahlung mit Nennwertänderung".equals(v.get("type"))
                                            || "Teilliquidation mit Nennwertreduzierung".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Storno, da der Ursprungsauftrag mit falscher Entgeltberechnung erfolgte.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Storno), .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 1.000 SEADRILL LTD. BMG7945E1057 (A0ERZ0)
                                        // REGISTERED SHARES DL 2,-
                                        // Ausführungskurs 1,75 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // EUR 2.000,00 8,75 % METALCORP GROUP B.V. DE000A1HLTD2 (A1HLTD)
                                        // EO-ANLEIHE 2013(18)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "nameContinued") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // EUR 2.000,00 8,75 % METALCORP GROUP B.V. DE000A1HLTD2 (A1HLTD)
                        // Stück 29,2893 COMSTAGE-MSCI WORLD TRN U.ETF LU0392494562 (ETF110)
                        // @formatter:on
                        .section("notation", "shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<notation>(St.ck|[\\w]{3})) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
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
                        // Schlusstag/-Zeit 25.11.2015 11:02:54 Zinstermin Monat(e) 27. Juni
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Schlusstag(\\/-Zeit)? .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time"))) //

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag 06.03.2017 Auftraggeber Max Mustermann
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag(\\/-Zeit)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // Den Gegenwert buchen wir mit Valuta 09.07.2020 zu Gunsten des Kontos 1053412345
                                        // Den Betrag buchen wir mit Valuta 31.07.2014 zu Gunsten des Kontos 16765097 (IBAN DE30 1203 0000 0026 6741 97),
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Den (Gegenwert|Betrag) buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Ausmachender Betrag 4.937,19 EUR
                        // Ausmachender Betrag 2.974,39+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs (EUR/CHF) 0,986 vom 10.01.2023
                        // Kurswert 984,79- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer 123456/12.34
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit 1,75 EUR
                        // Rückzahlungskurs 100 % Rückzahlungsdatum 31.07.2014
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Limit|R.ckzahlungskurs) [\\.,\\d]+ ([\\w]{3}|%)).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap((t, ctx) -> {
                            BuySellEntryItem item = new BuySellEntryItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            // @formatter:off
                            // Handshake for tax lost adjustment transaction
                            // Also use number for that is also used to (later) convert it back to a number
                            // @formatter:on
                            context.put("name", item.getSecurity().getName());
                            context.put("isin", item.getSecurity().getIsin());
                            context.put("wkn", item.getSecurity().getWkn());
                            context.put("shares", Long.toString(item.getShares()));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxLostAdjustmentTransaction(context, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift" //
                        + "|Zinsgutschrift" //
                        + "|Gutschrift von Investmenterträgen" //
                        + "|Aussch.ttung aus Genussschein" //
                        + "|Aussch.ttung Investmentfonds" //
                        + "|Ertragsgutschrift nach . 27 KStG" //
                        + "|Gutschrift" //
                        + "|Ertr.gnisgutschrift aus Wertpapieren)", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^10919 Berlin( Seite 1)?$", "^Den Betrag buchen wir mit.*$");
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
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // EUR 10.000,00 PCC SE DE000A1R1AN5 (A1R1AN)
                                        // INH.-TEILSCHULDV. V.13(13/17)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "nameContinued") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                                                        .match("(?<nameContinued>.*)") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 30 DAIMLER AG DE0007100000 (710000)
                                        // NAMENS-AKTIEN O.N.
                                        // Zahlbarkeitstag 07.04.2016 Dividende pro Stück 3,25 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                                                        .match("(?<nameContinued>.*)") //
                                                        .match("^Zahlbarkeitstag .* [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 10,6841 SPDR S&P US DIVID.ARISTOCR.ETF
                                        // REGISTERED SHARES O.N.
                                        // IE00B6YX5D40 (A1JKS0)
                                        // Ertrag pro St. 0,230700000 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("(?<nameContinued>.*)") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                                                        .match("^Ertrag pro St. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // EUR 10.000,00 PCC SE DE000A1R1AN5 (A1R1AN)
                        // @formatter:on
                        .section("notation", "shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<notation>(St.ck|[\\w]{3})) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
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
                        // Den Betrag buchen wir mit Wertstellung 04.01.2016 zu Gunsten des Kontos 12345678 (IBAN DE30 1203 0000 0012 3456
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 144,52+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / CHF 1,1959
                        // Ausschüttung 51,00 CHF 42,65+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^(Aussch.ttung|Dividendengutschrift|Kurswert) (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Abrechnungsnr. 86525618110
                        // Herrn Abrechnungsnr. 12345678901Datum 31.05.2018
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Ex-Tag 09.02.2017 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        // @formatter:off
                        // Kapitalrückzahlung
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Kapitalr.ckzahlung)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTransferOutTransaction()
    {
        DocumentType type = new DocumentType("Depotbuchung \\- Belastung", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Depotnummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                        // EUR 25.000,00 24,75 % UBS AG (LONDON BRANCH) DE000US9RGR9 (US9RGR)
                        // EO-ANL. 14(16) RWE
                        // @formatter:on
                        .section("currency", "shares", "name", "isin", "wkn", "nameContinued") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<currency>[\\w]{3}) (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            // Percentage quotation, workaround for bonds
                            BigDecimal shares = asBigDecimal(v.get("shares"));
                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));

                            t.setAmount(0L);
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        // @formatter:off
                        // Valuta 30.11.2015 externe Referenz-Nr. KP40030120300340
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Auftragsnummer 489130/67.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Depotkonto-Nr.
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Depotkonto\\-Nr\\. .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale Investmentfonds");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnungsnr.*$", "^Keine Steuerbescheinigung\\.$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                        // Stück 49,1102 VANGUARD FTSE ALL-WORLD U.ETF IE00BK5BQT80 (A2PKXG)
                        // REG. SHS USD ACC. ON
                        // Zahlbarkeitstag 04.01.2021 Vorabpauschale pro St. 0,037971560 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(St.ck|[\\w]{3}) (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.* Vorabpauschale pro St\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // EUR 2.000,00 8,75 % METALCORP GROUP B.V. DE000A1HLTD2 (A1HLTD)
                        // Stück 29,2893 COMSTAGE-MSCI WORLD TRN U.ETF LU0392494562 (ETF110)
                        // @formatter:on
                        .section("notation", "shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<notation>St.ck|[\\w]{3}) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
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
                                        // Den Betrag buchen wir mit Wertstellung 06.01.2021 zu Lasten des Kontos 1234567890 (IBAN DE99 9999 9999 9999 9999
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Herrn Datum 15.01.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.* Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag 0,08- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Berechnungsgrundlage für die Kapitalertragsteuer 0,00+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Berechnungsgrundlage f.r die Kapitalertrags(s)?teuer (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Abrechnungsnr. 12345678901
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addBuyTransactionFundsSavingsPlan()
    {
        final DocumentType type = new DocumentType("Halbjahresabrechnung Sparplan", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // COMSTA.-MSCI EM.MKTS.TRN U.ETF LU0635178014 (ETF127)
                                        // @formatter:on
                                        .section("name", "isin", "wkn") //
                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("name", trim(v.get("name")));
                                            ctx.put("isin", v.get("isin"));
                                            ctx.put("wkn", v.get("wkn"));
                                        })

                                        // @formatter:off
                                        // Im Abrechnungszeitraum angelegter Betrag EUR 540,00
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Im Abrechnungszeitraum angelegter Betrag (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Kauf [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Kauf 90,00 531781/77.00 40,1900 1,0000 2,2394 05.07.2018 09.07.2018 0,00 0,00
                                        // + Provision 0,49 Summe 200,49
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "amount", "shares", "date") //
                                                        .documentContext("name", "isin", "wkn", "currency") //
                                                        .match("^Kauf [\\.,\\d]+ (?<note>[\\d]+\\/[\\.\\d]+).* (?<shares>[\\.,\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*$") //
                                                        .match("^\\+ Provision [\\.,\\d]+ Summe (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setDate(asDate(v.get("date")));
                                                            t.setNote("Auftragsnummer " + trim(v.get("note")));

                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Kauf 90,00 531781/77.00 40,1900
                                        // 1,0000 2,2394 05.07.2018 09.07.2018
                                        // 0,00 0,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "amount", "shares", "date") //
                                                        .documentContext("name", "isin", "wkn", "currency") //
                                                        .match("^Kauf (?<amount>[\\.,\\d]+) (?<note>[\\d]+\\/[\\.\\d]+).* (?<shares>[\\.,\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setDate(asDate(v.get("date")));
                                                            t.setNote("Auftragsnummer " + trim(v.get("note")));

                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction_Format01()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nummer", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Bu.Tag Wert Wir haben für Sie gebucht Belastung in EUR Gutschrift in EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Bu\\.Tag Wert Wir haben f.r Sie gebucht Belastung in (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Kontoauszug Nummer 002 / 2021 vom 05.01.2021 bis 04.02.2021
                                        // @formatter:on
                                        .section("nr", "year") //
                                        .match("^Kontoauszug Nummer (?<nr>[\\d]+) \\/ (?<year>[\\d]{4}) vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("nr", v.get("nr").replaceFirst("^0+(?!$)", ""));
                                            ctx.put("year", v.get("year"));
                                        })

                                        // @formatter:off
                                        // Abrechnung 30.12.2015
                                        // @formatter:on
                                        .section("date").optional() //
                                        .match("^Abrechnung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));

        this.addDocumentTyp(type);

        Block interestChargeBlock = new Block("^Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(interestChargeBlock);
        interestChargeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type").optional() //
                        .documentContext("date", "currency") //
                        .match("^(?<note>Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .match("^Zinsen f.r (einger.umte Konto.berziehung|Guthaben) ([\\s]+)?(?<amount>[\\.,\\d]+)(?<type>([\\-|\\+]))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block interestChargeCreditBlock = new Block("^Zinsen f.r Dispositionskredit ([\\s]+)?[\\.,\\d]+([\\-|\\+])$");
        type.addBlock(interestChargeCreditBlock);
        interestChargeCreditBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type") //
                        .documentContext("date", "currency") //
                        .match("^(?<note>Zinsen f.r Dispositionskredit) ([\\s]+)?(?<amount>[\\.,\\d]+)(?<type>([\\-|\\+]))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block taxesBlock = new Block("^(Kapitalertrags(s)?teuer|Solidarit.tszuschlag|Kirchensteuer)[\\s]{1,}[\\.,\\d]+(([\\-|\\+]))$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type") //
                        .documentContext("date", "currency") //
                        .match("^(?<note>(Kapitalertrags(s)?teuer|Solidarit.tszuschlag|Kirchensteuer))[\\s]{1,}(?<amount>[\\.,\\d]+)(?<type>([\\-|\\+]))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "+" change from TAXES to TAX_REFUND
                            // @formatter:on
                            if ("+".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block removalBlock = new Block("^(?i)[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(.berweisung" //
                        + "|Dauerauftrag" //
                        + "|Basislastschrift" //
                        + "|Lastschrift" //
                        + "|Kartenzahlung.*" //
                        + "|Kreditkartenabr\\." //
                        + "|Verf.gung Geldautomat" //
                        + "|Verf.g\\. Geldautom\\. FW" //
                        + "|.berweis\\. entgeltfr\\.) " //
                        + "[\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("month1", "day", "month2", "note", "amount") //
                        .documentContext("nr", "year", "currency") //
                        .match("^(?i)[\\d]{2}\\.(?<month1>[\\d]{2})\\. (?<day>[\\d]{2})\\.(?<month2>[\\d]{2})\\. " //
                                        + "(?<note>(.berweisung" //
                                        + "|Dauerauftrag" //
                                        + "|Basislastschrift" //
                                        + "|Lastschrift" //
                                        + "|Kartenzahlung.*" //
                                        + "|Kreditkartenabr\\." //
                                        + "|Verf.gung Geldautomat" //
                                        + "|Verf.g\\. Geldautom\\. FW" //
                                        + "|.berweis\\. entgeltfr\\.)) " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            dateTranactionHelper(t, v);

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Kreditkartenabr.".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Kreditkartenabrechnung");

                            if ("Verfügung Geldautomat".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Geldautomat");

                            if ("Verfüg. Geldautom. FW".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Geldautomat (Fremdwährung)");

                            if ("Kartenzahlung onl".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Kartenzahlung online");

                            if ("Überweis. entgeltfr.".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Überweisung entgeltfrei");

                            if ("Kartenzahlung FW".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Kartenzahlung (Fremdwährung)");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block depositBlock = new Block("^(?i)[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(Lohn, Gehalt, Rente" //
                        + "|Zahlungseingang" //
                        + "|Storno Gutschrift" //
                        + "|Bareinzahlung am GA" //
                        + "|sonstige Buchung" //
                        + "|Eingang Inst\\.Paym\\." //
                        + "|Eingang Echtzeit.berw) " //
                        + "[\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("month1", "day", "month2", "note", "amount") //
                        .documentContext("nr", "year", "currency") //
                        .match("^(?i)[\\d]{2}\\.(?<month1>[\\d]{2})\\. (?<day>[\\d]{2})\\.(?<month2>[\\d]{2})\\. " //
                                        + "(?<note>(Lohn, Gehalt, Rente" //
                                        + "|Zahlungseingang" //
                                        + "|Storno Gutschrift" //
                                        + "|Bareinzahlung am GA" //
                                        + "|sonstige Buchung" //
                                        + "|Eingang Inst\\.Paym\\." //
                                        + "|Eingang Echtzeit.berw)) " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            dateTranactionHelper(t, v);

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Eingang Echtzeitüberw".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Eingang Echtzeitüberweisung");

                            if ("Bareinzahlung am GA".equalsIgnoreCase(v.get("note")))
                                v.put("note", "Bareinzahlung am Geldautomat");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block taxReturnBlock = new Block("^(?i)[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. [\\d]+ Steuerausgleich [\\.,\\d]+$");
        type.addBlock(taxReturnBlock);
        taxReturnBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("month1", "day", "month2", "note", "amount") //
                        .documentContext("nr", "year", "currency") //
                        .match("^(?i)[\\d]{2}\\.(?<month1>[\\d]{2})\\. (?<day>[\\d]{2})\\.(?<month2>[\\d]{2})\\. [\\d]+ " //
                                        + "(?<note>Steuerausgleich) " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            dateTranactionHelper(t, v);

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block feesBlock = new Block("^(?i)[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(Rechnung" //
                        + "|Buchung" //
                        + "|sonstige Entgelte) " //
                        + "[\\.,\\d]+$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("month1", "day", "month2", "note1", "amount", "note2") //
                        .documentContext("nr", "year", "currency") //
                        .match("^(?i)[\\d]{2}\\.(?<month1>[\\d]{2})\\. (?<day>[\\d]{2})\\.(?<month2>[\\d]{2})\\. " //
                                        + "(?<note1>(Rechnung" //
                                        + "|Buchung" //
                                        + "|sonstige Entgelte)) " //
                                        + "(?<amount>[\\.,\\d]+)$")
                        .match("^(?i).*(?<note2>(Bargeldeinzahlung" //
                                        + "|R.ckruf\\/Nachforschung" //
                                        + "|Identifikationscode" //
                                        + "|Stornorechnung" //
                                        + "|Girokarte" //
                                        + "|Entgelt f.r Konto ohne)).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "Stornorechnung" change from FEES to FEES_REFUND
                            // @formatter:on
                            if ("Stornorechnung".equals(v.get("note2")))
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            dateTranactionHelper(t, v);

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note1") + " " + v.get("note2"));

                            // Formatting some notes
                            if ("BUCHUNG IDENTIFIKATIONSCODE".equalsIgnoreCase(t.getNote()))
                                t.setNote("Buchung Identifikationscode");

                            if ("Rechnung Entgelt für Konto ohne".equalsIgnoreCase(t.getNote()))
                                t.setNote("Entgelt für Konto ohne mtl. Eingang");
                        })

                        .wrap(TransactionItem::new));
    }

    private void addAccountStatementTransaction_Format02()
    {
        final DocumentType type = new DocumentType("Kontoauszug [\\d]{1,2}\\/[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Gesamtumsatzsummen Summe Soll EUR Anzahl Summe Haben EUR Anzahl
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Gesamtumsatzsummen Summe Soll (?<currency>[\\w]{3}) Anzahl Summe Haben [\\w]{3} Anzahl$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // This is for interest charge
                                        //
                                        // 02.10.2023 Abrechnung 29.09.2023 / Wert: 01.10.2023
                                        // 01.07.2024 Abrechnung 28.06.2024
                                        // @formatter:on
                                        .section("date").optional() //
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Abrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));

        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block("^[\\s]+ (\\-)?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.setMaxSize(2);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date", "note").optional() //
                        .documentContext("currency") //
                        .match("^[\\s]+ (?<type>[\\-\\s])(?<amount>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?!(Wertpapierabrechnung|Abrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))" //
                                        + "(?<note>(Lohn, Gehalt, Rente" //
                                        + "|Zahlungseingang" //
                                        + "|Storno Gutschrift" //
                                        + "|Bareinzahlung am GA" //
                                        + "|sonstige Buchung" //
                                        + "|Eingang Inst\\.Paym\\." //
                                        + "|Eingang Echtzeit.berw" //
                                        + "|.berweisung" //
                                        + "|Dauerauftrag" //
                                        + "|Basislastschrift" //
                                        + "|Lastschrift" //
                                        + "|Kartenzahlung.*" //
                                        + "|Kreditkartenabr\\." //
                                        + "|Verf.gung Geldautomat" //
                                        + "|Verf.g\\. Geldautom\\. FW" //
                                        + "|.berweis\\. entgeltfr\\.)).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Kreditkartenabr.".equals(v.get("note")))
                                v.put("note", "Kreditkartenabrechnung");

                            if ("Verfügung Geldautomat".equals(v.get("note")))
                                v.put("note", "Geldautomat");

                            if ("Verfüg. Geldautom. FW".equals(v.get("note")))
                                v.put("note", "Geldautomat (Fremdwährung)");

                            if ("Kartenzahlung onl".equals(v.get("note")))
                                v.put("note", "Kartenzahlung online");

                            if ("Überweis. entgeltfr.".equals(v.get("note")))
                                v.put("note", "Überweisung entgeltfrei");

                            if ("Kartenzahlung FW".equals(v.get("note")))
                                v.put("note", "Kartenzahlung (Fremdwährung)");

                            if ("Eingang Echtzeitüberw".equals(v.get("note")))
                                v.put("note", "Eingang Echtzeitüberweisung");

                            if ("Bareinzahlung am GA".equals(v.get("note")))
                                v.put("note", "Bareinzahlung am Geldautomat");

                            t.setNote(v.get("note"));
                        })

                        .wrap((t) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getDateTime() != null)
                                return item;

                            return null;
                        }));

        Block feesBlock = new Block("^[\\s]+ (\\-)?[\\.,\\d]+$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(3);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date", "note1", "note2").optional() //
                        .documentContext("currency") //
                        .match("^[\\s]+ (?<type>[\\-\\s])(?<amount>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?!(Wertpapierabrechnung|Abrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))" //
                                        + "(?<note1>(Rechnung" //
                                        + "|Buchung" //
                                        + "|sonstige Entgelte)).*$") //
                        .match("^.* (?<note2>(Bargeldeinzahlung" //
                                        + "|R.ckruf\\/Nachforschung" //
                                        + "|Identifikationscode" //
                                        + "|Stornorechnung" //
                                        + "|Girokarte" //
                                        + "|Entgelt f.r Konto ohne)).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from FEES_REFUND to FEES
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note1") + " " + v.get("note2"));

                            // Formatting some notes
                            if ("Rechnung Entgelt für Konto ohne".equals(t.getNote()))
                                t.setNote("Entgelt für Konto ohne mtl. Eingang");
                        })

                        .wrap((t) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getDateTime() != null)
                                return item;

                            return null;
                        }));

        // @formatter:off
        // Abrechnungszeitraum vom 01.07.2023 bis 30.09.2023
        // Zinsen für eingeräumte Kontoüberziehung                              0,01-
        // @formatter:on
        Block interestChargeBlock = new Block("^Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(interestChargeBlock);
        interestChargeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type").optional() //
                        .documentContext("currency", "date") //
                        .match("^(?<note>Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .match("^Zinsen f.r (einger.umte Konto.berziehung|Guthaben) ([\\s]+)?(?<amount>[\\.,\\d]+)(?<type>([\\-|\\+]))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block taxesBlock = new Block("^(Kapitalertrags(s)?teuer|Solidarit.tszuschlag|Kirchensteuer)[\\s]{1,}[\\.,\\d]+(([\\-|\\+]))$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("note", "amount", "type") //
                        .documentContext("date", "currency") //
                        .match("^(?<note>(Kapitalertrags(s)?teuer|Solidarit.tszuschlag|Kirchensteuer))[\\s]{1,}(?<amount>[\\.,\\d]+)(?<type>([\\-|\\+]))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "+" change from TAXES to TAX_REFUND
                            // @formatter:on
                            if ("+".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addCreditcardStatementTransaction()
    {
        final DocumentType type = new DocumentType("Ihre Abrechnung vom ", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // DKB-VISA-Card beträgt 100 EUR. Soweit auf dem Umsatzsteuernummer: DE137178746
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^DKB\\-VISA\\-Card betr.gt [\\.,\\d]+ (?<currency>[\\w]{3})\\. .*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}(?! Habenzins).* [\\.,\\d]+\\+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                                                        + "(?<note>Ausgleich Kreditkarte gem\\. Abrechnung) v\\. " //
                                                                        + "(?<amount>[\\.,\\d]+)\\+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})" //
                                                                        + "(?<note>(?! Habenzins).*) " //
                                                                        + "[\\w]{3} [\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<amount>[\\.,\\d]+)\\+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));

                                                            // @formatter:off
                                                            // Deletes characters that occur during withdrawals from foreign banks
                                                            // @formatter:on
                                                            if (t.getNote().startsWith("*"))
                                                                t.setNote(trim(t.getNote().substring(1)));

                                                            if (t.getNote().endsWith(">"))
                                                                t.setNote(trim(t.getNote().substring(0, t.getNote().length() - 1)));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})" //
                                                                        + "(?<note>(?! Habenzins).*) " //
                                                                        + "(?<amount>[\\.,\\d]+)\\+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));

                                                            // @formatter:off
                                                            // Deletes characters that occur during withdrawals from foreign banks
                                                            // @formatter:on
                                                            if (t.getNote().startsWith("*"))
                                                                t.setNote(trim(t.getNote().substring(1)));

                                                            if (t.getNote().endsWith(">"))
                                                                t.setNote(trim(t.getNote().substring(0, t.getNote().length() - 1)));
                                                        }))

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} Habenzins auf [\\d]+ Tage [\\.,\\d]+\\+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                        + "(?<note>Habenzins auf [\\d]+ Tage) " //
                                        + "(?<amount>[\\.,\\d]+)\\+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) Abgeltungsteuer [\\.,\\d]+ \\-$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                        + "(?<note>Abgeltungsteuer) " //
                                        + "(?<amount>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}(?! (Abgeltungsteuer|Kartenpreis|PIN\\-Geb.hr)).* [\\.,\\d]+ \\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .oneOf( //

                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})" //
                                                                        + "(?<note>(?! (Abgeltungsteuer|Kartenpreis|PIN\\-Geb.hr)).*) " //
                                                                        + "[\\w]{3} [\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<amount>[\\.,\\d]+) \\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));

                                                        // @formatter:off
                                            // Deletes characters that occur during withdrawals from foreign banks
                                            // @formatter:on
                                                            if (t.getNote().startsWith("*"))
                                                                t.setNote(trim(t.getNote().substring(1)));

                                                            if (t.getNote().endsWith(">"))
                                                                t.setNote(trim(t.getNote().substring(0,
                                                                                t.getNote().length() - 1)));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})" //
                                                                        + "(?<note>(?! (Abgeltungsteuer|Kartenpreis|PIN\\-Geb.hr)).*) " //
                                                                        + "(?<amount>[\\.,\\d]+) \\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));

                                                            // @formatter:off
                                                            // Deletes characters that occur during withdrawals from foreign banks
                                                            // @formatter:on
                                                            if (t.getNote().startsWith("*"))
                                                                t.setNote(trim(t.getNote().substring(1)));

                                                            if (t.getNote().endsWith(">"))
                                                                t.setNote(trim(t.getNote().substring(0, t.getNote().length() - 1)));
                                                        }))

                        .wrap(TransactionItem::new));

        Block feeBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (Kartenpreis|PIN\\-Geb.hr) [\\.,\\d]+ \\-$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                        + "(?<note>(Kartenpreis|PIN\\-Geb.hr)) (?<amount>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addTaxLostAdjustmentTransaction(Map<String, String> context, DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Ausmachender Betrag 56,57 EUR
                        // Den Gegenwert buchen wir mit Valuta 27.10.2015 zu Gunsten des Kontos 12345678
                        // @formatter:on
                        .section("amount", "currency", "date").optional() //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(Long.parseLong(context.get("shares")));
                            t.setSecurity(getOrCreateSecurity(context));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer (Account)
                        // Kapitalertragsteuer 24,45% auf 1.718,79 EUR 420,24- EUR
                        // Kapitalertragsteuer 24,45 % auf 131,25 EUR 32,09- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalerstragsteuer (Joint Account)
                        // Kapitalertragsteuer 24,45% auf 1.718,79 EUR 420,24- EUR
                        // Kapitalertragsteuer 24,45 % auf 131,25 EUR 32,09- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Solidaritätszuschlag 5,50% auf 420,24 EUR 23,11- EUR
                        // Solidaritätszuschlag 5,5 % auf 32,09 EUR 1,76- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag (Joint Account)
                        // Solidaritätszuschlag 5,50% auf 420,24 EUR 23,11- EUR
                        // Solidaritätszuschlag 5,5 % auf 32,09 EUR 1,76- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Kirchensteuer 9,00% auf 420,24 EUR 37,82- EUR
                        // Kirchensteuer 9 % auf 32,09 EUR 2,88- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer (Joint Account)
                        // Kirchensteuer 9,00% auf 420,24 EUR 37,82- EUR
                        // Kirchensteuer 9 % auf 32,09 EUR 2,88- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Finanztransaktionssteuer 5,71- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Finanztransaktionssteuer (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 35 % auf 51,00 CHF 14,93- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 42,65 EUR 6,40 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 7,50- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // + Provision 0,49 Summe 200,49
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^\\+ Provision (?<fee>[\\.,\\d]+) Summe [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            v.put("currency", context.get("currency"));

                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Transaktionsentgelt Börse 0,71- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,20- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Abwicklungsgebühr für die Umschreibung von Namensaktien 0,60- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Abwicklungsgeb.hr .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Abwicklungskosten Börse 0,06- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Abwicklungskosten B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Maklercourtage 0,0800 % vom Kurswert 1,67- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Maklercourtage .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen 20,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Auslagen 9,89- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Auslagen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    /**
     * Helper method to set the date of an AccountTransaction based on the provided ParsedData.
     *
     * This method checks if the transaction's "nr" field is "1" and if the months "month1" and "month2" are different.
     * If both conditions are met, it assumes the transaction should be recorded in the previous year.
     * Otherwise, it uses the year provided in the ParsedData. The final date is set using "day", "month2", and the determined year.
     *
     * @param t The AccountTransaction object to set the date for.
     * @param v The ParsedData object containing the date information. It should provide "nr", "day", "month1", "month2", and "year".
     */
    private void dateTranactionHelper(AccountTransaction t, ParsedData v)
    {
        final String SPECIAL_NR = "1";

        String nr = v.get("nr");
        int month1 = Integer.parseInt(v.get("month1"));
        int month2 = Integer.parseInt(v.get("month2"));
        int year = Integer.parseInt(v.get("year"));

        if (nr.compareTo(SPECIAL_NR) == 0 && month1 != month2)
        {
            year--;
        }

        t.setDateTime(asDate(v.get("day") + "." + v.get("month2") + "." + year));
    }
}

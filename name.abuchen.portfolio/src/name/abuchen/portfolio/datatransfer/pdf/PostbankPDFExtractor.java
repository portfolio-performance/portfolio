package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

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
public class PostbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isJointAccount";

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("Anteilige Berechnungsgrundlage .* \\([\\d]{2},[\\d]{2} %\\).*");

        for (String line : lines)
        {
            if (pJointAccount.matcher(line).matches())
            {
                context.putBoolean(IS_JOINT_ACCOUNT, true);
                break;
            }
        }
    };

    public PostbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Postbank");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Postbank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Wertpapier )?Abrechnung(:)? "//
                        + "(Kauf" //
                        + "|Kauf von Wertpapieren" //
                        + "|Verkauf" //
                        + "|Verkauf\\-Festpreisgesch.ft" //
                        + "|Ausgabe Investmentfonds" //
                        + "|R.cknahme Investmentfonds)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Postbank(?! \\- ).*$");
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
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf|Ausgabe Investmentfonds|R.cknahme Investmentfonds)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Rücknahme Investmentfonds".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Stück 158 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                                        // REGISTERED SHARES 1C O.N.
                                        // Ausführungskurs 62,821 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 123 4567890 00 DWS ESG TOP ASIEN INHABER-ANTEILE LC 1/1
                                        // WKN 976976 Nominal ST 88
                                        // ISIN DE0009769760 Kurs EUR 196,05
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\d]{3} [\\d]+ [\\d]+ (?<name>.*) [\\d]\\/[\\d]$") //
                                                        .match("^WKN (?<wkn>[A-Z0-9]{6}) .*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Kurs (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Stück 158 XTR.(IE) - MSCI WORLD IE00BJ0KDQ92 (A1XB5U)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("notation", "shares") //
                                                        .match("^(?<notation>(St.ck|[\\w]{3})) (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
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
                                                        }),
                                        // @formatter:off
                                        // WKN 976976 Nominal ST 88
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^WKN [A-Z0-9]{6} .* ST (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Belegnummer 1481234555 / 987123 Schlusstag 24.07.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.*Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Schlusstag/-Zeit 04.02.2020 08:00:04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Belegnummer 1222222222 / 22212322 Schlusstag/-zeit MEZ 23.07.2024 09:04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^.*Schlusstag\\/\\-zeit ... (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Den Gegenwert buchen wir mit Valuta 14.01.2020 zu Gunsten des Kontos 012345678
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Den Gegenwert buchen wir mit Valuta ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf(
                                        // @formatter:off
                                        // Ausmachender Betrag 9.978,18- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Buchung auf Kontonummer 4567890 00 mit Wertstellung 26.07.2023 EUR 17.169,09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Buchung auf Kontonummer [\\s\\d]+ mit Wertstellung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Abrech.-Preis 196,00 USD
                        // Devisenkurs (EUR/USD) 1,06386 vom 12.12.2016
                        // Kurswert 1.289,64 EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        //  Auftragsnummer 69123/22.01
                                        // @formatter:on
                                        section -> section
                                                        .attributes("note")
                                                        .match("^.*(?<note>Auftragsnummer [\\d]+\\/[\\.\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Belegnummer 1481234555 / 987123 Schlusstag 24.07.2023
                                        // @formatter:on
                                        section -> section
                                                        .attributes("note")
                                                        .match("^(?<note>Belegnummer [\\d]+([\\s])?\\/([\\s])?[\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        // @formatter:off
                        // Limit 43,00 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        // @formatter:off
                        // Barabfindung wegen Fusion
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Barabfindung wegen .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift" //
                        + "|Aussch.ttung Investmentfonds" //
                        + "|Gutschrift von Investmentertr.gen" //
                        + "|Ertragsgutschrift" //
                        + "|Zinsgutschrift" //
                        + "|Kupongutschrift)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Postbank(?! \\- ).*$");
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
                                        // Stück 12 JOHNSON & JOHNSON SHARES US4781601046 (853260)
                                        // REGISTERED SHARES DL 1
                                        // Zahlbarkeitstag 14.01.2022 Ausschüttung pro St. 1,390000000 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("(?<name1>.*)") //
                                                        .match("^.* (Aussch.ttung|Dividende|Ertrag) ([\\s]+)?pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // EUR 15.000,00 ENEL FINANCE INTL N.V. XS0177089298 (908043)
                                        // EO-MEDIUM-TERM NOTES 2003(23)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "name1") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("(?<name1>.*)") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 16.000,000000 EUR A0JCCZ XS1014610254
                                        // 2,625% VOLKSWAGEN LEASING MTN.V.14 15.1. 24
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "wkn", "isin", "name") //
                                                        .match("^[\\.,\\d]+ (?<currency>[\\w]{3}) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))


                        .oneOf( //
                                        // @formatter:off
                                        // Stück 12 JOHNSON & JOHNSON SHARES
                                        // US4781601046 (853260)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // EUR 15.000,00 ENEL FINANCE INTL N.V.
                                        // XS0177089298 (908043)
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // 16.000,000000 EUR A0JCCZ XS1014610254
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) [\\w]{3} [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Zahlbarkeitstag 08.04.2021 Ertrag  pro Stück 0,60 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Gutschrift mit Wert 16.01.2023 309,23 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Gutschrift mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag 8,64+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>\\w{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Wir überweisen den Betrag von 309,23 EUR auf Ihr Konto 2222222 00.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Wir überweisen den Betrag von (?<amount>[\\.,\\d]+) (?<currency>\\w{3}) .*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs EUR / USD 1,1920
                                        // Dividendengutschrift 12,12 USD 10,17+ EUR
                                        //
                                        // Devisenkurs EUR / USD  0,9997
                                        // Zinsertrag 166,25 USD 166,30+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .match("^(Dividendengutschrift|Aussch.ttung|Zinsertrag) (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}.*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag 312,50 USD 285,83 EUR
                                        // Umrechnungskurs USD zu EUR 1,0933000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Umrechnungskurs (?<termCurrency>[\\w]{3}) zu (?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        //  Abrechnungsnr. 12345678999
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs EUR / USD 1,1920
                                        // Dividendengutschrift 12,12 USD 10,17+ EUR
                                        //
                                        // Devisenkurs EUR / USD  0,9997
                                        // Zinsertrag 166,25 USD 166,30+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* Art der Dividende (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Bruttoertrag 312,50 USD 285,83 EUR
                                        // Umrechnungskurs USD zu EUR 1,0933000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* (?<note>Zinsschein .*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Postbank(?! \\- ).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück WKN ISIN
                        // 88,000000 976976 DE0009769760
                        // DWS ESG TOP ASIEN INHABER-ANTEILE LC
                        // Vorabpauschale Jahreswert (p.St.) 3,2556615000 EUR Kalenderjahr 2023
                        // @formatter:on
                        .section("wkn", "isin", "name", "currency") //
                        .find("St.ck WKN ISIN")
                        .match("^[\\.,\\d]+ (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("(?<name>.*)") //
                        .match("^Vorabpauschale Jahreswert .* [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück WKN ISIN
                        // 88,000000 976976 DE0009769760
                        // @formatter:on
                        .section("shares") //
                        .find("St.ck WKN ISIN")
                        .match("^(?<shares>[\\.,\\d]+) [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Belastung mit Wert 02.01.2024 26,41 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^Belastung mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Belastung mit Wert 02.01.2024 26,41 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Belastung mit Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addDepotStatementTransaction()
    {
        var currencyAndYear = "^[\\d]{3} (?<year>[\\d]{4}) [\\d] [\\d]+ [A-Z]{2}(?:[\\s]?[0-9]){18,20} (?<baseCurrency>[\\w]{3}) ([\\-|\\+])? [\\.,\\d]+$";

        final DocumentType type = new DocumentType("Kontoauszug", new Block(currencyAndYear)
                        .asRange(section -> section.attributes("baseCurrency", "year").match(currencyAndYear)));
        this.addDocumentTyp(type);

        // @formatter:off
        // 01.08./01.08. D Gut SEPA + 2,70
        // 01.08./01.08. Gutschr.SEPA + 600,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.\\/[\\d]{2}\\.[\\d]{2}\\. (D Gut SEPA|Gutschr\\.SEPA) \\+ [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("date", "note", "amount").optional() //
                        .documentRange("baseCurrency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)\\/[\\d]{2}\\.[\\d]{2}\\. (?<note>(D Gut SEPA|Gutschr\\.SEPA)) \\+ (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(v.get("baseCurrency")));
                            t.setAmount(asAmount(v.get("amount")));

                            // Formatting some notes
                            if ("D Gut SEPA".equals(v.get("note")))
                                v.put("note", "Dauerauftrag");

                            // Formatting some notes
                            if ("Gutschr.SEPA".equals(v.get("note")))
                                v.put("note", "SEPA Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // 06.08./07.08. SEPA Überw. Einzel - 250,00
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.\\/[\\d]{2}\\.[\\d]{2}\\. SEPA Überw\\. Einzel \\- [\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.REMOVAL);
                            return t;
                        })

                        .section("date", "note", "amount").optional() //
                        .documentRange("baseCurrency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)\\/[\\d]{2}\\.[\\d]{2}\\. (?<note>SEPA Überw\\. Einzel) \\- (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(v.get("baseCurrency")));
                            t.setAmount(asAmount(v.get("amount")));

                            // Formatting some notes
                            if ("SEPA Überw. Einzel".equals(v.get("note")))
                                v.put("note", "SEPA Überweisungslastschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 12,12 USD 1,53- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 10,17 EUR 1,53 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer pro Stück 0,0144878 EUR 0,29 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer pro St.ck [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Kapitalertragsteuer (Account)
                        // Kapitalertragsteuer 24,51% auf 0,71 EUR 0,17- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalerstragsteuer (Joint Account)
                        // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                        // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Kapitalertragsteuer (KESt) - 105,00 EUR
                        // Kapitalertragsteuer (KESt) - 78,13 USD - 71,46 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer \\(KESt\\).* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag (Account)
                        // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solitaritätszuschlag (Joint Account)
                        // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                        // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Solidaritätszuschlag auf KESt - 5,77 EUR
                        // Solidaritätszuschlag auf KESt - 4,30 USD - 3,93 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf KESt.* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer (Account)
                        // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean(IS_JOINT_ACCOUNT))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer (Joint Account)
                        // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                        // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                        // @formatter:on
                        .section("tax1", "currency1", "tax2", "currency2").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$") //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$") //
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
                        // Kirchensteuer auf KESt - 5,77 EUR
                        // Kirchensteuer auf KESt - 4,30 USD - 3,93 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf KESt.* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 39,95- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision EUR 39,95
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // XETRA-Kosten EUR 0,60
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^XETRA\\-Kosten (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Abwicklungskosten Börse 0,04- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Abwicklungskosten B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 11,82- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,65- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Auslagen 16,86- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Auslagen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Ausgabeaufschlag 4,00 %
                        // Kurswert EUR 17.252,30
                        // Preisnachlass EUR -82,94
                        // @formatter:on
                        .section("percentageFee", "currency", "amount", "discountCurrency", "discount").optional() //
                        .match("^Ausgabeaufschlag (?<percentageFee>[\\.,\\d]+) %$") //
                        .match("^Kurswert (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .match("^Preisnachlass (?<discountCurrency>[\\w]{3}) \\-(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                            BigDecimal amount = asBigDecimal(v.get("amount"));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                BigDecimal fxFee = amount.divide(percentageFee //
                                                .divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE), Values.MC)
                                                .multiply(percentageFee, Values.MC);

                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // @formatter:off
                                // fee = fee - discount
                                // @formatter:on
                                fee = fee.subtract(discount);

                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });
    }
}

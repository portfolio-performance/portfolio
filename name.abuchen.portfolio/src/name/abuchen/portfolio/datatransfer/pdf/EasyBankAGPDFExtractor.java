package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
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
public class EasyBankAGPDFExtractor extends AbstractPDFExtractor
{
    public EasyBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("easybank Service Center");
        addBankIdentifier("Ihre easybank AG");

        addBuySellTransaction();
        addDividendTransaction();
        addTaxesLostAdjustmentTransaction();
        addDepotStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Easybank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: (Kauf" //
                        + "|Kauf aus Dauerauftrag" //
                        + "|Verkauf" //
                        + "|Tilgung" //
                        + "|Spitze Verkauf)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Tilgung" change from BUY to SELL
                        // Is type --> "Spitze Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Gesch.ftsart: (?<type>(Kauf" //
                                        + "|Kauf aus Dauerauftrag" //
                                        + "|Verkauf" //
                                        + "|Tilgung" //
                                        + "|Spitze Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "Tilgung".equals(v.get("type")) //
                                            || "Spitze Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: DE000A0F5UK5  i S h . S T . E u . 6 00 Bas.Res.U.ETF DE
                                        // Inhaber-Anlageaktien
                                        // Kurs: 66,88 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\-\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000A1R0RZ5 Ekosem-Agrar AG
                                        // Inh.-Schv. v.2012(2020/2027)
                                        // Kurswert: 404,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurswert: [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: AT0000A0VRQ6 Oesterreich, Republik
                                        // Bundesanleihe 2012-2044/4
                                        // Stückzinsen für 6 Tage: -1,55 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^St.ckzinsen f.r [\\d]+ Tage: [\\-\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelszeit: 13.6.2022 um 09:04:00 Uhr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag/-zeit: 05.07.2021 09:04:16
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag\\/\\-zeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handelszeit: 11.11.2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$")
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // endfällig per 14.7.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^endf.llig per (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$")
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zugang: 6,94 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Zugang: Stk 100
                                        // @formatter:on
                                        section -> section.attributes("shares") //
                                                        .match("^(Zugang|Abgang): Stk (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Zugang: - Teilausführung Stk 1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): \\- Teilausf.hrung Stk (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Abgang: Nom. 2.000
                                        // Kurs: 20,2 %
                                        //
                                        // Abgang: Nom. 2.000
                                        // Kurs: Kurs: 100%
                                        //
                                        // Zugang: Nom. 3.000
                                        // Kurs: 96,45 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Abgang|Zugang): Nom\\. (?<shares>[\\.,\\d]+)$") //
                                                        .find("Kurs: [\\.,\\d]+[\\s]*%$") //
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                        var shares = asBigDecimal(v.get("shares"));
                                                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Zu Lasten IBAN AT00 0000 0000 0000 0000 -468,43 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Zu Lasten|Zu Gunsten) .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Kurswert: -579,25 USD
                                        // Devisenkurs: 1,0745 (20.02.2020) -551,42 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency") //
                                                        .match("^Kurswert: (\\-)?(?<gross>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ (?<termCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Auftrags-Nr.: 00000000-3.5.2017
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Auftrags-Nr\\.:.*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Geschäftsart: Verkauf Auftrags-Nr.: 25866072 - 04.01.2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* (?<note>Auftrags-Nr\\.:.*) \\-.*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        //  Limit: Stoplimit: 2.725,000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Limit: (?<note>.*:.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | "))),
                                        // @formatter:off
                                        // Limit: 42,500000
                                        // @formatter:on
                                        section -> section.attributes("note").match("^(?<note>Limit:.*)$")
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | "))))

                        // @formatter:off
                        // Stückzinsen für 6 Tage: -1,55 EUR
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>St.ckzinsen .* [\\d]+ Tage:) \\-(?<note2>[\\.,\\d]+) (?<note3>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note2"), " "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: (Dividende|Ertrag|Ertrag \\- STORNO)(?!\\/Steueranteil)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} (unten angef.hrtes Gesch.ft abgerechnet:|das Gesch.ft mit der Ausf.hrungsnummer).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Geschäftsart: Ertrag - STORNO
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Gesch.ftsart: (?<type>Ertrag \\- STORNO).*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: AT0000APOST4  O E S T E R R E I C H ISCHE POST AG
                                        // AKTIEN O.N.
                                        // Dividende: 1,9 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Dividende") || !v.get("name1").startsWith("Ertrag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000A14J587  t h y s s e n k r u p p AG
                                        // Medium Term Notes v.15(25)
                                        // Kup. 25.2.2023/GZJ Endfälligkeit 25.2.2025
                                        // Zinsertrag für 365 Tage: 50,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zinsertrag f.r [\\d]+ Tage: [\\-\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Bestand: Stk 1.200
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Bestand: Stk (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 100 Stk
                                        // Titel: DE0008404005  A l l i a n z  S E
                                        //
                                        // 2.000 EUR
                                        // Titel: DE000A14J587  t h y s s e n k r u p p
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "notation") //
                                                        .match("^(?<shares>[\\.,\\d]+) (?<notation>(Stk|[A-Z]{3})).*$") //
                                                        .find("Titel:.*") //
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                            if (v.get("notation") != null && !"Stk".equalsIgnoreCase(v.get("notation")))
                                                            {
                                                                var shares = asBigDecimal(v.get("shares"));
                                                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                            }
                                                            else
                                                            {
                                                                t.setShares(asShares(v.get("shares")));
                                                            }
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Valuta 5.5.2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT11 1111 1111 1111 1111 Valuta 07.06.2022 478,50 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu Gunsten .* Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zu Lasten IBAN dp14 7923 7170 5086 2037 Valuta 04.01.2021 -7,38 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu Lasten .* Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT11 1111 1111 1111 1111 Valuta 07.06.2022 478,50 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Zu Lasten IBAN dp14 7923 7170 5086 2037 Valuta 04.01.2021 -7,38 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Ertrag: 0,279 USD
                                        // Bruttoertrag: 336,16 USD
                                        // Devisenkurs: 1,06145 (11.5.2022) 316,70 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<termCurrency>[A-Z]{3}).*$") //
                                                        .match("^Bruttoertrag: (?<fxGross>[\\-\\.,\\d]+) [A-Z]{3}.*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Dividende: 1 USD
                                        // Ertrag: 16,00 USD
                                        // Devisenkurs: 0,99975 (31.10.2022) 11,60 EUR
                                        //
                                        // Ertrag: 0,2885 USD
                                        // Ertrag: 9,18 USD
                                        // Devisenkurs: 1,0684 (12.11.2024) 6,23 EUR
                                        //
                                        // Ertrag: -12,60 USD
                                        // Ertrag: 9,18 USD
                                        // Devisenkurs: 1,0684 (12.11.2024) 6,23 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<termCurrency>[A-Z]{3}).*$") //
                                                        .match("^Ertrag: (?<fxGross>[\\.,\\d]+) [A-Z]{3}.*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Ertrag: -12,60 USD
                                        // Devisenkurs: 1,23235 (31.12.2020) -7,41 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency") //
                                                        .match("^Ertrag: \\-(?<fxGross>[\\.,\\d]+) (?<termCurrency>[A-Z]{3}).*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Zinssatz: 2,5 % von 25.2.2022 bis 24.2.2023
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zinssatz: (?<note>.* [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} .* [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesLostAdjustmentTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: (Steuerkorrektur" //
                        + "|(Dividende" //
                        + "|Ertrag)(\\/Steueranteil)?" //
                        + "|Verkauf" //
                        + "|Tilgung" //
                        + "|Spitze Verkauf)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: AT0000APOST4  O E S T E R R E I C H ISCHE POST AG
                                        // AKTIEN O.N.
                                        // Dividende: 1,9 EUR
                                        //
                                        // Titel: AT0000A3EPA4 ams-OSRAM AG
                                        // Inhaber-Aktien o.N.
                                        // Kurs: 9,9 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Dividende|Ertrag|Kurs): [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Dividende") || !v.get("name1").startsWith("Ertrag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: IE00BF4RFH31 iShsIII-MSCI Wld Sm.Ca.UCI.ETF
                                        // Registered Shares USD(Acc)o.N.
                                        // Ertrag/Steueranteil pro Stk.: 0,011287 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Dividende|Ertrag)\\/Steueranteil pro Stk\\.: [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Dividende") || !v.get("name1").startsWith("Ertrag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000A14J587  t h y s s e n k r u p p AG
                                        // Medium Term Notes v.15(25)
                                        // Kup. 25.2.2023/GZJ Endfälligkeit 25.2.2025
                                        // Zinsertrag für 365 Tage: 50,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zinsertrag f.r [\\d]+ Tage: [\\-\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000A0F5UK5  i S h . S T . E u . 6 00 Bas.Res.U.ETF DE
                                        // Inhaber-Anlageaktien
                                        // Kurs: 66,88 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\-\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000A1R0RZ5 Ekosem-Agrar AG
                                        // Inh.-Schv. v.2012(2020/2027)
                                        // Kurswert: 404,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurswert: [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Bestand: Stk 1.200
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Bestand: Stk (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 100 Stk
                                        // Titel: DE0008404005  A l l i a n z  S E
                                        //
                                        // 2.000 EUR
                                        // Titel: DE000A14J587  t h y s s e n k r u p p
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "notation") //
                                                        .match("^(?<shares>[\\.,\\d]+) (?<notation>(Stk|[A-Z]{3})).*$") //
                                                        .find("Titel:.*")
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                            if (v.get("notation") != null && !"Stk".equalsIgnoreCase(v.get("notation")))
                                                            {
                                                                var shares = asBigDecimal(v.get("shares"));
                                                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                            }
                                                            else
                                                            {
                                                                t.setShares(asShares(v.get("shares")));
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Zugang: 6,94 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Zugang: Stk 100
                                        // @formatter:on
                                        section -> section.attributes("shares") //
                                                        .match("^(Zugang|Abgang): Stk (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Abgang: Nom. 2.000
                                        // Kurs: 20,2 %
                                        // Abgang: Nom. 2.000
                                        // Kurs: Kurs: 100%
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Abgang: Nom. (?<shares>[\\.,\\d]+)$") //
                                                        .find("Kurs: [\\.,\\d]+[\\s]*%") //
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                        var shares = asBigDecimal(v.get("shares"));
                                                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Valuta 5.5.2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT11 1111 1111 1111 1111 Valuta 07.06.2022 478,50 EUR
                                        // Zu Lasten IBAN oF94 4801 5892 6067 2199 Valuta 31.12.2024 -1,76 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu (Gunsten|Lasten) .* Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Steuerkorrektur KESt aus Neubestand: -1,76 EUR
                                        // Zu Lasten IBAN oF94 4801 5892 6067 2199 Valuta 31.12.2024 -1,76 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Steuerkorrektur KESt aus Neubestand: \\-[\\.,\\d]+ [A-Z]{3}.*") //
                                                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAXES);

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // KESt aus Neubestand: -2,02 EUR
                                        // Auslands-KESt neu: -36,22 EUR
                                        // Zu Lasten IBAN dy78 6263 9990 0993 9533 Valuta 30.07.2025 -38,24 EUR
                                        // KESt-Gutschrift 7,48 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency", "taxRefund", "taxRefundCurrency") //
                                                        .find("KESt aus Neubestand: \\-[\\.,\\d]+ [A-Z]{3}.*") //
                                                        .find("Auslands\\-KESt neu: \\-[\\.,\\d]+ [A-Z]{3}.*") //
                                                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .match("^KESt\\-Gutschrift (?<taxRefund>[\\.,\\d]+) (?<taxRefundCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            var taxRefund = Money.of(asCurrencyCode(v.get("taxRefundCurrency")), asAmount(v.get("taxRefund")));

                                                            t.setType(AccountTransaction.Type.TAXES);

                                                            // Subtract tax refund from amount
                                                            t.setMonetaryAmount(amount.subtract(taxRefund));
                                                        }),
                                        // @formatter:off
                                        // KESt aus Neubestand: -8,81 USD
                                        // Auslands-KESt neu: -15,14 USD
                                        // Zu Lasten IBAN gW23 6257 9138 2168 9133 Valuta 10.01.2025 -23,11 E
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("KESt aus Neubestand: \\-[\\.,\\d]+ [A-Z]{3}.*") //
                                                        .find("Auslands\\-KESt neu: \\-[\\.,\\d]+ [A-Z]{3}.*") //
                                                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAXES);

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Gutschrift aus Verlustausgleich: 2,36 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gutschrift aus Verlustausgleich: (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // KESt-Gutschrift 109,69 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^KESt\\-Gutschrift (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Ertrag: 0,2885 USD
                                        // Ertrag: 9,18 USD
                                        // Devisenkurs: 1,0684 (12.11.2024) 6,23 EUR
                                        // Gutschrift aus Verlustausgleich: 2,36 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "exchangeRate", "gross", "baseCurrency") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<termCurrency>[A-Z]{3}).*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ [A-Z]{3}.*$") //
                                                        .match("^Gutschrift aus Verlustausgleich: (?<gross>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Ertrag/Steueranteil pro Stk.: 0,011287 EUR
                                        // KESt aus Neubestand: -8,81 USD
                                        // Auslands-KESt neu: -15,14 USD
                                        // -23,95 USD
                                        // Devisenkurs: 1,0366 (09.01.2025) -23,11 EUR
                                        // Zu Lasten IBAN gW23 6257 9138 2168 9133 Valuta 10.01.2025 -23,11 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "exchangeRate", "gross", "baseCurrency") //
                                                        .find("Auslands\\-KESt neu:.*")
                                                        .match("^\\-[\\.,\\d]+ (?<termCurrency>[A-Z]{3}).*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ [A-Z]{3}.*$") //
                                                        .match("^Zu Lasten .* \\-(?<gross>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Geschäftsart: Steuerkorrektur - Abrechnung Verkauf 47346359-18.12.2024/Nr.1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Steuerkorrektur).*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        // @formatter:off
                        // Geschäftsart: Steuerkorrektur - Abrechnung Verkauf 47346359-18.12.2024/Nr.1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Abrechnung Verkauf(?<note>.*).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addDepotStatementTransaction()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Mustermann EUR 2.281,75
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.* (?<currency>[A-Z]{3}) [\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Alter Saldo per 01.06.2022 6.974,89
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) [\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2} (?!Ertrag).* [\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 28.06 Mustermann 28.06 2.000,00
                        // IBAN: AT12 1234 1234 1234 1234
                        // REF: 38000220627-5336530-0000561
                        // @formatter:on
                        .section("date", "amount", "note") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) .* [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)$") //
                        .match("^IBAN: .*$") //
                        .match("^(?<note>REF: .*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        var feeBlock = new Block("^[\\d]{2}\\.[\\d]{2} Abschluss [\\d]{2}\\.[\\d]{2} [\\.,\\d]+\\-$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 30.09 Abschluss 30.09 4,50-
                        // Kontoführungsgebühr                          4,50-
                        // @formatter:on
                        .section("date", "amount", "note").optional() //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) Abschluss [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)\\-$") //
                        .match("^(?<note>Kontof.hrungsgeb.hr) .* [\\.,\\d]+\\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addNonImportableTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: (Umtausch\\/Bezug"
                        + "|Ausbuchung wegen Titelumtausch"
                        + "|Kapitalmaßnahme wegen Entflechtung)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            return portfolioTransaction;
                        })

                        // Is type --> "Zugang" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                        .section("type").optional() //
                        .match("^(?<type>(Abgang|Zugang)).*$") //
                        .assign((t, v) -> {
                            if ("Zugang".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                        })

                        // @formatter:off
                        // Geschäftsart: Umtausch/Bezug Auftrags-Nr.: 17374528 - 28.01.2025
                        // Geschäftsart: Ausbuchung wegen Titelumtausch
                        // Geschäftsart: Kapitalmaßnahme wegen Entflechtung
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Gesch.ftsart: (?<type>(Umtausch\\/Bezug" //
                                        + "|Ausbuchung wegen Titelumtausch" //
                                        + "|Kapitalmaßnahme wegen Entflechtung)).*$") //
                        .assign((t, v) -> {
                            if ("Umtausch/Bezug".equals(v.get("type")))
                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                            else if ("Ausbuchung wegen Titelumtausch".equals(v.get("type")))
                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                            else if ("Kapitalmaßnahme wegen Entflechtung".equals(v.get("type")))
                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);

                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: AT0000A1Z023 CONWERT IMMOBILIEN INVEST SE
                                        // AKTIEN O.N./ANSPR.NACHZAHLUNG
                                        // Kurs: 0,55 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: GB00B03MLX29 Shell PLC
                                        // Reg. Shares Class A EO -,07
                                        // Bezugsverhältnis: 1: 1
                                        // 0,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .find("Bezugsverh.ltnis:.*") //
                                                        .match("^[\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Verwahrart"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Abgang: Stk 27
                        // Zugang: Stk 4
                        // @formatter:on
                        .section("shares") //
                        .match("^(Abgang|Zugang): Stk (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN as17 4873 4614 7852 4461 Valuta 18.02.2025 14,85 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu Gunsten .* Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Record-Tag: 28.01.2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Record\\-Tag: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN as17 4873 4614 7852 4461 Valuta 18.02.2025 14,85 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Bezugsverhältnis: 1: 1
                                        // 0,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Bezugsverh.ltnis:.*") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Geschäftsart: Umtausch/Bezug Auftrags-Nr.: 17374528 - 28.01.2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Auftrags\\-Nr\\.: .* \\- [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer: -52,25 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer: (\\-)?(?<tax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KESt aus Neubestand: -93,23 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt aus Neubestand: (\\-)?(?<tax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Auslands-KESt neu: -0,83 EUR
                        // Auslands-KESt neu: 1,58 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Auslands\\-KESt neu: (\\-)?(?<tax>[\\-\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // USt: -0,50 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^USt: (\\-)?(?<tax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Umsatzsteuer: -0,62 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Umsatzsteuer: (\\-)?(?<tax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer: -327,58 EUR
                        // Quellensteuer: 1,89 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer: (\\-)?(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Quellensteuer US-Emittent: -54,80 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer US\\-Emittent: (\\-)?(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Fremde Börsespesen: -3,47 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde B.rsespesen: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Settlementspesen: -0,24 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Settlementspesen: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Spesen: -2,86 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Spesen: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen: -1,28 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Devisenprovision: -0,04 EUR
                        // Devisenprovision: 0,03 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Devisenprovision: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Grundgebühr: -3,-- EUR
                        // Grundgebühr: -7,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Grundgeb.hr: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Inkassoprovision: -3,11 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Inkassoprovision: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // easybank Orderspesen: -9,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*Orderspesen: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Flat Fee/Provision: -19,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Flat Fee\\/Provision: (\\-)?(?<fee>[\\-\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

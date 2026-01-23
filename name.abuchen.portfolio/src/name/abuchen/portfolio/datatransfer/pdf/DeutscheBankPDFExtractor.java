package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;

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
public class DeutscheBankPDFExtractor extends AbstractPDFExtractor
{
    private static final String SKIP_TRANSACTION = "skipTransaction";

    public DeutscheBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Deutsche Bank");
        addBankIdentifier("DB Privat- und Firmenkundenbank AG");

        addBuySellTransaction();
        addBuyTransactionFundsSavingsPlan();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Bank Privat- und Geschäftskunden AG";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Abrechnung: (Kauf|Verkauf|Zeichnung) von Wertpapieren");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{1,2}\\. .* [\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Abrechnung: (?<type>(Kauf|Verkauf)) von Wertpapieren.*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 123 1234567 00 BASF SE
                                        // WKN BASF11 Nominal ST 19
                                        // ISIN DE000BASF111 Kurs EUR 35,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\d]{3} [\\d]+ [\\d]{2} (?<name>.*)$") //
                                                        .match("^WKN (?<wkn>[A-Z0-9]{6}) Nominal ST [\\.,\\d]+$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Kurs (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 444 1234567 02 IVU TRAFFIC TECHNOLOGIES AG INH.AKT. O.N. 1/2
                                        // WKN 744850 Nominal 120
                                        // ISIN DE0007448508 Mischkurs (EUR) 14,80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\d]{3} [\\d]+ [\\d]{2} (?<name>.*) [\\d]\\/[\\d]{1,2}$")//
                                                        .match("^WKN (?<wkn>[A-Z0-9]{6}) Nominal [\\.,\\d]+$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .* \\((?<currency>[A-Z]{3})\\).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 123 1234567 01 6,875% TÜRKEI, REPUBLIK NT.06 17.M/S 03.36 1/2
                                        // WKN A0GLU5 Nominal USD 5.000,00
                                        // ISIN US900123AY60 Kurs 100,06 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\d]{3} [\\d]+ [\\d]{2} (?<name>.*) [\\d]\\/[\\d]{1,2}$")//
                                                        .match("^WKN (?<wkn>[A-Z0-9]{6}) Nominal (?<currency>[A-Z]{3}) [\\.,\\d]+$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Kurs .* %$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // WKN BASF11 Nominal ST 19
                                        // WKN 744850 Nominal 120
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* Nominal( ST)? (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // WKN A0GLU5 Nominal USD 5.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* Nominal [A-Z]{3} (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))
                        // @formatter:off
                        // 09:05 MEZ 1447743358 618 14,80 9.146,40 9.120,93
                        // @formatter:on
                        .section("time").optional() //
                        .match("^(?<time>[\\d]{2}:[\\d]{2}) MEZ [\\d]+ [\\d]{3} [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf(
                                        // @formatter:off
                                        // Schlusstag 07.07.2022 Ordernummer 123545
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // Belegnummer 1522788379 / 181373046 Schlusstag 23.07.2024 18:21
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Belegnummer .* Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Belegnummer 1234567890 / 123456 Schlusstag/-zeit MEZ 02.04.2015 / 09:04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Belegnummer .* Schlusstag\\/\\-zeit .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Belegnummer 1039975477 / 91752537 Schlusstag/-zeit MEZ 23.07.2024 18:20
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Belegnummer .* Schlusstag\\/\\-zeit .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Belegnummer 1694278628 / 24281 Schlusstag 20.08.2019
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Belegnummer .* Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag
                                        // EUR 9.613,77
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Gesamtbetrag") //
                                                        .match("^(?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Buchung auf Kontonummer 1234567 40 mit Wertstellung 08.04.2015 EUR 675,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Buchung auf Kontonummer [\\s\\d]+ mit Wertstellung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Belegnummer 1234567890 / 123456 Schlusstag/-zeit MEZ 02.04.2015 / 09:04
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Belegnummer [\\d]+ \\/ [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Zinsen für 158 Zinstage USD 150,86
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3") //
                                                        .match("^(?<note1>Zinsen .* [\\d]+ Zinstag(e)?).* (?<note3>[A-Z]{3}) (?<note2>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                                                            t.setNote(concatenate(t.getNote(), v.get("note2"), ": "));
                                                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                                                        }))

                        .wrap(t -> {
                            // If we have multiple entries in the document, with
                            // fee and fee refunds, then the "noProvision" flag
                            // must be removed.
                            type.getCurrentContext().remove("noProvision");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuyTransactionFundsSavingsPlan()
    {
        var type = new DocumentType("(db AnsparPlan|Verm.gensSparplan)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*, WKN [A-Z0-9]{6}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                        // @formatter:off
                                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652, Ausgabeaufschlag 5,00%
                                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652
                                        //
                                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "currency") //
                                                        .match("^(?<name>.*), WKN (?<wkn>[A-Z0-9]{6}).*$")//
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),

                                        // @formatter:off
                                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652, Ausgabeaufschlag 5,00%
                                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652
                                        //
                                        // 02.09.2019 0,1042 172,4300 25,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "currency") //
                                                        .match("^(?<name>.*), WKN (?<wkn>[A-Z0-9]{6}).*$")//
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<currency>[A-Z]{3})") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )
                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // 02.09.2019 0,1042 172,4300 25,00 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<shares>[\\.,\\d]+) [\\.,\\d]+ ([A-Z]{3}\\s)?[\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // 02.09.2019 0,1042 172,4300 25,00 EUR
                        // @formatter:on
                        .section("date")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\.,\\d]+ ([A-Z]{3}\\s)?[\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // 02.09.2019 0,1042 172,4300 25,00 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ ([A-Z]{3}\\s)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            // If we have multiple entries in the document, with
                            // fee and fee refunds, then the "noProvision" flag
                            // must be removed.
                            type.getCurrentContext().remove("noProvision");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift|Kupongutschrift)");
        this.addDocumentTyp(type);

        var firstRelevantLine = new Block("^(Dividendengutschrift|Ertragsgutschrift|Kupongutschrift)$");
        type.addBlock(firstRelevantLine);

        var pdfTransaction = new Transaction<AccountTransaction>();
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 380,000000 878841 US17275R1023
                                        // CISCO SYSTEMS INC.REGISTERED SHARES DL-,001
                                        // Dividende pro Stück 0,2600000000 USD Zahlbar 15.12.2014
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "currency") //
                                                        .match("^[\\.,\\d]+ (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^(Dividende|Aussch.ttung) pro St.ck [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominal Währung WKN ISIN
                                        // 6.000,000000 EUR 650155 DE0006501554
                                        // 6% MAGNUM AG GENUßSCHEINE 99/UNBEGR.
                                        // Dividende in % 6,0000000000 Zahlbar 05.09.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "wkn", "isin", "name") //
                                                        .match("^[\\.,\\d]+ (?<currency>[A-Z]{3}) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 380,000000 878841 US17275R1023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 6.000,000000 EUR 650155 DE0006501554
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) [A-Z]{3} [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Gutschrift mit Wert 15.12.2014 64,88 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^Gutschrift mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift mit Wert 15.12.2014 64,88 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Gutschrift mit Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttoertrag 98,80 USD 87,13 EUR
                                        // Umrechnungskurs USD zu EUR 1,1339000000
                                        //
                                        // Bruttoertrag 220,00 CAD 137,53 EUR
                                        // Umrechnungskurs CAD zu EUR 1,5996000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+) [A-Z]{3}.*$") //
                                                        .match("^Umrechnungskurs (?<termCurrency>[A-Z]{3}) zu (?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag 78,20 USD
                                        // Umrechnungskurs USD zu EUR 1,1019000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "baseCurrency", "termCurrency",
                                                                        "exchangeRate") //
                                                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) [A-Z]{3}[\\s]*$") //
                                                        .match("^Umrechnungskurs (?<termCurrency>[A-Z]{3}) zu (?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            if (!type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                var rate = asExchangeRate(v);
                                                                type.getCurrentContext().putType(rate);

                                                                var fxGross = Money.of(rate.getTermCurrency(),
                                                                                asAmount(v.get("fxGross")));
                                                                var gross = rate.convert(rate.getBaseCurrency(),
                                                                                fxGross);

                                                                // @formatter:off
                                                                // Even though this is a forex transaction, the security may be
                                                                // held in the native currency. In that case, the GROSS_VALUE
                                                                // unit needs to be set. In the foreign currency that is.
                                                                // @formatter:on
                                                                if (t.getSecurity().getCurrencyCode()
                                                                                .equals(t.getCurrencyCode()))
                                                                    checkAndSetGrossUnit(gross, fxGross, t,
                                                                                    type.getCurrentContext());
                                                                else
                                                                    checkAndSetGrossUnit(fxGross, gross, t,
                                                                                    type.getCurrentContext());
                                                            }
                                                        }))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug", //
                        builder -> builder //
                                        .section("currency") //
                                        .match("^.*[\\d]{4} [\\d]{4} [\\d]{4} [\\d]{4} [\\d]{2} .*(?<currency>[A-Z]{3}) [\\-|\\+] [\\.,\\d]+.*$")
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        .section("year").optional() //
                                        .match("^Kontoauszug vom [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$")
                                        .assign(Map::putAll));

        this.addDocumentTyp(type);

        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "((SEPA )?" //
                        + "(Dauerauftrag" //
                        + "|.berweisung" //
                        + "|Lastschrifteinzug" //
                        + "|Echtzeit.berweisung) .*" //
                        + "|Bargeldauszahlung GAA" //
                        + "|Kartenzahlung" //
                        + "|Verwendungszweck\\/ Kundenreferenz" //
                        + "|.bertrag \\(.berweisung\\) von) " //
                        + "(\\-|\\+) [\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.setMaxSize(2);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 15.10. 15.10. SEPA Überweisung von + 400,00
                                        // 2025 2025 Dr. kQEbfBPDq ZgltrGG wBPgFcQwn
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "type", "amount", "year", "note1") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) " //
                                                                        + "(SEPA )?" //
                                                                        + "(?<note>(Dauerauftrag" //
                                                                        + "|.berweisung" //
                                                                        + "|Lastschrifteinzug" //
                                                                        + "|Echtzeit.berweisung) .*" //
                                                                        + "|Bargeldauszahlung GAA" //
                                                                        + "|Kartenzahlung" //
                                                                        + "|Verwendungszweck\\/ Kundenreferenz" //
                                                                        + "|.bertrag \\(.berweisung\\) von) " //
                                                                        + "(?<type>(\\-|\\+)) (?<amount>[\\.,\\d]+)$")
                                                        .match("^[\\d]{4} (?<year>[\\d]{4}) (Buchung )?(?<note1>.*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type --> "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            // Formatting some notes
                                                            if (!v.get("note1").startsWith("Verwendungszweck"))
                                                                v.put("note", trim(v.get("note")) + " " + trim(v.get("note1")));

                                                            if (v.get("note").startsWith("Lastschrifteinzug"))
                                                                v.put("note", trim(v.get("note1")));

                                                            if (v.get("note").startsWith("Verwendungszweck"))
                                                                v.put("note", "");

                                                            if (v.get("note1").contains("Steuererstattung"))
                                                            {
                                                                // @formatter:off
                                                                // Change from DEPOSIT to TAX_REFUND
                                                                // @formatter:on
                                                                t.setType(AccountTransaction.Type.TAX_REFUND);

                                                                v.put("note", "Steuererstattung");
                                                            }

                                                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));

                                                            // @formatter:off
                                                            // If we have fees, then we skip the transaction
                                                            //
                                                            // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
                                                            // Saldo der Abschlussposten
                                                            // @formatter:on
                                                            if ("Saldo der Abschlussposten".equals(v.get("note1")))
                                                                type.getCurrentContext().put(SKIP_TRANSACTION,
                                                                                Messages.PDFSkipMissingDetails);

                                                            // @formatter:off
                                                            // If we have security transaction, then we skip the transaction
                                                            //
                                                            // 01.06. 02.06. Verwendungszweck/ Kundenreferenz - 1.073,65
                                                            // 2023 2023 WERTPAPIER-KAUF STK/NOM: 35
                                                            //
                                                            // 16.08. 16.08. Verwendungszweck/ Kundenreferenz + 33,23
                                                            // 2023 2023 ZINSEN/DIVIDENDEN/ERTRAEGE FIL/DEPOT-NR:
                                                            // @formatter:on
                                                            if (v.get("note1").contains("WERTPAPIER") || v.get("note1").contains("DEPOT-NR:") || v.get("note1").contains("STK/NOM"))
                                                                type.getCurrentContext().put(SKIP_TRANSACTION,
                                                                                Messages.PDFSkipMissingDetails);
                                                        }),
                                        // @formatter:off
                                        // 01.12. 01.12. SEPA Dauerauftrag an - 40,00
                                        // Mustermann, Max
                                        // IBAN DE1111110000111111
                                        // BIC OSDDDE81XXX
                                        //
                                        // 07.12. 07.12. SEPA Überweisung von + 562,00
                                        // Unser Sparverein
                                        // Verwendungszweck/ Kundenreferenz
                                        // 1111111111111111 1220 INKL. SONDERZAHLUNG
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "type", "amount", "note1") //
                                                        .documentContext("currency", "year") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) " //
                                                                        + "(SEPA )?" //
                                                                        + "(?<note>(Dauerauftrag" //
                                                                        + "|.berweisung" //
                                                                        + "|Lastschrifteinzug" //
                                                                        + "|Echtzeit.berweisung) .*" //
                                                                        + "|Bargeldauszahlung GAA" //
                                                                        + "|Kartenzahlung" //
                                                                        + "|Verwendungszweck\\/ Kundenreferenz" //
                                                                        + "|.bertrag \\(.berweisung\\) von) " //
                                                                        + "(?<type>(\\-|\\+)) (?<amount>[\\.,\\d]+)$")
                                                        .match("^(?<note1>.*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type --> "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            // Formatting some notes
                                                            if (!v.get("note1").startsWith("Verwendungszweck"))
                                                                v.put("note", trim(v.get("note")) + " " + trim(v.get("note1")));

                                                            if (v.get("note").startsWith("Lastschrifteinzug"))
                                                                v.put("note", trim(v.get("note1")));

                                                            if (v.get("note").startsWith("Verwendungszweck"))
                                                                v.put("note", "");

                                                            if (v.get("note1").contains("Steuererstattung"))
                                                            {
                                                                // @formatter:off
                                                                // Change from DEPOSIT to TAX_REFUND
                                                                // @formatter:on
                                                                t.setType(AccountTransaction.Type.TAX_REFUND);

                                                                v.put("note", "Steuererstattung");
                                                            }

                                                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));

                                                            // @formatter:off
                                                            // If we have fees, then we skip the transaction
                                                            //
                                                            // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
                                                            // Saldo der Abschlussposten
                                                            // @formatter:on
                                                                if ("Saldo der Abschlussposten".equals(v.get("note1")))
                                                                type.getCurrentContext().put(SKIP_TRANSACTION,
                                                                                Messages.PDFSkipMissingDetails);

                                                            // @formatter:off
                                                            // If we have security transaction, then we skip the transaction
                                                            //
                                                            // 01.06. 02.06. Verwendungszweck/ Kundenreferenz - 1.073,65
                                                            // 2023 2023 WERTPAPIER-KAUF STK/NOM: 35
                                                            //
                                                            // 16.08. 16.08. Verwendungszweck/ Kundenreferenz + 33,23
                                                            // 2023 2023 ZINSEN/DIVIDENDEN/ERTRAEGE FIL/DEPOT-NR:
                                                            // @formatter:on
                                                            if (v.get("note1").contains("WERTPAPIER") || v.get("note1").contains("DEPOT-NR:") || v.get("note1").contains("STK/NOM"))
                                                                type.getCurrentContext().put(SKIP_TRANSACTION,
                                                                                Messages.PDFSkipMissingDetails);
                                                        }))

                    .wrap(t -> {
                        var item = new TransactionItem(t);

                        if (t.getCurrencyCode() != null && t.getAmount() == 0)
                            item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                        if (type.getCurrentContext().containsKey(SKIP_TRANSACTION))
                        {
                            var skipped = new SkippedItem(item, type.getCurrentContext().get(SKIP_TRANSACTION));
                            // If we have multiple entries in the document,
                            // then the SKIP_TRANSACTION flag must be
                            // removed.
                            type.getCurrentContext().remove(SKIP_TRANSACTION);

                            return skipped;
                        }

                        return item;
                    }));

        // @formatter:off
        // Formatting:
        // Buchung | Valuta | Vorgang | Soll | Haben
        //
        // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
        // Saldo der Abschlussposten
        // @formatter:on
        var blockFees = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Verwendungszweck\\/ Kundenreferenz \\- [\\.,\\d]+$");
        type.addBlock(blockFees);
        blockFees.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note").optional() //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) Verwendungszweck\\/ Kundenreferenz \\- (?<amount>[\\.,\\d]+)$") //
                        .match("^(?<note>Saldo der Abschlussposten)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
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
                        // Kapitalertragsteuer (KESt)  - 9,88 USD - 8,71 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\)[\\s]{1,}\\- [\\.,\\d]+ [A-Z]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer (KESt) - 4,28 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\)[\\s]{1,}\\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer EUR -122,94
                        //@formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer (?<currency>[A-Z]{3}) \\-(?<tax>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf KESt - 0,53 USD - 0,47 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf KESt[\\s]{1,}\\- [\\.,\\d]+ [A-Z]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf KESt - 0,23 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf KESt[\\s]{1,}\\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf Kapitalertragsteuer (?<currency>[A-Z]{3}) \\-(?<tax>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf Kapitalertragsteuer EUR -1,23
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf Kapitalertragsteuer (?<currency>[A-Z]{3}) \\-(?<tax>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf KESt - 5,28 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf KESt[\\s]{1,}\\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf KESt - 11,79 USD - 10,43 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf KESt[\\s]{1,}\\- [\\.,\\d]+ [A-Z]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Anrechenbare ausländische Quellensteuer 13,07 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare ausl.ndische Quellensteuer (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        // If the provision fee and the fee refund are the same,
        // then we set a flag and don't book provision fee.
        transaction
                        // @formatter:off
                        // Provision EUR 3,13
                        // Provisions-Rabatt EUR -1,13
                        // Ausgekehrte Zuwendungen, die die Bank von DWS Xtrackers erhält EUR -2,00
                        // @formatter:on
                        .section("currency", "fee", "discountCurrency1", "discount1", "discountCurrency2", "discount2")
                        .optional() //
                        .match("^Provision (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Provisions\\-Rabatt (?<discountCurrency1>[A-Z]{3}) (\\-)?(?<discount1>[\\.,\\d]+)$") //
                        .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency2>[A-Z]{3}) \\-(?<discount2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            var discount1 = Money.of(asCurrencyCode(v.get("discountCurrency1")), asAmount(v.get("discount1")));
                            var discount2 = Money.of(asCurrencyCode(v.get("discountCurrency2")), asAmount(v.get("discount2")));

                            if (fee.subtract(discount1.add(discount2)).isPositive())
                            {
                                fee = fee.subtract(discount1.add(discount2));
                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }

                            type.getCurrentContext().putBoolean("noProvision", true);
                        })

                        // @formatter:off
                        // Provision EUR 3,13
                        // Provisions-Rabatt EUR -3,13
                        // @formatter:on
                        .section("currency", "fee", "discountCurrency", "discount").optional() //
                        .match("^Provision (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Provisions\\-Rabatt (?<discountCurrency>[A-Z]{3}) (\\-)?(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            var discount = Money.of(asCurrencyCode(v.get("discountCurrency")),asAmount(v.get("discount")));

                            if (!type.getCurrentContext().getBoolean("noProvision"))
                            {
                                if (fee.subtract(discount).isPositive())
                                {
                                    fee = fee.subtract(discount);
                                    checkAndSetFee(fee, t, type.getCurrentContext());
                                }

                                type.getCurrentContext().putBoolean("noProvision", true);
                            }
                        })

                        // @formatter:off
                        // Provision EUR 1,26
                        // Ausgekehrte Zuwendungen, die die Bank von DWS Xtrackers erhält EUR -1,26
                        // @formatter:on
                        .section("currency", "fee", "discountCurrency", "discount").optional() //
                        .match("^Provision (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency>[A-Z]{3}) \\-(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            var discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (!type.getCurrentContext().getBoolean("noProvision"))
                            {
                                if (fee.subtract(discount).isPositive())
                                {
                                    fee = fee.subtract(discount);
                                    checkAndSetFee(fee, t, type.getCurrentContext());
                                }

                                type.getCurrentContext().putBoolean("noProvision", true);
                            }
                        })

                        // @formatter:off
                        // ISIN DE0008476524 Kurs EUR 319,49
                        // Ihre Referenz DBM01 Ausgabeaufschlag 5,00 %
                        // Preisnachlass EUR -4,88
                        // @formatter:on
                        .section("currency", "amount", "discountCurrency", "discount", "percentageFee").optional() //
                        .match("^.* Kurs (?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                        .match("^.* Ausgabeaufschlag (?<percentageFee>[\\.,\\d]+) %$") //
                        .match("^Preisnachlass (?<discountCurrency>[A-Z]{3}) \\-(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var percentageFee = asBigDecimal(v.get("percentageFee"));
                            var amount = asBigDecimal(v.get("amount"));
                            var discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                var fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC); //

                                var fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // fee = fee - discount
                                fee = fee.subtract(discount);

                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        })

                        // @formatter:off
                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652, Ausgabeaufschlag 5,00%
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // @formatter:on
                        .section("percentageFee", "amount", "currency").optional() //
                        .match("^.*, WKN [A-Z0-9]{6}, .* (?<percentageFee>[\\.,\\d]+)%$")//
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var percentageFee = asBigDecimal(v.get("percentageFee")); // 5.00
                            var amount = asBigDecimal(v.get("amount")); // 279.38

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                var fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC);

                                // fee = fee - discount
                                var fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // Assign the fee to the current context
                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });

        transaction
                        // @formatter:off
                        // Provision EUR 7,90
                        // Provision EUR -7,90
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noProvision"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Provision (0,25 %) EUR 8,78
                        // Provision (0,25 %) EUR -8,78
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision \\([\\.,\\d]+ %\\) (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // XETRA-Kosten EUR 0,60
                        // XETRA-Kosten EUR -0,60
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^XETRA-Kosten (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Weitere Provision der Bank bei der börslichen
                        // Orderausführung EUR 2,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Weitere Provision .* (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Spesen und Auslagen EUR -5,40
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Fremde Spesen und Auslagen (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

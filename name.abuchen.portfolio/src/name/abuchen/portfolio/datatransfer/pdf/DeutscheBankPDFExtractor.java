package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;

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
public class DeutscheBankPDFExtractor extends AbstractPDFExtractor
{
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
        DocumentType type = new DocumentType("Abrechnung: (Kauf|Verkauf) von Wertpapieren");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\. .* [\\d]{4}$");
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
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Kurs (?<currency>[\\w]{3}) .*$") //
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
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .* \\((?<currency>[\\w]{3})\\) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // WKN BASF11 Nominal ST 19
                        // WKN 744850 Nominal 120
                        // @formatter:on
                        .section("shares") //
                        .match("^.* Nominal( ST)? (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

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
                                        // Belegnummer 1694278628 / 24281 Schlusstag 20.08.2019
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Belegnummer .* Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
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
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag
                                        // EUR 9.613,77
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Gesamtbetrag") //
                                                        .match("^(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Buchung auf Kontonummer 1234567 40 mit Wertstellung 08.04.2015 EUR 675,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Buchung auf Kontonummer [\\s\\d]+ mit Wertstellung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
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
        DocumentType type = new DocumentType("Ihr db AnsparPlan");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*, WKN [A-Z0-9]{6}, .* [\\.,\\d]+%$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD , WKN 847652, Ausgabeaufschlag 5,00%
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // @formatter:on
                        .section("name", "wkn", "currency") //
                        .match("^(?<name>.*), WKN (?<wkn>[A-Z0-9]{6}), .* [\\.,\\d]+%$")//
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // @formatter:on
                        .section("date")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // 03.01.2024 0,1610 279,3800 EUR 44,98 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ [\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^^(Dividendengutschrift|Ertragsgutschrift)$");
        type.addBlock(firstRelevantLine);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
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
                                                        .match("^(Dividende|Aussch.ttung) pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominal Währung WKN ISIN
                                        // 6.000,000000 EUR 650155 DE0006501554
                                        // 6% MAGNUM AG GENUßSCHEINE 99/UNBEGR.
                                        // Dividende in % 6,0000000000 Zahlbar 05.09.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "wkn", "isin", "name") //
                                                        .match("^[\\.,\\d]+ (?<currency>[\\w]{3}) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
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
                                                        .match("^(?<shares>[\\.,\\d]+) [\\w]{3} [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Gutschrift mit Wert 15.12.2014 64,88 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^Gutschrift mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift mit Wert 15.12.2014 64,88 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Gutschrift mit Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttoertrag 98,80 USD 87,13 EUR
                                        // Umrechnungskurs USD zu EUR 1,1339000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Umrechnungskurs (?<termCurrency>[\\w]{3}) zu (?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag 78,20 USD
                                        // Umrechnungskurs USD zu EUR 1,1019000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttoertrag (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Umrechnungskurs (?<termCurrency>[\\w]{3}) zu (?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            if (!type.getCurrentContext().getBoolean("negative"))
                                                            {
                                                                ExtrExchangeRate rate = asExchangeRate(v);
                                                                type.getCurrentContext().putType(rate);

                                                                Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                                Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }
                                                        }))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug vom", //
                        builder -> builder //
                                        .section("currency") //
                                        .match("^.* [\\d]{4} [\\d]{4} [\\d]{4} [\\d]{4} [\\d]{2} .*(?<currency>[\\w]{3}) [\\-|\\+] [\\.,\\d]+$")
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        .section("year") //
                                        .match("^Kontoauszug vom [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                                        .assign(Map::putAll));

        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Buchung | Valuta | Vorgang | Soll | Haben
        //
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
        Block blockDepositRemoval = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "((SEPA )?" //
                        + "(Dauerauftrag" //
                        + "|.berweisung" //
                        + "|Lastschrifteinzug" //
                        + "|Echtzeit.berweisung) .*" //
                        + "|Bargeldauszahlung GAA" //
                        + "|Kartenzahlung" //
                        + "|Verwendungszweck\\/ Kundenreferenz) " //
                        + "(\\-|\\+) [\\.,\\d]+$");
        type.addBlock(blockDepositRemoval);
        blockDepositRemoval.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "sign", "amount", "note1") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) " //
                                        + "(SEPA )?" //
                                        + "(?<note>(Dauerauftrag" //
                                        + "|.berweisung" //
                                        + "|Lastschrifteinzug" //
                                        + "|Echtzeit.berweisung) .*" //
                                        + "|Bargeldauszahlung GAA" //
                                        + "|Kartenzahlung" //
                                        + "|Verwendungszweck\\/ Kundenreferenz) " //
                                        + "(?<sign>(\\-|\\+)) (?<amount>[\\.,\\d]+)$")
                        .match("^(?<note1>.*)$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // Formatting some notes
                            if (!v.get("note1").startsWith("Verwendungszweck"))
                                v.put("note", trim(v.get("note")) + " " + trim(v.get("note1")));

                            if (v.get("note").startsWith("Lastschrifteinzug"))
                                v.put("note", trim(v.get("note1")));

                            if (v.get("note").startsWith("Verwendungszweck"))
                                v.put("note", "");

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
                                    type.getCurrentContext().putBoolean("skipTransaction", true);

                            // @formatter:off
                            // If we have security transaction, then we skip the transaction
                            //
                            // 01.06. 02.06. Verwendungszweck/ Kundenreferenz - 1.073,65
                            // 2023 2023 WERTPAPIER-KAUF STK/NOM: 35
                            //
                            // 16.08. 16.08. Verwendungszweck/ Kundenreferenz + 33,23
                            // 2023 2023 ZINSEN/DIVIDENDEN/ERTRAEGE FIL/DEPOT-NR:
                            // @formatter:on
                            if (v.get("note1").contains("WERTPAPIER") || v.get("note1").contains("DEPOT-NR:"))
                                type.getCurrentContext().putBoolean("skipTransaction", true);
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            if (type.getCurrentContext().getBoolean("skipTransaction"))
                                return null;

                            // If we have multiple entries in the document,
                            // then the "skipTransaction" flag must be removed.
                            type.getCurrentContext().remove("skipTransaction");

                            return item;
                        }));

        // @formatter:off
        // Formatting:
        // Buchung | Valuta | Vorgang | Soll | Haben
        //
        // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
        // Saldo der Abschlussposten
        // @formatter:on
        Block blockFees = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Verwendungszweck\\/ Kundenreferenz \\- [\\.,\\d]+$");
        type.addBlock(blockFees);
        blockFees.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
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
        transaction
                        // @formatter:off
                        // Kapitalertragsteuer (KESt)  - 9,88 USD - 8,71 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer (KESt) - 4,28 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer EUR -122,94
                        //@formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf KESt - 0,53 USD - 0,47 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf KESt - 0,23 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag auf Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf Kapitalertragsteuer EUR -1,23
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf KESt - 5,28 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf KESt ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer auf KESt - 11,79 USD - 10,43 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer auf KESt ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Anrechenbare ausländische Quellensteuer 13,07 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare ausl.ndische Quellensteuer (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
                        .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Provisions\\-Rabatt (?<discountCurrency1>[\\w]{3}) (\\-)?(?<discount1>[\\.,\\d]+)$") //
                        .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency2>[\\w]{3}) \\-(?<discount2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            Money discount1 = Money.of(asCurrencyCode(v.get("discountCurrency1")), asAmount(v.get("discount1")));
                            Money discount2 = Money.of(asCurrencyCode(v.get("discountCurrency2")), asAmount(v.get("discount2")));

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
                        .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Provisions\\-Rabatt (?<discountCurrency>[\\w]{3}) (\\-)?(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")),asAmount(v.get("discount")));

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
                        .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency>[\\w]{3}) \\-(?<discount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

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
                        .match("^.* Kurs (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .match("^.* Ausgabeaufschlag (?<percentageFee>[\\.,\\d]+) %$") //
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
                                BigDecimal fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC); //

                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

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
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(v.get("percentageFee")); // 5.00
                            BigDecimal amount = asBigDecimal(v.get("amount")); // 279.38

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                BigDecimal fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC);

                                // fee = fee - discount
                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

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
                        .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noProvision"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Provision (0,25 %) EUR 8,78
                        // Provision (0,25 %) EUR -8,78
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision \\([\\.,\\d]+ %\\) (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // XETRA-Kosten EUR 0,60
                        // XETRA-Kosten EUR -0,60
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^XETRA-Kosten (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Weitere Provision der Bank bei der börslichen
                        // Orderausführung EUR 2,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Weitere Provision .* (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Spesen und Auslagen EUR -5,40
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Fremde Spesen und Auslagen (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

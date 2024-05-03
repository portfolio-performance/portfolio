package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class GenoBrokerPDFExtractor extends AbstractPDFExtractor
{
    public GenoBrokerPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("GENO Broker GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
        addDeliveryInOutBoundTransaction();
    }

    @Override
    public String getLabel()
    {
        return "GENO Broker GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$", "^Den Gegenwert buchen wir.*$");
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
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> { //
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 30 Carbios SA Anrechte Aktie FR001400IRI9 (A3EJEH)
                                        // 1Ausführungskurs 30,88 EUR Auftraggeber Mustermann
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^.*Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> { //
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz")) //
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1"))); //

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 2.100 LOGAN ENERGY CORP.                 CA5408991019 (A3EMQR)
                                        // REGISTERED SHARES O.N.
                                        // Abrech.-Preis 0,35 CAD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Abrech\\.\\-Preis [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> { //
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz")) //
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1"))); //

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Stück 30 Carbios SA Anrechte Aktie FR001400IRI9 (A3EJEH)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 30.06.2023 09:57:4 Fällig am 07.07.2023
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 967,47 EUR
                        // Ausmachender Betrag 4.714,55- EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs (EUR/CAD) 1,4508 vom 01.08.2023
                        // Kurswert 506,62- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)\\- [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        //  Auftragsnummer
                                        // Mustermann 00000000     Datum
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.*(?<note1>Auftragsnummer).*$") //
                                                        .match(".* (?<note2>[\\d]+\\/[\\.\\d]+) .* Datum.*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2")))),
                                        // @formatter:off
                                        //  Auftragsnummer 433499/69.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.*(?<note1>Auftragsnummer) (?<note2>[\\d]+\\/[\\.\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2")))))

                        // @formatter:off
                        // Limit billigst
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.* Kundenservice .*$");
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
                                        // Stück 1.000                              CROPENERGIES AG
                                        // INHABER-AKTIEN O.N.
                                        // DE000A0LAUP1    (A0LAUP)
                                        // Dividende pro Stück                      0,60         EUR
                                        // @formatter:on
                                        section -> section
                                                        .attributes("name", "nameContinued", "isin", "wkn", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]{1,}\\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^Dividende pro St.ck .* (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Stück 30 VANGUARD S&P 500 UCITS ETF IE00B3XXRP09 (A1JX53)
                                        // REGISTERED SHARES USD DIS.ON
                                        // Zahlbarkeitstag 27.12.2023 Ausschüttung pro St. 0,279226000 USD
                                        // @formatter:on
                                        section -> section
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^.* Aussch.ttung pro St\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Stück 600 EQUINOR ASA NO0010096985 (675213)
                                        // NAVNE-AKSJER NK 2,50
                                        // Zahlbarkeitstag 25.08.2023 Dividende pro Stück 9,4091 NOK
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^.* Dividende pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Stück 1.000                              CROPENERGIES AG
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 14.07.2023 zu Gunsten des Kontos 12345678 (IBAN DE05 7206 9274 0000 0000
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag        445,94+   EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag[\\s]{1,}(?<amount>[\\.,\\d]+)\\+[\\s]{1,}(?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs                             EUR / CAD  1,4915
                                        // Dividendengutschrift                                   14.133,00     CAD        9.475,70+   EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Devisenkurs[\\s]{1,}(?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^Dividendengutschrift[\\s]{1,}(?<fxGross>[\\.,\\d]+)[\\s]{1,}[\\w]{3}[\\s]{1,}(?<gross>[\\.,\\d]+)\\+[\\s]{1,}[\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Devisenkurs EUR / USD  1,1168
                                        // Ausschüttung 8,38 USD 7,50+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^Aussch.ttung[\\s]{1,}(?<fxGross>[\\.,\\d]+)[\\s]{1,}[\\w]{3}[\\s]{1,}(?<gross>[\\.,\\d]+)\\+[\\s]{1,}[\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        //      Abrechnungsnr.      60007000
                        // Abrechnungsnr.        000000000
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.*(?<note1>Abrechnungsnr\\.)[\\s]{1,}(?<note2>[\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        final DocumentType type = new DocumentType("Fusion", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Ex-Tag: 07.08.2023
                                        // @formatter:on
                                        .section("date") //
                                        .match("^Ex\\-Tag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));

        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Einbuchung|Ausbuchung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Ausbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Einbuchung|Ausbuchung).*$") //
                        .assign((t, v) -> {
                            if (v.get("type").contains("Ausbuchung"))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        // @formatter:off
                        // Gattungsbezeichnung ISIN
                        // Stück 50- PDC ENERGY INC. US69327R1014 (A1JZ02)
                        // REGISTERED SHARES DL -,01
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued") //
                        .documentContext("date") //
                        .find("(Einbuchung|Ausbuchung).*") //
                        .match("^St.ck [\\.,\\d]+(\\-)? (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date")));

                            // No amount available
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                            t.setCurrencyCode("EUR");
                            t.setAmount(0);
                        })

                        // @formatter:off
                        // Stück 50- PDC ENERGY INC.                    US69327R1014 (A1JZ02)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)(\\-)? .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

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
                        // Einbehaltene Quellensteuer 25 % auf 14.133,00 CAD     2.368,93-    EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 9.475,70 EUR             1.421,36     EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<creditableWithHoldingTax>[\\.,\\d]+)[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Verrechnete anrechenbare ausländische Quellensteuer
                        // (Verhältnis 100/25) auf 3,96 EUR    15,87  -  EUR
                        // Berechnungsgrundlage für die Kapitalertragsteuer                     584,13     EUR
                        //
                        // Verrechnete anrechenbare ausländische Quellensteuer
                        // (Verhältnis 100/25) auf 2,44 EUR 9,76 - EUR
                        // Berechnungsgrundlage für die Kapitalertragsteuer 0,00 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency", "amount").optional() //
                        .find("Verrechnete anrechenbare ausl.ndische Quellensteuer") //
                        .match("^\\(Verh.ltnis .*\\) auf [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<creditableWithHoldingTax>[\\.,\\d]+)([\\s]{1,})?(\\-)?[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .match("^Berechnungsgrundlage für die Kapitalertrags(s)?teuer[\\s]{1,}(?<amount>[\\.,\\d]+)([\\s]{1,})?(\\-)?[\\s]{1,}[\\w]{3}.*$")
                        .assign((t, v) -> {
                            if (asAmount(v.get("amount")) != 0)
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer 25 % auf 584,13 EUR      146,03-    EUR
                        // Kapitalertragsteuer 25 % auf 486,03 EUR 121,50- EUR
                        // Kapitalertragsteuer 25,00% auf 38,43 EUR 9,61- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<tax>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 146,03 EUR     8,03-    EUR
                        // Solidaritätszuschlag 5,5 % auf 121,50 EUR 6,68- EUR
                        // Solidaritätszuschlag 5,50% auf 9,61 EUR 0,53- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<tax>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 5,5 % auf 146,03 EUR     8,03-    EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<tax>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 32,95-EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\-([\\s]+)?(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 0,1900 % vom Kurswert 12,03- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 5,60- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsentgelt 2,52- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Handelsentgelt (?<fee>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,10- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\-[\\s]{1,}(?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

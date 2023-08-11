package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import static name.abuchen.portfolio.util.TextUtil.trim;

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
public class EbasePDFExtractor extends AbstractPDFExtractor
{
    public EbasePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("European Bank for Financial Services GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addFeesWithSecurityTransaction();
        addDeliveryInOutBoundTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "European Bank for Financial Services GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf"
                        + "|Ansparplan"
                        + "|Fondsertrag"
                        + "|Verkauf"
                        + "|Entgelt Verkauf"
                        + "|Entgeltbelastung Verkauf"
                        + "|Entnahmeplan"
                        + "|Fondsumschichtung"
                        + "|Wiederanlage Fondsertrag"
                        + "|Wiederanlage Ertragsaussch.ttung)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);

            // We have multiple entries in the document
            // and cannot split it with "endWith",
            // so the "skipTransaction" flag must be removed.
            type.getCurrentContext().remove("skipTransaction");

            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf"
                                                + "|Ansparplan"
                                                + "|Fondsertrag"
                                                + "|Verkauf"
                                                + "|Entgelt Verkauf"
                                                + "|Entgeltbelastung Verkauf"
                                                + "|Entnahmeplan"
                                                + "|Fondsumschichtung \\((Abgang|Zugang)\\) .*"
                                                + "|Wiederanlage Fondsertrag [\\.,\\d]+ [\\w]+"
                                                + "|\\(Anteilpreis\\))( .*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Kauf"
                                + "|Ansparplan"
                                + "|Fondsertrag"
                                + "|Verkauf"
                                + "|Entgelt Verkauf"
                                + "|Entgeltbelastung Verkauf"
                                + "|Entnahmeplan"
                                + "|Fondsumschichtung \\(Abgang\\)"
                                + "|Fondsumschichtung \\(Zugang\\)"
                                + "|Wiederanlage Fondsertrag"
                                + "|Wiederanlage Ertragsaussch.ttung)) .*$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type"))
                                    || "Entgelt Verkauf".equals(v.get("type"))
                                    || "Entgeltbelastung Verkauf".equals(v.get("type"))
                                    || "Entnahmeplan".equals(v.get("type"))
                                    || "Fondsumschichtung (Abgang)".equals(v.get("type")))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Here we set a flag for all entries
                // which should be skipped.
                // If this is not done, it can happen that taxes and fees
                // are included from the following entries
                .section("type").optional()
                .match("^(?<type>Fondsertrag) .*$")
                .assign((t, v) -> {
                    if ("Fondsertrag".equals(v.get("type")))
                        type.getCurrentContext().putBoolean("skipTransaction", true);
                })

                // @formatter:off
                // Kauf 300,00 EUR mit Kursdatum 20.11.2019 in Depotposition 1234567890.01
                // Xtr.(IE) - Russell Midcap Registered Shares 1C USD o.N.
                // IE00BJZ2DC62 12,729132 26,002300 USD 1,105500 299,40 EUR
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .find("(Kauf"
                            + "|Ansparplan"
                            + "|Fondsertrag"
                            + "|Verkauf"
                            + "|Entgelt Verkauf"
                            + "|Entgeltbelastung Verkauf"
                            + "|Entnahmeplan"
                            + "|Fondsumschichtung \\((Abgang|Zugang)\\)"
                            + "|Wiederanlage Fondsertrag [\\.,\\d]+ [\\w]+) .*")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (\\-)?[\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3})( [\\.,\\d]+)? [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // 444444.09 iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile (ISIN DE000A0F5UH1)
                // @formatter:on
                // Wiederanlage Ertragsausschüttung 400023594/2001 18.01.2016 23,550000 0,082378 1,94 EUR
                .section("name", "isin", "currency").optional()
                .match("^[\\d]+\\.[\\d]+ (?<name>.*) \\(ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$$")
                .match("^Wiederanlage Ertragsaussch.ttung .* (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // @formatter:off
                                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 21.11.2019
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^.* Buchungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Wiederanlage Ertragsausschüttung 400023594/2001 18.01.2016 23,550000 0,082378 1,94 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Wiederanlage Ertragsaussch.ttung .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                .oneOf(
                                // @formatter:off
                                // IE00BJZ2DC62 12,729132 26,002300 USD 1,105500 299,40 EUR
                                // LU0592215403 -0,084735 1,824200 USD 1,104100 0,14 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (\\-)?(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // FR0010405431 199,500000 1,055800 EUR 210,63 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (\\-)?(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // Wiederanlage Ertragsausschüttung 400023594/2001 18.01.2016 23,550000 0,082378 1,94 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^Wiederanlage Ertragsaussch.ttung .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3}")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // If we have a "Verkauf wegen Vorabpauschale"
                                // we set this amount.

                                // @formatter:off
                                // Verkauf wegen Vorabpauschale 0,13 EUR mit Kursdatum 27.01.2020 aus Depotposition XXXXXXXX.02
                                // @formatter:on
                                // abzgl. Steuereinbehalt 0,13 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .find("Verkauf wegen Vorabpauschale .*")
                                        .match("^abzgl\\. Steuereinbehalt (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // If we have a "Entgelt Verkauf"
                                // in which fee are immediately withheld,
                                // without a separate transaction,
                                // we first post the sale and then the fee payment.

                                // @formatter:off
                                // Entgelt Verkauf mit Kursdatum 20.12.2017 aus Depotposition 11111111111.01
                                // Depotführungsentgelt inkl. 19% USt 12,00 EUR
                                // VL-Vertragsentgelt inkl. 16 % USt 9,75 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .find("Entgelt Verkauf .*")
                                        .match("^(Depotf.hrungsentgelt|VL\\-Vertragsentgelt) inkl\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // If we have a "Entgeltbelastung Verkauf"
                                // in which fee are immediately withheld,
                                // without a separate transaction,
                                // we first post the sale and then the fee payment.

                                // @formatter:off
                                // Entgeltbelastung Verkauf 3,00 EUR mit Kursdatum 06.04.2021 aus Depotposition 99999999999.01
                                // Summe 3,00 EUR
                                // Depotführungsentgelt inkl. 19 %
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .find("Entgeltbelastung Verkauf .*")
                                        .match("^Summe (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^(Depotf.hrungsentgelt|VL\\-Vertragsentgelt) inkl\\. .*$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Wiederanlage Ertragsausschüttung 400023594/2001 18.01.2016 23,550000 0,082378 1,94 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Wiederanlage Ertragsaussch.ttung .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Abwicklung über IBAN Institut Zahlungsbetrag
                                // DE49XXXXXXXX XXXXXXXX XXXXXXXX XXXXXXXX XXXXXXXX 300,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .find("Abwicklung über IBAN Institut Zahlungsbetrag")
                                        .match("^.* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Zahlungsbetrag 0,37 EUR
                                // Zahlungsbetrag aus Überweisung 50,00 EUR
                                // Zahlungsbetrag nach Währungskonvertierung mit Devisenkurs 1,051200 EUR/USD 4,37 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Zahlungsbetrag( aus Überweisung| nach W.hrungskonvertierung .*)? (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // IE00BJZ2DC62 12,729132 26,002300 USD 1,105500 299,40 EUR
                                // LU0552385295 -0,000268 136,090000 USD 1,216800 0,03 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("termCurrency", "exchangeRate", "gross", "baseCurrency")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (\\-)?[\\.,\\d]+ [\\.,\\d]+ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // ISIN Anteilsbestand Betrag je Anteil Betrag
                                // GB00B0MY6T00 300,991871 0,012407000 GBP 3,73 GBP
                                // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs abzgl. Steuern
                                // 0,73 EUR 0,04 EUR 0,00 EUR 0,899700 0,70 GBP
                                // Zahlungsbetrag 3,03 GBP
                                // @formatter:on
                                section -> section
                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency")
                                        .find("ISIN Anteilsbestand Betrag je Anteil Betrag")
                                        .match("^.* [\\.,\\d]+ [\\.,\\d]+ [\\w]{3} (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})$")
                                        .find("Kapitalertragsteuer Solidarit.tszuschlag Kirchensteuer Devisenkurs abzgl\\. Steuern")
                                        .match("^[\\.,\\d]+ (?<termCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3}$")
                                        .match("^Zahlungsbetrag [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 21.11.2019
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ref. Nr. .*), .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .optionalOneOf(
                                // @formatter:off
                                // Entgelt Verkauf mit Kursdatum 20.12.2017 aus Depotposition 11111111111.01
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Entgelt Verkauf) .*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // Entgeltbelastung Verkauf 3,00 EUR mit Kursdatum 06.04.2021 aus Depotposition 99999999999.01
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Entgeltbelastung Verkauf) .*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // vermögenswirksame Leistungen
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>verm.genswirksame Leistungen)$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // Verkauf wegen Vorabpauschale 0,14 EUR mit Kursdatum 27.01.2020 aus Depotposition XXXXXXXXXX.05
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Verkauf wegen Vorabpauschale) .*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // Wiederanlage Fondsertrag 0,37 EUR mit Kursdatum 20.01.2020 in Depotposition 1234567890.21
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Wiederanlage Fondsertrag) .*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                ,
                                // @formatter:off
                                // Wiederanlage Ertragsausschüttung 400023594/2001 18.01.2016 23,550000 0,082378 1,94 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Wiederanlage Ertragsaussch.ttung) .*$")
                                        .assign((t, v) -> {
                                            if (t.getNote() != null)
                                                t.setNote(t.getNote() + " | " + trim(v.get("note")));
                                            else
                                                t.setNote(v.get("note"));
                                        })
                                )

                .wrap(t -> {
                    if (!type.getCurrentContext().getBoolean("skipTransaction"))
                        return new BuySellEntryItem(t);

                    // If we have multiple entries in the document,
                    // then the "skipTransaction" flag must be removed.
                    type.getCurrentContext().remove("skipTransaction");
                    return null;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Fondsertrag");
        this.addDocumentTyp(type);

        Block block = new Block("^Fondsertrag .*$", "^(Zahlungsbetrag nach|Zahlungsbetrag(?! in)|Die Auszahlung|Summe der belasteten) .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Fondsertrag (Ausschüttung) mit Bestandsdatum 11.12.2019 in Depotposition 1234567890.31
                // Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN
                // IE00B3RBWM25 7,332986 0,297309 USD 2,18 USD
                // @formatter:on
                .section("name", "isin", "currency")
                .find("Fondsertrag .*")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 02.01.2020
                // @formatter:on
                .section("date")
                .match("^.* Buchungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                // IE00BJZ2DC62 12,729132 26,002300 USD 1,105500 299,40 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // FR0010405431 199,500000 1,055800 EUR 210,63 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // DE123 European Bank for Financial Services 0,03 EUR
                                // Die Auszahlung erfolgt über die oben genannte Bankverbindung.
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^.* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^Die Auszahlung erfolgt über die oben genannte Bankverbindung.$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Zahlungsbetrag 1,79 USD
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Zahlungsbetrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Zahlungsbetrag nach Währungskonvertierung mit Devisenkurs 1,104600 0,29 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Zahlungsbetrag nach W.hrungskonvertierung .* [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Zahlungsbetrag nach Währungskonvertierung mit Devisenkurs 1,051200 EUR/USD 4,37 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Zahlungsbetrag nach W.hrungskonvertierung .* [\\.,\\d]+ [\\w]{3}\\/[\\w]{3} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // ISIN Anteilsbestand Betrag je Anteil Betrag
                                // DE000A0D8Q49 1,000000 0,394130 USD 0,39 USD
                                // Zahlungsbetrag nach Währungskonvertierung mit Devisenkurs 1,104600 0,29 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "termCurrency", "exchangeRate", "baseCurrency")
                                        .find("ISIN Anteilsbestand Betrag je Anteil Betrag")
                                        .match("^.* [\\.,\\d]+ [\\.,\\d]+ [\\w]{3} (?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$")
                                        .match("^Zahlungsbetrag nach W.hrungskonvertierung .* (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ (?<baseCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // ISIN Anteilsbestand Betrag je Anteil Betrag
                                // DE000A0F5UF5 27,707360 0,202830000 USD 5,62 USD
                                // Zahlungsbetrag nach Währungskonvertierung mit Devisenkurs 1,051200 EUR/USD 4,37 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "exchangeRate", "baseCurrency", "termCurrency")
                                        .find("ISIN Anteilsbestand Betrag je Anteil Betrag")
                                        .match("^.* [\\.,\\d]+ [\\.,\\d]+ [\\w]{3} (?<fxGross>[\\.,\\d]+) [\\w]{3}$")
                                        .match("^Zahlungsbetrag nach W.hrungskonvertierung .* (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Exchange rate for foreign currency account
                                //
                                // 0,34 EUR 0,01 EUR 0,00 EUR 1,117800 0,39 USD
                                // @formatter:on
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate")
                                        .match("^[\\.,\\d]+ (?<baseCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ (?<termCurrency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);
                                        })
                        )

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 30.09.2019
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ref. Nr. .*), .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale zum Stichtag");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        Block firstRelevantLine = new Block("^Vorabpauschale zum Stichtag .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        // @formatter:off
        // The advance tax payment is always paid in local currency.
        // If the security is in foreign currency,
        // the exchange rate is missing in the document for posting.
        //
        // Vorabpauschale zum Stichtag 31.12.2020 aus Depotposition xxx.21
        // Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N.
        // Ref. Nr. 000/22012021, Buchungsdatum 22.01.2021
        // ISIN Betrag je Anteil
        // LU0552385295 0,038342500 EUR <-- MISSING Exchange Rate
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer
        // 0,03 EUR 0,00 EUR 0,00 EUR
        // Belastung der angefallenen Steuern in Höhe von 0,03 EUR erfolgt durch Verkauf aus Depotposition xxx.21 mit Ref. Nr.
        // 000/22012021
        //
        // --------------------------------------------------------
        //
        // Verkauf wegen Vorabpauschale 0,03 EUR mit Kursdatum 25.01.2021 aus Depotposition xxx.21
        // Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N.
        // Ref. Nr. 000/22012021, Buchungsdatum 26.01.2021
        // Belastung der angefallenen Steuern aus Depotposition xxx.21 mit Ref. Nr. 000/22012021 (Vorabpauschale)
        // ISIN Anteile Abrechnungskurs Devisenkurs Betrag
        // LU0552385295 -0,000268 136,090000 USD 1,216800 0,03 EUR <-- Exchange Rate
        // abzgl. Steuereinbehalt 0,03 EUR
        // Zahlungsbetrag 0,00 EUR
        // Vorabpauschale zum Stichtag 31.12.2020 aus Depotposition xxx.25
        // @formatter:on

        pdfTransaction
                // @formatter:off
                // Vorabpauschale zum Stichtag 31.12.2019 aus Depotposition XXXXXXXXXX.05
                // Xtrackers MSCI Philippines Inhaber-Anteile 1C-USD o.N.
                // LU0592215403 0,005674390 EUR
                // @formatter:on
                .section("name", "isin", "currency")
                .find("Vorabpauschale zum Stichtag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(0L);
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Ref. Nr. XXXXXXXXXX/XXXXXXXXXX, Buchungsdatum 24.01.2020
                // @formatter:on
                .section("date")
                .match("^.* Buchungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Belastung der angefallenen Steuern in Höhe von 0,14 EUR erfolgt durch Verkauf aus Depotposition XXXXXXXXXX.05 mit Ref. Nr.
                // @formatter:on
                .section("currency", "amount")
                .match("^Belastung der angefallenen Steuern in H.he von (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 21.11.2019
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ref. Nr. .*), .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // Vorabpauschale zum Stichtag 31.12.2019 aus Depotposition XXXXXXXXXX.05
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Vorabpauschale zum Stichtag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + trim(v.get("note")));
                    else
                        t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new);
    }

    private void addFeesWithSecurityTransaction()
    {
        DocumentType type = new DocumentType("(Entgelt|Entgeltbelastung) Verkauf");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Entgelt|Entgeltbelastung) Verkauf .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Entgelt Verkauf mit Kursdatum 20.12.2017 aus Depotposition 11111111111.01
                // db x-trackers DAX ETF (DR) Inhaber-Anteile 1C o.N.
                // LU0274211480 -0,100548 127,228500 EUR 12,79 EUR
                // @formatter:on
                .section("name", "isin", "currency").optional()
                .find("(Entgelt|Entgeltbelastung) Verkauf .*")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\-[\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3})( [\\.,\\d]+)? [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // @formatter:off
                                // IE00BJZ2DC62 -12,729132 26,002300 USD 1,105500 299,40 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\-(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // LU0274211480 -0,100548 127,228500 EUR 12,79 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\-(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // @formatter:off
                // Ref. Nr. XXXXXXXXXX/XXXXXXXXXX, Buchungsdatum 24.01.2020
                // @formatter:on
                .section("date")
                .match("^.* Buchungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                // Depotführungsentgelt inkl. 19% USt 12,00 EUR
                                // VL-Vertragsentgelt inkl. 16 % USt 9,75 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(Depotf.hrungsentgelt|VL\\-Vertragsentgelt) inkl\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Entgeltbelastung Verkauf 3,00 EUR mit Kursdatum 06.04.2021 aus Depotposition 99999999999.01
                                // Summe 3,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .find("Entgeltbelastung Verkauf .*")
                                        .match("^Summe (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                // @formatter:off
                // IE00B4L5Y983 -0,167976 71,243400 USD 1,227400 9,75 EUR
                // IE00BJZ2DC62 12,729132 26,002300 USD 1,105500 299,40 EUR
                // @formatter:on
                .section("termCurrency", "baseCurrency", "exchangeRate", "gross").optional()
                .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (\\-)?[\\.,\\d]+ [\\.,\\d]+ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 21.11.2019
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ref. Nr. .*), .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // Depotführungsentgelt inkl. 19% USt 12,00 EUR
                // VL-Vertragsentgelt inkl. 16 % USt 9,75 EUR
                // Depotführungsentgelt inkl. 19 %
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>(Depotf.hrungsentgelt|VL\\-Vertragsentgelt)) inkl\\. .*$")
                .assign((t, v) -> {
                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + trim(v.get("note")));
                    else
                        t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("Eingang externer .bertrag");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^Eingang externer .bertrag .*$", "^Gegenwert der Anteile: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Einbuchung" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                .section("type").optional()
                .match("^(?<type>Eingang) externer .bertrag .*$")
                .assign((t, v) -> {
                    if ("Eingang".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                })

                // @formatter:off
                // Eingang externer Übertrag 10,000000 Anteile mit Kursdatum 27.09.2019 in Depotposition 1234567890.24
                // ComStage-Nikkei 225 UCITS ET99133507781F Inhaber-Anteile I o.N.
                // LU0378453376 10,000000
                // Gegenwert der Anteile: 202,64 EU
                // @formatter:on
                .section("name", "isin", "currency")
                .match("^Eingang externer .bertrag .*$")
                .match("^(?<name>.*)$")
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+$")
                .match("^Gegenwert der Anteile: [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 30.09.2019
                // @formatter:on
                .section("date")
                .match("^.* Buchungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // LU0378453376 10,000000
                // @formatter:on
                .section("shares")
                .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Gegenwert der Anteile: 202,64 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^Gegenwert der Anteile: (?<amount>[\\,.\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Ref. Nr. XXXXXXXX/XXXXXXXX, Buchungsdatum 30.09.2019
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Ref. Nr. .*), .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepotStatementTransaction()
    {
        DocumentType type = new DocumentType("Kontoauszug\\-Nr\\. [\\d]+");
        this.addDocumentTyp(type);

        // @formatter:off
        // 12.12.2022 020739500 12.12.2022 SEPA Lastschrift Einzug 200,00 EUR
        // 14.04.2020 123456 14.04.2020 SEPA Lastschrift Einzug 15,00 EUR
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} SEPA Lastschrift Einzug [\\.,\\d]+ [\\w]{3}$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                .section("date", "note", "amount", "currency")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>[\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} SEPA Lastschrift Einzug (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
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
                // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs abzgl. Steuern
                // 0,34 EUR 0,01 EUR 0,00 EUR 1,117800 0,39 USD
                // @formatter:on
                .section("tax", "currency").optional()
                .find("Kapitalertragsteuer .*")
                .match("^(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .match("^(Zahlungsbetrag|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs abzgl. Steuern
                // 0,34 EUR 0,01 EUR 0,00 EUR 1,117800 0,39 USD
                // @formatter:on
                .section("tax", "currency").optional()
                .find(".* Solidarit.tszuschlag .*")
                .match("^[\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .match("^(Zahlungsbetrag|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs abzgl. Steuern
                // 0,34 EUR 0,01 EUR 0,00 EUR 1,117800 0,39 USD
                // @formatter:on
                .section("tax", "currency").optional()
                .find(".* Kirchensteuer .*")
                .match("^[\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .match("^(Zahlungsbetrag|Abwicklung) .*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // ETF-Transaktionsentgelt 0,60 EUR
                // Abwicklung über IBAN Institut Zahlungsbetrag
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^ETF\\-Transaktionsentgelt (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(Zahlungsbetrag nach|Zahlungsbetrag(?! in)|Die Auszahlung|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // ETF-Transaktionsentgelt 0,05 EUR 0,00 EUR 0,05 EUR
                // Abwicklung über IBAN Institut Zahlungsbetrag
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^ETF\\-Transaktionsentgelt (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$")
                .match("^(Zahlungsbetrag nach|Zahlungsbetrag(?! in)|Die Auszahlung|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // gezahlte Vertriebsprovision 0,00 EUR (im Abrechnungskurs enthalten, 100,000 % Bonus)
                // Abwicklung über IBAN Institut Zahlungsbetrag
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^gezahlte Vertriebsprovision (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .match("^(Zahlungsbetrag nach|Zahlungsbetrag(?! in)|Die Auszahlung|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Additional Trading Costs (ATC) 0,13 EUR
                // Abwicklung über IBAN Institut Zahlungsbetrag
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Additional Trading Costs \\(ATC\\) (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(Zahlungsbetrag nach|Zahlungsbetrag(?! in)|Die Auszahlung|Abwicklung|Summe der belasteten) .*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
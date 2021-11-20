package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class OnvistaPDFExtractor extends AbstractPDFExtractor
{
    public OnvistaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("onvista bank"); //$NON-NLS-1$
        addBankIdentifier("OnVista Bank"); //$NON-NLS-1$
        addBankIdentifier("onvist a bank"); //$NON-NLS-1$
        addBankIdentifier("Frankfurt am Main"); //$NON-NLS-1$

        addBuySellTransaction();
        addReinvestTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addDeliveryInOutBoundTransaction();
        addRegistrationFeeTransaction();
        addAccountStatementTransaction();
        addTaxesBlock();
    }

    @Override
    public String getLabel()
    {
        return "OnVista Bank GmbH"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf|Gesamtfälligkeit|Zwangsabfindung|Dividende)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^(Spitze )?(Kauf|Verkauf|Gesamtf.lligkeit|Ausbuchung:)( .*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("(^|Wir haben für Sie )(?<type>verkauft|Gesamtf.lligkeit|Ausbuchung:)( .*)?$")
                .assign((t, v) -> {
                    if (v.get("type").equals("verkauft")
                                    || v.get("type").equals("Gesamtfälligkeit")
                                    || v.get("type").equals("Ausbuchung:"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Gattungsbezeichnung ISIN
                // DWS Deutschland Inhaber-Anteile LC DE0008490962
                // STK 0,7445 EUR 200,1500
                .section("name", "isin", "name1", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                // Fres. SE
                // STK 65,000 EUR 39,4400
                .section("name", "isin", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // Société Générale Effekten GmbH DISC.Z 13.05.2021 DE000SE8F9E8
                // 13.05.21 NVIDIA 498
                .section("name", "isin", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung ISIN
                // Sky Deutschland AG Namens-Aktien o.N. DE000SKYD000
                // Abfindung zu:
                // EUR 6,680000
                .section("name", "isin", "name1", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .find("Abfindung zu:")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominal Einlösung zu:
                // STK 65,000 EUR 39,4400
                // Nominal Kurs
                // STK 0,7445 EUR 200,1500
                // Nominal Ex-Tag
                // STK 25,000 22.09.2015
                .section("notation", "shares")
                .find("Nominal (Kurs|Einl.sung zu:|Ex-Tag)")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} [\\.,\\d]+|[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    // Workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // Handelszeit 15:30 Orderprovision USD 11,03-
                // Handelszeit 12:00
                .section("time").optional()
                .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2})( .*)?$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Handelstag 15.08.2019 Kurswert EUR 149,01-
                                section -> section
                                        .attributes("date")
                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                                ,
                                // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                                // TUI AG Wandelanl.v.2009(2014) 17.11.2014 DE000TUAG117
                                section -> section
                                        .attributes("date")
                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4})( [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4})? [\\w]{12}$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Ausbuchung:
                                // Nominal Ex-Tag
                                // STK 25,000 22.09.2015
                                section -> section
                                        .attributes("date")
                                        .find("Ausbuchung:")
                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // STK 25,000 17.05.2013 17.05.2013 EUR 0,700000
                                section -> section
                                        .attributes("date")
                                        .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4}) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // 19.08.2019 123450042 EUR 150,01
                .section("amount", "currency").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Handelstag 16.12.2020 Kurswert EUR 1.060,00 
                // Handelstag 19.08.2019 Kurswert USD 1.677,20-
                // 21.08.2019 372650044 EUR/USD 1,1026 EUR 1.536,13
                .section("fxCurrency", "fxAmount", "exchangeRate", "amount", "currency").optional()
                .match("^.* Kurswert (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)[-\\s]$")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));

                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // 21.08.2019 372650044 EUR/USD 1,1026 EUR 1.536,13
                .section("exchangeRate").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Zwangsabfindung gemäß Hauptversammlungsbeschluss vom 22. Juli 2015. Der Übertragungsbeschluss wurde am 15.
                .section("note").optional()
                .match("^(?<note>Zwangsabfindung gem.ß Hauptversammlungsbeschluss .*) Der Übertragungsbeschluss .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void addReinvestTransaction()
    {
        DocumentType type = new DocumentType("(Reinvestierung|Ertragsthesaurierung)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^(Reinvestierung|Ertragsthesaurierung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // iS.EO Go.B.C.2.5-5.5y.U.ETF DE Inhaber-Anteile DE000A0H08A8
                // Nominal Ex-Tag Zahltag Ausschüttungsbetrag pro Stück
                // STK 0,4512 06.10.2017 20.10.2017 EUR 0,169895
                .section("name", "isin", "name1", "date", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setDate(asDate(v.get("date")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                /***
                 * If we have a reinvest
                 * we pick the second
                 */
                // Gattungsbezeichnung ISIN
                // Deutsche Telekom AG Namens-Aktien o.N. DE0005557508
                // Die Dividende wurde wie folgt in neue Aktien reinvestiert:
                // Gattungsbezeichnung ISIN
                // Deutsche Telekom AG Dividend in Kind-Cash Line DE000A1TNRX5
                .section("name", "isin", "name1", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung ISIN
                // Deutsche Telekom AG Namens-Aktien o.N. DE0005557508
                // Gattungsbezeichnung ISIN
                // Deutsche Telekom AG Dividend in Kind-Cash Line DE000A1TNRX5
                .section("isin").optional()
                .find("Gattungsbezeichnung .*").optional()
                .match("^.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    // if we have a new isin for reinvest, we grab the old for note
                    Map<String, String> context = type.getCurrentContext();
                    context.put("isin", v.get("isin"));
                })

                // Nominal Reinvestierungspreis
                // STK 25,000 EUR 0,700000
                // Nominal Ex-Tag Zahltag Ausschüttungsbetrag pro Stück
                // STK 0,4512 06.10.2017 20.10.2017 EUR 0,169895
                .section("notation", "shares")
                .find("Nominal (Ex-Tag|Reinvestierungspreis)( .*)")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} [\\.,\\d]+"
                                + "|[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+)$")
                .assign((t, v) -> {
                    // Workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // Zahlungstag 17.05.2013
                .section("date").optional()
                .match("^Zahlungstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Leistungen aus dem steuerlichen Einlagenkonto (§27 KStG) EUR 17,50
                .section("amount", "currency").optional()
                .match("^Leistungen aus dem steuerlichen Einlagenkonto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Steuerliquidität EUR 0,02
                // zu versteuern EUR 0,08
                .section("tax", "amount", "currency").optional()
                .match("Ertragsthesaurierung .*")
                .match("Steuerliquidität [\\w]{3} (?<tax>[\\.,\\d]+)")
                .match("^zu versteuern (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")) - asAmount(v.get("tax")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .section("note").optional()
                .match("^(?<note>Reinvestierung) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setNote(v.get("note") + ": " + context.get("isin"));
                })

                .section("note").optional()
                .match("^(?<note>Ertragsthesaurierung) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Erträgnisgutschrift|Kupongutschrift|Reinvestierung)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende|Aussch.ttung|Zinsen) f.r( (?![\\d]+).*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });
        
        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                .section("name", "isin", "name1", "currency").optional()
                .match("^Gattungsbezeichnung ISIN$")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // 5,875% Telefónica Europe B.V. 14.02.2033 14.02.2020 XS0162869076
                // EO-Medium-Term Notes 2003(33)
                // EUR 5.000,000 14.02.2020 5,875000 %
                .section("name", "isin", "name1", "currency").optional()
                .match("^Gattungsbezeichnung F.lligkeit Zinstermin ISIN$")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\.,\\d]+ %$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit Zinstermin ISIN
                // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014 17.11.2010 DE000TUAG117
                // STK 1,000 17.11.2010 EUR 1,548250
                .section("name", "isin", "name1", "currency").optional()
                .match("^Gattungsbezeichnung F.lligkeit Zinstermin ISIN$")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                /***
                 * This is for the reinvestment of dividends
                 */
                // Gattungsbezeichnung ISIN
                // Gattungsbezeichnung ISIN
                .section("isin").optional()
                .find("Gattungsbezeichnung .*")
                .find("Gattungsbezeichnung .*")
                .match("^.* (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    // if we have a new isin for reinvest, we grab it
                    Map<String, String> context = type.getCurrentContext();
                    context.put("isin", v.get("isin"));
                })

                .oneOf(
                            // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                            // STK 1,000 17.11.2010 EUR 1,548250
                            section -> section
                                    .attributes("notation", "shares")
                                    .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+)( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})? [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$")
                                    .assign((t, v) -> {
                                        // Workaround for bonds
                                        if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                                            t.setShares((asShares(v.get("shares")) / 100));
                                        else
                                            t.setShares(asShares(v.get("shares")));
                                    })
                            ,
                            // EUR 5.000,000 14.02.2020 5,875000 %
                            section -> section
                                    .attributes("shares")
                                    .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ %$")
                                    .assign((t, v) -> t.setShares((asShares(v.get("shares")) / 100)))
                        )

                .oneOf(
                            // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                            // STK 1,000 17.11.2010 EUR 1,548250
                            section -> section
                                    .attributes("date")
                                    .match("^STK [\\.,\\d]+( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$")
                                    .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                            ,
                            // EUR 5.000,000 14.02.2020 5,875000 %
                            section -> section
                                    .attributes("date")
                                    .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ %$")
                                    .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // 21.04.2016 172306238 EUR 10,00
                .section("amount", "currency").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                /***
                 * This is for the reinvestment of dividends
                 */
                // Leistungen aus dem steuerlichen Einlagenkonto (§27 KStG) EUR 17,50
                .section("amount", "currency").optional()
                .match("^Leistungen aus dem steuerlichen Einlagenkonto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                /***
                 * This is for the "Ertragsthesaurierung"
                 */
                // Steuerliquidität EUR 0,02
                // zu versteuern EUR 0,08
                .section("tax", "amount", "currency").optional()
                .match("Ertragsthesaurierung .*")
                .match("Steuerliquidität [\\w]{3} (?<tax>[\\.,\\d]+)")
                .match("^zu versteuern (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")) - asAmount(v.get("tax")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // 15.07.2019 123456789 EUR/USD 1,1327 EUR 13,47
                .section("amount", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/(?<fxCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                    RoundingMode.HALF_DOWN);
                    BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                    RoundingMode.HALF_DOWN);
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money forex = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                    Math.round(t.getAmount() / inverseRate.doubleValue()));
                    Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, inverseRate);

                    if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                        t.addUnit(unit);
                })

                // Die Dividende wurde wie folgt in neue Aktien reinvestiert:
                .section("note").optional()
                .match("^.* (?<note>neue Aktien reinvestiert:)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setNote(v.get("note") + " " + context.get("isin"));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Block block = new Block("^Steuerpflichtige Vorabpauschale .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Deka DAX UCITS ETF Inhaber-Anteile DE000ETFL011
                // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                .section("name", "isin", "name1", "currency")
                .match("^Gattungsbezeichnung ISIN$")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                .section("shares")
                .match("^STK (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                .section("date")
                .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // 10.01.2020 123456789 EUR 0,02
                .section("amount", "currency").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                /***
                 * If all taxes are covered by Freistellungsauftrag/Verlusttopf, 
                 * section "Wert Konto-Nr. Betrag zu Ihren Lasten" is not present, 
                 * then extract currency here
                 */
                // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                .section("currency").optional()
                .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (t.getCurrencyCode() == null)
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                /***
                 * If the advance tax rate is 0,00 and parced correctly, 
                 * then do not import.
                 */
                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return new NonImportableItem("Steuerpflichtige Vorabpauschale mit 0 " + t.getCurrencyCode());
                });
        
        block.set(pdfTransaction);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("Fusion"
                        + "|Freier Erhalt"
                        + "|Einbuchung von Rechten"
                        + "|Wertlose Ausbuchung"
                        + "|Kapitalerhöhung"
                        + "|Kapitalherabsetzung"
                        + "|Umtausch", (context, lines) -> {
            Pattern pDate = Pattern.compile("(^|^[\\s]+).*, (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("date", m.group("date"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^(Einbuchung:|Ausbuchung:|Wir erhielten zu Gunsten Ihres Depots)(.*)?");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Ausbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                .section("type").optional()
                .match("^(?<type>Einbuchung|Ausbuchung):([\\s]+)?$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Ausbuchung"))
                    {
                        t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    }
                })

                // Gattungsbezeichnung ISIN
                // Gagfah S.A. Actions nom. EO 1,25 LU0269583422
                // Nominal Ex-Tag
                // STK 12,000 04.07.2017
                .section("name", "isin", "name1", "shares")
                .find("(Einbuchung:|Ausbuchung:|Wir erhielten zu Gunsten Ihres Depots)(.*)?")
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("^STK (?<shares>[\\.,\\d]+)( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})?( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})?$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setDateTime(asDate(context.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                    t.setAmount(0L);
                })

                // STK 12,000 04.07.2017
                // STK 28,000 02.12.2011 02.12.2011
                .section("date").optional()
                .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Kapitalherabsetzung im Verhältnis 10:1. Weitere Informationen finden Sie im elektronischen Bundesanzeiger
                .section("note").optional()
                .match("^Kapitalherabsetzung im (?<note>Verh.ltnis [\\.,\\d]+:[\\.,\\d]+.) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                // Umbuchung der Teil- in Vollrechte. Für die eventuell verbleibenden Bruchteile (Nachkommastellen) in den Teilrechten
                .section("note").optional()
                .match("^(?<note>Umbuchung der Teil- in Vollrechte.) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                // Wertlose Ausbuchung ADRESSZEILE1=Herr
                .section("note").optional()
                .match("^(?<note>Wertlose Ausbuchung) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                // Einbuchung der Rechte zur Dividende wahlweise. Bitte beachten Sie hierzu unser separates Anschreiben.
                .section("note").optional()
                .match("^(?<note>Einbuchung der Rechte zur Dividende wahlweise.) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);

        addTaxReturnBlock(type);
    }

    private void addRegistrationFeeTransaction()
    {
        DocumentType type = new DocumentType("Registrierung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return t;
        });

        Block firstRelevantLine = new Block("^Registrierungsgeb.hr .*, [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Vonovia SE Namens-Aktien o.N. DE000A1ML7J1
                .section("name", "isin", "name1")
                .match("^Gattungsbezeichnung ISIN$")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                // STK 6,000 22.07.2017
                .section("shares")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> {
                    // Workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // 24.07.2017 172406048 EUR 0,89
                .section("date", "amount", "currency")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Für die Registrierung der Namens-Aktien (auf Ihren Namen) im Aktionärs-Register belasten wir Ihrem Konto vorstehenden 
                .section("note").optional()
                .match("^F.r die (?<note>Registrierung der Namens-Aktien) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Kontoauszug|KONTOAUSZUG) Nr.", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(?<currency>[\\w]{3}) - Verrechnungskonto: .*$");
            Pattern pCurrency2 = Pattern.compile("^.* Customer Cash Account ([\\s]+)?(?<currency2>[\\w]{3})$");
            Pattern pYear = Pattern.compile("^(Kontoauszug|KONTOAUSZUG) Nr. (?<year>[\\d]{4}) / [\\d]+ .*$");
            Pattern pYear2 = Pattern.compile("^(Kontoauszug|KONTOAUSZUG) Nr. [\\d]+ per [\\d]{2}.[\\d]{2}.(?<year2>[\\d]{4})$");
            // read the current context here
            for (String line : lines)
            {
                // EUR - Verrechnungskonto: 0 111111 222
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
                // Ihre Kontonummer : 172306238 : Customer Cash Account  EUR
                m = pCurrency2.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency2"));
                }

                // Kontoauszug Nr. 2017 / 2 und Rechnungsabschluss zum 30.06.2017
                m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group("year"));
                }
                // KONTOAUSZUG Nr. 2 per 30.06.2015
                m = pYear2.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group("year2"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                // 04.04. 04.04. REF: 000045862247 200,00+
                // Überweisungseingang SEPA Max Mustermann
                .section("date", "amount", "note", "sign").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+)(?<sign>[-|+])$")
                .match("^(?<note>.berweisungseingang SEPA) .*$")
                .assign((t, v) -> {
                    // Is sign is negative change to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.REMOVAL);

                    t.setDateTime(asDate(v.get("date") + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 31.10. 31.10. REF: 000017304356 37,66
                // Saldenübernahme Nordnet
                .section("date", "amount", "note").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+)$")
                .match("^(?<note>Salden.bernahme) .*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 07.04. 03.04. REF: 000033640646 0,62-
                // Portogebühren
                .section("date", "amount", "note", "sign").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+)(?<sign>[-|+])$")
                .match("^(?<note>Portogeb.hren)$")
                .assign((t, v) -> {
                    // Is sign is negative change to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.FEES);

                    t.setDateTime(asDate(v.get("date") + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^(Spitze )?(Kauf|Verkauf|Gesamtf.lligkeit|Umtausch) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Gattungsbezeichnung ISIN
                // DWS Deutschland Inhaber-Anteile LC DE0008490962
                // STK 0,7445 EUR 200,1500
                .section("name", "isin", "name1", "currency").optional()
                .match("^Gattungsbezeichnung ISIN$")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                // Fres. SE
                // STK 65,000 EUR 39,4400
                .section("name", "isin", "name1", "currency").optional()
                .match("^Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN$")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // Société Générale Effekten GmbH DISC.Z 13.05.2021 DE000SE8F9E8
                // 13.05.21 NVIDIA 498
                .section("name", "isin", "name1").optional()
                .match("^Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN$")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[\\w]{12})$")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                // Gattungsbezeichnung ISIN
                // AGIF-Allianz Euro Bond Inhaber Anteile A (EUR) o.N. LU0165915215
                // STK 156,729
                .section("name", "isin", "name1").optional()
                .match("^Gattungsbezeichnung ISIN$")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^[\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                })

                .oneOf(
                                // STK 25,000 EUR 0,700000
                                section -> section
                                        .attributes("notation", "shares")
                                        .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            // Workaround for bonds
                                            if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                                                t.setShares((asShares(v.get("shares")) / 100));
                                            else
                                                t.setShares(asShares(v.get("shares")));
                                        })
                                ,
                                // STK 33,000 06.06.2011
                                section -> section
                                        .attributes("notation", "shares")
                                        .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+)( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})?$")
                                        .assign((t, v) -> {
                                            // Workaround for bonds
                                            if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                                                t.setShares((asShares(v.get("shares")) / 100));
                                            else
                                                t.setShares(asShares(v.get("shares")));
                                        })
                        )

                // Handelszeit 15:30 Orderprovision USD 11,03-
                // Handelszeit 12:00
                .section("time").optional()
                .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2})( .*)?$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Handelstag 15.08.2019 Kurswert EUR 149,01-
                                section -> section
                                        .attributes("date")
                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDateTime(asDate(v.get("date")));
                                        })
                                ,
                                // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                                // TUI AG Wandelanl.v.2009(2014) 17.11.2014 DE000TUAG117
                                section -> section
                                        .attributes("date")
                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4})( [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4})? [\\w]{12}$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // STK 33,000 06.06.2011
                                section -> section
                                        .attributes("date")
                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // 26.11.2015 172306238 68366911 EUR 7,90
                                section -> section
                                        .attributes("date")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\d]+ [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Frankfurt am Main, 26.02.2019
                                section -> section
                                        .attributes("date")
                                        .match("(^|^[\\s]+).*, (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                /***
                                 * This is for the reinvestment of dividends
                                 * We pick the second 
                                 */
                                // STK 25,000 17.05.2013 17.05.2013 EUR 0,700000
                                section -> section
                                        .attributes("date")
                                        .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // 17.12.2020 241462046 EUR/USD 1,2239 EUR 3.965,72
                .section("exchangeRate").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                // 16.12.2020 241462046 59592727 EUR 14,10
                .section("amount", "currency").optional()
                .find("Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));

                    /***
                     * If the currency of the security 
                     * differs from the account currency
                     */
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal exchangeRate = new BigDecimal(type.getCurrentContext().get("exchangeRate"));
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        Money amount = Money.of(t.getCurrencyCode(), asAmount(v.get("amount")));

                        // convert gross to security currency using exchangeRate
                        long gross = exchangeRate.multiply(BigDecimal.valueOf(asAmount(v.get("amount")))
                                        .setScale(0, RoundingMode.HALF_DOWN)).longValue();

                        Money fxAmount = Money.of(t.getSecurity().getCurrencyCode(), gross);

                        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate));
                    }
                })

                // Steuerausgleich nach § 43a EStG:
                .section("note").optional()
                .match("^(?<note>Steuerausgleich) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addTaxesBlock()
    {
        DocumentType type = new DocumentType("Umtausch", (context, lines) -> {
            Pattern pDate = Pattern.compile("(^|^[\\s]+).*, (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("date", m.group("date"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);

            /***
             * If we have multiple entries in the document,
             * with taxes and tax refunds,
             * then the "negative" flag must be removed.
             */
            type.getCurrentContext().remove("negative");

            return entry;
        });

        Block firstRelevantLine = new Block("^Ausbuchung:(.*)?");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Gagfah S.A. Actions nom. EO 1,25 LU0269583422
                // Nominal Ex-Tag
                // STK 12,000 04.07.2017
                .section("name", "isin", "name1")
                .find("(Einbuchung:|Ausbuchung:|Wir erhielten zu Gunsten Ihres Depots)(.*)?")
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setDateTime(asDate(context.get("date")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                    t.setAmount(0L);
                })

                // STK 12,000 04.07.2017
                // STK 28,000 02.12.2011 02.12.2011
                .section("date").optional()
                .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(.*)?$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // STK 33,000 06.06.2011
                // STK 156,729
                .section("notation", "shares")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+)( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})?$")
                .assign((t, v) -> {
                    // Workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // Wert Konto-Nr. Betrag zu Ihren Lasten
                // 23.11.2015 172306238 EUR 12,86
                .section("amount", "currency").optional()
                .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Ausmachender Betrag USD 0,30-
                // Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten
                // 21.08.2019 372650044 EUR/USD 1,1026 EUR 1.536,13
                .section("fxCurrency", "fxAmount", "exchangeRate", "amount", "currency").optional()
                .match("^Ausmachender Betrag (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)-$")
                .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));

                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.addUnit(grossValue);
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        /***
         * if we have a tax refunds,
         * we set a flag and don't book tax below
         */
        transaction
                .section("n").optional()
                .match("zu versteuern \\(negativ\\) (?<n>.*)")
                .assign((t, v) -> {
                    type.getCurrentContext().put("negative", "X");
                });

        transaction
                // Handelsplatz außerbörslich Lang & Schwarz Frz. Finanztrans. Steuer EUR 11,19-
                .section("currency", "tax").optional()
                .match("^.* Finanztrans\\. Steuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Börse Xetra/EDE Kapitalertragsteuer EUR 1,43-
                .section("currency", "tax").optional()
                .match("(^|^.* )Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Verwahrart Girosammelverwahrung Solidaritätszuschlag EUR 0,08-
                // Solidaritätszuschlag EUR 1,43-
                .section("currency", "tax").optional()
                .match("(^|^.* )Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer EUR 1,01-
                .section("currency", "tax").optional()
                .match("(^|^.* )Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // anrechenbare Quellensteuer 15% DKK 4,64
                // davon anrechenbare US-Quellensteuer EUR 4,74
                // davon anrechenbare US-Quellensteuer 15% EUR 2,72
                //    davon anrechenbare Quellensteuer Fondseingangsseite EUR 0,03
                .section("tax", "currency").optional()
                .match("(^|^.* davon |^davon )anrechenbare (US-)?Quellensteuer( [\\.,\\d]+%|.*)? (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // erstattungsfähige Quellensteuer 12% DKK 3,71
                .section("tax", "currency").optional()
                .match("^erstattungsf.hige Quellensteuer .* (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // einbehaltene Kapitalertragsteuer EUR 1,81
                // einbehaltene Kapitalertragsteuer EUR              0,39     
                .section("tax", "currency").optional()
                .match("^einbehaltene Kapitalertragsteuer ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // einbehaltener Solidaritätszuschlag EUR 0,10
                // einbehaltener Solidaritätszuschlag  EUR              0,02     
                .section("tax", "currency").optional()
                .match("^einbehaltener Solidarit.tszuschlag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // einbehaltene Kirchensteuer EUR 5,86
                // einbehaltener Kirchensteuer  EUR              0,02 
                .section("tax", "currency").optional()
                .match("^einbehaltene Kirchensteuer(  Ehegatte\\/Lebenspartner)? ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // einbehaltene Kirchensteuer Ehegatte/Lebenspartner EUR 1,09
                .section("tax", "currency").optional()
                .match("^einbehaltene Kirchensteuer Ehegatte\\/Lebenspartner ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(?<tax>[\\.,\\d]+)(.*)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelsplatz außerbörslich Orderprovision EUR 1,00-
                // Handelszeit 15:30 Orderprovision USD 11,03-
                .section("currency", "fee").optional()
                .match("^.* Orderprovision ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsplatz Börse NASDAQ/NAN Handelsplatzgebühr USD 5,51-
                .section("currency", "fee").optional()
                .match("^.* Handelsplatzgeb.hr ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börse Xetra/EDE Börsengebühr EUR 1,50-
                .section("currency", "fee").optional()
                .match("^.* Börsengeb.hr ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelszeit 12:30 Maklercourtage              EUR 0,75-
                .section("currency", "fee").optional()
                .match("^.* Maklercourtage ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}

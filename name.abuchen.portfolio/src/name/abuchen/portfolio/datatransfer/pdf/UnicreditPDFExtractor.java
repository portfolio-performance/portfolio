package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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
public class UnicreditPDFExtractor extends AbstractPDFExtractor
{

    public UnicreditPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UniCredit Bank AG");
        addBankIdentifier("UniCredit Bank GmbH");
        addBankIdentifier("DE 129 273 380");

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UniCredit Bank AG / HypoVereinsbank (HVB)";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(K a u f|V e r k a u f)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^W e r t p a p i e r \\- A b r e c h n u n g[\\s]{1,}(K a u f| V e r k a u f).*$");
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
                        .match("^W e r t p a p i e r \\- A b r e c h n u n g[\\s]{1,}(?<type>(K a u f| V e r k a u f)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(stripBlanks(v.get("type"))))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer/ISIN
                                        // SANOFI S.A. 920657
                                        // ST 22
                                        // ACTIONS PORT. EO 2 FR0000120578
                                        // Kurswert EUR 1.547,26
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "name1", "isin", "currency") //
                                                        .find("Nennbetrag Wertpapierbezeichnung.*")
                                                        .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6})$")
                                                        .match("^(?<name1>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+.*$")
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer/ISIN
                                        // ST 460 BANK OF AMERICA CORP. 858388   REGISTERED SHARES DL 0,01 US0605051046
                                        // Kurswert USD 19.200,40
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "name1", "wkn", "isin", "currency") //
                                                        .find("Nennbetrag Wertpapierbezeichnung.*")
                                                        .match("^ST [\\.,\\d]+ (?<name>.*) (?<wkn>[A-Z0-9]{6})[\\s]{2,}(?<name1>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+.*$")
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // ST 22
                        // ST 25 FIRST EAGLE AMUNDI-INTERNATIO. A1JQVV ACTIONS
                        // @formatter:on
                        .section("shares") //
                        .match("^ST (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zum Kurs von Ausführungstag/Zeit Ausführungsort Verwahrart
                        // EUR 192,14 20.04.2021 03.53.15 WP-Rechnung GS
                        // @formatter:on
                        .section("time").optional() //
                        .find("Zum Kurs von .*") //
                        .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Zum Kurs von Ausf}hrungstag/Zeit Ausf}hrungsort Verwahrart
                        // 15.02.2016
                        // EUR 70,33 PARIS WP-Rechnung
                        // 16.28.03
                        // @formatter:on
                        .section("time").optional().find("Zum Kurs von .*") //
                        .match("^[\\w]{3} [\\.,\\d]+ .*$") //
                        .match("^(?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Zum Kurs von Ausf}hrungstag/Zeit Ausf}hrungsort Verwahrart
                                        // 15.02.2016
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .find("Zum Kurs von .*") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // Zum Kurs von Ausführungstag/Zeit Ausführungsort Verwahrart
                                        // EUR 192,14 20.04.2021 03.53.15 WP-Rechnung GS
                                        // @formatter:on
                                        section -> section.attributes("date") //
                                                        .find("Zum Kurs von .*") //
                                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }))

                        // @formatter:off
                        // Belastung (vor Steuern) EUR 1.560,83
                        // Gutschrift (vor Steuern) EUR 8.175,91
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Belastung|Gutschrift) \\(vor Steuern\\) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kurswert USD 19.200,40
                        // Umrechnungskurs 1 EUR = 1,1 USD
                        // @formatter:on
                        .section("fxGross", "baseAmount", "baseCurrency", "termAmount", "termCurrency").optional() //
                        .match("^Kurswert [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                        .match("^Umrechnungskurs (?<baseAmount>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) = (?<termAmount>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            // Calculate and store the exchange rate
                            BigDecimal exchangeRate = BigDecimal.valueOf(((double) asAmount(v.get("termAmount")) / asAmount(v.get("baseAmount"))));
                            v.put("exchangeRate", exchangeRate.toPlainString());

                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Umsatzreferenz: 20160215WAB1861426155
                        // @formatter:on
                        .section("note").optional() //
                        .match("Umsatzreferenz: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Wertpapiermitteilung \\- Ertragszahlung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapiermitteilung \\- Ertragszahlung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // ACATIS GANÉ VALUE EVENT FONDS Wertpapierkennnummer  A1T73W / DE000A1T73W9
                        // INHABER-ANTEILE C
                        // Geschäftsjahr 2020/2021
                        // zahlbar mit EUR  15,00 Bruttobetrag   EUR 0,99
                        // @formatter:on
                        .section("name", "wkn", "isin", "name1", "currency") //
                        .match("^(?<name>.*) Wertpapierkennnummer[\\s]{1,}(?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)$") //
                        .match("^zahlbar mit (?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+.*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Geschäftsjahr"))
                                v.put("name", v.get("name") + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 0,066
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 18.05.2021 Gutschrift     EUR 0,99
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Valuta 18.05.2021 Gutschrift     EUR 0,99
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Gutschrift[\\s]{1,}(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Referenz 20210517KUP0123456789
                        // @formatter:on
                        .section("note").optional()
                        .match("Referenz (?<note>.*)$")
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Brokerkommission* EUR 0,27
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Brokerkommission\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt* EUR 3,09
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Transaktionsentgelt\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision EUR 10,21
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Wertpapierprovision* EUR 41,09-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Wertpapierprovision\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}

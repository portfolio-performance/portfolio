package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class DADATBankenhausPDFExtractor extends AbstractPDFExtractor
{
    public DADATBankenhausPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DADAT - Bankhaus");
        addBankIdentifier("DADAT-Bank");
        addBankIdentifier("Schelhammer Capital Bank AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addBuySellAccountStatementTransaction();
        addDividendeAccountStatementTransaction();
        addDividendeTaxesAccountStatementTransaction();
        addInterestAccountStatementTransaction();
        addFeesAccountStatementTransaction();
        addDepositRemovalAccountStatementTransaction();
        addTaxLossAdjustmentAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DADAT / Bankhaus Schelhammer & Schattera AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Abrechnungsauskunft|Kauf Depot)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Gesch.ftsart: Kauf|Buchungsbestätigung)$");
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
                                        // Titel: US09247X1019 B L A C K R O C K I NC.
                                        // Reg. Shares Class A DL -,01
                                        // Kurswert: -1.800,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]{1,}(?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurswert: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // ISIN NL0011794037 AHOLD DELHAIZE,KON.EO-,01 40,00000 STK
                                        // Kurs 26,160000 Kurswert -1.046,40 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) [\\.,\\d]+ STK$") //
                                                        .match("^Kurs .* (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zugang: 3 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Zugang: (?<shares>[\\.,\\d]+) Stk$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // ISIN NL0011794037 AHOLD DELHAIZE,KON.EO-,01 40,00000 STK
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^ISIN [A-Z]{2}[A-Z0-9]{9}[0-9] .* (?<shares>[\\.,\\d]+) STK$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelszeit: 17.2.2021 um 20:49:54 Uhr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Buchungsdatum: 27.12.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Buchungsdatum: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Lasten IBAN IBAN-NR -1.800,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu (Lasten|Gunsten) .* (\\-)?(?<amount>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Betrag in EUR -1.053,18 Eﬀekten
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Betrag in (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Auftrags-Nr.: 45247499-17.2.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr\\.: [\\d]+)\\-[\\d]{1,2}\\.[\\d]{1,2}.\\d{4}$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnung Ereignis$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Titel: US09247X1019 B L A C K R O C K I NC.
                        // Reg. Shares Class A DL -,01
                        // Dividende: 4,13 USD
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])[\\s]{1,}(?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("Dividende: [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Kurs:"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 3 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) Stk$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 23.3.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}.\\d{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Gunsten IBAN IBAN-NR 7,51 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs: 1,077 (30.4.2024) 32,99 EUR
                        // Ertrag: 45,50 EUR
                        // @formatter:on
                        .section("termCurrency", "exchangeRate", "baseCurrency", "gross").optional() //
                        .match("Dividende: [\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}.\\d{4}\\) [\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$") //
                        .match("^Ertrag: (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 92276651-25.11.2024
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\d]+)\\-[\\d]{1,2}\\.[\\d]{1,2}.\\d{4}$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // DI IMWEfP Tiftrog EUR 999.282,46
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Alter Saldo per 08.01.2024 45.452,58
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Kauf|Kauf aus Dauerauftrag|Verkauf) .*$");
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
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} (?<type>(Kauf|Kauf aus Dauerauftrag|Verkauf)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // STORNO VON 20210817  45747417
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>STORNO VON) .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        // @formatter:off
                        // 16.12 Kauf aus Dauerauftrag            Depot    7800000000/20191216-45514943 18.12 99,68-
                        // ISIN LU0378449770 COMST.-NASDAQ-100 U.ETF I               1,22000 STK
                        // Kurs                     80,340000  KURSWERT               -98,01 EUR
                        // @formatter:on
                        .section("date", "isin", "name", "shares", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (Kauf|Kauf aus Dauerauftrag|Verkauf)[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)[\\s]{1,}(?<shares>[\\.,\\d]+) STK$") //
                        .match("^(?i).* KURSWERT[\\s]{1,}(\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            v.put("name", replaceMultipleBlanks(v.get("name")));
                            t.setDate(asDate(v.get("date") + "." + v.get("year")));
                            t.setShares(asShares(v.get("shares")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 30.07 Kauf                             Depot    780680000/20200730-45125411 31.07 1.250,01-
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} (Kauf|Kauf aus Dauerauftrag|Verkauf)[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]{4}[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kurs                    282,740000  KURSWERT              1.979,18 USD
                        // DevKurs        1,187100/3.9.2020    DADAT Handelsspesen      -7,87 EUR
                        // Kurs                    206,940000  KURSWERT             -1.448,58 USD
                        // Handelsspesen            -5,06 USD  DevKurs        1,170500/30.7.2020
                        // @formatter:on
                        .section("fxGross", "termCurrency", "exchangeRate").optional() //
                        .match("^(?i).* KURSWERT[\\s]{1,}(\\-)?(?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$") //
                        .match("^.*DevKurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)\\/.*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap((t, ctx) -> {
                            BuySellEntryItem item = new BuySellEntryItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Moritz EUR 606,18
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Alter Saldo per 08.01.2024 45.452,58
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));


        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} Ertrag .*$");
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
                                        // 31.07 Ertrag                           Depot    7800000000/20200730-45756156 30.07 8,16
                                        // ISIN AT0000969985 AT+S AUST. TECH.SYS.O.N.               45,00000 STK
                                        // Kurs                      0,250000  ZINSERTRAG               11,25 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "isin", "name", "shares", "currency") //
                                                        .documentContext("year") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Ertrag[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)[\\s]{1,}(?<shares>[\\.,\\d]+) STK$") //
                                                        .match("^.* ZINSERTRAG[\\s]{1,}(\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", replaceMultipleBlanks(v.get("name")));
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 09.01 Ertrag                           Depot    0450115847/10802351-84757305 05.01 45,97
                                        // ISIN US7134481081 PEPSICO INC.     DL-,0166              55,00000 STK
                                        // Kurs                      1,265000  Zinsen/Dividenden        69,58 USD
                                        // Aktienanleihe v.23(24)ZAL
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "isin", "name", "shares", "currency") //
                                                        .documentContext("year") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Ertrag[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)[\\s]{1,}(?<shares>[\\.,\\d]+) STK$") //
                                                        .match("^.* Zinsen\\/Dividenden[\\s]{1,}(\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", replaceMultipleBlanks(v.get("name")));
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // 30.07 Kauf                             Depot    780680000/20200730-45125411 31.07 1.250,01-
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} Ertrag[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]{4}[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf(
                                        // @formatter:off
                                        // Kurs                      0,520000  ZINSERTRAG              104,00 USD
                                        // DevKurs        1,195900/30.7.2021
                                        // @formatter:on
                                        section -> section
                                                        .attributes("fxGross", "termCurrency", "exchangeRate")
                                                        .match("^(?i).* ZINSERTRAG[\\s]{1,}(\\-)?(?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$") //
                                                        .match("^.*DevKurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)\\/.*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Kurs                      1,265000  Zinsen/Dividenden        69,58 USD
                                        // DevKurs        1,097300/5.1.2024
                                        // @formatter:on
                                        section -> section
                                                        .attributes("fxGross", "termCurrency", "exchangeRate")
                                                        .match("^(?i).* Zinsen\\/Dividenden[\\s]{1,}(\\-)?(?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$") //
                                                        .match("^.*DevKurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)\\/.*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTaxesAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Moritz EUR 606,18
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Alter Saldo per 08.01.2024 45.452,58
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Steuern aussch.ttungsgl. Ertr.ge|Steuerdividende) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 10.11 Steuern ausschüttungsgl. Erträge Depot     639007999/20221109-76196684 09.11 5,18-
                        // ISIN IE00BYX2JD69 ISHSIV-MSCI WLD.SRI U.EOA              68,80900 STK
                        // Kurs                      0,000000  KEST                     -3,45 EUR
                        // @formatter:on
                        .section("date", "year", "isin", "name", "shares", "currency")
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (Steuern aussch.ttungsgl. Ertr.ge|Steuerdividende)[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$")
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)[\\s]{1,}(?<shares>[\\.,\\d]+) STK$")
                        .match("^.*KEST[\\s]{1,}\\-[\\.,\\d]+ (?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            v.put("name", replaceMultipleBlanks(v.get("name")));
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setShares(asShares(v.get("shares")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 10.11 Steuern ausschüttungsgl. Erträge Depot     639007999/20221109-76196684 09.11 5,18-
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} (Steuern aussch.ttungsgl. Ertr.ge|Steuerdividende)[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]{4}[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kurs                      0,000000  KEST                     -1,51 USD
                        // DevKurs        1,123200/2.1.2020
                        // @formatter:on
                        .section("termCurrency", "exchangeRate").optional() //
                        .match("^(?i).*KEST[\\s]{1,}\\-[\\.,\\d]+ (?<termCurrency>[\\w]{3})$") //
                        .match("^.*DevKurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)\\/.*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = t.getMonetaryAmount();
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);
    }

    private void addTaxLossAdjustmentAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Moritz EUR 606,18
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Alter Saldo per 08.01.2024 45.452,58
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} KESt\\-Verlustausgleich .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 15.10 KESt-Verlustausgleich            Depot    7806000200/20211014-41350584 18.10 159,57
                        // ISIN US92556H2067 VIACOMCBS INC. BDL-,001
                        // KEST                    159,57 EUR
                        //
                        // 23.11 KESt-Verlustausgleich            Depot    8607012549/20231123-42573067 27.11 461,29
                        // ISIN US7672921050 RIOT PLATFORMS    DL-,001
                        // Kapitalertragsteuer     461,29 EUR
                        // @formatter:on
                        .section("date", "note", "isin", "name", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>KESt\\-Verlustausgleich)[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?i).*(KEST|Kapitalertragsteuer)[\\s]{1,}[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            v.put("name", replaceMultipleBlanks(v.get("name")));
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setShares(0L);

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 15.10 KESt-Verlustausgleich            Depot    7806000200/20211014-41350584 18.10 159,57
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2} KESt\\-Verlustausgleich[\\s]{1,}Depot[\\s]{1,}[\\d]+\\/[\\d]{4}[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addInterestAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Saldo per 20.09.2021 8,27-
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}\\.(?<year>[\\d]{4}) .*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Mustermann EUR 10,81-
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)") //
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} Abschluss .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 31.03 Abschluss 31.03 7,26-
                                        // Sollzinsen
                                        // AB 2021-01-01          3,9000%               4,76-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$") //
                                                        .find("^(?<note>Sollzinsen)$") //
                                                        .match("^AB [\\d]{4}\\-[\\d]{1,2}\\-[\\d]{1,2}[\\s]{1,}[\\.,\\d]+%[\\s]{1,}(?<amount>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 30.09 Abschluss 30.09 2,54-
                                        // Sollzinsen                                   0,01-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$") //
                                                        .match("^(?<note>Sollzinsen)[\\s]{1,}(?<amount>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }))


                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addFeesAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Saldo per 20.09.2021 8,27-
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}\\.(?<year>[\\d]{4}) .*$")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Mustermann EUR 10,81-
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Abschluss|Depotgeb.hrenabrechnung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 31.03 Abschluss 31.03 7,26-
                                        // Kontoführungsgebühr                          2,50-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$") //
                                                        .match("^(?<note>Kontof.hrungsgeb.hr)[\\s]{1,}(?<amount>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 30.06 Abschluss 30.06 2,53-
                                        // Spesen                                       2,53-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$") //
                                                        .match("^(?<note>Spesen)[\\s]{1,}(?<amount>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 07.01 Depotgebührenabrechnung per 31.12.2020  20210106  12345678 31.12 63,68-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "year", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>Depotgeb.hrenabrechnung per [\\d]{1,2}\\.[\\d]{1,2}.[\\d]{4})[\\s]{1,}(?<year>[\\d]{4})[\\d]+[\\s]{1,}[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)\\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addDepositRemovalAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Saldo per 20.09.2021 8,27-
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}\\.(?<year>[\\d]{4}) .*$")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // Max Mustermann EUR 10,81-
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren (Gunsten|Lasten)")
                                        .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 18.06 Max Muster 19.06 100,00
                                        // IBAN: DE17 1234 1234 1234 1234 12
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>(?!(Sollzins ab|Information gem.ß)).*) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$") //
                                                        .match("^(IBAN: .*|Transfer)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 31.10 Werbebonus 31.10 75,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>Werbebonus) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 18.06 Max Muster 19.06 100,00-
                                        // IBAN: DE17 1234 1234 1234 1234 12
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContext("year", "currency") //
                                                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>(?!(Sollzins ab|Information gem.ß)).*) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)\\-$") //
                                                        .match("^(IBAN: .*|Transfer)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }))

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
                        // QUELLENSTEUER: -1,86 USD
                        // Quellensteuer: -4,13 USD
                        // Quellensteuer: -150,-- NOK
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^(?i).*QUELLENSTEUER:[\\s]{1,}\\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // QUELLENSTEUER -15,60 USD Auslands-KESt -13,00 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^(?i).*QUELLENSTEUER[\\s]{1,}\\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Auslands-KESt: -1,54 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(?i).*Auslands\\-KESt:[\\s]{1,}\\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // QUELLENSTEUER -3,77 USD Auslands-KESt -3,13 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(?i).*Auslands\\-KESt[\\s]{1,}\\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KEST -140,27 USD Handelsspesen -5,07 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(?i)KEST[\\s]{1,}\\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Handelsspesen -3,66 EUR DADAT Handelsspesen -6,36 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*  DADAT Handelsspesen ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // DADAT Handelsspesen -1,67 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^DADAT Handelsspesen ([\\s]+)?\\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // KEST -140,27 USD Handelsspesen -5,07 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*  Handelsspesen ([\\s]+)?\\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsspesen -3,66 EUR DADAT Handelsspesen -6,36 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Handelsspesen ([\\s]+)?\\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Clearing Gebühr -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Clearing Geb.hr ([\\s]+)?\\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // DADAT Handelsspesen -7,12 EUR Clearing Gebühr -1,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*  Clearing Geb.hr ([\\s]+)?\\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

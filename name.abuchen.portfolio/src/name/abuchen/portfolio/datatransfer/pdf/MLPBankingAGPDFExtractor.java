package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;

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
public class MLPBankingAGPDFExtractor extends AbstractPDFExtractor
{
    public MLPBankingAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("MLP Banking AG");
        addBankIdentifier("MLP FDL AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MLP Banking AG";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf)$");
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
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
                        // INHABER-ANTEILE A O.N
                        // Ausführungskurs 20,29 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
                        // @formatter:on
                        .section("shares").optional() //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag 14.01.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 100,01- EUR
                        // Ausmachender Betrag 0,79 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs (EUR/USD) 1,141 vom 18.02.2022
                        // Kurswert 24.013,85 EUR
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

                        // @formatter:off
                        // Veräußerungsverlust 3,00- EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Ver.ußerungsverlust [\\.,\\d]+\\- [\\w]{3})$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            BuySellEntryItem item = new BuySellEntryItem(t);

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
        final DocumentType type = new DocumentType("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift|Thesaurierung von Investmentertr.gen)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift|Thesaurierung von Investmentertr.gen)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // If we have a positive amount and a gross
                        // reinvestment, there is a tax refund.
                        // If the amount is negative, then it is taxes.
                        // @formatter:on
                        .section("type", "sign").optional() //
                        .match("^.* (?<type>(Aussch.ttung|Dividende|Ertrag|Thesaurierung brutto)) pro (St\\.|St.ck) [\\.,\\d]+ [\\w]{3}$") //
                        .match("^Ausmachender Betrag [\\.,\\d]+(?<sign>(\\+|\\-))? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if ("Thesaurierung brutto".equals(v.get("type")) && "+".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            if ("Thesaurierung brutto".equals(v.get("type")) && "-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        // @formatter:off
                        // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                        // REGISTERED SHARES USD O.N.
                        // Zahlbarkeitstag 29.12.2017 Ertrag pro St. 0,123000000 USD
                        // @@formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("(?<name1>.*)") //
                        .match("^(Zahlbarkeitstag|Tag des Zuflusses) .* pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Zahlbarkeitstag") || !v.get("name1").startsWith("Tag"))
                                v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 03.01.2018 zu Gunsten des Kontos xxxxxxxxxx (IBAN DExx xxxx xxxx xxxx
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 68,87+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\+|\\-)? (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD 1,2095
                        // Ausschüttung 113,16 USD 93,56+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^(Aussch.ttung|Dividendengutschrift) (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);

                addTaxesSectionsTransaction(pdfTransaction, type);
                addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Verm.gensdepot Konto", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // EUR-Konto Kontonummer 192837465
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[\\w]{3})\\-Konto Kontonummer .*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Postfach 1379, 69154 Wiesloch Kontoauszug Nr.  1/2022
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.* Kontoauszug ([\\s]+)?Nr\\. ([\\s]+)?[\\d]{1,2}\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Block depositRemovalblock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. LASTSCHRIFTEINR\\. .* [S|H]$");
        type.addBlock(depositRemovalblock);
        depositRemovalblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("day", "month", "note", "amount", "type") //
                        .documentContext("year", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (?<note>LASTSCHRIFTEINR\\.) .* (?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                        .assign((t, v) -> {
                            // Is type --> "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("LASTSCHRIFTEINR.".equals(v.get("note")))
                                v.put("note", "Lastschrifteinr.");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block feeblock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. (ENTGELT|EINZUGSERMAECHTIGUNG|GUTSCHRIFT) .* [S|H]$");
        type.addBlock(feeblock);
        feeblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("day", "month", "amount", "type", "note1", "note2") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) [\\d]+ (?<note2>[\\w]{2}\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // @formatter:off
                                                            // Formatting some notes
                                                            // @formatter:on
                                                            if ("DEPOTPREIS".equals(v.get("note1")))
                                                                v.put("note1", "Depotpreis");

                                                            if ("DEPOTENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Depotentgelt");

                                                            if ("VERWALTUNGSENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Verwaltungsentgeld");

                                                            if ("VERMOEGENSDEPOT".equals(v.get("note1")))
                                                                v.put("note1", "Vermögensdepot");

                                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                                        }),
                                        section -> section //
                                                        .attributes("day", "month", "amount", "type", "note1", "note2", "note3") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) VERW\\.G\\. (?<note2>[\\d]{4}) (?<note3>QUARTAL .*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // @formatter:off
                                                            // Formatting some notes
                                                            // @formatter:on
                                                            if ("DEPOTPREIS".equals(v.get("note1")))
                                                                v.put("note1", "Depotpreis");

                                                            if ("DEPOTENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Depotentgelt");

                                                            if ("VERWALTUNGSENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Verwaltungsentgeld");

                                                            if ("VERMOEGENSDEPOT".equals(v.get("note1")))
                                                                v.put("note1", "Vermögensdepot");

                                                            if ("QUARTAL I".equals(v.get("note3")))
                                                                v.put("note3", "Q1/");

                                                            if ("QUARTAL II".equals(v.get("note3")))
                                                                v.put("note3", "Q2/");

                                                            if ("QUARTAL III".equals(v.get("note3")))
                                                                v.put("note3", "Q3/");

                                                            if ("QUARTAL IV".equals(v.get("note3")))
                                                                v.put("note3", "Q4/");

                                                            t.setNote(v.get("note1") + " " + v.get("note3")
                                                                            + v.get("note2"));
                                                        }),
                                        section -> section //
                                                        .attributes("day", "month", "amount", "type", "note1", "note2", "note3") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) (?<note2>[\\d]{4}) (?<note3>QUARTAL .*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // @formatter:off
                                                            // Formatting some notes
                                                            // @formatter:on
                                                            if ("DEPOTPREIS".equals(v.get("note1")))
                                                                v.put("note1", "Depotpreis");

                                                            if ("DEPOTENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Depotentgelt");

                                                            if ("VERWALTUNGSENTGELT".equals(v.get("note1")))
                                                                v.put("note1", "Verwaltungsentgeld");

                                                            if ("VERMOEGENSDEPOT".equals(v.get("note1")))
                                                                v.put("note1", "Vermögensdepot");

                                                            if ("QUARTAL I".equals(v.get("note3")))
                                                                v.put("note3", "Q1/");

                                                            if ("QUARTAL II".equals(v.get("note3")))
                                                                v.put("note3", "Q2/");

                                                            if ("QUARTAL III".equals(v.get("note3")))
                                                                v.put("note3", "Q3/");

                                                            if ("QUARTAL IV".equals(v.get("note3")))
                                                                v.put("note3", "Q4/");

                                                            t.setNote(v.get("note1") + " " + v.get("note3")
                                                                            + v.get("note2"));
                                                        }),
                                        section -> section //
                                                        .attributes("day", "month", "amount", "type", "note1", "note2") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. GUTSCHRIFT .* (?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                                                        .match("^.* (?<note1>ERSTATTUNG VERTRIEBSFOLGEPROVISION).*$") //
                                                        .match("^.* (?<note2>Q[\\d]\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // @formatter:off
                                                            // Formatting some notes
                                                            // @formatter:on
                                                            if ("ERSTATTUNG VERTRIEBSFOLGEPROVISION".equals(v.get("note1")))
                                                                v.put("note1", "Erstattung Vertriebsfolgeprovision");

                                                            t.setNote(v.get("note1") + " " + v.get("note2"));
                                                        }))

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
                        // Ausmachender Betrag 0,79 EUR
                        // Den Gegenwert buchen wir mit Valuta 03.03.2022 zu Gunsten des Kontos 1111111111111
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
        transaction

                        // @formatter:off
                        // Kapitalertragsteuer 25,00% auf 2.809,62 EUR 702,41- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,50% auf 702,41 EUR 38,63- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 8 % auf 2,70 EUR 0,21- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // Ihr Ausgabeaufschlag betraegt:
                        // 0,00 EUR (0,000 Prozent)
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) \\([\\.,\\d]+ Prozent\\)$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

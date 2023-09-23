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
        DocumentType type = new DocumentType("Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // Handshake for tax refund transaction
        Map<String, String> context = type.getCurrentContext();

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
                // INHABER-ANTEILE A O.N
                // Ausführungskurs 20,29 EUR
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));

                    // Handshake, if there is a tax refund
                    context.put("name", v.get("name"));
                    context.put("isin", v.get("isin"));
                    context.put("wkn", v.get("wkn"));
                })

                // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
                .section("shares").optional()
                .match("^St.ck (?<shares>[\\.,\\d]+) .* [\\w]{12} \\(.*\\)$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));

                    // Handshake, if there is a tax refund
                    context.put("shares", v.get("shares"));
                })

                // Schlusstag 14.01.2021
                .section("date")
                .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Ausmachender Betrag 100,01- EUR
                // Ausmachender Betrag 0,79 EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ausführungskurs 167,82 USD
                // Devisenkurs (EUR/USD) 1,141 vom 18.02.2022
                // Kurswert 24.013,85 EUR
                .section("fxCurrency", "baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional()
                .match("^Ausf.hrungskurs [\\.,\\d]+ (?<fxCurrency>[\\w]{3})")
                .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$")
                .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // Veräußerungsverlust 3,00- EUR
                .section("note").optional()
                .match("^(?<note>Ver.ußerungsverlust [\\.,\\d]+\\- [\\w]{3})$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(context, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift|Thesaurierung von Investmentertr.gen)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Gutschrift von .*|Aussch.ttung Investmentfonds|Dividendengutschrift|Thesaurierung von Investmentertr.gen)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // If we have a positive amount and a gross
                // reinvestment, there is a tax refund.
                // If the amount is negative, then it is taxes.
                .section("type", "sign").optional()
                .match("^.* (?<type>(Aussch.ttung|Dividende|Ertrag|Thesaurierung brutto)) pro (St\\.|St.ck) [\\.,\\d]+ [\\w]{3}$")
                .match("^Ausmachender Betrag [\\.,\\d]+(?<sign>(\\+|\\-))? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("Thesaurierung brutto".equals(v.get("type")) && "+".equals(v.get("sign")))
                        t.setType(AccountTransaction.Type.TAX_REFUND);

                    if ("Thesaurierung brutto".equals(v.get("type")) && "-".equals(v.get("sign")))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                // REGISTERED SHARES USD O.N.
                // Zahlbarkeitstag 29.12.2017 Ertrag pro St. 0,123000000 USD
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("(?<name1>.*)")
                .match("^(Zahlbarkeitstag|Tag des Zuflusses) "
                                + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                + "(Aussch.ttung|Dividende|Ertrag|Thesaurierung brutto) pro (St\\.|St.ck) [\\.,\\d]+ "
                                + "(?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag") || !v.get("name1").startsWith("Tag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 920 ISHSIV-FA.AN.HI.YI.CO.BD U.ETF IE00BYM31M36 (A2AFCX)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Den Betrag buchen wir mit Wertstellung 03.01.2018 zu Gunsten des Kontos xxxxxxxxxx (IBAN DExx xxxx xxxx xxxx
                .section("date")
                .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag 68,87+ EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\+|\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Devisenkurs EUR / USD 1,2095
                // Ausschüttung 113,16 USD 93,56+ EUR
                .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional()
                .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .match("^(Aussch.ttung|Dividendengutschrift) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
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

                        .section("day", "month", "note", "amount", "sign") //
                        .documentContext("year", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (?<note>LASTSCHRIFTEINR\\.) .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                        .assign((t, v) -> {
                            // Is sign --> "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("sign")))
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
                                                        .attributes("day", "month", "amount", "sign", "note1", "note2") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) [\\d]+ (?<note2>[\\w]{2}\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("sign")))
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
                                                        .attributes("day", "month", "amount", "sign", "note1", "note2", "note3") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) VERW\\.G\\. (?<note2>[\\d]{4}) (?<note3>QUARTAL .*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("sign")))
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
                                                        .attributes("day", "month", "amount", "sign", "note1", "note2", "note3") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (ENTGELT|EINZUGSERMAECHTIGUNG) .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                                                        .match("^.* (?<note1>(DEPOTPREIS|DEPOTENTGELT|VERWALTUNGSENTGELT|VERMOEGENSDEPOT)) (?<note2>[\\d]{4}) (?<note3>QUARTAL .*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("sign")))
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
                                                        .attributes("day", "month", "amount", "sign", "note1", "note2") //
                                                        .documentContext("year", "currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. GUTSCHRIFT .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                                                        .match("^.* (?<note1>ERSTATTUNG VERTRIEBSFOLGEPROVISION).*$") //
                                                        .match("^.* (?<note2>Q[\\d]\\/[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is sign --> "S" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(v.get("sign")))
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

    private void addTaxReturnBlock(Map<String, String> context, DocumentType type)
    {
        Block block = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Ausmachender Betrag 0,79 EUR
                // Den Gegenwert buchen wir mit Valuta 03.03.2022 zu Gunsten des Kontos 1111111111111
                .section("amount", "currency", "date").optional()
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setShares(asShares(context.get("shares")));

                    t.setSecurity(getOrCreateSecurity(context));
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
                // Kapitalertragsteuer 25,00% auf 2.809,62 EUR 702,41- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,50% auf 702,41 EUR 38,63- EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer 8 % auf 2,70 EUR 0,21- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ihr Ausgabeaufschlag betraegt:
                // 0,00 EUR (0,000 Prozent)
                .section("fee", "currency").optional()
                .match("^(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) \\([\\.,\\d]+ Prozent\\)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

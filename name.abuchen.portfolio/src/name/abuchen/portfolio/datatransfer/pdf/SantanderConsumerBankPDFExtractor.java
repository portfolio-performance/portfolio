package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

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
public class SantanderConsumerBankPDFExtractor extends AbstractPDFExtractor
{
    public SantanderConsumerBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Santander Consumer Bank AG");
        addBankIdentifier("Santander Consumer Bank GmbH");

        addBankIdentifier("sa nta nder");
        addBankIdentifier("santander");

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransactionAT();
        addAccountStatementTransactionDE();
    }

    @Override
    public String getLabel()
    {
        return "Santander Consumer Bank";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Santander Consumer Bank.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Stück 2 3M CO. US88579Y1010 (851745)
                        // REGISTERED SHARES DL -,01
                        // Kurswert 317,96- EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (\\((?<wkn>[A-Z0-9]{6})\\))$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Kurswert [\\.,\\d]+\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 13 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 17.03.2021 16:53:45 Auftraggeber NACHNAME VORNAME
                        // @formatter:off
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 325,86- EUR
                        // @formatter:off
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        //  Auftragsnummer 000000/00.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit billigst
                        // @formatter:off
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Santander Consumer Bank.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                        // Stück 2 3M CO. US88579Y1010 (851745)
                        // REGISTERED SHARES DL -,01
                        // Zahlbarkeitstag 14.06.2021 Dividende pro Stück 1,48 USD
                        //
                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                        // Stück 495 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                        // REGISTERED SHARES USD DIS.ON
                        // Zahlbarkeitstag 28.06.2023 Ausschüttung pro St. 0,727274000 USD
                        // @formatter:off
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (\\((?<wkn>[A-Z0-9]{6})\\))$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Zahlbarkeitstag .* [\\.,\\d]+ (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 2 3M CO. US88579Y1010 (851745)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 16.06.2021 zu Gunsten des Kontos 0000000000 (IBAN XX00 0000 0000 0000
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 2,07+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD 1,2137
                        // Devisenkursdatum 14.06.2021
                        // Dividendengutschrift 2,96 USD 2,44+ EUR
                        //
                        // Devisenkurs EUR / USD  1,0977
                        // Devisenkursdatum 29.06.2023
                        // Ausschüttung 360,00 USD 327,96+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+)\\+ [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        //  Abrechnungsnr. 00000000000
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Ex-Tag 20.05.2021 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransactionAT()
    {
        final var type = new DocumentType("Kontoauszug", "KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // € 0,00 € 37,98 € 40,64 € 2,66 € 6,63 € 1,66
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>\\p{Sc}) [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ .*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 31.05.2023 31.05.2023 6,63 40,64 Zinsgutschrift Habenzinsen
        // 31.05.2023 31.05.2023 -1,66 37,98 Kapitalertragsteuer
        //
        // 30.11.2025 30.11.2025 18,29 44.290,01 Zinsgutschrift Habenzinsen
        // 30.11.2025 30.11.2025 -4,57 44.285,44 Kapitalertragsteuer
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ Zinsgutschrift.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) [\\.,\\d]+ Zinsgutschrift (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(replaceMultipleBlanks(v.get("note")));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<tax>[\\.,\\d]+) [\\.,\\d]+ Kapitalertrags(s)?teuer.*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<tax>[\\.,\\d]+) [\\.,\\d]+ Solidarit.tszuschlag.*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-)?(?<tax>[\\.,\\d]+) [\\.,\\d]+ Kirchensteuer.*$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 19.05.2023 19.05.2023 34,00 34,01 Einzahlung von ...
        // @formatter:on
        var depositBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [\\.,\\d]+ Einzahlung.*$");
        type.addBlock(depositBlock_Format01);
        depositBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) [\\.,\\d]+ Einzahlung (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(replaceMultipleBlanks(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Einzahlung von Konto  AT123456789012345678 lautend auf Max
        // 30.01.2026 30.01.2026 20.140,96 31.769,71
        // Mustermann, BKm
        //
        // Einzahlung von Konto Lr976088571958963679 lautend auf
        // 24.11.2025 24.11.2025 8,50 38.271,72 VOYfxcgsvJX BkOWWv
        // xnqYrAv afHFrgoYY
        // @formatter:on
        var depositBlock_Format02 = new Block("^Einzahlung von Konto .* lautend auf.*$");
        type.addBlock(depositBlock_Format02);
        depositBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note>Einzahlung von Konto .*) lautend auf.*$")
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) [\\.,\\d]+.*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(replaceMultipleBlanks(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 20.05.2023 20.05.2023 -1,00 33,01 Auszahlung auf ...
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-[\\.,\\d]+ [\\.,\\d]+ Auszahlung .*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-(?<amount>[\\.,\\d]+) [\\.,\\d]+ Auszahlung (?<note>.*)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(replaceMultipleBlanks(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addAccountStatementTransactionDE()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Ihre IBAN: DE93  3 1 01  083 3  2 2 64  01 3 1  2 0 BIC: SCFBDE3 3 Ko nto -Nr.  2 2 64 01 3 1 2 0 EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("Ihre IBAN: .* (?<currency>\\w{3})$")
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // 04.01 .2 02 2  BIS 02 .01 .2 02 3
                                        // @formatter:on
                                        .section("year") //
                                        .find("[\\s]*DIESER KONTOAUSZUG UMFASST DIE UMS.TZE VOM")
                                        .match("^[\\s]*[\\d]{2}[\\s]?\\.[\\d]{2}[\\s]?\\.(?<year>[\\d][\\s]?[\\d]{2}[\\s]?[\\d]).*BIS.*$") //
                                        .assign((ctx, v) -> ctx.put("year", stripBlanks(v.get("year")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 31.03. 31.03. 5,87 ÜBERWEISUNG VON MAX MUSTER 32930011633BBDBMFG IBAN
        // @formatter:on
        var depositBlock = new Block("^[\\d].*[\\s]*.BERWEISUNG VON .*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("year", "currency") //
                        .match("^.*[\\s]*(?<date>[\\d][\\s]?[\\d][\\s]?\\.[\\d][\\s]?[\\d][\\s]?\\.)[\\s]*(?<amount>[\\.,\\d]+)[\\s]*(?<note>.BERWEISUNG).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("date")) + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(stripBlanks(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 02 .01 .  3 0.1 2 . 0,01       Ha benzinsen 0,01
        // @formatter:on
        var interestBlock = new Block("^[\\d].*[\\s]*Ha[\\s]?benzinsen.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("year", "currency") //
                        .match("^.*[\\s]*(?<date>[\\d][\\s]?[\\d][\\s]?\\.[\\d][\\s]?[\\d][\\s]?\\.)[\\s]*(?<amount>[\\.,\\d]+)[\\s]*(?<note>Ha[\\s]?benzinsen).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("date")) + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(stripBlanks(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 2,96 USD 0,37- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\d]+ % .* [\\.,\\d]+ [A-Z]{3} (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 2,44 EUR 0,37 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\d]+ % .* [\\.,\\d]+ [A-Z]{3} (?<creditableWithHoldingTax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // Kapitalertragsteuer 24,45 % auf 254,18 EUR 62,15- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 62,15 EUR 3,41- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 9 % auf 62,15 EUR 5,59- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                 // @formatter:off
                 // Provision 7,90- EUR
                 // @formatter:on
                .section("fee", "currency").optional() //
                .match("^Provision (?<fee>[.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Fremde Abwicklungsgebühr für die Umschreibung von Namensaktien 0,60- EUR
                // @formatter:on
                .section("fee", "currency").optional() //
                .match("^Fremde Abwicklungsgeb.hr .* (?<fee>[.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

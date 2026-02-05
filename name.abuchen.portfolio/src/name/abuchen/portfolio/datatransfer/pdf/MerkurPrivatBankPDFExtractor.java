package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;


@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractor extends AbstractPDFExtractor
{
    public MerkurPrivatBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Am Marktplatz 10");
        addBankIdentifier("97762 Hammelburg");

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
        addAdvanceTaxTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MERKUR PRIVATBANK KGaA";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*Hammelburg.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Verkauf" change from BUY to SELL
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // REGISTERED SHARES 1C O.N.
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 09.05.2023 09:26:37 Auftraggeber jvDDiy QfmZL LloZHJy
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 47.811,46- EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Auftragsnummer 284722/61.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxesSectionsTransaction(pdfTransaction, type);

    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("(Ertragsgutschrift nach" //
                        + "|Aussch.ttung Investmentfonds" //
                        + "|Dividendengutschrift)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Hammelburg.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 55 DEUTSCHE TELEKOM AG DE0005557508 (555750)
                        // NAMENS-AKTIEN O.N.
                        // Zahlbarkeitstag 15.04.2024 Ertrag  pro Stück 0,77 EUR
                        //
                        // Stück 0,9854 CANADIAN NATIONAL RAILWAY CO. CA1363751027 (897879)
                        // REGISTERED SHARES O.N.
                        // Zahlbarkeitstag 29.09.2025 Dividende pro Stück 0,8875 CAD
                        // @formatter:on
                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.* (Ertrag|Aussch.ttung|Dividende)[\\s]{1,}pro St.*? [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 55 DEUTSCHE TELEKOM AG DE0005557508 (555750)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbarkeitstag 15.04.2024 Ertrag  pro Stück 0,77 EUR
                        // Zahlbarkeitstag 29.09.2025 Dividende pro Stück 0,8875 CAD
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 42,35+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD  1,1802
                        // Devisenkursdatum 02.10.2025
                        // Ausschüttung 1,63 USD 1,38+ EUR
                        //
                        // Devisenkurs EUR / CAD  1,6415
                        // Devisenkursdatum 30.09.2025
                        // Dividendengutschrift 0,87 CAD 0,53+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^(Aussch.ttung|Dividendengutschrift) (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+)\\+ [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        //  Abrechnungsnr. 60338188850
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType(".*\\-Konto Kontonummer", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug Nr.  1/2024
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>\\d{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // EUR-Konto Kontonummer 4737065
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* PN:\\d+[\\s]{1,}[\\.,\\d]+ [H|S]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>.*) PN:\\d+[\\s]{1,}(?<amount>[\\.,\\d]+) (?<type>[H|S])$") //
                        .assign((t, v) -> {
                            // Is type is "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("type")))
                                t.setType(Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            // Formatting some notes
                            if ("DAUERAUFTRAG".equals(v.get("note")))
                                v.put("note", "Dauerauftrag");

                            if ("GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "Gutschrift");

                            if ("FESTGELDANLAGE".equals(v.get("note")))
                                v.put("note", "Festgeldanlage");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addAdvanceTaxTransaction()
    {
        final var type = new DocumentType("Vorabpauschale Investmentfonds");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Abrechnungsnr.*$", "^Keine Steuerbescheinigung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("name", "isin", "wkn", "nameContinued", "currency") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^.* Vorabpauschale pro St\\. [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            var item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 25,00% auf 12.960,18 EUR 3.240,05- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s?)teuer [\\.,\\d]+[\\s]*% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,50% auf 3.240,05 EUR 178,20- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+[\\s]*% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 914,50 EUR
                        // Einbehaltene Quellensteuer 25 % auf 0,87 CAD 0,13- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional()
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+[\\s]*% .* (?<withHoldingTax>[\\.,\\d]+)(\\-)? (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer pro Stück 0,0465 EUR 137,18 EUR
                        // Anrechenbare Quellensteuer 15 % auf 0,53 EUR 0,08 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional()
                        .match("^Anrechenbare Quellensteuer pro St.ck [\\.,\\d]+ [A-Z]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 25,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,06- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

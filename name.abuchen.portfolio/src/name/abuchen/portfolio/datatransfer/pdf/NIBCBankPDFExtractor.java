package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class NIBCBankPDFExtractor extends AbstractPDFExtractor
{
    public NIBCBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("NIBC Direct");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "NIBC Bank N.V.";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        // Map for tax lost adjustment transaction
        var context = type.getCurrentContext();

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 13 VANGUARD FTSE ALL-WORLD U.ETF      IE00B3RBWM25 (A1JX52)
                        // REGISTERED SHARES USD DIS.ON
                        // Handels-/Ausführungsplatz Quotrix (gemäß Weisung)
                        // Kurswert 1.029,99- EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Kurswert [\\.,\\d]+(\\-)? (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausf"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 13 VANGUARD FTSE ALL-WORLD U.ETF      IE00B3RBWM25 (A1JX52)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 13.01.2020 11:42:59 Auftraggeber Vornamen Nachnamen
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 1.042,34- EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-\\+])? (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Limit 79,23 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Limit|Stoplimit) .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            var item = new BuySellEntryItem(t);

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
        final var type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift .*)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Stück 100 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                        // REGISTERED SHARES USD DIS.ON
                        // Zahlbarkeitstag 27.12.2019 Ausschüttung pro St. 0,297309000 USD
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^.* ((Dividende|Ertrag) ([\\s]+)?pro St.ck|Aussch.ttung pro St\\.) [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 100 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Den Betrag buchen wir mit Wertstellung 31.12.2019 zu Gunsten des Kontos 8000000000 (IBAN DE12 0000 0000 8000
                        // @formatter:on
                        .section("date") //
                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ex-Tag 12.12.2019 fonds) 0,208116300 USD
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ex\\-Tag (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Ausmachender Betrag 24,03+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD  1,1227
                        // Ausschüttung 29,73 USD 26,48+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[A-Z]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            var fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Ex-Tag 26.02.2021 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxLostAdjustmentTransaction(Map<String, String> context, DocumentType type)
    {
        var firstRelevantLine = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // Ausmachender Betrag 29,57 EUR
                        // Den Gegenwert buchen wir mit Valuta 13.08.2019 zu Gunsten des Kontos 5052258000
                        // @formatter:on
                        .section("amount", "currency", "date").optional() //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(Long.parseLong(context.get("shares")));
                            t.setSecurity(getOrCreateSecurity(context));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Verrechnete Aktienverluste 112,10- EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Verrechnete Aktienverluste .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            // We have no test case for this tax refund, however,
                            // probably this is part of sales document. The
                            // additional tax refund is created only if the sale
                            // realizes a loss.
                            return null;
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Finanztransaktionssteuer 10,10- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Finanztransaktionssteuer (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer 25 % auf 7,55 EUR 1,89- EUR
                        // Kapitalertragsteuer 25,00% auf 143,95 EUR 35,99- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 1,89 EUR 0,11- EUR
                        // Solidaritätszuschlag 5,50% auf 35,99 EUR 1,98- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 7 % auf 2,89 EUR 2,11- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 19 % auf 85,00 PLN 3,59- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 18,87 EUR 2,83 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                        // @formatter:off
                        // abgeführte Kapitalertragsteuer 0,00
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^abgef.hrte Kapitalertragsteuer (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                            processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // abgeführte Kirchensteuer 0,00
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^abgef.hrte Kirchensteuer (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                            processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 1,0000 % vom Kurswert 50,48- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 9,95- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 0,71- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,07- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

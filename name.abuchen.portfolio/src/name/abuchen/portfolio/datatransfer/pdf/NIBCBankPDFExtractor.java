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
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        // Map for tax lost adjustment transaction
        Map<String, String> context = type.getCurrentContext();

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Stück 13 VANGUARD FTSE ALL-WORLD U.ETF      IE00B3RBWM25 (A1JX52)
                // REGISTERED SHARES USD DIS.ON
                // ACCIONES NOM. EO -,10
                // Handels-/Ausführungsplatz Quotrix (gemäß Weisung)
                // Kurswert 1.029,99- EUR
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert [\\.,\\d]+([\\-])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 13 VANGUARD FTSE ALL-WORLD U.ETF      IE00B3RBWM25 (A1JX52)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))


                // Schlusstag/-Zeit 13.01.2020 11:42:59 Auftraggeber Vornamen Nachnamen
                .section("date", "time")
                .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Ausmachender Betrag 1.042,34- EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Limit 79,23 EUR
                .section("note").optional()
                .match("^(?<note>(Limit|Stoplimit) .*)$")
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
        DocumentType type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift .*)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 100 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                // REGISTERED SHARES USD DIS.ON
                // Zahlbarkeitstag 27.12.2019 Ausschüttung pro St. 0,297309000 USD
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^.* ((Dividende|Ertrag) ([\\s]+)?pro St.ck|Aussch.ttung pro St\\.) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 100 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Den Betrag buchen wir mit Wertstellung 31.12.2019 zu Gunsten des Kontos 8000000000 (IBAN DE12 0000 0000 8000
                .section("date")
                .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag 24,03+ EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Devisenkurs EUR / USD  1,1227
                // Ausschüttung 29,73 USD 26,48+ EUR
                .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional()
                .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$")
                .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // Ex-Tag 26.02.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addTaxLostAdjustmentTransaction(Map<String, String> context, DocumentType type)
    {
        Block block = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Ausmachender Betrag 29,57 EUR
                // Den Gegenwert buchen wir mit Valuta 13.08.2019 zu Gunsten des Kontos 5052258000
                .section("amount", "currency", "date").optional()
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setShares(Long.parseLong(context.get("shares")));
                    t.setSecurity(getOrCreateSecurity(context));

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Verrechnete Aktienverluste 112,10- EUR
                .section("note").optional()
                .match("^(?<note>Verrechnete Aktienverluste .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Finanztransaktionssteuer 10,10- EUR
                .section("tax", "currency").optional()
                .match("^Finanztransaktionssteuer (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25 % auf 7,55 EUR 1,89- EUR
                // Kapitalertragsteuer 25,00% auf 143,95 EUR 35,99- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 1,89 EUR 0,11- EUR
                // Solidaritätszuschlag 5,50% auf 35,99 EUR 1,98- EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer 7 % auf 2,89 EUR 2,11- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\d.]+,\\d+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 19 % auf 85,00 PLN 3,59- EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 18,87 EUR 2,83 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // abgeführte Kapitalertragsteuer 0,00
                .section("tax").optional()
                .match("^abgef.hrte Kapitalertragsteuer (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                    processTaxEntries(t, v, type);
                })

                // abgeführte Kirchensteuer 0,00
                .section("tax").optional()
                .match("^abgef.hrte Kirchensteuer (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 1,0000 % vom Kurswert 50,48- EUR
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision 9,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 0,71- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,07- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

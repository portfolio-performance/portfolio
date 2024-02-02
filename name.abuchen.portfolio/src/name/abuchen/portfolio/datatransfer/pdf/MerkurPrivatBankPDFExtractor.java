package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.function.BiConsumer;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractor extends AbstractPDFExtractor
{

    private static final String ACCOUNT_DEPOSIT = "^(?<date>\\d+.\\d+.) (\\d+.\\d+.) (GUTSCHRIFT|DAUERAUFTRAG|Neuanlage) (PN:\\d+)(\\s*)(?<amount>[\\d\\s,.]*) [H]";
    private static final String ACCOUNT_REMOVAL = "^(?<date>\\d+.\\d+.) (\\d+.\\d+.) (.*BERWEISUNG.*|FESTGELDANLAGE) (PN:\\d+)(\\s*)(?<amount>[\\d\\s,.]*) [S]";

    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";

    public MerkurPrivatBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Umsatzsteuer-ID DE198159260");
        addBankIdentifier("MERKUR PRIVATBANK KGaA");

        addBuySellTransaction();
        addAccountTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MERKUR PRIVATBANK KGaA";
    }

    private void addAccountTransaction()
    {
        final DocumentType type = new DocumentType(".*-Konto Kontonummer", //
                        documentContext -> documentContext //
                                        .section("year").match("(.*)(Kontoauszug Nr\\.)(\\s*)(\\d+)\\/(?<year>\\d{4})")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        .section("currency").match("(?<currency>[\\w]{3})(-Konto Kontonummer)(.*)")
                                        .assign((ctx, v) -> ctx.put(CONTEXT_KEY_CURRENCY,
                                                        asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        Block depositBlock = new Block(ACCOUNT_DEPOSIT);
        depositBlock.set(depositTransaction(type, ACCOUNT_DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(ACCOUNT_REMOVAL);
        removalBlock.set(removalTransaction(type, ACCOUNT_REMOVAL));
        type.addBlock(removalBlock);
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

        // @formatter:off
        // Wertpapier Abrechnung Kauf
        // @formatter:on
        Block firstRelevantLine = new Block("^Am Marktplatz.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction // @formatter:off
                        // Wertpapier Abrechnung Verkauf
                        // or
                        // Wertpapier Abrechnung Kauf
                        // @formatter:on
                        .section("type").match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*").assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // REGISTERED SHARES 1C O.N.
                        // @formatter:on
                        .section("name", "name1", "isin", "wkn", "currency")
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                        .match("^(?<name1>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$").assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 600 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag/-Zeit 09.05.2023 09:26:37 Auftraggeber jvDDiy QfmZL LloZHJy
                        // @formatter:on
                        .section("date", "time")
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 47.811,46- EUR
                        // @formatter:on
                        .section("amount", "currency")
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
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

    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction // @formatter:off
                        // Provision 25,00- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,06- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));

    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            String date = matcherMap.get("date");

            date += context.get(CONTEXT_KEY_YEAR);

            transaction.setDateTime(asDate(date));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            transaction.setCurrencyCode(asCurrencyCode(context.get(CONTEXT_KEY_CURRENCY)));
        };
    }
}

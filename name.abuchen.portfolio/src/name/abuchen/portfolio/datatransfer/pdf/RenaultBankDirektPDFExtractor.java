package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class RenaultBankDirektPDFExtractor extends AbstractPDFExtractor
{
    private static final String DEPOSIT_2019 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.)  (.*-Gutschrift)(\\s*)(?<amount>[\\.,\\d]+)[+](\\s*)$";
    private static final String REMOVAL_2019 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.)  (Wertstellung: \\d+.\\d+. )?(Internet-Euro-Überweisung)(\\s*)(?<amount>[\\.,\\d]+)[-](\\s*)$";
    private static final String INTEREST_2019 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.)  (.*)(Zinsen\\/Kontoführung)(\\s*)(?<amount>[\\.,\\d]+)[+](\\s*)$";

    private static final String DEPOSIT_2021 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (.*gutschr\\.?)(\\s*)(?<amount>[\\.,\\d]+) [H]";
    private static final String REMOVAL_2021 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Umbuchung|.*berweisungsauftrag)(\\s*)(?<amount>[\\.,\\d]+) [S]";
    private static final String INTEREST_2021 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Abschluss)(\\s*)(?<amount>[\\.,\\d]+) [H]";
    private static final String INTEREST_CHARGE_2021 = "^(?<date>\\d+.\\d+.) ([\\d]{2}\\.[\\d]{2}\\.) (Storno Abschluss)(\\s*)(?<amount>[\\.,\\d]+) [S]";

    private static final String DEPOSIT_AT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Zahlungseingang) (\\w+) (?<amount>[\\.,\\d]+) (.*)$";
    private static final String REMOVAL_AT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (.berweisung) (\\w+) (\\-)(?<amount>[\\.,\\d]+) (.*)$";
    private static final String INTEREST_AT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\w+zinsen) (\\w+) (?<amount>[\\.,\\d]+) (.*)";
    private static final String TAXES_AT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Kapitalertragsteuer) (\\w+) (\\-)(?<amount>[\\.,\\d]+) (.*)$";

    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";
    private static final String CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE = "transactions_full_date";
    private static final String CONTEXT_VALUE_TRUE = "true";

    public RenaultBankDirektPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("305 200 37");
        addBankIdentifier("Renault Bank direkt");

        addTransactionWith2019Format();
        addTransactionWith2021Format();
        addTransactionWithATFormat();
    }

    @Override
    public String getLabel()
    {
        return "Renault Bank direkt";
    }

    private void addTransactionWith2019Format()
    {
        final DocumentType type = new DocumentType("305 200 37", //
                        documentContext -> documentContext //
                        // @formatter:off
                                        .section("year")
                                        .match("(.*)(KONTOAUSZUG  Nr. )(\\d+)\\/(?<year>\\d{4})")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))
                                        
                                        .oneOf(
                                                section -> section
                                                .attributes("currency")
                                                .match("(.*)(Summe Zinsen\\/Kontoführung)(\\s*)(?<currency>[\\w]{3})(.*)")
                                                .assign((ctx, v) -> ctx.put(CONTEXT_KEY_CURRENCY, asCurrencyCode(v.get("currency"))))
                                        ,
                                                section -> section
                                                .attributes("currency")
                                                .match("^.*N.*E.*U.*E.*R.*K.*O.*N.*T.*O.*S.*T.*A.*N.*D.*V.*O.*M.*I.*N.*(?<currency>[A-Z_]{5,}).*$")
                                                .assign((t, v) -> {
                                                    t.put(CONTEXT_KEY_CURRENCY, asCurrencyCode(v.get("currency").replace("_", ""))); //
                                                })
                                         )
                        // @formatter:on
                        );
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2019);
        depositBlock.set(depositTransaction(type, DEPOSIT_2019));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2019);
        removalBlock.set(removalTransaction(type, REMOVAL_2019));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST_2019);
        interestBlock.set(interestTransaction(type, INTEREST_2019));
        type.addBlock(interestBlock);
    }

    private void addTransactionWith2021Format()
    {
        final DocumentType type = new DocumentType(".*-Konto Kontonummer", //
                        documentContext -> documentContext //
                                        .section("year")
                                        .match("(.*)(Kontoauszug Nr\\.)(\\s*)(\\d+)\\/(?<year>\\d{4})")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))
                                        
                                        .section("currency")
                                        .match("(?<currency>[\\w]{3})(-Konto Kontonummer)(.*)")
                                        .assign((ctx, v) -> ctx.put(CONTEXT_KEY_CURRENCY, asCurrencyCode(v.get("currency")))
                                        ));
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2021);
        depositBlock.set(depositTransaction(type, DEPOSIT_2021));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2021);
        removalBlock.set(removalTransaction(type, REMOVAL_2021));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST_2021);
        interestBlock.set(interestTransaction(type, INTEREST_2021));
        type.addBlock(interestBlock);

        Block interestChargeBlock = new Block(INTEREST_CHARGE_2021);
        interestChargeBlock.set(interestChargeTransaction(type, INTEREST_CHARGE_2021));
        type.addBlock(interestChargeBlock);
    }

    private void addTransactionWithATFormat()
    {
        final DocumentType type = new DocumentType("IBAN AT.*", //
                        documentContext -> documentContext //
                                        .section("currency")
                                        .match("(.*) Betrag in (?<currency>[\\w]{3}) (.*)")
                                        .assign((t, v) -> {
                                            t.put(CONTEXT_KEY_CURRENCY, asCurrencyCode(v.get("currency").replace("_", ""))); //
                                            t.put(CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE, CONTEXT_VALUE_TRUE);
                                        }));
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_AT);
        depositBlock.set(depositTransaction(type, DEPOSIT_AT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_AT);
        removalBlock.set(removalTransaction(type, REMOVAL_AT));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST_AT);
        interestBlock.set(interestTransaction(type, INTEREST_AT));
        type.addBlock(interestBlock);

        Block taxesBlock = new Block(TAXES_AT);
        taxesBlock.set(taxesTransaction(type, TAXES_AT));
        type.addBlock(taxesBlock);
    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestChargeTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> taxesTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            boolean transactionsHaveFullDate = CONTEXT_VALUE_TRUE.equals(context.get(CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE));

            String date = matcherMap.get("date");

            if (!transactionsHaveFullDate)
            {
                date += context.get(CONTEXT_KEY_YEAR);
            }

            transaction.setDateTime(asDate(date));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            transaction.setCurrencyCode(asCurrencyCode(context.get(CONTEXT_KEY_CURRENCY)));
        };
    }
}

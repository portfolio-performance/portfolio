package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

public class SutorPDFExtractor extends AbstractPDFExtractor
{

    private static final String REGEX_AMOUNT_NEGATIVE = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) -(?<amount>[\\.\\d]+[,\\d]*) .*"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_POSITIVE = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<amount>[\\.\\d]+[,\\d]*) .*"; //$NON-NLS-1$
    private static final String REGEX_DATE = "^(\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) .*"; //$NON-NLS-1$
    private static final String REGEX_SHARES_NEGATIVE = "^.* -(?<shares>[\\.\\d]+[,\\d]*)$"; //$NON-NLS-1$
    private static final String REGEX_SHARES_POSITIVE = "^.* (?<shares>[\\.\\d]+[,\\d]*)$"; //$NON-NLS-1$

    public SutorPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sutor"); //$NON-NLS-1$
        addBankIdentifier("Sutor Bank"); //$NON-NLS-1$

        addDeposit("automatischer Lastschrifteinzug"); //$NON-NLS-1$
        addDeposit("Zulage"); //$NON-NLS-1$
        addBuyTransaction();
        addSellForFeePayment();
        addFeePayment("Verwaltungsgebühr/Vertriebskosten"); //$NON-NLS-1$
        addFeePayment("anteil.Verwaltgebühr/Vertriebskosten"); //$NON-NLS-1$
        addFeePayment("Kontoführungs-u.Depotgebühren"); //$NON-NLS-1$
    }

    @Override
    public String getPDFAuthor()
    {
        return "Sutor Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addDeposit(String depositType)
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* " + depositType);
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("amount").match(REGEX_AMOUNT_POSITIVE).assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            // Sutor always provides the amount in EUR, column
                            // "Betrag in EUR"
                            t.setCurrencyCode(CurrencyUnit.EUR);
                        }).wrap(TransactionItem::new);
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* Kauf .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name").match("^.* Kauf (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount").match(REGEX_AMOUNT_NEGATIVE).assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            // Sutor always provides the amount in EUR, column
                            // "Betrag in EUR"
                            t.setCurrencyCode(CurrencyUnit.EUR);
                        })

                        .section("shares").match(REGEX_SHARES_POSITIVE)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellForFeePayment()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* Gebührentilgung .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name").match("^.* Gebührentilgung (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount").match(REGEX_AMOUNT_POSITIVE).assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            // Sutor always provides the amount in EUR, column
                            // "Betrag in EUR"
                            t.setCurrencyCode(CurrencyUnit.EUR);
                        })

                        .section("shares").match(REGEX_SHARES_NEGATIVE)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeePayment(String feeType)
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* " + feeType + ".*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("amount").match(REGEX_AMOUNT_NEGATIVE).assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            // Sutor always provides the amount in EUR, column
                            // "Betrag in EUR"
                            t.setCurrencyCode(CurrencyUnit.EUR);
                        }).wrap(TransactionItem::new);
    }

    @Override
    public String getLabel()
    {
        return "Sutor Fairriester"; //$NON-NLS-1$
    }
}

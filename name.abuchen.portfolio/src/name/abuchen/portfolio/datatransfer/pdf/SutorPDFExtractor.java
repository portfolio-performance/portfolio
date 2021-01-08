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

    private static final String REGEX_AMOUNT = "^(\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) -?(?<amount>[\\.\\d]+[,\\d]*).*$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_NEW_FORMAT = "^(\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}).*(\\s|-)(?<amount>[\\.\\d]+,\\d{2})$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_AND_SHARES = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<sign>[-])?(?<amount>[\\.\\d]+[,\\d]*) .* -?(?<shares>[\\.\\d]+[,\\d]*)$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_AND_SHARES_NEW_FORMAT = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+)[^,]* -?(?<shares>[\\.\\d]+,\\d{4}) ([\\.\\d]+,\\d{4}\\s)?(?<sign>[-])?(?<amount>[\\.\\d]+,\\d{2})$"; //$NON-NLS-1$
    private static final String REGEX_DATE = "^(\\d+\\.\\d+\\.\\d{4}) (?<date>\\d+\\.\\d+\\.\\d{4}) .*"; //$NON-NLS-1$

    public SutorPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sutor"); //$NON-NLS-1$
        addBankIdentifier("Sutor Bank"); //$NON-NLS-1$

        addDeposit();
        addBuySellTransaction();
        addFeePayment();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Sutor Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addDeposit()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".*(Zulage|automatischer Lastschrifteinzug|Einzahlung).*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( // check for old and new format
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }),
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }))
                        .wrap(TransactionItem::new);
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* (Kauf|Verkauf|Gebührentilgung) .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name").match("^.* (Kauf|Verkauf|Gebührentilgung) (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .oneOf( //
                                // check for old format, if amount is negative,
                                // we buy
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for old format - if amount is
                                        // positive (no sign), we sell
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }),
                                        // check for BUY with new format
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for SELL with new format
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeePayment()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(
                        ".* (Verwaltungsgebühr/Vertriebskosten|anteil.Verwaltgebühr/Vertriebskosten|Kontoführungs-u.Depotgebühren).*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( // check for old and new format
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }),
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }))
                        .wrap(TransactionItem::new);
    }

    @Override
    public String getLabel()
    {
        return "Sutor Fairriester"; //$NON-NLS-1$
    }
}

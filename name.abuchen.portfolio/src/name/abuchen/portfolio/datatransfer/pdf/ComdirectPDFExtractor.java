package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    public ComdirectPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("comdirect bank"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
        addSellTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierkauf *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^St\\. *(?<shares>\\d+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*Zu Ihren Lasten vor Steuern *") //
                        .match(".*(\\w{3}+) *\\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency") //
                        .optional().match(".*Summe Entgelte *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+) *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("G  u t s c h  ri f t fä  ll ig  e r W  e r t p a p i e r -E  r tr ä g e");
        this.addDocumentTyp(type);

        Block block = new Block(".*G  u t s c h  ri f t fä  ll ig  e r W  e r t p a p i e r -E  r tr ä g e *");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "name", "isin", "shares") //
                        .match("p e r *\\d \\d *. \\d\\d . \\d \\d \\d \\d (?<name>.*)      (?<wkn>.*)") //
                        .match("^S T K *(?<shares>(\\d )*(\\. )?(\\d )*, (\\d )*).*    .* {4}(?<isin>.*)$") //
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("currency", "amount", "date") //
                        .find(".*Zu Ihren Gunsten vor Steuern *") //
                        .match("^.*(?<date>\\d{2}.\\d{2}.\\d{4}) *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierverkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierverkauf *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        }).section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^St\\. *(?<shares>\\d+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*Zu Ihren Gunsten vor Steuern *") //
                        .match(".*(\\w{3}+) *\\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency") //
                        .optional().match(".*Summe Entgelte *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "comdirect"; //$NON-NLS-1$
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

}

package name.abuchen.portfolio.datatransfer.pdf;

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
public class CommerzbankPDFExtractor extends AbstractPDFExtractor
{
    public CommerzbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K"); //$NON-NLS-1$
        addBankIdentifier("Commerzbank AG"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("W e r t p a p i e r k a u f");
        this.addDocumentTyp(type);

        Block block = new Block("W e r t p a p i e r k a u f");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("amount", "currency") //
                        .match(".*Zu I h r e n L a s t e n.*")
                        .match("^.* (\\d \\d . \\d \\d . \\d \\d \\d \\d) (?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("date") //
                        .match("G e s c h ä f t s t a g : (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) .*")
                        .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                        .section("shares") //
                        .match("S t . (?<shares>[\\d ,.]*) .*")
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                        .section("wkn", "name", "isin", "currency")
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*").match("(?<name>.*) (?<wkn>\\S*)")
                        .match("^IBAN.*$") //
                        .match("^(?<isin>.*) (?<currency>\\w{3}+) (\\d \\d . \\d \\d . \\d \\d \\d \\d) (\\w{3}+)(( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private void addDividendTransaction()
    {
        DocumentType type1 = new DocumentType("E r t r a g s g u t s c h r i f t");
        this.addDocumentTyp(type1);

        Block block1 = new Block("E r t r a g s g u t s c h r i f t");
        type1.addBlock(block1);
        block1.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .match(".*Zu I h r e n Gunsten.*")
                        .match("^.* (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) (?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("wkn", "name", "shares", "isin")
                        //
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*")
                        .match("p e r \\d \\d . \\d \\d . \\d \\d \\d \\d (?<name>.*) (?<wkn>\\S*)")
                        .match("^STK (?<shares>(\\d )*(\\. )?(\\d )*, (\\d )*).* (?<isin>\\S*)$").assign((t, v) -> {
                            // if necessary, create the security with the
                            // currency of the transaction
                            v.put("currency", t.getCurrencyCode());
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .wrap(TransactionItem::new));

        DocumentType type2 = new DocumentType("D i v i d e n d e n g u t s c h r i f t");
        this.addDocumentTyp(type2);

        Block block2 = new Block("D i v i d e n d e n g u t s c h r i f t");
        type2.addBlock(block2);
        block2.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("date", "amount", "currency") //
                        .match(".*Zu I h r e n Gunsten.*")
                        .match("^.* (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) (?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("wkn", "name", "shares", "isin")
                        //
                        .match(".*WKN/ISIN.*")
                        .match("p e r \\d \\d . \\d \\d . \\d \\d \\d \\d\\s*(?<name>.*) (?<wkn>\\S*)$")
                        .match("^STK (?<shares>(\\d )*(\\. )?(\\d )*, (\\d )*).* (?<isin>\\S*)$").assign((t, v) -> {
                            // if necessary, create the security with the
                            // currency of the transaction
                            v.put("currency", t.getCurrencyCode());
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .wrap(TransactionItem::new));

        DocumentType type3 = new DocumentType(".*Steuerliche Behandlung:.*Dividende.*");
        this.addDocumentTyp(type3);

        Block block3 = new Block("Steuerliche Behandlung:.*Dividende.*");
        type3.addBlock(block3);
        block3.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("date") //
                        .match("Die Gutschrift erfolgt mit Valuta\\s*(?<date>\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d).*")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                        })

                        .section("amount", "currency", "tax1", "tax2") //
                        .match("^Kapitalertragsteuer \\s*(?<currency>\\w{3}+)(?<tax1>( -)?( \\d)*( \\.)?( \\d)* ,( \\d)*).*$")
                        .match("^Solidaritätszuschlag \\s*(?<currency>\\w{3}+)(?<tax2>( -)?( \\d)*( \\.)?( \\d)* ,( \\d)*).*$")
                        .match("^Zu Ihren Gunsten nach Steuern: \\s*(?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*).*$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                            long tax1 = asAmount(stripBlanks(v.get("tax1")));
                            long tax2 = asAmount(stripBlanks(v.get("tax2")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), tax1 + tax2)));
                        })

                        .section("wkn", "shares", "isin", "name")
                        //
                        .match("^.*Stk\\. (?<shares>([\\d\\.])*) (?<name>.*) , WKN \\/ ISIN: (?<wkn>\\S*) \\/ (?<isin>\\S*).*$")
                        .assign((t, v) -> {
                            // if necessary, create the security with the
                            // currency of the transaction
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripDots(stripBlanks(v.get("shares")))));
                        })

                        .wrap(TransactionItem::new));
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String stripDots(String input)
    {
        return input.replaceAll("\\.", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String getLabel()
    {
        return "Commerzbank"; //$NON-NLS-1$
    }
}

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

public class DkbPDFExtractor extends AbstractPDFExtractor
{
    public DkbPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addBuyTransactionFund();
        addSellTransaction();
        addInterestTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Kauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-) (?<currency>\\w{3}+)")
                        .match("(^Den Gegenwert buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu Lasten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("fees") //
                        .match("(^Provision) (?<fees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addBuyTransactionFund()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Ausgabe Investmentfonds");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Ausgabe Investmentfonds");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{4})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-) (?<currency>\\w{3}+)")
                        .match("(^Den Gegenwert buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu Lasten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("fees")
                        .optional()
                        //
                        .match("(^Provision) (?<fees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(
                                        new Unit(Unit.Type.FEE, Money.of(asCurrencyCode(v.get("currency")),
                                                        asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung Verkauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("notation", "shares", "name", "isin", "wkn")
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<currency>\\w{3}+)(.*)")
                        .match("(^Den Gegenwert buchen wir mit Valuta) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("tax").optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("soli").optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli"))))))

                        .section("fees") //
                        .match("(^Provision) (?<fees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Zinsgutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("notation", "shares", "name", "isin", "wkn") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(?<notation>^St\\Dck|^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !(notation.startsWith("St") && notation.endsWith("ck")))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*) (?<currency>\\w{3}+)")
                        .match("(^Lagerstelle) (.*)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("shares", "name", "isin", "wkn") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                        .match("(^St\\Dck) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{2})?) (?<name>.*) (?<isin>[^ ]*) (\\((?<wkn>.*)\\).*)$")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("date", "amount")
                        .match("(^Ausmachender Betrag) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*) (?<currency>\\w{3}+)")
                        .match("(^Lagerstelle) (.*)")
                        .match("(^Den Betrag buchen wir mit Wertstellung) (?<date>\\d+.\\d+.\\d{4}+) zu Gunsten des Kontos (.*)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("tax")
                        .optional()
                        .match("^Kapitalertragsteuer (.*) (\\w{3}+) (?<tax>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("soli")
                        .optional()
                        .match("^Solidarit(.*) (\\w{3}+) (?<soli>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli"))))))

                        .section("quellenst")
                        .optional()
                        .match("^Anrechenbare Quellensteuer(.*) (\\w{3}+) (?<quellenst>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("quellenst"))))))

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "DKB"; //$NON-NLS-1$
    }
}

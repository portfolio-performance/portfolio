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

public class DABPDFExctractor extends AbstractPDFExtractor
{
    public DABPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Kauf .*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .match("Handelstag (\\d+.\\d+.\\d{4}+) Kurswert (?<currency>\\w{3}+) ([\\d.]+,\\d+)-")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>\\d+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fees", "currency").optional()
                        .match("^.* Provision (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Verkauf .*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .match("STK \\d+(,\\d+)? (?<currency>\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>\\d+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fees", "currency").optional()
                        .match("^.*Provision (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional()
                        .match("^.*Kapitalertragsteuer (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional()
                        .match("^.*Solidaritätszuschlag (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional()
                        .match("^.*Kirchensteuer (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividendengutschrift .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$") //
                        .match("STK (\\d+(,\\d+)?) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (\\d+,\\d+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Ex-Tag Zahltag Dividenden-Betrag pro Stück")
                        .match("^STK (?<shares>\\d+(,\\d+)?) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(?<date>\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank"; //$NON-NLS-1$
    }

}

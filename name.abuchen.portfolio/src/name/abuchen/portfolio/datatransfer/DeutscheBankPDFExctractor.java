package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

public class DeutscheBankPDFExctractor extends AbstractPDFExtractor
{
    public DeutscheBankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf von Wertpapieren");
        this.addDocumentTyp(type);

        Block block = new Block("Abrechnung: Kauf von Wertpapieren");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            entry.setCurrencyCode(CurrencyUnit.EUR);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        .find("Filialnummer Depotnummer Wertpapierbezeichnung Seite")
                        .match("^.{15}(?<name>.*)$")
                        .match("^WKN (?<wkn>[^ ]*) (.*)$")
                        .match("^ISIN (?<isin>[^ ]*) (.*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        .match("^WKN [^ ]* Nominal ST (?<shares>\\d+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount")
                        .match("Buchung auf Kontonummer [\\d ]* mit Wertstellung (?<date>\\d+.\\d+.\\d{4}+) (\\w{3}+) (?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fees") //
                        .match("Kurswert (\\w{3}+) (?<fees>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            long marketValue = asAmount(v.get("fees"));
                            long totalAmount = t.getPortfolioTransaction().getAmount();
                            long taxes = t.getPortfolioTransaction().getTaxes();

                            switch (t.getPortfolioTransaction().getType())
                            {
                                case BUY:
                                    t.setFees(totalAmount - taxes - marketValue);
                                    break;
                                case SELL:
                                    t.setFees(marketValue - taxes - totalAmount);
                                    break;
                                default:
                                    throw new UnsupportedOperationException();
                            }
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf von Wertpapieren");
        this.addDocumentTyp(type);

        Block block = new Block("Abrechnung: Verkauf von Wertpapieren");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            entry.setCurrencyCode(CurrencyUnit.EUR);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        .find("Filialnummer Depotnummer Wertpapierbezeichnung Seite")
                        .match("^.{15}(?<name>.*)$")
                        .match("^WKN (?<wkn>[^ ]*) (.*)$")
                        .match("^ISIN (?<isin>[^ ]*) (.*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        .match("^WKN [^ ]* Nominal ST (?<shares>\\d+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount")
                        .match("Buchung auf Kontonummer [\\d ]* mit Wertstellung (?<date>\\d+.\\d+.\\d{4}+) (\\w{3}+) (?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("tax")
                        //
                        .match("Kapitalertragsteuer (\\w{3}+) (?<tax>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.setTaxes(asAmount(v.get("tax"))))

                        .section("soli")
                        .match("Solidaritätszuschlag auf Kapitalertragsteuer (\\w{3}+) (?<soli>[\\d.-]+,\\d+)") //
                        .assign((t, v) -> t.setTaxes(t.getPortfolioTransaction().getTaxes() + asAmount(v.get("soli"))))

                        .section("fees") //
                        .match("Kurswert (\\w{3}+) (?<fees>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            long marketValue = asAmount(v.get("fees"));
                            long totalAmount = t.getPortfolioTransaction().getAmount();
                            long taxes = t.getPortfolioTransaction().getTaxes();

                            switch (t.getPortfolioTransaction().getType())
                            {
                                case BUY:
                                    t.setFees(totalAmount - taxes - marketValue);
                                    break;
                                case SELL:
                                    t.setFees(marketValue - taxes - totalAmount);
                                    break;
                                default:
                                    throw new UnsupportedOperationException();
                            }
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsgutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            transaction.setCurrencyCode(CurrencyUnit.EUR);
                            return transaction;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .find("Stück WKN ISIN")
                        //
                        .match("(\\d+,\\d*) (?<wkn>\\S*) (?<isin>\\S*)")
                        //
                        .match("^(?<name>.*)$")
                        //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("(?<shares>\\d+,\\d*) (\\S*) (\\S*)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount")
                        .match("Gutschrift mit Wert (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return Messages.PDFdbLabel;
    }
}

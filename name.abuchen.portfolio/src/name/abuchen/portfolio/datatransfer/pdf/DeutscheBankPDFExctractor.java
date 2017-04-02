package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.math.BigDecimal;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class DeutscheBankPDFExctractor extends AbstractPDFExtractor
{
    public DeutscheBankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction("Ertragsgutschrift"); //$NON-NLS-1$
        addDividendTransaction("Dividendengutschrift"); //$NON-NLS-1$
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
                            return entry;
                        })

                        .section("wkn", "isin", "name", "currency")
                        .find("Filialnummer Depotnummer Wertpapierbezeichnung Seite") //
                        .match("^.{15}(?<name>.*)$") //
                        .match("^WKN (?<wkn>[^ ]*) (.*)$") //
                        .match("^ISIN (?<isin>[^ ]*) Kurs (?<currency>\\w{3}+) (.*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares") //
                        .match("^WKN [^ ]* Nominal ST (?<shares>\\d+(,\\d+)?)") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Buchung auf Kontonummer [\\d ]* mit Wertstellung (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .section("provision", "currency") //
                        .optional()
                        .match("Provision( \\([0-9,]* %\\))? (?<currency>\\w{3}+) (?<provision>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("provision"))))))

                        .section("additional", "currency") //
                        .optional()
                        .match("Weitere Provision der Bank bei der börslichen Orderausführung (?<currency>\\w{3}+) (?<additional>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("additional"))))))

                        .section("xetra", "currency") //
                        .optional() //
                        .match("XETRA-Kosten (?<currency>\\w{3}+) (?<xetra>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("xetra"))))))

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
                            return entry;
                        })

                        .section("wkn", "isin", "name", "currency")
                        .find("Filialnummer Depotnummer Wertpapierbezeichnung Seite") //
                        .match("^.{15}(?<name>.*)$") //
                        .match("^WKN (?<wkn>[^ ]*) (.*)$") //
                        .match("^ISIN (?<isin>[^ ]*) Kurs (?<currency>\\w{3}+) (.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^WKN [^ ]* Nominal ST (?<shares>\\d+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Buchung auf Kontonummer [\\d ]* mit Wertstellung (?<date>\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("tax", "currency") //
                        .optional() //
                        .match("Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("soli", "currency") //
                        .optional()
                        .match("Solidaritätszuschlag auf Kapitalertragsteuer (?<currency>\\w{3}+) (?<soli>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli"))))))

                        .section("provision", "currency") //
                        .optional().match("Provision.*(?<currency>\\w{3}+) -(?<provision>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("provision"))))))
                        
                        .section("charges", "currency") //
                        .optional().match("Fremde Spesen und Auslagen (?<currency>\\w{3}+) -(?<charges>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("charges"))))))

                        .section("additional", "currency") //
                        .optional()
                        .match("Weitere Provision der Bank bei der börslichen Orderausf.*hrung (?<currency>\\w{3}+) -(?<additional>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("additional"))))))

                        .section("xetra", "currency") //
                        .optional() //
                        .match("XETRA-Kosten (?<currency>\\w{3}+) -(?<xetra>[\\d.]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("xetra"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction(String nameOfTransaction)
    {
        DocumentType type = new DocumentType(nameOfTransaction);
        this.addDocumentTyp(type);

        Block block = new Block(nameOfTransaction);
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("wkn", "isin", "name", "currency") //
                        .find("Stück WKN ISIN") //
                        .match("(\\d+,\\d*) (?<wkn>\\S*) (?<isin>\\S*)") //
                        .match("^(?<name>.*)$") //
                        .match("Bruttoertrag ([\\d.]+,\\d+) (?<currency>\\w{3}+).*") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("(?<shares>\\d+,\\d*) (\\S*) (\\S*)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Gutschrift mit Wert (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("grossValue", "currency") //
                        .optional() //
                        .match("Bruttoertrag (?<grossValue>[\\d.]+,\\d+) (?<currency>\\w{3}+)").assign((t, v) -> {
                            Money grossValue = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("grossValue")));

                            // calculating taxes as the difference between gross
                            // value and transaction amount
                            Money taxes = MutableMoney.of(t.getCurrencyCode()).add(grossValue)
                                            .subtract(t.getMonetaryAmount()).toMoney();
                            if (!taxes.isZero())
                                t.addUnit(new Unit(Unit.Type.TAX, taxes));
                        })

                        // will match gross value only if forex data exists
                        .section("forexSum", "forexCurrency", "grossValue", "currency", "exchangeRate") //
                        .optional() //
                        .match("Bruttoertrag (?<forexSum>[\\d.]+,\\d+) (?<forexCurrency>\\w{3}+) (?<grossValue>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .match("Umrechnungskurs (\\w{3}+) zu (\\w{3}+) (?<exchangeRate>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            Money grossValue = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("grossValue")));
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forexSum")));
                            BigDecimal exchangeRate = BigDecimal.ONE.divide( //
                                            asExchangeRate(v.get("exchangeRate")), 10, BigDecimal.ROUND_HALF_DOWN);
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, grossValue, forex, exchangeRate);

                            // add gross value unit only if currency code of
                            // security actually matches
                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                t.addUnit(unit);

                            // calculating taxes as the difference between gross
                            // value and transaction amount
                            Money taxes = MutableMoney.of(t.getCurrencyCode()).add(grossValue)
                                            .subtract(t.getMonetaryAmount()).toMoney();
                            if (!taxes.isZero())
                                t.addUnit(new Unit(Unit.Type.TAX, taxes));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return Messages.PDFdbLabel;
    }
}

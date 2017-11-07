package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

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

        addBankIdentifier("DAB Bank"); //$NON-NLS-1$
        addBankIdentifier("BNP Paribas S.A. Niederlassung Deutschland"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Computershare Communication Services GmbH"; //$NON-NLS-1$
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
                        .match("STK [\\d.]+(,\\d+)? (?<currency>\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .optional() //
                        .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("amount", "currency", "exchangeRate", "forex", "forexCurrency").optional() //
                        .find(".* Ausmachender Betrag (?<forexCurrency>\\w{3}+) (?<forex>[\\d.]+,\\d+)-")
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) .../... (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {

                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(amount);

                            BigDecimal exchangeRate = BigDecimal.ONE.divide( //
                                            asExchangeRate(v.get("exchangeRate")), 10, BigDecimal.ROUND_HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));

                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                            t.getPortfolioTransaction().addUnit(grossValue);
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fees", "currency") //
                        .optional().match("^.* Provision (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            // FIXME forex fees must update gross value
                            if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(currency, asAmount(v.get("fees")))));
                            }
                        })

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getAmount() == 0L)
                                throw new IllegalArgumentException("No amount found");

                            return new BuySellEntryItem(t);
                        }));
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
                        .match("STK [\\d.]+(,\\d+)? (?<currency>\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency").optional() //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("amount", "currency", "exchangeRate", "forex", "forexCurrency").optional() //
                        .find(".* Ausmachender Betrag (?<forexCurrency>\\w{3}+) (?<forex>[\\d.]+,\\d+)")
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) .../... (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {

                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(amount);

                            BigDecimal exchangeRate = BigDecimal.ONE.divide( //
                                            asExchangeRate(v.get("exchangeRate")), 10, BigDecimal.ROUND_HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));

                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                            t.getPortfolioTransaction().addUnit(grossValue);
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fees", "currency").optional()
                        .match("^.*Provision (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$").assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(currency, asAmount(v.get("fees")))));
                            }
                        })

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*)Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign(this::addSellTaxUnit)

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*)Solidaritätszuschlag (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign(this::addSellTaxUnit)

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*)Kirchensteuer (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)-?$") //
                        .assign(this::addSellTaxUnit)

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getAmount() == 0L)
                                throw new IllegalArgumentException("No amount found");

                            return new BuySellEntryItem(t);
                        }));
    }

    @SuppressWarnings("nls")
    private void addSellTaxUnit(BuySellEntry t, Map<String, String> v)
    {
        if (v.get("label").contains("im laufenden Jahr einbehaltene"))
            return;

        String currency = asCurrencyCode(v.get("currency"));

        // FIXME forex fees must update gross value
        if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(currency, asAmount(v.get("tax")))));
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
                        .match("STK ([\\d.]+(,\\d+)?) (\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) (\\d+,\\d+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("^Nominal Ex-Tag Zahltag .*") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency") //
                        .optional() //
                        .find("Wert *Konto-Nr. *Betrag *zu *Ihren *Gunsten")
                        .match("^(?<date>\\d+.\\d+.\\d{4}+) ([0-9]*) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date", "amount", "currency", "forexCurrency", "exchangeRate") //
                        .optional() //
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten")
                        .match("^(?<date>\\d+.\\d+.\\d{4}+) ([0-9]*) \\w{3}+/(?<forexCurrency>\\w{3}+) (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                            BigDecimal.ROUND_HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                            Math.round(t.getAmount() / exchangeRate.doubleValue()));
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, exchangeRate);
                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        // this section is needed, if the dividend is payed in
                        // the forex currency to a account in forex curreny but
                        // the security is listed in local currency
                        .section("forex", "localCurrency", "forexCurrency", "exchangeRate") //
                        .optional() //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (\\w{3}+) (?<forex>[\\d.]+,\\d+)$")
                        .match("Devisenkurs: (?<localCurrency>\\w{3}+)/(?<forexCurrency>\\w{3}+) (?<exchangeRate>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                            BigDecimal.ROUND_HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            Money localAmount = Money.of(v.get("localCurrency"), Math.round(forex.getAmount()
                                            / Double.parseDouble(v.get("exchangeRate").replace(',', '.'))));
                            t.setAmount(forex.getAmount());
                            t.setCurrencyCode(forex.getCurrencyCode());
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, forex, localAmount, exchangeRate);
                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        .wrap(t -> {
                            if (t.getAmount() == 0)
                                throw new IllegalArgumentException("No dividend amount found.");
                            return new TransactionItem(t);
                        }));
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank"; //$NON-NLS-1$
    }

}

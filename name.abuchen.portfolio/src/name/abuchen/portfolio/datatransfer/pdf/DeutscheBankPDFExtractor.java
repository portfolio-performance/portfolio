package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class DeutscheBankPDFExtractor extends AbstractPDFExtractor
{
    public DeutscheBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Deutsche Bank"); //$NON-NLS-1$
        addBankIdentifier("DB Privat- und Firmenkundenbank AG"); //$NON-NLS-1$

        addDividendTransaction("Ertragsgutschrift"); //$NON-NLS-1$
        addDividendTransaction("Dividendengutschrift"); //$NON-NLS-1$
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
                        .match("([\\d.]+,\\d*) (?<wkn>\\S*) (?<isin>\\S*)") //
                        .match("^(?<name>.*)$") //
                        .match("Bruttoertrag ([\\d.]+,\\d+) (?<currency>\\w{3}+).*") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("(?<shares>[\\d.]+,\\d*) (\\S*) (\\S*)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency")
                        .match("Gutschrift mit Wert (?<date>\\d+.\\d+.\\d{4}+) (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
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
                                            asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
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

                        .wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Bank"; //$NON-NLS-1$
    }
}

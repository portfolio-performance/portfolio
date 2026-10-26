package name.abuchen.portfolio.cli.apply;

import name.abuchen.portfolio.cli.query.BuiltRecord;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The part of <code>transaction.add</code> that records buys, sells and
 * deliveries. A buy or sell creates both sides through core's
 * {@link BuySellEntry}, so they cannot get out of step. A delivery has no cash
 * side. Nothing is inserted until every check passed.
 */
final class TradeOperations
{
    private TradeOperations()
    {
    }

    static void add(OperationContext context, PortfolioTransaction.Type type)
    {
        var input = new TransactionInput(context);
        var delivery = type == PortfolioTransaction.Type.DELIVERY_INBOUND
                        || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND;

        input.refuse("kind", "from", "to", "targetAmount", "exchangeRate", "exDate");
        if (delivery)
            input.refuse("cashAccount");

        var portfolio = input.investmentAccount();
        var account = delivery ? null : input.cashAccount();
        var dateTime = input.dateTime();
        var currencyCode = input.currency(account);
        var amount = input.amount("amount", Values.Amount);
        var security = input.instrument(true, true);
        var shares = input.amount("shares", Values.Share);
        var units = input.units(currencyCode, security);

        input.grossAmount(type.isLiquidation(), amount, units);

        var note = input.text("note");
        var source = input.text("source");

        if (context.failed() || portfolio == null || dateTime == null || currencyCode == null || amount == null
                        || security == null || shares == null || !delivery && account == null)
            return;

        if (delivery)
        {
            var transaction = new PortfolioTransaction();
            transaction.setType(type);
            transaction.setDateTime(dateTime);
            transaction.setSecurity(security);
            transaction.setShares(shares);
            transaction.setMonetaryAmount(Money.of(currencyCode, amount));
            transaction.setNote(note);
            transaction.setSource(source);
            units.forEach(transaction::addUnit);

            TransactionOperations.runCoreChecks(context, check -> check.process(transaction, portfolio));
            TransactionOperations.commit(context, BuiltRecord.of(transaction, portfolio));
            return;
        }

        var entry = new BuySellEntry(portfolio, account);
        entry.setType(type);
        entry.setDate(dateTime);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setMonetaryAmount(Money.of(currencyCode, amount));
        entry.setNote(note);
        entry.setSource(source);
        units.forEach(entry.getPortfolioTransaction()::addUnit);

        TransactionOperations.runCoreChecks(context, check -> check.process(entry, account, portfolio));
        TransactionOperations.commit(context, BuiltRecord.of(entry));
    }
}

package name.abuchen.portfolio.cli;

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
        var input = context.input();
        var delivery = type == PortfolioTransaction.Type.DELIVERY_INBOUND
                        || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND;

        TransactionOperations.refuse(context, input, "kind", "from", "to", "targetAmount", "exchangeRate", "exDate");
        if (delivery)
            TransactionOperations.refuse(context, input, "cashAccount");

        var portfolio = TransactionOperations.parseInvestmentAccount(context, input);
        var account = delivery ? null : TransactionOperations.parseCashAccount(context, input);
        var dateTime = TransactionOperations.parseDateTime(context, input);
        var currencyCode = TransactionOperations.parseCurrency(context, input, account);
        var amount = TransactionOperations.parseAmount(context, input, "amount", Values.Amount, true);
        var security = TransactionOperations.parseInstrument(context, input, true, true);
        var shares = TransactionOperations.parseAmount(context, input, "shares", Values.Share, true);
        var units = TransactionOperations.parseUnits(context, input, currencyCode, security);

        TransactionOperations.parseGrossAmount(context, input, type.isLiquidation(), amount, units);

        var note = TransactionOperations.parseText(context, input, "note");
        var source = TransactionOperations.parseText(context, input, "source");

        if (context.failed() || portfolio == null || dateTime == null || currencyCode == null || amount == null
                        || security == null || shares == null || !delivery && account == null)
            return;

        OversellGuard.check(context, portfolio, security, dateTime.toLocalDate(),
                        type.isPurchase() ? shares : -shares);

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
            if (context.failed())
                return;

            portfolio.addTransaction(transaction);
            TransactionOperations.reportCreated(context, transaction);
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
        if (context.failed())
            return;

        entry.insert();
        TransactionOperations.reportCreated(context, entry.getPortfolioTransaction());
    }
}

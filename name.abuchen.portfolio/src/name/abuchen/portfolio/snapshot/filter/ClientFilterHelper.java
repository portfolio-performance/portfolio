package name.abuchen.portfolio.snapshot.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

/* protected */ class ClientFilterHelper
{
    private ClientFilterHelper()
    {
    }

    /* package */ static void recreateTransfer(PortfolioTransferEntry transferEntry, ReadOnlyPortfolio sourcePortfolio,
                    ReadOnlyPortfolio targetPortfolio)
    {
        recreateTransfer(transferEntry, sourcePortfolio, targetPortfolio, Classification.ONE_HUNDRED_PERCENT_BD);
    }

    /* package */ static void recreateTransfer(PortfolioTransferEntry transferEntry, ReadOnlyPortfolio sourcePortfolio,
                    ReadOnlyPortfolio targetPortfolio, BigDecimal weight)
    {
        PortfolioTransaction t = transferEntry.getSourceTransaction();

        PortfolioTransferEntry copy = new PortfolioTransferEntry(sourcePortfolio, targetPortfolio);
        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setNote(t.getNote());
        copy.setShares(value(t.getShares(), weight));
        copy.setAmount(value(t.getAmount(), weight));

        sourcePortfolio.internalAddTransaction(copy.getSourceTransaction());
        targetPortfolio.internalAddTransaction(copy.getTargetTransaction());
    }

    /**
     * Recreates a portfolio (security) transfer between two included portfolios
     * that may carry different ownership weights. The shares/amount common to
     * both sides (the lower weight) are recreated as a linked transfer; the
     * excess on the higher-weighted side becomes an additional security
     * delivery (DELIVERY_OUTBOUND on the source, DELIVERY_INBOUND on the
     * target) because the surplus is a security movement, not cash.
     */
    /* package */ static void recreateTransfer(PortfolioTransferEntry transferEntry, ReadOnlyPortfolio sourcePortfolio,
                    ReadOnlyPortfolio targetPortfolio, int sourceWeight, int targetWeight)
    {
        int common = Math.min(sourceWeight, targetWeight);

        recreateTransfer(transferEntry, sourcePortfolio, targetPortfolio, weightToBigDecimal(common));

        var t = transferEntry.getSourceTransaction();

        if (sourceWeight > common)
            sourcePortfolio.internalAddTransaction(
                            excessDelivery(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND, sourceWeight - common));

        if (targetWeight > common)
            targetPortfolio.internalAddTransaction(
                            excessDelivery(t, PortfolioTransaction.Type.DELIVERY_INBOUND, targetWeight - common));
    }

    private static PortfolioTransaction excessDelivery(PortfolioTransaction t, PortfolioTransaction.Type type,
                    int weight)
    {
        var w = weightToBigDecimal(weight);

        var delivery = new PortfolioTransaction();
        delivery.setType(type);
        delivery.setDateTime(t.getDateTime());
        delivery.setCurrencyCode(t.getCurrencyCode());
        delivery.setSecurity(t.getSecurity());
        delivery.setNote(t.getNote());
        delivery.setShares(value(t.getShares(), w));
        delivery.setAmount(value(t.getAmount(), w));

        // keep all units (including taxes), scaled proportionally
        t.getUnits().forEach(u -> delivery.addUnit(value(u, w)));

        return delivery;
    }

    /* package */ static void recreateTransfer(AccountTransferEntry transferEntry, ReadOnlyAccount sourceAccount,
                    ReadOnlyAccount targetAccount)
    {
        AccountTransaction t = transferEntry.getSourceTransaction();

        AccountTransferEntry copy = new AccountTransferEntry(sourceAccount, targetAccount);

        copy.setDate(t.getDateTime());
        copy.setNote(t.getNote());

        copy.getSourceTransaction().setCurrencyCode(t.getCurrencyCode());
        copy.getSourceTransaction().setAmount(t.getAmount());
        copy.getSourceTransaction().addUnits(t.getUnits());

        AccountTransaction tt = transferEntry.getTargetTransaction();
        copy.getTargetTransaction().setCurrencyCode(tt.getCurrencyCode());
        copy.getTargetTransaction().setAmount(tt.getAmount());

        sourceAccount.internalAddTransaction(copy.getSourceTransaction());
        targetAccount.internalAddTransaction(copy.getTargetTransaction());
    }

    /**
     * Recreates a cash transfer between two included accounts that may carry
     * different ownership weights. The amount common to both sides (the lower
     * weight) is recreated as a linked transfer; the excess on the
     * higher-weighted side becomes a DEPOSIT (target side) or REMOVAL (source
     * side) because the surplus is cash that crossed an ownership boundary.
     */
    /* package */ static void recreateTransfer(AccountTransferEntry transferEntry, ReadOnlyAccount sourceAccount,
                    ReadOnlyAccount targetAccount, int sourceWeight, int targetWeight)
    {
        if (sourceWeight == targetWeight && sourceWeight == Classification.ONE_HUNDRED_PERCENT)
        {
            recreateTransfer(transferEntry, sourceAccount, targetAccount);
            return;
        }

        int common = Math.min(sourceWeight, targetWeight);
        var commonWeight = weightToBigDecimal(common);

        var source = transferEntry.getSourceTransaction();
        var target = transferEntry.getTargetTransaction();

        var copy = new AccountTransferEntry(sourceAccount, targetAccount);
        copy.setDate(source.getDateTime());
        copy.setNote(source.getNote());
        copy.getSourceTransaction().setCurrencyCode(source.getCurrencyCode());
        copy.getTargetTransaction().setCurrencyCode(target.getCurrencyCode());
        copy.getSourceTransaction().setAmount(value(source.getAmount(), commonWeight));
        copy.getTargetTransaction().setAmount(value(target.getAmount(), commonWeight));

        sourceAccount.internalAddTransaction(copy.getSourceTransaction());
        targetAccount.internalAddTransaction(copy.getTargetTransaction());

        if (sourceWeight > common)
            sourceAccount.internalAddTransaction(new AccountTransaction(source.getDateTime(), source.getCurrencyCode(),
                            value(source.getAmount(), weightToBigDecimal(sourceWeight - common)), null,
                            AccountTransaction.Type.REMOVAL));

        if (targetWeight > common)
            targetAccount.internalAddTransaction(new AccountTransaction(target.getDateTime(), target.getCurrencyCode(),
                            value(target.getAmount(), weightToBigDecimal(targetWeight - common)), null,
                            AccountTransaction.Type.DEPOSIT));
    }

    private static BigDecimal weightToBigDecimal(int weight)
    {
        return BigDecimal.valueOf(weight);
    }

    /* package */ static Unit value(Unit unit, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return unit;
        else
            return unit.split(weight.divide(Classification.ONE_HUNDRED_PERCENT_BD, Values.MC).doubleValue());
    }

    /* package */ static long value(long value, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return value;
        else
            return BigDecimal.valueOf(value) //
                            .multiply(weight, Values.MC) //
                            .divide(Classification.ONE_HUNDRED_PERCENT_BD, Values.MC)
                            .setScale(0, RoundingMode.HALF_EVEN).longValue();
    }
}

package name.abuchen.portfolio.ui.util.searchfilter;

import java.util.function.Predicate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.ui.Messages;

public enum TransactionFilterCriteria
{
    NONE(Messages.TransactionFilterNone, 0, tx -> true), //
    SECURITY_TRANSACTIONS(Messages.TransactionFilterSecurityRelated, 0, tx -> {
        if (tx instanceof PortfolioTransaction)
            return true;
        else if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.DIVIDENDS || atx.getType() == AccountTransaction.Type.BUY
                            || atx.getType() == AccountTransaction.Type.SELL;
        else
            return false;
    }), //
    BUY_AND_SELL(Messages.TransactionFilterBuyAndSell, 1, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.BUY || ptx.getType() == PortfolioTransaction.Type.SELL;
        else if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.BUY || atx.getType() == AccountTransaction.Type.SELL;
        else
            return false;
    }), //
    BUY(Messages.TransactionFilterBuy, 2, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.BUY;
        else if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.BUY;
        else
            return false;
    }), //
    SELL(Messages.TransactionFilterSell, 2, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.SELL;
        else if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.SELL;
        else
            return false;
    }), //
    DIVIDEND(Messages.TransactionFilterDividend, 1, tx -> {
        if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.DIVIDENDS;
        else
            return false;
    }), //
    DEPOSIT_AND_REMOVAL(Messages.TransactionFilterDepositAndRemoval, 0, tx -> {
        if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.DEPOSIT || atx.getType() == AccountTransaction.Type.REMOVAL;
        else
            return false;
    }), //
    DEPOSIT(Messages.TransactionFilterDeposit, 1, tx -> {
        if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.DEPOSIT;
        else
            return false;
    }), //
    REMOVAL(Messages.TransactionFilterRemoval, 1, tx -> {
        if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.REMOVAL;
        else
            return false;
    }), //
    INTEREST(Messages.TransactionFilterInterest, 0, tx -> {
        if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.INTEREST
                            || atx.getType() == AccountTransaction.Type.INTEREST_CHARGE;
        else
            return false;
    }), //
    WITH_TAX(Messages.TransactionFilterTaxes, 0, tx -> {
        if (tx instanceof AccountTransaction atx && (atx.getType() == AccountTransaction.Type.TAXES
                        || atx.getType() == AccountTransaction.Type.TAX_REFUND))
            return true;
        return tx.getUnitSum(Transaction.Unit.Type.TAX).isPositive();
    }), //
    WITH_FEES(Messages.TransactionFilterFees, 0, tx -> {
        if (tx instanceof AccountTransaction atx && (atx.getType() == AccountTransaction.Type.FEES
                        || atx.getType() == AccountTransaction.Type.FEES_REFUND))
            return true;
        return tx.getUnitSum(Transaction.Unit.Type.FEE).isPositive();
    }), //
    TRANSFERS(Messages.TransactionFilterTransfers, 0, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.TRANSFER_IN
                            || ptx.getType() == PortfolioTransaction.Type.TRANSFER_OUT;
        else if (tx instanceof AccountTransaction atx)
            return atx.getType() == AccountTransaction.Type.TRANSFER_IN
                            || atx.getType() == AccountTransaction.Type.TRANSFER_OUT;
        else
            return false;
    }), //
    DELIVERIES(Messages.TransactionFilterDeliveries, 0, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                            || ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        else
            return false;
    }), //
    DELIVERIES_INBOUND(PortfolioTransaction.Type.DELIVERY_INBOUND.toString(), 1, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;
        else
            return false;
    }), //
    DELIVERIES_OUTBOUND(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString(), 1, tx -> {
        if (tx instanceof PortfolioTransaction ptx)
            return ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        else
            return false;
    });

    private String name;
    private int level;
    private Predicate<Transaction> predicate;

    TransactionFilterCriteria(String name, int level, Predicate<Transaction> predicate)
    {
        this.name = name;
        this.level = level;
        this.predicate = predicate;
    }

    public boolean matches(Transaction tx)
    {
        return predicate.test(tx);
    }

    public String getName()
    {
        return name;
    }

    public int getLevel()
    {
        return level;
    }
}

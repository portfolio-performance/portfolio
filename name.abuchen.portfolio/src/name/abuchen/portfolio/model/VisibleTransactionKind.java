package name.abuchen.portfolio.model;

import java.util.Optional;

public enum VisibleTransactionKind
{
    ACCOUNT_DEPOSIT,
    ACCOUNT_REMOVAL,
    ACCOUNT_INTEREST,
    ACCOUNT_INTEREST_CHARGE,
    ACCOUNT_DIVIDENDS,
    ACCOUNT_FEES,
    ACCOUNT_FEES_REFUND,
    ACCOUNT_TAXES,
    ACCOUNT_TAX_REFUND,
    ACCOUNT_BUY,
    ACCOUNT_SELL,
    ACCOUNT_TRANSFER_IN,
    ACCOUNT_TRANSFER_OUT,

    PORTFOLIO_BUY,
    PORTFOLIO_SELL,
    PORTFOLIO_TRANSFER_IN,
    PORTFOLIO_TRANSFER_OUT,
    PORTFOLIO_DELIVERY_INBOUND,
    PORTFOLIO_DELIVERY_OUTBOUND;

    public static Optional<VisibleTransactionKind> of(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return Optional.of(valueOf("ACCOUNT_" + accountTransaction.getType().name())); //$NON-NLS-1$

        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return Optional.of(valueOf("PORTFOLIO_" + portfolioTransaction.getType().name())); //$NON-NLS-1$

        return Optional.empty();
    }
}

package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.util.function.Predicate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel;

enum EarningType
{
    EARNINGS(Messages.LabelDividends + " + " + Messages.LabelInterest, //$NON-NLS-1$
                    t -> t.getType() == AccountTransaction.Type.DIVIDENDS
                                    || t.getType() == AccountTransaction.Type.INTEREST
                                    || t.getType() == AccountTransaction.Type.INTEREST_CHARGE), //
    DIVIDENDS(Messages.LabelDividends, t -> t.getType() == AccountTransaction.Type.DIVIDENDS), //
    INTEREST(Messages.LabelInterest, t -> t.getType() == AccountTransaction.Type.INTEREST
                    || t.getType() == AccountTransaction.Type.INTEREST_CHARGE);

    private String label;
    private Predicate<AccountTransaction> predicate;

    private EarningType(String label, Predicate<AccountTransaction> predicate)
    {
        this.label = label;
        this.predicate = predicate;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public boolean isIncluded(AccountTransaction t)
    {
        return predicate.test(t);
    }

    public PaymentsViewModel.Mode getPaymentsViewModelMode()
    {
        switch (this)
        {
            case EARNINGS:
                return PaymentsViewModel.Mode.EARNINGS;
            case DIVIDENDS:
                return PaymentsViewModel.Mode.DIVIDENDS;
            case INTEREST:
                return PaymentsViewModel.Mode.INTEREST;
            default:
                throw new UnsupportedOperationException();
        }
    }
}

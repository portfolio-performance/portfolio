package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.util.function.Function;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;

enum GrossNetType
{
    NET(Messages.LabelNet, t -> t.getMonetaryAmount()), //
    GROSS(Messages.LabelGross, t -> t.getGrossValue());

    private String label;
    private Function<AccountTransaction, Money> valueExtractor;

    private GrossNetType(String label, Function<AccountTransaction, Money> valueExtractor)
    {
        this.label = label;
        this.valueExtractor = valueExtractor;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public Money getValue(AccountTransaction t)
    {
        return valueExtractor.apply(t);
    }
}
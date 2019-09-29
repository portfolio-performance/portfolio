package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.function.DoubleBinaryOperator;

import name.abuchen.portfolio.ui.Messages;

public enum ExcessReturnOperator
{
    ALPHA(Messages.LabelExcessReturnOperatorAlpha, (x, y) -> x - y), //
    RELATIVE(Messages.LabelExcessReturnOperatorRelative, (x, y) -> ((x + 1) / (y + 1)) - 1);

    private String label;
    private DoubleBinaryOperator operator;

    private ExcessReturnOperator(String label, DoubleBinaryOperator operator)
    {
        this.label = label;
        this.operator = operator;
    }

    public DoubleBinaryOperator getOperator()
    {
        return operator;
    }

    @Override
    public String toString()
    {
        return label;
    }
}

package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.ui.AbstractFinanceView;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */abstract class AbstractListView extends AbstractFinanceView
{
    protected void setWeights(SashForm sash)
    {
        Control[] children = sash.getChildren();

        int[] weights = new int[children.length];
        for (int ii = 0; ii < weights.length; ii++)
            weights[ii] = (int) Math.pow(2, ii * 2 + 1);

        sash.setWeights(weights);
    }

    @Override
    protected final Control createBody(Composite parent)
    {
        SashForm sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);

        createTopTable(sash);
        createBottomTable(sash);

        setWeights(sash);

        return sash;
    }

    protected abstract void createBottomTable(Composite parent);

    protected abstract void createTopTable(Composite parent);

}

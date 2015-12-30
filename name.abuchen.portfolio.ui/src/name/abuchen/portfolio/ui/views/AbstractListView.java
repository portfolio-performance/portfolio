package name.abuchen.portfolio.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;

import name.abuchen.portfolio.ui.AbstractFinanceView;

/* package */abstract class AbstractListView extends AbstractFinanceView
{
    private final String identifier = getClass().getSimpleName() + "-sash-weights"; //$NON-NLS-1$

    @Override
    protected final Control createBody(Composite parent)
    {
        SashForm sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);

        createTopTable(sash);
        createBottomTable(sash);

        attachDisposeListener(sash);
        doSetWeights(sash);

        return sash;
    }

    protected abstract void createBottomTable(Composite parent);

    protected abstract void createTopTable(Composite parent);

    protected int[] getDefaultWeights(Control[] children)
    {
        int[] weights = new int[children.length];
        for (int ii = 0; ii < weights.length; ii++)
            weights[ii] = (int) Math.pow(2, ii * 2 + 1);
        return weights;
    }

    private void doSetWeights(final SashForm sash)
    {
        Control[] children = sash.getChildren();
        int[] weights = null;

        String config = getPart().getPreferenceStore().getString(identifier);
        if (config != null)
        {
            try
            {
                String[] parts = config.split(","); //$NON-NLS-1$
                if (children.length == parts.length)
                {
                    weights = new int[children.length];
                    for (int ii = 0; ii < weights.length; ii++)
                        weights[ii] = Integer.parseInt(parts[ii]);
                }
            }
            catch (NumberFormatException ignore)
            {
                // ignore -> assign weight from scratch
                weights = null;
            }
        }

        // otherwise: setup default weights
        if (weights == null)
            weights = getDefaultWeights(children);

        sash.setWeights(weights);
    }

    private void attachDisposeListener(final SashForm sash)
    {
        sash.addDisposeListener(e -> {
            StringBuilder buf = new StringBuilder();
            for (Control child : sash.getChildren())
            {
                if (child instanceof Sash)
                    continue;
                buf.append(child.getBounds().height).append(',');
            }
            getPart().getPreferenceStore().putValue(identifier, buf.toString());
        });
    }

}

package name.abuchen.portfolio.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;

/* package */abstract class AbstractListView extends AbstractFinanceView
{
    private final String identifier = getClass().getSimpleName() + "-newsash"; //$NON-NLS-1$

    @Override
    protected final Control createBody(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);

        SashLayout sashLayout = new SashLayout(sash, SWT.VERTICAL | SWT.END);
        sash.setLayout(sashLayout);

        createTopTable(sash);
        createBottomTable(sash);

        Control[] children = sash.getChildren();
        if (children.length > 2)
        {
            Control bottomControl = children[2];
            int size = getPreferenceStore().getInt(identifier);
            bottomControl.setLayoutData(new SashLayoutData(size != 0 ? size : 250));
            sash.addDisposeListener(e -> getPreferenceStore().setValue(identifier,
                            ((SashLayoutData) bottomControl.getLayoutData()).getSize()));
        }

        return sash;
    }

    protected abstract void createBottomTable(Composite parent);

    protected abstract void createTopTable(Composite parent);
}

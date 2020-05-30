package name.abuchen.portfolio.ui.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;

public abstract class AbstractListView extends AbstractFinanceView
{
    private final String identifier = getClass().getSimpleName() + "-newsash"; //$NON-NLS-1$

    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.END;
    }

    @Override
    protected final Control createBody(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);

        int style = getSashStyle();
        SashLayout sashLayout = new SashLayout(sash, style);
        sash.setLayout(sashLayout);

        createTopTable(sash);
        createBottomTable(sash);

        List<Control> children = sashLayout.getChildren();
        int childIndex = (style & SWT.BEGINNING) == SWT.BEGINNING ? 0 : 1;
        if (children.size() > childIndex)
        {
            Control control = children.get(childIndex);
            int size = getPreferenceStore().getInt(identifier);
            control.setLayoutData(new SashLayoutData(size != 0 ? size : 250));
            sash.addDisposeListener(e -> getPreferenceStore().setValue(identifier,
                            ((SashLayoutData) control.getLayoutData()).getSize()));
        }

        return sash;
    }

    protected abstract void createBottomTable(Composite parent);

    protected abstract void createTopTable(Composite parent);
}

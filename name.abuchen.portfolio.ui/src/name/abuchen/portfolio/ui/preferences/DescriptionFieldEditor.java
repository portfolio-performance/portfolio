package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.util.swt.StyledLabel;

class DescriptionFieldEditor extends FieldEditor
{
    private String descriptionText;
    private StyledLabel descriptionLabel;

    public DescriptionFieldEditor(String description, Composite parent)
    {
        this.descriptionText = description;
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns)
    {
        ((GridData) descriptionLabel.getLayoutData()).horizontalSpan = numColumns;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns)
    {
        descriptionLabel = new StyledLabel(parent, SWT.WRAP);
        GridDataFactory.swtDefaults().span(numColumns, 1)
                        .hint(Math.max(200, parent.getParent().getParent().getClientArea().width - 20), SWT.DEFAULT)
                        .applyTo(descriptionLabel);
        descriptionLabel.setText(descriptionText);

        // Attach a resize listener to the scrolled composite up in the Control
        // hierarchy
        var control = parent;
        while (control != null)
        {
            if (control instanceof ScrolledComposite scrolled)
            {
                scrolled.addControlListener(new ControlAdapter()
                {
                    @Override
                    public void controlResized(ControlEvent e)
                    {
                        GridDataFactory.swtDefaults().span(numColumns, 1)
                                        .hint(Math.max(200, scrolled.getClientArea().width - 20), SWT.DEFAULT)
                                        .applyTo(descriptionLabel);
                        descriptionLabel.getParent().layout();
                    }
                });
                break;
            }
            control = control.getParent();
        }
    }

    @Override
    protected void doLoad()
    {
        // Nothing to load
    }

    @Override
    protected void doLoadDefault()
    {
        // Nothing to load
    }

    @Override
    protected void doStore()
    {
        // Nothing to store
    }

    @Override
    public int getNumberOfControls()
    {
        return 2;
    }
}

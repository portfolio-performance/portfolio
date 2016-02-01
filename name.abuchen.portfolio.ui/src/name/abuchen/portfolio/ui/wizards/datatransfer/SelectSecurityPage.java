package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class SelectSecurityPage extends AbstractWizardPage
{
    /* package */ static final String PAGE_ID = "select-security-page"; //$NON-NLS-1$

    private Client client;

    private ComboViewer combo;

    public SelectSecurityPage(Client client)
    {
        super(PAGE_ID);
        setTitle(Messages.PageTitleSelectSecurity);
        setDescription(Messages.PageDescriptionSelectSecurity);

        this.client = client;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FormLayout());

        Label label = new Label(container, SWT.NONE);
        label.setText(Messages.ColumnSecurity);

        List<Security> securities = client.getActiveSecurities();

        combo = new ComboViewer(container);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setInput(securities);
        if (!securities.isEmpty())
            combo.setSelection(new StructuredSelection(securities.get(0)));
        combo.addSelectionChangedListener(e -> setPageComplete(!combo.getSelection().isEmpty()));

        FormDataFactory.startingWith(combo.getControl(), label);

        setPageComplete(!combo.getSelection().isEmpty());

        setControl(container);
    }

    public Security getSelectedSecurity()
    {
        return (Security) ((IStructuredSelection) combo.getSelection()).getFirstElement();
    }

}

package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PredefinedSecurity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ImportIndizesPage extends AbstractWizardPage
{
    Client client;
    Text accountName;
   
    public ImportIndizesPage(Client client)
    {
        super("New ...");
        this.client = client;
        setTitle("Import standard securities into client");
        PredefinedSecurity.read("indices");
    }
    
    
    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout());
        
        container.pack();
        setPageComplete(true);
    }

}


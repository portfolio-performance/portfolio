package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

public class SecurityMasterDataPage extends AbstractWizardPage
{
    static class Model extends BindingHelper.Model
    {
        private Security security;

        private String name;
        private String isin;
        private String tickerSymbol;
        private AssetClass type;

        public Model(Client client, Security security)
        {
            super(client);

            this.security = security;

            name = security.getName();
            isin = security.getIsin();
            tickerSymbol = security.getTickerSymbol();
            type = security.getType();
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            firePropertyChange("name", this.name, this.name = name); //$NON-NLS-1$
        }

        public String getIsin()
        {
            return isin;
        }

        public void setIsin(String isin)
        {
            firePropertyChange("isin", this.isin, this.isin = isin); //$NON-NLS-1$
        }

        public String getTickerSymbol()
        {
            return tickerSymbol;
        }

        public void setTickerSymbol(String tickerSymbol)
        {
            firePropertyChange("tickerSymbol", this.tickerSymbol, this.tickerSymbol = tickerSymbol); //$NON-NLS-1$
        }

        public AssetClass getType()
        {
            return type;
        }

        public void setType(AssetClass type)
        {
            firePropertyChange("type", this.type, this.type = type); //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {
            security.setName(name);
            security.setIsin(isin);
            security.setTickerSymbol(tickerSymbol);
            security.setType(type);
        }

        public void readFromSecurity()
        {
            setName(security.getName());
            setIsin(security.getIsin());
            setTickerSymbol(security.getTickerSymbol());
            setType(security.getType());
        }

    }

    private Model model;

    protected SecurityMasterDataPage(Client client, Security security)
    {
        super(Messages.EditWizardMasterData);
        setTitle(Messages.EditWizardMasterDataTitle);
        setDescription(Messages.EditWizardMasterDataDescription);

        this.model = new Model(client, security);
    }

    @Override
    public void beforePage()
    {
        model.readFromSecurity();
    }

    @Override
    public void afterPage()
    {
        model.applyChanges();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        final BindingHelper bindings = new BindingHelper(model)
        {
            @Override
            public void onValidationStatusChanged(IStatus status)
            {
                boolean isOK = status.getSeverity() == IStatus.OK;
                setErrorMessage(isOK ? null : status.getMessage());
                setPageComplete(isOK);
            }
        };

        bindings.bindMandatoryStringInput(container, Messages.ColumnName, "name").setFocus(); //$NON-NLS-1$
        bindings.bindMandatoryStringInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol"); //$NON-NLS-1$
        bindings.bindComboViewer(container, Messages.ColumnSecurityType, "type", new LabelProvider() //$NON-NLS-1$
        {
            @Override
            public String getText(Object element)
            {
                return ((AssetClass) element).name();
            }
        }, AssetClass.values());
    }

}

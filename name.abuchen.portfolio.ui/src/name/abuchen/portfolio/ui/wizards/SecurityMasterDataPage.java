package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

public class SecurityMasterDataPage extends AbstractWizardPage
{
    public static final String PAGE_NAME = "masterdata"; //$NON-NLS-1$

    static class Model extends BindingHelper.Model
    {
        private Security security;

        private String name;
        private String isin;
        private String tickerSymbol;
        private String wkn;
        private String finanzenFeedURL;
        private boolean isRetired;

        public Model(Client client, Security security)
        {
            super(client);

            setSecurity(security);
        }

        public void setSecurity(Security sec)
        {
            this.security = sec;
            readFromSecurity();
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

        public String getWkn()
        {
            return wkn;
        }

        public void setWkn(String wkn)
        {
            firePropertyChange("wkn", this.tickerSymbol, this.wkn = wkn); //$NON-NLS-1$
        }

        public boolean isRetired()
        {
            return isRetired;
        }

        public void setRetired(boolean isRetired)
        {
            firePropertyChange("retired", this.isRetired, this.isRetired = isRetired); //$NON-NLS-1$
        }

        public String getFinanzenFeedURL()
        {
            return finanzenFeedURL;
        }

        public void setFinanzenFeedURL(String finanzenFeedURL)
        {
            firePropertyChange("finanzenFeedURL", this.finanzenFeedURL, this.finanzenFeedURL = finanzenFeedURL); //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {
            security.setName(name);
            security.setIsin(isin);
            security.setTickerSymbol(tickerSymbol);
            security.setWkn(wkn);
            security.setRetired(isRetired);
            security.setQuoteFeedURL(finanzenFeedURL);
        }

        public void readFromSecurity()
        {
            setName(security.getName());
            setIsin(security.getIsin());
            setTickerSymbol(security.getTickerSymbol());
            setWkn(security.getWkn());
            setRetired(security.isRetired());
            setFinanzenFeedURL(security.getQuoteFeedURL());
        }

    }

    private BindingHelper bindings;
    private EditSecurityModel model;

    protected SecurityMasterDataPage(EditSecurityModel model)
    {
        super(PAGE_NAME);

        setTitle(Messages.EditWizardMasterDataTitle);
        setDescription(Messages.EditWizardMasterDataDescription);
        this.model = model;

        setTitle(Messages.EditWizardMasterDataTitle);
        setDescription(Messages.EditWizardMasterDataDescription);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        bindings = new BindingHelper(model)
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
        bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnWKN, "wkn"); //$NON-NLS-1$
        bindings.bindStringInput(container, "Kurs URL", "finanzenFeedURL");
        bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$

        Link link = new Link(container, SWT.UNDERLINE_LINK);
        link.setText(Messages.EditWizardMasterDataLinkToSearch);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(link);

        link.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getContainer().showPage(getWizard().getPage(SearchSecurityWizardPage.PAGE_NAME));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });

    }
}

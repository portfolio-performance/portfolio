package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
        private AssetClass type;

        public Model(Client client, Security security)
        {
            super(client);

            this.security = security;

            name = security.getName();
            isin = security.getIsin();
            tickerSymbol = security.getTickerSymbol();
            wkn = security.getWkn();
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

        public String getWkn()
        {
            return wkn;
        }

        public void setWkn(String wkn)
        {
            firePropertyChange("wkn", this.tickerSymbol, this.wkn = wkn); //$NON-NLS-1$
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
            security.setWkn(wkn);
            security.setType(type);
        }

        public void readFromSecurity()
        {
            setName(security.getName());
            setIsin(security.getIsin());
            setTickerSymbol(security.getTickerSymbol());
            setWkn(security.getWkn());
            setType(security.getType());
        }

    }

    private BindingHelper bindings;
    private Model model;

    private IndustryClassification taxonomy;
    private Label classication;

    protected SecurityMasterDataPage(Client client, Security security)
    {
        super(PAGE_NAME);
        
        this.taxonomy = client.getIndustryTaxonomy();
        this.model = new Model(client, security);
        
        setTitle(Messages.EditWizardMasterDataTitle);
        setDescription(Messages.EditWizardMasterDataDescription);

    }

    @Override
    public void beforePage()
    {
        model.readFromSecurity();
        bindings.getBindingContext().updateModels();
        updateIndustryClassification();
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
        bindings.bindComboViewer(container, Messages.ColumnSecurityType, "type", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((AssetClass) element).toString();
                            }
                        }, AssetClass.values());

        addIndustryPicker(container);

        Link link = new Link(container, SWT.UNDERLINE_LINK);
        link.setText(Messages.EditWizardMasterDataLinkToSearch);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(link);

        link.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                setPageComplete(false);
                getContainer().showPage(getWizard().getPage(SearchSecurityWizardPage.PAGE_NAME));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });

    }

    private void addIndustryPicker(Composite container)
    {
        Label label = new Label(container, SWT.NONE);
        label.setText(Messages.ShortLabelIndustry);

        Composite industryPicker = new Composite(container, SWT.NONE);

        classication = new Label(industryPicker, SWT.WRAP | SWT.BORDER | SWT.READ_ONLY);

        Button button = new Button(industryPicker, SWT.PUSH);
        button.setText(Messages.LabelChoose);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getContainer().showPage(getWizard().getPage(IndustryClassificationPage.PAGE_NAME));
            }
        });

        // layout: composite
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(industryPicker);

        // layout: label + button
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(0, 0).applyTo(industryPicker);
        int height = classication.getFont().getFontData()[0].getHeight();
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).hint(100, height * 4)
                        .applyTo(classication);
        GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.BEGINNING).applyTo(button);
    }

    private void updateIndustryClassification()
    {
        IndustryClassification.Category category = taxonomy.getCategoryById(model.security.getIndustryClassification());
        if (category != null)
        {
            StringBuilder buf = new StringBuilder();

            while (category != null)
            {
                if (buf.length() > 0)
                    buf.insert(0, " » "); //$NON-NLS-1$

                buf.insert(0, category.getLabel());
                category = category.getParent();

                if (category.getParent() == null)
                    break;
            }

            classication.setText(buf.toString().replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            classication.setText("---"); //$NON-NLS-1$
        }
    }
}

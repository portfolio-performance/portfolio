package name.abuchen.portfolio.ui.wizards.client;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ImportIndizesPage extends AbstractWizardPage
{
    private static final class ProposedSecurities
    {
        private String label;
        private List<Security> securities = new ArrayList<>();

        public ProposedSecurities(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private Client client;
    private List<ProposedSecurities> proposals = new ArrayList<>();

    private Combo comboDropDown;

    public ImportIndizesPage(Client client)
    {
        super(ImportIndizesPage.class.getSimpleName());
        this.client = client;
        setTitle(Messages.NewFileWizardSecurityTitle);
        setDescription(Messages.NewFileWizardSecurityDescription);

        ResourceBundle bundle = ResourceBundle.getBundle("name.abuchen.portfolio.ui.wizards.client.index"); //$NON-NLS-1$
        String indices = bundle.getString("proposals"); //$NON-NLS-1$
        String[] indAr = indices.split(","); //$NON-NLS-1$
        for (String index : indAr)
        {
            String label = bundle.getString(index + ".name"); //$NON-NLS-1$
            ProposedSecurities proposal = new ProposedSecurities(label);
            proposals.add(proposal);

            String[] values = bundle.getString(index).split(","); //$NON-NLS-1$
            for (String ticker : values)
            {
                String key = index + '.' + ticker;

                Security security = new Security();
                security.setTickerSymbol(ticker);
                security.setName(bundle.getString(key + ".name")); //$NON-NLS-1$
                security.setIsin(safeGetString(bundle, key + ".isin")); //$NON-NLS-1$
                security.setFeed("YAHOO"); //$NON-NLS-1$
                security.setCurrencyCode(
                                "XXX".equals(safeGetString(bundle, key + ".currency")) ? null : CurrencyUnit.EUR); //$NON-NLS-1$ //$NON-NLS-2$
                proposal.securities.add(security);
            }
        }
    }

    private String safeGetString(ResourceBundle bundle, String key)
    {
        try
        {
            return bundle.getString(key);
        }
        catch (MissingResourceException ignore)
        {
            return null;
        }
    }

    @Override
    public void beforePage()
    {
        comboDropDown.notifyListeners(SWT.Selection, new Event());
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().applyTo(container);

        comboDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(comboDropDown);
        final ComboViewer comboViewer = new ComboViewer(comboDropDown);
        comboViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboViewer.setInput(proposals);

        Composite tableContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(tableContainer);
        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        final TableViewer tViewer = new TableViewer(tableContainer);

        comboViewer.addSelectionChangedListener(event -> {
            ProposedSecurities element = (ProposedSecurities) ((IStructuredSelection) event.getSelectionProvider()
                            .getSelection()).getFirstElement();
            if (element != null)
                tViewer.setInput(element.securities);
        });

        Table table = tViewer.getTable();
        table.setHeaderVisible(true);

        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        layout.setColumnData(aCol.getColumn(), new ColumnWeightData(90));
        aCol.getColumn().setText(Messages.ColumnSecurity);
        aCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.SECURITY.image();
            }
        });

        aCol = new TableViewerColumn(tViewer, SWT.NONE);
        layout.setColumnData(aCol.getColumn(), new ColumnWeightData(10));
        aCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                return client.getSecurities().contains(element) ? Images.CHECK.image() : null;
            }
        });

        tViewer.addDoubleClickListener(event -> {
            Security security = (Security) ((IStructuredSelection) event.getSelection()).getFirstElement();

            if (security != null && !client.getSecurities().contains(security))
            {
                client.addSecurity(security);
                tViewer.refresh(security);
            }
        });

        Button button = new Button(container, SWT.PUSH);
        button.setText(Messages.NewFileWizardAddAll);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(button);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ProposedSecurities proposal = (ProposedSecurities) ((IStructuredSelection) comboViewer.getSelection())
                                .getFirstElement();
                for (Security security : proposal.securities)
                {
                    if (!client.getSecurities().contains(security))
                        client.addSecurity(security);
                }

                tViewer.refresh();
            }
        });

        comboDropDown.select(0);

        container.pack();
        setPageComplete(true);
    }
}

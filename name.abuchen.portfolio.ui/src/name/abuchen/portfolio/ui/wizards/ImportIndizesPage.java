package name.abuchen.portfolio.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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

public class ImportIndizesPage extends AbstractWizardPage
{
    private Client client;
    private Map<String, List<Security>> secs = new HashMap<String, List<Security>>();

    private Combo comboDropDown;

    public ImportIndizesPage(Client client)
    {
        super(ImportIndizesPage.class.getSimpleName());
        this.client = client;
        setTitle(Messages.NewFileWizardSecurityTitle);
        setDescription(Messages.NewFileWizardSecurityDescription);

        ResourceBundle bundle = ResourceBundle.getBundle("name.abuchen.portfolio.ui.wizards.index"); //$NON-NLS-1$
        String indices = bundle.getString("indices"); //$NON-NLS-1$
        String[] indAr = indices.split(","); //$NON-NLS-1$
        for (String index : indAr)
        {
            List<Security> indexSec = new ArrayList<Security>();
            String indexWerte = bundle.getString(index);
            String[] values = indexWerte.split(","); //$NON-NLS-1$
            for (String ticker : values)
            {
                String key = index + '.' + ticker;
                String name = bundle.getString(key + ".name"); //$NON-NLS-1$
                String isin = bundle.getString(key + ".isin"); //$NON-NLS-1$
                indexSec.add(new Security(name, isin, ticker, AssetClass.EQUITY, "YAHOO")); //$NON-NLS-1$
            }
            secs.put(index, indexSec);
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
        for (Entry<String, List<Security>> entry : secs.entrySet())
        {
            comboDropDown.add(entry.getKey());
            comboDropDown.setData(entry.getKey(), entry.getValue());
        }

        Composite tableContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(tableContainer);
        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        final TableViewer tViewer = new TableViewer(tableContainer);

        comboDropDown.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Combo c = (Combo) e.widget;
                String text = c.getText();
                tViewer.setInput(c.getData(text));
            }
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
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
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
                return client.getSecurities().contains(element) ? PortfolioPlugin.image(PortfolioPlugin.IMG_CHECK)
                                : null;
            }
        });

        tViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                Security security = (Security) ((IStructuredSelection) event.getSelection()).getFirstElement();

                if (security != null && !client.getSecurities().contains(security))
                {
                    client.addSecurity(security);
                    tViewer.refresh(security);
                }
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
                String text = comboDropDown.getText();
                @SuppressWarnings("unchecked")
                List<Security> secList = (List<Security>) comboDropDown.getData(text);
                for (Security security : secList)
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

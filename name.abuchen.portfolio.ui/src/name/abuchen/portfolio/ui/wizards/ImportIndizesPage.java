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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;


public class ImportIndizesPage extends AbstractWizardPage
{
    Client client;
    Text accountName;
    Map<String, List<Security>> secs = null;
   
    public ImportIndizesPage(Client client)
    {
        super("New ...");
        secs = new HashMap<String, List<Security>>();
        this.client = client;
        setTitle("Import standard securities into client");
        ResourceBundle bundle = ResourceBundle
                        .getBundle("name.abuchen.portfolio.model.index");
        String indices = bundle.getString("indices");
        String[] indAr = indices.split(",");
        for (String index: indAr) {
            List<Security> indexSec = new ArrayList<Security>();
            String indexWerte = bundle.getString(index);
            String[] werte = indexWerte.split(",");
            for (String ticker: werte) {
                String name = bundle.getString(index + "." + ticker + ".name");
                String isin = bundle.getString(index + "." + ticker + ".isin");
                indexSec.add(new Security(name, isin, ticker, AssetClass.EQUITY,"YAHOO"));
            }
            secs.put(index, indexSec);
        }
    }
    
    
    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout());
        Combo comboDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
        for (Entry<String, List<Security>> entry : secs.entrySet()) {
            comboDropDown.add(entry.getKey());
            comboDropDown.setData(entry.getKey(), entry.getValue());
        }
        final TableViewer tViewer = new TableViewer(container);
        comboDropDown.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Combo c = (Combo) e.widget;
                String text = c.getText();
                List<Security> secList = (List<Security>) c.getData(text);
                tViewer.setInput(secList);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                System.out.println("default " + e);
            }
            
        });
        Table table = tViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gridData = new GridData();
        gridData.heightHint = 300;
        table.setLayoutData(gridData);
        tViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        TableViewerColumn aCol = new TableViewerColumn(tViewer, SWT.NONE);
        aCol.getColumn().setText("Security");
        aCol.getColumn().setWidth(200);
        aCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }
        });
        container.pack();
        setPageComplete(true);
    }

}


package name.abuchen.portfolio.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PredefinedSecurity;

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
    Map<String, List<PredefinedSecurity>> secs = null;
   
    public ImportIndizesPage(Client client)
    {
        super("New ...");
        secs = new HashMap<String, List<PredefinedSecurity>>();
        this.client = client;
        setTitle("Import standard securities into client");
        String[] indices = new String[]{"DAX30"};
        for (String index: indices) {
            secs.put(index, new ArrayList<PredefinedSecurity>(PredefinedSecurity.read(index).values()));
        }
    }
    
    
    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout());
        Combo comboDropDown = new Combo(container, SWT.DROP_DOWN | SWT.BORDER);
        for (Entry<String, List<PredefinedSecurity>> entry : secs.entrySet()) {
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
                List<PredefinedSecurity> secList = (List<PredefinedSecurity>) c.getData(text);
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
                return ((PredefinedSecurity) element).getName();
            }
        });
        container.pack();
        setPageComplete(true);
    }

}


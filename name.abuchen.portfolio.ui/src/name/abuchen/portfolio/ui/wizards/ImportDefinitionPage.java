package name.abuchen.portfolio.ui.wizards;

import java.io.File;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ImportDefinitionPage extends AbstractWizardPage
{
    private Table table;

    public ImportDefinitionPage(Client client, File inputFile)
    {
        super("importdefinition"); //$NON-NLS-1$
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Label lblTarget = new Label(container, SWT.NONE);
        lblTarget.setText("Target:");
        Combo cmbTarget = new Combo(container, SWT.NONE);

        Label lblDelimiter = new Label(container, SWT.NONE);
        lblDelimiter.setText("Delimiter:");
        Combo cmbDelimiter = new Combo(container, SWT.NONE);

        Label lblSkipLines = new Label(container, SWT.NONE);
        lblSkipLines.setText("Skip Lines:");
        Spinner cmbSkipLines = new Spinner(container, SWT.BORDER);

        Label lblEncoding = new Label(container, SWT.NONE);
        lblEncoding.setText("Encoding:");
        Combo cmbEncoding = new Combo(container, SWT.NONE);

        Button cbxFirstLineIsHeader = new Button(container, SWT.CHECK);
        cbxFirstLineIsHeader.setText("First line contains header");

        Composite compositeTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        Label biggest = maxWidth(lblTarget, lblDelimiter, lblEncoding);

        FormData data = new FormData();
        data.top = new FormAttachment(cmbTarget, 0, SWT.CENTER);
        lblTarget.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(biggest, 5);
        data.right = new FormAttachment(50, -5);
        cmbTarget.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbDelimiter, 0, SWT.CENTER);
        lblDelimiter.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbTarget, 5);
        data.left = new FormAttachment(cmbTarget, 0, SWT.LEFT);
        data.right = new FormAttachment(50, -5);
        cmbDelimiter.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbEncoding, 0, SWT.CENTER);
        lblEncoding.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbDelimiter, 5);
        data.left = new FormAttachment(cmbDelimiter, 0, SWT.LEFT);
        data.right = new FormAttachment(50, -5);
        cmbEncoding.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(50, 5);
        data.top = new FormAttachment(cmbSkipLines, 0, SWT.CENTER);
        lblSkipLines.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbTarget, 5);
        data.left = new FormAttachment(lblSkipLines, 5);
        data.right = new FormAttachment(100, 0);
        cmbSkipLines.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbSkipLines, 5);
        data.left = new FormAttachment(cmbSkipLines, 0, SWT.LEFT);
        data.right = new FormAttachment(100, 0);
        cbxFirstLineIsHeader.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbEncoding, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);

        compositeTable.setLayoutData(data);

        //
        // table & columns
        //

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        TableViewer tableViewer = new TableViewer(compositeTable, SWT.BORDER);
        table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnDate);
        layout.setColumnData(column, new ColumnPixelData(80, true));

    }

    private Label maxWidth(Label... labels)
    {
        int width = 0;
        Label answer = null;

        for (int ii = 0; ii < labels.length; ii++)
        {
            int w = labels[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (w >= width)
                answer = labels[ii];
        }

        return answer;
    }
}

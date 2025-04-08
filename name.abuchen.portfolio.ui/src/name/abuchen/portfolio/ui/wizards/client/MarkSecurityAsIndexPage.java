package name.abuchen.portfolio.ui.wizards.client;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

/* package */ class MarkSecurityAsIndexPage extends AbstractWizardPage
{
    private Client client;

    private CheckboxTableViewer tableViewer;

    public MarkSecurityAsIndexPage(Client client)
    {
        super("mark-security-as-index"); //$NON-NLS-1$
        this.client = client;

        setTitle(Messages.MarkSecurityPageTitle);
        setDescription(Messages.MarkSecurityPageDescription);
    }

    @Override
    public void beforePage()
    {
        if (tableViewer.getTable().getItemCount() == 0)
        {
            List<Security> candidates = client.getSecurities().stream().filter(s -> !s.hasTransactions(client))
                            .sorted(new Security.ByName()).collect(Collectors.toList());

            tableViewer.setInput(candidates);
            tableViewer.setCheckedElements(candidates.stream()
                            .filter(s -> s.getTickerSymbol() != null && s.getTickerSymbol().startsWith("^")).toArray()); //$NON-NLS-1$
        }
    }

    public Stream<Security> getSelectedSecurities()
    {
        return Arrays.stream(tableViewer.getCheckedElements()).map(o -> (Security) o);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);
        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        tableViewer = CheckboxTableViewer.newCheckList(tableArea, SWT.BORDER | SWT.CHECK | SWT.MULTI);

        CopyPasteSupport.enableFor(tableViewer);

        final Table table = tableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        tableViewer.setLabelProvider(new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return Images.SECURITY.image();
            }

            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }
        });
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        container.pack();
        setPageComplete(true);
    }
}

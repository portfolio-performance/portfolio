package name.abuchen.portfolio.ui.dialogs;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class ListSelectionDialog extends Dialog
{
    private class ElementFilter extends ViewerFilter
    {
        private Pattern filterPattern;

        public void setSearchPattern(String pattern)
        {
            if (pattern != null)
                filterPattern = Pattern.compile(".*" + Pattern.quote(pattern) + ".*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
            else
                filterPattern = null;
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {
            if (filterPattern == null)
                return true;

            String text = labelProvider.getText(element);
            if (text == null)
                return false;
            return filterPattern.matcher(text).matches();
        }
    }

    private LabelProvider labelProvider;

    private String title;
    private String message = ""; //$NON-NLS-1$
    private boolean isMultiSelection = true;

    private String propertyLabel;
    private String property = ""; //$NON-NLS-1$

    private Object[] elements;
    private Object[] selected;

    private TableViewer tableViewer;
    private ElementFilter elementFilter;
    private Text searchText;

    public ListSelectionDialog(Shell parentShell, LabelProvider labelProvider)
    {
        super(parentShell);
        this.labelProvider = labelProvider;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setMultiSelection(boolean isMultiSelection)
    {
        this.isMultiSelection = isMultiSelection;
    }

    public void setElements(List<?> elements)
    {
        this.elements = elements.toArray();
    }

    public Object[] getResult()
    {
        return selected != null ? selected : new Object[0];
    }

    public String getProperty()
    {
        return property;
    }

    public void setPropertyLabel(String propertyLabel)
    {
        this.propertyLabel = propertyLabel;
    }

    @Override
    protected void setShellStyle(int newShellStyle)
    {
        super.setShellStyle(newShellStyle | SWT.RESIZE);
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        getShell().setText(title);
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(400, 300).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

        if (propertyLabel != null)
        {
            Label label = new Label(container, SWT.NONE);
            label.setText(propertyLabel);

            Text input = new Text(container, SWT.BORDER);
            input.setText(property);
            input.addFocusListener(FocusListener.focusGainedAdapter(e -> input.selectAll()));
            input.addModifyListener(e -> property = input.getText());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(input);
            input.setFocus(); // when text input visible, set focus
        }

        Label label = new Label(container, SWT.None);
        label.setText(this.message);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(label);

        searchText = new Text(container, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(searchText);
        if (propertyLabel == null)
            searchText.setFocus(); // only set focus if text input invisible

        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        elementFilter = new ElementFilter();

        int style = SWT.BORDER | SWT.FULL_SELECTION;
        if (isMultiSelection)
            style |= SWT.MULTI;
        tableViewer = new TableViewer(tableArea, style);
        CopyPasteSupport.enableFor(tableViewer);
        final Table table = tableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        tableViewer.setLabelProvider(labelProvider);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.addFilter(elementFilter);
        tableViewer.setInput(elements);

        tableViewer.setComparator(new ViewerComparator());

        hookListener();

        return composite;
    }

    private void hookListener()
    {
        tableViewer.addSelectionChangedListener(
                        event -> selected = ((IStructuredSelection) event.getSelection()).toArray());

        tableViewer.addDoubleClickListener(event -> {
            selected = ((IStructuredSelection) event.getSelection()).toArray();
            okPressed();
        });

        searchText.addModifyListener(e -> {
            String pattern = searchText.getText().trim();
            if (pattern.length() == 0)
            {
                elementFilter.setSearchPattern(null);
                tableViewer.refresh();
            }
            else
            {
                elementFilter.setSearchPattern(pattern);
                tableViewer.refresh();
            }
        });
    }
}

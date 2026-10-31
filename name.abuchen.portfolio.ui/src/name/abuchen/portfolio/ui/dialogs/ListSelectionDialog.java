package name.abuchen.portfolio.ui.dialogs;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.util.Colors;
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

    private ILabelProvider labelProvider;
    private ViewerComparator viewerComparator;

    private String title;
    private String message = ""; //$NON-NLS-1$
    private boolean isMultiSelection = true;

    private String propertyLabel;
    private String property = ""; //$NON-NLS-1$

    private Object[] elements;
    private Object[] selected;
    private Object initialSelection;
    private Set<Object> disabledElements = Set.of();

    private TableViewer tableViewer;
    private ElementFilter elementFilter;
    private Text searchText;

    public ListSelectionDialog(Shell parentShell, ILabelProvider labelProvider)
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

    public void setViewerComparator(ViewerComparator viewerComparator)
    {
        this.viewerComparator = viewerComparator;
    }

    public void setElements(List<?> elements)
    {
        this.elements = elements.toArray();
    }

    /**
     * Pre-selects the given element in the virtual table when the dialog is
     * opened. Without a selection the result stays empty, which callers treat
     * as "no change".
     */
    public void setInitialSelection(Object initialSelection)
    {
        this.initialSelection = initialSelection;
    }

    /**
     * Shows the given elements grayed out. They remain visible but are never
     * part of the result, and OK is enabled only while the selection contains
     * at least one other element.
     */
    public void setDisabledElements(Collection<?> disabledElements)
    {
        this.disabledElements = new HashSet<>(disabledElements);
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
        updateOkButton();
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

        if (message != null && !message.isEmpty())
        {
            Label label = new Label(container, SWT.NONE | SWT.WRAP);
            label.setText(this.message);
            GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(label);
        }

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

        int style = SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL;
        if (isMultiSelection)
            style |= SWT.MULTI;

        tableViewer = new TableViewer(tableArea, style);
        tableViewer.setUseHashlookup(true);
        CopyPasteSupport.enableFor(tableViewer);

        final Table table = tableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        tableViewer.setLabelProvider(disabledElements.isEmpty() ? labelProvider : new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return labelProvider.getText(element);
            }

            @Override
            public Image getImage(Object element)
            {
                return labelProvider.getImage(element);
            }

            @Override
            public Color getForeground(Object element)
            {
                return disabledElements.contains(element) ? Colors.theme().grayForeground() : null;
            }
        });
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.addFilter(elementFilter);
        tableViewer.setComparator(viewerComparator != null ? viewerComparator : new ViewerComparator());
        tableViewer.setInput(elements);

        // must be hooked up before the initial selection is applied, otherwise
        // the pre-selected element never becomes part of the result
        hookListener();

        if (initialSelection != null)
            tableViewer.setSelection(new StructuredSelection(initialSelection));

        return composite;
    }

    private void hookListener()
    {
        tableViewer.addSelectionChangedListener(event -> {
            selected = enabledElements((IStructuredSelection) event.getSelection());
            updateOkButton();
        });

        tableViewer.addDoubleClickListener(event -> {
            selected = enabledElements((IStructuredSelection) event.getSelection());
            if (disabledElements.isEmpty() || selected.length > 0)
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

    private Object[] enabledElements(IStructuredSelection selection)
    {
        return selection.toList().stream().filter(e -> !disabledElements.contains(e)).toArray();
    }

    private void updateOkButton()
    {
        if (disabledElements.isEmpty())
            return;

        var button = getButton(OK);
        if (button != null && !button.isDisposed())
            button.setEnabled(enabledElements(tableViewer.getStructuredSelection()).length > 0);
    }
}

package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.Type;

public class DataSeriesSelectionDialog extends Dialog
{
    private static class Node
    {
        private Node parent;
        private List<Node> children = new ArrayList<>();

        private String label;
        private DataSeries dataSeries;

        public Node(String label)
        {
            this.label = label;
        }
    }

    private static class NodeContentProvider implements ITreeContentProvider
    {

        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Node[])
                return (Node[]) inputElement;
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            Node parent = (Node) parentElement;
            return parent.children.toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return ((Node) element).parent;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((Node) element).children.isEmpty();
        }
    }

    private class ElementFilter extends ViewerFilter
    {
        private Pattern filterPattern;

        public void setSearchPattern(String pattern)
        {
            if (pattern != null)
                filterPattern = Pattern.compile(".*" + pattern + ".*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
            else
                filterPattern = null;
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element)
        {
            if (filterPattern == null)
                return true;

            Node node = (Node) element;

            // include all categories
            if (!node.children.isEmpty())
                return true;

            return filterPattern.matcher(node.label).matches();
        }
    }

    private boolean isMultiSelection = true;

    private Node[] elements;
    private Object[] selected;

    private TreeViewer treeViewer;
    private ElementFilter elementFilter;
    private Text searchText;

    public DataSeriesSelectionDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public void setMultiSelection(boolean isMultiSelection)
    {
        this.isMultiSelection = isMultiSelection;
    }

    public void setElements(List<DataSeries> elements)
    {
        Map<String, Node> type2node = new HashMap<>();
        Map<Object, Node> group2node = new HashMap<>();

        for (DataSeries series : elements)
        {
            Node child = new Node(series.getSearchLabel());
            child.dataSeries = series;

            Node parent = type2node.computeIfAbsent(map(series.getType()), Node::new);

            if (series.getGroup() != null)
            {
                Node group = group2node.computeIfAbsent(series.getGroup(), g -> {
                    Node n = new Node(g.toString());
                    n.parent = parent;
                    parent.children.add(n);
                    return n;
                });
                child.parent = group;
                group.children.add(child);
            }
            else
            {
                child.parent = parent;
                parent.children.add(child);
            }
        }

        this.elements = type2node.values().toArray(new Node[0]);
    }

    /**
     * Reduce number of first-level folders to a meaningful set for the
     * end-user.
     */
    private String map(Type type)
    {
        switch (type)
        {
            case SECURITY:
                return Messages.LabelSecurities;
            case SECURITY_BENCHMARK:
                return Messages.LabelBenchmarks;
            case CLASSIFICATION:
                return Messages.LabelTaxonomies;
            case CLIENT:
            case CLIENT_PRETAX:
                return Messages.LabelCommon;
            default:
                return Messages.LabelClientFilterDialogTitle;
        }
    }

    public List<DataSeries> getResult()
    {
        List<DataSeries> result = new ArrayList<>();
        for (Object node : selected)
        {
            Node n = (Node) node;
            if (n.dataSeries != null)
                result.add(n.dataSeries);
        }
        return result;
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
        getShell().setText(Messages.ChartSeriesPickerTitle);
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(400, 300).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        Label label = new Label(container, SWT.None);
        label.setText(Messages.ChartSeriesPickerTitle);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

        searchText = new Text(container, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(searchText);
        searchText.setFocus();

        Composite treeArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(treeArea);
        treeArea.setLayout(new FillLayout());

        TreeColumnLayout layout = new TreeColumnLayout();
        treeArea.setLayout(layout);

        elementFilter = new ElementFilter();

        int style = SWT.BORDER | SWT.FULL_SELECTION;
        if (isMultiSelection)
            style |= SWT.MULTI;
        treeViewer = new TreeViewer(treeArea, style);
        final Tree table = treeViewer.getTree();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        treeViewer.setLabelProvider(new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                Node node = (Node) element;
                return node.dataSeries != null ? node.dataSeries.getImage() : Images.UNASSIGNED_CATEGORY.image();
            }

            @Override
            public String getText(Object element)
            {
                return ((Node) element).label;
            }
        });
        treeViewer.setContentProvider(new NodeContentProvider());
        treeViewer.addFilter(elementFilter);
        treeViewer.setInput(elements);
        treeViewer.setComparator(new ViewerComparator());

        hookListener();

        treeViewer.expandAll();

        return composite;
    }

    @Override
    protected Control createButtonBar(Composite parent)
    {
        Control control = super.createButtonBar(parent);
        getButton(OK).setEnabled(false);
        return control;
    }

    private void hookListener()
    {
        treeViewer.addSelectionChangedListener(event -> {
            selected = ((IStructuredSelection) event.getSelection()).toArray();
            getButton(OK).setEnabled(!getResult().isEmpty());
        });

        treeViewer.addDoubleClickListener(event -> {
            selected = ((IStructuredSelection) event.getSelection()).toArray();

            if (!getResult().isEmpty())
                okPressed();
        });

        searchText.addModifyListener(e -> {
            String pattern = searchText.getText().trim();
            if (pattern.length() == 0)
                elementFilter.setSearchPattern(null);
            else
                elementFilter.setSearchPattern(pattern);
            treeViewer.refresh();
        });
    }
}

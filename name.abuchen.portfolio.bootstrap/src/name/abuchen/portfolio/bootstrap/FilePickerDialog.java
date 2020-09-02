package name.abuchen.portfolio.bootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class FilePickerDialog extends Dialog
{
    public static class FileInfo
    {
        private List<MPart> parts = new ArrayList<>();
        private String name;
        private String path;

        public FileInfo(String name, String path)
        {
            this.name = name;
            this.path = path;
        }

        public String getName()
        {
            return name;
        }

        public String getPath()
        {
            return path;
        }

        public void addPart(MPart part)
        {
            this.parts.add(part);
        }

        public void addParts(List<MPart> parts)
        {
            this.parts.addAll(parts);
        }

        public List<MPart> getParts()
        {
            return this.parts;
        }
    }

    public static final int SAVE_ALL = 42;
    public static final int SAVE_NONE = 43;

    private LabelProvider labelProvider;

    private Object[] elements;
    private Object[] selected;

    private CheckboxTableViewer tableViewer;

    public FilePickerDialog(Shell parentShell)
    {
        super(parentShell);
        this.labelProvider = new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                FileInfo info = (FileInfo) element;
                String path = info.getPath();
                return path != null ? info.getName() + " (" + path + ")" : info.getName(); //$NON-NLS-1$ //$NON-NLS-2$
            }
        };

        setShellStyle(getShellStyle() | SWT.SHEET);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        createButton(parent, SAVE_ALL, Messages.LabelSaveAll, false);
        createButton(parent, SAVE_NONE, Messages.LabelSaveNone, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == SAVE_ALL || buttonId == SAVE_NONE)
        {
            setReturnCode(buttonId);
            close();
        }
        else
        {
            super.buttonPressed(buttonId);
        }
    }

    public void setElements(Collection<?> elements)
    {
        this.elements = this.selected = elements.toArray();
    }

    public Object[] getResult()
    {
        return selected != null ? selected : new Object[0];
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        getShell().setText(Messages.SaveHandlerTitle);
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
        label.setText(Messages.SaveHandlerTitle);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        Table table = new Table(tableArea, SWT.BORDER | SWT.CHECK | SWT.MULTI);
        tableViewer = new CheckboxTableViewer(table);
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        tableViewer.setLabelProvider(labelProvider);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setInput(elements);
        tableViewer.setCheckedElements(elements);

        tableViewer.setComparator(new ViewerComparator());

        hookListener();

        return composite;
    }

    private void hookListener()
    {
        tableViewer.addCheckStateListener(event -> selected = tableViewer.getCheckedElements());
    }
}

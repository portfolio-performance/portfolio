package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ErroneousImportFilesPage extends AbstractWizardPage
{
    private class ErrorContentProvider implements ITreeContentProvider
    {
        @Override
        public Object[] getElements(Object inputElement)
        {
            return errors.keySet().toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof File)
                return errors.get(parentElement).toArray();
            else if (parentElement instanceof Throwable)
                return new Object[] { ((Exception) parentElement).getCause() };
            else
                return new Object[0];
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof File
                            || (element instanceof Throwable && ((Throwable) element).getCause() != null);
        }

    }

    private Map<File, List<Exception>> errors;

    public ErroneousImportFilesPage(Map<File, List<Exception>> errors)
    {
        super("errors"); //$NON-NLS-1$

        this.errors = errors;

        setTitle(Messages.LabelError);
        setDescription(Messages.PDFImportWizardErroneousFiles);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        TreeViewer treeViewer = new TreeViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        treeViewer.setContentProvider(new ErrorContentProvider());
        treeViewer.getTree().setHeaderVisible(true);
        treeViewer.getTree().setLinesVisible(true);

        addColumns(treeViewer, layout);

        treeViewer.setInput(errors);
    }

    private void addColumns(TreeViewer viewer, TreeColumnLayout layout)
    {
        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnErrorMessages);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof File)
                {
                    return ((File) element).getName();
                }
                else if (element instanceof Exception)
                {
                    Exception e = (Exception) element;
                    String text = e.getMessage();
                    return text == null || text.isEmpty() ? e.getClass().getName() : text;
                }
                else
                {
                    return String.valueOf(element);
                }
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof File)
                    return Images.SECURITY_RETIRED.image();
                else
                    return Images.WARNING.image();
            }

        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100, true));
    }
}

package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;

public class TreeViewerCSVExporter extends AbstractCSVExporter
{
    private final TreeViewer viewer;

    public TreeViewerCSVExporter(TreeViewer viewer)
    {
        this.viewer = viewer;
    }

    @Override
    protected Shell getShell()
    {
        return viewer.getTree().getShell();
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            ITreeContentProvider provider = (ITreeContentProvider) viewer.getContentProvider();

            int depth = depth(provider);
            int columnCount = viewer.getTree().getColumnCount();

            ILabelProvider[] labels = new ILabelProvider[columnCount];
            extractLabelProvider(labels);

            // write header
            String label = viewer.getTree().getColumn(0).getText();
            for (int ii = 0; ii < depth; ii++)
                printer.print(label + " " + (ii + 1)); //$NON-NLS-1$
            for (int ii = 1; ii < columnCount; ii++)
                printer.print(viewer.getTree().getColumn(ii).getText());
            printer.println();

            // write body
            LinkedList<String> path = new LinkedList<>();
            for (Object element : provider.getElements(null))
            {
                writeLine(printer, provider, labels, depth, path, element);
            }
        }
    }

    private void extractLabelProvider(ILabelProvider[] labels)
    {
        for (int ii = 0; ii < labels.length; ii++)
        {
            // label provider other than ColumnLabelProvider need special
            // treatment as there is no easy #getText method
            IBaseLabelProvider blp = viewer.getLabelProvider(ii);

            if (blp instanceof ILabelProvider)
            {
                labels[ii] = (ILabelProvider) blp;
            }
            else if (blp instanceof SharesLabelProvider)
            {
                labels[ii] = new LabelProvider()
                {
                    @Override
                    public String getText(Object element)
                    {
                        Long value = ((SharesLabelProvider) blp).getValue(element);
                        return value != null ? Values.Share.format(value) : null;
                    }
                };
            }
            else
            {
                labels[ii] = new LabelProvider()
                {
                    @Override
                    public String getText(Object element)
                    {
                        return String.valueOf(element);
                    }
                };
            }
        }
    }

    private void writeLine(CSVPrinter printer, ITreeContentProvider provider, ILabelProvider[] labels, final int depth,
                    LinkedList<String> path, Object element)
    {
        path.add(labels[0].getText(element));

        for (String s : path)
            printer.print(s);

        for (int ii = path.size(); ii < depth; ii++)
            printer.print(""); //$NON-NLS-1$

        for (int ii = 1; ii < labels.length; ii++)
        {
            String text = labels[ii].getText(element);
            printer.print(text != null ? text : ""); //$NON-NLS-1$
        }

        printer.println();

        if (provider.hasChildren(element))
        {
            for (Object child : provider.getChildren(element))
                writeLine(printer, provider, labels, depth, path, child);
        }

        path.removeLast();
    }

    private int depth(final ITreeContentProvider tree)
    {
        int depth = 0;

        for (Object element : tree.getElements(null))
            depth = Math.max(depth, depth(tree, element, 1));
        return depth;
    }

    private int depth(final ITreeContentProvider tree, final Object element, final int depth)
    {
        if (!tree.hasChildren(element))
            return depth;

        int d = depth;

        for (Object child : tree.getChildren(element))
            d = Math.max(d, depth(tree, child, depth + 1));

        return d;
    }
}

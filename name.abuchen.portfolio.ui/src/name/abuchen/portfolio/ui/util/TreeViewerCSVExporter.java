package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Control;

public class TreeViewerCSVExporter extends AbstractCSVExporter
{
    private final TreeViewer viewer;

    public TreeViewerCSVExporter(TreeViewer viewer)
    {
        this.viewer = viewer;
    }

    @Override
    protected Control getControl()
    {
        return viewer.getTree();
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

            ColumnLabelProvider[] labels = new ColumnLabelProvider[columnCount];
            for (int ii = 0; ii < labels.length; ii++)
                labels[ii] = (ColumnLabelProvider) viewer.getLabelProvider(ii);

            // write header
            String label = viewer.getTree().getColumn(0).getText();
            for (int ii = 0; ii < depth; ii++)
                printer.print(label + " " + (ii + 1)); //$NON-NLS-1$
            for (int ii = 1; ii < columnCount; ii++)
                printer.print(viewer.getTree().getColumn(ii).getText());
            printer.println();

            // write body
            LinkedList<String> path = new LinkedList<String>();
            for (Object element : provider.getElements(null))
            {
                writeLine(printer, provider, labels, depth, path, element);
            }
        }
    }

    private void writeLine(CSVPrinter printer, ITreeContentProvider provider, ColumnLabelProvider[] labels,
                    final int depth, LinkedList<String> path, Object element)
    {
        path.add(labels[0].getText(element));

        for (String s : path)
            printer.print(s);

        for (int ii = path.size(); ii < depth; ii++)
            printer.print(""); //$NON-NLS-1$

        for (int ii = 1; ii < labels.length; ii++)
            printer.print(labels[ii].getText(element));

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

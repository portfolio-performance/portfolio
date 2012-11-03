package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedList;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.TreeMapItem;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.widgets.Control;

public class TreeMapItemCSVExporter extends AbstractCSVExporter
{
    private final Control control;
    private final String categoryLabel;
    private final TreeMapItem root;

    public TreeMapItemCSVExporter(Control control, String categoryLabel, TreeMapItem root)
    {
        this.control = control;
        this.categoryLabel = categoryLabel;
        this.root = root;
    }

    @Override
    protected Control getControl()
    {
        return control;
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            int depth = depth(root);

            // write header
            for (int ii = 0; ii < depth; ii++)
                printer.print(categoryLabel + " " + (ii + 1)); //$NON-NLS-1$
            printer.print(Messages.ColumnValue);
            printer.print(Messages.ColumnShareInPercent);
            printer.println();

            // write body
            LinkedList<String> path = new LinkedList<String>();
            for (TreeMapItem child : root.getChildren())
            {
                writeLine(printer, child, depth, path);
            }
        }
        finally
        {
            writer.close();
        }
    }

    private void writeLine(CSVPrinter printer, TreeMapItem element, final int depth, LinkedList<String> path)
    {
        path.add(element.getLabel());

        for (String s : path)
            printer.print(s);

        for (int ii = path.size(); ii < depth; ii++)
            printer.print(""); //$NON-NLS-1$

        printer.print(Values.Amount.format(element.getValuation()));
        printer.print(Values.Percent.format(element.getPercentage()));
        printer.println();

        for (TreeMapItem child : element.getChildren())
            writeLine(printer, child, depth, path);

        path.removeLast();
    }

    private int depth(final TreeMapItem root)
    {
        int depth = 0;

        for (TreeMapItem child : root.getChildren())
            depth = Math.max(depth, depth(child, 1));
        return depth;
    }

    private int depth(final TreeMapItem element, final int depth)
    {
        int d = depth;

        for (TreeMapItem child : element.getChildren())
            d = Math.max(d, depth(child, depth + 1));

        return d;
    }
}

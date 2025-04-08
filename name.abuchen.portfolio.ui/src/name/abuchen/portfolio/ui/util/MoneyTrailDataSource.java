package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.ui.Messages;

public class MoneyTrailDataSource
{
    private Trail trail;

    public MoneyTrailDataSource(Trail trail)
    {
        this.trail = trail;
    }

    public Composite createPlainComposite(Composite parent)
    {
        int depth = trail.getDepth();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);
        GridLayoutFactory.swtDefaults().numColumns(depth + 3).applyTo(composite);

        Label heading = new Label(composite, SWT.NONE);
        heading.setBackground(composite.getBackground());
        heading.setText(trail.getLabel());
        GridDataFactory.fillDefaults().span(depth + 3, 1).applyTo(heading);

        // if the trail contains too many items, it becomes very slow and anyway
        // can only show part of the calculation. With this variable, we limit
        // the number of rows to be created and rendered
        int[] rowsToCreate = new int[] { 50 };

        addRow(rowsToCreate, composite, trail.getRecord(), depth - 1, depth);

        if (rowsToCreate[0] < 0)
        {
            Label info = new Label(composite, SWT.NONE);
            info.setBackground(composite.getBackground());
            info.setText("..."); //$NON-NLS-1$
            GridDataFactory.fillDefaults().span(depth + 3, 1).applyTo(info);
        }

        return composite;
    }

    private Optional<Label> addRow(int[] rowsToCreate, Composite composite, TrailRecord trail, int level, int depth)
    {
        List<Label> inputs = new ArrayList<>();

        for (TrailRecord child : trail.getInputs())
            addRow(rowsToCreate, composite, child, level - 1, depth).ifPresent(inputs::add);

        if (rowsToCreate[0] <= 0)
        {
            // in order to show a message that rows are missing, we need to know
            // that trail did not just accidentally fit into the limit
            rowsToCreate[0] = -10;
            return Optional.empty();
        }

        rowsToCreate[0]--;

        Label date = new Label(composite, SWT.NONE);
        date.setBackground(composite.getBackground());
        if (trail.getDate() != null)
            date.setText(Values.Date.format(trail.getDate()));

        Label label = new Label(composite, SWT.NONE);
        label.setBackground(composite.getBackground());
        label.setText(trail.getLabel());

        Label shares = new Label(composite, SWT.RIGHT);
        shares.setBackground(composite.getBackground());
        GridDataFactory.fillDefaults().applyTo(shares);
        if (trail.getShares() != null)
            shares.setText(Values.Share.format(trail.getShares()));

        Label answer = null;

        for (int index = 0; index < depth; index++)
        {
            Label column = new Label(composite, SWT.RIGHT);
            column.setBackground(composite.getBackground());
            GridDataFactory.fillDefaults().applyTo(column);

            if (index == level)
            {
                answer = column;
                column.setText(trail.getValue() != null ? Values.Money.format(trail.getValue())
                                : Messages.LabelNotAvailable);

                highlight(Arrays.asList(label, column), inputs);
            }
        }

        return Optional.of(answer);
    }

    private void highlight(List<Label> outputs, List<Label> inputs)
    {
        if (inputs.isEmpty())
            return;

        outputs.forEach(label -> label.addMouseTrackListener(new MouseTrackAdapter()
        {
            private Color background = Colors.INFO_TOOLTIP_BACKGROUND;

            @Override
            public void mouseExit(MouseEvent e)
            {
                outputs.forEach(l -> l.setBackground(this.background));
                inputs.forEach(l -> l.setBackground(this.background));
            }

            @Override
            public void mouseEnter(MouseEvent e)
            {
                // background color is theme dependent -> save color to restore
                // during mouseExit
                this.background = outputs.get(0).getBackground();

                outputs.forEach(l -> l.setBackground(Colors.ICON_ORANGE));
                inputs.forEach(l -> l.setBackground(Colors.ICON_GREEN));
            }
        }));
    }
}

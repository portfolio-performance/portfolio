package name.abuchen.portfolio.ui;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;

public class SashHelper
{
    private final String identifier;
    private final IPreferenceStore preferences;

    private int[] defaultWidths;

    public SashHelper(String identifier, IPreferenceStore preferences)
    {
        this.identifier = identifier;
        this.preferences = preferences;

        load();
    }

    public void setConstantWidth(int[] defaultWidths)
    {
        if (this.defaultWidths == null)
            this.defaultWidths = defaultWidths;
    }

    public void attachTo(SashForm sash)
    {
        Objects.requireNonNull(sash);

        sash.addListener(SWT.Resize, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                updateDefaultWeights(sash);
                applyWeights(sash);
            }
        });

        sash.addDisposeListener(e -> save(sash));

        applyWeights(sash);
    }

    private void load()
    {
        String config = preferences.getString(identifier);
        if (config != null)
        {
            try
            {
                String[] parts = config.split(","); //$NON-NLS-1$
                int[] widths = new int[parts.length];
                for (int ii = 0; ii < widths.length; ii++)
                    widths[ii] = Integer.parseInt(parts[ii]);

                defaultWidths = widths;
            }
            catch (NumberFormatException ignore)
            {
                // ignore -> assign weight from scratch
            }
        }
    }

    private void save(SashForm sash)
    {
        updateDefaultWeights(sash);

        StringJoiner config = new StringJoiner(","); //$NON-NLS-1$
        for (int w : defaultWidths)
            config.add(String.valueOf(w));

        preferences.putValue(identifier, config.toString());
    }

    private void updateDefaultWeights(SashForm sash)
    {
        Control[] children = sash.getChildren();

        for (int ii = 0; ii < defaultWidths.length; ii++)
        {
            if (defaultWidths[ii] > 0 && ii < children.length)
            {
                int width = children[ii].getBounds().width;
                if (width > 0)
                    defaultWidths[ii] = width;
            }
        }
    }

    private void applyWeights(SashForm sash)
    {
        int numOfChildren = (int) Arrays.stream(sash.getChildren()).filter(c -> !(c instanceof Sash)).count();

        if (defaultWidths == null)
            defaultWidths = new int[numOfChildren];

        int parentWidth = getParentWidth(sash);

        int[] weights = new int[numOfChildren];

        int columnsWithoutWidth = 0;
        for (int ii = 0; ii < weights.length; ii++)
        {
            if (defaultWidths[ii] > 0)
            {
                weights[ii] = defaultWidths[ii];
                parentWidth -= weights[ii];
            }
            else
            {
                columnsWithoutWidth++;
            }
        }

        for (int ii = 0; ii < weights.length; ii++)
        {
            if (weights[ii] == 0)
                weights[ii] = parentWidth / columnsWithoutWidth;
        }

        sash.setWeights(weights);
    }

    private int getParentWidth(SashForm sash)
    {
        Composite parent = sash.getParent().getParent();
        int parentWidth = parent.getBounds().width;
        if (parentWidth == 0)
        {
            // #pack is required if parent has never been rendered before
            parent.pack();
            parentWidth = parent.getBounds().width;
        }

        parentWidth -= sash.getSashWidth();
        return parentWidth;
    }
}

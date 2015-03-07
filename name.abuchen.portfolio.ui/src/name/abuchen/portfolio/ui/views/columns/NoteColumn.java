package name.abuchen.portfolio.ui.views.columns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.StringEditingSupport;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

public class NoteColumn extends Column
{
    public NoteColumn()
    {
        super("note", Messages.ColumnNote, SWT.LEFT, 22); //$NON-NLS-1$

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Annotated n = Adaptor.adapt(Annotated.class, e);
                return n != null ? n.getNote() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getText(e);
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getText(e);
                if (note == null)
                    return null;

                // add a word boundary to correctly match a full line
                note += "X"; //$NON-NLS-1$

                StringBuilder tooltip = new StringBuilder();
                Pattern p = Pattern.compile(".{0,80}\\b[ \\t\\n\\x0b\\r\\f,.]*"); //$NON-NLS-1$
                Matcher m = p.matcher(note);
                while (m.find())
                {
                    if (tooltip.length() > 0)
                        tooltip.append("\n"); //$NON-NLS-1$

                    String substring = note.substring(m.start(), m.end());
                    tooltip.append(substring.replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
                }

                // remove added character needed to create a word boundary
                return tooltip.substring(0, tooltip.length() - 2);
            }

        });
        setSorter(ColumnViewerSorter.create(Annotated.class, "note")); //$NON-NLS-1$
        new StringEditingSupport(Annotated.class, "note").attachTo(this); //$NON-NLS-1$
    }
}

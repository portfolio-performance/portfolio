package name.abuchen.portfolio.ui.views.columns;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Named;
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
                Named n = Adaptor.adapt(Named.class, e);
                return n != null ? n.getNote() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getText(e);
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
            }
        });
        setSorter(ColumnViewerSorter.create(Named.class, "note")); //$NON-NLS-1$
        new StringEditingSupport(Named.class, "note").attachTo(this); //$NON-NLS-1$
    }
}

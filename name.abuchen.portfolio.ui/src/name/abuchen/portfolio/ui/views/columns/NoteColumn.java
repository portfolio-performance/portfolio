package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.util.TextUtil;

public class NoteColumn extends Column
{
    public NoteColumn()
    {
        this("note"); //$NON-NLS-1$
    }

    public NoteColumn(String id)
    {
        super(id, Messages.ColumnNote, SWT.LEFT, 22);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Annotated n = Adaptor.adapt(Annotated.class, e);
                if (n != null)
                    return n.getNote();

                Named n2 = Adaptor.adapt(Named.class, e);
                return n2 != null ? n2.getNote() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getText(e);
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getText(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(note);
            }

        });
        setSorter(ColumnViewerSorter.create(Annotated.class, "note")); //$NON-NLS-1$
        new StringEditingSupport(Annotated.class, "note").attachTo(this); //$NON-NLS-1$
    }
}

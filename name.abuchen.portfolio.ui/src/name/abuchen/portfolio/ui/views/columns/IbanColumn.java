package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import java.io.StringWriter;
import java.io.PrintWriter;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Peer;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.util.Iban;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

public class IbanColumn extends Column
{
    public static class IbanEditingSupport extends ColumnEditingSupport
    {
        @Override
        public boolean canEdit(Object element)
        {
            if (element instanceof Account)
            {
                Account s = Adaptor.adapt(Account.class, element);
                return s != null;
            }
            else if (element instanceof Peer)
            {
                Peer s = Adaptor.adapt(Peer.class, element);
                return s != null;
            }
            else
                throw new IllegalArgumentException();
        }

        @Override
        public Object getValue(Object element)
        {
            System.err.println(">>>> IbanColumn::getValue ");
            if (element instanceof Account)
            {
                Account s = Adaptor.adapt(Account.class, element);
                return s.getIban() != null ? s.getIban() : ""; //$NON-NLS-1$
            }
            else if (element instanceof Peer)
            {
                Peer s = Adaptor.adapt(Peer.class, element);
                return s.getIban() != null ? s.getIban() : ""; //$NON-NLS-1$
            }
            else
                throw new IllegalArgumentException();
        }

        @Override
        public void setValue(Object element, Object value)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            new Exception().printStackTrace(pw);
            String newValue = ((String) value).trim();

            String oldValue = "fjewfij23p"+ newValue;
            if (element instanceof Account)
                oldValue = Adaptor.adapt(Account.class, element).getIban();
            else if (element instanceof Peer)
                oldValue = Adaptor.adapt(Peer.class, element).getIban();                
            System.err.println(">>>> IbanColumn::setValue new: " + newValue + " | old: " + oldValue);// TODO: still needed for debug?
            if (!newValue.equals(oldValue))
            {
                if (element instanceof Account)
                    ((Account) element).setIban(newValue);
                else if (element instanceof Peer)
                    ((Peer) element).setIban(newValue);
                notify(element, newValue, oldValue);
            }
        }
    }

    public IbanColumn()
    {
        this("iban"); //$NON-NLS-1$
    }

    public IbanColumn(String id)
    {
        super(id, Messages.ColumnIBAN, SWT.LEFT, 160);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof Account)
                {
                    Account s = Adaptor.adapt(Account.class, e);
                    return s != null ? s.getIban() : null;
                }
                else if (e instanceof Peer)
                {
                    Peer s = Adaptor.adapt(Peer.class, e);
                    return s != null ? s.getIban() : null;                    
                }
                else
                    throw new IllegalArgumentException();
            }
            @Override
            public Color getForeground(Object e)
            {
                String iban = null;
                if (e instanceof Account)
                {
                    Account a = Adaptor.adapt(Account.class, e);
                    iban = a.getIban();
                }
                else if (e instanceof Peer)
                {
                    Peer p = Adaptor.adapt(Peer.class, e);
                    iban = p.getIban();
                }
                if (iban != null)
                {
                    if (iban.length() > 0 && Iban.isValid(iban))
                        return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
                    else
                        return Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);                    
                }
                else
                    return null;
            }
        });
        setSorter(ColumnViewerSorter.create(Account.class, "iban")); //$NON-NLS-1$
        new IbanEditingSupport().attachTo(this);
    }
}

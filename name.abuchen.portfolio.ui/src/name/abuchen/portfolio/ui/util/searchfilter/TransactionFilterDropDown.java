package name.abuchen.portfolio.ui.util.searchfilter;

import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import com.google.common.base.Strings;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;

public class TransactionFilterDropDown extends DropDown implements IMenuListener
{
    private TransactionFilterCriteria filterCriteria = TransactionFilterCriteria.NONE;
    private Consumer<TransactionFilterCriteria> notifyModelUpdated;

    public TransactionFilterDropDown(IPreferenceStore preferenceStore, String prefKey,
                    Consumer<TransactionFilterCriteria> notifyModelUpdated)
    {
        super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

        this.notifyModelUpdated = notifyModelUpdated;

        preferenceStore.setDefault(prefKey, TransactionFilterCriteria.NONE.name());
        TransactionFilterCriteria transactionFilter = TransactionFilterCriteria
                        .valueOf(preferenceStore.getString(prefKey));
        filterCriteria = transactionFilter;

        setMenuListener(this);

        updateIcon();

        addDisposeListener(e -> preferenceStore.setValue(prefKey, filterCriteria.name()));
    }

    private void updateIcon()
    {
        boolean hasActiveFilter = filterCriteria != TransactionFilterCriteria.NONE;
        setImage(hasActiveFilter ? Images.FILTER_ON : Images.FILTER_OFF);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new LabelOnly(Messages.TransactionFilter));
        for (TransactionFilterCriteria f : TransactionFilterCriteria.values())
            manager.add(addTypeFilter(f));
    }

    public TransactionFilterCriteria getFilterCriteria()
    {
        return filterCriteria;
    }

    public boolean hasActiveCriteria()
    {
        return filterCriteria != TransactionFilterCriteria.NONE;
    }

    private Action addTypeFilter(TransactionFilterCriteria filter)
    {
        String label = Strings.repeat(" ", filter.getLevel() * 2) + filter.getName(); //$NON-NLS-1$
        Action action = new Action(label, IAction.AS_CHECK_BOX)
        {
            @Override
            public void run()
            {
                boolean isChecked = filterCriteria == filter;

                // only one TransactionFilter can be selected at a time
                if (isChecked)
                    filterCriteria = TransactionFilterCriteria.NONE;
                else
                    filterCriteria = filter;

                updateIcon();
                notifyModelUpdated.accept(filterCriteria);
            }
        };
        action.setChecked(filterCriteria == filter);
        return action;
    }
}

package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;

public class ConfirmActionWithSelection extends Action
{
    @FunctionalInterface
    public interface Runnable
    {
        void run(IStructuredSelection selection, Action action);
    }

    private final String singleSelectionMessage;
    private final String multiSelectionMessage;
    private final IStructuredSelection selection;
    private final Runnable runnable;

    public ConfirmActionWithSelection(String title, String singleSelectionMessage, String multiSelectionMessage,
                    IStructuredSelection selection, Runnable runnable)
    {
        super(title);
        this.singleSelectionMessage = singleSelectionMessage;
        this.multiSelectionMessage = multiSelectionMessage;
        this.selection = selection;
        this.runnable = runnable;
    }

    @Override
    public void run()
    {
        if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), getText(), getMessageForSelection()))
            runnable.run(selection, this);
    }

    private String getMessageForSelection()
    {
        return selection.size() > 1 ? MessageFormat.format(multiSelectionMessage, selection.size())
                        : singleSelectionMessage;
    }

}

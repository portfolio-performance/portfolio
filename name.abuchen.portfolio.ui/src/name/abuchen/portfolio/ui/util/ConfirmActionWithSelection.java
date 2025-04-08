package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;

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

    public ConfirmActionWithSelection(String singleSelectionTitle, String multiSelectionTitle,
                    String singleSelectionMessage, String multiSelectionMessage,
                    IStructuredSelection selection, Runnable runnable)
    {
        super(selection.size() > 1 ? MessageFormat.format(multiSelectionTitle, selection.size()) : singleSelectionTitle);
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

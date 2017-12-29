package name.abuchen.portfolio.bootstrap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.ui.internal.workbench.PartServiceSaveHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public class CustomSaveHandler extends PartServiceSaveHandler
{
    private static final class PromptForSaveDialog extends MessageDialog
    {
        private PromptForSaveDialog(Shell parentShell, String dialogMessage)
        {
            super(parentShell, Messages.SaveHandlerTitle, null, dialogMessage, MessageDialog.INFORMATION, //
                            new String[] { Messages.LabelYes, Messages.LabelNo, Messages.LabelCancel }, 0);

            setShellStyle(getShellStyle() | SWT.SHEET);
        }
    }

    @Override
    public Save promptToSave(MPart dirtyPart)
    {
        String prompt = MessageFormat.format(Messages.SaveHandlerPrompt, dirtyPart.getLabel());

        MessageDialog dialog = new PromptForSaveDialog(Display.getDefault().getActiveShell(), prompt);

        switch (dialog.open())
        {
            case 0:
                return Save.YES;
            case 1:
                return Save.NO;
            case 2:
            default:
                return Save.CANCEL;
        }
    }

    @Override
    public Save[] promptToSave(Collection<MPart> dirtyParts)
    {
        if (dirtyParts.size() == 1)
            return new Save[] { promptToSave(dirtyParts.iterator().next()) };
        else
            return promptToSaveMultiple(dirtyParts);
    }

    private Save[] promptToSaveMultiple(Collection<MPart> dirtyParts)
    {
        FilePickerDialog dialog = new FilePickerDialog(Display.getDefault().getActiveShell());
        dialog.setElements(dirtyParts);

        int returnCode = dialog.open();

        Save[] answer = new Save[dirtyParts.size()];

        if (returnCode == Dialog.OK)
        {
            Arrays.fill(answer, Save.NO);
            if (dialog.getResult() != null)
            {
                List<MPart> parts = new ArrayList<MPart>(dirtyParts);
                for (Object toBeSaved : dialog.getResult())
                    answer[parts.indexOf(toBeSaved)] = Save.YES;
            }
        }
        else if (returnCode == FilePickerDialog.SAVE_ALL)
        {
            Arrays.fill(answer, Save.YES);
        }
        else if (returnCode == FilePickerDialog.SAVE_NONE)
        {
            Arrays.fill(answer, Save.NO);
        }
        else if (returnCode == Dialog.CANCEL)
        {
            Arrays.fill(answer, Save.CANCEL);
        }

        return answer;
    }

}

package name.abuchen.portfolio.bootstrap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.ui.internal.workbench.PartServiceSaveHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.bootstrap.FilePickerDialog.FileInfo;

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
        // need to map dirtyParts to editor inputs, because only editor inputs
        // are saved. For other parts (the second or third open window) just
        // return no.

        Collection<FileInfo> editorInputs = collectFileInfos(dirtyParts);

        if (editorInputs.size() == 1)
            return new Save[] { promptToSave(editorInputs.iterator().next().getParts().get(0)) };
        else
            return promptToSaveMultiple(dirtyParts, editorInputs);
    }

    private Save[] promptToSaveMultiple(Collection<MPart> dirtyParts, Collection<FileInfo> editorInputs)
    {
        FilePickerDialog dialog = new FilePickerDialog(Display.getDefault().getActiveShell());
        dialog.setElements(editorInputs);

        int returnCode = dialog.open();

        Save[] answer = new Save[dirtyParts.size()];
        Arrays.fill(answer, Save.NO);

        if (returnCode == Dialog.OK)
        {
            if (dialog.getResult() != null)
            {
                List<MPart> parts = new ArrayList<>(dirtyParts);

                for (Object toBeSaved : dialog.getResult())
                    answer[parts.indexOf(((FileInfo) toBeSaved).getParts().get(0))] = Save.YES;
            }
        }
        else if (returnCode == FilePickerDialog.SAVE_ALL)
        {
            List<MPart> parts = new ArrayList<>(dirtyParts);
            for (FileInfo toBeSaved : editorInputs)
                answer[parts.indexOf(toBeSaved.getParts().get(0))] = Save.YES;
        }
        else if (returnCode == FilePickerDialog.SAVE_NONE)
        {
            // nothing to do
        }
        else if (returnCode == Dialog.CANCEL)
        {
            Arrays.fill(answer, Save.CANCEL);
        }

        return answer;
    }

    private Collection<FileInfo> collectFileInfos(Collection<MPart> parts)
    {
        return parts.stream().filter(p -> ModelConstants.PORTFOLIO_PART.equals(p.getElementId()))
                        .collect(Collectors.toMap( //
                                        p -> p.getTransientData().get(ModelConstants.EDITOR_INPUT), //
                                        p -> {
                                            FileInfo info = new FileInfo(p.getLabel(), p.getTooltip());
                                            info.addPart(p);
                                            return info;
                                        }, (r, l) -> {
                                            r.addParts(l.getParts());
                                            return r;
                                        }))
                        .values();
    }
}

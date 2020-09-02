package name.abuchen.portfolio.ui.dnd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.handlers.ImportPDFHandler;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.wizards.datatransfer.CSVImportWizard;

public class ImportFromFileDropAdapter extends AbstractDropAdapter
{
    private static final Pattern PDF_FILE = Pattern.compile(".*\\.pdf$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern CSV_FILE = Pattern.compile(".*\\.csv$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final PortfolioPart part;

    private ImportFromFileDropAdapter(Transfer transfer, Control control, PortfolioPart part)
    {
        super(transfer, control);
        this.part = part;
    }

    public static void attach(Control control, PortfolioPart part)
    {
        new ImportFromFileDropAdapter(FileTransfer.getInstance(), control, part);
    }

    @Override
    public void doDrop(DropTargetEvent event)
    {
        String[] files = (String[]) event.data;
        if (files.length == 0)
        {
            event.detail = DND.DROP_NONE;
            return;
        }

        if (PDF_FILE.matcher(files[0]).matches())
        {
            openPDFImport(event, files);
        }
        else if (CSV_FILE.matcher(files[0]).matches())
        {
            openCSVImport(event, files);
        }
        else
        {
            event.detail = DND.DROP_NONE;
        }
    }

    private void openPDFImport(DropTargetEvent event, String[] fileNames)
    {
        List<File> files = new ArrayList<>();
        for (String f : fileNames)
            files.add(new File(f));

        DropTarget source = (DropTarget) event.getSource();
        Display display = source.getDisplay();
        display.asyncExec(() -> ImportPDFHandler.runImportWithFiles(part, ActiveShell.get(), part.getClient(), null,
                        null, files));
    }

    private void openCSVImport(DropTargetEvent event, String[] fileNames)
    {
        DropTarget source = (DropTarget) event.getSource();
        Display display = source.getDisplay();
        display.asyncExec(() -> {
            CSVImportWizard wizard = new CSVImportWizard(part.getClient(), part.getPreferenceStore(),
                            new File(fileNames[0]));
            part.inject(wizard);
            Dialog wizwardDialog = new WizardDialog(ActiveShell.get(), wizard);
            wizwardDialog.open();
        });
    }
}

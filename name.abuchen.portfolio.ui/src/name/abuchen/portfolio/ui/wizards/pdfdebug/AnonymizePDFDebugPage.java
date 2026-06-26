package name.abuchen.portfolio.ui.wizards.pdfdebug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

@SuppressWarnings("nls")
public class AnonymizePDFDebugPage extends WizardPage
{
    private final SelectPDFDebugSourcePage sourcePage;

    private Text textWidget;

    public AnonymizePDFDebugPage(SelectPDFDebugSourcePage sourcePage)
    {
        super("anonymizePDFDebug");
        this.sourcePage = sourcePage;
        setTitle(Messages.PDFImportDebugAnonymizeTitle);
        setDescription(Messages.PDFImportDebugAnonymizeDescription);
        setPageComplete(true);
    }

    @Override
    public void createControl(Composite parent)
    {
        var container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        // instructions on how to anonymize and where to share the text;
        // collapsed by default as they take up a lot of space. The headline is
        // used as the (always visible) section heading.
        var instructions = new ExpandableComposite(container, SWT.NONE,
                        ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
        instructions.setText(Messages.PDFImportDebugInformationHeadline);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(instructions);

        var info = new StyledLabel(instructions, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        info.setText(Messages.PDFImportDebugInformation);
        instructions.setClient(info);
        instructions.addExpansionListener(new ExpansionAdapter()
        {
            @Override
            public void expansionStateChanged(ExpansionEvent e)
            {
                container.layout(true, true);
            }
        });

        textWidget = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        textWidget.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.CODE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(textWidget);
        textWidget.addMouseListener(new PDFAnonymizeMouseListener(textWidget));

        var buttonRow = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonRow);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonRow);

        var copyButton = new Button(buttonRow, SWT.PUSH);
        copyButton.setText(Messages.LabelCopyToClipboard);
        copyButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (textWidget.isDisposed())
                return;
            var cb = new Clipboard(Display.getCurrent());
            cb.setContents(new Object[] { textWidget.getText() }, new Transfer[] { TextTransfer.getInstance() });
            cb.dispose();
        }));

        var saveButton = new Button(buttonRow, SWT.PUSH);
        saveButton.setText(Messages.LabelSaveInFile);
        saveButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (textWidget.isDisposed())
                return;
            var dialog = new FileDialog(getShell(), SWT.SAVE);
            dialog.setFileName(Messages.LabelUnnamedFile + ".txt");
            dialog.setOverwrite(true);
            var path = dialog.open();
            if (path == null)
                return;
            try (var writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8))
            {
                writer.write(textWidget.getText());
                writer.flush();
            }
            catch (IOException x)
            {
                PortfolioPlugin.log(x);
                MessageDialog.openError(getShell(), Messages.LabelError, x.getMessage());
            }
        }));
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            var entry = sourcePage.getSelectedEntry();
            var file = sourcePage.getSelectedFile();

            // clear any previous content first so that a failed conversion can
            // never leave stale debug text of an earlier document on screen
            textWidget.setText("");

            String extractedText;
            String pdfBoxVersion;

            if (entry != null)
            {
                extractedText = entry.getExtractedText();
                pdfBoxVersion = entry.getPdfBoxVersion();
            }
            else if (file != null)
            {
                try
                {
                    var inputFile = new PDFInputFile(file);
                    inputFile.convertPDFtoText();
                    extractedText = inputFile.getText();
                    pdfBoxVersion = inputFile.getPDFBoxVersion();
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                    MessageDialog.openError(getShell(), Messages.LabelError, e.getMessage());

                    // conversion failed; do not strand the user on this page
                    // with an empty text area. Navigate back to the source
                    // selection page once the current transition has settled.
                    var previousPage = getPreviousPage();
                    if (previousPage != null)
                        getContainer().getShell().getDisplay().asyncExec(() -> {
                            if (!textWidget.isDisposed())
                                getContainer().showPage(previousPage);
                        });

                    super.setVisible(visible);
                    return;
                }
            }
            else
            {
                super.setVisible(visible);
                return;
            }

            textWidget.setText(PDFDebugText.build(pdfBoxVersion, extractedText));
        }

        super.setVisible(visible);

        if (visible)
            textWidget.setFocus();
    }
}

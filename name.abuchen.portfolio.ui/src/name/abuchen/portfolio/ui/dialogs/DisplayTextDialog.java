package name.abuchen.portfolio.ui.dialogs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class DisplayTextDialog extends Dialog
{
    private String dialogTitle;
    private String additionalText;
    private File source;
    private String text;
    private Text widget;
    private boolean createPDFDebug;

    public DisplayTextDialog(Shell parentShell, String text)
    {
        this(parentShell, null, text);
    }

    public DisplayTextDialog(Shell parentShell, File source, String text)
    {
        super(parentShell);
        this.source = source;
        this.text = text;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);

        Button button = createButton(parent, 9999, Messages.LabelCopyToClipboard, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (widget.isDisposed())
                return;

            Clipboard cb = new Clipboard(Display.getCurrent());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { widget.getText() }, new Transfer[] { textTransfer });
        }));

        button = createButton(parent, 9998, Messages.LabelSaveInFile, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (widget.isDisposed())
                return;

            FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);

            if (source != null)
            {
                dialog.setFilterPath(source.getAbsolutePath());
                dialog.setFileName(source.getName() + ".txt"); //$NON-NLS-1$
            }
            else
            {
                dialog.setFileName(Messages.LabelUnnamedFile + ".txt"); //$NON-NLS-1$
            }

            dialog.setOverwrite(true);

            String path = dialog.open();
            if (path == null)
                return;

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8))
            {
                writer.write(widget.getText());
                writer.flush();
            }
            catch (IOException x)
            {
                throw new IllegalArgumentException(x);
            }
        }));
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(800, 500).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        if (dialogTitle != null)
            getShell().setText(dialogTitle);

        if (additionalText != null)
        {
            List<StyleRange> styles = new ArrayList<>();
            addBoldFirstLine(additionalText, styles);
            additionalText = addMarkdownLikeHyperlinks(additionalText, styles);

            StyledText additionalTextBox = new StyledText(container, SWT.MULTI | SWT.NONE | SWT.WRAP | SWT.READ_ONLY);
            additionalTextBox.setText(additionalText);
            additionalTextBox.setStyleRanges(styles.toArray(new StyleRange[0]));

            additionalTextBox.addListener(SWT.MouseDown, e -> openBrowser(e, additionalTextBox));

            container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).align(SWT.FILL, SWT.TOP)
                            .applyTo(additionalTextBox);
            additionalTextBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
            additionalTextBox.setMargins(5, 5, 5, 5);
        }

        widget = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        widget.setText(text);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(widget);

        if (createPDFDebug)
            widget.addMouseListener(new PDFDebugMouseListener(widget));

        return container;
    }

    public void setDialogTitle(String dialogTitle)
    {
        this.dialogTitle = dialogTitle;
    }

    public void setAdditionalText(String additionalText)
    {
        this.additionalText = additionalText;
    }

    public void createPDFDebug(boolean createPDFDebug)
    {
        this.createPDFDebug = createPDFDebug;
    }

    private static class PDFDebugMouseListener extends MouseAdapter
    {
        private final Text widget;

        public PDFDebugMouseListener(Text widget)
        {
            this.widget = widget;
        }

        @Override
        public void mouseDoubleClick(MouseEvent e)
        {
            String selectedText = widget.getSelectionText();

            // Check if selectedText is not empty and does not contain forbidden
            // characters or currency codes
            if (!selectedText.isEmpty() && !containsForbiddenCharacters(selectedText)
                            && !CurrencyUnit.containsCurrencyCode(selectedText.trim()))
            {
                // Generate a new string of random characters to replace the
                // selected text
                StringBuilder replacementTextBuilder = new StringBuilder();
                for (int i = 0; i < selectedText.length(); i++)
                {
                    char c = selectedText.charAt(i);
                    if (Character.isLetter(c))
                    {
                        replacementTextBuilder.append(generateRandomLetter());
                    }
                    else if (Character.isDigit(c))
                    {
                        replacementTextBuilder.append(generateRandomNumber());
                    }
                    else
                    {
                        replacementTextBuilder.append(c);
                    }
                }
                String replacementText = replacementTextBuilder.toString();

                // Replace selectedText with replacementText
                int startIndex = widget.getSelection().x;
                widget.insert(replacementText);
                widget.setSelection(startIndex, startIndex + replacementText.length());
            }
        }

        // Check if the selected string contains any forbidden characters
        private static boolean containsForbiddenCharacters(String string)
        {
            return string.matches(".*[\\-\\.,':\\/].*") || string.matches(".*[A-Z]{2}[A-Z0-9]{9}[0-9].*"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Generates a random letter
        private static char generateRandomLetter()
        {
            Random random = new Random();
            boolean isUpperCase = random.nextBoolean();
            int offset = isUpperCase ? 'A' : 'a';
            return (char) (random.nextInt(26) + offset);
        }

        // Generates a random digit between 0 and 9
        private static char generateRandomNumber()
        {
            Random random = new Random();
            return (char) (random.nextInt(10) + '0');
        }
    }

    private String addMarkdownLikeHyperlinks(String additionalText, List<StyleRange> styles)
    {
        Pattern pattern = Pattern.compile("\\[(?<text>[^\\]]*)\\]\\((?<link>[^\\)]*)\\)"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher(additionalText);

        StringBuilder answer = new StringBuilder(additionalText.length());
        int pointer = 0;

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            answer.append(additionalText.substring(pointer, start));

            String text = matcher.group("text"); //$NON-NLS-1$
            String link = matcher.group("link"); //$NON-NLS-1$

            StyleRange styleRange = new StyleRange();
            styleRange.underline = true;
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underlineColor = Colors.theme().hyperlink();
            styleRange.foreground = Colors.theme().hyperlink();
            styleRange.data = link;
            styleRange.start = answer.length();
            styleRange.length = text.length();
            styles.add(styleRange);

            answer.append(text);

            pointer = end;
        }

        if (pointer < additionalText.length())
            answer.append(additionalText.substring(pointer));

        return answer.toString();
    }

    private void openBrowser(Event e, StyledText textBox)
    {
        int offset = textBox.getOffsetAtPoint(new Point(e.x, e.y));
        if (offset == -1)
            return;

        StyleRange style = textBox.getStyleRangeAtOffset(offset);
        if (style != null && style.data != null)
            DesktopAPI.browse(String.valueOf(style.data));
    }

    private void addBoldFirstLine(String additionalText, List<StyleRange> ranges)
    {
        StyleRange styleRange = new StyleRange();
        styleRange.fontStyle = SWT.BOLD;
        styleRange.start = 0;
        styleRange.length = additionalText.indexOf('\n');
        ranges.add(styleRange);
    }
}

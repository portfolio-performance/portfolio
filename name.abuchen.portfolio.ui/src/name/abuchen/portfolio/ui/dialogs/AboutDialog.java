package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class AboutDialog extends Dialog
{
    private StyledText text;

    @Inject
    public AboutDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        String aboutText = MessageFormat.format(Messages.AboutText,
                        PortfolioPlugin.getDefault().getBundle().getVersion().toString());

        Composite area = new Composite(parent, SWT.NONE);

        area.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getShell().setText(Messages.LabelAbout);

        Label imageLabel = new Label(area, SWT.NONE);
        imageLabel.setBackground(area.getBackground());
        imageLabel.setImage(Images.LOGO_128.image());

        List<StyleRange> styles = new ArrayList<StyleRange>();
        aboutText = addContributorHyperlinks(aboutText, styles);
        addBoldFirstLine(aboutText, styles);
        addHyperlinks(aboutText, styles);

        Collections.sort(styles, new Comparator<StyleRange>()
        {
            @Override
            public int compare(StyleRange o1, StyleRange o2)
            {
                return Integer.compare(o1.start, o2.start);
            }
        });

        text = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        text.setText(aboutText);
        text.setStyleRanges(styles.toArray(new StyleRange[0]));

        text.addListener(SWT.MouseDown, new Listener()
        {
            public void handleEvent(Event event)
            {
                openBrowser(event);
            }
        });

        // layout

        GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(10, 3).applyTo(area);
        GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.TOP).applyTo(imageLabel);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(text);

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        Button b = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        b.setFocus();
    }

    private String addContributorHyperlinks(String aboutText, List<StyleRange> styles)
    {
        Pattern pattern = Pattern.compile("\\[([\\w]*)\\]"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher(aboutText);

        StringBuilder answer = new StringBuilder(aboutText.length());
        int pointer = 0;
        int found = 0;

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            StyleRange styleRange = new StyleRange();
            styleRange.underline = true;
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underlineColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
            styleRange.data = "https://github.com/" + matcher.group(1); //$NON-NLS-1$
            styleRange.start = start - (2 * found);
            styleRange.length = end - start - 2;
            styles.add(styleRange);

            answer.append(aboutText.substring(pointer, start));
            answer.append(aboutText.substring(start + 1, end - 1));

            pointer = end;
            found++;
        }

        if (pointer < aboutText.length())
            answer.append(aboutText.substring(pointer));

        return answer.toString();
    }

    private void addBoldFirstLine(String aboutText, List<StyleRange> ranges)
    {
        StyleRange styleRange = new StyleRange();
        styleRange.fontStyle = SWT.BOLD;
        styleRange.start = 0;
        styleRange.length = aboutText.indexOf('\n');
        ranges.add(styleRange);
    }

    private void addHyperlinks(String aboutText, List<StyleRange> ranges)
    {
        Pattern pattern = Pattern.compile("https?://[^ \n]*"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher(aboutText);
        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            StyleRange styleRange = new StyleRange();
            styleRange.underline = true;
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underlineColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
            styleRange.data = matcher.group();
            styleRange.start = start;
            styleRange.length = end - start;
            ranges.add(styleRange);
        }
    }

    private void openBrowser(Event event)
    {
        try
        {
            int offset = text.getOffsetAtLocation(new Point(event.x, event.y));
            StyleRange style = text.getStyleRangeAtOffset(offset);
            if (style != null && style.data != null)
                DesktopAPI.browse(String.valueOf(style.data));
        }
        catch (IllegalArgumentException ignore)
        {
            // no character at position
        }
    }
}

package name.abuchen.portfolio.ui.update;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.update.NewVersion.ConditionalMessage;
import name.abuchen.portfolio.ui.update.NewVersion.Release;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

/* package */class UpdateMessageDialog extends MessageDialog
{
    private Button checkOnUpdate;
    private NewVersion newVersion;

    public UpdateMessageDialog(Shell parentShell, String title, String message, NewVersion newVersion)
    {
        super(parentShell, title, null, message, CONFIRM,
                        new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        this.newVersion = newVersion;
    }

    @Override
    protected Control createCustomArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        create32BitWarning(container);
        createHeader(container);
        createText(container);

        checkOnUpdate = new Button(container, SWT.CHECK);
        checkOnUpdate.setSelection(PortfolioPlugin.getDefault().getPreferenceStore()
                        .getBoolean(UIConstants.Preferences.AUTO_UPDATE));
        checkOnUpdate.setText(Messages.PrefCheckOnStartup);
        checkOnUpdate.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                PortfolioPlugin.getDefault().getPreferenceStore().setValue(UIConstants.Preferences.AUTO_UPDATE,
                                checkOnUpdate.getSelection());
            }
        });
        GridDataFactory.fillDefaults().grab(true, false);

        return container;
    }

    private void create32BitWarning(Composite container)
    {
        if (newVersion.get32BitWarning() == null)
            return;

        StyledLabel label = new StyledLabel(container, SWT.WRAP);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.END).applyTo(label);
        label.setText(newVersion.get32BitWarning());
        label.setBackground(Colors.WARNING);
    }

    private void createHeader(Composite container)
    {
        if (newVersion.getHeader() == null)
            return;

        try
        {
            StyledLabel label = new StyledLabel(container, SWT.WRAP);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.END).applyTo(label);
            label.setText(newVersion.getHeader());
        }
        catch (IllegalArgumentException ignore)
        {
            // display dialog even if test cannot be parsed
            PortfolioPlugin.log(ignore);
        }
    }

    private void createText(Composite container)
    {
        StyledText text = new StyledText(container,
                        SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        List<StyleRange> ranges = new ArrayList<>();

        StringBuilder buffer = new StringBuilder();
        if (newVersion.requiresNewJavaVersion())
        {
            StyleRange style = new StyleRange();
            style.start = buffer.length();
            style.length = Messages.MsgUpdateRequiresLatestJavaVersion.length();
            style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED);
            style.fontStyle = SWT.BOLD;
            ranges.add(style);

            buffer.append(Messages.MsgUpdateRequiresLatestJavaVersion);

        }

        appendReleases(buffer, ranges);

        text.setText(buffer.toString());
        text.setStyleRanges(ranges.toArray(new StyleRange[0]));

        GridDataFactory.fillDefaults().hint(convertHorizontalDLUsToPixels(400), convertVerticalDLUsToPixels(100))
                        .applyTo(text);
    }

    private void appendReleases(StringBuilder buffer, List<StyleRange> styles)
    {
        Version currentVersion = FrameworkUtil.getBundle(this.getClass()).getVersion();

        for (Release release : newVersion.getReleases())
        {
            if (release.getVersion().compareTo(currentVersion) <= 0)
                continue;

            if (buffer.length() > 0)
                buffer.append("\n\n"); //$NON-NLS-1$
            String heading = MessageFormat.format(Messages.MsgUpdateNewInVersionX, release.getVersion().toString());

            StyleRange style = new StyleRange();
            style.start = buffer.length();
            style.length = heading.length();
            style.fontStyle = SWT.BOLD;
            styles.add(style);
            buffer.append(heading);
            buffer.append("\n\n"); //$NON-NLS-1$

            appendMessages(buffer, styles, release);

            for (String line : release.getLines())
                buffer.append(line).append("\n"); //$NON-NLS-1$
        }
    }

    private void appendMessages(StringBuilder buffer, List<StyleRange> styles, Release release)
    {
        for (ConditionalMessage msg : release.getMessages())
        {
            if (!msg.isApplicable())
                continue;

            StyleRange style = new StyleRange();
            style.start = buffer.length();
            style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED);
            style.fontStyle = SWT.BOLD;

            for (String line : msg.getLines())
                buffer.append(line).append("\n"); //$NON-NLS-1$
            style.length = buffer.length() - style.start;
            styles.add(style);

            buffer.append("\n\n"); //$NON-NLS-1$
        }
    }
}

package name.abuchen.portfolio.ui.dialogs;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.util.BuildInfo;

public class AboutDialog extends Dialog
{
    @Inject
    public AboutDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.WHITE);
        GridDataFactory.fillDefaults().grab(true, true).hint(700, 500).applyTo(container);
        GridLayoutFactory.fillDefaults().spacing(5, 5).margins(5, 5).applyTo(container);

        Control aboutText = createAboutText(container);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(aboutText);

        CTabFolder folder = new CTabFolder(container, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(folder);

        makeSoftwareTab(folder);
        makeCodeContributorsTab(folder);
        makeTranslatorsTab(folder);
        makeHelpWritersTab(folder);
        makeInstallationDetailsTab(folder);

        folder.setSelection(0);

        return container;
    }

    @Override
    protected void setShellStyle(int newShellStyle)
    {
        super.setShellStyle(newShellStyle | SWT.RESIZE);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    private Control createAboutText(Composite parent)
    {
        String aboutText = MessageFormat.format(Messages.AboutText,
                        PortfolioPlugin.getDefault().getBundle().getVersion().toString(), //
                        DateTimeFormatter.ofPattern("MMMM yyyy").format(BuildInfo.INSTANCE.getBuildTime()), //$NON-NLS-1$
                        System.getProperty("osgi.os"), //$NON-NLS-1$
                        System.getProperty("osgi.arch"), //$NON-NLS-1$
                        System.getProperty("java.vm.version"), //$NON-NLS-1$
                        System.getProperty("java.vm.vendor")); //$NON-NLS-1$

        Composite area = new Composite(parent, SWT.NONE);

        area.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getShell().setText(Messages.LabelAbout);

        Label imageLabel = new Label(area, SWT.NONE);
        imageLabel.setBackground(area.getBackground());
        imageLabel.setImage(Images.LOGO_128.image());

        List<StyleRange> styles = new ArrayList<>();
        aboutText = addMarkdownLikeHyperlinks(aboutText, styles);
        addBoldFirstLine(aboutText, styles);

        Collections.sort(styles, (o1, o2) -> Integer.compare(o1.start, o2.start));

        StyledText aboutTextBox = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        aboutTextBox.setText(aboutText);
        aboutTextBox.setStyleRanges(styles.toArray(new StyleRange[0]));

        aboutTextBox.addListener(SWT.MouseDown, e -> openBrowser(e, aboutTextBox));

        // layout

        GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(10, 10).applyTo(area);
        GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.TOP).applyTo(imageLabel);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(aboutTextBox);

        return area;
    }

    private void makeSoftwareTab(CTabFolder folder)
    {
        String componentsText = read("about.software.txt"); //$NON-NLS-1$
        constructTab(folder, "Software", componentsText); //$NON-NLS-1$
    }

    private void makeCodeContributorsTab(CTabFolder folder)
    {
        String developers = generateDeveloperListText(read("about.contributors.txt")); //$NON-NLS-1$
        String text = MessageFormat.format(Messages.AboutTextDeveloped, developers);

        constructTab(folder, "Code Contributors", text); //$NON-NLS-1$
    }

    private void makeTranslatorsTab(CTabFolder folder)
    {
        String translatorsText = read("about.translations.txt"); //$NON-NLS-1$
        constructTab(folder, "Translators", translatorsText); //$NON-NLS-1$
    }

    private void makeHelpWritersTab(CTabFolder folder)
    {
        String helpWritersText = read("about.writers.txt"); //$NON-NLS-1$
        constructTab(folder, "Writers", helpWritersText); //$NON-NLS-1$
    }

    private void constructTab(CTabFolder folder, String label, String text)
    {
        List<StyleRange> styles = new ArrayList<>();
        String body = addMarkdownLikeHyperlinks(text, styles);

        StyledText textBox = new StyledText(folder, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        textBox.setMargins(5, 5, 5, 5);
        textBox.setText(body);
        textBox.setStyleRanges(styles.toArray(new StyleRange[0]));

        textBox.addListener(SWT.MouseDown, e -> openBrowser(e, textBox));

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(label);
        item.setControl(textBox);
    }

    private void makeInstallationDetailsTab(CTabFolder folder)
    {
        Text installationDetails = new Text(folder, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        installationDetails.setText(""); //$NON-NLS-1$

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.LabelInstallationDetails);
        item.setControl(installationDetails);

        folder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (item == e.item && installationDetails.getText().isEmpty())
            {
                installationDetails.setText("Loading..."); //$NON-NLS-1$
                new Job(Messages.LabelInfo)
                {
                    @Override
                    protected IStatus run(IProgressMonitor monitor)
                    {
                        String infoText = buildInfoText();
                        Display.getDefault().asyncExec(() -> installationDetails.setText(infoText));
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }));
    }

    private String addMarkdownLikeHyperlinks(String aboutText, List<StyleRange> styles)
    {
        Pattern pattern = Pattern.compile("\\[(?<text>[^\\]]*)\\]\\((?<link>[^\\)]*)\\)"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher(aboutText);

        StringBuilder answer = new StringBuilder(aboutText.length());
        int pointer = 0;

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            answer.append(aboutText.substring(pointer, start));

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

    private String generateDeveloperListText(String developers)
    {
        StringBuilder text = new StringBuilder();

        developers.lines().forEach(line -> {
            if (!text.isEmpty())
                text.append(", "); //$NON-NLS-1$
            text.append("[").append(line).append("](https://github.com/").append(line).append(")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        });

        return text.toString();
    }

    private void openBrowser(Event event, StyledText textBox)
    {
        int offset = textBox.getOffsetAtPoint(new Point(event.x, event.y));
        if (offset == -1)
            return;

        StyleRange style = textBox.getStyleRangeAtOffset(offset);
        if (style != null && style.data != null)
            DesktopAPI.browse(String.valueOf(style.data));
    }

    @SuppressWarnings("nls")
    private String buildInfoText()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("Generated at " + LocalDateTime.now());
        builder.append("\n\nSystem Properties:\n\n");

        System.getProperties().entrySet().stream()
                        .sorted((r, l) -> r.getKey().toString().compareTo(l.getKey().toString()))
                        .forEach(e -> builder.append(e.getKey() + ": " + e.getValue() + "\n"));

        builder.append("\n\nOSGi Bundles:\n\n");

        Bundle[] bundles = PortfolioPlugin.getDefault().getBundle().getBundleContext().getBundles();
        Arrays.sort(bundles, (r, l) -> r.getSymbolicName().compareTo(l.getSymbolicName()));

        for (int ii = 0; ii < bundles.length; ii++)
        {
            Bundle b = bundles[ii];

            builder.append(b.getSymbolicName() + " (" + b.getVersion().toString() + ")");

            addSignerInfo(builder, b);

            if (!b.getSignerCertificates(Bundle.SIGNERS_TRUSTED).isEmpty())
                builder.append(" [trusted]");

            builder.append("\n");
        }

        return builder.toString();
    }

    @SuppressWarnings("nls")
    private void addSignerInfo(StringBuilder builder, Bundle b)
    {
        Map<X509Certificate, List<X509Certificate>> certificates = b.getSignerCertificates(Bundle.SIGNERS_ALL);
        if (certificates.isEmpty())
            return;

        builder.append(" [signed by ");

        boolean isFirstCertificate = true;

        for (X509Certificate cert : certificates.keySet())
        {
            try
            {
                LdapName ldapDN = new LdapName(cert.getSubjectX500Principal().getName());
                for (Rdn rdn : ldapDN.getRdns())
                {
                    if ("CN".equals(rdn.getType()))
                    {
                        if (!isFirstCertificate)
                            builder.append(", ");
                        builder.append(rdn.getValue());
                        isFirstCertificate = false;
                    }
                }
            }
            catch (InvalidNameException ignore)
            {
                // ignore
            }
        }

        builder.append("]");
    }

    private String read(String name)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(name), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
        }
    }

}

package name.abuchen.portfolio.ui.dialogs;

import static name.abuchen.portfolio.ui.util.DialogTextUtils.addBoldFirstLine;
import static name.abuchen.portfolio.ui.util.DialogTextUtils.addMarkdownLikeHyperlinks;

import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
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
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.util.BuildInfo;

public class AboutDialog extends Dialog
{
    private static final int DETAILS_BTN_ID = 4711;

    private Composite container;
    private StackLayout layout;

    private Text infoTextBox;

    @Inject
    public AboutDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        layout = new StackLayout();
        container.setLayout(layout);

        layout.topControl = createAboutText(container);
        createInfoArea(container);

        return container;
    }

    private Control createAboutText(Composite parent)
    {
        String version = PortfolioPlugin.getDefault().getBundle().getVersion().toString();
        String buildTime = DateTimeFormatter.ofPattern("MMM yyyy").format(BuildInfo.INSTANCE.getBuildTime()); //$NON-NLS-1$
        String os = System.getProperty("osgi.os"); //$NON-NLS-1$
        String arch = System.getProperty("osgi.arch"); //$NON-NLS-1$
        String jvmVersion = System.getProperty("java.vm.version"); //$NON-NLS-1$
        String jvmVendor = System.getProperty("java.vm.vendor"); //$NON-NLS-1$
        String aboutText = MessageFormat.format(Messages.AboutText, version, buildTime, os, arch, jvmVersion, jvmVendor);

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

        String contributionsText = generateDeveloperListText(Messages.AboutTextDevelopers);
        styles = new ArrayList<>();
        contributionsText = addMarkdownLikeHyperlinks(contributionsText, styles);
        addBoldFirstLine(contributionsText, styles);
        Collections.sort(styles, (o1, o2) -> Integer.compare(o1.start, o2.start));

        StyledText contributionsBox = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        contributionsBox.setText(contributionsText);
        contributionsBox.setStyleRanges(styles.toArray(new StyleRange[0]));
        contributionsBox.addListener(SWT.MouseDown, e -> openBrowser(e, contributionsBox));

        styles = new ArrayList<>();
        String translationDevelopersText = addMarkdownLikeHyperlinks(Messages.AboutTextTranslationDevelopers, styles);
        addBoldFirstLine(translationDevelopersText, styles);
        Collections.sort(styles, (o1, o2) -> Integer.compare(o1.start, o2.start));

        StyledText translationDevelopersBox = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        translationDevelopersBox.setText(translationDevelopersText);
        translationDevelopersBox.setStyleRanges(styles.toArray(new StyleRange[0]));
        translationDevelopersBox.addListener(SWT.MouseDown, e -> openBrowser(e, translationDevelopersBox));

        styles = new ArrayList<>();
        String otherSoftwareText = addMarkdownLikeHyperlinks(Messages.AboutTextOtherSoftware, styles);

        StyledText otherSoftwareBox = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        otherSoftwareBox.setText(otherSoftwareText);
        otherSoftwareBox.setStyleRanges(styles.toArray(new StyleRange[0]));
        otherSoftwareBox.addListener(SWT.MouseDown, e -> openBrowser(e, otherSoftwareBox));

        // layout
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(area);
        GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.TOP).applyTo(imageLabel);

        // Configurations for the aboutTextBox
        GridDataFactory.fillDefaults().grab(true, true).applyTo(aboutTextBox);

        // Configurations for the contributionsBox
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(contributionsBox);

        // Configurations for the translationDevelopersBox
        GridDataFactory.fillDefaults().grab(true, false).applyTo(translationDevelopersBox);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(otherSoftwareBox);
        Composite bottomArea = new Composite(area, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 5).applyTo(bottomArea);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(bottomArea);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(translationDevelopersBox);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(otherSoftwareBox);
        bottomArea.layout();

        return area;
    }

    private void createInfoArea(Composite container)
    {
        Composite area = new Composite(container, SWT.NONE);
        area.setLayout(new FillLayout());

        infoTextBox = new Text(area, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        Button b = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        b.setFocus();

        createButton(parent, DETAILS_BTN_ID, Messages.LabelInstallationDetails, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId != DETAILS_BTN_ID)
        {
            super.buttonPressed(buttonId);
            return;
        }

        boolean showsAbout = layout.topControl == container.getChildren()[0];

        if (showsAbout)
        {
            infoTextBox.setText(buildInfoText());
            layout.topControl = container.getChildren()[1];
            container.layout();
            getButton(DETAILS_BTN_ID).setText(Messages.LabelInfo);
        }
        else
        {
            layout.topControl = container.getChildren()[0];
            container.layout();
            getButton(DETAILS_BTN_ID).setText(Messages.LabelInstallationDetails);
        }
    }

    private String generateDeveloperListText(String developers)
    {
        List<String> developerList = Arrays.asList(developers.split(",\\s*")); //$NON-NLS-1$
        StringBuilder builder = new StringBuilder(Messages.AboutTextDeveloped).append("\n  "); //$NON-NLS-1$
        int size = developerList.size();
        int lineLength = 0;
        for (int i = 0; i < size; i++)
        {
            String dev = developerList.get(i);
            String link = "(https://github.com/" + dev + ")";  //$NON-NLS-1$//$NON-NLS-2$

            int length = dev.length() + (i == size - 1 ? 1 : 2);

            if (lineLength + length > 130)
            {
                builder.append("\n  "); //$NON-NLS-1$
                lineLength = 2;
            }
            if (i == 0)
            {
                builder.append("[").append(dev).append("]").append(link).append(", "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                lineLength = dev.length() + 2;
            }
            else if (i == size - 1)
            {
                builder.append(" and [").append(dev).append("]").append(link).append(".\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                lineLength = 0;
            }
            else
            {
                if (i + 1 == size - 1)
                {
                    builder.append("[").append(dev).append("]").append(link); //$NON-NLS-1$ //$NON-NLS-2$
                }
                else
                {
                    builder.append("[").append(dev).append("]").append(link).append(", "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    lineLength += 2;
                }
            }
            lineLength += dev.length();
        }
        builder.append("  Many thanks for your support."); //$NON-NLS-1$

        return builder.toString();
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

    private String buildInfoText()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("Generated at " + LocalDateTime.now()); //$NON-NLS-1$
        builder.append("\n\nSystem Properties:\n\n"); //$NON-NLS-1$

        System.getProperties().entrySet().stream()
                        .sorted((r, l) -> r.getKey().toString().compareTo(l.getKey().toString()))
                        .forEach(e -> builder.append(e.getKey() + ": " + e.getValue() + "\n")); //$NON-NLS-1$ //$NON-NLS-2$

        builder.append("\n\nOSGi Bundles:\n\n"); //$NON-NLS-1$

        Bundle[] bundles = PortfolioPlugin.getDefault().getBundle().getBundleContext().getBundles();
        Arrays.sort(bundles, (r, l) -> r.getSymbolicName().compareTo(l.getSymbolicName()));

        for (int ii = 0; ii < bundles.length; ii++)
        {
            Bundle b = bundles[ii];

            builder.append(b.getSymbolicName() + " (" + b.getVersion().toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

            addSignerInfo(builder, b);

            if (!b.getSignerCertificates(Bundle.SIGNERS_TRUSTED).isEmpty())
                builder.append(" [trusted]"); //$NON-NLS-1$

            builder.append("\n"); //$NON-NLS-1$
        }

        return builder.toString();
    }

    private void addSignerInfo(StringBuilder builder, Bundle b)
    {
        Map<X509Certificate, List<X509Certificate>> certificates = b.getSignerCertificates(Bundle.SIGNERS_ALL);
        if (certificates.isEmpty())
            return;

        builder.append(" [signed by "); //$NON-NLS-1$

        boolean isFirstCertificate = true;

        for (X509Certificate cert : certificates.keySet())
        {
            try
            {
                LdapName ldapDN = new LdapName(cert.getSubjectX500Principal().getName());
                for (Rdn rdn : ldapDN.getRdns())
                {
                    if ("CN".equals(rdn.getType())) //$NON-NLS-1$
                    {
                        if (!isFirstCertificate)
                            builder.append(", "); //$NON-NLS-1$
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

        builder.append("]"); //$NON-NLS-1$
    }

}

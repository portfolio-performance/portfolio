package name.abuchen.portfolio.ui.dialogs;

import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class AboutDialog extends Dialog
{
    private static final int DETAILS_BTN_ID = 4711;

    private Composite container;
    private StackLayout layout;

    private StyledText aboutTextBox;
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
        String aboutText = MessageFormat.format(Messages.AboutText,
                        PortfolioPlugin.getDefault().getBundle().getVersion().toString(), //
                        System.getProperty("osgi.os"), //$NON-NLS-1$
                        System.getProperty("osgi.arch")); //$NON-NLS-1$

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

        aboutTextBox = new StyledText(area, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        aboutTextBox.setText(aboutText);
        aboutTextBox.setStyleRanges(styles.toArray(new StyleRange[0]));

        aboutTextBox.addListener(SWT.MouseDown, this::openBrowser);

        // layout

        GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(10, 3).applyTo(area);
        GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.TOP).applyTo(imageLabel);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(aboutTextBox);

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
            styleRange.underlineColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
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

    private void openBrowser(Event event)
    {
        int offset = aboutTextBox.getOffsetAtPoint(new Point(event.x, event.y));
        if (offset == -1)
            return;

        StyleRange style = aboutTextBox.getStyleRangeAtOffset(offset);
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
                LdapName ldapDN = new LdapName(cert.getSubjectDN().getName());
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

}

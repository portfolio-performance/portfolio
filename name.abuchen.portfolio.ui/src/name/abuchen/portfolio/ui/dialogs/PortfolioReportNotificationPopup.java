package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;

public class PortfolioReportNotificationPopup extends AbstractNotificationPopup
{
    private final Client client;
    private final List<Security> portfolioReportSecurities;
    private final IEclipseContext context;

    public PortfolioReportNotificationPopup(Shell parentShell, Client client, List<Security> portfolioReportSecurities,
                    IEclipseContext context)
    {
        super(parentShell.getDisplay());
        setParentShell(parentShell);

        this.client = client;
        this.portfolioReportSecurities = portfolioReportSecurities;
        this.context = context;
    }

    @Override
    protected String getPopupShellTitle()
    {
        return Messages.CmdMigratePortfolioReport;
    }

    @Override
    protected void createContentArea(Composite parent)
    {
        var messageComposite = new Composite(parent, SWT.NONE);
        messageComposite.setBackground(Colors.WHITE);
        messageComposite.setLayout(new GridLayout(1, false));
        messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        var messageLabel = new Label(messageComposite, SWT.WRAP);
        messageLabel.setText(MessageFormat.format(Messages.PortfolioReportNotificationMessage,
                        portfolioReportSecurities.size()));
        GridDataFactory.fillDefaults().grab(true, false).hint(300, SWT.DEFAULT).applyTo(messageLabel);

        var actionLink = new Link(messageComposite, SWT.NONE);
        actionLink.setBackground(Colors.WHITE);
        actionLink.setText("<a>" + Messages.CmdMigratePortfolioReport + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        actionLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            close();
            openFindQuoteProviderDialog();
        }));

        var infoLink = new Link(messageComposite, SWT.NONE);
        infoLink.setBackground(Colors.WHITE);
        infoLink.setText("<a>" + Messages.LabelInfo + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        infoLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            DesktopAPI.browse(Messages.SiteInfoPortfolioReportMigration);
            close();
        }));

    }

    private void openFindQuoteProviderDialog()
    {
        var dialog = new FindQuoteProviderDialog(getShell(), client, portfolioReportSecurities);
        ContextInjectionFactory.inject(dialog, context);
        dialog.open();
    }
}

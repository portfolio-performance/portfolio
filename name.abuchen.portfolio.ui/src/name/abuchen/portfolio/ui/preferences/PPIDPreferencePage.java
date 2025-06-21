package name.abuchen.portfolio.ui.preferences;

import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.oauth.AccessToken;
import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.OAuthHelper;

public class PPIDPreferencePage extends PreferencePage
{
    private static final String EMPTY_USER_TEXT = "-"; //$NON-NLS-1$

    private final OAuthClient oauthClient = OAuthClient.INSTANCE;

    private Label user;
    private Button action;

    private final Runnable updateListener = () -> Display.getDefault().asyncExec(this::triggerUpdate);

    public PPIDPreferencePage()
    {
        setTitle(Factory.getQuoteFeed(PortfolioPerformanceFeed.class).getName());

        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent)
    {
        this.oauthClient.addStatusListener(updateListener);
        parent.addDisposeListener(event -> oauthClient.removeStatusListener(updateListener));

        var area = new Composite(parent, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(2).spacing(5, 10).applyTo(area);

        new DescriptionFieldEditor(Messages.PrefDescriptionPortfolioPerformanceID, area);
        
        var label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelUser);

        user = new Label(area, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(user);
        user.setText(EMPTY_USER_TEXT);

        action = new Button(area, SWT.NONE);
        GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).span(2, 1).applyTo(action);
        action.setEnabled(false);
        action.setText(Messages.CmdLogin);
        action.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event)
            {
                try
                {
                    if (oauthClient.isAuthenticated())
                    {
                        OAuthHelper.run(() -> {
                            oauthClient.signOut();
                            return null;
                        }, (var o) -> triggerUpdate());
                    }
                    else
                    {
                        action.setEnabled(false);
                        oauthClient.signIn(DesktopAPI::browse);
                    }
                }
                catch (AuthenticationException e)
                {
                    PortfolioPlugin.log(e);
                    MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
                }
            }
        });

        triggerUpdate();

        return area;
    }

    private void triggerUpdate()
    {
        if (user.isDisposed())
            return;

        var isLoading = oauthClient.isAuthenticationOngoing();
        var isAuthenticated = oauthClient.isAuthenticated();

        if (!isLoading && isAuthenticated)
        {
            OAuthHelper.run(oauthClient::getAPIAccessToken, this::updateUserAndPlan);
        }
        else
        {
            user.setText(EMPTY_USER_TEXT);
        }

        action.setEnabled(!isLoading);
        action.setText(isAuthenticated ? Messages.CmdLogout : Messages.CmdLogin);
    }

    private void updateUserAndPlan(Optional<AccessToken> accessToken)
    {
        if (accessToken.isPresent())
        {
            var claims = accessToken.get().getClaims();
            user.setText(claims.getEmail());
        }
        else
        {
            user.setText(EMPTY_USER_TEXT);
        }
    }
}

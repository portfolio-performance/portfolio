package name.abuchen.portfolio.ui.preferences;

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
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
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

public class PPIDPreferencePage extends PreferencePage
{
    private static final String EMPTY_USER_TEXT = "-"; //$NON-NLS-1$
    private final OAuthClient oauthClient = OAuthClient.INSTANCE;
    private Label user;
    private Button action;
    private final Display display = Display.getDefault();
    private final Runnable updateListener = () -> display.asyncExec(this::triggerUpdate);

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

        var description = new StyledLabel(area, SWT.WRAP);
        GridDataFactory.swtDefaults().span(2, 1).hint(400, SWT.DEFAULT).applyTo(description);
        description.setText(Messages.PrefDescriptionPortfolioPerformanceID);

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
                        run(() -> {
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
                    MessageDialog.openError(display.getActiveShell(), Messages.LabelError, e.getMessage());
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
            run(oauthClient::getAPIAccessToken, this::updateUserAndPlan);
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
            user.setText(claims.getSub() + " (" + claims.getEmail() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            user.setText(EMPTY_USER_TEXT);
        }
    }

    @FunctionalInterface
    public interface AccessTokenRunnable<T>
    {
        T get() throws AuthenticationException;
    }

    private <T> void run(AccessTokenRunnable<T> supplier, Consumer<T> consumer)
    {
        Job.createSystem("Asynchronously retrieve token", monitor -> { //$NON-NLS-1$
            try
            {
                var result = supplier.get();
                display.asyncExec(() -> consumer.accept(result));
            }
            catch (AuthenticationException e)
            {
                display.asyncExec(() -> MessageDialog.openError(display.getActiveShell(), Messages.LabelError,
                                e.getMessage()));
            }
        }).schedule();
    }
}

package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper.Model;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class IBFlexConfigurationDialog extends AbstractDialog
{
    public static class IBFlexModel extends Model
    {
        private static final String PROPERTY_TOKEN = "ibflex-token"; //$NON-NLS-1$
        private static final String PROPERTY_QUERY_ID = "ibflex-query-id"; //$NON-NLS-1$

        private String token;
        private String queryId;

        public IBFlexModel(Client client)
        {
            super(client);
            String storedToken = client.getProperty(PROPERTY_TOKEN);
            String storedQueryId = client.getProperty(PROPERTY_QUERY_ID);
            this.token = storedToken != null ? storedToken : "";
            this.queryId = storedQueryId != null ? storedQueryId : "";
        }

        public String getToken()
        {
            return token;
        }

        public void setToken(String token)
        {
            firePropertyChange("token", this.token, this.token = token); //$NON-NLS-1$
        }

        public String getQueryId()
        {
            return queryId;
        }

        public void setQueryId(String queryId)
        {
            firePropertyChange("queryId", this.queryId, this.queryId = queryId); //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {
            if (token != null && !token.isBlank())
                getClient().setProperty(PROPERTY_TOKEN, token.trim());
            else
                getClient().removeProperty(PROPERTY_TOKEN);

            if (queryId != null && !queryId.isBlank())
                getClient().setProperty(PROPERTY_QUERY_ID, queryId.trim());
            else
                getClient().removeProperty(PROPERTY_QUERY_ID);
        }

        public static String getToken(Client client)
        {
            return client.getProperty(PROPERTY_TOKEN);
        }

        public static String getQueryId(Client client)
        {
            return client.getProperty(PROPERTY_QUERY_ID);
        }

        public static void clearConfiguration(Client client)
        {
            client.removeProperty(PROPERTY_TOKEN);
            client.removeProperty(PROPERTY_QUERY_ID);
        }

        public static boolean hasConfiguration(Client client)
        {
            String token = client.getProperty(PROPERTY_TOKEN);
            String queryId = client.getProperty(PROPERTY_QUERY_ID);
            return (token != null && !token.isBlank()) || (queryId != null && !queryId.isBlank());
        }
    }

    private static final int DELETE_ID = IDialogConstants.CLIENT_ID + 1;

    private final Client client;

    public IBFlexConfigurationDialog(Shell parentShell, Client client)
    {
        super(parentShell, Messages.IBFlexConfigureCredentials, new IBFlexModel(client));
        this.client = client;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        Button deleteButton = createButton(parent, DELETE_ID, Messages.IBFlexDeleteConfiguration, false);
        deleteButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (MessageDialog.openConfirm(getShell(), Messages.IBFlexDeleteConfiguration,
                            Messages.IBFlexDeleteConfigurationConfirm))
            {
                IBFlexModel.clearConfiguration(client);
                setReturnCode(CANCEL);
                close();
            }
        }));
        deleteButton.setEnabled(IBFlexModel.hasConfiguration(client));

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        var tokenObservable = bindings().bindStringInput(editArea, Messages.IBFlexToken, "token", SWT.PASSWORD); //$NON-NLS-1$
        var queryIdObservable = bindings().bindStringInput(editArea, Messages.IBFlexQueryId, "queryId", SWT.NONE); //$NON-NLS-1$

        // Validation: both fields required
        MultiValidator validator = new MultiValidator()
        {
            @Override
            protected IStatus validate()
            {
                String tokenValue = tokenObservable.getValue();
                String queryIdValue = queryIdObservable.getValue();

                if (tokenValue == null || tokenValue.isBlank())
                    return ValidationStatus.error(MessageFormat.format(Messages.IBFlexFieldRequired, Messages.IBFlexToken));
                if (queryIdValue == null || queryIdValue.isBlank())
                    return ValidationStatus.error(MessageFormat.format(Messages.IBFlexFieldRequired, Messages.IBFlexQueryId));

                return ValidationStatus.ok();
            }
        };
        bindings().getBindingContext().addValidationStatusProvider(validator);

        // Security hint
        Label securityHint = new Label(editArea, SWT.WRAP);
        securityHint.setText(Messages.IBFlexSecurityHint);
        GridDataFactory.fillDefaults().span(2, 1).hint(400, SWT.DEFAULT).applyTo(securityHint);

        // Documentation link
        Link link = new Link(editArea, SWT.NONE);
        link.setText("<a>" + Messages.IBFlexDocumentationLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        GridDataFactory.fillDefaults().span(2, 1).applyTo(link);
        link.addListener(SWT.Selection, e -> DesktopAPI.browse(
                        "https://www.interactivebrokers.com/campus/ibkr-api-page/flex-web-service/")); //$NON-NLS-1$
    }

    public String getToken()
    {
        return ((IBFlexModel) getModel()).getToken();
    }

    public String getQueryId()
    {
        return ((IBFlexModel) getModel()).getQueryId();
    }
}

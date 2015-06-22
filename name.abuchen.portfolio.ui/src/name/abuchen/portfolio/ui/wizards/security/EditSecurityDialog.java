package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditSecurityDialog extends Dialog
{
    private CTabFolder tabFolder;
    private Label errorMessage;

    private final EditSecurityModel model;
    private final BindingHelper bindings;

    private boolean showQuoteConfigurationInitially = false;

    public EditSecurityDialog(Shell parentShell, Client client, Security security)
    {
        super(parentShell);

        this.model = new EditSecurityModel(client, security);
        this.bindings = new BindingHelper(model)
        {
            @Override
            public void onValidationStatusChanged(IStatus status)
            {
                boolean isOK = status.getSeverity() == IStatus.OK;

                if (errorMessage != null && !errorMessage.isDisposed())
                    errorMessage.setText(isOK ? "" : status.getMessage()); //$NON-NLS-1$

                Button button = getButton(OK);
                if (button != null && !button.isDisposed())
                    button.setEnabled(isOK);
            }
        };
    }

    public void setShowQuoteConfigurationInitially(boolean showQuoteConfigurationInitially)
    {
        this.showQuoteConfigurationInitially = showQuoteConfigurationInitially;
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(model.getName() != null ? model.getName() : Messages.NewFileWizardSecurityTitle);
    }

    @Override
    protected Point getInitialSize()
    {
        Point preferredSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        // create dialog with a minimum size
        preferredSize.x = Math.max(preferredSize.x, 700);
        preferredSize.y = Math.max(preferredSize.y, 500);
        return preferredSize;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);

        // When creating the binding helper, the ok button does not yet exist.
        // Make sure that it has the correct initial enablement state as the
        // data loaded might not validate.
        getButton(OK).setEnabled("".equals(errorMessage.getText())); //$NON-NLS-1$

        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).applyTo(container);

        createUpperArea(container);
        createTabFolder(container);

        return container;
    }

    private void createUpperArea(Composite container)
    {
        Composite header = new Composite(container, SWT.NONE);
        header.setLayout(new FormLayout());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

        Label lblName = new Label(header, SWT.NONE);
        lblName.setText(Messages.ColumnName);
        Text name = new Text(header, SWT.BORDER);
        errorMessage = new Label(header, SWT.NONE);
        errorMessage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

        // form layout

        FormData data = new FormData();
        data.top = new FormAttachment(0, 10);
        data.left = new FormAttachment(0, 5);
        lblName.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(lblName, 0, SWT.CENTER);
        data.left = new FormAttachment(lblName, 10);
        data.right = new FormAttachment(100, -5);
        name.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(lblName, 10);
        data.left = new FormAttachment(0, 5);
        data.right = new FormAttachment(100, 5);
        errorMessage.setLayoutData(data);

        // bind to model

        bindings.getBindingContext().bindValue(SWTObservables.observeText(name, SWT.Modify), //
                        BeansObservables.observeValue(model, "name"), //$NON-NLS-1$
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                String v = (String) value;
                                return v != null && v.trim().length() > 0 ? ValidationStatus.ok() : ValidationStatus
                                                .error(MessageFormat.format(Messages.MsgDialogInputRequired,
                                                                Messages.ColumnName));
                            }
                        }), //
                        null);
    }

    private void createTabFolder(Composite container)
    {
        tabFolder = new CTabFolder(container, SWT.TOP | SWT.FLAT);
        tabFolder.setBorderVisible(true);
        tabFolder.setTabHeight(20);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

        tabFolder.addSelectionListener(new SelectionAdapter()
        {
            private AbstractPage current = null;

            public void widgetSelected(SelectionEvent e)
            {
                if (current != null)
                    current.afterPage();

                current = (AbstractPage) ((CTabItem) e.item).getData();
                current.beforePage();
            }
        });

        addPage(new SecurityMasterDataPage(model, bindings), PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY));
        addPage(new AttributesPage(model, bindings), null);
        addPage(new SecurityTaxonomyPage(model, bindings), null);
        addPage(new HistoricalQuoteProviderPage(model, bindings), null);
        addPage(new LatestQuoteProviderPage(model, bindings), null);

        tabFolder.setSelection(showQuoteConfigurationInitially ? 3 : 0);

        // selection event not fired for initial selection
        ((AbstractPage) tabFolder.getSelection().getData()).beforePage();
    }

    private void addPage(AbstractPage page, Image image)
    {
        page.createControl(tabFolder);

        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setImage(image);
        item.setControl(page.getControl());
        item.setText(page.getTitle());
        item.setData(page);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected void okPressed()
    {
        ((AbstractPage) tabFolder.getSelection().getData()).afterPage();

        Security security = model.getSecurity();

        // ask user what to do with existing quotes
        boolean hasQuotes = !security.getPrices().isEmpty();

        boolean feedChanged = !Objects.equals(model.getFeed(), security.getFeed());
        boolean tickerChanged = !Objects.equals(model.getTickerSymbol(), security.getTickerSymbol());
        boolean feedURLChanged = !Objects.equals(model.getFeedURL(), security.getFeedURL());
        boolean currencyChanged = !Objects.equals(model.getCurrencyCode(), security.getCurrencyCode());

        boolean quotesCanChange = feedChanged || tickerChanged || feedURLChanged || currencyChanged;

        model.applyChanges();

        if (hasQuotes && quotesCanChange)
        {
            MessageDialog dialog = new MessageDialog(getShell(), //
                            Messages.MessageDialogProviderChanged, null, //
                            Messages.MessageDialogProviderChangedText, //
                            MessageDialog.QUESTION, //
                            new String[] { Messages.MessageDialogProviderAnswerKeep,
                                            Messages.MessageDialogProviderAnswerReplace }, 0);
            if (dialog.open() == 1)
                security.removeAllPrices();
        }

        super.okPressed();
    }
}

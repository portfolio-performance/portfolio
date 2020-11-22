package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.Objects;

import javax.inject.Inject;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.databinding.swt.WidgetProperties;
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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.events.ChangeEventConstants;
import name.abuchen.portfolio.events.SecurityChangeEvent;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;

public class EditSecurityDialog extends Dialog
{
    @Inject
    private IEventBroker eventBroker;

    private CTabFolder tabFolder;
    private Label errorMessage;

    private final EditSecurityModel model;
    private final BindingHelper bindings;

    private boolean showQuoteConfigurationInitially = false;

    @Inject
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
        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(container);

        createUpperArea(container);
        createTabFolder(container);

        return container;
    }

    private void createUpperArea(Composite container)
    {
        Composite header = new Composite(container, SWT.NONE);
        header.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        header.setLayout(new FormLayout());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

        Label lblName = new Label(header, SWT.NONE);
        lblName.setText(Messages.ColumnName);
        lblName.setBackground(header.getBackground());
        Text name = new Text(header, SWT.BORDER);
        name.setBackground(header.getBackground());

        errorMessage = new Label(header, SWT.NONE);
        errorMessage.setForeground(Colors.theme().redForeground());
        errorMessage.setBackground(header.getBackground());

        Label imageLabel = new Label(header, SWT.NONE);
        imageLabel.setBackground(header.getBackground());
        imageLabel.setImage(Images.BANNER.image());

        // form layout

        FormDataFactory.startingWith(imageLabel).right(new FormAttachment(100));

        FormDataFactory.startingWith(lblName) //
                        .left(new FormAttachment(0, 5)).top(new FormAttachment(0, 10)) //
                        .thenRight(name).right(new FormAttachment(imageLabel, -10));

        FormDataFactory.startingWith(errorMessage) //
                        .left(new FormAttachment(0, 5)).top(new FormAttachment(lblName, 10))
                        .right(new FormAttachment(imageLabel, -10));

        // bind to model

        @SuppressWarnings("unchecked")
        IObservableValue<String> targetName = WidgetProperties.text(SWT.Modify).observe(name);
        @SuppressWarnings("unchecked")
        IObservableValue<String> observable = BeanProperties.value("name").observe(model); //$NON-NLS-1$
        bindings.getBindingContext().bindValue(targetName, observable,
                        new UpdateValueStrategy<String, String>().setAfterConvertValidator(
                                        v -> v != null && v.trim().length() > 0 ? ValidationStatus.ok()
                                                        : ValidationStatus.error(MessageFormat.format(
                                                                        Messages.MsgDialogInputRequired,
                                                                        Messages.ColumnName))),
                        null);
    }

    private void createTabFolder(Composite container)
    {
        tabFolder = new CTabFolder(container, SWT.TOP | SWT.FLAT);
        tabFolder.setBorderVisible(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

        tabFolder.addSelectionListener(new SelectionAdapter()
        {
            private AbstractPage current = null;

            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (current != null)
                    current.afterPage();

                current = (AbstractPage) ((CTabItem) e.item).getData();
                current.beforePage();
            }
        });

        addPage(new SecurityMasterDataPage(model, bindings), Images.SECURITY.image());
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

        eventBroker.post(ChangeEventConstants.Security.EDITED, new SecurityChangeEvent(model.getClient(), security));

        if (hasQuotes && quotesCanChange)
        {
            MessageDialog dialog = new MessageDialog(getShell(), //
                            Messages.MessageDialogProviderChanged, null, //
                            Messages.MessageDialogProviderChangedText, //
                            MessageDialog.QUESTION, //
                            new String[] { Messages.MessageDialogProviderAnswerKeep,
                                            Messages.MessageDialogProviderAnswerReplace },
                            0);
            if (dialog.open() == 1)
                security.removeAllPrices();
        }

        super.okPressed();
    }
}

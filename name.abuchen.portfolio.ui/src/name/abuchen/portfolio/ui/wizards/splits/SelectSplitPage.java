package name.abuchen.portfolio.ui.wizards.splits;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class SelectSplitPage extends AbstractWizardPage
{
    private StockSplitModel model;
    private BindingHelper bindings;

    public SelectSplitPage(StockSplitModel model)
    {
        super("select-stock-split"); //$NON-NLS-1$

        setTitle(Messages.SplitWizardDefinitionTitle);
        setDescription(Messages.SplitWizardDefinitionDescription);

        this.model = model;

        bindings = new BindingHelper(model)
        {
            @Override
            public void onValidationStatusChanged(IStatus status)
            {
                boolean isOK = status.getSeverity() == IStatus.OK;
                setErrorMessage(isOK ? null : status.getMessage());
                setPageComplete(isOK);
            }
        };
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new FormLayout());

        Label labelSecurity = new Label(container, SWT.NONE);
        labelSecurity.setText(Messages.ColumnSecurity);

        List<Security> securities = model.getClient().getActiveSecurities();
        if (model.getSecurity() != null && !securities.contains(model.getSecurity()))
            securities.add(0, model.getSecurity());

        ComboViewer comboSecurity = new ComboViewer(container, SWT.READ_ONLY);
        comboSecurity.setContentProvider(ArrayContentProvider.getInstance());
        comboSecurity.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }
        });
        comboSecurity.setInput(securities);

        Label labelExDate = new Label(container, SWT.NONE);
        labelExDate.setText(Messages.ColumnExDate);

        DatePicker boxExDate = new DatePicker(container);

        Label labelSplit = new Label(container, SWT.NONE);
        labelSplit.setText(Messages.SplitWizardLabelSplit);

        Spinner spinnerNewShares = new Spinner(container, SWT.BORDER);
        spinnerNewShares.setMinimum(1);
        spinnerNewShares.setMaximum(1000000);
        spinnerNewShares.setSelection(1);
        spinnerNewShares.setIncrement(1);
        spinnerNewShares.setFocus();

        Label labelColon = new Label(container, SWT.NONE);
        labelColon.setText(Messages.SplitWizardLabelNewForOld);

        Spinner spinnerOldShares = new Spinner(container, SWT.BORDER);
        spinnerOldShares.setMinimum(1);
        spinnerOldShares.setMaximum(1000000);
        spinnerOldShares.setSelection(1);
        spinnerOldShares.setIncrement(1);

        // form layout data

        FormData data = new FormData();
        data.top = new FormAttachment(comboSecurity.getControl(), 0, SWT.CENTER);
        labelSecurity.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(labelSecurity, 5);
        data.right = new FormAttachment(100);
        comboSecurity.getControl().setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(boxExDate.getControl(), 0, SWT.CENTER);
        labelExDate.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(comboSecurity.getControl(), 5);
        data.left = new FormAttachment(comboSecurity.getControl(), 2, SWT.LEFT);
        boxExDate.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(spinnerNewShares, 0, SWT.CENTER);
        labelSplit.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(boxExDate.getControl(), 5);
        data.left = new FormAttachment(boxExDate.getControl(), 0, SWT.LEFT);
        spinnerNewShares.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(spinnerNewShares, 0, SWT.CENTER);
        data.left = new FormAttachment(spinnerNewShares, 5);
        labelColon.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(boxExDate.getControl(), 5);
        data.left = new FormAttachment(labelColon, 5);
        spinnerOldShares.setLayoutData(data);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        context.bindValue(ViewersObservables.observeSingleSelection(comboSecurity),
                        BeanProperties.value("security").observe(model), null, null); //$NON-NLS-1$

        context.bindValue(new SimpleDateTimeDateSelectionProperty().observe(boxExDate.getControl()), //
                        BeanProperties.value("exDate").observe(model), //$NON-NLS-1$
                        new UpdateValueStrategy() //
                                        .setAfterConvertValidator(value -> {
                                            return value != null ? ValidationStatus.ok()
                                                            : ValidationStatus.error(MessageFormat.format(
                                                                            Messages.MsgDialogInputRequired,
                                                                            Messages.ColumnExDate));
                                        }),
                        null);

        final ISWTObservableValue observeNewShares = WidgetProperties.selection().observe(spinnerNewShares);
        context.bindValue(observeNewShares, BeanProperties.value("newShares").observe(model)); //$NON-NLS-1$

        final ISWTObservableValue observeOldShares = WidgetProperties.selection().observe(spinnerOldShares);
        context.bindValue(observeOldShares, BeanProperties.value("oldShares").observe(model)); //$NON-NLS-1$

        MultiValidator validator = new MultiValidator()
        {

            @Override
            protected IStatus validate()
            {
                Object newShares = observeNewShares.getValue();
                Object oldShares = observeOldShares.getValue();

                return newShares.equals(oldShares)
                                ? ValidationStatus.error(Messages.SplitWizardErrorNewAndOldMustNotBeEqual)
                                : ValidationStatus.ok();
            }

        };
        context.addValidationStatusProvider(validator);
    }
}

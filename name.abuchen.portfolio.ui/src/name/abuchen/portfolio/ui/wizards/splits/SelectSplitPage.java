package name.abuchen.portfolio.ui.wizards.splits;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
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

        int labelWidth = widest(labelSecurity, labelExDate, labelSplit);

        startingWith(comboSecurity.getControl(), labelSecurity) //
                        .thenBelow(boxExDate.getControl()).label(labelExDate) //
                        .thenBelow(spinnerNewShares).label(labelSplit).thenRight(labelColon)
                        .thenRight(spinnerOldShares);

        startingWith(labelSecurity).width(labelWidth);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        @SuppressWarnings("unchecked")
        IObservableValue<?> securityObservable = BeanProperties.value("security").observe(model); //$NON-NLS-1$
        context.bindValue(ViewersObservables.observeSingleSelection(comboSecurity), securityObservable, null, null);

        @SuppressWarnings("unchecked")
        IObservableValue<?> dateObservable = BeanProperties.value("exDate").observe(model); //$NON-NLS-1$
        context.bindValue(new SimpleDateTimeDateSelectionProperty().observe(boxExDate.getControl()), dateObservable,
                        new UpdateValueStrategy() //
                                        .setAfterConvertValidator(value -> value != null ? ValidationStatus.ok()
                                                        : ValidationStatus.error(MessageFormat.format(
                                                                        Messages.MsgDialogInputRequired,
                                                                        Messages.ColumnExDate))),
                        null);

        final ISWTObservableValue newSharesTargetObservable = WidgetProperties.selection().observe(spinnerNewShares);
        @SuppressWarnings("unchecked")
        IObservableValue<?> newSharesModelObservable = BeanProperties.value("newShares").observe(model); //$NON-NLS-1$
        context.bindValue(newSharesTargetObservable, newSharesModelObservable);

        final ISWTObservableValue oldSharesTargetObservable = WidgetProperties.selection().observe(spinnerOldShares);
        @SuppressWarnings("unchecked")
        IObservableValue<?> oldSharesModelObservable = BeanProperties.value("oldShares").observe(model); //$NON-NLS-1$
        context.bindValue(oldSharesTargetObservable, oldSharesModelObservable);

        MultiValidator validator = new MultiValidator()
        {

            @Override
            protected IStatus validate()
            {
                Object newShares = newSharesTargetObservable.getValue();
                Object oldShares = oldSharesTargetObservable.getValue();

                return newShares.equals(oldShares)
                                ? ValidationStatus.error(Messages.SplitWizardErrorNewAndOldMustNotBeEqual)
                                : ValidationStatus.ok();
            }

        };
        context.addValidationStatusProvider(validator);
    }
}

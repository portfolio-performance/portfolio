package name.abuchen.portfolio.ui.wizards.splits;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.text.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.IValidatingConverter;
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

        Text newShares = new Text(container, SWT.BORDER | SWT.RIGHT);
        newShares.setFocus();

        Label labelColon = new Label(container, SWT.NONE);
        labelColon.setText(Messages.SplitWizardLabelNewForOld);

        Text oldShares = new Text(container, SWT.BORDER | SWT.RIGHT);

        // form layout data

        int amountWidth = amountWidth(oldShares);
        int labelWidth = widest(labelSecurity, labelExDate, labelSplit);

        startingWith(comboSecurity.getControl(), labelSecurity) //
                        .thenBelow(boxExDate.getControl()).label(labelExDate) //
                        .thenBelow(newShares).width(amountWidth).label(labelSplit).thenRight(labelColon)
                        .thenRight(oldShares).width(amountWidth);

        startingWith(labelSecurity).width(labelWidth);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        IObservableValue<?> targetObservable = ViewerProperties.singleSelection().observe(comboSecurity);
        IObservableValue<?> securityObservable = BeanProperties.value("security").observe(model); //$NON-NLS-1$
        context.bindValue(targetObservable, securityObservable, null, null);

        IObservableValue<LocalDate> targetExDate = new SimpleDateTimeDateSelectionProperty()
                        .observe(boxExDate.getControl());
        IObservableValue<LocalDate> modelExDate = BeanProperties.value("exDate", LocalDate.class).observe(model); //$NON-NLS-1$
        context.bindValue(targetExDate, modelExDate, new UpdateValueStrategy<LocalDate, LocalDate>() //
                        .setAfterConvertValidator(value -> value != null ? ValidationStatus.ok()
                                        : ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired,
                                                        Messages.ColumnExDate))),
                        null);

        IObservableValue<String> newSharesTargetObservable = setupBinding(context, newShares, "newShares", //$NON-NLS-1$
                        Messages.ColumnUpdatedShares);
        IObservableValue<String> oldSharesTargetObservable = setupBinding(context, oldShares, "oldShares", //$NON-NLS-1$
                        Messages.ColumnCurrentShares);

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

    private IObservableValue<String> setupBinding(DataBindingContext context, Text input, String propertyName,
                    String propertyLabel)
    {
        NumberFormat format = new DecimalFormat("#,##0"); //$NON-NLS-1$

        IValidatingConverter<Object, BigDecimal> converter = IValidatingConverter
                        .wrap(StringToNumberConverter.toBigDecimal());

        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(input);
        IObservableValue<BigDecimal> modelObservable = BeanProperties.value(propertyName, BigDecimal.class)
                        .observe(model);

        context.bindValue(targetObservable, modelObservable, //
                        new UpdateValueStrategy<String, BigDecimal>().setAfterGetValidator(converter)
                                        .setConverter(converter)
                                        .setAfterConvertValidator(convertedValue -> convertedValue != null
                                                        && convertedValue.compareTo(BigDecimal.ZERO) > 0
                                                                        ? ValidationStatus.ok()
                                                                        : ValidationStatus.error(MessageFormat.format(
                                                                                        Messages.MsgDialogInputRequired,
                                                                                        propertyLabel))),
                        new UpdateValueStrategy<BigDecimal, String>()
                                        .setConverter(NumberToStringConverter.fromBigDecimal(format)));

        return targetObservable;
    }
}

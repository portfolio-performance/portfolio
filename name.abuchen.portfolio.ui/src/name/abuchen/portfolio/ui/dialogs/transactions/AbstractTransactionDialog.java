package name.abuchen.portfolio.ui.dialogs.transactions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.conversion.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.IValidatingConverter;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;

public abstract class AbstractTransactionDialog extends TitleAreaDialog
{
    public class Input
    {
        public final Label label;
        public final Text value;
        public final Label currency;

        public Input(Composite editArea, String text)
        {
            label = new Label(editArea, SWT.LEFT);
            label.setText(text);
            value = new Text(editArea, SWT.BORDER | SWT.RIGHT);
            value.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusGained(FocusEvent e)
                {
                    value.selectAll();
                }
            });

            currency = new Label(editArea, SWT.NONE);
        }

        public void bindValue(String property, String description, Values<?> values, boolean isMandatory)
        {
            StringToCurrencyConverter converter = new StringToCurrencyConverter(values);
            UpdateValueStrategy strategy = new UpdateValueStrategy();
            strategy.setAfterGetValidator(converter);
            strategy.setConverter(converter);
            if (isMandatory)
            {
                strategy.setAfterConvertValidator(convertedValue -> {
                    Long v = (Long) convertedValue;
                    return v != null && v.longValue() > 0 ? ValidationStatus.ok()
                                    : ValidationStatus.error(
                                                    MessageFormat.format(Messages.MsgDialogInputRequired, description));
                });
            }

            context.bindValue(WidgetProperties.text(SWT.Modify).observe(value), //
                            BeanProperties.value(property).observe(model), //
                            strategy, new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(values)));
        }

        public void bindCurrency(String property)
        {
            context.bindValue(WidgetProperties.text().observe(currency), //
                            BeanProperties.value(property).observe(model));
        }

        public void bindBigDecimal(String property, String pattern)
        {
            NumberFormat format = new DecimalFormat(pattern);

            IValidatingConverter converter = IValidatingConverter.wrap(StringToNumberConverter.toBigDecimal());

            context.bindValue(WidgetProperties.text(SWT.Modify).observe(value), //
                            BeanProperties.value(property).observe(model), //
                            new UpdateValueStrategy().setAfterGetValidator(converter).setConverter(converter),
                            new UpdateValueStrategy().setConverter(NumberToStringConverter.fromBigDecimal(format)));
        }

        public void setVisible(boolean visible)
        {
            label.setVisible(visible);
            value.setVisible(visible);
            currency.setVisible(visible);
        }
    }

    public class ComboInput
    {
        public final Label label;
        public final ComboViewer value;
        public final Label currency;

        public ComboInput(Composite editArea, String text)
        {
            if (text != null)
            {
                label = new Label(editArea, SWT.RIGHT);
                label.setText(text);
            }
            else
            {
                label = null;
            }
            value = new ComboViewer(editArea);
            value.setContentProvider(new ArrayContentProvider());
            currency = new Label(editArea, SWT.NONE);
        }

        public IObservableValue bindValue(String property, String missingValueMessage)
        {
            UpdateValueStrategy strategy = new UpdateValueStrategy();
            strategy.setAfterConvertValidator(
                            v -> v != null ? ValidationStatus.ok() : ValidationStatus.error(missingValueMessage));
            IObservableValue observable = ViewersObservables.observeSingleSelection(value);
            context.bindValue(observable, BeanProperties.value(property).observe(model), strategy, null);
            return observable;
        }

        public void bindCurrency(String property)
        {
            context.bindValue(WidgetProperties.text().observe(currency), BeanProperties.value(property).observe(model));
        }
    }

    class ModelStatusListener
    {
        public void setStatus(IStatus status)
        {
            setErrorMessage(status.getSeverity() == IStatus.OK ? null : status.getMessage());

            for (int buttonId : new int[] { IDialogConstants.OK_ID, SAVE_AND_NEW_ID })
            {
                Control button = getButton(buttonId);
                if (button != null)
                    button.setEnabled(status.getSeverity() == IStatus.OK);
            }
        }

        public IStatus getStatus()
        {
            // irrelevant
            return ValidationStatus.ok();
        }
    }

    public static final int SAVE_AND_NEW_ID = 4711;

    protected AbstractModel model;
    protected DataBindingContext context = new DataBindingContext();
    protected ModelStatusListener status = new ModelStatusListener();

    public AbstractTransactionDialog(Shell parentShell)
    {
        super(parentShell);
        setTitleImage(Images.BANNER.image());
    }

    protected void setModel(AbstractModel model)
    {
        this.model = model;
    }

    @Override
    public void create()
    {
        super.create();

        setTitle(model.getHeading());
        setMessage(""); //$NON-NLS-1$

        setShellStyle(getShellStyle() | SWT.RESIZE);

        status.setStatus(AggregateValidationStatus.getStatusMaxSeverity(context.getValidationStatusProviders()));
    }

    @Override
    protected int getShellStyle()
    {
        return super.getShellStyle() | SWT.RESIZE;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, Messages.LabelSave, true);
        createButton(parent, SAVE_AND_NEW_ID, Messages.LabelSaveAndNew, false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(area, SWT.NONE);
        FormLayout layout = new FormLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 5;
        editArea.setLayout(layout);

        createFormElements(editArea);

        IObservableValue<IStatus> calculationStatus = BeanProperties.value("calculationStatus").observe(model); //$NON-NLS-1$
        this.context.addValidationStatusProvider(new MultiValidator()
        {
            @Override
            protected IStatus validate()
            {
                return calculationStatus.getValue();
            }
        });

        context.bindValue(PojoProperties.value("status").observe(status), //$NON-NLS-1$
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY));

        return editArea;
    }

    protected abstract void createFormElements(Composite editArea);

    @Override
    protected void okPressed()
    {
        model.applyChanges();
        super.okPressed();
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == SAVE_AND_NEW_ID)
        {
            model.applyChanges();
            model.resetToNewTransaction();
            getDialogArea().setFocus();
        }
        else
        {
            super.buttonPressed(buttonId);
        }
    }

    public void setAccount(Account account)
    {}

    public void setPortfolio(Portfolio portfolio)
    {}

    public void setSecurity(Security security)
    {}

    /**
     * make sure drop-down boxes contain the security, portfolio and account of this
     * transaction (they might be "retired" and do not show by default)
     */
    protected <T> List<T> including(List<T> list, T element)
    {
        if (element != null && !list.contains(element))
            list.add(0, element);
        return list;
    }
}

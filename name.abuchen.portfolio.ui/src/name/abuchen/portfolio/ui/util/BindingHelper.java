package name.abuchen.portfolio.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Isin;

public class BindingHelper
{
    private static final class StringToCurrencyUnitConverter implements IConverter
    {
        @Override
        public Object getToType()
        {
            return CurrencyUnit.class;
        }

        @Override
        public Object getFromType()
        {
            return String.class;
        }

        @Override
        public Object convert(Object fromObject)
        {
            return fromObject == null ? CurrencyUnit.EMPTY : CurrencyUnit.getInstance((String) fromObject);
        }
    }

    private static final class CurrencyUnitToStringConverter implements IConverter
    {
        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public Object getFromType()
        {
            return CurrencyUnit.class;
        }

        @Override
        public Object convert(Object fromObject)
        {
            return CurrencyUnit.EMPTY.equals(fromObject) ? null : ((CurrencyUnit) fromObject).getCurrencyCode();
        }
    }

    public abstract static class Model
    {
        private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

        private Client client;

        public Model()
        {}

        public Model(Client client)
        {
            this.client = client;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

        public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
        {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
        {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        }

        public Client getClient()
        {
            return client;
        }

        protected void firePropertyChange(String attribute, Object oldValue, Object newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        protected void firePropertyChange(String attribute, long oldValue, long newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        public abstract void applyChanges();
    }

    private static final class StatusTextConverter implements IConverter
    {
        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public Object getFromType()
        {
            return IStatus.class;
        }

        @Override
        public Object convert(Object fromObject)
        {
            IStatus status = (IStatus) fromObject;
            return status.isOK() ? "" : status.getMessage(); //$NON-NLS-1$
        }
    }

    class ModelStatusListener
    {
        public void setStatus(IStatus status)
        {
            onValidationStatusChanged(status);
        }

        public IStatus getStatus()
        {
            // irrelevant
            return ValidationStatus.ok();
        }
    }

    private Model model;
    private ModelStatusListener listener = new ModelStatusListener();
    private DataBindingContext context;

    /** average char width needed to resize input fields on length */
    private int averageCharWidth = -1;

    public BindingHelper(Model model)
    {
        this.model = model;
        this.context = new DataBindingContext();

        context.bindValue(PojoProperties.value("status").observe(listener), //$NON-NLS-1$
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY));
    }

    public void onValidationStatusChanged(IStatus status)
    {}

    public DataBindingContext getBindingContext()
    {
        return context;
    }

    public final void createErrorLabel(Composite editArea)
    {
        // error label
        Label errorLabel = new Label(editArea, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(errorLabel);

        // error label
        context.bindValue(WidgetProperties.text().observe(errorLabel), //
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY), //
                        null, //
                        new UpdateValueStrategy().setConverter(new StatusTextConverter()));
    }

    public final void createLabel(Composite editArea, String text)
    {
        Label lblTransactionType = new Label(editArea, SWT.NONE);
        lblTransactionType.setText(text);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(lblTransactionType);
    }

    public final void bindLabel(Composite editArea, String property)
    {
        Label label = new Label(editArea, SWT.NONE);
        context.bindValue(WidgetProperties.text().observe(label), BeanProperties.value(property).observe(model));
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(label);
    }

    public final void bindSpinner(Composite editArea, String label, String property, int min, int max, int selection,
                    int increment)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        Spinner spinner = new Spinner(editArea, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setSelection(selection);
        spinner.setIncrement(increment);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
                        .hint(5 * getAverageCharWidth(spinner), SWT.DEFAULT).applyTo(spinner);
        context.bindValue(WidgetProperties.selection().observe(spinner), BeanProperties.value(property).observe(model));
    }

    public final ComboViewer bindComboViewer(Composite editArea, String label, String property,
                    IBaseLabelProvider labelProvider, Object input)
    {
        return bindComboViewer(editArea, label, property, labelProvider, null, input);
    }

    public final ComboViewer bindComboViewer(Composite editArea, String label, String property,
                    IBaseLabelProvider labelProvider, IValidator validator, Object input)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(labelProvider);
        combo.setInput(input);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(combo.getControl());

        UpdateValueStrategy strategy = new UpdateValueStrategy();
        if (validator != null)
            strategy.setAfterConvertValidator(validator);

        context.bindValue(ViewersObservables.observeSingleSelection(combo), //
                        BeanProperties.value(property).observe(model), strategy, null);
        return combo;
    }

    public final ComboViewer bindCurrencyCodeCombo(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(new LabelProvider());

        List<CurrencyUnit> currencies = new ArrayList<>();
        currencies.add(CurrencyUnit.EMPTY);
        currencies.addAll(CurrencyUnit.getAvailableCurrencyUnits().stream().sorted().collect(Collectors.toList()));
        combo.setInput(currencies);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(combo.getControl());

        UpdateValueStrategy targetToModel = new UpdateValueStrategy();
        targetToModel.setConverter(new CurrencyUnitToStringConverter());

        UpdateValueStrategy modelToTarget = new UpdateValueStrategy();
        modelToTarget.setConverter(new StringToCurrencyUnitConverter());

        context.bindValue(ViewersObservables.observeSingleSelection(combo), //
                        BeanProperties.value(property).observe(model), targetToModel, modelToTarget);
        return combo;
    }

    public final void bindDatePicker(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        DateTimePicker boxDate = new DateTimePicker(editArea);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(boxDate.getControl());

        context.bindValue(new SimpleDateTimeSelectionProperty().observe(boxDate.getControl()),
                        BeanProperties.value(property).observe(model), new UpdateValueStrategy() //
                                        .setAfterConvertValidator(value -> {
                                            return value != null ? ValidationStatus.ok()
                                                            : ValidationStatus.error(MessageFormat.format(
                                                                            Messages.MsgDialogInputRequired, label));
                                        }),
                        null);
    }

    public final Control bindMandatoryQuoteInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);
        bindMandatoryDecimalInput(label, property, txtValue, Values.Quote);
        return txtValue;
    }

    private void bindMandatoryDecimalInput(final String label, String property, Text txtValue, Values<?> type)
    {
        StringToCurrencyConverter converter = new StringToCurrencyConverter(type);

        UpdateValueStrategy input2model = new UpdateValueStrategy() //
                        .setAfterGetValidator(converter) //
                        .setConverter(converter) //
                        .setAfterConvertValidator(value -> {
                            Long v = (Long) value;
                            return v != null && v.longValue() > 0 ? ValidationStatus.ok()
                                            : ValidationStatus.error(MessageFormat
                                                            .format(Messages.MsgDialogInputRequired, label));
                        });

        context.bindValue(WidgetProperties.text(SWT.Modify).observe(txtValue), //
                        BeanProperties.value(property).observe(model), //
                        input2model, // ,
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(type)));
    }

    private Text createTextInput(Composite editArea, final String label)
    {
        return createTextInput(editArea, label, SWT.NONE, SWT.DEFAULT);
    }

    private Text createTextInput(Composite editArea, final String label, int style, int lenghtInCharacters)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Text txtValue = new Text(editArea, SWT.BORDER | style);
        txtValue.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                txtValue.selectAll();
            }
        });

        if (lenghtInCharacters == SWT.DEFAULT)
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(txtValue);
        else
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
                            .hint((lenghtInCharacters + 5) * getAverageCharWidth(txtValue), SWT.DEFAULT)
                            .applyTo(txtValue);

        return txtValue;
    }

    public final Control bindMandatoryLongInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);

        context.bindValue(WidgetProperties.text(SWT.Modify).observe(txtValue), //
                        BeanProperties.value(property).observe(model), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                Long v = (Long) value;
                                return v != null && v.longValue() > 0 ? ValidationStatus.ok()
                                                : ValidationStatus.error(MessageFormat
                                                                .format(Messages.MsgDialogInputRequired, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property)
    {
        return bindStringInput(editArea, label, property, SWT.NONE, SWT.DEFAULT);
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property, int style)
    {
        return bindStringInput(editArea, label, property, style, SWT.DEFAULT);
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property, int style,
                    int lenghtInCharacters)
    {
        Text txtValue = createTextInput(editArea, label, style, lenghtInCharacters);

        ISWTObservableValue observeText = WidgetProperties.text(SWT.Modify).observe(txtValue);
        context.bindValue(observeText, BeanProperties.value(property).observe(model));

        return observeText;
    }

    public final Control bindMandatoryStringInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);

        context.bindValue(WidgetProperties.text(SWT.Modify).observe(txtValue), //
                        BeanProperties.value(property).observe(model), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                String v = (String) value;
                                return v != null && v.trim().length() > 0 ? ValidationStatus.ok()
                                                : ValidationStatus.error(MessageFormat
                                                                .format(Messages.MsgDialogInputRequired, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final Control bindISINInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label, SWT.NONE, 12);
        txtValue.setTextLimit(12);

        context.bindValue(WidgetProperties.text(SWT.Modify).observe(txtValue), //
                        BeanProperties.value(property).observe(model), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                String v = (String) value;
                                return v == null || v.trim().length() == 0 || Isin.isValid(v) ? ValidationStatus.ok()
                                                : ValidationStatus.error(MessageFormat
                                                                .format(Messages.MsgDialogNotAValidISIN, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final Control bindBooleanInput(Composite editArea, final String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Button btnCheckbox = new Button(editArea, SWT.CHECK);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(btnCheckbox);

        context.bindValue(WidgetProperties.selection().observe(btnCheckbox), //
                        BeanProperties.value(property).observe(model));
        return btnCheckbox;
    }

    private int getAverageCharWidth(Control control)
    {
        if (averageCharWidth > 0)
            return averageCharWidth;

        GC gc = new GC(control);
        FontMetrics fm = gc.getFontMetrics();
        this.averageCharWidth = fm.getAverageCharWidth();
        gc.dispose();

        return averageCharWidth;
    }
}

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
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
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
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class BindingHelper
{
    private static final class StringToCurrencyUnitConverter implements IConverter<String, CurrencyUnit>
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
        public CurrencyUnit convert(String fromObject)
        {
            return fromObject == null ? CurrencyUnit.EMPTY : CurrencyUnit.getInstance((String) fromObject);
        }
    }

    private static final class CurrencyUnitToStringConverter implements IConverter<CurrencyUnit, String>
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
        public String convert(CurrencyUnit fromObject)
        {
            return CurrencyUnit.EMPTY.equals(fromObject) ? null : fromObject.getCurrencyCode();
        }
    }

    private static final class StringToCalendarConverter implements IConverter<String, TradeCalendar>
    {
        private TradeCalendar emptyOption;

        public StringToCalendarConverter(TradeCalendar emptyOption)
        {
            this.emptyOption = emptyOption;
        }

        @Override
        public Object getToType()
        {
            return TradeCalendar.class;
        }

        @Override
        public Object getFromType()
        {
            return String.class;
        }

        @Override
        public TradeCalendar convert(String fromObject)
        {
            return fromObject == null ? emptyOption : TradeCalendarManager.getInstance(fromObject);
        }
    }

    private static final class CalendarToStringConverter implements IConverter<TradeCalendar, String>
    {
        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public Object getFromType()
        {
            return TradeCalendar.class;
        }

        @Override
        public String convert(TradeCalendar fromObject)
        {
            return fromObject.getCode().isEmpty() ? null : fromObject.getCode();
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

    private static final class StatusTextConverter implements IConverter<IStatus, String>
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
        public String convert(IStatus fromObject)
        {
            return fromObject.isOK() ? "" : fromObject.getMessage(); //$NON-NLS-1$
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
    private double averageCharWidth = -1;

    public BindingHelper(Model model)
    {
        this.model = model;
        this.context = new DataBindingContext();

        @SuppressWarnings("unchecked")
        IObservableValue<?> observable = PojoProperties.value("status").observe(listener); //$NON-NLS-1$
        context.bindValue(observable, new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY));
    }

    public void onValidationStatusChanged(IStatus status)
    {
        // can be overridden by sub-classes to change the UI on validation
        // changes
    }

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
        @SuppressWarnings("unchecked")
        IObservableValue<String> statusTarget = WidgetProperties.text().observe(errorLabel);
        IObservableValue<IStatus> statusModel = new AggregateValidationStatus(context,
                        AggregateValidationStatus.MAX_SEVERITY);
        context.bindValue(statusTarget, statusModel, null,
                        new UpdateValueStrategy<IStatus, String>().setConverter(new StatusTextConverter()));
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
        @SuppressWarnings("unchecked")
        IObservableValue<?> labelModel = BeanProperties.value(property).observe(model);
        IObservableValue<?> labelTarget = WidgetProperties.text().observe(label);
        context.bindValue(labelTarget, labelModel);
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
                        .hint((int) Math.round(5 * getAverageCharWidth(spinner)), SWT.DEFAULT).applyTo(spinner);
        @SuppressWarnings("unchecked")
        IObservableValue<?> spinnerModel = BeanProperties.value(property).observe(model);
        IObservableValue<?> spinnerTarget = WidgetProperties.selection().observe(spinner);
        context.bindValue(spinnerTarget, spinnerModel);
    }

    public final ComboViewer bindCurrencyCodeCombo(Composite editArea, String label, String property)
    {
        return bindCurrencyCodeCombo(editArea, label, property, true);
    }

    public final ComboViewer bindCurrencyCodeCombo(Composite editArea, String label, String property,
                    boolean includeEmpty)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(new LabelProvider());

        List<CurrencyUnit> currencies = new ArrayList<>();
        if (includeEmpty)
            currencies.add(CurrencyUnit.EMPTY);
        currencies.addAll(CurrencyUnit.getAvailableCurrencyUnits().stream().sorted().collect(Collectors.toList()));
        combo.setInput(currencies);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(combo.getControl());

        UpdateValueStrategy<CurrencyUnit, String> targetToModel = new UpdateValueStrategy<>();
        targetToModel.setConverter(new CurrencyUnitToStringConverter());

        UpdateValueStrategy<String, CurrencyUnit> modelToTarget = new UpdateValueStrategy<>();
        modelToTarget.setConverter(new StringToCurrencyUnitConverter());

        @SuppressWarnings("unchecked")
        IObservableValue<String> comboModel = BeanProperties.value(property).observe(model);
        @SuppressWarnings("unchecked")
        IObservableValue<CurrencyUnit> comboTarget = ViewersObservables.observeSingleSelection(combo);
        context.bindValue(comboTarget, comboModel, targetToModel, modelToTarget);
        return combo;
    }

    public final ComboViewer bindCalendarCombo(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(new LabelProvider());

        TradeCalendar emptyOption = TradeCalendarManager.createInheritDefaultOption();

        List<TradeCalendar> calendar = new ArrayList<>();
        calendar.add(emptyOption);
        calendar.addAll(TradeCalendarManager.getAvailableCalendar().sorted().collect(Collectors.toList()));
        combo.setInput(calendar);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(combo.getControl());

        UpdateValueStrategy<TradeCalendar, String> targetToModel = new UpdateValueStrategy<>();
        targetToModel.setConverter(new CalendarToStringConverter());

        UpdateValueStrategy<String, TradeCalendar> modelToTarget = new UpdateValueStrategy<>();
        modelToTarget.setConverter(new StringToCalendarConverter(emptyOption));

        @SuppressWarnings("unchecked")
        IObservableValue<TradeCalendar> targetObservable = ViewersObservables.observeSingleSelection(combo);
        @SuppressWarnings("unchecked")
        IObservableValue<String> observable = BeanProperties.value(property).observe(model);
        context.bindValue(targetObservable, observable, targetToModel, modelToTarget);
        return combo;
    }

    public final Control bindDatePicker(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        DatePicker boxDate = new DatePicker(editArea);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(boxDate.getControl());

        @SuppressWarnings("unchecked")
        IObservableValue<Object> targetObservable = new SimpleDateTimeDateSelectionProperty()
                        .observe(boxDate.getControl());
        @SuppressWarnings("unchecked")
        IObservableValue<Object> modelObservable = BeanProperties.value(property).observe(model);
        context.bindValue(targetObservable, modelObservable,
                        new UpdateValueStrategy<Object, Object>().setAfterConvertValidator(value -> value != null
                                        ? ValidationStatus.ok()
                                        : ValidationStatus.error(
                                                        MessageFormat.format(Messages.MsgDialogInputRequired, label))),
                        null);

        return boxDate.getControl();
    }

    public final Control bindMandatoryAmountInput(Composite editArea, final String label, String property, int style,
                    int lenghtInCharacters)
    {
        Text txtValue = createTextInput(editArea, label, style, lenghtInCharacters);
        bindMandatoryDecimalInput(label, property, txtValue, Values.Amount);
        return txtValue;
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

        UpdateValueStrategy<String, Long> input2model = new UpdateValueStrategy<>();
        input2model.setAfterGetValidator(converter);
        input2model.setConverter(converter);
        input2model.setAfterConvertValidator(v -> v != null && v.longValue() > 0 ? ValidationStatus.ok()
                        : ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, label)));

        @SuppressWarnings("unchecked")
        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(txtValue);
        @SuppressWarnings("unchecked")
        IObservableValue<Long> modelObservable = BeanProperties.value(property).observe(model);
        context.bindValue(targetObservable, modelObservable, input2model,
                        new UpdateValueStrategy<Long, String>().setConverter(new CurrencyToStringConverter(type)));
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
                            .hint((int) Math.round((lenghtInCharacters + 5) * getAverageCharWidth(txtValue)),
                                            SWT.DEFAULT)
                            .applyTo(txtValue);

        return txtValue;
    }

    public final Text bindStringInput(Composite editArea, final String label, String property)
    {
        return bindStringInput(editArea, label, property, SWT.NONE, SWT.DEFAULT);
    }

    public final IObservableValue<String> bindStringInput(Composite editArea, final String label, String property,
                    int style)
    {
        Text txtValue = createTextInput(editArea, label, style, SWT.DEFAULT);

        @SuppressWarnings("unchecked")
        IObservableValue<String> targetObservablebserveText = WidgetProperties.text(SWT.Modify).observe(txtValue);
        @SuppressWarnings("unchecked")
        IObservableValue<String> modelObeservable = BeanProperties.value(property).observe(model);
        context.bindValue(targetObservablebserveText, modelObeservable);

        return targetObservablebserveText;
    }

    @SuppressWarnings("unchecked")
    public final Text bindStringInput(Composite editArea, final String label, String property,
                    int style, int lenghtInCharacters)
    {
        Text txtValue = createTextInput(editArea, label, style, lenghtInCharacters);

        ISWTObservableValue observeText = WidgetProperties.text(SWT.Modify).observe(txtValue);
        context.bindValue(observeText, BeanProperties.value(property).observe(model));

        return txtValue;
    }

    public final Text bindISINInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label, SWT.NONE, 12);
        txtValue.setTextLimit(12);

        @SuppressWarnings("unchecked")
        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(txtValue);
        @SuppressWarnings("unchecked")
        IObservableValue<String> modelObservable = BeanProperties.value(property).observe(model);

        context.bindValue(targetObservable, modelObservable, //
                        new UpdateValueStrategy<String, String>().setAfterConvertValidator(
                                        v -> v == null || v.trim().length() == 0 || Isin.isValid(v)
                                                        ? ValidationStatus.ok()
                                                        : ValidationStatus.error(MessageFormat.format(
                                                                        Messages.MsgDialogNotAValidISIN, label))),
                        null);
        return txtValue;
    }

    public final Control bindBooleanInput(Composite editArea, final String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Button btnCheckbox = new Button(editArea, SWT.CHECK);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(btnCheckbox);

        IObservableValue<?> targetObservable = WidgetProperties.selection().observe(btnCheckbox);
        @SuppressWarnings("unchecked")
        IObservableValue<?> modelObservable = BeanProperties.value(property).observe(model);

        context.bindValue(targetObservable, modelObservable);
        return btnCheckbox;
    }

    private double getAverageCharWidth(Control control)
    {
        if (averageCharWidth > 0)
            return averageCharWidth;

        GC gc = new GC(control);
        FontMetrics fm = gc.getFontMetrics();
        this.averageCharWidth = fm.getAverageCharacterWidth();
        gc.dispose();

        return averageCharWidth;
    }
}

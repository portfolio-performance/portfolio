package name.abuchen.portfolio.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
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
    private static final class CurrencyProposalProvider implements IContentProposalProvider
    {
        private List<CurrencyUnit> currencies;

        private CurrencyProposalProvider(List<CurrencyUnit> currencies)
        {
            this.currencies = currencies;
        }

        @Override
        public IContentProposal[] getProposals(String contents, int position)
        {
            List<IContentProposal> proposals = new ArrayList<>();

            if (!contents.isEmpty())
            {
                String c = contents.toLowerCase();

                for (CurrencyUnit currency : currencies)
                {
                    String label = currency.getLabel().toLowerCase();
                    if (label.contains(c))
                        proposals.add(new ContentProposal(currency.getCurrencyCode(), currency.getLabel(), null));
                }
            }

            // below matching currencies, add everything for scrolling
            proposals.add(new ContentProposal(String.format("--- %s ---", Messages.LabelAllCurrencies))); //$NON-NLS-1$
            for (CurrencyUnit currency : currencies)
                proposals.add(new ContentProposal(currency.getCurrencyCode(), currency.getLabel(), null));
            return proposals.toArray(new IContentProposal[0]);
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
        {
        }

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
        IObservableValue<?> labelModel = BeanProperties.value(property).observe(model);
        IObservableValue<?> labelTarget = WidgetProperties.text().observe(label);
        context.bindValue(labelTarget, labelModel);
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(label);
    }

    public final void bindSpinner(Composite editArea, String label, String property, int min, int max, int increment)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        Spinner spinner = new Spinner(editArea, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setIncrement(increment);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
                        .hint((int) Math.round(5 * getAverageCharWidth(spinner)), SWT.DEFAULT).applyTo(spinner);
        IObservableValue<?> spinnerModel = BeanProperties.value(property).observe(model);
        IObservableValue<?> spinnerTarget = WidgetProperties.spinnerSelection().observe(spinner);
        context.bindValue(spinnerTarget, spinnerModel);
    }

    public final Control bindCurrencyCodeCombo(Composite editArea, String label, String property)
    {
        return bindCurrencyCodeCombo(editArea, label, property, true);
    }

    public final Control bindCurrencyCodeCombo(Composite editArea, String label, String property, boolean includeEmpty)
    {
        List<CurrencyUnit> units = new ArrayList<>();
        units.addAll(CurrencyUnit.getAvailableCurrencyUnits());

        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        Composite area = new Composite(editArea, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(area);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(area);

        Text currencyCode = new Text(area, SWT.BORDER);
        currencyCode.setTextLimit(3);
        currencyCode.addFocusListener(FocusListener.focusGainedAdapter(e -> currencyCode.selectAll()));

        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
                        .hint((int) Math.round(10 * getAverageCharWidth(currencyCode)), SWT.DEFAULT)
                        .applyTo(currencyCode);
        Label description = new Label(area, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(description);

        // test + content provider

        IContentProposalProvider provider = new CurrencyProposalProvider(units);
        ContentProposalAdapter adapter = new ContentProposalAdapter(currencyCode, new TextContentAdapter(), provider,
                        null, null);
        adapter.setPropagateKeys(true);
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

        currencyCode.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.BS)
                adapter.openProposalPopup();
        }));

        currencyCode.addMouseListener(MouseListener.mouseUpAdapter(e -> adapter.openProposalPopup()));

        currencyCode.addModifyListener(e -> {
            String code = currencyCode.getText();

            CurrencyUnit unit = CurrencyUnit.getInstance(code);
            if (unit != null)
            {
                description.setText(unit.getDisplayName());
            }
            else if (code.isEmpty() && includeEmpty)
            {
                description.setText(CurrencyUnit.EMPTY.getLabel());
            }
            else
            {
                description.setText(""); //$NON-NLS-1$
            }
        });

        UpdateValueStrategy<String, String> targetToModel = new UpdateValueStrategy<>();
        targetToModel.setAfterGetValidator(c -> {
            if ((c.isEmpty() && includeEmpty) || CurrencyUnit.getInstance(c) != null)
                return ValidationStatus.ok();
            else
                return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, label));
        });
        targetToModel.setConverter(IConverter.create(selected -> selected.isEmpty() ? null : selected.substring(0, 3)));

        UpdateValueStrategy<String, String> modelToTarget = new UpdateValueStrategy<>();
        modelToTarget.setConverter(IConverter.create(code -> {
            CurrencyUnit unit = CurrencyUnit.getInstance(code);
            return unit != null ? unit.getCurrencyCode() : null;
        }));

        IObservableValue<String> textModel = BeanProperties.value(property, String.class).observe(model);
        IObservableValue<String> textTarget = WidgetProperties.text(SWT.Modify).observe(currencyCode);
        context.bindValue(textTarget, textModel, targetToModel, modelToTarget);
        return area;
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

        IObservableValue<TradeCalendar> targetObservable = ViewerProperties.singleSelection(TradeCalendar.class)
                        .observe(combo);
        IObservableValue<String> observable = BeanProperties.value(property, String.class).observe(model);
        context.bindValue(targetObservable, observable, targetToModel, modelToTarget);
        return combo;
    }

    public final Control bindDatePicker(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        DatePicker boxDate = new DatePicker(editArea);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(boxDate.getControl());

        IObservableValue<LocalDate> targetObservable = new SimpleDateTimeDateSelectionProperty()
                        .observe(boxDate.getControl());
        IObservableValue<LocalDate> modelObservable = BeanProperties.value(property, LocalDate.class).observe(model);
        context.bindValue(targetObservable, modelObservable,
                        new UpdateValueStrategy<LocalDate, LocalDate>().setAfterConvertValidator(value -> value != null
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

        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(txtValue);
        IObservableValue<Long> modelObservable = BeanProperties.value(property, Long.class).observe(model);
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

        IObservableValue<String> targetObservablebserveText = WidgetProperties.text(SWT.Modify).observe(txtValue);
        IObservableValue<String> modelObeservable = BeanProperties.value(property, String.class).observe(model);
        context.bindValue(targetObservablebserveText, modelObeservable);

        return targetObservablebserveText;
    }

    public final Text bindStringInput(Composite editArea, final String label, String property, int style,
                    int lenghtInCharacters)
    {
        Text txtValue = createTextInput(editArea, label, style, lenghtInCharacters);

        ISWTObservableValue<String> observeText = WidgetProperties.text(SWT.Modify).observe(txtValue);
        context.bindValue(observeText, BeanProperties.value(property).observe(model));

        return txtValue;
    }

    public final Text bindISINInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label, SWT.NONE, 12);
        txtValue.setTextLimit(12);

        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(txtValue);
        IObservableValue<String> modelObservable = BeanProperties.value(property, String.class).observe(model);

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

        IObservableValue<?> targetObservable = WidgetProperties.buttonSelection().observe(btnCheckbox);
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

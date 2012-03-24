package name.abuchen.portfolio.ui.dialogs;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/* package */abstract class AbstractDialog extends Dialog
{
    /* package */abstract static class Model
    {
        private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

        private Client client;

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

        protected Client getClient()
        {
            return client;
        }

        protected void firePropertyChange(String attribute, Object oldValue, Object newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        protected void firePropertyChange(String attribute, int oldValue, int newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        public abstract void createChanges();
    }

    class ModelStatusListener
    {
        public void setStatus(IStatus status)
        {
            Control button = getButton(IDialogConstants.OK_ID);
            if (button != null)
                button.setEnabled(status.getSeverity() == IStatus.OK);
        }

        public IStatus getStatus()
        {
            // irrelevant
            return ValidationStatus.ok();
        }
    }

    private String title;
    private Model model;
    private ModelStatusListener listener = new ModelStatusListener();
    private DataBindingContext context;

    public AbstractDialog(Shell parentShell, String title, Client client, Model model)
    {
        super(parentShell);
        this.title = title;
        this.model = model;
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(title);
    }

    protected Model getModel()
    {
        return model;
    }

    protected DataBindingContext getBindingContext()
    {
        return context;
    }

    @Override
    protected final Control createContents(Composite parent)
    {
        Control answer = super.createContents(parent);

        listener.setStatus(AggregateValidationStatus.getStatusMaxSeverity(context.getValidationStatusProviders()));

        return answer;
    }

    protected final Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(editArea);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        context = new DataBindingContext();

        createFormElements(editArea);

        // error label
        Label errorLabel = new Label(editArea, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(errorLabel);

        // error label
        context.bindValue(SWTObservables.observeText(errorLabel), new AggregateValidationStatus(context,
                        AggregateValidationStatus.MAX_SEVERITY));
        context.bindValue(PojoObservables.observeValue(listener, "status"), new AggregateValidationStatus(context, //$NON-NLS-1$
                        AggregateValidationStatus.MAX_SEVERITY));

        return composite;
    }

    protected abstract void createFormElements(Composite editArea);

    protected final void createLabel(Composite editArea, String text)
    {
        Label lblTransactionType = new Label(editArea, SWT.NONE);
        lblTransactionType.setText(text);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(lblTransactionType);
    }

    protected final void bindComboViewer(Composite editArea, String label, String property,
                    IBaseLabelProvider labelProvider, Object input)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(labelProvider);
        combo.setInput(input);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(combo.getControl());

        context.bindValue(ViewersObservables.observeSingleSelection(combo), //
                        BeansObservables.observeValue(model, property));
    }

    protected final void bindDatePicker(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        DateTime boxDate = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(boxDate);

        context.bindValue(new SimpleDateTimeSelectionProperty().observe(boxDate),
                        BeansObservables.observeValue(model, property));
    }

    protected final void bindPriceInput(Composite editArea, String label, String property)
    {
        Text txtValue = createPriceInput(editArea, label);

        context.bindValue(
                        SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setConverter(new StringToCurrencyConverter()),
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter()));
    }

    protected final void bindMandatoryPriceInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createPriceInput(editArea, label);

        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy() //
                                        .setConverter(new StringToCurrencyConverter()) //
                                        .setAfterConvertValidator(new IValidator()
                                        {
                                            @Override
                                            public IStatus validate(Object value)
                                            {
                                                Integer v = (Integer) value;
                                                return v != null && v.intValue() > 0 ? ValidationStatus.ok()
                                                                : ValidationStatus.error(MessageFormat.format(
                                                                                Messages.MsgDialogInputRequired, label));
                                            }
                                        }), // ,
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter()));
    }

    private Text createPriceInput(Composite editArea, final String label)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        Text txtValue = new Text(editArea, SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(txtValue);
        return txtValue;
    }

    protected final void bindMandatoryIntegerInput(Composite editArea, final String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        Text txtValue = new Text(editArea, SWT.BORDER);
        txtValue.setFocus();
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(txtValue);

        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                Integer v = (Integer) value;
                                return v != null && v.intValue() > 0 ? ValidationStatus.ok() : ValidationStatus
                                                .error(MessageFormat.format(Messages.MsgDialogInputRequired, label));
                            }
                        }), //
                        null);
    }

    @Override
    protected void okPressed()
    {
        model.createChanges();
        super.okPressed();
    }

}

package name.abuchen.portfolio.ui.wizards.events;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
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
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class AddCustomEventPage extends AbstractWizardPage
{
    private CustomEventModel model;
    private BindingHelper bindings;

    public AddCustomEventPage(CustomEventModel model)
    {
        super("add-custom-event"); //$NON-NLS-1$

        setTitle("Add Custom Event"); // Messages.SplitWizardDefinitionTitle);
        setDescription("enter a user-defined free text"); // Messages.SplitWizardDefinitionDescription);

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

        Label labelMessage = new Label(container, SWT.NONE);
        labelMessage.setText(Messages.ColumnEventMessage);

        Text text = new Text(container, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        text.setText("custom message"); //$NON-NLS-1$

        // form layout data

        int labelWidth = widest(labelSecurity, labelExDate);

        startingWith(comboSecurity.getControl(), labelSecurity) //
                        .thenBelow(boxExDate.getControl()).label(labelExDate) //
                        .thenBelow(text).label(labelMessage);

        startingWith(labelSecurity).width(labelWidth);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        IObservableValue<?> targetObservable = ViewersObservables.observeSingleSelection(comboSecurity);
        @SuppressWarnings("unchecked")
        IObservableValue<?> securityObservable = BeanProperties.value("security").observe(model); //$NON-NLS-1$
        context.bindValue(targetObservable, securityObservable, null, null);

        @SuppressWarnings("unchecked")
        IObservableValue<Object> targetExDate = new SimpleDateTimeDateSelectionProperty()
                        .observe(boxExDate.getControl());
        @SuppressWarnings("unchecked")
        IObservableValue<Object> modelExDate = BeanProperties.value("exDate").observe(model); //$NON-NLS-1$
        context.bindValue(targetExDate, modelExDate, new UpdateValueStrategy<Object, Object>() //
                        .setAfterConvertValidator(value -> value != null ? ValidationStatus.ok()
                                        : ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired,
                                                        Messages.ColumnExDate))),
                        null);

        IObservableValue<?> messageTargetObservable = WidgetProperties.text(SWT.Modify).observe(text);
        @SuppressWarnings("unchecked")
        IObservableValue<?> messageModelObservable = BeanProperties.value("message").observe(model); //$NON-NLS-1$
        context.bindValue(messageTargetObservable, messageModelObservable);
    }
}

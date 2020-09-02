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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
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

        setTitle(Messages.EventWizardTitle);
        setDescription(Messages.EventWizardDescription);

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
        labelSecurity.setLayoutData(new FormData());
        labelSecurity.setText(Messages.ColumnSecurity);

        List<Security> securities = model.getClient().getActiveSecurities();
        if (model.getSecurity() != null && !securities.contains(model.getSecurity()))
            securities.add(0, model.getSecurity());

        ComboViewer comboSecurity = new ComboViewer(container, SWT.READ_ONLY);
        Combo combo = comboSecurity.getCombo();
        combo.setLayoutData(new FormData());
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

        Label labelDate = new Label(container, SWT.NONE);
        labelDate.setLayoutData(new FormData());
        labelDate.setText(Messages.ColumnDate);

        DatePicker boxDate = new DatePicker(container);

        Label labelMessage = new Label(container, SWT.NONE);
        labelMessage.setLayoutData(new FormData());
        labelMessage.setText(Messages.EventWizardLabelMessage);

        Text text = new Text(container, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        FormData fd_text = new FormData();
        fd_text.right = new FormAttachment(0, 480);
        text.setLayoutData(fd_text);
        text.setText("custom message"); //$NON-NLS-1$

        // form layout data

        int labelWidth = widest(labelSecurity, labelDate);

        startingWith(comboSecurity.getControl(), labelSecurity) //
                        .thenBelow(boxDate.getControl()).label(labelDate) //
                        .thenBelow(text).label(labelMessage);

        startingWith(labelSecurity).width(labelWidth);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        IObservableValue<?> targetObservable = ViewersObservables.observeSingleSelection(comboSecurity);
        @SuppressWarnings("unchecked")
        IObservableValue<?> securityObservable = BeanProperties.value("security").observe(model); //$NON-NLS-1$
        context.bindValue(targetObservable, securityObservable, null, null);

        @SuppressWarnings("unchecked")
        IObservableValue<Object> targetExDate = new SimpleDateTimeDateSelectionProperty().observe(boxDate.getControl());
        @SuppressWarnings("unchecked")
        IObservableValue<Object> modelExDate = BeanProperties.value("date").observe(model); //$NON-NLS-1$
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

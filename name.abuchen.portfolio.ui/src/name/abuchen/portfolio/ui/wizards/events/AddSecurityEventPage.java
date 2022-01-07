package name.abuchen.portfolio.ui.wizards.events;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.Type;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class AddSecurityEventPage extends AbstractWizardPage
{
    private SecurityEventModel model;
    private BindingHelper bindings;

    public AddSecurityEventPage(SecurityEventModel model)
    {
        super("add-security-event"); //$NON-NLS-1$

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
        
        setHeader();
    }

    private void setHeader()
    {
        
        String title;
        String description;
        
        Type type = model.getType();
        switch (type)
        {
            case DIVIDEND_DECLARATION:
                title = Messages.EventWizardTitleDividendDeclaration;
                description = Messages.EventWizardDescriptionDividendDeclaration;
                break;
                
            case DIVIDEND_RECORD:
                title = Messages.EventWizardTitleDividendRecord;
                description = Messages.EventWizardDescriptionDividendRecord;
                break;
                
            case EARNINGS_REPORT:
                title = Messages.EventWizardTitleEarningsReport;
                description = Messages.EventWizardDescriptionEarningsReport;
                break;
                
            case EX_DIVIDEND:
                title = Messages.EventWizardTitleExDividend;
                description = Messages.EventWizardDescriptionExDividend;
                break;
                
            case NOTE:
                title = Messages.EventWizardTitleNote;
                description = Messages.EventWizardDescriptionNote;
                break;
                
            case PAYDAY:
                title = Messages.EventWizardTitlePayday;
                description = Messages.EventWizardDescriptionPayday;
                break;
                
            case SHAREHOLDER_MEETING:
                title = Messages.EventWizardTitleShareholderMeeting;
                description = Messages.EventWizardDescriptionShareholderMeeting;
                break;
                
            default:
                throw new UnsupportedOperationException("unsupported event type: " + type); //$NON-NLS-1$
        }
        
        setTitle(title);
        setDescription(description);
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

        // form layout data

        int labelWidth = widest(labelSecurity, labelDate);

        startingWith(comboSecurity.getControl(), labelSecurity) //
                        .thenBelow(boxDate.getControl()).label(labelDate);

        startingWith(labelSecurity).width(labelWidth);

        // model binding

        DataBindingContext context = bindings.getBindingContext();
        IObservableValue<?> targetObservable = ViewerProperties.singleSelection().observe(comboSecurity);
        IObservableValue<?> securityObservable = BeanProperties.value("security").observe(model); //$NON-NLS-1$
        context.bindValue(targetObservable, securityObservable, null, null);

        IObservableValue<LocalDate> targetExDate = new SimpleDateTimeDateSelectionProperty()
                        .observe(boxDate.getControl());
        IObservableValue<LocalDate> modelExDate = BeanProperties.value("date", LocalDate.class).observe(model); //$NON-NLS-1$
        context.bindValue(targetExDate, modelExDate, new UpdateValueStrategy<LocalDate, LocalDate>() //
                        .setAfterConvertValidator(value -> value != null ? ValidationStatus.ok()
                                        : ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired,
                                                        Messages.ColumnExDate))),
                        null);
    }
}

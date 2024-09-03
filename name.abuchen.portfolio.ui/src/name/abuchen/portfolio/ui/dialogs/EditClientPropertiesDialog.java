package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.text.FrenchKeypadSupport;

public class EditClientPropertiesDialog extends AbstractDialog
{
    private final class DoubleToStringConverter implements IConverter<Double, String>
    {
        @Override
        public Object getFromType()
        {
            return Double.class;
        }

        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public String convert(Double fromObject)
        {
            return new PercentConverter().toString(fromObject);
        }
    }

    private final class StringToDoubleConverter implements IConverter<String, Double>
    {
        @Override
        public Object getFromType()
        {
            return String.class;
        }

        @Override
        public Object getToType()
        {
            return Double.class;
        }

        @Override
        public Double convert(String fromObject)
        {
            return (Double) new PercentConverter().fromString(fromObject);
        }
    }

    public static class SettingsModel extends BindingHelper.Model
    {
        private final ClientProperties clientProperties;
        private double riskFreeRateOfReturn;

        public SettingsModel(ClientProperties clientProperties)
        {
            super();

            this.clientProperties = clientProperties;
            this.riskFreeRateOfReturn = clientProperties.getRiskFreeRateOfReturn();
        }

        public double getRiskFreeRateOfReturn()
        {
            return riskFreeRateOfReturn;
        }

        public void setRiskFreeRateOfReturn(double riskFreeRateOfReturn)
        {
            firePropertyChange("riskFreeRateOfReturn", this.riskFreeRateOfReturn, //$NON-NLS-1$
                            this.riskFreeRateOfReturn = riskFreeRateOfReturn); // NOSONAR
        }

        @Override
        public void applyChanges()
        {
            clientProperties.setRiskFreeRateOfReturn(riskFreeRateOfReturn);
        }
    }

    public EditClientPropertiesDialog(Shell parentShell, ClientProperties clientProperties)
    {
        super(parentShell, Messages.LabelSettings, new SettingsModel(clientProperties));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        var label = Messages.SharpeRatioRisklessIRR;

        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Text txtValue = new Text(editArea, SWT.BORDER);
        FrenchKeypadSupport.configure(txtValue);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(txtValue);

        UpdateValueStrategy<String, Double> input2model = new UpdateValueStrategy<>();
        input2model.setConverter(new StringToDoubleConverter());
        input2model.setAfterConvertValidator(v -> v != null ? ValidationStatus.ok()
                        : ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, label)));

        IObservableValue<String> targetObservable = WidgetProperties.text(SWT.Modify).observe(txtValue);
        IObservableValue<Double> modelObservable = BeanProperties.value("riskFreeRateOfReturn", Double.class) //$NON-NLS-1$
                        .observe(getModel());

        bindings().getBindingContext().bindValue(targetObservable, modelObservable, input2model,
                        new UpdateValueStrategy<Double, String>().setConverter(new DoubleToStringConverter()));

    }
}

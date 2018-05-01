package name.abuchen.portfolio.ui.dialogs;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper.Model;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class PasswordDialog extends AbstractDialog
{
    public static class PasswordModel extends Model
    {
        private String password;
        private String repeat;

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            firePropertyChange("password", this.password, this.password = password); //$NON-NLS-1$
        }

        public String getRepeat()
        {
            return repeat;
        }

        public void setRepeat(String repeat)
        {
            firePropertyChange("repeat", this.repeat, this.repeat = repeat); //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {}
    }

    public PasswordDialog(Shell parentShell)
    {
        super(parentShell, Messages.TitlePasswordDialog, new PasswordModel());
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        final IObservableValue<String> password = bindings().bindStringInput(editArea, Messages.LabelPassword,
                        "password", SWT.PASSWORD); //$NON-NLS-1$
        final IObservableValue<String> repeat = bindings().bindStringInput(editArea, Messages.LabelPasswordRepeat,
                        "repeat", SWT.PASSWORD); //$NON-NLS-1$

        // multi validator (passwords must be identical)
        MultiValidator validator = new MultiValidator()
        {

            @Override
            protected IStatus validate()
            {
                String pwd = password.getValue();
                String rpt = repeat.getValue();

                if (pwd.length() < 6)
                    return ValidationStatus.error(Messages.MsgPasswordMinCharacters);

                return pwd.equals(rpt) ? ValidationStatus.ok()
                                : ValidationStatus.error(Messages.MsgPasswordNotIdentical);
            }

        };
        bindings().getBindingContext().addValidationStatusProvider(validator);
    }

    public String getPassword()
    {
        return ((PasswordModel) getModel()).getPassword();
    }
}

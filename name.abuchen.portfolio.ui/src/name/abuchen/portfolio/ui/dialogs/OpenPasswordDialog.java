package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper.Model;

public class OpenPasswordDialog extends AbstractDialog
{
    public static class OpenPasswordModel extends Model
    {
        private String password;

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            firePropertyChange("password", this.password, this.password = password); //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {
            // nichts zu persistieren
        }
    }

    private final String fileName;

    public OpenPasswordDialog(Shell parentShell, String fileName)
    {
        super(parentShell, buildTitle(fileName), new OpenPasswordModel());
        this.fileName = fileName;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        bindings().bindStringInput(editArea, Messages.LabelPassword, "password", SWT.PASSWORD); //$NON-NLS-1$
    }

    public String getPassword()
    {
        return ((OpenPasswordModel) getModel()).getPassword();
    }

    public String getFileName()
    {
        return fileName;
    }

    private static String buildTitle(String fileName)
    {
        if (fileName == null || fileName.trim().isEmpty())
            return Messages.LabelPassword;

        return Messages.LabelPassword + " - " + fileName; //$NON-NLS-1$
    }
}
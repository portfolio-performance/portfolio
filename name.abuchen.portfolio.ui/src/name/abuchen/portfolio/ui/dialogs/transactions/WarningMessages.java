package name.abuchen.portfolio.ui.dialogs.transactions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;

/**
 * Utility class to run multiple checks and set the warning message to the
 * TitleAreaDialog accordingly.
 */
public class WarningMessages
{
    private TitleAreaDialog dialog;
    private List<Supplier<String>> checks = new ArrayList<>();

    public WarningMessages(TitleAreaDialog dialog)
    {
        this.dialog = dialog;
    }

    public WarningMessages add(Supplier<String> check)
    {
        this.checks.add(check);
        return this;
    }

    public void check()
    {
        List<String> messages = new ArrayList<>();
        for (Supplier<String> check : checks)
        {
            String message = check.get();
            if (message != null)
                messages.add(message);
        }

        if (messages.size() > 0)
        {
            dialog.setMessage(String.join("\n", messages), IMessageProvider.WARNING); //$NON-NLS-1$
        }
        else
        {
            dialog.setMessage(null);
        }
    }
}

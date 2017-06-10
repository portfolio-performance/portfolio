package name.abuchen.portfolio.ui.update;

import org.eclipse.core.runtime.IStatus;

public class UpdateConflictException extends Exception
{
    private static final long serialVersionUID = 9213953180557225361L;
    private IStatus status;

    public UpdateConflictException(final IStatus status)
    {
        this.status = status;
    }

    public IStatus getStatus()
    {
        return status;
    }
}

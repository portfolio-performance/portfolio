package name.abuchen.portfolio.ui;

import name.abuchen.portfolio.model.Client;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;

public class ClientEditorInput extends PlatformObject implements IPathEditorInput, IPersistableElement
{
    private IPath path;
    private Client client;

    public ClientEditorInput(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    public ClientEditorInput(IPath path)
    {
        this.path = path;
    }

    @Override
    public boolean exists()
    {
        return path != null && path.toFile().exists();
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return path != null ? path.toOSString() : Messages.LabelPortfolioPerformanceFile;
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return path != null ? this : null;
    }

    @Override
    public String getToolTipText()
    {
        return getName();
    }

    @Override
    public IPath getPath()
    {
        return path;
    }

    @Override
    public void saveState(IMemento memento)
    {
        if (path != null)
            memento.putString("file", path.toOSString()); //$NON-NLS-1$
    }

    @Override
    public String getFactoryId()
    {
        return "name.abuchen.portfolio.ui.factory"; //$NON-NLS-1$
    }
}

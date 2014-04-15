package name.abuchen.portfolio.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class ElementFactory implements IElementFactory
{

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        String fileOSString = memento.getString("file"); //$NON-NLS-1$
        return new ClientEditorInput(new Path(fileOSString));
    }

}

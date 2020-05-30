package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

public class SimpleAction extends Action
{
    @FunctionalInterface
    public interface Runnable
    {
        void run(Action action);
    }

    private final Runnable runnable;

    public SimpleAction(String text, int style, Runnable runnable)
    {
        super(text, style);
        this.runnable = runnable;
    }

    public SimpleAction(String text, Runnable runnable)
    {
        super(text);
        this.runnable = runnable;
    }

    public SimpleAction(String text, ImageDescriptor imageDescriptor, Runnable runnable)
    {
        super(text, imageDescriptor);
        this.runnable = runnable;
    }

    public SimpleAction(Runnable runnable)
    {
        super();
        this.runnable = runnable;
    }

    @Override
    public void run()
    {
        runnable.run(this);
    }

}

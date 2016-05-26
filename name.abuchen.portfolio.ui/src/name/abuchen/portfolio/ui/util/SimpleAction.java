package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;

public class SimpleAction extends Action
{
    @FunctionalInterface
    public interface Runnable
    {
        void run(Action action);
    }

    private final Runnable runnable;

    public SimpleAction(String text, Runnable runnable)
    {
        super(text);
        this.runnable = runnable;
    }

    @Override
    public void run()
    {
        runnable.run(this);
    }

}

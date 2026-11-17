package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Evaluate;

/**
 * Shows the menu item to export the Markdown report for AI assistants only if
 * the experimental feature is enabled.
 */
public class ExportMarkdownEnabledExpression
{
    @Evaluate
    public boolean evaluate()
    {
        return ExportMarkdownHandler.isEnabled();
    }
}

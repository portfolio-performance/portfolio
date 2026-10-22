package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Evaluate;

/**
 * Shows the menu item to import text files extracted from PDF documents only
 * if the experimental feature is enabled.
 */
public class ImportPDFTextEnabledExpression
{
    @Evaluate
    public boolean evaluate()
    {
        return ImportPDFTextHandler.isEnabled();
    }
}

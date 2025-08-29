package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.TaxonomyJSONExporter;
import name.abuchen.portfolio.ui.util.JSONExporterDialog;

public class ExportTaxonomiesHandler
{

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, IStylingEngine stylingEngine)
    {
        MenuHelper.getActiveClientInput(part).ifPresent(input -> new JSONExporterDialog(shell,
                        new TaxonomyJSONExporter.AllTaxonomies(input.getClient())).export());
    }
}

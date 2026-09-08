package name.abuchen.portfolio.ui.update;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class ShowAvailableUpdateHandler
{
    @Execute
    public void execute(@Optional AvailableUpdate availableUpdate, MApplication application,
                    EModelService modelService)
    {
        if (availableUpdate == null)
            return;

        var outcome = availableUpdate.promptAndInstallLatest();

        if (outcome == UpdateHelper.Outcome.INSTALLED || outcome == UpdateHelper.Outcome.NOTHING_TO_UPDATE)
            UpdateIndicatorAddon.hideIndicator(modelService, application);
    }
}

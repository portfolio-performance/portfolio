package name.abuchen.portfolio.ui.survey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.WebAccess;

public class SurveyAddon
{
    private static final String SURVEY_TAG = "SURVEY"; //$NON-NLS-1$

    @Inject
    private EModelService modelService;

    @Inject
    private ECommandService commandService;

    @Inject
    private MApplication application;

    @Inject
    @Preference
    IEclipsePreferences preferences;

    @PostConstruct
    public void setupPreferences()
    {
        Survey.setActive(preferences.getBoolean(UIConstants.Preferences.ENABLE_SURVEY_REMINDER, false));
    }

    @PostConstruct
    public void setupPartToolbars(IEventBroker eventBroker)
    {
        // dynamically add the tool bar to the model whenever a new portfolio
        // part is created. The tool bar item must not be persisted with the
        // model in order to be able to remove it once the survey is over

        eventBroker.subscribe(UIEvents.UIElement.TOPIC_WIDGET, event -> {
            Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
            if (!(element instanceof MPart mPart))
                return;

            if (!(mPart.getElementId().equals(UIConstants.Part.PORTFOLIO)))
                return;

            if (!mPart.isToBeRendered())
                return;

            createToolbarAction(mPart);
        });
    }

    @PostConstruct
    public void setupJob()
    {
        new Job(SURVEY_TAG)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                boolean isActive = checkIfSurveyIsActive();

                if (isActive != Survey.isActive())
                {
                    updateStatusAndUI(isActive);
                }

                schedule(Duration.ofHours(24).toMillis());

                return Status.OK_STATUS;
            }

        }.schedule(10000);
    }

    private boolean checkIfSurveyIsActive()
    {
        try
        {
            new WebAccess("https://www.portfolio-performance.info/survey_active").get(); //$NON-NLS-1$
            return true;
        }
        catch (IOException | URISyntaxException e)
        {
            return false;
        }
    }

    protected void updateStatusAndUI(boolean isActive)
    {
        Display.getDefault().asyncExec(() -> {

            Survey.setActive(isActive);
            preferences.putBoolean(UIConstants.Preferences.ENABLE_SURVEY_REMINDER, Survey.isActive());

            modelService.findElements(application, UIConstants.PartStack.MAIN, MPartStack.class).forEach(partStack -> {
                CTabFolder folder = (CTabFolder) partStack.getWidget();
                int height = folder.getTabHeight();
                if (height < 22)
                    folder.setTabHeight(22);
            });

            modelService.findElements(application, MHandledToolItem.class, EModelService.IN_PART,
                            e -> e.getTags().contains(SURVEY_TAG)) //
                            .forEach(item -> item.setVisible(Survey.isActive()));
        });
    }

    private void createToolbarAction(MPart mPart)
    {
        // create tool bar
        final MToolBar mBar = modelService.createModelElement(MToolBar.class);
        mBar.getPersistedState().put("persistState", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        mPart.setToolbar(mBar);

        // create command
        MParameter parameter = modelService.createModelElement(MParameter.class);
        parameter.setName(UIConstants.Parameter.URL);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIConstants.Parameter.URL,
                        "https://www.portfolio-performance.info/survey?lang=" + Locale.getDefault().getLanguage()); //$NON-NLS-1$
        ParameterizedCommand command = commandService.createCommand(UIConstants.Command.OPEN_BROWSER, parameters);

        // create tool item
        final MHandledToolItem mItem = modelService.createModelElement(MHandledToolItem.class);
        mBar.getChildren().add(mItem);

        mItem.setToBeRendered(true);
        mItem.setVisible(Survey.isActive());
        mItem.setIconURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID + "/icons/" + Messages.SurveyIcon); //$NON-NLS-1$ //$NON-NLS-2$
        mItem.setTooltip(Messages.SurveyTooltip);
        mItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        mItem.getParameters().add(parameter);
        mItem.setWbCommand(command);
        mItem.setCommand(application.getCommand(UIConstants.Command.OPEN_BROWSER));
        mItem.getTags().add(SURVEY_TAG);
    }

}

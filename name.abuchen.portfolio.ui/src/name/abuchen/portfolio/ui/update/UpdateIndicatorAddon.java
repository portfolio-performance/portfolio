package name.abuchen.portfolio.ui.update;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.service.event.Event;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.preferences.Experiments;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Checks for updates in the background and, instead of interrupting the user
 * with a dialog, shows an unobtrusive button in the top right corner of the tab
 * folder. The version history is shown only if the user clicks that button.
 */
public class UpdateIndicatorAddon
{
    private static final String TAG_UPDATE_AVAILABLE = "UPDATE_AVAILABLE"; //$NON-NLS-1$

    private static final String ITEM_ID = "name.abuchen.portfolio.ui.toolitem.updateAvailable"; //$NON-NLS-1$

    private static final long INITIAL_CHECK_DELAY = Duration.ofSeconds(3).toMillis();

    private static final long CHECK_INTERVAL = Duration.ofHours(24).toMillis();

    @Inject
    private EModelService modelService;

    @Inject
    private MApplication application;

    private volatile boolean isDisposed = false;

    private Job checkForUpdatesJob;

    @PostConstruct
    public void setupPartToolbars(IEventBroker eventBroker)
    {
        if (!isIndicatorEnabled())
            return;

        // dynamically add the tool bar to the model whenever a welcome or
        // portfolio part is created. The tab folder shows the tool bar of the
        // active part only, therefore every part that can be on top needs its
        // own item. The item must not be persisted with the model because it
        // exists only as long as an update is pending.

        eventBroker.subscribe(UIEvents.UIElement.TOPIC_WIDGET, event -> {
            var element = event.getProperty(UIEvents.EventTags.ELEMENT);

            if (element instanceof MDirectToolItem mItem && mItem.getTags().contains(TAG_UPDATE_AVAILABLE))
            {
                // color the button as soon as its widget has been created
                styleToolItem(mItem);
                return;
            }

            if (!(element instanceof MPart mPart))
                return;

            var elementId = mPart.getElementId();
            if (!UIConstants.Part.PORTFOLIO.equals(elementId) && !UIConstants.Part.WELCOME.equals(elementId))
                return;

            if (!mPart.isToBeRendered())
                return;

            createToolbarItem(mPart);
        });
    }

    @Inject
    @Optional
    public void checkForUpdates(@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) Event event, // NOSONAR
                    @Preference(value = UIConstants.Preferences.AUTO_UPDATE) boolean autoUpdate)
    {
        if (!autoUpdate || !UpdateHelper.isInAppUpdateEnabled())
            return;

        var showIndicator = isIndicatorEnabled();

        checkForUpdatesJob = new Job(Messages.JobMsgCheckingForUpdates)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                // check again later unless an update has been found: the
                // application is often kept running for weeks, and without the
                // indicator there would be no way to learn about a new version
                // short of restarting

                var checkAgainLater = true;

                try
                {
                    monitor.beginTask(Messages.JobMsgCheckingForUpdates, 100);

                    var updateHelper = new UpdateHelper();
                    var newVersion = updateHelper.findUpdates(monitor);

                    if (newVersion != null)
                    {
                        checkAgainLater = false;

                        if (showIndicator)
                            showIndicator(new AvailableUpdate(updateHelper, newVersion));
                        else
                            updateHelper.promptAndInstall(newVersion);
                    }
                }
                catch (CoreException e) // NOSONAR
                {
                    // for example because the machine is offline right now
                    PortfolioPlugin.log(e.getStatus());
                }

                // only the indicator can report a new version without getting
                // in the way. Without it, keep the previous behavior and check
                // once after the start.

                if (checkAgainLater && showIndicator && !isDisposed)
                    schedule(CHECK_INTERVAL);

                return Status.OK_STATUS;
            }
        };

        checkForUpdatesJob.setSystem(true);
        checkForUpdatesJob.schedule(INITIAL_CHECK_DELAY);
    }

    @PreDestroy
    public void dispose()
    {
        // the update check runs in the background and can complete while the
        // application is already shutting down

        isDisposed = true;

        if (checkForUpdatesJob != null)
            checkForUpdatesJob.cancel();
    }

    private void showIndicator(AvailableUpdate update)
    {
        var display = Display.getDefault();
        if (isDisposed || display.isDisposed())
            return;

        display.asyncExec(() -> {
            if (isDisposed)
                return;

            application.getContext().set(AvailableUpdate.class, update);

            // The tool bar is created with SWT.WRAP, i.e. it puts the item into
            // a second row while it is still negotiating its width. Show the
            // button and correct the tab height within one turn of the event
            // loop and with redraw switched off, otherwise that intermediate
            // state is painted - on Linux it remains visible noticeably long.

            var folders = getTabFolders();
            folders.forEach(folder -> folder.setRedraw(false));

            try
            {
                findIndicatorItems(modelService, application).forEach(item -> {
                    item.setVisible(true);
                    styleToolItem(item);
                });

                folders.forEach(this::adjustTabHeight);
            }
            finally
            {
                folders.forEach(folder -> folder.setRedraw(true));
            }
        });
    }

    /**
     * Drops the pending update and hides the button because there is nothing
     * left to install: either the update has been installed - and the
     * application must be restarted - or the update site does not offer an
     * update anymore.
     */
    /* package */ static void hideIndicator(EModelService modelService, MApplication application)
    {
        application.getContext().remove(AvailableUpdate.class);

        findIndicatorItems(modelService, application).forEach(item -> item.setVisible(false));
    }

    private static List<MDirectToolItem> findIndicatorItems(EModelService modelService, MApplication application)
    {
        return modelService.findElements(application, MDirectToolItem.class, EModelService.IN_PART,
                        item -> item.getTags().contains(TAG_UPDATE_AVAILABLE));
    }

    private void createToolbarItem(MPart mPart)
    {
        var mBar = mPart.getToolbar();

        if (mBar == null)
        {
            mBar = modelService.createModelElement(MToolBar.class);
            mBar.getPersistedState().put(IWorkbench.PERSIST_STATE, Boolean.FALSE.toString());
            mPart.setToolbar(mBar);
        }
        else if (mBar.getChildren().stream().anyMatch(item -> ITEM_ID.equals(item.getElementId())))
        {
            // the widget event can be sent more than once for the same part
            return;
        }

        var mItem = modelService.createModelElement(MDirectToolItem.class);
        mItem.setElementId(ITEM_ID);
        mItem.setLabel(Messages.LabelUpdateAvailableShort);
        mItem.setTooltip(Messages.LabelUpdatesAvailable);
        mItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        mItem.setContributionURI("bundleclass://" + PortfolioPlugin.PLUGIN_ID + "/" //$NON-NLS-1$ //$NON-NLS-2$
                        + ShowAvailableUpdateHandler.class.getName());
        mItem.getTags().add(TAG_UPDATE_AVAILABLE);
        mItem.setToBeRendered(true);

        var hasUpdate = hasAvailableUpdate();
        mItem.setVisible(hasUpdate);

        mBar.getChildren().add(mItem);

        if (hasUpdate)
        {
            // the update was found before this part was created
            Display.getDefault().asyncExec(() -> getTabFolders().forEach(this::adjustTabHeight));
        }
    }

    private void styleToolItem(MDirectToolItem mItem)
    {
        if (!(mItem.getWidget() instanceof ToolItem toolItem) || toolItem.isDisposed())
            return;

        var background = Colors.theme().warningBackground();
        toolItem.setBackground(background);
        toolItem.setForeground(Colors.getTextColor(background));

        // the renderer decides whether it shows the label of a tool item. As
        // the button has no icon, it would be invisible without the text -
        // therefore set it should the renderer ever decide otherwise.

        if (toolItem.getText().isEmpty())
            toolItem.setText(Messages.LabelUpdateAvailableShort);
    }

    private boolean isIndicatorEnabled()
    {
        return new Experiments().isEnabled(Experiments.Feature.AUG26_UNOBTRUSIVE_UPDATE_NOTIFICATION);
    }

    /**
     * The pending update is kept in the application context: the handler
     * removes it once there is nothing left to install, and parts created after
     * that must not show the button anymore.
     */
    private boolean hasAvailableUpdate()
    {
        return application.getContext().get(AvailableUpdate.class) != null;
    }

    private List<CTabFolder> getTabFolders()
    {
        if (isDisposed)
            return Collections.emptyList();

        return modelService.findElements(application, UIConstants.PartStack.MAIN, MPartStack.class).stream()
                        .map(MPartStack::getWidget) //
                        .filter(CTabFolder.class::isInstance) //
                        .map(CTabFolder.class::cast) //
                        .filter(folder -> !folder.isDisposed()) //
                        .toList();
    }

    private void adjustTabHeight(CTabFolder folder)
    {
        // the tab folder derives its tab height from the tab labels alone. The
        // tool bar in the top right corner is therefore clipped if it needs
        // more space. Grow - but never shrink - the tab height to whatever the
        // tool bar requires. The height must be measured on the widget because
        // it depends on the font size and the platform.

        if (folder.isDisposed())
            return;

        var topRight = folder.getTopRight();
        if (topRight == null || topRight.isDisposed())
            return;

        int required = topRight.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        if (required > folder.getTabHeight())
        {
            folder.setTabHeight(required);
            folder.layout(true, true);
        }
    }
}

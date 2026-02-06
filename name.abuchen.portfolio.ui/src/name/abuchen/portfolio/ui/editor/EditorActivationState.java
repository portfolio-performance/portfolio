package name.abuchen.portfolio.ui.editor;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationListener;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;

import name.abuchen.portfolio.ui.preferences.Experiments;

/**
 * Keeps track if an in-place editor is currently active and if an editor is
 * active, runs the Runnable only after the editor is deactivated.
 * <p/>
 * Background: when updating the prices, the views are updated every 250 or so
 * ms. That results in an call to recalculate the tables which deactivates the
 * editor. Practically, it is not possible use the editor in this case.
 */
@Creatable
@Singleton
public class EditorActivationState
{
    @Inject
    private Experiments experiments;

    private boolean isFeatureEnabled;

    private boolean isEditorActive = false;
    private Runnable onEditorDeactivated;

    public EditorActivationState()
    {
        // default constructor
    }

    @PostConstruct
    void init()
    {
        this.isFeatureEnabled = experiments.isEnabled(Experiments.Feature.JULY26_PREVENT_UPDATE_WHILE_EDITING_CELLS);
    }

    private void activateEditor()
    {
        isEditorActive = true;
    }

    private void deactivateEditor()
    {
        if (isEditorActive && onEditorDeactivated != null)
        {
            onEditorDeactivated.run();
            onEditorDeactivated = null;
        }

        isEditorActive = false;
    }

    public void deferUntilNotEditing(Runnable runnable)
    {
        if (!isFeatureEnabled)
            runnable.run();

        if (!isEditorActive)
        {
            runnable.run();
        }
        else
        {
            this.onEditorDeactivated = runnable;
        }
    }

    public ColumnViewerEditorActivationListener createListener()
    {
        return new ColumnViewerEditorActivationListener()
        {
            @Override
            public void beforeEditorActivated(ColumnViewerEditorActivationEvent event)
            {
                activateEditor();
            }

            @Override
            public void afterEditorActivated(ColumnViewerEditorActivationEvent event)
            {
                // not needed
            }

            @Override
            public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event)
            {
                // not needed
            }

            @Override
            public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event)
            {
                deactivateEditor();
            }
        };
    }
}

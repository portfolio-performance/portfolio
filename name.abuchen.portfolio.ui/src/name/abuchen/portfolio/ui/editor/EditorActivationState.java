package name.abuchen.portfolio.ui.editor;

import org.eclipse.jface.viewers.ColumnViewer;
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
public class EditorActivationState
{
    /**
     * The viewer whose in-place editor is currently active, if any. At most one
     * in-place editor can be active across the application because activating an
     * editor requires focus and SWT gives focus to a single control.
     * <p/>
     * Deliberately not a boolean: {@link #isAnyEditorActive()} asks the viewer
     * instead of trusting this reference, so a missed deactivation callback
     * cannot leave the application permanently "editing" - which would block all
     * REST API writes with 423 for the rest of the session.
     * <p/>
     * Note that this is read by the REST API write gate regardless of the
     * {@link Experiments.Feature#JULY26_PREVENT_UPDATE_WHILE_EDITING_CELLS}
     * experiment, which gates only the price-update deferral below.
     */
    private static ColumnViewer activeViewer;

    /**
     * Whether the user is currently editing a table cell anywhere in the
     * application. Must be called on the UI thread.
     */
    public static boolean isAnyEditorActive()
    {
        if (activeViewer == null)
            return false;

        var control = activeViewer.getControl();
        if (control == null || control.isDisposed() || !activeViewer.isCellEditorActive())
        {
            activeViewer = null;
            return false;
        }

        return true;
    }

    private final boolean isFeatureEnabled = new Experiments()
                    .isEnabled(Experiments.Feature.JULY26_PREVENT_UPDATE_WHILE_EDITING_CELLS);

    private boolean isEditorActive = false;
    private Runnable onEditorDeactivated;

    private void activateEditor(ColumnViewer viewer)
    {
        isEditorActive = true;
        activeViewer = viewer;
    }

    private void deactivateEditor(ColumnViewer viewer)
    {
        if (isEditorActive && onEditorDeactivated != null)
        {
            onEditorDeactivated.run();
            onEditorDeactivated = null;
        }

        isEditorActive = false;

        // only the viewer that activated the editor may clear the reference
        if (activeViewer == viewer)
            activeViewer = null;
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

    public ColumnViewerEditorActivationListener createListener(ColumnViewer viewer)
    {
        return new ColumnViewerEditorActivationListener()
        {
            @Override
            public void beforeEditorActivated(ColumnViewerEditorActivationEvent event)
            {
                activateEditor(viewer);
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
                deactivateEditor(viewer);
            }
        };
    }
}

package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.EditorActivationState;

public abstract class ColumnEditingSupport
{
    @FunctionalInterface
    public interface ModificationListener
    {
        void onModified(Object element, Object newValue, Object oldValue);
    }

    public static class TouchClientListener implements ModificationListener
    {
        private final Client client;

        public TouchClientListener(Client client)
        {
            this.client = client;
        }

        @Override
        public void onModified(Object element, Object newValue, Object oldValue)
        {
            client.touch();
        }
    }

    public static class MarkDirtyClientListener implements ModificationListener
    {
        private final Client client;

        public MarkDirtyClientListener(Client client)
        {
            this.client = client;
        }

        @Override
        public void onModified(Object element, Object newValue, Object oldValue)
        {
            client.markDirty();
        }
    }

    private List<ModificationListener> listeners;

    public CellEditor createEditor(Composite composite)
    {
        return new TextCellEditor(composite);
    }

    public boolean canEdit(Object element)
    {
        return true;
    }

    /**
     * Called before the editor for the given element is made visible
     */
    public void prepareEditor(Object element)
    {
    }

    public abstract Object getValue(Object element) throws Exception;

    public abstract void setValue(Object element, Object value) throws Exception;

    public final ColumnEditingSupport addListener(ModificationListener listener)
    {
        if (listeners == null)
            listeners = new ArrayList<>();
        listeners.add(listener);
        return this;
    }

    public final void attachTo(Column column)
    {
        column.setEditingSupport(this);
    }

    protected final void notify(Object element, Object newValue, Object oldValue)
    {
        if (listeners != null)
        {
            for (ModificationListener listener : listeners)
                listener.onModified(element, newValue, oldValue);
        }
    }

    public static void prepare(ColumnViewer viewer)
    {
        prepare(null, viewer);
    }

    public static void prepare(EditorActivationState state, ColumnViewer viewer)
    {
        ColumnViewerEditorActivationStrategy activationStrategy = new ColumnViewerEditorActivationStrategy(viewer)
        {
            @Override
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
            {
                // activate on double-click only if MOD3 (usually the Alt key)
                // is *not* pressed because pressing MOD3 copies cell content to
                // the clipboard (see CopyPasteSupport)
                if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION)
                {
                    if (event.sourceEvent instanceof MouseEvent mouseEvent && mouseEvent.stateMask == SWT.MOD3)
                        return false;

                    return PortfolioPlugin.getDefault().getPreferenceStore()
                                    .getBoolean(UIConstants.Preferences.DOUBLE_CLICK_CELL_TO_EDIT);
                }

                return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
                                || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED
                                                && event.keyCode == SWT.CR)
                                || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
            }
        };

        int feature = ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                        | ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION;

        if (viewer instanceof TableViewer tableViewer)
            TableViewerEditor.create(tableViewer, null, activationStrategy, feature);
        else if (viewer instanceof TreeViewer treeViewer)
            TreeViewerEditor.create(treeViewer, activationStrategy, feature);

        if (state != null)
            viewer.getColumnViewerEditor().addEditorActivationListener(state.createListener());
    }
}

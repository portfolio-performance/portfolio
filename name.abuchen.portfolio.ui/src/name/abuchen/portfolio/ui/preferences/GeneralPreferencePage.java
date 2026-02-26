package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class GeneralPreferencePage extends FieldEditorPreferencePage
{

    public GeneralPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleGeneral);
    }

    @Override
    public void createFieldEditors()
    {
        var link = new Link(getFieldEditorParent(), 0);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(link);
        link.setText("<a>" + Messages.PrefUpdateQuotesAfterFileOpen + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var dialog = (PreferenceDialog) getContainer();
            var node = dialog.getPreferenceManager().find("general/prices"); //$NON-NLS-1$
            if (node != null)
                dialog.getTreeViewer().setSelection(new org.eclipse.jface.viewers.StructuredSelection(node));
        }));

        addField(new BooleanFieldEditor(UIConstants.Preferences.STORE_SETTINGS_NEXT_TO_FILE, //
                        Messages.PrefStoreSettingsNextToFile, getFieldEditorParent()));

        PreferenceDialogUtil.createNoteComposite(getFieldEditorParent().getFont(), getFieldEditorParent(),
                        Messages.PrefLabelNote, Messages.PrefNoteStoreSettingsNextToFile);

        addField(new BooleanFieldEditor(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS, //
                        Messages.PrefLabelUseSWTChartLibrary, getFieldEditorParent()));

        addField(new BooleanFieldEditor(UIConstants.Preferences.DOUBLE_CLICK_CELL_TO_EDIT, //
                        Messages.PrefDoubleClickCellToEdit, getFieldEditorParent()));

    }
}

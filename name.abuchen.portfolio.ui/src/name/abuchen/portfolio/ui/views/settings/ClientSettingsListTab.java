package name.abuchen.portfolio.ui.views.settings;

import javax.inject.Inject;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientAttribute;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.AttributeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;

public class ClientSettingsListTab implements AbstractTabbedView.Tab, ModificationListener
{
    private TableViewer settings;

    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Override
    public String getTitle()
    {
        return Messages.SettingsListView_title;
    }

    @Override
    public void addButtons(ToolBarManager manager)
    {
    }

    @Override
    public Composite createTab(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        settings = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ColumnEditingSupport.prepare(settings);

        ShowHideColumnHelper support = new ShowHideColumnHelper(ClientSettingsListTab.class.getSimpleName() + "@bottom", //$NON-NLS-1$
                        preferences, settings, layout);

        // Create Column for Setting
        Column column = new Column(Messages.SettingsListView_setting, SWT.None, 180);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ClientAttribute) element).getColumnLabel();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.SETTING.image();
            }

        });
        support.addColumn(column);

        column = new Column("", Messages.SettingsListView_type, SWT.None, 150); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return AttributeFieldType.of((ClientAttribute) element).toString();
            }
        });
        support.addColumn(column);

        column = new AttributeColumn("-SettingList", Messages.SettingsListView_value, SWT.None, 500); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ClientAttribute) element).getString();
            }
        });

        new AttributeEditingSupport(client.getSettings().getClientAttributes().stream()).addListener(this).attachTo(column);
        support.addColumn(column);

        support.createColumns();

        settings.getTable().setHeaderVisible(true);
        settings.getTable().setLinesVisible(true);

        settings.setContentProvider(ArrayContentProvider.getInstance());

        settings.setInput(client.getSettings().getClientAttributes().stream().filter(t -> t.getTarget() == Client.class).toArray());
        
        settings.refresh();

        return container;
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        if (newValue != null && !newValue.equals(oldValue) && (element instanceof ClientAttribute))
            ((ClientAttribute) element).setValue(newValue); 
        client.touch();
    }

}

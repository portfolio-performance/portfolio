package name.abuchen.portfolio.ui.wizards.sync;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class SyncMasterDataPage extends AbstractWizardPage
{
    public class ColumnMenuDetectListener implements MenuDetectListener
    {
        private Table table;
        private TableViewer tableViewer;

        private OnlineState.Property selectedProperty = OnlineState.Property.NAME;

        private int lastMenuDetectEventTime = 0;

        private MenuManager manager;
        private Menu menu;

        public ColumnMenuDetectListener(TableViewer tableViewer,
                        BiConsumer<IMenuManager, OnlineState.Property> menuBuilder)
        {
            this.table = tableViewer.getTable();
            this.tableViewer = tableViewer;

            manager = new MenuManager();
            manager.setRemoveAllWhenShown(true);
            manager.addMenuListener(m -> menuBuilder.accept(m, selectedProperty));
            menu = manager.createContextMenu(table);
        }

        @Override
        public void menuDetected(MenuDetectEvent event)
        {
            if (lastMenuDetectEventTime == event.time)
                return;
            lastMenuDetectEventTime = event.time;

            Point location = Display.getCurrent().map(null, tableViewer.getControl(), new Point(event.x, event.y));

            int xOffset = 0;
            for (int index : table.getColumnOrder())
            {
                int width = table.getColumn(index).getWidth();

                if (xOffset <= location.x && location.x < (xOffset + width))
                {
                    OnlinePropertyColumn column = (OnlinePropertyColumn) table.getColumn(index)
                                    .getData(Column.class.getName());

                    this.selectedProperty = column.getProperty();

                    menu.setLocation(event.x, event.y);
                    menu.setVisible(true);
                    break;
                }
                xOffset += width;
            }

        }
    }

    private final IPreferenceStore preferences;

    private List<SecurityDecorator> securities;

    private TableViewer tableViewer;

    public SyncMasterDataPage(Client client, IPreferenceStore preferences)
    {
        super("syncpage"); //$NON-NLS-1$
        this.preferences = preferences;

        this.securities = client.getSecurities().stream().map(SecurityDecorator::new).collect(Collectors.toList());

        setTitle("Stammdatenabgleich");
        setMessage("Grün markierte Wertpapiere werden aktualisiert. Per Kontextmenü den passenden Wert auswählen.");
    }

    public Stream<SecurityDecorator> getSecurities()
    {
        return this.securities.stream();
    }

    @Override
    public void beforePage()
    {
        Display.getDefault().asyncExec(this::runOnlineSync);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);

        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ShowHideColumnHelper support = new ShowHideColumnHelper(SyncMasterDataPage.class.getSimpleName() + "@start2", // FIXME
                        preferences, tableViewer, layout);

        addColumns(support);
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.getTable().addMenuDetectListener(new ColumnMenuDetectListener(tableViewer, this::fillContextMenu));

        setControl(container);
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        support.addColumn(new OnlinePropertyColumn(OnlineState.Property.NAME, 250));
        support.addColumn(new OnlinePropertyColumn(OnlineState.Property.ISIN, 120));
        support.addColumn(new OnlinePropertyColumn(OnlineState.Property.WKN, 120));
        support.addColumn(new OnlinePropertyColumn(OnlineState.Property.TICKER, 120));
    }

    private void fillContextMenu(IMenuManager menuManager, OnlineState.Property property)
    {
        if (tableViewer.getSelection().isEmpty())
            return;

        menuManager.add(new LabelOnly(property.getLabel()));

        boolean isMultiSelection = tableViewer.getStructuredSelection().size() > 1;

        if (isMultiSelection)
        {
            @SuppressWarnings("unchecked")
            List<SecurityDecorator> securities = new ArrayList<>(tableViewer.getStructuredSelection().toList());

            menuManager.add(new SimpleAction("Alle annehmen", a -> {
                securities.forEach(security -> {
                    OnlineProperty onlineProperty = security.getProperty(property);
                    if (onlineProperty.hasSuggestedValue())
                        onlineProperty.setModified(true);
                });
                tableViewer.refresh();
            }));

            menuManager.add(new SimpleAction("Alle ablehnen", a -> {
                securities.forEach(security -> {
                    OnlineProperty onlineProperty = security.getProperty(property);
                    onlineProperty.setModified(false);
                });
                tableViewer.refresh();
            }));
        }
        else
        {
            SecurityDecorator security = (SecurityDecorator) tableViewer.getStructuredSelection().getFirstElement();
            OnlineProperty onlineProperty = security.getProperty(property);

            menuManager.add(new LabelOnly("Vorschlagswert"));
            SimpleAction action = new SimpleAction(onlineProperty.getSuggestedValue(), a -> {
                onlineProperty.setModified(true);
                tableViewer.refresh(security);
            });
            action.setChecked(onlineProperty.isModified());
            menuManager.add(action);

            menuManager.add(new LabelOnly("Originalwert"));
            action = new SimpleAction(onlineProperty.getOriginalValue(), a -> {
                onlineProperty.setModified(false);
                tableViewer.refresh(security);
            });
            action.setChecked(!onlineProperty.isModified());
            menuManager.add(action);

        }
    }

    private void runOnlineSync()
    {
        try
        {
            getContainer().run(true, true, progress -> {
                progress.beginTask("Datenabgleich durchführen", securities.size());

                for (SecurityDecorator delegate : securities)
                {
                    if (progress.isCanceled())
                        break;

                    progress.setTaskName("Datenabgleich durchführen: " + delegate.getSecurity().getName());
                    delegate.checkOnline();
                    progress.worked(1);
                }

                Display.getDefault().syncExec(() -> tableViewer.setInput(securities));
            });
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }

}

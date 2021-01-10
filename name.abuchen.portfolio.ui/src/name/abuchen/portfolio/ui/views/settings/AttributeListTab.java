package name.abuchen.portfolio.ui.views.settings;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;

public class AttributeListTab implements AbstractTabbedView.Tab, ModificationListener
{
    /* package */ enum Mode
    {
        SECURITY(Security.class, Messages.LabelSecurities, client -> client.getSecurities()), //
        ACCOUNT(Account.class, Messages.LabelAccounts, client -> client.getAccounts()), //
        PORTFOLIO(Portfolio.class, Messages.LabelPortfolios, client -> client.getPortfolios()), //
        INVESTMENT_PLAN(InvestmentPlan.class, Messages.LabelInvestmentPlans, client -> client.getPlans());

        private final Class<? extends Attributable> type;
        private final String label;
        private final Function<Client, List<? extends Attributable>> listFunction;

        private Mode(Class<? extends Attributable> type, String label,
                        Function<Client, List<? extends Attributable>> listFunction)
        {
            this.type = type;
            this.label = label;
            this.listFunction = listFunction;
        }

        public Class<? extends Attributable> getType()
        {
            return type;
        }

        public String getLabel()
        {
            return label;
        }

        public List<? extends Attributable> getObjects(Client client) // NOSONAR
        {
            return listFunction.apply(client);
        }
    }

    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private Mode mode;

    private TableViewer tableViewer;

    @Override
    public String getTitle()
    {
        return Messages.AttributeTypeTitle + ": " + mode.getLabel(); //$NON-NLS-1$
    }

    @Override
    public void addButtons(ToolBarManager manager)
    {
        manager.add(new DropDown(Messages.LabelNewFieldByType, Images.PLUS, SWT.NONE, menuListener -> {
            menuListener.add(new LabelOnly(Messages.LabelNewFieldByType));

            for (AttributeFieldType fieldType : AttributeFieldType.values())
            {
                if (!fieldType.supports(mode.getType()))
                    continue;

                menuListener.add(new Action(fieldType.toString())
                {
                    @Override
                    public void run()
                    {
                        AttributeType attributeType = new AttributeType(UUID.randomUUID().toString());
                        attributeType.setName(Messages.ColumnName);
                        attributeType.setColumnLabel(Messages.ColumnColumnLabel);
                        attributeType.setConverter(fieldType.getConverterClass());
                        attributeType.setType(fieldType.getFieldClass());
                        attributeType.setTarget(mode.getType());

                        client.getSettings().addAttributeType(attributeType);
                        tableViewer.setInput(client.getSettings().getAttributeTypes()
                                        .filter(t -> t.getTarget() == mode.getType()).toArray());
                        client.touch();

                        tableViewer.editElement(attributeType, 0);
                    }
                });
            }
        }));
    }

    @Override
    public Composite createTab(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ColumnEditingSupport.prepare(tableViewer);

        ShowHideColumnHelper support = new ShowHideColumnHelper(AttributeListTab.class.getSimpleName() + "@v2", //$NON-NLS-1$
                        preferences, tableViewer, layout);

        addColumns(support);

        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new ArrayContentProvider());

        tableViewer.setInput(client.getSettings().getAttributeTypes().filter(t -> t.getTarget() == mode.getType())
                        .toArray());

        new ContextMenu(tableViewer.getTable(), this::fillContextMenu).hook();

        return container;
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnName, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((AttributeType) element).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.TEXT.image();
            }
        });
        new StringEditingSupport(AttributeType.class, "name").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnColumnLabel, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((AttributeType) element).getColumnLabel();
            }
        });
        new StringEditingSupport(AttributeType.class, "columnLabel").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFieldType, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return AttributeFieldType.of((AttributeType) element).toString();
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnSource, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((AttributeType) element).getSource();
            }
        });
        support.addColumn(column);

    }

    private void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        AttributeType attributeType = (AttributeType) selection.getFirstElement();

        if (selection.size() == 1)
            addMoveUpAndDownActions(manager, attributeType);

        manager.add(new Separator());
        addDeleteActions(manager, selection);
    }

    private void addMoveUpAndDownActions(IMenuManager manager, AttributeType attributeType)
    {
        int index = tableViewer.getTable().getSelectionIndex();

        if (index < 0)
            return;

        if (index > 0)
        {
            manager.add(new Action(Messages.MenuMoveUp)
            {
                @Override
                public void run()
                {
                    AttributeType above = (AttributeType) tableViewer.getTable().getItem(index - 1).getData();
                    int insertAt = client.getSettings().getAttributeTypeIndexOf(above);

                    moveAttribute(attributeType, insertAt);
                }
            });
        }

        if (index < tableViewer.getTable().getItemCount() - 1)
        {
            manager.add(new Action(Messages.MenuMoveDown)
            {
                @Override
                public void run()
                {
                    AttributeType below = (AttributeType) tableViewer.getTable().getItem(index + 1).getData();
                    int insertAt = client.getSettings().getAttributeTypeIndexOf(below);

                    moveAttribute(attributeType, insertAt);
                }
            });
        }
    }

    private void moveAttribute(AttributeType attributeType, int insertAt)
    {
        ClientSettings settings = client.getSettings();
        settings.removeAttributeType(attributeType);
        settings.addAttributeType(insertAt, attributeType);
        tableViewer.setInput(client.getSettings().getAttributeTypes().filter(t -> t.getTarget() == mode.getType())
                        .toArray());
        client.touch();
    }

    private void addDeleteActions(IMenuManager manager, IStructuredSelection selection)
    {
        manager.add(new Separator());
        manager.add(new Action(Messages.BookmarksListView_delete)
        {
            @Override
            public void run()
            {
                ClientSettings settings = client.getSettings();
                for (Object element : selection.toArray())
                {
                    AttributeType attribute = (AttributeType) element;
                    // remove any existing attribute values from objects
                    for (Attributable a : mode.getObjects(client))
                        a.getAttributes().remove(attribute);
                    settings.removeAttributeType(attribute);
                }

                client.touch();
                tableViewer.setInput(
                                settings.getAttributeTypes().filter(t -> t.getTarget() == mode.getType()).toArray());
            }
        });
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        client.touch();
    }
}

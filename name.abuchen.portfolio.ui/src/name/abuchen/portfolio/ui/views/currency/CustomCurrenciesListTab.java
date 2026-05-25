package name.abuchen.portfolio.ui.views.currency;

import jakarta.inject.Inject;

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

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CustomCurrency;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;

@SuppressWarnings("nls")
public class CustomCurrenciesListTab implements AbstractTabbedView.Tab, ModificationListener
{
    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private AbstractFinanceView view;

    private TableViewer tableViewer;

    @Override
    public String getTitle()
    {
        return Messages.LabelCurrencies;
    }

    @Override
    public void addButtons(ToolBarManager manager)
    {
        manager.add(new Action(Messages.NewFileWizardButtonAdd, Images.PLUS.descriptor())
        {
            @Override
            public void run()
            {
                CustomCurrency currency = new CustomCurrency(nextAvailableCurrencyCode("XXX"), "New Currency", "");

                client.addCustomCurrency(currency);
                client.touch();

                refresh();

                tableViewer.editElement(currency, 0);
            }
        });
    }

    @Override
    public Composite createTab(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        CopyPasteSupport.enableFor(tableViewer);

        ColumnEditingSupport.prepare(view.getEditorActivationState(), tableViewer);

        ShowHideColumnHelper support = new ShowHideColumnHelper(CustomCurrenciesListTab.class.getSimpleName(),
                        preferences, tableViewer, layout);

        addColumns(support);
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        refresh();

        new ContextMenu(tableViewer.getTable(), this::fillContextMenu).hook();

        return container;
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new Column("Code", SWT.NONE, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((CustomCurrency) element).getCurrencyCode();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.CHEVRON.image();
            }
        });

        new StringEditingSupport(CustomCurrency.class, "currencyCode")
        {
            @Override
            public void setValue(Object element, Object value) throws Exception
            {
                CustomCurrency currency = (CustomCurrency) element;
                String newCode = ((String) value).trim().toUpperCase();

                if (!isValidCurrencyCode(newCode, currency))
                    return;

                super.setValue(element, newCode);
            }
        }.addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnName, SWT.NONE, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((CustomCurrency) element).getDisplayName();
            }
        });

        new StringEditingSupport(CustomCurrency.class, "displayName")
        {
            @Override
            public void setValue(Object element, Object value) throws Exception
            {
                String newName = ((String) value).trim();

                if (newName.isEmpty())
                    return;

                super.setValue(element, nextAvailableCurrencyName(newName, (CustomCurrency) element));
            }
        }.addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column("Symbol", SWT.NONE, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((CustomCurrency) element).getCurrencySymbol();
            }
        });

        new StringEditingSupport(CustomCurrency.class, "currencySymbol").addListener(this).attachTo(column);
        support.addColumn(column);
    }

    private void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

        boolean canDelete = true;
        for (Object element : selection.toArray())
        {
            CustomCurrency currency = (CustomCurrency) element;
            if (isCurrencyCodeUsed(currency.getCurrencyCode()))
            {
                canDelete = false;
                break;
            }
        }

        manager.add(new Separator());

        Action delete = new Action(Messages.BookmarksListView_delete)
        {
            @Override
            public void run()
            {
                for (Object element : selection.toArray())
                    client.removeCustomCurrency((CustomCurrency) element);

                client.touch();
                refresh();
            }
        };

        delete.setEnabled(canDelete);
        manager.add(delete);
    }

    private boolean isValidCurrencyCode(String code, CustomCurrency current)
    {
        if (code.isEmpty())
            return false;

        if (CurrencyUnit.containsCurrencyCode(code))
            return false;

        for (CustomCurrency currency : client.getCustomCurrencies())
        {
            if (currency != current && code.equalsIgnoreCase(currency.getCurrencyCode()))
                return false;
        }

        return true;
    }

    private String nextAvailableCurrencyCode(String baseCode)
    {
        String code = baseCode;
        int index = 2;

        while (!isValidCurrencyCode(code, null))
        {
            code = "X" + index;
            index++;
        }

        return code;
    }

    private void refresh()
    {
        tableViewer.setInput(client.getCustomCurrencies().toArray());
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        client.touch();
        tableViewer.refresh(element);
    }

    private boolean isCurrencyCodeUsed(String currencyCode)
    {
        return client.getUsedCurrencies().stream().anyMatch(u -> currencyCode.equals(u.getCurrencyCode()));
    }

    private String nextAvailableCurrencyName(String baseName, CustomCurrency current)
    {
        String name = baseName;
        int index = 2;

        while (true)
        {
            final String candidate = name;

            boolean exists = client.getCustomCurrencies().stream().filter(c -> c != current)
                            .anyMatch(c -> candidate.equals(c.getDisplayName()));

            if (!exists)
                return candidate;

            name = baseName + " (" + index + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            index++;
        }
    }
}

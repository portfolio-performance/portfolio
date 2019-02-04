package name.abuchen.portfolio.ui.views;

import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.DateSelectionDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;

public class StatementOfAssetsView extends AbstractFinanceView
{
    private StatementOfAssetsViewer assetViewer;
    private PropertyChangeListener currencyChangeListener;
    private ClientFilterDropDown clientFilter;

    /**
     * The date for which to calculate the statement of assets. If the date is
     * empty, it means using the current date. We do not save the current date
     * here, because some users have the view open more than 24 hours.
     */
    private Optional<LocalDate> snapshotDate = Optional.empty();

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return assetViewer == null ? Messages.LabelStatementOfAssets : Messages.LabelStatementOfAssets + //
                        " (" + assetViewer.getColumnHelper().getConfigurationName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void notifyModelUpdated()
    {
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, converter,
                        snapshotDate.orElse(LocalDate.now()));

        assetViewer.setInput(snapshot, clientFilter.getSelectedFilter());
        updateTitle(getDefaultTitle());
    }

    @Override
    protected void addButtons(final ToolBarManager toolBar)
    {
        DropDown dropDown = new DropDown(getClient().getBaseCurrency());
        dropDown.setMenuListener(manager -> {
            // put list of favorite units on top
            List<CurrencyUnit> allUnits = getClient().getUsedCurrencies();
            // add a separator marker
            allUnits.add(null);
            // then all available units
            List<CurrencyUnit> available = CurrencyUnit.getAvailableCurrencyUnits();
            Collections.sort(available);
            allUnits.addAll(available);
            // now show the list
            for (final CurrencyUnit unit : allUnits)
            {
                // is this a unit or a separator?
                if (unit != null)
                {
                    Action action = new SimpleAction(unit.getLabel(), a -> {
                        dropDown.setLabel(unit.getCurrencyCode());
                        getClient().setBaseCurrency(unit.getCurrencyCode());
                    });
                    action.setChecked(getClient().getBaseCurrency().equals(unit.getCurrencyCode()));
                    manager.add(action);
                }
                else
                {
                    // add a separator
                    manager.add(new Separator());
                }
            }
        });

        toolBar.add(dropDown);
        currencyChangeListener = e -> dropDown.setLabel(e.getNewValue().toString());
        getClient().addPropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$

        addCalendarButton(toolBar);

        this.clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        StatementOfAssetsView.class.getSimpleName(), filter -> notifyModelUpdated());
        toolBar.add(clientFilter);

        Action export = new SimpleAction(null, action -> new TableViewerCSVExporter(assetViewer.getTableViewer())
                        .export(Messages.LabelStatementOfAssets + ".csv")); //$NON-NLS-1$
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);
        toolBar.add(export);

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> assetViewer.menuAboutToShow(manager)));
    }

    private void addCalendarButton(ToolBarManager toolBar)
    {
        DropDown dropDown = new DropDown(Messages.LabelPortfolioTimeMachine, Images.CALENDAR_OFF, SWT.NONE);
        dropDown.setMenuListener(manager -> {
            manager.add(new LabelOnly(Values.Date.format(snapshotDate.orElse(LocalDate.now()))));
            manager.add(new Separator());

            SimpleAction action = new SimpleAction(Messages.LabelToday, a -> {
                snapshotDate = Optional.empty();
                notifyModelUpdated();
                dropDown.setImage(Images.CALENDAR_OFF);
            });
            action.setEnabled(snapshotDate.isPresent());
            manager.add(action);

            manager.add(new SimpleAction(Messages.MenuPickOtherDate, a -> {
                DateSelectionDialog dialog = new DateSelectionDialog(getActiveShell());
                dialog.setSelection(snapshotDate.orElse(LocalDate.now()));
                if (dialog.open() != DateSelectionDialog.OK)
                    return;
                if (snapshotDate.isPresent() && snapshotDate.get().equals(dialog.getSelection()))
                    return;

                snapshotDate = LocalDate.now().equals(dialog.getSelection()) ? Optional.empty()
                                : Optional.of(dialog.getSelection());
                notifyModelUpdated();
                dropDown.setImage(!snapshotDate.isPresent() ? Images.CALENDAR_OFF : Images.CALENDAR_ON);
            }));
        });

        toolBar.add(dropDown);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assetViewer = make(StatementOfAssetsViewer.class);
        Control control = assetViewer.createControl(parent);
        assetViewer.setToolBarManager(getViewToolBarManager());

        updateTitle(getDefaultTitle());
        assetViewer.getColumnHelper().addListener(() -> updateTitle(getDefaultTitle()));

        hookContextMenu(assetViewer.getTableViewer().getControl(),
                        manager -> assetViewer.hookMenuListener(manager, StatementOfAssetsView.this));
        notifyModelUpdated();

        return control;
    }

    @Override
    public void dispose()
    {
        if (currencyChangeListener != null)
            getClient().removePropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$
    }
}

package name.abuchen.portfolio.ui.views.columns;

import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

public class CurrencyColumn extends Column
{
    private static class CurrencyColumnLabelProvider extends ColumnLabelProvider
    {
        @Override
        public String getText(Object e)
        {
            InvestmentVehicle n = Adaptor.adapt(InvestmentVehicle.class, e);
            return n != null ? n.getCurrencyCode() : null;
        }
    }

    public static class CurrencyEditingSupport extends ListEditingSupport
    {
        public CurrencyEditingSupport()
        {
            super(InvestmentVehicle.class, "currencyCode", //$NON-NLS-1$
                            CurrencyUnit.getAvailableCurrencyUnits().stream() //
                                            .map(u -> u.getCurrencyCode()).sorted().collect(Collectors.toList()));
        }

    }

    public CurrencyColumn()
    {
        this("currency", Messages.ColumnCurrency, SWT.LEFT, 60); //$NON-NLS-1$
    }

    public CurrencyColumn(String id, String label, int style, int defaultWidth)
    {
        super(id, label, style, defaultWidth);

        setLabelProvider(new CurrencyColumnLabelProvider());
        setSorter(ColumnViewerSorter.create(InvestmentVehicle.class, "currencyCode")); //$NON-NLS-1$
    }
}

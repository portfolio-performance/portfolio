package name.abuchen.portfolio.ui.views.currency;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;

public class CurrencyView extends AbstractTabbedView<AbstractTabbedView.Tab>
{
    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelCurrencies;
    }

    @Override
    protected List<AbstractTabbedView.Tab> createTabs()
    {
        return Arrays.asList(make(ExchangeRatesListTab.class), make(CurrencyConverterTab.class));
    }
}

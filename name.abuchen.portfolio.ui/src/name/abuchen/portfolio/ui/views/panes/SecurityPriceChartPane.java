package name.abuchen.portfolio.ui.views.panes;

import java.util.ArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.SecuritiesChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.IntervalOption;
import name.abuchen.portfolio.ui.views.SecurityDetailsViewer;

public class SecurityPriceChartPane implements InformationPanePage
{
    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    private IStylingEngine stylingEngine;

    private Object securitySelection;
    private SecuritiesChart chart;
    private SecurityDetailsViewer details;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabChart;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);
        sash.setLayout(new SashLayout(sash, SWT.HORIZONTAL | SWT.END));

        chart = new SecuritiesChart(sash, client, new CurrencyConverterImpl(factory, client.getBaseCurrency()));

        String option = preferences.getString(SecurityPriceChartPane.class.getSimpleName());
        if (option != null && !option.isEmpty())
        {
            try
            {
                IntervalOption o = IntervalOption.valueOf(option);
                chart.setIntervalOption(o);
            }
            catch (IllegalArgumentException ignore)
            {
                // keep default interval option if value is not found
            }
        }

        chart.getControl().addDisposeListener(e -> preferences.setValue(SecurityPriceChartPane.class.getSimpleName(),
                        chart.getIntervalOption().name()));

        stylingEngine.style(chart.getControl());
        stylingEngine.style(chart);

        details = new SecurityDetailsViewer(sash, SWT.NONE, client, true);

        final String sashIdentifier = getClass().getSimpleName() + "-newsash"; //$NON-NLS-1$
        int size = preferences.getInt(sashIdentifier);
        if (size == 0)
            size = SWTHelper.getPackedWidth(details.getControl());
        details.getControl().setLayoutData(new SashLayoutData(size));
        sash.addDisposeListener(e -> preferences.setValue(sashIdentifier,
                        ((SashLayoutData) details.getControl().getLayoutData()).getSize()));

        return sash;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        chart.addButtons(toolBar);
    }

    @Override
    public void setInput(Object input)
    {
        // because SecurityPriceChartPane currently has different types parents
        // with different security selection methods, we need to check both and
        // map to multiple security selection
        SecuritySelection selection = Adaptor.adapt(SecuritySelection.class, input);
        if (selection == null)
        {
            Security singleSecuritySelection = Adaptor.adapt(Security.class, input);
            if (singleSecuritySelection != null)
                selection = new SecuritySelection(client, singleSecuritySelection);
            else
                selection = new SecuritySelection(client, new ArrayList<>());
        }

        var selectedSecurities = selection.getSecurities();

        chart.updateChart(client, selectedSecurities.toArray(new Security[0]));

        Security singleOrNull;
        if (selectedSecurities.size() != 1)
            singleOrNull = null;
        else
            singleOrNull = selectedSecurities.getFirst();

        details.setInput(singleOrNull);
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (securitySelection != null)
            setInput(securitySelection);
    }
}

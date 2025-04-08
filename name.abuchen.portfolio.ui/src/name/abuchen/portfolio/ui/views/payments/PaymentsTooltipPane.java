package name.abuchen.portfolio.ui.views.payments;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class PaymentsTooltipPane implements InformationPanePage
{
    @Inject
    private AbstractFinanceView view;

    @Inject
    private Client client;

    @Inject
    private PaymentsViewModel model;

    private TabularDataSource source;

    private Composite container;
    private Composite tableViewer;

    @PostConstruct
    private void setupListener()
    {
        // data not only has to be recalculated when the client changes, but
        // also if configuration of the model changes (net/gross, consolidate
        // retired)
        model.addUpdateListener(() -> {
            if (source != null)
            {
                source.invalidate();
                setInput(source);
            }
        });
    }

    @Override
    public String getLabel()
    {
        return Messages.ColumnDetails;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());
        return container;
    }

    @Override
    public void setInput(Object input)
    {
        if (tableViewer != null && !tableViewer.isDisposed())
            tableViewer.dispose();
        tableViewer = null;

        if (input instanceof TabularDataSource dataSource)
        {
            this.source = dataSource;
            tableViewer = dataSource.createTableViewer(client, view, container);
            container.layout();
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        // problem: updating the tabular source will only have an effect if the
        // payments view model has been updated before. We do not recalculate
        // the model here because most likely it already has been recalculated
        // (and we do not have a unique identifier with the refresh event to
        // tell).
        if (source != null)
            setInput(source);
    }

}

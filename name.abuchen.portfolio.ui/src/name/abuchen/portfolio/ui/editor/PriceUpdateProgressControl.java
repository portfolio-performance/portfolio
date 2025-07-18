package name.abuchen.portfolio.ui.editor;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateProgress;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateSnapshot;
import name.abuchen.portfolio.ui.views.SecurityPriceUpdateView;

public class PriceUpdateProgressControl implements PriceUpdateProgress.Listener
{
    private final Client client;
    private final PortfolioPart editor;

    private ProgressBar progressBar;

    private PriceUpdateSnapshot lastStatus;

    @Inject
    public PriceUpdateProgressControl(Client client, PortfolioPart editor)
    {
        this.client = client;
        this.editor = editor;

        PriceUpdateProgress.getInstance().register(client, this);
    }

    @PostConstruct
    public void createComposite(Composite parent)
    {
        progressBar = new ProgressBar(parent, SWT.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setVisible(false);

        progressBar.addMouseListener(MouseListener
                        .mouseUpAdapter(e -> editor.activateView(SecurityPriceUpdateView.class, this.lastStatus)));

        parent.addDisposeListener(e -> PriceUpdateProgress.getInstance().unregister(client, this));
    }

    public Control getControl()
    {
        return progressBar;
    }

    @Override
    public void onProgress(PriceUpdateSnapshot status)
    {
        this.lastStatus = status;
        if (progressBar != null && !progressBar.isDisposed())
        {
            progressBar.setMaximum(status.getCount());
            progressBar.setSelection(status.getCompleted());
            progressBar.setVisible(status.getCount() != status.getCompleted());
            progressBar.setToolTipText(status.getCompleted() + " / " + status.getCount()); //$NON-NLS-1$
        }
    }
}

package name.abuchen.portfolio.ui.wizards.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Feed;

public abstract class AbstractProviderPage extends AbstractPage
{
    public static final String YAHOO = "YAHOO"; //$NON-NLS-1$
    public static final String HTML = "HTML"; //$NON-NLS-1$

    protected ComboViewer comboProvider;

    protected Group grpFeed;
    protected Label labelDetailData;
    protected ComboViewer comboExchange;
    protected Text textFeedURL;

    protected final EditSecurityModel model;

    // used to identify if the ticker has been changed on another page
    protected String tickerSymbol;

    protected Map<Feed, List<Exchange>> cacheExchanges = new HashMap<Feed, List<Exchange>>();

    protected AbstractProviderPage(EditSecurityModel model)
    {
        this.model = model;
    }

    protected final EditSecurityModel getModel()
    {
        return model;
    }

    protected abstract String getFeed();

    protected abstract void setFeed(String feed);

    protected abstract String getFeedURL();

    protected abstract void setFeedURL(String feedURL);

    protected abstract void setStatus(String status);

    protected abstract void createSampleArea(Composite parent);

    protected abstract List<Feed> getAvailableFeeds();

    protected abstract Feed getFeedProvider(String feedId);

    protected abstract void reinitCaches();

    protected abstract void clearSamples();

    protected abstract void showSamples(Feed feed, Exchange exchange);

    @Override
    public abstract void beforePage();

    @Override
    public abstract void afterPage();

    /**
     * Builds a temporary {@link Security} from the currently selected values.
     * 
     * @return {@link Security}
     */
    protected Security buildTemporarySecurity()
    {
        // create a temporary security and set all attributes
        Security security = new Security();
        model.setAttributes(security);
        return security;
    }

    abstract protected void createProviderGroup(Composite container);
    
    abstract protected void setupInitialData();

    abstract protected void onFeedProviderChanged(SelectionChangedEvent event);
    
    protected void onExchangeChanged(SelectionChangedEvent event)
    {
        Exchange exchange = (Exchange) ((IStructuredSelection) event.getSelection()).getFirstElement();
        setStatus(null);

        if (exchange == null)
        {
            clearSamples();
        }
        else
        {
            Feed feed = (Feed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSamples(feed, exchange);
        }
    }

    abstract protected void onFeedURLChanged();
}

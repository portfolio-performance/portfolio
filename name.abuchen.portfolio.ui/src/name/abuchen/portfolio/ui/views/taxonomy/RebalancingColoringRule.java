package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class RebalancingColoringRule extends BindingHelper.Model
{
    private static final String KEY_RELATIVE_THRESHOLD = "taxonomy.rebalancing.relative-threshold"; //$NON-NLS-1$
    private static final String KEY_ABSOLUTE_THRESHOLD = "taxonomy.rebalancing.absolute-threshold"; //$NON-NLS-1$
    private static final String KEY_BAR_LENGTH = "taxonomy.rebalancing.bar-length"; //$NON-NLS-1$

    private int relativeThreshold;
    private int absoluteThreshold;
    private int barLength;

    public RebalancingColoringRule(Client client)
    {
        super(client);

        relativeThreshold = client.getPropertyInt(KEY_RELATIVE_THRESHOLD);
        if (relativeThreshold == 0)
            relativeThreshold = 25;

        absoluteThreshold = client.getPropertyInt(KEY_ABSOLUTE_THRESHOLD);
        if (absoluteThreshold == 0)
            absoluteThreshold = 5;

        barLength = client.getPropertyInt(KEY_BAR_LENGTH);
        if (barLength == 0)
            barLength = 20;
    }

    public int getRelativeThreshold()
    {
        return relativeThreshold;
    }

    public void setRelativeThreshold(int relativeThreshold)
    {
        firePropertyChange("relativeThreshold", this.relativeThreshold, this.relativeThreshold = relativeThreshold); // NOSONAR //$NON-NLS-1$
    }

    public int getAbsoluteThreshold()
    {
        return absoluteThreshold;
    }

    public void setAbsoluteThreshold(int absoluteThreshold)
    {
        firePropertyChange("absoluteThreshold", this.absoluteThreshold, this.absoluteThreshold = absoluteThreshold); // NOSONAR //$NON-NLS-1$
    }

    public int getBarLength()
    {
        return barLength;
    }

    public void setBarLength(int barLength)
    {
        firePropertyChange("price", this.barLength, this.barLength = barLength); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public void applyChanges()
    {
        Client client = getClient();
        client.setProperty(KEY_RELATIVE_THRESHOLD, String.valueOf(relativeThreshold));
        client.setProperty(KEY_ABSOLUTE_THRESHOLD, String.valueOf(absoluteThreshold));
        client.setProperty(KEY_BAR_LENGTH, String.valueOf(barLength));
    }
}

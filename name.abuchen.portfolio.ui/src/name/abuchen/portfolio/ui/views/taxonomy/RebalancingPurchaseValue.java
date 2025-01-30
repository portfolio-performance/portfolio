package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class RebalancingPurchaseValue extends BindingHelper.Model
{
    private static final String KEY_PURCHASE_VALUE = "taxonomy.rebalancing.purchase"; //$NON-NLS-1$

    private long purchaseValue = Values.Amount.factorize(100);

    public RebalancingPurchaseValue(Client client)
    {
        super(client);

        purchaseValue = client.getPropertyInt(KEY_PURCHASE_VALUE);
    }

    public long getPurchaseValue()
    {
        return purchaseValue;
    }

    public void setPurchaseValue(long purchaseValue)
    {
        firePropertyChange("purchaseValue", this.purchaseValue, this.purchaseValue = purchaseValue); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public void applyChanges()
    {
        Client client = getClient();
        client.setProperty(KEY_PURCHASE_VALUE, String.valueOf(purchaseValue));
    }
}

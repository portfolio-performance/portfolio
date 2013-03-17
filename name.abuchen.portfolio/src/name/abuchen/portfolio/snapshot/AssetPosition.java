package name.abuchen.portfolio.snapshot;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Security;

public class AssetPosition implements Comparable<AssetPosition>
{
    private final SecurityPosition position;
    private final Account account;
    private final long valuation;
    private final long totalAssets;

    /* package */AssetPosition(SecurityPosition position, long totalAssets)
    {
        this.position = position;
        this.account = null;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
    }

    /* package */AssetPosition(SecurityPosition position, Account account, long totalAssets)
    {
        this.position = position;
        this.account = account;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
    }

    public long getValuation()
    {
        return this.valuation;
    }

    public double getShare()
    {
        return (double) getValuation() / (double) this.totalAssets;
    }

    public String getDescription()
    {
        return account != null ? account.getName() : position.getSecurity().getName();
    }

    public Security getSecurity()
    {
        return position.getSecurity();
    }

    public SecurityPosition getPosition()
    {
        return position;
    }

    public Account getAccount()
    {
        return account;
    }

    public int compareTo(AssetPosition o)
    {
        return getDescription().compareTo(o.getDescription());
    }
}

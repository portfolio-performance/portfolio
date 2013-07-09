package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;

public class TreeMapItem
{
    private TreeMapItem parent;
    private List<TreeMapItem> children = new ArrayList<TreeMapItem>();

    private long valuation;
    private double percentage;

    private IndustryClassification.Category category;
    private Security security;
    private Account account;
    private AssetCategory assetCategory;
    private AssetPosition assetPosition;

    public TreeMapItem()
    {}

    public TreeMapItem(TreeMapItem parent, Category category)
    {
        this.parent = parent;
        this.category = category;
    }

    public TreeMapItem(TreeMapItem parent, Security security, long valuation)
    {
        this.parent = parent;
        this.security = security;
        this.valuation = valuation;
    }

    public TreeMapItem(TreeMapItem parent, Account account, long valuation)
    {
        this.parent = parent;
        this.account = account;
        this.valuation = valuation;
    }

    public TreeMapItem(TreeMapItem parent, AssetCategory assetCategory)
    {
        this.parent = parent;
        this.assetCategory = assetCategory;
        this.valuation = assetCategory.getValuation();
    }

    public TreeMapItem(TreeMapItem parent, AssetPosition assetPosition)
    {
        this.parent = parent;
        this.security = assetPosition.getSecurity();
        this.assetPosition = assetPosition;
        this.valuation = assetPosition.getValuation();
    }

    public TreeMapItem getParent()
    {
        return parent;
    }

    public boolean isCategory()
    {
        return category != null;
    }

    public IndustryClassification.Category getCategory()
    {
        return category;
    }

    public boolean isSecurity()
    {
        return security != null;
    }

    public Security getSecurity()
    {
        return security;
    }

    public boolean isAccount()
    {
        return account != null;
    }

    public Account getAccount()
    {
        return account;
    }
    
    public boolean isAssetCategory()
    {
        return assetCategory != null;
    }

    public AssetCategory getAssetCategory()
    {
        return assetCategory;
    }

    public void setValuation(long valuation)
    {
        this.valuation = valuation;
    }

    public long getValuation()
    {
        return valuation;
    }

    public void setPercentage(double percentage)
    {
        this.percentage = percentage;
    }

    public double getPercentage()
    {
        return percentage;
    }

    public List<TreeMapItem> getChildren()
    {
        return children;
    }

    public List<TreeMapItem> getPath()
    {
        LinkedList<TreeMapItem> path = new LinkedList<TreeMapItem>();

        TreeMapItem item = this;
        while (item != null)
        {
            path.addFirst(item);
            item = item.getParent();
        }

        return path;
    }

    public void pruneEmpty()
    {
        LinkedList<TreeMapItem> stack = new LinkedList<TreeMapItem>();
        stack.add(this);

        while (!stack.isEmpty())
        {
            TreeMapItem item = stack.remove();
            Iterator<TreeMapItem> iterator = item.getChildren().iterator();
            while (iterator.hasNext())
            {
                TreeMapItem child = iterator.next();
                if (child.getValuation() == 0)
                    iterator.remove();
            }

            stack.addAll(item.getChildren());
        }
    }

    public void calculatePercentages(long assets)
    {
        LinkedList<TreeMapItem> stack = new LinkedList<TreeMapItem>();
        stack.add(this);

        while (!stack.isEmpty())
        {
            TreeMapItem item = stack.remove();
            item.setPercentage((double) item.getValuation() / (double) assets);

            stack.addAll(item.getChildren());
        }
    }

    public void sortBySize()
    {
        if (!getChildren().isEmpty())
        {

            Collections.sort(getChildren(), new Comparator<TreeMapItem>()
            {
                @Override
                public int compare(TreeMapItem o1, TreeMapItem o2)
                {
                    return Long.valueOf(o2.getValuation()).compareTo(Long.valueOf(o1.getValuation()));
                }
            });

            for (TreeMapItem child : getChildren())
                child.sortBySize();

        }
    }

    public String getLabel()
    {
        if (isCategory())
            return category.getLabel();
        else if (isSecurity())
            return security.getName();
        else if (isAccount())
            return account.getName();
        else if (isAssetCategory())
            return assetCategory.getClassification().toString();
        else if (assetPosition != null)
            return assetPosition.getDescription();
        else
            return super.toString();
    }

    @Override
    public String toString()
    {
        return getLabel();
    }
}

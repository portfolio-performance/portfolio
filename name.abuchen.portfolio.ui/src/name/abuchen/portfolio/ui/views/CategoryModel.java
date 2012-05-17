package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class CategoryModel
{
    private CategoryModel parent;
    private List<CategoryModel> children = new ArrayList<CategoryModel>();

    private Category subject;

    private long actual;
    private long target;

    private CategoryModel(CategoryModel parent, Category subject)
    {
        this.subject = subject;
        this.parent = parent;
    }

    public Category getSubject()
    {
        return subject;
    }

    public long getActual()
    {
        return actual;
    }

    public long getTarget()
    {
        return target;
    }

    public CategoryModel getParent()
    {
        return parent;
    }

    public List<CategoryModel> getChildren()
    {
        return children;
    }

    public List<Object> getElements()
    {
        return subject.getElements();
    }

    public String getName()
    {
        return subject.getName();
    }

    public int getPercentage()
    {
        return subject.getPercentage();
    }

    public void setName(String name)
    {
        subject.setName(name);
    }

    public void setPercentage(int percentage)
    {
        subject.setPercentage(percentage);
    }

    public void addCategory(Category category)
    {
        subject.addCategory(category);
        children.add(new CategoryModel(this, category));
    }

    public void removeCategory(CategoryModel category)
    {
        subject.removeCategory(category.subject);
        children.remove(category);
    }

    public void recalculate(ClientSnapshot snapshot)
    {
        this.target = snapshot.getAssets();
        visitActuals(snapshot, this);
        recalculateTargets();
    }

    private void recalculateTargets()
    {
        for (CategoryModel child : children)
        {
            child.target = target * child.subject.getPercentage() / 100;
            child.recalculateTargets();
        }
    }

    public CategoryModel findFor(Object element)
    {
        if (subject.getElements().contains(element))
            return this;

        for (CategoryModel child : this.children)
        {
            CategoryModel cat = child.findFor(element);
            if (cat != null)
                return cat;
        }

        return null;
    }

    public static CategoryModel create(ClientSnapshot snapshot)
    {
        Category root = snapshot.getClient().getRootCategory();
        CategoryModel modelRoot = new CategoryModel(null, root);
        modelRoot.target = snapshot.getAssets();

        Stack<CategoryModel> stack = new Stack<CategoryModel>();
        stack.add(modelRoot);

        // create CategoryModel tree
        while (!stack.isEmpty())
        {
            CategoryModel m = stack.pop();

            for (Category c : m.subject.getChildren())
            {
                CategoryModel cm = new CategoryModel(m, c);
                stack.push(cm);
                m.children.add(cm);
            }
        }

        // calculate actuals
        visitActuals(snapshot, modelRoot);

        // calculate targets
        modelRoot.recalculateTargets();

        return modelRoot;
    }

    private static void visitActuals(ClientSnapshot snapshot, CategoryModel model)
    {
        long actual = 0;

        for (CategoryModel child : model.children)
        {
            visitActuals(snapshot, child);
            actual += child.actual;
        }

        for (Object element : model.subject.getElements())
        {
            if (element instanceof Security)
            {
                PortfolioSnapshot portfolio = snapshot.getJointPortfolio();
                SecurityPosition p = portfolio.getPositionsBySecurity().get(element);
                if (p != null)
                    actual += p.calculateValue();
            }
            else if (element instanceof Account)
            {
                for (AccountSnapshot s : snapshot.getAccounts())
                {
                    if (s.getAccount() == element)
                        actual += s.getFunds();
                }
            }
            else
            {
                throw new RuntimeException("unknown element: " + element.getClass().getName()); //$NON-NLS-1$
            }

        }

        model.actual = actual;
    }

}

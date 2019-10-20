package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;

public class ExpectedReturnsAttachedModel implements TaxonomyModel.AttachedModel
{
    public static final String KEY_EXPECTED_RETURN = "expected-return"; //$NON-NLS-1$

    private Map<TaxonomyNode, Boolean> isERinUse = new HashMap<>();

    @Override
    public void setup(TaxonomyModel model)
    {
        model.visitAll(n -> {
            n.setData("test", Integer.valueOf(42));
        });
    }

    @Override
    public void addColumns(ShowHideColumnHelper columns)
    {
    }

    @Override
    public void recalculate(TaxonomyModel model)
    {
        // Recalculate full expected returns tree
        calcFullERTree(model.getVirtualRootNode());
    }

    private int getExpectedReturnFor(TaxonomyNode node)
    {
        Integer expectedReturn = (Integer) node.getData(KEY_EXPECTED_RETURN);
        return expectedReturn == null ? 0 : expectedReturn.intValue();
    }

    private void setExpectedReturnFor(TaxonomyNode node, int expectedReturn)
    {
        node.setData(KEY_EXPECTED_RETURN, Integer.valueOf(expectedReturn));
    }

    private void calcFullERTree(TaxonomyNode virtualRootNode)
    {
        // ...
    }

}

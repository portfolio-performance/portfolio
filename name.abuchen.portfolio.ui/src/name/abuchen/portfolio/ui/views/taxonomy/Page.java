package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelChangeListener;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */abstract class Page implements TaxonomyModelChangeListener
{
    private TaxonomyModel model;
    private TaxonomyNodeRenderer renderer;

    protected Page(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        this.model = model;
        this.renderer = renderer;

        this.model.addListener(this);
    }

    protected final TaxonomyModel getModel()
    {
        return model;
    }

    protected final TaxonomyNodeRenderer getRenderer()
    {
        return renderer;
    }

    public abstract Control createControl(Composite parent);

    public abstract void beforePage();

    public abstract void afterPage();

}

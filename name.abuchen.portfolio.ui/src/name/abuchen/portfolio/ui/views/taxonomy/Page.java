package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelUpdatedListener;

/* package */abstract class Page implements TaxonomyModelUpdatedListener
{
    private TaxonomyModel model;
    private TaxonomyNodeRenderer renderer;

    @Inject
    private IPreferenceStore preferenceStore;

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

    public final IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void configMenuAboutToShow(IMenuManager manager)
    {}

    public void exportMenuAboutToShow(IMenuManager manager)
    {}

    public abstract Control createControl(Composite parent);

    public abstract void beforePage();

    public abstract void afterPage();

    public void dispose()
    {}
}

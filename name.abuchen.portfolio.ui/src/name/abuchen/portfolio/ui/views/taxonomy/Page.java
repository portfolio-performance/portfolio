package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel.TaxonomyModelChangeListener;

/* package */abstract class Page implements TaxonomyModelChangeListener
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

    public IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void showConfigMenu(Shell shell)
    {}

    public abstract Control createControl(Composite parent);

    public abstract void beforePage();

    public abstract void afterPage();

    public void dispose()
    {}
}

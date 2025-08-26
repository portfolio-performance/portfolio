package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.TaxonomyImportModel.ImportAction;
import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.TaxonomyImportModel.ImportItem;

/**
 * Wizard page displaying detailed preview of a taxonomy import with tree
 * structure and messages tabs.
 */
public class TaxonomyDetailPage extends AbstractWizardPage
{
    private final IStylingEngine stylingEngine;
    private final TaxonomyImportModel importModel;
    private final ImportItem importItem;

    public TaxonomyDetailPage(IStylingEngine stylingEngine, TaxonomyImportModel importModel, ImportItem importItem)
    {
        super("taxonomyDetail_" + importItem.getName()); //$NON-NLS-1$
        this.stylingEngine = stylingEngine;
        this.importModel = importModel;
        this.importItem = importItem;

        if (importItem.getImportAction() == ImportAction.UPDATE)
            setTitle(importItem.getName() + " -> " + importItem.getTargetTaxonomy().getName()); //$NON-NLS-1$
        else
            setTitle(importItem.getName());

        setDescription(importItem.getChangesText());
    }

    @Override
    public void createControl(Composite parent)
    {
        var container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        var taxonomyTabFolder = new TaxonomyTabFolder(importModel.getClient(), stylingEngine);
        var tabFolder = taxonomyTabFolder.createTabFolder(container, importItem.getName());
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 400).applyTo(tabFolder);

        taxonomyTabFolder.setImportResult(importItem.getDryrunTaxonomy(), importItem.getDryrunResult());

        setControl(container);
    }
}

package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.bootstrap.BundleMessages;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ImportResult;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.TaxonomyImportModel.ImportAction;
import name.abuchen.portfolio.util.Pair;

/**
 * Wizard for importing multiple taxonomies from a JSON array.
 */
public class MultiTaxonomyImportWizard extends Wizard
{
    private final IStylingEngine stylingEngine;

    private TaxonomyImportModel importModel;
    private TaxonomyFileSelectionPage fileSelectionPage;
    private List<TaxonomyDetailPage> detailPages = new ArrayList<>();
    private boolean detailPagesAdded = false;

    public MultiTaxonomyImportWizard(Client client, IPreferenceStore preferences, IStylingEngine stylingEngine)
    {
        this.stylingEngine = stylingEngine;

        this.importModel = new TaxonomyImportModel(client, preferences);

        setWindowTitle(BundleMessages.getString(BundleMessages.Label.Command.importTaxonomy));
        setNeedsProgressMonitor(false);
        setDialogSettings(PortfolioPlugin.getDefault().getDialogSettings());

        setForcePreviousAndNextButtons(true);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        fileSelectionPage = new TaxonomyFileSelectionPage(importModel);
        fileSelectionPage.setWizard(this);
        addPage(fileSelectionPage);
    }

    /**
     * Reset detail pages when import configuration changes.
     */
    public void resetDetailPages()
    {
        if (!detailPagesAdded)
            return;

        detailPages.clear();
        detailPagesAdded = false;

        if (getContainer().getCurrentPage() != fileSelectionPage)
            getContainer().showPage(fileSelectionPage);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page)
    {
        if (page == fileSelectionPage)
        {
            if (!detailPagesAdded && fileSelectionPage.isPageComplete())
            {
                addDetailPages();
            }

            if (!detailPages.isEmpty())
                return detailPages.get(0);
        }

        if (detailPages.contains(page))
        {
            var currentIndex = detailPages.indexOf(page);
            if (currentIndex >= 0 && currentIndex < detailPages.size() - 1)
                return detailPages.get(currentIndex + 1);
        }

        return super.getNextPage(page);
    }

    /**
     * Lazily add detail pages dynamically for each import that will be executed
     * (not skipped).
     */
    private void addDetailPages()
    {
        if (detailPagesAdded)
            return;

        // Remove any existing detail pages
        detailPages.clear();

        // Add detail pages for each import item that is not skipped
        for (var item : importModel.getImportItems())
        {
            if (item.getImportAction() != ImportAction.SKIP)
            {
                var detailPage = new TaxonomyDetailPage(stylingEngine, importModel, item);
                detailPages.add(detailPage);
                addPage(detailPage);
            }
        }

        detailPagesAdded = true;
    }

    @Override
    public boolean performFinish()
    {
        var isDirty = false;

        try
        {
            var importItems = importModel.getImportItems();

            var results = new ArrayList<Pair<Taxonomy, ImportResult>>();
            var newTaxonomies = new ArrayList<Taxonomy>();

            for (var item : importItems)
            {
                if (item.getImportAction() == ImportAction.SKIP)
                    continue;

                Taxonomy targetTaxonomy;

                if (item.getImportAction() == ImportAction.NEW)
                {
                    targetTaxonomy = new Taxonomy(item.getName());
                    targetTaxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), item.getName()));
                    newTaxonomies.add(targetTaxonomy);
                }
                else
                {
                    targetTaxonomy = item.getTargetTaxonomy();
                }

                var importer = new TaxonomyJSONImporter(importModel.getClient(), targetTaxonomy,
                                importModel.isPreserveNameAndDescription(), importModel.doPruneAbsentClassifications());

                var result = importer.importTaxonomy(item.getJsonData());

                if (result.hasChanges())
                {
                    results.add(new Pair<>(targetTaxonomy, result));
                    isDirty = true;
                }
            }

            for (var taxonomy : newTaxonomies)
            {
                importModel.getClient().addTaxonomy(taxonomy);
                isDirty = true;
            }

            if (!results.isEmpty())
                showSuccessMessage(results);

            return true;
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getShell(), Messages.LabelError, e.getMessage());
            return false;
        }
        finally
        {
            if (isDirty)
                importModel.getClient().markDirty();
        }
    }

    private void showSuccessMessage(List<Pair<Taxonomy, ImportResult>> results)
    {
        var message = new StringBuilder();

        message.append(Messages.MsgImportCompletedSuccessfully);

        for (var result : results)
        {
            message.append("\n\n"); //$NON-NLS-1$

            message.append(MessageFormat.format(Messages.LabelColonSeparated, result.getLeft().getName(),
                            MessageFormat.format(Messages.LabelAdditionsAndModifications,
                                            result.getRight().getCreatedObjects(),
                                            result.getRight().getModifiedObjects())));
        }

        MessageDialog.openInformation(getShell(), Messages.MsgImportCompletedSuccessfully, message.toString());
    }
}

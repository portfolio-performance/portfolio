package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ImportResult;
import name.abuchen.portfolio.ui.Messages;

/* package */ class TaxonomyImportModel
{
    public enum ImportAction
    {
        NEW(Messages.CmdCreateNewTaxonomy), UPDATE(Messages.CmdUpdate), SKIP(Messages.CmdDoNotImport);

        private final String label;

        ImportAction(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    public static class ImportItem
    {
        private final String name;
        private final Map<String, Object> jsonData;

        /**
         * The action defines *what* to do during the import
         */
        private ImportAction importAction;

        /**
         * The target taxonomy upon *which* taxonomy to apply the import action
         */
        private Taxonomy targetTaxonomy;

        /**
         * The result of the dry-run import.
         */
        private ImportResult dryrunResult;

        /**
         * The resulting taxonomy after applying the dry-run import.
         */
        private Taxonomy dryrunTaxonomy;

        /**
         * Error messages recorded during the dry-run import.
         */
        private String errorMessage;

        public ImportItem(Map<String, Object> jsonData)
        {
            this.jsonData = jsonData;
            var taxonomyName = (String) jsonData.get("name"); //$NON-NLS-1$
            this.name = taxonomyName != null ? taxonomyName : Messages.LabelUnknown;
            this.importAction = ImportAction.NEW; // Default action
        }

        public String getName()
        {
            return name;
        }

        public Map<String, Object> getJsonData()
        {
            return jsonData;
        }

        public ImportAction getImportAction()
        {
            return importAction;
        }

        public void setImportAction(ImportAction importAction)
        {
            this.importAction = importAction;
        }

        public Taxonomy getTargetTaxonomy()
        {
            return targetTaxonomy;
        }

        public void setTargetTaxonomy(Taxonomy targetTaxonomy)
        {
            this.targetTaxonomy = targetTaxonomy;
        }

        public ImportResult getDryrunResult()
        {
            return dryrunResult;
        }

        public Taxonomy getDryrunTaxonomy()
        {
            return dryrunTaxonomy;
        }

        public void setImportResult(ImportResult importResult, Taxonomy dryrunTaxonomy)
        {
            this.dryrunResult = importResult;
            this.dryrunTaxonomy = dryrunTaxonomy;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
        }

        /**
         * Gets the display text for the changes summary.
         * 
         * @return Formatted string showing additions and modifications, or
         *         empty string if no result
         */
        public String getChangesText()
        {
            if (errorMessage != null)
                return errorMessage;

            if (dryrunResult == null)
                return ""; //$NON-NLS-1$

            return MessageFormat.format(Messages.LabelAdditionsAndModifications, dryrunResult.getCreatedObjects(),
                            dryrunResult.getModifiedObjects());
        }
    }

    /* package */ static final String PREF_PRESERVE_NAME_DESCRIPTION = TaxonomyImportDialog.class.getSimpleName()
                    + "-preserve.name.description"; //$NON-NLS-1$
    /* package */ static final String PREF_PRUNE_ABSENT_CLASSIFICATIONS = TaxonomyImportDialog.class.getSimpleName()
                    + "-replace.mode"; //$NON-NLS-1$

    private final Client client;
    private final IPreferenceStore preferences;

    private boolean preserveNameAndDescription;
    private boolean pruneAbsentClassifications;
    private List<ImportItem> importItems = new ArrayList<>();

    public TaxonomyImportModel(Client client, IPreferenceStore preferences)
    {
        this.client = client;
        this.preferences = preferences;
        this.preserveNameAndDescription = preferences.getBoolean(PREF_PRESERVE_NAME_DESCRIPTION);
        this.pruneAbsentClassifications = preferences.getBoolean(PREF_PRUNE_ABSENT_CLASSIFICATIONS);
    }

    public Client getClient()
    {
        return client;
    }

    public boolean isPreserveNameAndDescription()
    {
        return preserveNameAndDescription;
    }

    public void setPreserveNameAndDescription(boolean preserveNameAndDescription)
    {
        this.preserveNameAndDescription = preserveNameAndDescription;
        preferences.setValue(TaxonomyImportModel.PREF_PRESERVE_NAME_DESCRIPTION, preserveNameAndDescription);
    }

    public boolean doPruneAbsentClassifications()
    {
        return pruneAbsentClassifications;
    }

    public void setPruneAbsentClassifications(boolean pruneAbsentClassifications)
    {
        this.pruneAbsentClassifications = pruneAbsentClassifications;
        preferences.setValue(TaxonomyImportModel.PREF_PRUNE_ABSENT_CLASSIFICATIONS, pruneAbsentClassifications);
    }

    public List<ImportItem> getImportItems()
    {
        return importItems;
    }

    public void setImportItems(List<ImportItem> newItems)
    {
        this.importItems = newItems;
    }
}

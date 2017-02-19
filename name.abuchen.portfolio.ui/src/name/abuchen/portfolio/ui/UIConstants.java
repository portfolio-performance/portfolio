package name.abuchen.portfolio.ui;

public interface UIConstants
{
    interface Part
    {
        String PORTFOLIO = "name.abuchen.portfolio.ui.part.portfolio"; //$NON-NLS-1$
        String ERROR_LOG = "name.abuchen.portfolio.ui.part.errorlog"; //$NON-NLS-1$
        String TEXT_VIEWER = "name.abuchen.portfolio.ui.part.textviewer"; //$NON-NLS-1$
    }

    interface PartStack
    {
        String MAIN = "name.abuchen.portfolio.ui.partstack.main"; //$NON-NLS-1$
    }

    interface Event
    {
        interface Log
        {
            String CREATED = "errorlog/created"; //$NON-NLS-1$
            String CLEARED = "errorlog/cleared"; //$NON-NLS-1$
        }

        interface File
        {
            String ALL_SUB_TOPICS = "file/*"; //$NON-NLS-1$
            String OPENED = "file/opened"; //$NON-NLS-1$
            String SAVED = "file/saved"; //$NON-NLS-1$
            String REMOVED = "file/removed"; //$NON-NLS-1$
        }

        interface ExchangeRates
        {
            String LOADED = "exchangeRates/loaded"; //$NON-NLS-1$
        }
    }

    interface File
    {
        String ENCRYPTED_EXTENSION = "portfolio"; //$NON-NLS-1$
        String PERSISTED_STATE_KEY = "file"; //$NON-NLS-1$
    }

    /**
     * Command names defined in application.e4xmi
     */
    interface Command
    {
        String OPEN_RECENT_FILE = "name.abuchen.portfolio.ui.command.openRecentFile"; //$NON-NLS-1$
    }

    /**
     * Parameter keys used in application.e4xmi
     */
    interface Parameter
    {
        String PART = "name.abuchen.portfolio.ui.param.part"; //$NON-NLS-1$
        String FILE = "name.abuchen.portfolio.ui.param.file"; //$NON-NLS-1$
        String EXTENSION = "name.abuchen.portfolio.ui.param.extension"; //$NON-NLS-1$
        String ENCRYPTION_METHOD = "name.abuchen.portfolio.ui.param.encryptionmethod"; //$NON-NLS-1$
        String SAMPLE_FILE = "name.abuchen.portfolio.ui.param.samplefile"; //$NON-NLS-1$
    }

    interface Preferences
    {
        String UPDATE_SITE = "UPDATE_SITE"; //$NON-NLS-1$
        String AUTO_UPDATE = "AUTO_UPDATE"; //$NON-NLS-1$

        String PROXY_HOST = "PROXY_HOST"; //$NON-NLS-1$
        String PROXY_PORT = "PROXY_PORT"; //$NON-NLS-1$

        /**
         * Preference key to use indirect quotation ("Mengennotierung") when
         * displaying exchange rates.
         */
        String USE_INDIRECT_QUOTATION = "USE_INDIRECT_QUOTATION"; //$NON-NLS-1$

        /**
         * Preference key whether to create a backup of the original file before
         * saving. The backup file has the postfix ".backup".
         */
        String CREATE_BACKUP_BEFORE_SAVING = "CREATE_BACKUP_BEFORE_SAVING"; //$NON-NLS-1$

        /**
         * Preference key to store a comma-separated list of recent files
         */
        String RECENT_FILES = "RECENT_FILES"; //$NON-NLS-1$
    }
}

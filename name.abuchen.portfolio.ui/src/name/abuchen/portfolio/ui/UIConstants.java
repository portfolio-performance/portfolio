package name.abuchen.portfolio.ui;

public interface UIConstants
{
    interface Part // NOSONAR
    {
        String PORTFOLIO = "name.abuchen.portfolio.ui.part.portfolio"; //$NON-NLS-1$
        String ERROR_LOG = "name.abuchen.portfolio.ui.part.errorlog"; //$NON-NLS-1$
        String TEXT_VIEWER = "name.abuchen.portfolio.ui.part.textviewer"; //$NON-NLS-1$
    }

    interface PartStack // NOSONAR
    {
        String MAIN = "name.abuchen.portfolio.ui.partstack.main"; //$NON-NLS-1$
    }

    interface Event
    {
        interface Log // NOSONAR
        {
            String CREATED = "errorlog/created"; //$NON-NLS-1$
            String CLEARED = "errorlog/cleared"; //$NON-NLS-1$
        }

        interface File // NOSONAR
        {
            String ALL_SUB_TOPICS = "file/*"; //$NON-NLS-1$
            String OPENED = "file/opened"; //$NON-NLS-1$
            String SAVED = "file/saved"; //$NON-NLS-1$
            String REMOVED = "file/removed"; //$NON-NLS-1$
        }

        interface ExchangeRates // NOSONAR
        {
            String LOADED = "exchangeRates/loaded"; //$NON-NLS-1$
        }
    }

    interface File // NOSONAR
    {
        String ENCRYPTED_EXTENSION = "portfolio"; //$NON-NLS-1$
    }

    /**
     * State persisted for portfolio parts in application model
     */
    interface PersistedState // NOSONAR
    {
        String FILENAME = "file"; //$NON-NLS-1$
        String REPORTING_PERIOD = "reporting-period"; //$NON-NLS-1$

        /**
         * Last view displayed before the part was destroyed
         */
        String VIEW = "view"; //$NON-NLS-1$

        /**
         * View to open initially after restoring part (if set)
         */
        String INITIAL_VIEW = "initial-view"; //$NON-NLS-1$
    }

    /**
     * Command names defined in application.e4xmi
     */
    interface Command // NOSONAR
    {
        String OPEN_RECENT_FILE = "name.abuchen.portfolio.ui.command.openRecentFile"; //$NON-NLS-1$
        String IMPORT_CSV = "name.abuchen.portfolio.ui.command.import"; //$NON-NLS-1$
        String PREFERENCES = "org.eclipse.ui.window.preferences"; //$NON-NLS-1$
    }

    /**
     * Parameter keys used in application.e4xmi
     */
    interface Parameter // NOSONAR
    {
        String PART = "name.abuchen.portfolio.ui.param.part"; //$NON-NLS-1$
        String FILE = "name.abuchen.portfolio.ui.param.file"; //$NON-NLS-1$
        String EXTENSION = "name.abuchen.portfolio.ui.param.extension"; //$NON-NLS-1$
        String ENCRYPTION_METHOD = "name.abuchen.portfolio.ui.param.encryptionmethod"; //$NON-NLS-1$
        String SAMPLE_FILE = "name.abuchen.portfolio.ui.param.samplefile"; //$NON-NLS-1$
        String NAME = "name.abuchen.portfolio.ui.param.name"; //$NON-NLS-1$
        String VIEW_PARAMETER = "name.abuchen.portfolio.ui.param.viewparameter"; //$NON-NLS-1$
        String URL = "name.abuchen.portfolio.ui.param.url"; //$NON-NLS-1$
    }

    interface Preferences // NOSONAR
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

        /**
         * Preference key whether to automatically update quotes after opening a
         * data file.
         */
        String UPDATE_QUOTES_AFTER_FILE_OPEN = "UPDATE_QUOTES_AFTER_FILE_OPEN"; //$NON-NLS-1$

        /**
         * Preference key whether to automatically update quotes after opening a
         * data file.
         */
        String AUTO_SAVE_FILE = "AUTO_SAVE_FILE"; //$NON-NLS-1$

        /**
         * Preference key whether to store settings (column width, last expanded
         * tree nodes, etc.) next to the data file as opposed to in the
         * workspace folder
         */
        String STORE_SETTINGS_NEXT_TO_FILE = "STORE_SETTINGS_NEXT_TO_FILE"; //$NON-NLS-1$

        String ALPHAVANTAGE_API_KEY = "ALPHAVANTAGE_API_KEY"; //$NON-NLS-1$
        String ALPHAVANTAGE_CALL_FREQUENCY_LIMIT = "ALPHAVANTAGE_CALL_FREQUENCY_LIMIT"; //$NON-NLS-1$

        String QUANDL_API_KEY = "QUANDL_API_KEY"; //$NON-NLS-1$

        String FINNHUB_API_KEY = "FINNHUB_API_KEY"; //$NON-NLS-1$

        String DIVVYDIARY_API_KEY = "DIVVYDIARY_API_KEY"; //$NON-NLS-1$

        /**
         * Preference key whether to store settings (standard calendar)
         */
        String CALENDAR = "CALENDAR"; //$NON-NLS-1$
    }

    interface CSS
    {
        String CLASS_NAME = "org.eclipse.e4.ui.css.CssClassName"; //$NON-NLS-1$
    }
}

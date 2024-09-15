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
        
        interface RecentFiles // NOSONAR
        {
            String UPDATED = "recent/updated"; //$NON-NLS-1$
        }

        interface ExchangeRates // NOSONAR
        {
            String LOADED = "exchangeRates/loaded"; //$NON-NLS-1$
        }

        interface Global // NOSONAR
        {
            String DISCREET_MODE = "global/discreet-mode"; //$NON-NLS-1$
        }
    }

    interface Context // NOSONAR
    {
        String FILTERED_CLIENT = "FILTERED_CLIENT"; //$NON-NLS-1$

        String ACTIVE_CLIENT = "ACTIVE_CLIENT"; //$NON-NLS-1$
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
        String OPEN_BROWSER = "name.abuchen.portfolio.ui.command.openBrowser"; //$NON-NLS-1$
        String OPEN_RECENT_FILE = "name.abuchen.portfolio.ui.command.openRecentFile"; //$NON-NLS-1$
        String NEW_DOMAIN_ELEMENT = "name.abuchen.portfolio.ui.command.newDomainElement"; //$NON-NLS-1$
        String IMPORT_CSV = "name.abuchen.portfolio.ui.command.import"; //$NON-NLS-1$
        String PREFERENCES = "org.eclipse.ui.window.preferences"; //$NON-NLS-1$
        String UPDATE_QUOTES = "name.abuchen.portfolio.ui.command.updatequotes"; //$NON-NLS-1$
    }

    /**
     * Parameter keys used in application.e4xmi
     */
    interface Parameter // NOSONAR
    {
        String PART = "name.abuchen.portfolio.ui.param.part"; //$NON-NLS-1$
        String FILE = "name.abuchen.portfolio.ui.param.file"; //$NON-NLS-1$
        String FILE_TYPE = "name.abuchen.portfolio.ui.param.file-type"; //$NON-NLS-1$
        String ENCRYPTION_METHOD = "name.abuchen.portfolio.ui.param.encryptionmethod"; //$NON-NLS-1$
        String SAMPLE_FILE = "name.abuchen.portfolio.ui.param.samplefile"; //$NON-NLS-1$
        String NAME = "name.abuchen.portfolio.ui.param.name"; //$NON-NLS-1$
        String VIEW_PARAMETER = "name.abuchen.portfolio.ui.param.viewparameter"; //$NON-NLS-1$
        String URL = "name.abuchen.portfolio.ui.param.url"; //$NON-NLS-1$
        String TAG = "name.abuchen.portfolio.ui.param.tag"; //$NON-NLS-1$
        String PAGE = "name.abuchen.portfolio.ui.param.page"; //$NON-NLS-1$
        String TYPE = "name.abuchen.portfolio.ui.param.type"; //$NON-NLS-1$
        String FILTER = "name.abuchen.portfolio.ui.param.filter"; //$NON-NLS-1$
    }

    /**
     * Well-known element ids from the application e4xmi file
     */
    interface ElementId // NOSONAR
    {
        String MENU_ITEM_UPDATE_QUOTES_SELECTED_SECURITIES = "name.abuchen.portfolio.ui.menu.online.updatequotes.selectedsecurities"; //$NON-NLS-1$
    }

    interface Preferences // NOSONAR
    {
        String UPDATE_SITE = "UPDATE_SITE"; //$NON-NLS-1$
        String AUTO_UPDATE = "AUTO_UPDATE"; //$NON-NLS-1$

        String PROXY_HOST = "PROXY_HOST"; //$NON-NLS-1$
        String PROXY_PORT = "PROXY_PORT"; //$NON-NLS-1$

        /**
         * Preference key for display precision of share amounts
         */
        String FORMAT_SHARES_DIGITS = "FORMAT_SHARES_DIGITS"; //$NON-NLS-1$

        /*
         * Preference key for display precision of quote values
         */
        String FORMAT_CALCULATED_QUOTE_DIGITS = "FORMAT_CALCULATED_QUOTE_DIGITS"; //$NON-NLS-1$

        /**
         * Preference key to use indirect quotation ("Mengennotierung") when
         * displaying exchange rates.
         */
        String USE_INDIRECT_QUOTATION = "USE_INDIRECT_QUOTATION"; //$NON-NLS-1$

        /**
         * Preference key to display the currency code also for the reporting
         * currency.
         */
        String ALWAYS_DISPLAY_CURRENCY_CODE = "ALWAYS_DISPLAY_CURRENCY_CODE"; //$NON-NLS-1$

        /**
         * Add "p.a." to annualized return rates 
         */
        String DISPLAY_PER_ANNUM = "DISPLAY_PER_ANNUM"; //$NON-NLS-1$

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

        String ENABLE_EXPERIMENTAL_FEATURES = "ENABLE_EXPERIMENTAL_FEATURES"; //$NON-NLS-1$

        /**
         * Preference key used in 2023 to indicate whether the survey is active.
         * Do not reuse!
         */
        @Deprecated
        String ENABLE_SURVEY_REMINDER = "ENABLE_SURVEY_REMINDER"; //$NON-NLS-1$

        String ENABLE_SWTCHART_PIECHARTS = "ENABLE_SWTCHART_PIECHARTS"; //$NON-NLS-1$

        String ALPHAVANTAGE_API_KEY = "ALPHAVANTAGE_API_KEY"; //$NON-NLS-1$
        String ALPHAVANTAGE_CALL_FREQUENCY_LIMIT = "ALPHAVANTAGE_CALL_FREQUENCY_LIMIT"; //$NON-NLS-1$

        String QUANDL_API_KEY = "QUANDL_API_KEY"; //$NON-NLS-1$

        String FINNHUB_API_KEY = "FINNHUB_API_KEY"; //$NON-NLS-1$

        String LEEWAY_API_KEY = "LEEWAY_API_KEY"; //$NON-NLS-1$

        String TWELVEDATA_API_KEY = "TWELVEDATA_API_KEY"; //$NON-NLS-1$
        
        String DIVVYDIARY_API_KEY = "DIVVYDIARY_API_KEY"; //$NON-NLS-1$

        String MYDIVIDENDS24_API_KEY = "MYDIVIDENDS24_API_KEY"; //$NON-NLS-1$

        String EOD_HISTORICAL_DATA_API_KEY = "EOD_HISTORICAL_DATA_API_KEY"; //$NON-NLS-1$

        String COINGECKO_API_KEY = "COINGECKO_API_KEY"; //$NON-NLS-1$
        
        String PORTFOLIO_REPORT_API_KEY = "PORTFOLIO_REPORT_API_KEY"; //$NON-NLS-1$
        String PORTFOLIO_REPORT_API_URL = "PORTFOLIO_REPORT_API_URL"; //$NON-NLS-1$

        /**
         * Preference key whether to store settings (standard calendar)
         */
        String CALENDAR = "CALENDAR"; //$NON-NLS-1$

        /**
         * Preference key for preset time in new transactions.
         */
        String PRESET_VALUE_TIME = "PRESET_VALUE_TIME"; //$NON-NLS-1$

        /**
         * Preference key for the mode of the backup
         */
        String BACKUP_MODE = "BACKUP_MODE"; //$NON-NLS-1$

        /**
         * Preference key for the backup folder relative to the data file
         */
        String BACKUP_FOLDER_RELATIVE = "BACKUP_FOLDER_RELATIVE"; //$NON-NLS-1$

        /**
         * Preference key for the absolute backup
         */
        String BACKUP_FOLDER_ABSOLUTE = "BACKUP_FOLDER_ABSOLUTE"; //$NON-NLS-1$

        /**
         * Preference for directory from which to open data files
         */
        String DEFAULT_OPEN_PATH = "DEFAULT_OPEN_PATH"; //$NON-NLS-1$

        /**
         * Preference for directory from which to import PDF files
         */
        String PDF_IMPORT_PATH = "PDF_IMPORT_PATH"; //$NON-NLS-1$

        /**
         * Preference for directory from which to import CSV files
         */
        String CSV_IMPORT_PATH = "CSV_IMPORT_PATH"; //$NON-NLS-1$

        /**
         * Preference for directory from which to export CSV files
         */
        String CSV_EXPORT_PATH = "CSV_EXPORT_PATH"; //$NON-NLS-1$

    }

    interface Theme // NOSONAR
    {
        String LIGHT = "name.abuchen.portfolio.light"; //$NON-NLS-1$
        String DARK = "name.abuchen.portfolio.dark"; //$NON-NLS-1$
    }

    interface CSS // NOSONAR
    {
        String CLASS_NAME = "org.eclipse.e4.ui.css.CssClassName"; //$NON-NLS-1$
        String DISABLE_CSS_STYLING = "org.eclipse.e4.ui.css.disabled"; //$NON-NLS-1$
        
        String HEADING1 = "heading1"; //$NON-NLS-1$
        String HEADING2 = "heading2"; //$NON-NLS-1$
        String KPI = "kpi"; //$NON-NLS-1$
        String TITLE = "title"; //$NON-NLS-1$
        String DATAPOINT = "datapoint"; //$NON-NLS-1$
    }

    interface Tag // NOSONAR
    {
        String SIDEBAR = "sidebar"; //$NON-NLS-1$
        String INFORMATIONPANE = "informationpane"; //$NON-NLS-1$
    }
}

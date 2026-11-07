package name.abuchen.portfolio.cli.command;

/**
 * An error code the tool can fail with; <code>--help</code> publishes it.
 * {@link CliException} and {@link FieldFailure} take only these constants, so
 * the set of codes is closed at compile time. A code that a facet declares is
 * the <code>code</code> of a problem document; one that a write operation or
 * the batch declares (see {@link OperationSpec#failureCodes()}) is the
 * <code>code</code> of an entry of a <code>validation-failed</code>
 * problem's <code>failures</code>. A code can be both. The scope says which
 * commands can return it, so that a command's help lists only its own codes.
 * No command or operation is named here: a {@link Scope#BY_USE} code belongs
 * to the commands whose facets declare it or whose option types imply it, and
 * to the operations that declare it.
 */
public enum ErrorCode
{
    // invocation
    UNKNOWN_COMMAND("unknown-command", "The command does not exist; the detail lists the known commands.",
                    Scope.GLOBAL),
    UNEXPECTED_ARGUMENT("unexpected-argument", "An argument is not an option; options look like --name value.",
                    Scope.EVERY_COMMAND),
    UNKNOWN_OPTION("unknown-option", "The command has no such option.", Scope.EVERY_COMMAND),
    OPTION_VALUE_REQUIRED("option-value-required", "An option that takes a value was given without one.",
                    Scope.EVERY_COMMAND),
    OPTION_REQUIRED("option-required", "A required option is missing, or an option needs another option.",
                    Scope.BY_USE),
    INVALID_OPTION_VALUE("invalid-option-value",
                    "An option value is not accepted, including an unknown operation after apply --help; the detail states the expected value.",
                    Scope.EVERY_COMMAND),
    CONFLICTING_OPTIONS("conflicting-options", "Options were combined that can't be used together.", Scope.BY_USE),
    INVALID_DATE_RANGE("invalid-date-range", "The start of the period is after its end.", Scope.BY_USE),
    PERIOD_REQUIRED("period-required", "performance needs --from or --period.", Scope.BY_USE),

    // client file
    FILE_REQUIRED("file-required", "No client file was given: pass --file or set PP_FILE.", Scope.BY_USE),
    FILE_NOT_FOUND("file-not-found", "There is no client file at the given path.", Scope.BY_USE),
    FILE_UNREADABLE("file-unreadable", "The client file exists but could not be read or parsed.", Scope.BY_USE),
    FILE_VERSION_UNSUPPORTED("file-version-unsupported",
                    "The client file is newer than this CLI supports (fileVersion > maxSupportedVersion),"
                                    + " usually because the desktop app is newer than the CLI's core."
                                    + " Rebuild the CLI from the matching upstream tag.",
                    Scope.BY_USE),
    PASSWORD_REQUIRED("password-required",
                    "The client file is encrypted and no password was given: set PP_PASSWORD.", Scope.BY_USE),
    INCORRECT_PASSWORD("incorrect-password", "The client file could not be decrypted with the given password.",
                    Scope.BY_USE),

    // references
    REFERENCE_NOT_FOUND("reference-not-found", "No entity of the expected kind matches the reference.",
                    Scope.BY_USE),
    REFERENCE_AMBIGUOUS("reference-ambiguous",
                    "The reference matches several entities; candidates lists their UUIDs.", Scope.BY_USE),
    NO_REFERENCE_CASH_ACCOUNT("no-reference-cash-account",
                    "The investment account has no reference cash account to include.", Scope.BY_USE),

    // writes
    INPUT_UNREADABLE("input-unreadable", "The --input file could not be read.", Scope.BY_USE),
    INVALID_INPUT("invalid-input", "The --input batch is not valid JSON or not shaped as a batch.", Scope.BY_USE),
    VALIDATION_FAILED("validation-failed",
                    "One or more operations of the batch failed validation; failures lists every one"
                                    + " with its operationIndex, field and code. Nothing was written.",
                    Scope.BY_USE),
    CONSISTENCY_CHECK_FAILED("consistency-check-failed",
                    "The change would introduce inconsistencies into the client file. Nothing was written.",
                    Scope.BY_USE),
    FILE_OPEN_IN_APP("file-open-in-app",
                    "Another process, probably the Portfolio Performance app, has the client file open."
                                    + " Nothing was written; close the file there first.",
                    Scope.BY_USE),
    FILE_LOCKED("file-locked",
                    "Another pp-cli is writing the client file, or crashed while doing so; the detail names the lock file.",
                    Scope.BY_USE),
    FILE_CHANGED("file-changed",
                    "The client file changed on disk after it was read. Nothing was written; run the command again.",
                    Scope.BY_USE),
    WRITE_FAILED("write-failed", "The client file or its lock file could not be written. The file was not changed.",
                    Scope.BY_USE),
    BACKUP_NOT_FOUND("backup-not-found", "There is no backup file at the given path.", Scope.BY_USE),

    // failures of write operations: failures[].code of validation-failed

    // the batch
    INVALID_OPERATION("invalid-operation", "An operation is not a JSON object.", Scope.BY_USE),
    UNKNOWN_OPERATION("unknown-operation", "The operation member is missing or names no operation.", Scope.BY_USE),

    // implied by an operation's input schema
    UNKNOWN_FIELD("unknown-field", "The object has no such field.", Scope.BY_USE),
    REQUIRED("required", "A required field is missing or empty.", Scope.BY_USE),
    INVALID_TYPE("invalid-type", "A field has the wrong JSON type.", Scope.BY_USE),
    NOT_POSITIVE("not-positive", "A number that must be positive is not.", Scope.BY_USE),
    INVALID_AMOUNT("invalid-amount", "More decimal places than are stored.", Scope.BY_USE),
    DUPLICATE_TARGET("duplicate-target", "An earlier operation changes the entity.", Scope.BY_USE),

    // master data
    INVALID_CURRENCY("invalid-currency", "Not a known ISO currency code.", Scope.BY_USE),
    INVALID_ISIN("invalid-isin", "Not a valid ISIN.", Scope.BY_USE),
    DUPLICATE_ISIN("duplicate-isin", "Another instrument has the ISIN.", Scope.BY_USE),
    CURRENCY_IMMUTABLE("currency-immutable", "The currency can't change: there are transactions.", Scope.BY_USE),

    // taxonomy assignments
    NOT_A_LEAF("not-a-leaf", "The classification has children.", Scope.BY_USE),
    DUPLICATE_CLASSIFICATION("duplicate-classification", "The classification is listed twice.", Scope.BY_USE),
    WEIGHT_TOTAL_EXCEEDED("weight-total-exceeded", "The weights add up to more than 100%.", Scope.BY_USE),

    // transaction records: the type, the rule table's refusals and the rules across fields
    UNSUPPORTED_TYPE("unsupported-type", "Not a transaction type an add takes.", Scope.BY_USE),
    UNSUPPORTED_KIND("unsupported-kind", "Not a kind of transfer.", Scope.BY_USE),
    TYPE_IMMUTABLE("type-immutable", "An update can't change the type.", Scope.BY_USE),
    KIND_IMMUTABLE("kind-immutable", "An update can't change the kind.", Scope.BY_USE),
    RECORD_INCOMPLETE("record-incomplete", "The record lacks a side; delete it instead.", Scope.BY_USE),
    KIND_NOT_ALLOWED("kind-not-allowed", "kind is refused.", Scope.BY_USE),
    TARGET_AMOUNT_NOT_ALLOWED("target-amount-not-allowed", "targetAmount is refused.", Scope.BY_USE),
    EXCHANGE_RATE_NOT_ALLOWED("exchange-rate-not-allowed", "exchangeRate is refused.", Scope.BY_USE),
    INSTRUMENT_NOT_ALLOWED("instrument-not-allowed", "instrument is refused.", Scope.BY_USE),
    SHARES_NOT_ALLOWED("shares-not-allowed", "shares is refused.", Scope.BY_USE),
    INVESTMENT_ACCOUNT_NOT_ALLOWED("investment-account-not-allowed", "investmentAccount is refused.",
                    Scope.BY_USE),
    CASH_ACCOUNT_NOT_ALLOWED("cash-account-not-allowed", "cashAccount is refused.", Scope.BY_USE),
    FROM_NOT_ALLOWED("from-not-allowed", "from is refused.", Scope.BY_USE),
    TO_NOT_ALLOWED("to-not-allowed", "to is refused.", Scope.BY_USE),
    EXDATE_NOT_ALLOWED("exdate-not-allowed", "exDate is refused.", Scope.BY_USE),
    GROSS_AMOUNT_NOT_ALLOWED("gross-amount-not-allowed", "grossAmount is refused.", Scope.BY_USE),
    UNITS_NOT_ALLOWED("units-not-allowed", "units is refused.", Scope.BY_USE),
    INVALID_DATE("invalid-date", "Not an ISO date (yyyy-MM-dd).", Scope.BY_USE),
    INVALID_TIME("invalid-time", "Not a time of day (HH:mm).", Scope.BY_USE),
    CURRENCY_MISMATCH("currency-mismatch", "Not the currency the record needs.",
                    Scope.BY_USE),
    GROSS_AMOUNT_MISMATCH("gross-amount-mismatch", "grossAmount does not reconcile.",
                    Scope.BY_USE),
    EXDATE_AFTER_PAYMENT("exdate-after-payment", "The ex-date is after the date.", Scope.BY_USE),
    UNSUPPORTED_UNIT_TYPE("unsupported-unit-type", "Not a unit type.", Scope.BY_USE),
    FOREX_REQUIRED("forex-required", "A GROSS_VALUE unit needs forex.", Scope.BY_USE),
    FOREX_INCOMPLETE("forex-incomplete", "Forex fields given only in part.", Scope.BY_USE),
    FOREX_CURRENCY_MISMATCH("forex-currency-mismatch", "The forex is not in the instrument's currency.",
                    Scope.BY_USE),
    FOREX_MISMATCH("forex-mismatch", "The exchange rate does not fit the amounts.", Scope.BY_USE),
    FOREX_GROSS_VALUE_REQUIRED("forex-gross-value-required",
                    "The instrument's currency needs a GROSS_VALUE unit.", Scope.BY_USE),
    SAME_ACCOUNT("same-account", "A transfer names the same account twice.", Scope.BY_USE),
    IMPORT_CHECK_FAILED("import-check-failed", "Core's import check refused the record; check names it.",
                    Scope.BY_USE),
    NEGATIVE_HOLDING("negative-holding", "A holding would go negative.", Scope.BY_USE),

    INTERNAL_ERROR("internal-error", "An unexpected failure inside the tool.", Scope.EVERY_COMMAND);

    /** Where an error code can occur. */
    public enum Scope
    {
        /** Only before a command is chosen; listed in the global help. */
        GLOBAL,
        /** Any command. */
        EVERY_COMMAND,
        /**
         * The commands that use what raises it: a facet that declares it, or
         * an option whose type implies it (see {@link CommandSpec#errorCodes()});
         * or the write operations that declare it.
         */
        BY_USE
    }

    private final String code;
    private final String description;
    private final Scope scope;

    ErrorCode(String code, String description, Scope scope)
    {
        this.code = code;
        this.description = description;
        this.scope = scope;
    }

    /** The published code, as it appears in a problem document. */
    public String code()
    {
        return code;
    }

    /** The code's one generic meaning, which every help that lists the code shows. */
    public String description()
    {
        return description;
    }

    public Scope scope()
    {
        return scope;
    }
}

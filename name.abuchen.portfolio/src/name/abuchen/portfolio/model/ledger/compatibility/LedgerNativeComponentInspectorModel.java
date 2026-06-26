package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Collects read-only display rows for inspecting a Ledger entry.
 * This class is part of the compatibility layer used by UI code that must show
 * ledger-backed projections without opening internal Ledger model packages.
 * It reads Ledger facts and optional Java-only native leg configuration metadata, but it never mutates them.
 */
public final class LedgerNativeComponentInspectorModel
{
    public enum HeaderField
    {
        ENTRY_TYPE,
        ENTRY_UUID,
        DATE_TIME,
        NOTE,
        SOURCE,
        SHAPE,
        NATIVE_TARGETED,
        SELECTED_PROJECTION_ROLE,
        SELECTED_PROJECTION_UUID,
        SELECTED_PRIMARY_POSTING_UUID,
        SELECTED_POSTING_GROUP_UUID
    }

    public record HeaderRow(HeaderField field, String value)
    {
    }

    public record ParameterRow(String parameter, String code, String valueKind, String value, String domain)
    {
    }

    public record LegRow(String legRole, String postingType, String cardinality, String projectionRole,
                    String primaryExpected, String groupExpected)
    {
    }

    public record PostingRow(String postingUUID, String postingType, String amount, String currency, String security,
                    String shares, String account, String portfolio)
    {
    }

    public record PostingParameterRow(String postingUUID, String postingType, String parameter, String code,
                    String valueKind, String value, String domain)
    {
    }

    public record ProjectionRefRow(String projectionRole, String owner, String projectionUUID,
                    String primaryPostingUUID, String postingGroupUUID)
    {
    }

    private final List<HeaderRow> headerRows;
    private final List<ParameterRow> entryParameters;
    private final List<LegRow> legs;
    private final List<PostingRow> postings;
    private final List<PostingParameterRow> postingParameters;
    private final List<ProjectionRefRow> projectionRefs;
    private final boolean nativeEntryDefinitionAvailable;

    private LedgerNativeComponentInspectorModel(List<HeaderRow> headerRows, List<ParameterRow> entryParameters,
                    List<LegRow> legs, List<PostingRow> postings, List<PostingParameterRow> postingParameters,
                    List<ProjectionRefRow> projectionRefs, boolean nativeEntryDefinitionAvailable)
    {
        this.headerRows = List.copyOf(headerRows);
        this.entryParameters = List.copyOf(entryParameters);
        this.legs = List.copyOf(legs);
        this.postings = List.copyOf(postings);
        this.postingParameters = List.copyOf(postingParameters);
        this.projectionRefs = List.copyOf(projectionRefs);
        this.nativeEntryDefinitionAvailable = nativeEntryDefinitionAvailable;
    }

    public static Optional<LedgerNativeComponentInspectorModel> from(Object transaction)
    {
        if (transaction instanceof LedgerBackedTransaction ledgerBackedTransaction)
            return from(ledgerBackedTransaction);

        return Optional.empty();
    }

    public static boolean isLedgerBackedProjection(Object transaction)
    {
        return transaction instanceof LedgerBackedTransaction;
    }

    public static boolean isLedgerNativeTargetedProjection(Object transaction)
    {
        return transaction instanceof LedgerBackedTransaction ledgerBackedTransaction
                        && ledgerBackedTransaction.getLedgerEntry().getType() != null
                        && ledgerBackedTransaction.getLedgerEntry().getType().isLedgerNativeTargeted();
    }

    static Optional<LedgerNativeComponentInspectorModel> from(LedgerBackedTransaction transaction)
    {
        Objects.requireNonNull(transaction);
        return from(transaction.getLedgerEntry(), transaction.getLedgerProjectionRef(), LedgerEntryDefinitionRegistry::lookup);
    }

    static Optional<LedgerNativeComponentInspectorModel> from(LedgerEntry entry, LedgerProjectionRef selectedProjectionRef,
                    Function<LedgerEntryType, Optional<LedgerEntryDefinition>> definitionLookup)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(definitionLookup);

        if (entry.getType() == null)
            return Optional.empty();

        var definition = entry.getType().isLedgerNativeTargeted() ? definitionLookup.apply(entry.getType())
                        : Optional.<LedgerEntryDefinition>empty();
        var nativeEntryDefinitionAvailable = definition.isPresent();

        return Optional.of(new LedgerNativeComponentInspectorModel(headerRows(entry, selectedProjectionRef),
                        parameterRows(entry.getParameters()),
                        definition.map(d -> legRows(d.getLegDefinitions())).orElseGet(List::of),
                        postingRows(entry.getPostings()), postingParameterRows(entry.getPostings()),
                        projectionRefRows(entry.getProjectionRefs()), nativeEntryDefinitionAvailable));
    }

    public List<HeaderRow> getHeaderRows()
    {
        return headerRows;
    }

    public List<ParameterRow> getEntryParameters()
    {
        return entryParameters;
    }

    public List<LegRow> getLegs()
    {
        return legs;
    }

    public List<PostingRow> getPostings()
    {
        return postings;
    }

    public List<PostingParameterRow> getPostingParameters()
    {
        return postingParameters;
    }

    public List<ProjectionRefRow> getProjectionRefs()
    {
        return projectionRefs;
    }

    public boolean isNativeEntryDefinitionAvailable()
    {
        return nativeEntryDefinitionAvailable;
    }

    private static List<HeaderRow> headerRows(LedgerEntry entry, LedgerProjectionRef selectedProjectionRef)
    {
        return List.of(new HeaderRow(HeaderField.ENTRY_TYPE, format(entry.getType())),
                        new HeaderRow(HeaderField.ENTRY_UUID, format(entry.getUUID())),
                        new HeaderRow(HeaderField.DATE_TIME, format(entry.getDateTime())),
                        new HeaderRow(HeaderField.NOTE, format(entry.getNote())),
                        new HeaderRow(HeaderField.SOURCE, format(entry.getSource())),
                        new HeaderRow(HeaderField.SHAPE,
                                        entry.getType().isLedgerNativeTargeted() ? "Ledger-native targeted" //$NON-NLS-1$
                                                        : "Legacy fixed-shape"), //$NON-NLS-1$
                        new HeaderRow(HeaderField.NATIVE_TARGETED,
                                        Boolean.toString(entry.getType().isLedgerNativeTargeted())),
                        new HeaderRow(HeaderField.SELECTED_PROJECTION_ROLE,
                                        selectedProjectionRef != null ? format(selectedProjectionRef.getRole()) : ""), //$NON-NLS-1$
                        new HeaderRow(HeaderField.SELECTED_PROJECTION_UUID,
                                        selectedProjectionRef != null ? format(selectedProjectionRef.getUUID()) : ""), //$NON-NLS-1$
                        new HeaderRow(HeaderField.SELECTED_PRIMARY_POSTING_UUID,
                                        selectedProjectionRef != null
                                                        ? format(selectedProjectionRef.getPrimaryPostingUUID())
                                                        : ""), //$NON-NLS-1$
                        new HeaderRow(HeaderField.SELECTED_POSTING_GROUP_UUID,
                                        selectedProjectionRef != null
                                                        ? format(selectedProjectionRef.getPostingGroupUUID())
                                                        : "")); //$NON-NLS-1$
    }

    private static List<ParameterRow> parameterRows(List<LedgerParameter<?>> parameters)
    {
        return parameters.stream().map(parameter -> new ParameterRow(format(parameter.getType()),
                        code(parameter.getType()), format(parameter.getValueKind()), format(parameter.getValue()),
                        domain(parameter.getType()))).toList();
    }

    private static List<LegRow> legRows(Collection<LedgerLegDefinition> definitions)
    {
        return definitions.stream()
                        .map(definition -> new LegRow(format(definition.getRole()), format(definition.getPostingType()),
                                        format(definition.getCardinality()),
                                        definition.getProjectionRole()
                                                        .map(LedgerNativeComponentInspectorModel::format)
                                                        .orElse(""), //$NON-NLS-1$
                                        Boolean.toString(definition.isPrimaryPostingExpected()),
                                        Boolean.toString(definition.isPostingGroupExpected())))
                        .toList();
    }

    private static List<PostingRow> postingRows(List<LedgerPosting> postings)
    {
        return postings.stream()
                        .map(posting -> new PostingRow(format(posting.getUUID()), format(posting.getType()),
                                        formatAmount(posting), format(posting.getCurrency()),
                                        format(posting.getSecurity()), formatShares(posting.getShares()),
                                        format(posting.getAccount()), format(posting.getPortfolio())))
                        .toList();
    }

    private static List<PostingParameterRow> postingParameterRows(List<LedgerPosting> postings)
    {
        return postings.stream()
                        .flatMap(posting -> posting.getParameters().stream()
                                        .map(parameter -> new PostingParameterRow(format(posting.getUUID()),
                                                        format(posting.getType()), format(parameter.getType()),
                                                        code(parameter.getType()), format(parameter.getValueKind()),
                                                        format(parameter.getValue()), domain(parameter.getType()))))
                        .toList();
    }

    private static List<ProjectionRefRow> projectionRefRows(List<LedgerProjectionRef> projectionRefs)
    {
        return projectionRefs.stream()
                        .map(projectionRef -> new ProjectionRefRow(format(projectionRef.getRole()),
                                        owner(projectionRef), format(projectionRef.getUUID()),
                                        format(projectionRef.getPrimaryPostingUUID()),
                                        format(projectionRef.getPostingGroupUUID())))
                        .toList();
    }

    private static String owner(LedgerProjectionRef projectionRef)
    {
        if (projectionRef.getAccount() != null)
            return format(projectionRef.getAccount());

        return format(projectionRef.getPortfolio());
    }

    private static String code(LedgerParameterType type)
    {
        return type != null ? type.getCode() : ""; //$NON-NLS-1$
    }

    private static String domain(LedgerParameterType type)
    {
        return type != null && type.hasCodeDomain() ? format(type.getCodeDomain()) : ""; //$NON-NLS-1$
    }

    private static String formatAmount(LedgerPosting posting)
    {
        if (posting.getCurrency() == null || posting.getCurrency().isBlank())
            return posting.getAmount() != 0 ? Long.toString(posting.getAmount()) : ""; //$NON-NLS-1$

        return Values.Money.format(Money.of(posting.getCurrency(), posting.getAmount()));
    }

    private static String formatShares(long shares)
    {
        return shares != 0 ? Values.Share.format(shares) : ""; //$NON-NLS-1$
    }

    private static String format(Object value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        if (value instanceof Money money)
            return Values.Money.format(money);
        else if (value instanceof LocalDate date)
            return Values.Date.format(date);
        else if (value instanceof LocalDateTime dateTime)
            return Values.DateTime.format(dateTime);
        else if (value instanceof BigDecimal decimal)
            return decimal.toPlainString();
        else if (value instanceof Security security)
            return security.getName();
        else if (value instanceof Account account)
            return account.getName();
        else if (value instanceof Portfolio portfolio)
            return portfolio.getName();

        return String.valueOf(value);
    }
}

package name.abuchen.portfolio.datatransfer.csv;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public final class SecurityAttributeColumns
{
    /**
     * Namespace prefix for the CSV {@link CSVImporter.Field} code of an
     * attribute column. Keeps attribute-derived field codes disjoint from the
     * built-in field codes (isin, note, currency, ...) so they can never
     * collide in the code-keyed field-to-column map, and makes a persisted
     * {@link CSVConfig} entry self-describing rather than a bare id/UUID. This
     * is the transient CSV field code only; it is unrelated to
     * {@link AttributeType#getId()}, which remains the persistence key for
     * attribute values.
     */
    private static final String FIELD_CODE_PREFIX = "attribute:";

    private SecurityAttributeColumns()
    {
    }

    /**
     * The CSV field code for an attribute column: the attribute id namespaced
     * with {@value #FIELD_CODE_PREFIX}. The registration site and every lookup
     * must use this single definition so they agree on the map key. The code is
     * only ever constructed here, never parsed back into an id.
     */
    public static String fieldCode(AttributeType attribute)
    {
        return FIELD_CODE_PREFIX + attribute.getId();
    }

    public static Stream<AttributeType> importable(Client client)
    {
        return client.getSettings().getAttributeTypes() //
                        .filter(a -> a.supports(Security.class)) //
                        .filter(a -> !(a.getConverter() instanceof ImageConverter));
    }

    /**
     * All names a CSV header may match this attribute on (display name and
     * column label, non-blank, deduplicated, display name first). Both
     * {@code name} and {@code columnLabel} are nullable (AttributeType.java:499-500)
     * and {@code Field} normalizes every name via {@code name.length()} with no
     * null check (CSVImporter.java:187-188), so blanks are filtered here. Falls
     * back to the id to guarantee Field's {@code names.length >= 1} invariant.
     */
    public static String[] columnNames(AttributeType attribute)
    {
        var names = new LinkedHashSet<String>();
        if (attribute.getName() != null && !attribute.getName().isBlank())
            names.add(attribute.getName());
        if (attribute.getColumnLabel() != null && !attribute.getColumnLabel().isBlank())
            names.add(attribute.getColumnLabel());
        if (names.isEmpty())
            names.add(attribute.getId());
        return names.toArray(new String[0]);
    }

    /**
     * The header a CSV export writes for this attribute. Equal to the primary
     * name a {@code Field} built from {@link #columnNames} reports via
     * {@code getName()} (= {@code columnNames(attribute)[0]}), so an exported
     * file re-imports and auto-maps back onto the same attribute even when the
     * display name is blank.
     */
    public static String preferredColumnName(AttributeType attribute)
    {
        return columnNames(attribute)[0];
    }
}

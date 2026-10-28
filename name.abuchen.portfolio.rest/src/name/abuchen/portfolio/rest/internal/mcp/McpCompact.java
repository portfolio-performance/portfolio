package name.abuchen.portfolio.rest.internal.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The payload as a model reads it: the same values as the REST API serves -
 * not one number is rounded or recomputed here - in a shape that costs fewer
 * tokens.
 * <ul>
 * <li>A list of items becomes a table: {@code columns} once, then one array of
 * values per row. Nested objects become columns of their own, named with a dot:
 * {@code gains.realizedCapitalGains}.</li>
 * <li>An amount in the reporting currency loses its currency: the report names
 * that currency once, in {@code reportingCurrency}. Decided per value - per
 * column in a table - not per field: an amount that is in another currency, or
 * a column with even one such amount, keeps its currency. A price is not an
 * amount of the report but a quote in the instrument's currency, and keeps its
 * currency always.</li>
 * <li>A holding's four price-age fields become one {@code priceNote}, present
 * only when there is something to say: a stale or a manually maintained
 * price. The price date stays in every row.</li>
 * <li>The file list loses {@code id}, which is always {@code use} again, and the
 * local {@code path}, which no tool takes.</li>
 * <li>In a long table, the fields only a few rows have - nine of ten cells
 * {@code null} - leave their columns for one column {@code more}: per row an
 * object of the ones it has, or {@code null}. Every {@code null} of a sparse
 * column costs as much as a short value, and a column of them carries
 * nothing.</li>
 * </ul>
 * The REST API is left as it is: its contract only grows by additions, and a
 * script reading it wants the objects.
 */
/* package */ final class McpCompact
{
    /**
     * What was done to a payload, which decides the notes it needs:
     * {@code currencyColumn} when a table carries a {@code <name>.currency}
     * column - beside amounts in another currency, or beside a price -,
     * {@code moreColumn} when its sparse fields went into {@code more}, and
     * {@code hasNull} when the payload as sent holds a {@code null} anywhere.
     */
    /* package */ record Result(JsonElement payload, boolean tabulated, String currencyDropped,
                    boolean currencyColumn, boolean moreColumn, boolean hasNull)
    {
    }

    /** the column that holds the sparse fields of a long table */
    /* package */ static final String MORE = "more"; //$NON-NLS-1$

    /** a table this long has sparse columns worth folding; a short one does not */
    private static final int MIN_ROWS_FOR_MORE = 20;

    /** a column is sparse when at most one cell in this many holds a value */
    private static final int SPARSE_RATIO = 10;

    private static final String ITEMS = "items"; //$NON-NLS-1$
    private static final String VALUE = "value"; //$NON-NLS-1$
    private static final String CURRENCY = "currency"; //$NON-NLS-1$

    /** the four price-age fields of a holding, replaced by {@code priceNote} */
    private static final Set<String> PRICE_AGE_FIELDS = Set.of("priceStale", "priceAgeDays", //$NON-NLS-1$ //$NON-NLS-2$
                    "priceMissedTradingDays", "priceManual"); //$NON-NLS-1$ //$NON-NLS-2$

    private McpCompact()
    {
    }

    /* package */ static Result compact(McpTool tool, JsonElement payload)
    {
        if (payload == null || !payload.isJsonObject())
            // a 204 renders as a sentence, not as null, and a bare list is not this
            // API's shape: neither has a null worth explaining
            return new Result(payload, false, null, false, false, false);

        var report = payload.getAsJsonObject().deepCopy();
        var currency = stringOf(report.get("reportingCurrency")); //$NON-NLS-1$

        if ("pp_list_files".equals(tool.name())) //$NON-NLS-1$
            forEachItem(report, item -> {
                item.remove("id"); //$NON-NLS-1$
                item.remove("path"); //$NON-NLS-1$
            });

        forEachItem(report, McpCompact::notePriceAge);

        var dropped = new boolean[1];
        for (var entry : report.entrySet())
        {
            if (!ITEMS.equals(entry.getKey()))
                entry.setValue(dropCurrency(entry.getValue(), currency, dropped));
        }

        var tabulated = false;
        var currencyColumn = new boolean[1];
        var moreColumn = new boolean[1];
        var items = report.get(ITEMS);
        if (items != null && items.isJsonArray() && !items.getAsJsonArray().isEmpty()
                        && allObjects(items.getAsJsonArray()))
        {
            report.add(ITEMS, table(items.getAsJsonArray(), currency, dropped, currencyColumn, moreColumn));
            tabulated = true;
        }

        return new Result(report, tabulated, dropped[0] ? currency : null, currencyColumn[0], moreColumn[0],
                        hasNull(report));
    }

    // ── price age ───────────────────────────────────────────────────────────

    /** the four price-age fields as one note, or none when the price is fresh */
    private static void notePriceAge(JsonObject item)
    {
        if (!item.has("priceStale")) //$NON-NLS-1$
            return;

        var manual = isTrue(item.get("priceManual")); //$NON-NLS-1$
        var stale = isTrue(item.get("priceStale")); //$NON-NLS-1$
        var ageDays = item.get("priceAgeDays"); //$NON-NLS-1$
        var missed = item.get("priceMissedTradingDays"); //$NON-NLS-1$

        String note = null;
        if (manual)
            note = "manual price, " + ageDays + " days old"; //$NON-NLS-1$ //$NON-NLS-2$
        else if (stale)
            note = "stale: " + missed + " trading days without a quote"; //$NON-NLS-1$ //$NON-NLS-2$

        // in the place of the fields it replaces, next to the price it is about
        var entries = new ArrayList<>(item.entrySet());
        entries.forEach(entry -> item.remove(entry.getKey()));
        for (var entry : entries)
        {
            if ("priceStale".equals(entry.getKey())) //$NON-NLS-1$
            {
                if (note != null)
                    item.addProperty("priceNote", note); //$NON-NLS-1$
            }
            else if (!PRICE_AGE_FIELDS.contains(entry.getKey()))
            {
                item.add(entry.getKey(), entry.getValue());
            }
        }
    }

    // ── currencies ──────────────────────────────────────────────────────────

    /** an amount: exactly a numeric value and a currency */
    private static boolean isMoney(JsonElement element)
    {
        if (element == null || !element.isJsonObject())
            return false;
        var object = element.getAsJsonObject();
        return object.size() == 2 && object.has(VALUE) && object.has(CURRENCY)
                        && object.get(VALUE).isJsonPrimitive() && object.get(VALUE).getAsJsonPrimitive().isNumber()
                        && object.get(CURRENCY).isJsonPrimitive();
    }

    /** every amount in the reporting currency as its bare value, everywhere below this element */
    private static JsonElement dropCurrency(JsonElement element, String currency, boolean[] dropped)
    {
        if (currency == null)
            return element;

        if (isMoney(element))
        {
            if (!currency.equals(stringOf(element.getAsJsonObject().get(CURRENCY))))
                return element;
            dropped[0] = true;
            return element.getAsJsonObject().get(VALUE);
        }

        if (element.isJsonObject())
        {
            for (var entry : element.getAsJsonObject().entrySet())
                entry.setValue(dropCurrency(entry.getValue(), currency, dropped));
        }
        else if (element.isJsonArray())
        {
            var array = element.getAsJsonArray();
            for (int ii = 0; ii < array.size(); ii++)
                array.set(ii, dropCurrency(array.get(ii), currency, dropped));
        }

        return element;
    }

    // ── tables ──────────────────────────────────────────────────────────────

    /**
     * The items as {@code columns} and {@code rows}. A missing field is a
     * {@code null} cell - which a model is already told means "does not apply".
     */
    private static JsonObject table(JsonArray items, String currency, boolean[] dropped,
                    boolean[] currencyColumn, boolean[] moreColumn)
    {
        var flattened = new ArrayList<Map<String, JsonElement>>();
        var names = new ArrayList<String>();
        for (var item : items)
        {
            var cells = new LinkedHashMap<String, JsonElement>();
            flatten("", item.getAsJsonObject(), cells); //$NON-NLS-1$
            flattened.add(cells);
            merge(names, cells.keySet());
        }

        // the fields only a few rows have go into one column at the end
        var sparse = sparseColumns(flattened, names);
        names.removeAll(sparse);
        moreColumn[0] = !sparse.isEmpty();

        // a column of amounts: bare values when every one is in the reporting
        // currency, otherwise values and a currency column beside them
        var columns = new ArrayList<String>();
        var moneyColumns = new LinkedHashMap<String, Boolean>();
        for (var name : names)
        {
            columns.add(name);
            if (!isMoneyColumn(flattened, name))
                continue;
            var bare = currency != null && allInCurrency(flattened, name, currency);
            moneyColumns.put(name, bare);
            if (bare)
                dropped[0] = true;
            else
                columns.add(name + "." + CURRENCY); //$NON-NLS-1$
        }

        var rows = new JsonArray();
        for (var cells : flattened)
        {
            var row = new JsonArray();
            for (var name : names)
            {
                var cell = cells.getOrDefault(name, JsonNull.INSTANCE);
                if (!moneyColumns.containsKey(name))
                {
                    row.add(cell);
                    continue;
                }

                row.add(isMoney(cell) ? cell.getAsJsonObject().get(VALUE) : JsonNull.INSTANCE);
                if (!moneyColumns.get(name))
                    row.add(isMoney(cell) ? cell.getAsJsonObject().get(CURRENCY) : JsonNull.INSTANCE);
            }

            if (!sparse.isEmpty())
            {
                var more = new JsonObject();
                for (var name : sparse)
                {
                    var cell = cells.get(name);
                    if (cell != null && !cell.isJsonNull())
                        more.add(name, cell);
                }
                row.add(more.isEmpty() ? JsonNull.INSTANCE : more);
            }
            rows.add(row);
        }

        if (!sparse.isEmpty())
            columns.add(MORE);

        // an amount in another currency, or a price, which keeps its own
        for (var name : columns)
        {
            if (name.endsWith("." + CURRENCY)) //$NON-NLS-1$
                currencyColumn[0] = true;
        }

        var columnArray = new JsonArray();
        columns.forEach(columnArray::add);

        var table = new JsonObject();
        table.add("columns", columnArray); //$NON-NLS-1$
        table.add("rows", rows); //$NON-NLS-1$
        return table;
    }

    /**
     * An object's fields as cells. An amount stays one cell, decided on per
     * column. Any other object becomes cells of its own; its {@code value}, if
     * it has one, is the cell named after the object itself - a price's quote is
     * {@code price}, its date {@code price.date}.
     */
    private static void flatten(String prefix, JsonObject object, Map<String, JsonElement> cells)
    {
        for (var entry : object.entrySet())
        {
            var key = entry.getKey();
            var name = prefix.isEmpty() ? key : VALUE.equals(key) ? prefix.substring(0, prefix.length() - 1)
                            : prefix + key;
            var value = entry.getValue();

            if (value.isJsonObject() && !isMoney(value))
                flatten(name + ".", value.getAsJsonObject(), cells); //$NON-NLS-1$
            else
                cells.put(name, value);
        }
    }

    /**
     * Adds the fields of one row that no row before had, each right after the
     * field it follows in that row - so a field only some rows carry, such as
     * {@code priceNote}, sits where it belongs rather than at the end.
     */
    private static void merge(List<String> names, Iterable<String> row)
    {
        String previous = null;
        for (var name : row)
        {
            if (!names.contains(name))
                names.add(previous == null ? 0 : names.indexOf(previous) + 1, name);
            previous = name;
        }
    }

    /**
     * The columns that are {@code null} in nine of ten rows or more, when there
     * are at least two of them in a table long enough to make them worth
     * folding. An amount stays a column: its currency is decided per column,
     * and moving it would move that decision too.
     */
    private static List<String> sparseColumns(List<Map<String, JsonElement>> rows, List<String> names)
    {
        if (rows.size() < MIN_ROWS_FOR_MORE)
            return List.of();

        var sparse = new ArrayList<String>();
        for (var name : names)
        {
            if (MORE.equals(name) || isMoneyColumn(rows, name))
                continue;

            var filled = 0;
            for (var cells : rows)
            {
                var cell = cells.get(name);
                if (cell != null && !cell.isJsonNull())
                    filled++;
            }
            if (filled * SPARSE_RATIO <= rows.size())
                sparse.add(name);
        }

        // one sparse column saves nothing: its nulls become nulls in `more`
        return sparse.size() >= 2 ? sparse : List.of();
    }

    /** whether a {@code null} appears anywhere in this element */
    private static boolean hasNull(JsonElement element)
    {
        if (element == null || element.isJsonNull())
            return true;
        if (element.isJsonObject())
        {
            for (var entry : element.getAsJsonObject().entrySet())
            {
                if (hasNull(entry.getValue()))
                    return true;
            }
        }
        else if (element.isJsonArray())
        {
            for (var child : element.getAsJsonArray())
            {
                if (hasNull(child))
                    return true;
            }
        }
        return false;
    }

    private static boolean isMoneyColumn(List<Map<String, JsonElement>> rows, String name)
    {
        var any = false;
        for (var cells : rows)
        {
            var cell = cells.get(name);
            if (cell == null || cell.isJsonNull())
                continue;
            if (!isMoney(cell))
                return false;
            any = true;
        }
        return any;
    }

    private static boolean allInCurrency(List<Map<String, JsonElement>> rows, String name, String currency)
    {
        for (var cells : rows)
        {
            var cell = cells.get(name);
            if (isMoney(cell) && !currency.equals(stringOf(cell.getAsJsonObject().get(CURRENCY))))
                return false;
        }
        return true;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private interface ItemVisitor
    {
        void visit(JsonObject item);
    }

    private static void forEachItem(JsonObject report, ItemVisitor visitor)
    {
        var items = report.get(ITEMS);
        if (items == null || !items.isJsonArray())
            return;
        for (var item : items.getAsJsonArray())
        {
            if (item.isJsonObject())
                visitor.visit(item.getAsJsonObject());
        }
    }

    private static boolean allObjects(JsonArray array)
    {
        for (var element : array)
        {
            if (!element.isJsonObject())
                return false;
        }
        return true;
    }

    private static boolean isTrue(JsonElement element)
    {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                        && element.getAsBoolean();
    }

    private static String stringOf(JsonElement element)
    {
        return element != null && element.isJsonPrimitive() && element instanceof JsonPrimitive primitive
                        && primitive.isString() ? primitive.getAsString() : null;
    }
}

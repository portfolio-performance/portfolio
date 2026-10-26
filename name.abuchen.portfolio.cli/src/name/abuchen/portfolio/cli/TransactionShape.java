package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The members of a {@link TransactionRecord} and of its units, each declared
 * once for both directions: how <code>transactions</code> shows it and how
 * <code>transaction.add</code> takes it. The read and write schemas are derived
 * from the same entries, so the input mirrors the output by construction.
 */
final class TransactionShape
{
    /** The types of a record, in the order they are published. */
    static final List<String> TYPES = List.of("BUY", "SELL", "DEPOSIT", "REMOVAL", "INTEREST", "INTEREST_CHARGE",
                    "DIVIDENDS", "FEES", "FEES_REFUND", "TAXES", "TAX_REFUND", "DELIVERY_INBOUND", "DELIVERY_OUTBOUND",
                    TransactionRecord.TRANSFER);

    /** What an orphaned side can add under its raw core type. */
    private static final List<String> ORPHAN_TYPES = List.of("TRANSFER_IN", "TRANSFER_OUT");

    /** Every type a record can be read as: {@link #TYPES}, then those of orphaned sides. */
    static final List<String> FILTERABLE_TYPES = Stream.concat(TYPES.stream(), ORPHAN_TYPES.stream()).toList();

    static final List<String> UNIT_TYPES = List.of("FEE", "TAX", "GROSS_VALUE");

    /** Whether a record shows the member. */
    enum Read
    {
        ALWAYS, SOMETIMES, NEVER
    }

    /** Whether an add takes the member; a derived one follows from the others. */
    enum Write
    {
        REQUIRED, OPTIONAL, DERIVED
    }

    /**
     * One member, with a description for each direction: what it means when
     * read, and the rules for writing it.
     *
     * @param value
     *            the schema of the value given its description
     * @param writeValue
     *            the schema when written, where it differs from the read one
     */
    record Member(String name, Function<String, Schema> value, Function<String, Schema> writeValue, Read read,
                    Write write, String readDescription, String writeDescription)
    {
        static Member of(String name, Function<String, Schema> value, Read read, String readDescription,
                        Write write, String writeDescription)
        {
            return new Member(name, value, value, read, write, readDescription, writeDescription);
        }

        Member writtenAs(Function<String, Schema> schema)
        {
            return new Member(name, value, schema, read, write, readDescription, writeDescription);
        }

        boolean isInput()
        {
            return write != Write.DERIVED;
        }
    }

    private static final List<Member> UNIT_MEMBERS = List.of(
                    Member.of("type", description -> Schema.enumOf(description, UNIT_TYPES),
                                    Read.ALWAYS, "Kind of unit.",
                                    Write.REQUIRED, "Kind of unit."),
                    Member.of("amount", Schema::number,
                                    Read.ALWAYS, "Amount in the currency of the record." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.REQUIRED, "Positive amount, in the transaction's currency."),
                    Member.of("currencyCode", Schema::string,
                                    Read.ALWAYS, "ISO currency code of the amount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.REQUIRED, "ISO currency code of amount; the transaction's currency."),
                    Member.of("forexAmount", Schema::number,
                                    Read.SOMETIMES, "Original amount in a foreign currency." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.OPTIONAL, "Original amount in the instrument's currency."),
                    Member.of("forexCurrencyCode", Schema::string,
                                    Read.SOMETIMES, "ISO currency code of forexAmount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.OPTIONAL, "ISO currency code of forexAmount."),
                    Member.of("exchangeRate", Schema::number,
                                    Read.SOMETIMES, "Unrounded as stored: forexAmount times exchangeRate is approximately amount.",
                                    Write.OPTIONAL, "Positive, unrounded: forexAmount times exchangeRate is approximately amount."));

    /** A fee, tax or forex gross value in the input. */
    static final Schema UNIT = writeSchema(
                    Schema.object("A fee, tax or forex gross value, in the shape the transactions output uses."),
                    UNIT_MEMBERS, false);

    private TransactionShape()
    {
    }

    /** A fee, tax or gross value as a record shows it. */
    static Schema unitSchema()
    {
        return readSchema(Schema.object(
                        "A fee, tax or gross value of the record, in the shape that transaction writes accept."),
                        UNIT_MEMBERS);
    }

    /** Declares the members a record shows on the object. */
    private static Schema readSchema(Schema object, List<Member> members)
    {
        for (var member : members)
        {
            if (member.read() == Read.NEVER)
                continue;

            var value = member.value().apply(member.readDescription());
            if (member.read() == Read.ALWAYS)
                object.property(member.name(), value);
            else
                object.optionalProperty(member.name(), value);
        }
        return object;
    }

    /**
     * Declares the members an add takes on the object. Of an update every
     * member is optional and the optional ones can be cleared with null.
     */
    private static Schema writeSchema(Schema object, List<Member> members, boolean update)
    {
        for (var member : members)
        {
            if (!member.isInput())
                continue;

            var value = member.writeValue().apply(member.writeDescription());
            if (member.write() == Write.REQUIRED && !update)
                object.property(member.name(), value);
            else
                object.optionalProperty(member.name(),
                                update && member.write() == Write.OPTIONAL ? value.nullable() : value);
        }
        return object;
    }
}

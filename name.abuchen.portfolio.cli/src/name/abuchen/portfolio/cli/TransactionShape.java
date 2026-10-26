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

    /** The members of a record, in the order it shows them. */
    private static final List<Member> MEMBERS = List.of(
                    Member.of("id", Schema::string,
                                    Read.ALWAYS, "UUID of the investment-account side of a buy or sell, the outgoing side of a transfer, else of the transaction.",
                                    Write.DERIVED, null),
                    Member.of("type", description -> Schema.openEnumOf(description, FILTERABLE_TYPES),
                                    Read.ALWAYS, "Core's transaction type. An open enum: new values may appear. An intact transfer is TRANSFER; a side without its partner keeps its raw type, e.g. TRANSFER_OUT.",
                                    Write.REQUIRED, "Transaction type. TRANSFER needs a kind, from and to; BUY and SELL need an investmentAccount and a cashAccount; deliveries need an investmentAccount; the others need a cashAccount.")
                                    .writtenAs(description -> Schema.enumOf(description, TYPES)),
                    Member.of("kind", description -> Schema.enumOf(description, TransactionRecord.TransferKind.ids()),
                                    Read.SOMETIMES, "Of a TRANSFER.",
                                    Write.OPTIONAL, "Of a TRANSFER only: cash or security."),
                    Member.of("date", Schema::string,
                                    Read.ALWAYS, "ISO date.",
                                    Write.REQUIRED, "ISO date of the event."),
                    Member.of("time", Schema::string,
                                    Read.SOMETIMES, "HH:mm; absent if the stored time is midnight.",
                                    Write.OPTIONAL, "HH:mm; omit for midnight."),
                    Member.of("sides", description -> Schema.array(description, Schema.string(null)),
                                    Read.ALWAYS, "UUIDs of every side of the event, id first.",
                                    Write.DERIVED, null),
                    Member.of("amount", Schema::number,
                                    Read.ALWAYS, "Net amount as stored, of the investment-account side of a buy or sell and the outgoing side of a transfer." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.REQUIRED, "Positive net amount. Of a BUY or SELL in the cash account's currency, of a cash transfer in the currency of from, of a security transfer in the instrument's currency."),
                    Member.of("currencyCode", Schema::string,
                                    Read.ALWAYS, "ISO currency code of amount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION,
                                    Write.REQUIRED, "ISO currency code of amount: the cash account's, or for a transfer the currency of from (cash) or of the instrument (security). A delivery may use any currency; if it is not the instrument's, a GROSS_VALUE unit is required."),
                    Member.of("targetAmount", Schema::number,
                                    Read.SOMETIMES, "Amount of the incoming side of a cross-currency transfer.",
                                    Write.OPTIONAL, "Of a cash transfer between accounts of different currencies: positive, in the currency of to. Refused when the currencies are the same."),
                    Member.of("targetCurrencyCode", Schema::string,
                                    Read.SOMETIMES, "ISO currency code of targetAmount.",
                                    Write.DERIVED, null),
                    Member.of("exchangeRate", Schema::number,
                                    Read.SOMETIMES, "Of a cross-currency cash transfer with a rate: unrounded as stored, targetAmount times exchangeRate is approximately amount.",
                                    Write.OPTIONAL, "Of a cross-currency cash transfer: optional, only checked against core's rounding tolerance. The stored rate is always amount divided by targetAmount and is returned in the record."),
                    Member.of("instrument", Schema::string,
                                    Read.SOMETIMES, "UUID of the instrument.",
                                    Write.OPTIONAL, "UUID of the instrument. Required for DIVIDENDS, BUY, SELL, deliveries and security transfers; optional for fees and taxes; refused for deposits, removals, interest and cash transfers."),
                    nameOf("instrument"),
                    Member.of("shares", Schema::number,
                                    Read.SOMETIMES, "Number of shares.",
                                    Write.OPTIONAL, "Positive number of shares. Required for BUY, SELL, deliveries and security transfers; accepted on a dividend; refused otherwise."),
                    Member.of("investmentAccount", Schema::string,
                                    Read.SOMETIMES, "UUID of the investment account.",
                                    Write.OPTIONAL, "UUID of the investment account; it must already be in the file. Required for BUY, SELL and deliveries; refused for cash-account events and transfers."),
                    nameOf("investmentAccount"),
                    Member.of("cashAccount", Schema::string,
                                    Read.SOMETIMES, "UUID of the cash account.",
                                    Write.OPTIONAL, "UUID of the cash account; it must already be in the file. Required for cash-account events, BUY and SELL; refused for deliveries and transfers."),
                    nameOf("cashAccount"),
                    Member.of("from", Schema::string,
                                    Read.SOMETIMES, "Of a TRANSFER: UUID of the account it leaves.",
                                    Write.OPTIONAL, "Of a TRANSFER only: UUID of the account it leaves, a cash account for a cash transfer and an investment account for a security transfer. Must differ from to."),
                    nameOf("from"),
                    Member.of("to", Schema::string,
                                    Read.SOMETIMES, "Of a TRANSFER: UUID of the account it enters.",
                                    Write.OPTIONAL, "Of a TRANSFER only: UUID of the account it enters, of the same kind as from."),
                    nameOf("to"),
                    Member.of("exDate", Schema::string,
                                    Read.SOMETIMES, "ISO ex-date of a dividend.",
                                    Write.OPTIONAL, "ISO ex-date; accepted only on a dividend with an instrument and never after the date."),
                    Member.of("grossAmount", Schema::number,
                                    Read.NEVER, null,
                                    Write.OPTIONAL, "Optional cross-check: amount plus or minus fees and taxes (for a BUY or inbound delivery amount minus them). Refused if it does not reconcile and on transfers; amount stays authoritative."),
                    Member.of("note", Schema::string,
                                    Read.SOMETIMES, "The note.",
                                    Write.OPTIONAL, "Note."),
                    Member.of("source", Schema::string,
                                    Read.SOMETIMES, "Where the record was imported from.",
                                    Write.OPTIONAL, "Where the record came from."),
                    Member.of("updatedAt", Schema::string,
                                    Read.SOMETIMES, "When the record last changed: the latest of its sides, as an ISO-8601 UTC instant such as 2026-10-24T15:04:57Z. Absent if none has one.",
                                    Write.DERIVED, null),
                    Member.of("units", description -> Schema.array(description, unitSchema()),
                                    Read.ALWAYS, "Fees, taxes and forex of the record; empty if none.",
                                    Write.OPTIONAL, "Fees, taxes and the forex gross value an instrument in another currency needs. Taxes are accepted only on DIVIDENDS and INTEREST and fees only on DIVIDENDS, as in core's import check; refused on transfers.")
                                    .writtenAs(description -> Schema.array(description, UNIT)));

    private TransactionShape()
    {
    }

    /** The name that goes with a UUID member: present exactly when that member is. */
    private static Member nameOf(String idMember)
    {
        return Member.of(idMember + "Name", Schema::string,
                        Read.SOMETIMES, "Name for " + idMember + ", empty if none. Present exactly when it is.",
                        Write.DERIVED, null);
    }

    /** A record as <code>transactions</code> shows it. */
    static Schema recordSchema(String description)
    {
        return readSchema(Schema.object(description), MEMBERS);
    }

    /**
     * Declares the members of a record that a write takes on the object. Of
     * an update every member is optional and the optional ones can be
     * cleared with null.
     */
    static Schema declareInput(Schema object, boolean update)
    {
        return writeSchema(object, MEMBERS, update);
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

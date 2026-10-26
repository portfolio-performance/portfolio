package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

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

    /** The types that are recorded in an investment account, as opposed to a cash account. */
    static final Set<String> INVESTMENT_ACCOUNT_TYPES = Set.of("BUY", "SELL", "DELIVERY_INBOUND", "DELIVERY_OUTBOUND");

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

    /** How a record shows a member, and what the member means there. */
    record Shown(Read presence, String description)
    {
    }

    /** How an add takes a member, and the rules for writing it. */
    record Taken(Write presence, String description)
    {
    }

    /**
     * One member, with a description for each direction.
     *
     * @param schemaOf
     *            the schema of the value, given its description
     * @param inputSchemaOf
     *            the schema of the value when written, where it differs
     */
    record Member(String name, Function<String, Schema> schemaOf, Function<String, Schema> inputSchemaOf,
                    Shown shown, Taken taken)
    {
        Member(String name, Function<String, Schema> schemaOf, Shown shown, Taken taken)
        {
            this(name, schemaOf, schemaOf, shown, taken);
        }

        Member takenAs(Function<String, Schema> schema)
        {
            return new Member(name, schemaOf, schema, shown, taken);
        }

        boolean isInput()
        {
            return taken.presence() != Write.DERIVED;
        }

        /** Declares the member on the object as a record shows it, if it does. */
        private void declareShown(Schema object)
        {
            if (shown.presence() == Read.NEVER)
                return;

            var value = schemaOf.apply(shown.description());
            if (shown.presence() == Read.ALWAYS)
                object.property(name, value);
            else
                object.optionalProperty(name, value);
        }

        /** Declares the member on the object as an add takes it, if it does. */
        private void declareInput(Schema object)
        {
            if (!isInput())
                return;

            var value = inputSchemaOf.apply(taken.description());
            if (taken.presence() == Write.REQUIRED)
                object.property(name, value);
            else
                object.optionalProperty(name, value);
        }

        /**
         * Declares the member on the object as an update takes it, if it
         * does: never required, and an optional one can be cleared with null.
         */
        private void declarePatch(Schema object)
        {
            if (!isInput())
                return;

            var value = inputSchemaOf.apply(taken.description());
            object.optionalProperty(name, taken.presence() == Write.OPTIONAL ? value.nullable() : value);
        }
    }

    static final Member UNIT_TYPE = new Member("type", description -> Schema.enumOf(description, UNIT_TYPES),
                    always("Kind of unit."),
                    required("Kind of unit."));
    static final Member UNIT_AMOUNT = new Member("amount", Schema::number,
                    always("Amount in the currency of the record." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    required("Positive amount, in the transaction's currency."));
    static final Member UNIT_CURRENCY_CODE = new Member("currencyCode", Schema::string,
                    always("ISO currency code of the amount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    required("ISO currency code of amount; the transaction's currency."));
    static final Member FOREX_AMOUNT = new Member("forexAmount", Schema::number,
                    sometimes("Original amount in a foreign currency." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    optional("Original amount in the instrument's currency."));
    static final Member FOREX_CURRENCY_CODE = new Member("forexCurrencyCode", Schema::string,
                    sometimes("ISO currency code of forexAmount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    optional("ISO currency code of forexAmount."));
    static final Member UNIT_EXCHANGE_RATE = new Member("exchangeRate", Schema::number,
                    sometimes("Unrounded as stored: forexAmount times exchangeRate is approximately amount."),
                    optional("Positive, unrounded: forexAmount times exchangeRate is approximately amount."));

    private static final List<Member> UNIT_MEMBERS = List.of(UNIT_TYPE, UNIT_AMOUNT, UNIT_CURRENCY_CODE,
                    FOREX_AMOUNT, FOREX_CURRENCY_CODE, UNIT_EXCHANGE_RATE);

    /** A fee, tax or forex gross value in the input. */
    static final Schema UNIT = declare(
                    Schema.object("A fee, tax or forex gross value, in the shape the transactions output uses."),
                    UNIT_MEMBERS, Member::declareInput);

    static final Member ID = new Member("id", Schema::string,
                    always("UUID of the investment-account side of a buy or sell, the outgoing side of a transfer, else of the transaction."),
                    derived());
    static final Member TYPE = new Member("type", description -> Schema.openEnumOf(description, FILTERABLE_TYPES),
                    always("Core's transaction type. An open enum: new values may appear. An intact transfer is TRANSFER; a side without its partner keeps its raw type, e.g. TRANSFER_OUT."),
                    required("Transaction type. TRANSFER needs a kind, from and to; BUY and SELL need an investmentAccount and a cashAccount; deliveries need an investmentAccount; the others need a cashAccount."))
                    .takenAs(description -> Schema.enumOf(description, TYPES));
    static final Member KIND = new Member("kind", description -> Schema.enumOf(description, TransactionRecord.TransferKind.ids()),
                    sometimes("Of a TRANSFER."),
                    optional("Of a TRANSFER only: cash or security."));
    static final Member DATE = new Member("date", Schema::string,
                    always("ISO date."),
                    required("ISO date of the event."));
    static final Member TIME = new Member("time", Schema::string,
                    sometimes("HH:mm; absent if the stored time is midnight."),
                    optional("HH:mm; omit for midnight."));
    static final Member SIDES = new Member("sides", description -> Schema.array(description, Schema.string(null)),
                    always("UUIDs of every side of the event, id first."),
                    derived());
    static final Member AMOUNT = new Member("amount", Schema::number,
                    always("Net amount as stored, of the investment-account side of a buy or sell and the outgoing side of a transfer." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    required("Positive net amount. Of a BUY or SELL in the cash account's currency, of a cash transfer in the currency of from, of a security transfer in the instrument's currency."));
    static final Member CURRENCY_CODE = new Member("currencyCode", Schema::string,
                    always("ISO currency code of amount." + MinorUnits.CONVERTED_WITH_CURRENCY_OPTION),
                    required("ISO currency code of amount: the cash account's, or for a transfer the currency of from (cash) or of the instrument (security). A delivery may use any currency; if it is not the instrument's, a GROSS_VALUE unit is required."));
    static final Member TARGET_AMOUNT = new Member("targetAmount", Schema::number,
                    sometimes("Amount of the incoming side of a cross-currency transfer."),
                    optional("Of a cash transfer between accounts of different currencies: positive, in the currency of to. Refused when the currencies are the same."));
    static final Member TARGET_CURRENCY_CODE = new Member("targetCurrencyCode", Schema::string,
                    sometimes("ISO currency code of targetAmount."),
                    derived());
    static final Member EXCHANGE_RATE = new Member("exchangeRate", Schema::number,
                    sometimes("Of a cross-currency cash transfer with a rate: unrounded as stored, targetAmount times exchangeRate is approximately amount."),
                    optional("Of a cross-currency cash transfer: optional, only checked against core's rounding tolerance. The stored rate is always amount divided by targetAmount and is returned in the record."));
    static final Member INSTRUMENT = new Member("instrument", Schema::string,
                    sometimes("UUID of the instrument."),
                    optional("UUID of the instrument. Required for DIVIDENDS, BUY, SELL, deliveries and security transfers; optional for fees and taxes; refused for deposits, removals, interest and cash transfers."));
    static final Member INSTRUMENT_NAME = nameOf(INSTRUMENT);
    static final Member SHARES = new Member("shares", Schema::number,
                    sometimes("Number of shares."),
                    optional("Positive number of shares. Required for BUY, SELL, deliveries and security transfers; accepted on a dividend; refused otherwise."));
    static final Member INVESTMENT_ACCOUNT = new Member("investmentAccount", Schema::string,
                    sometimes("UUID of the investment account."),
                    optional("UUID of the investment account; it must already be in the file. Required for BUY, SELL and deliveries; refused for cash-account events and transfers."));
    static final Member INVESTMENT_ACCOUNT_NAME = nameOf(INVESTMENT_ACCOUNT);
    static final Member CASH_ACCOUNT = new Member("cashAccount", Schema::string,
                    sometimes("UUID of the cash account."),
                    optional("UUID of the cash account; it must already be in the file. Required for cash-account events, BUY and SELL; refused for deliveries and transfers."));
    static final Member CASH_ACCOUNT_NAME = nameOf(CASH_ACCOUNT);
    static final Member FROM = new Member("from", Schema::string,
                    sometimes("Of a TRANSFER: UUID of the account it leaves."),
                    optional("Of a TRANSFER only: UUID of the account it leaves, a cash account for a cash transfer and an investment account for a security transfer. Must differ from to."));
    static final Member FROM_NAME = nameOf(FROM);
    static final Member TO = new Member("to", Schema::string,
                    sometimes("Of a TRANSFER: UUID of the account it enters."),
                    optional("Of a TRANSFER only: UUID of the account it enters, of the same kind as from."));
    static final Member TO_NAME = nameOf(TO);
    static final Member EX_DATE = new Member("exDate", Schema::string,
                    sometimes("ISO ex-date of a dividend."),
                    optional("ISO ex-date; accepted only on a dividend with an instrument and never after the date."));
    static final Member GROSS_AMOUNT = new Member("grossAmount", Schema::number,
                    never(),
                    optional("Optional cross-check: amount plus or minus fees and taxes (for a BUY or inbound delivery amount minus them). Refused if it does not reconcile and on transfers; amount stays authoritative."));
    static final Member NOTE = new Member("note", Schema::string,
                    sometimes("The note."),
                    optional("Note."));
    static final Member SOURCE = new Member("source", Schema::string,
                    sometimes("Where the record was imported from."),
                    optional("Where the record came from."));
    static final Member UPDATED_AT = new Member(MasterData.UPDATED_AT_MEMBER, Schema::string,
                    sometimes("When the record last changed: the latest of its sides, as an ISO-8601 UTC instant such as 2026-10-24T15:04:57Z. Absent if none has one."),
                    derived());
    static final Member UNITS = new Member("units", description -> Schema.array(description, unitSchema()),
                    always("Fees, taxes and forex of the record; empty if none."),
                    optional("Fees, taxes and the forex gross value an instrument in another currency needs. Taxes are accepted only on DIVIDENDS and INTEREST and fees only on DIVIDENDS, as in core's import check; refused on transfers."))
                    .takenAs(description -> Schema.array(description, UNIT));

    /** The members of a record, in the order it shows them. */
    private static final List<Member> MEMBERS = List.of(ID, TYPE, KIND, DATE, TIME, SIDES, AMOUNT, CURRENCY_CODE,
                    TARGET_AMOUNT, TARGET_CURRENCY_CODE, EXCHANGE_RATE, INSTRUMENT, INSTRUMENT_NAME, SHARES,
                    INVESTMENT_ACCOUNT, INVESTMENT_ACCOUNT_NAME, CASH_ACCOUNT, CASH_ACCOUNT_NAME, FROM, FROM_NAME, TO,
                    TO_NAME, EX_DATE, GROSS_AMOUNT, NOTE, SOURCE, UPDATED_AT, UNITS);

    /** The names of the members of a record that an add takes. */
    private static final Set<String> INPUT = MEMBERS.stream().filter(Member::isInput).map(Member::name)
                    .collect(Collectors.toUnmodifiableSet());

    private TransactionShape()
    {
    }

    private static Shown always(String description)
    {
        return new Shown(Read.ALWAYS, description);
    }

    private static Shown sometimes(String description)
    {
        return new Shown(Read.SOMETIMES, description);
    }

    private static Shown never()
    {
        return new Shown(Read.NEVER, null);
    }

    private static Taken required(String description)
    {
        return new Taken(Write.REQUIRED, description);
    }

    private static Taken optional(String description)
    {
        return new Taken(Write.OPTIONAL, description);
    }

    private static Taken derived()
    {
        return new Taken(Write.DERIVED, null);
    }

    /** The name that goes with a UUID member: present exactly when that member is. */
    private static Member nameOf(Member uuid)
    {
        return new Member(uuid.name() + "Name", Schema::string,
                        sometimes("Name for " + uuid.name() + ", empty if none. Present exactly when it is."),
                        derived());
    }

    /** A record as <code>transactions</code> shows it. */
    static Schema recordSchema(String description)
    {
        return declare(Schema.object(description), MEMBERS, Member::declareShown);
    }

    /** Declares the members of a record that an add takes on the object. */
    static Schema declareInput(Schema object)
    {
        return declare(object, MEMBERS, Member::declareInput);
    }

    /**
     * Declares the members of a record that an update takes on the object:
     * every one is optional, and the optional ones can be cleared with null.
     */
    static Schema declarePatch(Schema object)
    {
        return declare(object, MEMBERS, Member::declarePatch);
    }

    /**
     * The members of a record as <code>transactions</code> shows it that an
     * add takes, which is the input that adds the same record again.
     */
    static JsonObject inputOf(JsonObject record)
    {
        var input = new JsonObject();
        for (var member : record.entrySet())
        {
            var name = member.getKey();
            var value = member.getValue();
            // an empty list of units is the same as none, and a transfer refuses any
            if (INPUT.contains(name) && !(name.equals(UNITS.name()) && value.getAsJsonArray().isEmpty()))
                input.add(name, value);
        }
        return input;
    }

    /** A fee, tax or gross value as a record shows it. */
    static Schema unitSchema()
    {
        return declare(Schema.object(
                        "A fee, tax or gross value of the record, in the shape that transaction writes accept."),
                        UNIT_MEMBERS, Member::declareShown);
    }

    private static Schema declare(Schema object, List<Member> members, BiConsumer<Member, Schema> how)
    {
        members.forEach(member -> how.accept(member, object));
        return object;
    }
}

package name.abuchen.portfolio.cli.transaction;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * The input an update checks: the members an add of the existing record would
 * take, read from its stored sides, with the members of the patch laid on
 * them. The type and the kind of a transfer are fixed; a patch that changes
 * them is refused here, before anything is built.
 */
final class Draft
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final TransactionRecord record;
    private final JsonObject input;

    private Draft(TransactionRecord record, JsonObject input)
    {
        this.record = record;
        this.input = input;
    }

    /** The draft of the record as it is stored. */
    static Draft of(TransactionRecord record)
    {
        var primary = record.primary();
        var json = new JsonObject();

        json.addProperty(TransactionShape.TYPE.name(), record.type());
        record.kind().ifPresent(kind -> json.addProperty(TransactionShape.KIND.name(), kind.id()));

        var dateTime = primary.getDateTime();
        json.addProperty(TransactionShape.DATE.name(), dateTime.toLocalDate().toString());
        if (!dateTime.toLocalTime().equals(LocalTime.MIDNIGHT))
            json.addProperty(TransactionShape.TIME.name(), dateTime.format(TIME));

        json.add(TransactionShape.AMOUNT.name(), Decimals.of(primary.getAmount(), Values.Amount));
        putIfSet(json, TransactionShape.CURRENCY_CODE.name(), primary.getCurrencyCode());

        record.crossCurrencyIncoming().ifPresent(incoming -> json.add(TransactionShape.TARGET_AMOUNT.name(),
                        Decimals.of(incoming.getAmount(), Values.Amount)));
        var transferRate = record.transferRate();
        transferRate.ifPresent(unit -> json.add(TransactionShape.EXCHANGE_RATE.name(),
                        Decimals.of(unit.getExchangeRate())));

        if (primary.getSecurity() != null)
            json.addProperty(TransactionShape.INSTRUMENT.uuid().name(), primary.getSecurity().getUUID());
        if (record.hasShares())
            json.add(TransactionShape.SHARES.name(), Decimals.of(primary.getShares(), Values.Share));

        putIfPresent(json, TransactionShape.INVESTMENT_ACCOUNT, record.investmentAccount());
        putIfPresent(json, TransactionShape.CASH_ACCOUNT, record.cashAccount());
        putIfPresent(json, TransactionShape.FROM, record.from());
        putIfPresent(json, TransactionShape.TO, record.to());

        if (primary instanceof AccountTransaction account && account.getExDate() != null
                        && account.getType() == AccountTransaction.Type.DIVIDENDS)
            json.addProperty(TransactionShape.EX_DATE.name(), account.getExDate().toLocalDate().toString());

        putIfSet(json, TransactionShape.NOTE.name(), primary.getNote());
        putIfSet(json, TransactionShape.SOURCE.name(), primary.getSource());

        var units = new JsonArray();
        primary.getUnits().filter(unit -> transferRate.isEmpty() || unit != transferRate.get())
                        .forEach(unit -> units.add(unit(unit)));
        if (!units.isEmpty())
            json.add(TransactionShape.UNITS.name(), units);

        return new Draft(record, json);
    }

    private static void putIfSet(JsonObject json, String member, String value)
    {
        if (MasterData.isSet(value))
            json.addProperty(member, value);
    }

    private static void putIfPresent(JsonObject json, TransactionShape.NamedUuid member, Optional<String> uuid)
    {
        uuid.ifPresent(value -> json.addProperty(member.uuid().name(), value));
    }

    private static JsonObject unit(Transaction.Unit unit)
    {
        var json = new JsonObject();
        json.addProperty(TransactionShape.UNIT_TYPE.name(), unit.getType().name());
        json.add(TransactionShape.UNIT_AMOUNT.name(), Decimals.amount(unit.getAmount()));
        json.addProperty(TransactionShape.UNIT_CURRENCY_CODE.name(), unit.getAmount().getCurrencyCode());
        if (unit.getForex() != null)
        {
            json.add(TransactionShape.UNIT_FOREX_AMOUNT.name(), Decimals.amount(unit.getForex()));
            json.addProperty(TransactionShape.UNIT_FOREX_CURRENCY_CODE.name(), unit.getForex().getCurrencyCode());
            json.add(TransactionShape.UNIT_EXCHANGE_RATE.name(), Decimals.of(unit.getExchangeRate()));
        }
        return json;
    }

    /**
     * Lays the members of the patch on the draft, except those named in
     * <code>skip</code>: a value replaces the stored one, and null clears it.
     *
     * @return the <code>type-immutable</code> and <code>kind-immutable</code>
     *         failures; the type or kind they name is not laid on the draft
     */
    List<FieldFailure> lay(JsonObject patch, Set<String> skip)
    {
        var failures = new ArrayList<FieldFailure>();
        for (var member : patch.entrySet())
        {
            var name = member.getKey();
            var value = member.getValue();
            if (skip.contains(name))
                continue;

            if (name.equals(TransactionShape.TYPE.name()))
            {
                if (differs(value, record.type()))
                    failures.add(new FieldFailure(name, "type-immutable",
                                    "The type of a record can't change; delete it and add a new one.", null));
            }
            else if (name.equals(TransactionShape.KIND.name()) && record.kind().isPresent())
            {
                if (differs(value, record.kind().get().id()))
                    failures.add(new FieldFailure(name, "kind-immutable",
                                    "The kind of a transfer can't change; delete it and add a new one.", null));
            }
            else if (value.isJsonNull())
                input.remove(name);
            else
                input.add(name, value);
        }
        return failures;
    }

    private static boolean differs(JsonElement value, String current)
    {
        return !(OperationInput.isText(value) && current.equals(value.getAsString()));
    }

    /** The draft as the input of an add. */
    JsonObject input()
    {
        return input;
    }
}

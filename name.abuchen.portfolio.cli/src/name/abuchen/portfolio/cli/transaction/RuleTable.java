package name.abuchen.portfolio.cli.transaction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.ErrorCode;

/**
 * Which members each type of record takes: a table of {@link Row} (type and,
 * for a transfer, its kind) by {@link Ruled} member. Each cell is
 * {@link Rule#REQUIRED}, {@link Rule#OPTIONAL} or {@link Rule#REFUSED}. A
 * member's refusal code is declared once, on the member. The rules that look
 * at more than one member at a time are code next to the builders.
 */
final class RuleTable
{
    enum Rule
    {
        REQUIRED, OPTIONAL, REFUSED;

        static Rule of(char cell)
        {
            return switch (cell)
            {
                case 'R' -> REQUIRED;
                case 'o' -> OPTIONAL;
                case '-' -> REFUSED;
                default -> throw new IllegalArgumentException(String.valueOf(cell));
            };
        }
    }

    /** The members whose presence depends on the type, in the order failures are reported. */
    enum Ruled
    {
        KIND(TransactionShape.KIND, ErrorCode.KIND_NOT_ALLOWED), //
        TARGET_AMOUNT(TransactionShape.TARGET_AMOUNT, ErrorCode.TARGET_AMOUNT_NOT_ALLOWED), //
        EXCHANGE_RATE(TransactionShape.EXCHANGE_RATE, ErrorCode.EXCHANGE_RATE_NOT_ALLOWED), //
        INSTRUMENT(TransactionShape.INSTRUMENT.uuid(), ErrorCode.INSTRUMENT_NOT_ALLOWED), //
        SHARES(TransactionShape.SHARES, ErrorCode.SHARES_NOT_ALLOWED), //
        INVESTMENT_ACCOUNT(TransactionShape.INVESTMENT_ACCOUNT.uuid(), ErrorCode.INVESTMENT_ACCOUNT_NOT_ALLOWED), //
        CASH_ACCOUNT(TransactionShape.CASH_ACCOUNT.uuid(), ErrorCode.CASH_ACCOUNT_NOT_ALLOWED), //
        FROM(TransactionShape.FROM.uuid(), ErrorCode.FROM_NOT_ALLOWED), //
        TO(TransactionShape.TO.uuid(), ErrorCode.TO_NOT_ALLOWED), //
        EX_DATE(TransactionShape.EX_DATE, ErrorCode.EXDATE_NOT_ALLOWED), //
        GROSS_AMOUNT(TransactionShape.GROSS_AMOUNT, ErrorCode.GROSS_AMOUNT_NOT_ALLOWED), //
        UNITS(TransactionShape.UNITS, ErrorCode.UNITS_NOT_ALLOWED);

        private final TransactionShape.Member member;
        private final ErrorCode refusalCode;

        Ruled(TransactionShape.Member member, ErrorCode refusalCode)
        {
            this.member = member;
            this.refusalCode = refusalCode;
        }

        String id()
        {
            return member.name();
        }

        TransactionShape.Member member()
        {
            return member;
        }

        ErrorCode refusalCode()
        {
            return refusalCode;
        }

        /** The ruled member of that name, if it is one. */
        static Optional<Ruled> byId(String id)
        {
            return Arrays.stream(values()).filter(member -> member.id().equals(id)).findFirst();
        }

        /** The message of a refusal of this member, for the type as {@link Row#label()} names it. */
        String refusalMessage(String label)
        {
            return label + " must not have " + id() + ".";
        }
    }

    /**
     * One row of the table. The cells follow the order of {@link Ruled}.
     */
    enum Row
    {
        // @formatter:off
        //                                                             kind  tAmt  rate  inst  shrs  inv   cash  from  to    exDt  gross units
        DEPOSIT          ("DEPOSIT",           null,                   "-     -     -     -     -     -     R     -     -     -     o     o"),
        REMOVAL          ("REMOVAL",           null,                   "-     -     -     -     -     -     R     -     -     -     o     o"),
        INTEREST         ("INTEREST",          null,                   "-     -     -     -     -     -     R     -     -     -     o     o"),
        INTEREST_CHARGE  ("INTEREST_CHARGE",   null,                   "-     -     -     -     -     -     R     -     -     -     o     o"),
        DIVIDENDS        ("DIVIDENDS",         null,                   "-     -     -     R     o     -     R     -     -     o     o     o"),
        FEES             ("FEES",              null,                   "-     -     -     o     -     -     R     -     -     -     o     o"),
        FEES_REFUND      ("FEES_REFUND",       null,                   "-     -     -     o     -     -     R     -     -     -     o     o"),
        TAXES            ("TAXES",             null,                   "-     -     -     o     -     -     R     -     -     -     o     o"),
        TAX_REFUND       ("TAX_REFUND",        null,                   "-     -     -     o     -     -     R     -     -     -     o     o"),
        BUY              ("BUY",               null,                   "-     -     -     R     R     R     R     -     -     -     o     o"),
        SELL             ("SELL",              null,                   "-     -     -     R     R     R     R     -     -     -     o     o"),
        DELIVERY_INBOUND ("DELIVERY_INBOUND",  null,                   "-     -     -     R     R     R     -     -     -     -     o     o"),
        DELIVERY_OUTBOUND("DELIVERY_OUTBOUND", null,                   "-     -     -     R     R     R     -     -     -     -     o     o"),
        CASH_TRANSFER    ("TRANSFER", TransactionRecord.TransferKind.CASH,     "R     o     o     -     -     -     -     R     R     -     -     -"),
        SECURITY_TRANSFER("TRANSFER", TransactionRecord.TransferKind.SECURITY, "R     -     -     R     R     -     -     R     R     -     -     -");
        // @formatter:on

        private final String type;
        private final TransactionRecord.TransferKind kind;
        private final List<Rule> cells;

        Row(String type, TransactionRecord.TransferKind kind, String cells)
        {
            this.type = type;
            this.kind = kind;
            this.cells = Arrays.stream(cells.trim().split("\\s+")).map(cell -> Rule.of(cell.charAt(0))).toList();
            if (this.cells.size() != Ruled.values().length)
                throw new IllegalStateException(name() + " has " + this.cells.size() + " cells");
        }

        String type()
        {
            return type;
        }

        Optional<TransactionRecord.TransferKind> kind()
        {
            return Optional.ofNullable(kind);
        }

        Rule rule(Ruled member)
        {
            return cells.get(member.ordinal());
        }

        /** The type as the refusal messages name it, with the kind of a transfer. */
        String label()
        {
            return kind == null ? type : type + " of kind " + kind.id();
        }

        /** The row of the type and the kind, which only a transfer has. */
        static Optional<Row> of(String type, TransactionRecord.TransferKind kind)
        {
            return Arrays.stream(values()).filter(row -> row.type.equals(type) && row.kind == kind).findFirst();
        }
    }

    private RuleTable()
    {
    }

    /**
     * The refusals of a type whose row is unknown because the kind of a
     * transfer is missing or wrong: the members that every row of the type
     * refuses.
     */
    static List<Ruled> refusedByEveryRowOf(String type)
    {
        var rows = Arrays.stream(Row.values()).filter(row -> row.type.equals(type)).toList();
        if (rows.isEmpty())
            return List.of();
        return Arrays.stream(Ruled.values())
                        .filter(member -> rows.stream().allMatch(row -> row.rule(member) == Rule.REFUSED)).toList();
    }
}

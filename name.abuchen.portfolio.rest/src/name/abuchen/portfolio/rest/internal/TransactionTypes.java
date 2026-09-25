package name.abuchen.portfolio.rest.internal;

import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

/**
 * The API's transaction type vocabulary: stable, machine-readable kebab-case
 * values, deliberately independent of {@code Type#toString()} (locale
 * dependent, see {@code AttributeCodec}'s class comment for the same concern
 * with attribute types).
 * <p/>
 * A transaction is <em>reported</em> with the type of the leg the transaction
 * list shows ({@link #wireType(Transaction)}). A transaction is
 * <em>created</em> with one of {@link #CREATE_TYPES}: the reported types minus
 * the two transfer legs, plus {@code cash-transfer} and
 * {@code security-transfer}, which create both legs of a transfer at once.
 */
@SuppressWarnings("nls")
public final class TransactionTypes
{
    public static final String CASH_TRANSFER = "cash-transfer";
    public static final String SECURITY_TRANSFER = "security-transfer";

    /** every value {@link #wireType(Transaction)} can return */
    public static final Set<String> WIRE_TYPES = Set.of("deposit", "removal", "interest", "interest-charge",
                    "dividends", "fees", "fees-refund", "taxes", "tax-refund", "buy", "sell", "transfer-in",
                    "transfer-out", "delivery-inbound", "delivery-outbound");

    /**
     * The shape of a transaction: which model objects it consists of and which
     * fields describe it. The type of a transaction can only be changed
     * within its kind.
     */
    public enum Kind
    {
        /** a {@code BuySellEntry}: investment-account leg plus cash-account leg */
        BUY_SELL(List.of("buy", "sell")),
        /** a single {@link PortfolioTransaction} without cash leg */
        DELIVERY(List.of("delivery-inbound", "delivery-outbound")),
        /** a single {@link AccountTransaction} of an instrument */
        DIVIDEND(List.of("dividends")),
        /** a single {@link AccountTransaction}, optionally of an instrument */
        CASH(List.of("deposit", "removal", "interest", "interest-charge", "fees", "fees-refund", "taxes",
                        "tax-refund")),
        /** an {@code AccountTransferEntry} */
        CASH_TRANSFER(List.of(TransactionTypes.CASH_TRANSFER)),
        /** a {@code PortfolioTransferEntry} */
        SECURITY_TRANSFER(List.of(TransactionTypes.SECURITY_TRANSFER));

        private final List<String> types;

        Kind(List<String> types)
        {
            this.types = types;
        }

        /** the create types of this kind */
        public List<String> types()
        {
            return types;
        }
    }

    /** every value a transaction can be created with */
    public static final List<String> CREATE_TYPES = List.of(Kind.values()).stream()
                    .flatMap(kind -> kind.types().stream()).toList();

    private TransactionTypes()
    {
    }

    /** the kind of a create type, or null if the type is unknown */
    public static Kind kindOf(String createType)
    {
        for (var kind : Kind.values())
        {
            if (kind.types().contains(createType))
                return kind;
        }
        return null;
    }

    /**
     * The kind of an existing transaction, given the leg the transaction list
     * reports; null for a leg that cannot be the reported one (the cash leg of
     * a buy/sell, the inbound leg of a transfer) or is not linked to its
     * other leg.
     */
    public static Kind kindOf(Transaction transaction)
    {
        if (transaction instanceof PortfolioTransaction t)
        {
            return switch (t.getType())
            {
                case BUY, SELL -> t.getCrossEntry() != null ? Kind.BUY_SELL : null;
                case DELIVERY_INBOUND, DELIVERY_OUTBOUND -> Kind.DELIVERY;
                case TRANSFER_OUT -> t.getCrossEntry() != null ? Kind.SECURITY_TRANSFER : null;
                case TRANSFER_IN -> null;
            };
        }
        else if (transaction instanceof AccountTransaction t)
        {
            return switch (t.getType())
            {
                case DIVIDENDS -> Kind.DIVIDEND;
                case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND -> Kind.CASH;
                case TRANSFER_OUT -> t.getCrossEntry() != null ? Kind.CASH_TRANSFER : null;
                case BUY, SELL, TRANSFER_IN -> null;
            };
        }
        return null;
    }

    /** the investment-account transaction type of a buy, sell or delivery create type */
    public static PortfolioTransaction.Type portfolioType(String createType)
    {
        return switch (createType)
        {
            case "buy" -> PortfolioTransaction.Type.BUY;
            case "sell" -> PortfolioTransaction.Type.SELL;
            case "delivery-inbound" -> PortfolioTransaction.Type.DELIVERY_INBOUND;
            case "delivery-outbound" -> PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            default -> throw new IllegalArgumentException(createType);
        };
    }

    /** the cash-account transaction type of a dividend or cash create type */
    public static AccountTransaction.Type accountType(String createType)
    {
        return switch (createType)
        {
            case "deposit" -> AccountTransaction.Type.DEPOSIT;
            case "removal" -> AccountTransaction.Type.REMOVAL;
            case "interest" -> AccountTransaction.Type.INTEREST;
            case "interest-charge" -> AccountTransaction.Type.INTEREST_CHARGE;
            case "dividends" -> AccountTransaction.Type.DIVIDENDS;
            case "fees" -> AccountTransaction.Type.FEES;
            case "fees-refund" -> AccountTransaction.Type.FEES_REFUND;
            case "taxes" -> AccountTransaction.Type.TAXES;
            case "tax-refund" -> AccountTransaction.Type.TAX_REFUND;
            default -> throw new IllegalArgumentException(createType);
        };
    }

    /** the reported type of a transaction leg */
    public static String wireType(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction t)
        {
            return switch (t.getType())
            {
                case DEPOSIT -> "deposit";
                case REMOVAL -> "removal";
                case INTEREST -> "interest";
                case INTEREST_CHARGE -> "interest-charge";
                case DIVIDENDS -> "dividends";
                case FEES -> "fees";
                case FEES_REFUND -> "fees-refund";
                case TAXES -> "taxes";
                case TAX_REFUND -> "tax-refund";
                case BUY -> "buy";
                case SELL -> "sell";
                case TRANSFER_IN -> "transfer-in";
                case TRANSFER_OUT -> "transfer-out";
            };
        }
        else if (transaction instanceof PortfolioTransaction t)
        {
            return switch (t.getType())
            {
                case BUY -> "buy";
                case SELL -> "sell";
                case TRANSFER_IN -> "transfer-in";
                case TRANSFER_OUT -> "transfer-out";
                case DELIVERY_INBOUND -> "delivery-inbound";
                case DELIVERY_OUTBOUND -> "delivery-outbound";
            };
        }
        throw new IllegalArgumentException("unsupported transaction type: " + transaction.getClass());
    }
}

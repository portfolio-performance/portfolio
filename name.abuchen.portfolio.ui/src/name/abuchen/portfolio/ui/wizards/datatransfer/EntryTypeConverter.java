package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction.Unit;

/**
 * Changes the type of extracted entries, e.g. a purchase into an inbound
 * delivery or portfolio transfer, or a deposit into an inbound account
 * transfer. The original item is kept so that the change can be undone. Fees
 * and taxes of a transaction converted into a portfolio transfer become
 * additional entries. The class works on the list of entries of the table and
 * has no user interface.
 */
final class EntryTypeConverter
{
    /** the types an entry can be changed to */
    enum TypeOption
    {
        ORIGINAL, DELIVERY, PORTFOLIO_TRANSFER, ACCOUNT_TRANSFER
    }

    /**
     * An entry whose type has been changed: the original item, the new type
     * and the additional fee and tax entries.
     */
    private record Conversion(Item original, TypeOption type, List<ExtractedEntry> additionalEntries)
    {
    }

    private final List<ExtractedEntry> entries;

    /** entries with a changed type -> original item and additional entries */
    private final Map<ExtractedEntry, Conversion> conversions = new HashMap<>();

    EntryTypeConverter(List<ExtractedEntry> entries)
    {
        this.entries = entries;
    }

    /**
     * Forgets the conversions of entries which have been removed, e.g. when
     * the documents are extracted again.
     */
    void forgetRemovedEntries()
    {
        conversions.keySet().removeIf(entry -> !entries.contains(entry));
    }

    /**
     * Returns the types the entry can be changed to (including the original
     * type) or an empty list if the type cannot be changed.
     */
    List<TypeOption> getTypeOptions(ExtractedEntry entry)
    {
        // the additional fee and tax entries belong to the converted entry
        if (conversions.values().stream().anyMatch(c -> c.additionalEntries().contains(entry)))
            return List.of();

        var original = getOriginalItem(entry);
        if (original.isFailure() || original.isSkipped())
            return List.of();

        var subject = original.getSubject();

        if (subject instanceof BuySellEntry buySell)
        {
            if (hasSecurityCurrency(buySell.getPortfolioTransaction()))
                return List.of(TypeOption.ORIGINAL, TypeOption.DELIVERY, TypeOption.PORTFOLIO_TRANSFER);
            else
                return List.of(TypeOption.ORIGINAL, TypeOption.DELIVERY);
        }
        else if (subject instanceof PortfolioTransaction transaction
                        && (transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        || transaction.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                        && hasSecurityCurrency(transaction))
        {
            return List.of(TypeOption.ORIGINAL, TypeOption.PORTFOLIO_TRANSFER);
        }
        else if (subject instanceof AccountTransaction transaction
                        && (transaction.getType() == AccountTransaction.Type.DEPOSIT
                                        || transaction.getType() == AccountTransaction.Type.REMOVAL)
                        && transaction.getUnits().findAny().isEmpty())
        {
            return List.of(TypeOption.ORIGINAL, TypeOption.ACCOUNT_TRANSFER);
        }

        return List.of();
    }

    /**
     * A transfer carries no exchange rate, therefore the transaction must be
     * in the currency of the security.
     */
    private boolean hasSecurityCurrency(PortfolioTransaction transaction)
    {
        return transaction.getSecurity() != null
                        && transaction.getCurrencyCode().equals(transaction.getSecurity().getCurrencyCode());
    }

    /**
     * Returns the additional fee and tax entries created when the type of the
     * entry was changed, or an empty list.
     */
    List<ExtractedEntry> getAdditionalEntries(ExtractedEntry entry)
    {
        var conversion = conversions.get(entry);
        return conversion != null ? conversion.additionalEntries() : List.of();
    }

    Item getOriginalItem(ExtractedEntry entry)
    {
        var conversion = conversions.get(entry);
        return conversion != null ? conversion.original() : entry.getItem();
    }

    TypeOption getCurrentType(ExtractedEntry entry)
    {
        var conversion = conversions.get(entry);
        return conversion != null ? conversion.type() : TypeOption.ORIGINAL;
    }

    String getTypeLabel(Item original, TypeOption option)
    {
        var isInbound = isInbound(original);

        return switch (option)
        {
            case ORIGINAL -> original.getTypeInformation();
            case DELIVERY -> (isInbound ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND).toString();
            case PORTFOLIO_TRANSFER, ACCOUNT_TRANSFER -> (isInbound ? PortfolioTransaction.Type.TRANSFER_IN
                            : PortfolioTransaction.Type.TRANSFER_OUT).toString();
        };
    }

    /**
     * Returns true if the original item brings securities or money in (buy,
     * inbound delivery, deposit).
     */
    private boolean isInbound(Item original)
    {
        var subject = original.getSubject();

        if (subject instanceof BuySellEntry buySell)
            return buySell.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY;
        else if (subject instanceof PortfolioTransaction transaction)
            return transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;
        else if (subject instanceof AccountTransaction transaction)
            return transaction.getType() == AccountTransaction.Type.DEPOSIT;

        return true;
    }

    /**
     * Changes the type of the entry. The entry is replaced (together with its
     * additional fee and tax entries) by new entries created from the original
     * item. The choice of the user (import or not) is kept.
     */
    void changeType(ExtractedEntry entry, TypeOption option)
    {
        if (getCurrentType(entry) == option)
            return;

        var index = entries.indexOf(entry);
        if (index < 0)
            return;

        var original = getOriginalItem(entry);

        var conversion = conversions.remove(entry);
        if (conversion != null)
            entries.removeAll(conversion.additionalEntries());

        var items = createItems(original, option);

        var main = entry.copyWith(items.get(0));
        entries.set(index, main);

        var additionalEntries = new ArrayList<ExtractedEntry>();
        for (var ii = 1; ii < items.size(); ii++)
        {
            var additional = entry.copyWith(items.get(ii));
            additional.setOwner(main);
            entries.add(index + ii, additional);
            additionalEntries.add(additional);
        }

        if (option != TypeOption.ORIGINAL)
            conversions.put(main, new Conversion(original, option, additionalEntries));
    }

    /**
     * Creates the items for the given type. The first item replaces the
     * original item, the others are additional fee and tax items.
     */
    private List<Item> createItems(Item original, TypeOption option)
    {
        return switch (option)
        {
            case ORIGINAL -> List.of(original);
            case DELIVERY -> List.of(createDeliveryItem(original));
            case PORTFOLIO_TRANSFER -> createPortfolioTransferItems(original);
            case ACCOUNT_TRANSFER -> List.of(createAccountTransferItem(original));
        };
    }

    /**
     * Converts a buy/sell into a delivery. Same conversion as the InsertAction
     * does for the option to convert buy/sell transactions into deliveries.
     */
    private Item createDeliveryItem(Item original)
    {
        var t = ((BuySellEntry) original.getSubject()).getPortfolioTransaction();

        var delivery = new PortfolioTransaction();
        delivery.setType(t.getType() == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.DELIVERY_INBOUND
                        : PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        delivery.setDateTime(t.getDateTime());
        delivery.setSecurity(t.getSecurity());
        delivery.setMonetaryAmount(t.getMonetaryAmount());
        delivery.setNote(t.getNote());
        delivery.setSource(t.getSource());
        delivery.setShares(t.getShares());
        delivery.addUnits(t.getUnits());

        var item = new Extractor.TransactionItem(delivery);
        item.setPortfolioPrimary(original.getPortfolioPrimary());
        return item;
    }

    /**
     * Converts a buy/sell or a delivery into a portfolio transfer. The transfer
     * carries the market value; fees and taxes are booked separately so that
     * they are not lost.
     */
    private List<Item> createPortfolioTransferItems(Item original)
    {
        var subject = original.getSubject();
        var t = subject instanceof BuySellEntry buySell ? buySell.getPortfolioTransaction()
                        : (PortfolioTransaction) subject;

        var transfer = new PortfolioTransferEntry();
        transfer.setSecurity(t.getSecurity());
        transfer.setDate(t.getDateTime());
        transfer.setShares(t.getShares());
        transfer.setCurrencyCode(t.getCurrencyCode());
        transfer.setAmount(t.getGrossValue().getAmount());
        transfer.setNote(t.getNote());
        transfer.setSource(t.getSource());

        var item = new Extractor.PortfolioTransferItem(transfer, !isInbound(original));
        item.setPortfolioPrimary(original.getPortfolioPrimary());

        var items = new ArrayList<Item>();
        items.add(item);
        addFeeOrTaxItem(items, original, t, Unit.Type.FEE, AccountTransaction.Type.FEES);
        addFeeOrTaxItem(items, original, t, Unit.Type.TAX, AccountTransaction.Type.TAXES);
        return items;
    }

    /**
     * Converts a deposit or removal into an account transfer in the same
     * currency.
     */
    private Item createAccountTransferItem(Item original)
    {
        var t = (AccountTransaction) original.getSubject();

        var transfer = new AccountTransferEntry();
        transfer.setDate(t.getDateTime());
        transfer.setCurrencyCode(t.getCurrencyCode());
        transfer.setAmount(t.getAmount());
        transfer.setNote(t.getNote());
        transfer.setSource(t.getSource());

        var item = new Extractor.AccountTransferItem(transfer, !isInbound(original));
        item.setAccountPrimary(original.getAccountPrimary());
        return item;
    }

    private void addFeeOrTaxItem(List<Item> items, Item original, PortfolioTransaction t, Unit.Type unitType,
                    AccountTransaction.Type type)
    {
        var sum = t.getUnitSum(unitType);
        if (sum.isZero())
            return;

        var transaction = new AccountTransaction();
        transaction.setType(type);
        transaction.setDateTime(t.getDateTime());
        transaction.setSecurity(t.getSecurity());
        transaction.setMonetaryAmount(sum);
        transaction.setNote(t.getNote());
        transaction.setSource(t.getSource());

        var item = new Extractor.TransactionItem(transaction);
        item.setAccountPrimary(original.getAccountPrimary());
        items.add(item);
    }
}

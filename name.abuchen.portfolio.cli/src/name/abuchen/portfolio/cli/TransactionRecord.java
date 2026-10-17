package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;

/**
 * One business event: a buy or sell joins its investment-account and
 * cash-account sides, a transfer joins its outgoing and incoming side. A side
 * whose partner is missing, which only happens in damaged or hand-edited
 * files, is a record of its own under its raw core type.
 *
 * @param id
 *            the investment-account side of a buy or sell, the outgoing side of
 *            a transfer, and the transaction itself otherwise
 * @param type
 *            core's enum name, except that an intact transfer is
 *            <code>TRANSFER</code>
 * @param kind
 *            of a transfer: <code>cash</code> or <code>security</code>
 * @param primary
 *            the side that supplies date, amount, units, note and source
 * @param sides
 *            the UUIDs of all sides, {@code id} first
 * @param incoming
 *            the incoming side of a transfer
 */
record TransactionRecord(String id, String type, Optional<String> kind, Transaction primary, List<String> sides,
                Optional<String> investmentAccount, Optional<String> cashAccount, Optional<String> from,
                Optional<String> to, Optional<Transaction> incoming)
{
    static final String TRANSFER = "TRANSFER";
    static final String CASH = "cash";
    static final String SECURITY = "security";

    /** Whether any side is in the given investment account. */
    boolean touchesInvestmentAccount(String uuid)
    {
        return investmentAccount.filter(uuid::equals).isPresent()
                        || kind.filter(SECURITY::equals).isPresent() && (from.filter(uuid::equals).isPresent()
                                        || to.filter(uuid::equals).isPresent());
    }

    /** Whether any side is in the given cash account. */
    boolean touchesCashAccount(String uuid)
    {
        return cashAccount.filter(uuid::equals).isPresent()
                        || kind.filter(CASH::equals).isPresent() && (from.filter(uuid::equals).isPresent()
                                        || to.filter(uuid::equals).isPresent());
    }

    /** All records of the client, in no particular order. */
    static List<TransactionRecord> allOf(Client client)
    {
        var accountOwners = new IdentityHashMap<Transaction, Account>();
        var portfolioOwners = new IdentityHashMap<Transaction, Portfolio>();
        client.getAccounts().forEach(account -> account.getTransactions().forEach(t -> accountOwners.put(t, account)));
        client.getPortfolios()
                        .forEach(portfolio -> portfolio.getTransactions().forEach(t -> portfolioOwners.put(t, portfolio)));

        var join = new Join(accountOwners, portfolioOwners);
        var records = new ArrayList<TransactionRecord>();
        accountOwners.keySet().forEach(t -> join.toRecord(t).ifPresent(records::add));
        portfolioOwners.keySet().forEach(t -> join.toRecord(t).ifPresent(records::add));
        return records;
    }

    private record Join(IdentityHashMap<Transaction, Account> accountOwners,
                    IdentityHashMap<Transaction, Portfolio> portfolioOwners)
    {
        /** The record this side starts, or empty if its partner starts it. */
        Optional<TransactionRecord> toRecord(Transaction side)
        {
            var partner = partnerOf(side);
            if (partner == null)
                return Optional.of(orphan(side));

            return switch (side.getCrossEntry())
            {
                case BuySellEntry entry -> side == entry.getPortfolioTransaction()
                                ? Optional.of(buySell(entry.getPortfolioTransaction(), entry.getAccountTransaction()))
                                : Optional.empty();
                case AccountTransferEntry entry -> side == entry.getSourceTransaction()
                                ? Optional.of(cashTransfer(entry.getSourceTransaction(), entry.getTargetTransaction()))
                                : Optional.empty();
                case PortfolioTransferEntry entry -> side == entry.getSourceTransaction()
                                ? Optional.of(securityTransfer(entry.getSourceTransaction(),
                                                entry.getTargetTransaction()))
                                : Optional.empty();
                default -> Optional.of(orphan(side));
            };
        }

        /** The other side, or null if there is none or it is in no account or portfolio. */
        private Transaction partnerOf(Transaction side)
        {
            var entry = side.getCrossEntry();
            if (entry == null)
                return null;

            Transaction partner;
            try
            {
                partner = entry.getCrossTransaction(side);
            }
            catch (UnsupportedOperationException e)
            {
                // the entry doesn't know this side: damaged file
                return null;
            }

            return partner != null && (accountOwners.containsKey(partner) || portfolioOwners.containsKey(partner))
                            ? partner
                            : null;
        }

        private TransactionRecord buySell(PortfolioTransaction portfolioSide, AccountTransaction accountSide)
        {
            return new TransactionRecord(portfolioSide.getUUID(), portfolioSide.getType().name(), Optional.empty(),
                            portfolioSide, List.of(portfolioSide.getUUID(), accountSide.getUUID()),
                            uuid(portfolioOwners.get(portfolioSide)), uuid(accountOwners.get(accountSide)),
                            Optional.empty(), Optional.empty(), Optional.empty());
        }

        private TransactionRecord cashTransfer(AccountTransaction outgoing, AccountTransaction incoming)
        {
            return new TransactionRecord(outgoing.getUUID(), TRANSFER, Optional.of(CASH), outgoing,
                            List.of(outgoing.getUUID(), incoming.getUUID()), Optional.empty(), Optional.empty(),
                            uuid(accountOwners.get(outgoing)), uuid(accountOwners.get(incoming)),
                            Optional.of(incoming));
        }

        private TransactionRecord securityTransfer(PortfolioTransaction outgoing, PortfolioTransaction incoming)
        {
            return new TransactionRecord(outgoing.getUUID(), TRANSFER, Optional.of(SECURITY), outgoing,
                            List.of(outgoing.getUUID(), incoming.getUUID()), Optional.empty(), Optional.empty(),
                            uuid(portfolioOwners.get(outgoing)), uuid(portfolioOwners.get(incoming)),
                            Optional.of(incoming));
        }

        private TransactionRecord orphan(Transaction side)
        {
            var type = side instanceof AccountTransaction account ? account.getType().name()
                            : ((PortfolioTransaction) side).getType().name();

            return new TransactionRecord(side.getUUID(), type, Optional.empty(), side, List.of(side.getUUID()),
                            side instanceof PortfolioTransaction ? uuid(portfolioOwners.get(side)) : Optional.empty(),
                            side instanceof AccountTransaction ? uuid(accountOwners.get(side)) : Optional.empty(),
                            Optional.empty(), Optional.empty(), Optional.empty());
        }

        private static Optional<String> uuid(Portfolio portfolio)
        {
            return Optional.ofNullable(portfolio).map(Portfolio::getUUID);
        }

        private static Optional<String> uuid(Account account)
        {
            return Optional.ofNullable(account).map(Account::getUUID);
        }
    }
}

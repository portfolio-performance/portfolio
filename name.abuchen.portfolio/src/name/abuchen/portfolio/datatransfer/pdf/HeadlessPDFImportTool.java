package name.abuchen.portfolio.datatransfer.pdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Headless PDF import command for automation and importer triage.
 */
public final class HeadlessPDFImportTool
{
    private static final String DEFAULT_ACCOUNT = "Imported Cash"; //$NON-NLS-1$
    private static final String DEFAULT_PORTFOLIO = "Imported Depot"; //$NON-NLS-1$
    private static final String DEFAULT_SECONDARY_ACCOUNT = "Imported Transfer Cash"; //$NON-NLS-1$
    private static final String DEFAULT_SECONDARY_PORTFOLIO = "Imported Transfer Depot"; //$NON-NLS-1$
    private static final String INFERRED_CASH_DEPOSIT_NOTE = "Inferred cash funding to keep imported cash balance non-negative"; //$NON-NLS-1$

    private HeadlessPDFImportTool()
    {
        // utility class
    }

    public static void main(String[] args) throws Exception
    {
        Options options = Options.parse(args);
        Result result = run(options);
        result.writeTo(System.out);

        if (options.reportFile != null)
        {
            try (PrintWriter writer = new PrintWriter(options.reportFile, StandardCharsets.UTF_8))
            {
                result.writeTo(writer);
            }
        }

        if (result.hasErrors())
            throw new IllegalStateException("Headless import completed with errors"); //$NON-NLS-1$
    }

    public static Result run(Options options) throws IOException
    {
        Client client = loadClient(options);
        if (options.baseCurrency == null)
            options.baseCurrency = client.getBaseCurrency();
        client.setBaseCurrency(options.baseCurrency);

        ImportContext context = new ImportContext(client, options);

        Map<File, List<Exception>> extractionErrors = new HashMap<>();
        Result result = new Result(options, client);

        if (!options.inputFiles.isEmpty())
        {
            Map<Extractor, List<Extractor.Item>> itemsByExtractor = new PDFImportAssistant(client, options.inputFiles)
                            .run(new NullProgressMonitor(), extractionErrors);

            extractionErrors.forEach((file, errors) -> errors
                            .forEach(error -> result.addExtractionError(file, error)));

            List<ImportAction> checks = List.of(new CheckTransactionDateAction(), new CheckValidTypesAction(),
                            new CheckSecurityRelatedValuesAction(), new DetectDuplicatesAction(client),
                            new CheckCurrenciesAction(), new CheckForexGrossValueAction());

            InsertAction insert = new InsertAction(client);
            insert.setConvertBuySellToDelivery(options.convertBuySellToDelivery);
            insert.setRemoveDividends(options.removeDividends);

            itemsByExtractor.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getLabel())).forEach(
                            e -> importItems(e.getKey(), e.getValue(), checks, insert, context, options, result));
        }

        for (ManualShareUpdate update : options.manualShareUpdates)
            applyManualShareUpdate(client, update, result);

        for (ManualAccountUpdate update : options.manualAccountUpdates)
            applyManualAccountUpdate(client, update, result);

        for (ManualAccountTransaction transaction : options.manualAccountTransactions)
            applyManualAccountTransaction(client, context.getAccount(transaction.currencyCode()), transaction, result);

        for (ManualDelivery delivery : options.manualDeliveries)
            applyManualDelivery(client, context.getPortfolio(), delivery, result);

        for (ManualPortfolioTransfer transfer : options.manualPortfolioTransfers)
            applyManualPortfolioTransfer(client, context, transfer, result);

        if (options.fillZeroTransferValues)
            fillZeroTransferValues(client, result);

        if (options.inferCashDeposits)
            inferCashDeposits(client, result);

        sortAccountTransactions(client);

        ClientFactory.saveAs(client, options.outputFile, null, EnumSet.of(SaveFlag.XML));
        result.savedFile = options.outputFile;

        Client reloaded = ClientFactory.load(options.outputFile, null, new NullProgressMonitor());
        result.reloadedAccounts = reloaded.getAccounts().size();
        result.reloadedPortfolios = reloaded.getPortfolios().size();
        result.reloadedSecurities = reloaded.getSecurities().size();

        return result;
    }

    private static void importItems(Extractor extractor, List<Extractor.Item> items, List<ImportAction> checks,
                    InsertAction insert, ImportContext context, Options options, Result result)
    {
        for (Extractor.Item item : items)
        {
            result.extracted++;

            if (item.isFailure())
            {
                result.reject();
                result.addItemStatus(extractor, item, Code.ERROR, item.getFailureMessage());
                continue;
            }

            if (item.isSkipped())
            {
                result.reject();
                result.addItemStatus(extractor, item, Code.SKIP, "Skipped by extractor"); //$NON-NLS-1$
                continue;
            }

            List<ImportAction.Status> statuses = new ArrayList<>();
            for (ImportAction action : checks)
            {
                try
                {
                    ImportAction.Status status = item.apply(action, context);
                    statuses.add(status);
                    result.addItemStatus(extractor, item, status.getCode(), status.getMessage());
                }
                catch (RuntimeException e)
                {
                    statuses.add(new ImportAction.Status(Code.ERROR, e.getMessage()));
                    result.addItemStatus(extractor, item, Code.ERROR, e.getMessage());
                }
            }

            if (statuses.stream().anyMatch(s -> s.getCode() == Code.ERROR))
            {
                result.reject();
                continue;
            }

            boolean onlyDuplicateWarnings = statuses.stream().anyMatch(s -> s.getCode() != Code.OK) && statuses
                            .stream().filter(s -> s.getCode() != Code.OK)
                            .allMatch(HeadlessPDFImportTool::isDuplicateWarning);
            if (onlyDuplicateWarnings)
            {
                DuplicateDisposition duplicateDisposition = classifyDuplicateWarning(item, context);
                if (duplicateDisposition == DuplicateDisposition.EXACT)
                {
                    result.skipDuplicate();
                    continue;
                }

                if (duplicateDisposition == DuplicateDisposition.UNKNOWN && !options.allowWarnings)
                {
                    result.reject();
                    continue;
                }
            }

            if (!onlyDuplicateWarnings && !options.allowWarnings
                            && statuses.stream().anyMatch(s -> s.getCode() == Code.WARNING))
            {
                result.reject();
                continue;
            }

            insert.setInvestmentPlanItem(item.isInvestmentPlanItem());
            item.apply(insert, context);
            result.imported++;
        }
    }

    private static boolean isDuplicateWarning(ImportAction.Status status)
    {
        return status.getCode() == Code.WARNING && Messages.LabelPotentialDuplicate.equals(status.getMessage());
    }

    private enum DuplicateDisposition
    {
        EXACT, NOT_EXACT, UNKNOWN
    }

    private static DuplicateDisposition classifyDuplicateWarning(Extractor.Item item, ImportContext context)
    {
        if (item.getSubject() instanceof BuySellEntry entry)
            return hasExactBuySellEntry(entry, context) ? DuplicateDisposition.EXACT : DuplicateDisposition.NOT_EXACT;

        if (item.getSubject() instanceof AccountTransaction transaction)
            return hasExactAccountTransaction(transaction, context) ? DuplicateDisposition.EXACT
                            : DuplicateDisposition.NOT_EXACT;

        return DuplicateDisposition.UNKNOWN;
    }

    private static boolean hasExactBuySellEntry(BuySellEntry entry, ImportContext context)
    {
        PortfolioTransaction transaction = entry.getPortfolioTransaction();
        Portfolio portfolio = context.getPortfolio();

        return portfolio.getTransactions().stream()
                        .anyMatch(existing -> existing.getType() == transaction.getType()
                                        && Objects.equals(existing.getDateTime(), transaction.getDateTime())
                                        && Objects.equals(existing.getCurrencyCode(), transaction.getCurrencyCode())
                                        && existing.getAmount() == transaction.getAmount()
                                        && existing.getShares() == transaction.getShares()
                                        && sameSecurity(existing.getSecurity(), transaction.getSecurity()));
    }

    private static boolean hasExactAccountTransaction(AccountTransaction transaction, ImportContext context)
    {
        Account account = context.getAccount(transaction.getCurrencyCode());

        return account.getTransactions().stream()
                        .anyMatch(existing -> existing.getType() == transaction.getType()
                                        && Objects.equals(existing.getDateTime(), transaction.getDateTime())
                                        && Objects.equals(existing.getCurrencyCode(), transaction.getCurrencyCode())
                                        && existing.getAmount() == transaction.getAmount()
                                        && existing.getShares() == transaction.getShares()
                                        && sameSecurity(existing.getSecurity(), transaction.getSecurity()));
    }

    private static boolean sameSecurity(Security left, Security right)
    {
        if (left == right)
            return true;

        if (left == null || right == null)
            return false;

        return Objects.equals(left.getIsin(), right.getIsin()) && Objects.equals(left.getWkn(), right.getWkn())
                        && Objects.equals(left.getName(), right.getName());
    }

    private static void inferCashDeposits(Client client, Result result)
    {
        for (Account account : client.getAccounts())
        {
            normalizeInferredCashDeposits(account);

            long balance = 0;
            List<AccountTransaction> transactions = new ArrayList<>(account.getTransactions());
            transactions.sort(Comparator.comparing(AccountTransaction::getDateTime)
                            .thenComparing(t -> isInferredCashDeposit(t) ? 0 : 1)
                            .thenComparing(AccountTransaction::getUUID));

            for (AccountTransaction transaction : transactions)
            {
                long delta = transaction.getType().isCredit() ? transaction.getAmount() : -transaction.getAmount();
                long nextBalance = balance + delta;

                if (!isInferredCashDeposit(transaction) && nextBalance < 0)
                {
                    long depositAmount = -nextBalance;
                    AccountTransaction deposit = new AccountTransaction(AccountTransaction.Type.DEPOSIT);
                    deposit.setDateTime(inferredCashDepositDateTime(transaction));
                    deposit.setCurrencyCode(account.getCurrencyCode());
                    deposit.setAmount(depositAmount);
                    deposit.setNote(INFERRED_CASH_DEPOSIT_NOTE);
                    deposit.setSource(transaction.getSource());
                    account.addTransaction(deposit);

                    result.addInferredCashDeposit(account, deposit);
                    balance += depositAmount;
                }

                balance += delta;
            }
        }
    }

    private static void sortAccountTransactions(Client client)
    {
        for (Account account : client.getAccounts())
            sortAccountTransactions(account);
    }

    static void sortAccountTransactions(Account account)
    {
        account.getTransactions().sort(Comparator.comparing(AccountTransaction::getDateTime)
                        .thenComparing(t -> isInferredCashDeposit(t) ? 0 : 1)
                        .thenComparing(AccountTransaction::getUUID));
    }

    private static void normalizeInferredCashDeposits(Account account)
    {
        List<AccountTransaction> transactions = account.getTransactions();

        for (AccountTransaction deposit : transactions)
        {
            if (!isInferredCashDeposit(deposit))
                continue;

            transactions.stream()
                            .filter(transaction -> !isInferredCashDeposit(transaction))
                            .filter(transaction -> Objects.equals(transaction.getSource(), deposit.getSource()))
                            .filter(transaction -> transaction.getDateTime().equals(deposit.getDateTime()))
                            .findFirst()
                            .ifPresent(transaction -> deposit.setDateTime(inferredCashDepositDateTime(transaction)));
        }
    }

    private static boolean isInferredCashDeposit(AccountTransaction transaction)
    {
        return transaction.getType() == AccountTransaction.Type.DEPOSIT
                        && INFERRED_CASH_DEPOSIT_NOTE.equals(transaction.getNote());
    }

    static LocalDateTime inferredCashDepositDateTime(AccountTransaction transaction)
    {
        return transaction.getDateTime().minusMinutes(1);
    }

    private static void applyManualDelivery(Client client, Portfolio portfolio, ManualDelivery delivery, Result result)
    {
        Security security = findSecurity(client, delivery.isin()).orElseGet(() -> createManualSecurity(client, delivery));

        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setType(delivery.type());
        transaction.setDateTime(delivery.dateTime());
        transaction.setCurrencyCode(delivery.currencyCode());
        transaction.setAmount(delivery.amount());
        transaction.setSecurity(security);
        transaction.setShares(delivery.shares());
        transaction.setNote(delivery.note());
        transaction.setSource(delivery.source());
        portfolio.addTransaction(transaction);

        result.addManualDelivery(delivery);
    }

    private static Security createManualSecurity(Client client, ManualDelivery delivery)
    {
        if (delivery.securityName() == null || delivery.securityName().isBlank())
            throw new IllegalArgumentException("Unknown security ISIN for manual delivery: " + delivery.isin()); //$NON-NLS-1$

        Security security = new Security(delivery.securityName(), delivery.currencyCode());
        security.setIsin(delivery.isin());
        client.addSecurity(security);
        return security;
    }

    private static Security createManualSecurity(Client client, ManualAccountTransaction item)
    {
        if (item.securityName() == null || item.securityName().isBlank())
            throw new IllegalArgumentException("Unknown security ISIN for manual account transaction: " + item.isin()); //$NON-NLS-1$

        Security security = new Security(item.securityName(), item.currencyCode());
        security.setIsin(item.isin());
        client.addSecurity(security);
        return security;
    }

    private static void applyManualPortfolioTransfer(Client client, ImportContext context, ManualPortfolioTransfer transfer,
                    Result result)
    {
        Security security = findSecurity(client, transfer.isin())
                        .orElseGet(() -> createManualSecurity(client, transfer.asInboundDelivery()));

        PortfolioTransferEntry entry = new PortfolioTransferEntry(context.getPortfolio(),
                        context.getSecondaryPortfolio());
        entry.setDate(transfer.dateTime());
        entry.setCurrencyCode(transfer.currencyCode());
        entry.setAmount(transfer.amount());
        entry.setSecurity(security);
        entry.setShares(transfer.shares());
        entry.setNote(transfer.note());
        entry.setSource(transfer.source());
        entry.insert();

        result.addManualPortfolioTransfer(transfer);
    }

    private static void fillZeroTransferValues(Client client, Result result)
    {
        Map<Portfolio, Map<Security, PositionCost>> positions = new HashMap<>();
        client.getPortfolios().forEach(p -> positions.put(p, new HashMap<>()));

        List<PortfolioEvent> events = new ArrayList<>();
        for (Portfolio portfolio : client.getPortfolios())
            for (PortfolioTransaction transaction : portfolio.getTransactions())
                events.add(new PortfolioEvent(portfolio, transaction));

        events.sort(Comparator.comparing((PortfolioEvent e) -> e.transaction.getDateTime().toLocalDate())
                        .thenComparing(e -> isPortfolioTransfer(e.transaction) ? 1 : 0)
                        .thenComparing(e -> e.transaction.getDateTime()));

        for (PortfolioEvent event : events)
        {
            PortfolioTransaction transaction = event.transaction;
            if (transaction.getSecurity() == null)
                continue;

            if (transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                continue;

            Map<Security, PositionCost> portfolioPositions = positions.get(event.portfolio);
            PositionCost position = portfolioPositions.computeIfAbsent(transaction.getSecurity(), s -> new PositionCost());

            switch (transaction.getType())
            {
                case BUY, DELIVERY_INBOUND -> position.add(transaction.getShares(), transaction.getAmount());
                case SELL, DELIVERY_OUTBOUND -> position.remove(transaction.getShares());
                case TRANSFER_OUT -> fillZeroTransferValue(event.portfolio, transaction, position, positions, result);
                default -> {
                    // Other portfolio transaction types do not affect share cost basis.
                }
            }
        }
    }

    private static boolean isPortfolioTransfer(PortfolioTransaction transaction)
    {
        return transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN
                        || transaction.getType() == PortfolioTransaction.Type.TRANSFER_OUT;
    }

    private static void fillZeroTransferValue(Portfolio sourcePortfolio, PortfolioTransaction sourceTransaction,
                    PositionCost sourcePosition, Map<Portfolio, Map<Security, PositionCost>> positions, Result result)
    {
        if (!(sourceTransaction.getCrossEntry() instanceof PortfolioTransferEntry entry))
        {
            sourcePosition.remove(sourceTransaction.getShares());
            return;
        }

        long cost = sourcePosition.remove(sourceTransaction.getShares());
        if (sourceTransaction.getAmount() == 0 && cost > 0)
        {
            entry.setAmount(cost);
            entry.setNote(appendNote(entry.getNote(),
                            "Cleanup: set transfer value from source position cost basis")); //$NON-NLS-1$
            result.fillZeroTransferValue(sourceTransaction, cost);
        }

        TransactionOwner<?> targetOwner = entry.getCrossOwner(sourceTransaction);
        if (targetOwner instanceof Portfolio targetPortfolio)
        {
            positions.computeIfAbsent(targetPortfolio, p -> new HashMap<>())
                            .computeIfAbsent(sourceTransaction.getSecurity(), s -> new PositionCost())
                            .add(sourceTransaction.getShares(),
                                            sourceTransaction.getAmount() == 0 ? cost : sourceTransaction.getAmount());
        }
        else if (!sourcePortfolio.equals(targetOwner))
        {
            throw new IllegalStateException("Unsupported portfolio transfer target: " + targetOwner); //$NON-NLS-1$
        }
    }

    private static final class PositionCost
    {
        private long shares;
        private long cost;

        void add(long additionalShares, long additionalCost)
        {
            this.shares += additionalShares;
            this.cost += additionalCost;
        }

        long remove(long removedShares)
        {
            if (removedShares <= 0 || shares <= 0 || cost <= 0)
            {
                this.shares -= removedShares;
                return 0;
            }

            long removedCost = BigDecimal.valueOf(cost).multiply(BigDecimal.valueOf(removedShares))
                            .divide(BigDecimal.valueOf(shares), 0, RoundingMode.HALF_UP).longValue();
            this.shares -= removedShares;
            this.cost -= removedCost;
            return removedCost;
        }
    }

    private record PortfolioEvent(Portfolio portfolio, PortfolioTransaction transaction)
    {
    }

    private static void applyManualShareUpdate(Client client, ManualShareUpdate update, Result result)
    {
        List<PortfolioTransaction> matches = client.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getType() == update.type())
                        .filter(t -> Objects.equals(t.getDateTime(), update.dateTime()))
                        .filter(t -> Objects.equals(t.getSource(), update.source()))
                        .filter(t -> t.getSecurity() != null && Objects.equals(t.getSecurity().getIsin(), update.isin()))
                        .filter(t -> t.getShares() == update.oldShares()).toList();

        if (matches.size() != 1)
            throw new IllegalArgumentException("Manual share update expected one matching transaction but found " //$NON-NLS-1$
                            + matches.size() + ": " + update); //$NON-NLS-1$

        PortfolioTransaction transaction = matches.get(0);
        transaction.setShares(update.newShares());
        transaction.setNote(appendNote(transaction.getNote(), update.note()));

        result.addManualShareUpdate(update);
    }

    private static void applyManualAccountUpdate(Client client, ManualAccountUpdate update, Result result)
    {
        List<AccountTransaction> matches = client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .filter(t -> t.getType() == update.type())
                        .filter(t -> Objects.equals(t.getDateTime(), update.dateTime()))
                        .filter(t -> Objects.equals(t.getSource(), update.source()))
                        .filter(t -> Objects.equals(t.getCurrencyCode(), update.currencyCode()))
                        .filter(t -> t.getSecurity() != null && Objects.equals(t.getSecurity().getIsin(), update.isin()))
                        .filter(t -> t.getAmount() == update.oldAmount())
                        .filter(t -> t.getUnitSum(Unit.Type.TAX).getAmount() == update.oldTax()).toList();

        if (matches.size() != 1)
            throw new IllegalArgumentException(
                            "Manual account update expected one matching transaction but found " //$NON-NLS-1$
                                            + matches.size() + ": " + update); //$NON-NLS-1$

        AccountTransaction transaction = matches.get(0);
        transaction.setAmount(update.newAmount());
        transaction.removeUnits(Unit.Type.TAX);
        if (update.newTax() != 0)
            transaction.addUnit(new Unit(Unit.Type.TAX, Money.of(update.currencyCode(), update.newTax())));
        transaction.setNote(appendNote(transaction.getNote(), update.note()));

        result.addManualAccountUpdate(update);
    }

    private static void applyManualAccountTransaction(Client client, Account account, ManualAccountTransaction item,
                    Result result)
    {
        AccountTransaction transaction = new AccountTransaction(item.type());
        transaction.setDateTime(item.dateTime());
        transaction.setCurrencyCode(item.currencyCode());
        transaction.setAmount(item.amount());
        transaction.setNote(item.note());
        transaction.setSource(item.source());

        if (item.isin() != null && !item.isin().isBlank())
        {
            Security security = findSecurity(client, item.isin()).orElseGet(() -> createManualSecurity(client, item));
            transaction.setSecurity(security);
            transaction.setShares(item.shares());
        }

        if (item.tax() != 0)
            transaction.addUnit(new Unit(Unit.Type.TAX, Money.of(item.currencyCode(), item.tax())));

        account.addTransaction(transaction);
        result.addManualAccountTransaction(item);
    }

    private static String appendNote(String existing, String addition)
    {
        if (existing == null || existing.isBlank())
            return addition;
        return existing + " | " + addition; //$NON-NLS-1$
    }

    private static Optional<Security> findSecurity(Client client, String isin)
    {
        return client.getSecurities().stream().filter(s -> Objects.equals(isin, s.getIsin())).findFirst();
    }

    private static Client loadClient(Options options) throws IOException
    {
        if (options.clientFile == null)
            return new Client();

        return ClientFactory.load(options.clientFile, null, new NullProgressMonitor());
    }

    public static final class Options
    {
        private final List<File> inputFiles = new ArrayList<>();
        private File clientFile;
        private File outputFile;
        private File reportFile;
        private String baseCurrency;
        private String accountName = DEFAULT_ACCOUNT;
        private String portfolioName = DEFAULT_PORTFOLIO;
        private String secondaryAccountName = DEFAULT_SECONDARY_ACCOUNT;
        private String secondaryPortfolioName = DEFAULT_SECONDARY_PORTFOLIO;
        private boolean allowWarnings;
        private boolean convertBuySellToDelivery;
        private boolean removeDividends;
        private boolean inferCashDeposits;
        private boolean normalizeOnly;
        private boolean fillZeroTransferValues;
        private boolean reportHoldings;
        private final List<ManualShareUpdate> manualShareUpdates = new ArrayList<>();
        private final List<ManualAccountUpdate> manualAccountUpdates = new ArrayList<>();
        private final List<ManualAccountTransaction> manualAccountTransactions = new ArrayList<>();
        private final List<ManualDelivery> manualDeliveries = new ArrayList<>();
        private final List<ManualPortfolioTransfer> manualPortfolioTransfers = new ArrayList<>();

        public static Options parse(String[] args)
        {
            Options options = new Options();

            for (int ii = 0; ii < args.length; ii++)
            {
                String arg = args[ii];
                switch (arg)
                {
                    case "--input":
                        options.addInput(new File(required(args, ++ii, arg)));
                        break;
                    case "--client":
                        options.clientFile = new File(required(args, ++ii, arg));
                        break;
                    case "--output":
                        options.outputFile = new File(required(args, ++ii, arg));
                        break;
                    case "--report":
                        options.reportFile = new File(required(args, ++ii, arg));
                        break;
                    case "--base-currency":
                        options.baseCurrency = required(args, ++ii, arg);
                        break;
                    case "--account":
                        options.accountName = required(args, ++ii, arg);
                        break;
                    case "--portfolio":
                        options.portfolioName = required(args, ++ii, arg);
                        break;
                    case "--secondary-account":
                        options.secondaryAccountName = required(args, ++ii, arg);
                        break;
                    case "--secondary-portfolio":
                        options.secondaryPortfolioName = required(args, ++ii, arg);
                        break;
                    case "--allow-warnings":
                        options.allowWarnings = true;
                        break;
                    case "--convert-buy-sell-to-delivery":
                        options.convertBuySellToDelivery = true;
                        break;
                    case "--remove-dividends":
                        options.removeDividends = true;
                        break;
                    case "--infer-cash-deposits":
                        options.inferCashDeposits = true;
                        break;
                    case "--normalize-only":
                        options.normalizeOnly = true;
                        break;
                    case "--fill-zero-transfer-values":
                        options.fillZeroTransferValues = true;
                        break;
                    case "--report-holdings":
                        options.reportHoldings = true;
                        break;
                    case "--manual-delivery":
                        options.manualDeliveries.add(ManualDelivery.parse(required(args, ++ii, arg)));
                        break;
                    case "--manual-portfolio-transfer":
                        options.manualPortfolioTransfers.add(ManualPortfolioTransfer.parse(required(args, ++ii, arg)));
                        break;
                    case "--manual-share-update":
                        options.manualShareUpdates.add(ManualShareUpdate.parse(required(args, ++ii, arg)));
                        break;
                    case "--manual-account-update":
                        options.manualAccountUpdates.add(ManualAccountUpdate.parse(required(args, ++ii, arg)));
                        break;
                    case "--manual-account-transaction":
                        options.manualAccountTransactions
                                        .add(ManualAccountTransaction.parse(required(args, ++ii, arg)));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + arg); //$NON-NLS-1$
                }
            }

            options.validate();
            return options;
        }

        private static String required(String[] args, int index, String option)
        {
            if (index >= args.length)
                throw new IllegalArgumentException("Missing value for " + option); //$NON-NLS-1$
            return args[index];
        }

        private void addInput(File file)
        {
            if (!file.exists())
                throw new IllegalArgumentException("Input does not exist: " + file.getPath()); //$NON-NLS-1$

            if (file.isDirectory())
            {
                File[] files = file.listFiles();
                if (files != null)
                {
                    for (File child : files)
                        addInput(child);
                }
            }
            else if (file.getName().toLowerCase().endsWith(".pdf")) //$NON-NLS-1$
            {
                inputFiles.add(file);
            }
        }

        private void validate()
        {
            if (normalizeOnly && clientFile == null)
                throw new IllegalArgumentException("--normalize-only requires --client"); //$NON-NLS-1$
            if (!normalizeOnly && inputFiles.isEmpty() && manualDeliveries.isEmpty() && manualShareUpdates.isEmpty()
                            && manualAccountUpdates.isEmpty() && manualAccountTransactions.isEmpty()
                            && manualPortfolioTransfers.isEmpty() && !fillZeroTransferValues && !reportHoldings)
                throw new IllegalArgumentException(
                                "At least one --input PDF file or directory, --manual-delivery, --manual-portfolio-transfer, --manual-share-update, --manual-account-update, --manual-account-transaction, --fill-zero-transfer-values, or --report-holdings is required"); //$NON-NLS-1$
            if (reportHoldings && clientFile == null && inputFiles.isEmpty())
                throw new IllegalArgumentException("--report-holdings requires --client when no input is provided"); //$NON-NLS-1$
            if (outputFile == null)
                throw new IllegalArgumentException("--output is required"); //$NON-NLS-1$
            if (baseCurrency != null && CurrencyUnit.getInstance(baseCurrency) == null)
                throw new IllegalArgumentException("Unsupported base currency: " + baseCurrency); //$NON-NLS-1$

            inputFiles.sort(Comparator.comparing(File::getPath));
        }
    }

    private record ManualPortfolioTransfer(LocalDateTime dateTime, String isin, long shares, long amount,
                    String currencyCode, String note, String source, String securityName)
    {
        private static ManualPortfolioTransfer parse(String value)
        {
            String[] parts = value.split("\\|", -1); //$NON-NLS-1$
            if (parts.length != 7 && parts.length != 8)
                throw new IllegalArgumentException(
                                "--manual-portfolio-transfer expects dateTime|isin|shares|amount|currency|note|source[|securityName]"); //$NON-NLS-1$

            String currencyCode = parts[4];
            if (CurrencyUnit.getInstance(currencyCode) == null)
                throw new IllegalArgumentException("Unsupported manual portfolio transfer currency: " + currencyCode); //$NON-NLS-1$

            String securityName = parts.length == 8 ? parts[7] : null;
            return new ManualPortfolioTransfer(LocalDateTime.parse(parts[0]), parts[1], parseShares(parts[2]),
                            parseAmount(parts[3]), currencyCode, parts[5], parts[6], securityName);
        }

        private ManualDelivery asInboundDelivery()
        {
            return new ManualDelivery(PortfolioTransaction.Type.DELIVERY_INBOUND, dateTime, isin, shares, amount,
                            currencyCode, note, source, securityName);
        }

        private static long parseShares(String value)
        {
            return Values.Share.factorize(Double.parseDouble(value));
        }

        private static long parseAmount(String value)
        {
            return new BigDecimal(value).movePointRight(2).setScale(0).longValueExact();
        }
    }

    private record ManualDelivery(PortfolioTransaction.Type type, LocalDateTime dateTime, String isin, long shares,
                    long amount, String currencyCode, String note, String source, String securityName)
    {
        private static ManualDelivery parse(String value)
        {
            String[] parts = value.split("\\|", -1); //$NON-NLS-1$
            if (parts.length != 8 && parts.length != 9)
                throw new IllegalArgumentException(
                                "--manual-delivery expects type|dateTime|isin|shares|amount|currency|note|source[|securityName]"); //$NON-NLS-1$

            PortfolioTransaction.Type type = PortfolioTransaction.Type.valueOf(parts[0]);
            if (type != PortfolioTransaction.Type.DELIVERY_INBOUND && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                throw new IllegalArgumentException("--manual-delivery only supports DELIVERY_INBOUND/OUTBOUND"); //$NON-NLS-1$

            String currencyCode = parts[5];
            if (CurrencyUnit.getInstance(currencyCode) == null)
                throw new IllegalArgumentException("Unsupported manual delivery currency: " + currencyCode); //$NON-NLS-1$

            String securityName = parts.length == 9 ? parts[8] : null;
            return new ManualDelivery(type, LocalDateTime.parse(parts[1]), parts[2], parseShares(parts[3]),
                            parseAmount(parts[4]), currencyCode, parts[6], parts[7], securityName);
        }

        private static long parseShares(String value)
        {
            return Values.Share.factorize(Double.parseDouble(value));
        }

        private static long parseAmount(String value)
        {
            return new BigDecimal(value).movePointRight(2).setScale(0).longValueExact();
        }
    }

    private record ManualShareUpdate(PortfolioTransaction.Type type, LocalDateTime dateTime, String isin, String source,
                    long oldShares, long newShares, String note)
    {
        private static ManualShareUpdate parse(String value)
        {
            String[] parts = value.split("\\|", -1); //$NON-NLS-1$
            if (parts.length != 7)
                throw new IllegalArgumentException(
                                "--manual-share-update expects type|dateTime|isin|source|oldShares|newShares|note"); //$NON-NLS-1$

            return new ManualShareUpdate(PortfolioTransaction.Type.valueOf(parts[0]), LocalDateTime.parse(parts[1]),
                            parts[2], parts[3], parseShares(parts[4]), parseShares(parts[5]), parts[6]);
        }

        private static long parseShares(String value)
        {
            return Values.Share.factorize(Double.parseDouble(value));
        }
    }

    private record ManualAccountUpdate(AccountTransaction.Type type, LocalDateTime dateTime, String isin, String source,
                    long oldAmount, long newAmount, String currencyCode, long oldTax, long newTax, String note)
    {
        private static ManualAccountUpdate parse(String value)
        {
            String[] parts = value.split("\\|", -1); //$NON-NLS-1$
            if (parts.length != 10)
                throw new IllegalArgumentException(
                                "--manual-account-update expects type|dateTime|isin|source|oldAmount|newAmount|currency|oldTax|newTax|note"); //$NON-NLS-1$

            String currencyCode = parts[6];
            if (CurrencyUnit.getInstance(currencyCode) == null)
                throw new IllegalArgumentException("Unsupported manual account update currency: " + currencyCode); //$NON-NLS-1$

            return new ManualAccountUpdate(AccountTransaction.Type.valueOf(parts[0]), LocalDateTime.parse(parts[1]),
                            parts[2], parts[3], parseAmount(parts[4]), parseAmount(parts[5]), currencyCode,
                            parseAmount(parts[7]), parseAmount(parts[8]), parts[9]);
        }

        private static long parseAmount(String value)
        {
            return new BigDecimal(value).movePointRight(2).setScale(0).longValueExact();
        }
    }

    private record ManualAccountTransaction(AccountTransaction.Type type, LocalDateTime dateTime, String isin,
                    long shares, long amount, String currencyCode, long tax, String note, String source,
                    String securityName)
    {
        private static ManualAccountTransaction parse(String value)
        {
            String[] parts = value.split("\\|", -1); //$NON-NLS-1$
            if (parts.length != 9 && parts.length != 10)
                throw new IllegalArgumentException(
                                "--manual-account-transaction expects type|dateTime|isin|shares|amount|currency|tax|note|source[|securityName]"); //$NON-NLS-1$

            String currencyCode = parts[5];
            if (CurrencyUnit.getInstance(currencyCode) == null)
                throw new IllegalArgumentException("Unsupported manual account transaction currency: " + currencyCode); //$NON-NLS-1$

            String securityName = parts.length == 10 ? parts[9] : null;
            return new ManualAccountTransaction(AccountTransaction.Type.valueOf(parts[0]),
                            LocalDateTime.parse(parts[1]), parts[2], parseShares(parts[3]), parseAmount(parts[4]),
                            currencyCode, parseAmount(parts[6]), parts[7], parts[8], securityName);
        }

        private static long parseShares(String value)
        {
            return Values.Share.factorize(Double.parseDouble(value));
        }

        private static long parseAmount(String value)
        {
            return new BigDecimal(value).movePointRight(2).setScale(0).longValueExact();
        }
    }

    private static final class ImportContext implements ImportAction.Context
    {
        private final Client client;
        private final Options options;
        private final Map<String, Account> primaryAccounts = new HashMap<>();
        private final Map<String, Account> secondaryAccounts = new HashMap<>();
        private Portfolio primaryPortfolio;
        private Portfolio secondaryPortfolio;

        private ImportContext(Client client, Options options)
        {
            this.client = client;
            this.options = options;
        }

        @Override
        public Account getAccount(String currencyCode)
        {
            return primaryAccounts.computeIfAbsent(currencyCode,
                            c -> findOrCreateAccount(options.accountName, c));
        }

        @Override
        public Portfolio getPortfolio()
        {
            if (primaryPortfolio == null)
                primaryPortfolio = findOrCreatePortfolio(options.portfolioName, getAccount(options.baseCurrency));
            return primaryPortfolio;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            return secondaryAccounts.computeIfAbsent(currencyCode,
                            c -> findOrCreateAccount(options.secondaryAccountName, c));
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            if (secondaryPortfolio == null)
                secondaryPortfolio = findOrCreatePortfolio(options.secondaryPortfolioName,
                                getSecondaryAccount(options.baseCurrency));
            return secondaryPortfolio;
        }

        private Account findOrCreateAccount(String baseName, String currencyCode)
        {
            String name = baseName + " " + currencyCode; //$NON-NLS-1$
            return client.getAccounts().stream()
                            .filter(a -> Objects.equals(a.getName(), name)
                                            && Objects.equals(a.getCurrencyCode(), currencyCode))
                            .findFirst().orElseGet(() -> {
                                Account account = new Account(name);
                                account.setCurrencyCode(currencyCode);
                                client.addAccount(account);
                                return account;
                            });
        }

        private Portfolio findOrCreatePortfolio(String name, Account referenceAccount)
        {
            return client.getPortfolios().stream().filter(p -> Objects.equals(p.getName(), name)).findFirst()
                            .orElseGet(() -> {
                                Portfolio portfolio = new Portfolio(name);
                                portfolio.setReferenceAccount(referenceAccount);
                                client.addPortfolio(portfolio);
                                return portfolio;
                            });
        }
    }

    public static final class Result
    {
        private final Options options;
        private final Client client;
        private final List<String> lines = new ArrayList<>();
        private int rejected;
        private int extracted;
        private int imported;
        private int skippedDuplicates;
        private int inferredCashDeposits;
        private int manualShareUpdates;
        private int manualAccountUpdates;
        private int manualAccountTransactions;
        private int manualDeliveries;
        private int manualPortfolioTransfers;
        private int filledZeroTransferValues;
        private long filledZeroTransferValueTotal;
        private final Map<String, Long> inferredCashDepositTotals = new HashMap<>();
        private File savedFile;
        private int reloadedAccounts;
        private int reloadedPortfolios;
        private int reloadedSecurities;

        private Result(Options options, Client client)
        {
            this.options = options;
            this.client = client;
        }

        private void addExtractionError(File file, Exception error)
        {
            rejected++;
            lines.add(MessageFormat.format("ERROR\t{0}\t{1}", file.getPath(), error.getMessage())); //$NON-NLS-1$
        }

        private void addItemStatus(Extractor extractor, Extractor.Item item, Code code, String message)
        {
            if (code == Code.OK)
                return;

            lines.add(MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}", code, extractor.getLabel(), //$NON-NLS-1$
                            item.getSource(), item.getTypeInformation(), message));
        }

        private void addInferredCashDeposit(Account account, AccountTransaction deposit)
        {
            inferredCashDeposits++;
            inferredCashDepositTotals.merge(deposit.getCurrencyCode(), deposit.getAmount(), Long::sum);
            lines.add(MessageFormat.format("INFO\t{0}\tINFERRED_CASH_DEPOSIT\t{1}\t{2} {3}\t{4}", //$NON-NLS-1$
                            account.getName(), deposit.getDateTime(), deposit.getCurrencyCode(),
                            formatAmount(deposit.getAmount()), deposit.getSource()));
        }

        private void addManualDelivery(ManualDelivery delivery)
        {
            manualDeliveries++;
            lines.add(MessageFormat.format("INFO\tMANUAL_DELIVERY\t{0}\t{1}\t{2}\t{3}\t{4}", //$NON-NLS-1$
                            delivery.type(), delivery.dateTime(), delivery.isin(),
                            Values.Share.format(delivery.shares()), delivery.source()));
        }

        private void addManualPortfolioTransfer(ManualPortfolioTransfer transfer)
        {
            manualPortfolioTransfers++;
            lines.add(MessageFormat.format("INFO\tMANUAL_PORTFOLIO_TRANSFER\t{0}\t{1}\t{2}\t{3}\t{4}", //$NON-NLS-1$
                            transfer.dateTime(), transfer.isin(), Values.Share.format(transfer.shares()),
                            transfer.currencyCode(), transfer.source()));
        }

        private void fillZeroTransferValue(PortfolioTransaction transaction, long amount)
        {
            filledZeroTransferValues++;
            filledZeroTransferValueTotal += amount;
            lines.add(MessageFormat.format("INFO\tFILL_ZERO_TRANSFER_VALUE\t{0}\t{1}\t{2}\t{3}\t{4}", //$NON-NLS-1$
                            transaction.getDateTime(), transaction.getSecurity().getIsin(),
                            Values.Share.format(transaction.getShares()), transaction.getCurrencyCode(),
                            formatAmount(amount)));
        }

        private void addManualShareUpdate(ManualShareUpdate update)
        {
            manualShareUpdates++;
            lines.add(MessageFormat.format("INFO\tMANUAL_SHARE_UPDATE\t{0}\t{1}\t{2}\t{3} -> {4}\t{5}", //$NON-NLS-1$
                            update.type(), update.dateTime(), update.isin(), Values.Share.format(update.oldShares()),
                            Values.Share.format(update.newShares()), update.source()));
        }

        private void addManualAccountUpdate(ManualAccountUpdate update)
        {
            manualAccountUpdates++;
            lines.add(MessageFormat.format(
                            "INFO\tMANUAL_ACCOUNT_UPDATE\t{0}\t{1}\t{2}\tamount {3} -> {4}\ttax {5} -> {6}\t{7}", //$NON-NLS-1$
                            update.type(), update.dateTime(), update.isin(), formatAmount(update.oldAmount()),
                            formatAmount(update.newAmount()), formatAmount(update.oldTax()), formatAmount(update.newTax()),
                            update.source()));
        }

        private void addManualAccountTransaction(ManualAccountTransaction item)
        {
            manualAccountTransactions++;
            lines.add(MessageFormat.format(
                            "INFO\tMANUAL_ACCOUNT_TRANSACTION\t{0}\t{1}\t{2}\t{3}\tamount {4}\ttax {5}\t{6}", //$NON-NLS-1$
                            item.type(), item.dateTime(), item.isin(), Values.Share.format(item.shares()),
                            formatAmount(item.amount()), formatAmount(item.tax()), item.source()));
        }

        public boolean hasErrors()
        {
            return rejected > 0;
        }

        private void reject()
        {
            rejected++;
        }

        private void skipDuplicate()
        {
            skippedDuplicates++;
        }

        private void writeTo(Appendable out)
        {
            try
            {
                out.append("Headless PDF import\n"); //$NON-NLS-1$
                out.append("Input files: ").append(String.valueOf(options.inputFiles.size())).append('\n'); //$NON-NLS-1$
                out.append("Extracted items: ").append(String.valueOf(extracted)).append('\n'); //$NON-NLS-1$
                out.append("Imported items: ").append(String.valueOf(imported)).append('\n'); //$NON-NLS-1$
                out.append("Skipped duplicate items: ").append(String.valueOf(skippedDuplicates)).append('\n'); //$NON-NLS-1$
                out.append("Rejected items: ").append(String.valueOf(rejected)).append('\n'); //$NON-NLS-1$
                out.append("Manual share updates: ").append(String.valueOf(manualShareUpdates)).append('\n'); //$NON-NLS-1$
                out.append("Manual account updates: ").append(String.valueOf(manualAccountUpdates)).append('\n'); //$NON-NLS-1$
                out.append("Manual account transactions: ").append(String.valueOf(manualAccountTransactions)).append('\n'); //$NON-NLS-1$
                out.append("Manual deliveries: ").append(String.valueOf(manualDeliveries)).append('\n'); //$NON-NLS-1$
                out.append("Manual portfolio transfers: ").append(String.valueOf(manualPortfolioTransfers)).append('\n'); //$NON-NLS-1$
                out.append("Filled zero transfer values: ").append(String.valueOf(filledZeroTransferValues)).append('\n'); //$NON-NLS-1$
                out.append("Filled zero transfer value total: ")
                                .append(formatAmount(filledZeroTransferValueTotal)).append('\n'); //$NON-NLS-1$
                out.append("Inferred cash deposits: ").append(String.valueOf(inferredCashDeposits)).append('\n'); //$NON-NLS-1$
                out.append("Inferred cash totals: ").append(formatTotals(inferredCashDepositTotals)).append('\n'); //$NON-NLS-1$
                out.append("Accounts: ").append(String.valueOf(client.getAccounts().size())).append('\n'); //$NON-NLS-1$
                out.append("Portfolios: ").append(String.valueOf(client.getPortfolios().size())).append('\n'); //$NON-NLS-1$
                out.append("Securities: ").append(String.valueOf(client.getSecurities().size())).append('\n'); //$NON-NLS-1$
                out.append("Saved: ").append(savedFile != null ? savedFile.getPath() : "").append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
                out.append("Reloaded accounts: ").append(String.valueOf(reloadedAccounts)).append('\n'); //$NON-NLS-1$
                out.append("Reloaded portfolios: ").append(String.valueOf(reloadedPortfolios)).append('\n'); //$NON-NLS-1$
                out.append("Reloaded securities: ").append(String.valueOf(reloadedSecurities)).append('\n'); //$NON-NLS-1$
                for (String line : lines)
                    out.append(line).append('\n');
                if (options.reportHoldings)
                    writeHoldings(out);
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
        }

        private void writeHoldings(Appendable out) throws IOException
        {
            List<Portfolio> portfolios = new ArrayList<>(client.getPortfolios());
            portfolios.sort(Comparator.comparing(Portfolio::getName));

            for (Portfolio portfolio : portfolios)
            {
                Map<Security, Long> sharesBySecurity = new HashMap<>();

                for (PortfolioTransaction transaction : portfolio.getTransactions())
                {
                    Security security = transaction.getSecurity();
                    if (security == null)
                        continue;

                    long delta = switch (transaction.getType())
                    {
                        case BUY, DELIVERY_INBOUND, TRANSFER_IN -> transaction.getShares();
                        case SELL, DELIVERY_OUTBOUND, TRANSFER_OUT -> -transaction.getShares();
                        default -> 0;
                    };

                    if (delta != 0)
                        sharesBySecurity.merge(security, delta, Long::sum);
                }

                List<Map.Entry<Security, Long>> holdings = new ArrayList<>(sharesBySecurity.entrySet());
                holdings.removeIf(e -> e.getValue() == 0);
                holdings.sort(Comparator
                                .comparing((Map.Entry<Security, Long> e) -> safe(e.getKey().getName()))
                                .thenComparing(e -> safe(e.getKey().getIsin())));

                for (Map.Entry<Security, Long> holding : holdings)
                {
                    Security security = holding.getKey();
                    out.append(MessageFormat.format("INFO\tHOLDING\t{0}\t{1}\t{2}\t{3}", //$NON-NLS-1$
                                    portfolio.getName(), safe(security.getIsin()), safe(security.getName()),
                                    Values.Share.format(holding.getValue()))).append('\n');
                }
            }
        }

        private static String safe(String value)
        {
            return value != null ? value : ""; //$NON-NLS-1$
        }

        private static String formatTotals(Map<String, Long> totals)
        {
            if (totals.isEmpty())
                return ""; //$NON-NLS-1$

            List<String> values = new ArrayList<>();
            totals.entrySet().stream().sorted(Map.Entry.comparingByKey())
                            .forEach(e -> values.add(e.getKey() + " " + formatAmount(e.getValue()))); //$NON-NLS-1$
            return String.join(", ", values); //$NON-NLS-1$
        }

        private static String formatAmount(long amount)
        {
            return BigDecimal.valueOf(amount, 2).toPlainString();
        }
    }
}

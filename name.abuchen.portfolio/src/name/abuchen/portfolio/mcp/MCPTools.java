package name.abuchen.portfolio.mcp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class MCPTools
{
    private static final Set<AccountTransaction.Type> SIMPLE_ACCOUNT_TYPES = EnumSet.of( //
                    AccountTransaction.Type.DEPOSIT, //
                    AccountTransaction.Type.REMOVAL, //
                    AccountTransaction.Type.INTEREST, //
                    AccountTransaction.Type.INTEREST_CHARGE, //
                    AccountTransaction.Type.DIVIDENDS, //
                    AccountTransaction.Type.FEES, //
                    AccountTransaction.Type.FEES_REFUND, //
                    AccountTransaction.Type.TAXES, //
                    AccountTransaction.Type.TAX_REFUND);

    private static final Set<PortfolioTransaction.Type> SECURITY_TYPES = EnumSet.of( //
                    PortfolioTransaction.Type.BUY, //
                    PortfolioTransaction.Type.SELL);

    private final ClientSource clientSource;
    private final ModelExecutor modelExecutor;
    private final boolean autoSave;

    public MCPTools(ClientSource clientSource, ModelExecutor modelExecutor, boolean autoSave)
    {
        this.clientSource = clientSource;
        this.modelExecutor = modelExecutor;
        this.autoSave = autoSave;
    }

    public JsonArray listToolDefinitions()
    {
        JsonArray tools = new JsonArray();

        tools.add(toolDefinition("list_open_files", "List portfolio files currently open in the application.", //
                        schemaObject()));
        tools.add(toolDefinition("list_accounts",
                        "List accounts and portfolios in an open portfolio file. Requires file id from list_open_files.",
                        schemaObject(required("file", "string", "Open file id from list_open_files"))));
        tools.add(toolDefinition("list_securities",
                        "List securities in an open portfolio file. Requires file id from list_open_files.",
                        schemaObject(required("file", "string", "Open file id from list_open_files"))));
        tools.add(toolDefinition("list_transactions",
                        "List transactions in an open portfolio file, optionally filtered by account. Transfer transactions include counterparty and direction fields.",
                        schemaObject(required("file", "string", "Open file id from list_open_files"), //
                                        optional("account", "string", "Account uuid or name"))));
        tools.add(toolDefinition("add_transaction",
                        "Add a transaction to an open portfolio file. Types: simple account (DEPOSIT, REMOVAL, INTEREST, etc.), BUY/SELL, TRANSFER (account to account, use fromAccount/toAccount, optional targetAmount for FX), SECURITY_TRANSFER (portfolio to portfolio, use fromPortfolio/toPortfolio), DELIVERY_INBOUND/DELIVERY_OUTBOUND (single portfolio).",
                        schemaObject(required("file", "string", "Open file id from list_open_files"), //
                                        required("type", "string", "Transaction type"), //
                                        optional("account", "string", "Account uuid or name"), //
                                        optional("fromAccount", "string", "Source account for TRANSFER"), //
                                        optional("toAccount", "string", "Target account for TRANSFER"), //
                                        optional("portfolio", "string", "Portfolio uuid or name"), //
                                        optional("fromPortfolio", "string", "Source portfolio for SECURITY_TRANSFER"), //
                                        optional("toPortfolio", "string", "Target portfolio for SECURITY_TRANSFER"), //
                                        optional("security", "string", "Security uuid or name"), //
                                        optional("date", "string", "ISO date or date-time"), //
                                        optional("amount", "number", "Amount in source/account currency"), //
                                        optional("targetAmount", "number", "Target amount for FX account transfers"), //
                                        optional("shares", "number", "Number of shares"), //
                                        optional("note", "string", "Note"), //
                                        optional("fee", "number", "Fee amount"), //
                                        optional("tax", "number", "Tax amount"))));
        tools.add(toolDefinition("update_transaction",
                        "Update an existing transaction in an open portfolio file. For transfers, date/note/security/shares propagate to the linked leg. FX account transfers accept amount (source) and targetAmount.",
                        schemaObject(required("file", "string", "Open file id from list_open_files"), //
                                        required("id", "string", "Transaction uuid"), //
                                        optional("type", "string", "Transaction type"), //
                                        optional("date", "string", "ISO date or date-time"), //
                                        optional("amount", "number", "Amount in source/account currency"), //
                                        optional("targetAmount", "number", "Target amount for FX account transfers"), //
                                        optional("shares", "number", "Number of shares"), //
                                        optional("security", "string", "Security uuid or name"), //
                                        optional("note", "string", "Note"), //
                                        optional("fee", "number", "Fee amount"), //
                                        optional("tax", "number", "Tax amount"))));
        tools.add(toolDefinition("delete_transaction",
                        "Delete an existing transaction from an open portfolio file. For buy/sell transactions, the linked portfolio and account legs are removed together.",
                        schemaObject(required("file", "string", "Open file id from list_open_files"), //
                                        required("id", "string", "Transaction uuid"))));

        return tools;
    }

    public String callTool(String name, JsonObject arguments) throws Exception
    {
        return switch (name)
        {
            case "list_open_files" -> toJson(callListOpenFiles());
            case "list_accounts" -> toJson(callListAccounts(arguments));
            case "list_securities" -> toJson(callListSecurities(arguments));
            case "list_transactions" -> toJson(callListTransactions(arguments));
            case "add_transaction" -> toJson(callAddTransaction(arguments));
            case "update_transaction" -> toJson(callUpdateTransaction(arguments));
            case "delete_transaction" -> toJson(callDeleteTransaction(arguments));
            default -> throw new MCPException("Unknown tool: " + name);
        };
    }

    private JsonArray callListOpenFiles() throws Exception
    {
        return modelExecutor.execute(() -> {
            JsonArray result = new JsonArray();
            for (OpenFile openFile : clientSource.listOpenFiles())
            {
                JsonObject item = new JsonObject();
                item.addProperty("id", openFile.id());
                item.addProperty("label", openFile.label());
                item.addProperty("path", openFile.file() != null ? openFile.file().getAbsolutePath() : null);
                item.addProperty("dirty", openFile.dirty());
                item.addProperty("baseCurrency", openFile.client().getBaseCurrency());
                result.add(item);
            }
            return result;
        });
    }

    private JsonObject callListAccounts(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            Client client = openFile.client();

            JsonArray accounts = new JsonArray();
            for (Account account : client.getAccounts())
            {
                JsonObject item = new JsonObject();
                item.addProperty("uuid", account.getUUID());
                item.addProperty("name", account.getName());
                item.addProperty("currency", account.getCurrencyCode());
                item.addProperty("retired", account.isRetired());
                if (account.getNote() != null && !account.getNote().isBlank())
                    item.addProperty("note", account.getNote());
                accounts.add(item);
            }

            JsonArray portfolios = new JsonArray();
            for (Portfolio portfolio : client.getPortfolios())
            {
                JsonObject item = new JsonObject();
                item.addProperty("uuid", portfolio.getUUID());
                item.addProperty("name", portfolio.getName());
                if (portfolio.getReferenceAccount() != null)
                    item.addProperty("referenceAccount", portfolio.getReferenceAccount().getUUID());
                if (portfolio.getNote() != null && !portfolio.getNote().isBlank())
                    item.addProperty("note", portfolio.getNote());
                portfolios.add(item);
            }

            JsonObject result = new JsonObject();
            result.add("accounts", accounts);
            result.add("portfolios", portfolios);
            return result;
        });
    }

    private JsonArray callListSecurities(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            JsonArray result = new JsonArray();
            for (Security security : openFile.client().getSecurities())
            {
                JsonObject item = new JsonObject();
                item.addProperty("uuid", security.getUUID());
                item.addProperty("name", security.getName());
                item.addProperty("isin", security.getIsin());
                item.addProperty("tickerSymbol", security.getTickerSymbol());
                item.addProperty("currency", security.getCurrencyCode());
                result.add(item);
            }
            return result;
        });
    }

    private JsonArray callListTransactions(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            Client client = openFile.client();
            String accountFilter = getString(arguments, "account");

            JsonArray result = new JsonArray();

            for (Account account : client.getAccounts())
            {
                if (!matchesAccountFilter(account, accountFilter))
                    continue;

                for (AccountTransaction transaction : account.getTransactions())
                {
                    if (transaction.getType() == AccountTransaction.Type.BUY)
                        continue;

                    result.add(toTransactionJson("account", account.getUUID(), account.getName(), transaction));
                }
            }

            for (Portfolio portfolio : client.getPortfolios())
            {
                for (PortfolioTransaction transaction : portfolio.getTransactions())
                {
                    Account linkedAccount = findLinkedAccount(transaction);
                    if (accountFilter != null && linkedAccount != null
                                    && !matchesAccountFilter(linkedAccount, accountFilter))
                        continue;
                    if (accountFilter != null && linkedAccount == null)
                        continue;

                    result.add(toTransactionJson("portfolio", portfolio.getUUID(), portfolio.getName(), transaction));
                }
            }

            return result;
        });
    }

    private JsonObject callAddTransaction(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            Client client = openFile.client();

            String typeName = requireString(arguments, "type");
            JsonObject result;

            if ("TRANSFER".equals(typeName))
            {
                result = addAccountTransfer(client, arguments);
            }
            else if ("SECURITY_TRANSFER".equals(typeName))
            {
                result = addSecurityTransfer(client, arguments);
            }
            else if ("DELIVERY_INBOUND".equals(typeName) || "DELIVERY_OUTBOUND".equals(typeName))
            {
                result = addDelivery(client, arguments, PortfolioTransaction.Type.valueOf(typeName));
            }
            else if (isSecurityType(typeName))
            {
                result = addBuySell(client, arguments, PortfolioTransaction.Type.valueOf(typeName));
            }
            else
            {
                result = addSimpleAccountTransaction(client, arguments, typeName);
            }

            afterMutation(openFile, client);
            return result;
        });
    }

    private JsonObject addAccountTransfer(Client client, JsonObject arguments) throws MCPException
    {
        Account fromAccount = resolveAccount(client, requireString(arguments, "fromAccount"));
        Account toAccount = resolveAccount(client, requireString(arguments, "toAccount"));
        if (fromAccount.equals(toAccount))
            throw new MCPException("fromAccount and toAccount must be different");

        double sourceAmountValue = requireDouble(arguments, "amount");
        if (sourceAmountValue <= 0)
            throw new MCPException("amount must be greater than zero");

        AccountTransferEntry entry = new AccountTransferEntry(fromAccount, toAccount);
        entry.setDate(parseDateTime(arguments));
        if (arguments.has("note"))
            entry.setNote(getString(arguments, "note"));

        AccountTransaction sourceTx = entry.getSourceTransaction();
        AccountTransaction targetTx = entry.getTargetTransaction();
        sourceTx.setCurrencyCode(fromAccount.getCurrencyCode());
        targetTx.setCurrencyCode(toAccount.getCurrencyCode());

        long sourceAmount = Values.Amount.factorize(sourceAmountValue);

        if (fromAccount.getCurrencyCode().equals(toAccount.getCurrencyCode()))
        {
            sourceTx.setAmount(sourceAmount);
            targetTx.setAmount(sourceAmount);
        }
        else
        {
            if (!arguments.has("targetAmount"))
                throw new MCPException("targetAmount is required for cross-currency transfers");

            double targetAmountValue = requireDouble(arguments, "targetAmount");
            if (targetAmountValue <= 0)
                throw new MCPException("targetAmount must be greater than zero");

            long targetAmount = Values.Amount.factorize(targetAmountValue);

            sourceTx.clearUnits();
            sourceTx.setAmount(sourceAmount);
            targetTx.setAmount(targetAmount);

            BigDecimal rate = BigDecimal.valueOf(sourceAmountValue).divide(BigDecimal.valueOf(targetAmountValue), 10,
                            RoundingMode.HALF_DOWN);
            sourceTx.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(fromAccount.getCurrencyCode(), sourceAmount),
                            Money.of(toAccount.getCurrencyCode(), targetAmount), rate));
        }

        entry.insert();

        JsonObject result = new JsonObject();
        result.addProperty("fromTransactionId", sourceTx.getUUID());
        result.addProperty("toTransactionId", targetTx.getUUID());
        return result;
    }

    private JsonObject addSecurityTransfer(Client client, JsonObject arguments) throws MCPException
    {
        Portfolio fromPortfolio = resolvePortfolio(client, requireString(arguments, "fromPortfolio"));
        Portfolio toPortfolio = resolvePortfolio(client, requireString(arguments, "toPortfolio"));
        if (fromPortfolio.equals(toPortfolio))
            throw new MCPException("fromPortfolio and toPortfolio must be different");

        Security security = resolveSecurity(client, requireString(arguments, "security"));

        PortfolioTransferEntry entry = new PortfolioTransferEntry(fromPortfolio, toPortfolio);
        entry.setDate(parseDateTime(arguments));
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(requireDouble(arguments, "shares")));
        entry.setAmount(Values.Amount.factorize(requireDouble(arguments, "amount")));
        entry.setCurrencyCode(security.getCurrencyCode());
        if (arguments.has("note"))
            entry.setNote(getString(arguments, "note"));

        entry.insert();

        JsonObject result = new JsonObject();
        result.addProperty("fromTransactionId", entry.getSourceTransaction().getUUID());
        result.addProperty("toTransactionId", entry.getTargetTransaction().getUUID());
        return result;
    }

    private JsonObject addDelivery(Client client, JsonObject arguments, PortfolioTransaction.Type type)
                    throws MCPException
    {
        Portfolio portfolio = resolvePortfolio(client, requireString(arguments, "portfolio"));
        Security security = resolveSecurity(client, requireString(arguments, "security"));

        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setType(type);
        transaction.setDateTime(parseDateTime(arguments));
        transaction.setCurrencyCode(security.getCurrencyCode());
        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(requireDouble(arguments, "shares")));
        transaction.setAmount(Values.Amount.factorize(requireDouble(arguments, "amount")));
        if (arguments.has("note"))
            transaction.setNote(getString(arguments, "note"));

        applyFeeAndTax(transaction, arguments, security.getCurrencyCode());
        portfolio.addTransaction(transaction);

        JsonObject result = new JsonObject();
        result.addProperty("id", transaction.getUUID());
        return result;
    }

    private JsonObject addBuySell(Client client, JsonObject arguments, PortfolioTransaction.Type type)
                    throws MCPException
    {
        Portfolio portfolio = resolvePortfolio(client, requireString(arguments, "portfolio"));
        Account account = resolveAccount(client, requireString(arguments, "account"));
        Security security = resolveSecurity(client, requireString(arguments, "security"));

        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(type);
        entry.setDate(parseDateTime(arguments));
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(requireDouble(arguments, "shares")));
        entry.setCurrencyCode(account.getCurrencyCode());
        entry.setAmount(Values.Amount.factorize(requireDouble(arguments, "amount")));
        if (arguments.has("note"))
            entry.setNote(getString(arguments, "note"));

        applyFeeAndTax(entry.getPortfolioTransaction(), arguments, account.getCurrencyCode());

        entry.insert();

        JsonObject result = new JsonObject();
        result.addProperty("portfolioTransactionId", entry.getPortfolioTransaction().getUUID());
        result.addProperty("accountTransactionId", entry.getAccountTransaction().getUUID());
        return result;
    }

    private JsonObject addSimpleAccountTransaction(Client client, JsonObject arguments, String typeName)
                    throws MCPException
    {
        AccountTransaction.Type type = AccountTransaction.Type.valueOf(typeName);
        if (!SIMPLE_ACCOUNT_TYPES.contains(type))
            throw new MCPException("Unsupported account transaction type: " + typeName);

        Account account = resolveAccount(client, requireString(arguments, "account"));

        AccountTransaction transaction = new AccountTransaction();
        transaction.setType(type);
        transaction.setDateTime(parseDateTime(arguments));
        transaction.setCurrencyCode(account.getCurrencyCode());
        transaction.setAmount(Values.Amount.factorize(requireDouble(arguments, "amount")));

        if (arguments.has("security"))
            transaction.setSecurity(resolveSecurity(client, requireString(arguments, "security")));
        else if (type == AccountTransaction.Type.DIVIDENDS)
            throw new MCPException("Security is required for DIVIDENDS transactions");

        if (arguments.has("note"))
            transaction.setNote(getString(arguments, "note"));

        applyFeeAndTax(transaction, arguments, account.getCurrencyCode());

        account.addTransaction(transaction);

        JsonObject result = new JsonObject();
        result.addProperty("id", transaction.getUUID());
        return result;
    }

    private JsonObject callDeleteTransaction(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            Client client = openFile.client();
            String id = requireString(arguments, "id");

            TransactionPair<?> pair = findTransactionPair(client, id)
                            .orElseThrow(() -> new MCPException("Transaction not found: " + id));

            String crossEntryId = null;
            if (pair.getTransaction().getCrossEntry() != null)
            {
                Transaction other = pair.getTransaction().getCrossEntry()
                                .getCrossTransaction(pair.getTransaction());
                crossEntryId = other.getUUID();
            }

            pair.deleteTransaction(client);
            afterMutation(openFile, client);

            JsonObject result = new JsonObject();
            result.addProperty("id", id);
            if (crossEntryId != null)
                result.addProperty("crossEntryId", crossEntryId);
            return result;
        });
    }

    private JsonObject callUpdateTransaction(JsonObject arguments) throws Exception
    {
        return modelExecutor.execute(() -> {
            OpenFile openFile = resolveFile(arguments);
            Client client = openFile.client();
            String id = requireString(arguments, "id");

            Transaction transaction = findTransactionPair(client, id).map(TransactionPair::getTransaction)
                            .orElseThrow(() -> new MCPException("Transaction not found: " + id));

            if (arguments.has("type"))
            {
                String typeName = requireString(arguments, "type");
                CrossEntry crossEntry = transaction.getCrossEntry();
                if (crossEntry instanceof AccountTransferEntry || crossEntry instanceof PortfolioTransferEntry)
                    throw new MCPException("Changing the type of a transfer transaction is not supported");

                if (transaction instanceof AccountTransaction accountTransaction)
                    accountTransaction.setType(AccountTransaction.Type.valueOf(typeName));
                else if (transaction instanceof PortfolioTransaction portfolioTransaction)
                {
                    PortfolioTransaction.Type type = PortfolioTransaction.Type.valueOf(typeName);
                    portfolioTransaction.setType(type);
                    syncBuySellType(portfolioTransaction, type);
                }
            }

            if (arguments.has("date"))
            {
                transaction.setDateTime(parseDateTime(arguments));
                syncTransferFrom(transaction);
            }

            if (arguments.has("amount") || arguments.has("targetAmount"))
            {
                if (transaction.getCrossEntry() instanceof AccountTransferEntry entry)
                    syncAccountTransferAmounts(entry, arguments);
                else if (transaction.getCrossEntry() instanceof PortfolioTransferEntry entry)
                {
                    if (arguments.has("amount"))
                        entry.setAmount(Values.Amount.factorize(requireDouble(arguments, "amount")));
                }
                else if (arguments.has("amount"))
                {
                    long amount = Values.Amount.factorize(requireDouble(arguments, "amount"));
                    transaction.setAmount(amount);
                    syncBuySellAmount(transaction, amount);
                }
            }

            if (arguments.has("shares") && transaction instanceof PortfolioTransaction portfolioTransaction)
            {
                portfolioTransaction.setShares(Values.Share.factorize(requireDouble(arguments, "shares")));
                syncTransferFrom(transaction);
            }

            if (arguments.has("security"))
            {
                Security security = resolveSecurity(client, requireString(arguments, "security"));
                transaction.setSecurity(security);
                syncBuySellSecurity(transaction, security);
                syncTransferFrom(transaction);
            }

            if (arguments.has("note"))
            {
                transaction.setNote(getString(arguments, "note"));
                syncBuySellNote(transaction, getString(arguments, "note"));
                syncTransferFrom(transaction);
            }

            if (arguments.has("fee") || arguments.has("tax"))
            {
                String currency = transaction.getCurrencyCode();
                Optional<Unit> grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE);
                transaction.clearUnits();
                grossValue.ifPresent(transaction::addUnit);
                applyFeeAndTax(transaction, arguments, currency);
                syncBuySellUnits(transaction);
            }

            afterMutation(openFile, client);

            JsonObject result = new JsonObject();
            result.addProperty("id", transaction.getUUID());
            return result;
        });
    }

    private void afterMutation(OpenFile openFile, Client client)
    {
        client.markDirty();
        if (autoSave)
            openFile.save();
    }

    private OpenFile resolveFile(JsonObject arguments) throws MCPException
    {
        String fileId = requireString(arguments, "file");
        return clientSource.listOpenFiles().stream().filter(f -> matchesFile(f, fileId)).findFirst()
                        .orElseThrow(() -> new MCPException("Unknown or closed file: " + fileId));
    }

    private boolean matchesFile(OpenFile openFile, String fileId)
    {
        if (Objects.equals(openFile.id(), fileId))
            return true;
        if (Objects.equals(openFile.label(), fileId))
            return true;
        if (openFile.file() != null && Objects.equals(openFile.file().getAbsolutePath(), fileId))
            return true;
        return openFile.file() != null && Objects.equals(openFile.file().getName(), fileId);
    }

    private Account resolveAccount(Client client, String id) throws MCPException
    {
        return client.getAccounts().stream().filter(a -> matchesEntity(a.getUUID(), a.getName(), id)).findFirst()
                        .orElseThrow(() -> new MCPException("Account not found: " + id));
    }

    private Portfolio resolvePortfolio(Client client, String id) throws MCPException
    {
        return client.getPortfolios().stream().filter(p -> matchesEntity(p.getUUID(), p.getName(), id)).findFirst()
                        .orElseThrow(() -> new MCPException("Portfolio not found: " + id));
    }

    private Security resolveSecurity(Client client, String id) throws MCPException
    {
        return client.getSecurities().stream().filter(s -> matchesEntity(s.getUUID(), s.getName(), id)).findFirst()
                        .orElseThrow(() -> new MCPException("Security not found: " + id));
    }

    private boolean matchesEntity(String uuid, String name, String id)
    {
        return Objects.equals(uuid, id) || Objects.equals(name, id);
    }

    private boolean matchesAccountFilter(Account account, String filter)
    {
        if (filter == null)
            return true;
        return matchesEntity(account.getUUID(), account.getName(), filter);
    }

    private Optional<TransactionPair<?>> findTransactionPair(Client client, String id)
    {
        for (Account account : client.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                if (transaction.getUUID().equals(id))
                    return Optional.of(new TransactionPair<>(account, transaction));
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction transaction : portfolio.getTransactions())
            {
                if (transaction.getUUID().equals(id))
                    return Optional.of(new TransactionPair<>(portfolio, transaction));
            }
        }

        return Optional.empty();
    }

    private Account findLinkedAccount(PortfolioTransaction transaction)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
            return entry.getAccount();
        return null;
    }

    private JsonObject toTransactionJson(String ownerType, String ownerId, String ownerName, Transaction transaction)
    {
        JsonObject item = new JsonObject();
        item.addProperty("id", transaction.getUUID());
        item.addProperty("ownerType", ownerType);
        item.addProperty("ownerId", ownerId);
        item.addProperty("ownerName", ownerName);
        item.addProperty("type", transactionTypeName(transaction));
        item.addProperty("date", transaction.getDateTime().toString());
        item.addProperty("amount", (double) transaction.getAmount() / Values.Amount.factor());
        item.addProperty("currency", transaction.getCurrencyCode());
        if (transaction.getSecurity() != null)
            item.addProperty("security", transaction.getSecurity().getUUID());
        if (transaction.getShares() != 0)
            item.addProperty("shares", (double) transaction.getShares() / Values.Share.factor());
        if (transaction.getNote() != null)
            item.addProperty("note", transaction.getNote());

        CrossEntry crossEntry = transaction.getCrossEntry();
        if (crossEntry != null)
        {
            TransactionOwner<? extends Transaction> crossOwner = crossEntry.getCrossOwner(transaction);
            JsonObject counterparty = new JsonObject();
            if (crossOwner instanceof Account account)
            {
                counterparty.addProperty("type", "account");
                counterparty.addProperty("uuid", account.getUUID());
                counterparty.addProperty("name", account.getName());
            }
            else if (crossOwner instanceof Portfolio portfolio)
            {
                counterparty.addProperty("type", "portfolio");
                counterparty.addProperty("uuid", portfolio.getUUID());
                counterparty.addProperty("name", portfolio.getName());
            }
            item.add("counterparty", counterparty);

            if (isTransferType(transaction))
                item.addProperty("direction", isInboundTransfer(transaction) ? "IN" : "OUT");
        }

        return item;
    }

    private boolean isTransferType(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
        {
            AccountTransaction.Type type = accountTransaction.getType();
            return type == AccountTransaction.Type.TRANSFER_IN || type == AccountTransaction.Type.TRANSFER_OUT;
        }
        if (transaction instanceof PortfolioTransaction portfolioTransaction)
        {
            PortfolioTransaction.Type type = portfolioTransaction.getType();
            return type == PortfolioTransaction.Type.TRANSFER_IN
                            || type == PortfolioTransaction.Type.TRANSFER_OUT;
        }
        return false;
    }

    private boolean isInboundTransfer(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getType() == AccountTransaction.Type.TRANSFER_IN;
        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return portfolioTransaction.getType() == PortfolioTransaction.Type.TRANSFER_IN;
        return false;
    }

    private String transactionTypeName(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getType().name();
        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return portfolioTransaction.getType().name();
        return "UNKNOWN";
    }

    private boolean isSecurityType(String typeName)
    {
        try
        {
            return SECURITY_TYPES.contains(PortfolioTransaction.Type.valueOf(typeName));
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    private LocalDateTime parseDateTime(JsonObject arguments) throws MCPException
    {
        String dateValue = requireString(arguments, "date");
        try
        {
            if (dateValue.contains("T"))
                return LocalDateTime.parse(dateValue);
            return LocalDate.parse(dateValue).atTime(LocalTime.MIDNIGHT);
        }
        catch (DateTimeParseException e)
        {
            throw new MCPException("Invalid date: " + dateValue);
        }
    }

    private void applyFeeAndTax(Transaction transaction, JsonObject arguments, String currency) throws MCPException
    {
        if (arguments.has("fee"))
        {
            long fee = Values.Amount.factorize(requireDouble(arguments, "fee"));
            if (fee > 0)
                transaction.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, fee)));
        }

        if (arguments.has("tax"))
        {
            long tax = Values.Amount.factorize(requireDouble(arguments, "tax"));
            if (tax > 0)
                transaction.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, tax)));
        }
    }

    private void syncTransferFrom(Transaction transaction)
    {
        CrossEntry crossEntry = transaction.getCrossEntry();
        if (crossEntry instanceof AccountTransferEntry || crossEntry instanceof PortfolioTransferEntry)
            crossEntry.updateFrom(transaction);
    }

    private void syncAccountTransferAmounts(AccountTransferEntry entry, JsonObject arguments) throws MCPException
    {
        Account fromAccount = entry.getSourceAccount();
        Account toAccount = entry.getTargetAccount();
        AccountTransaction sourceTx = entry.getSourceTransaction();
        AccountTransaction targetTx = entry.getTargetTransaction();

        if (fromAccount.getCurrencyCode().equals(toAccount.getCurrencyCode()))
        {
            if (!arguments.has("amount") && !arguments.has("targetAmount"))
                return;

            long amount = arguments.has("amount") ? Values.Amount.factorize(requireDouble(arguments, "amount"))
                            : Values.Amount.factorize(requireDouble(arguments, "targetAmount"));
            sourceTx.setAmount(amount);
            targetTx.setAmount(amount);
            return;
        }

        double sourceAmountValue = arguments.has("amount") ? requireDouble(arguments, "amount")
                        : (double) sourceTx.getAmount() / Values.Amount.factor();
        double targetAmountValue = arguments.has("targetAmount") ? requireDouble(arguments, "targetAmount")
                        : (double) targetTx.getAmount() / Values.Amount.factor();

        if (sourceAmountValue <= 0)
            throw new MCPException("amount must be greater than zero");
        if (targetAmountValue <= 0)
            throw new MCPException("targetAmount must be greater than zero");

        long sourceAmount = Values.Amount.factorize(sourceAmountValue);
        long targetAmount = Values.Amount.factorize(targetAmountValue);

        sourceTx.clearUnits();
        sourceTx.setAmount(sourceAmount);
        targetTx.setAmount(targetAmount);

        BigDecimal rate = BigDecimal.valueOf(sourceAmountValue).divide(BigDecimal.valueOf(targetAmountValue), 10,
                        RoundingMode.HALF_DOWN);
        sourceTx.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(fromAccount.getCurrencyCode(), sourceAmount),
                        Money.of(toAccount.getCurrencyCode(), targetAmount), rate));
    }

    private void syncBuySellType(PortfolioTransaction transaction, PortfolioTransaction.Type type)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
            entry.setType(type);
    }

    private void syncBuySellAmount(Transaction transaction, long amount)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
            entry.setAmount(amount);
    }

    private void syncBuySellSecurity(Transaction transaction, Security security)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
            entry.setSecurity(security);
    }

    private void syncBuySellNote(Transaction transaction, String note)
    {
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
            entry.setNote(note);
    }

    private void syncBuySellUnits(Transaction transaction)
    {
        if (!(transaction.getCrossEntry() instanceof BuySellEntry entry))
            return;

        Transaction other = transaction instanceof PortfolioTransaction ? entry.getAccountTransaction()
                        : entry.getPortfolioTransaction();
        other.clearUnits();
        transaction.getUnits().forEach(other::addUnit);
    }

    private JsonObject toolDefinition(String name, String description, JsonObject inputSchema)
    {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("description", description);
        tool.add("inputSchema", inputSchema);
        return tool;
    }

    private JsonObject schemaObject(JsonObject... properties)
    {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonArray required = new JsonArray();
        for (JsonObject property : properties)
        {
            String key = property.get("key").getAsString();
            props.add(key, property.get("schema"));
            if (property.get("required").getAsBoolean())
                required.add(key);
        }
        schema.add("properties", props);
        if (!required.isEmpty())
            schema.add("required", required);
        return schema;
    }

    private JsonObject required(String name, String type, String description)
    {
        return property(name, type, description, true);
    }

    private JsonObject optional(String name, String type, String description)
    {
        return property(name, type, description, false);
    }

    private JsonObject property(String name, String type, String description, boolean required)
    {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("key", name);
        wrapper.addProperty("required", required);

        JsonObject schema = new JsonObject();
        schema.addProperty("type", type);
        schema.addProperty("description", description);
        wrapper.add("schema", schema);
        return wrapper;
    }

    private String requireString(JsonObject arguments, String key) throws MCPException
    {
        if (!arguments.has(key) || arguments.get(key).isJsonNull())
            throw new MCPException("Missing required parameter: " + key);
        return arguments.get(key).getAsString();
    }

    private String getString(JsonObject arguments, String key)
    {
        JsonElement element = arguments.get(key);
        if (element == null || element.isJsonNull())
            return null;
        return element.getAsString();
    }

    private double requireDouble(JsonObject arguments, String key) throws MCPException
    {
        if (!arguments.has(key) || arguments.get(key).isJsonNull())
            throw new MCPException("Missing required parameter: " + key);
        return arguments.get(key).getAsDouble();
    }

    private String toJson(JsonElement element)
    {
        return element.toString();
    }
}

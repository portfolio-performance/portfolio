package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerDiagnosticMessageFormatter;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.Money;

public final class LedgerXmlPersistenceSupport
{
    private LedgerXmlPersistenceSupport()
    {
    }

    @SuppressWarnings("nls")
    public static void configureXStream(XStream xstream)
    {
        xstream.allowTypes(new Class[] { Money.class });

        xstream.registerConverter(new LedgerEntryConverter());
        xstream.registerConverter(new LedgerPostingConverter());
        xstream.registerConverter(new LedgerParameterConverter());

        xstream.alias("ledger", Ledger.class);
        xstream.alias("ledger-entry", LedgerEntry.class);
        xstream.useAttributeFor(LedgerEntry.class, "uuid");
        xstream.useAttributeFor(LedgerEntry.class, "type");
        xstream.useAttributeFor(LedgerEntry.class, "dateTime");
        xstream.useAttributeFor(LedgerEntry.class, "updatedAt");
        xstream.alias("ledger-posting", LedgerPosting.class);
        xstream.useAttributeFor(LedgerPosting.class, "uuid");
        xstream.useAttributeFor(LedgerPosting.class, "type");
        xstream.useAttributeFor(LedgerPosting.class, "amount");
        xstream.useAttributeFor(LedgerPosting.class, "currency");
        xstream.useAttributeFor(LedgerPosting.class, "forexAmount");
        xstream.useAttributeFor(LedgerPosting.class, "forexCurrency");
        xstream.useAttributeFor(LedgerPosting.class, "exchangeRate");
        xstream.useAttributeFor(LedgerPosting.class, "shares");
        xstream.alias("ledger-posting-parameter", LedgerParameter.class);
        xstream.alias("ledger-posting-parameter-type", LedgerParameterType.class);
        xstream.alias("ledger-parameter", LedgerParameter.class);
        xstream.alias("ledger-parameter-type", LedgerParameterType.class);
        xstream.alias("ledger-entry-type", LedgerEntryType.class);
        xstream.alias("ledger-posting-type", LedgerPostingType.class);
    }

    public static void initializeAfterLoad(Client client) throws IOException
    {
        keepCorporateActionsOnly(client);
        LedgerProjectionService.restoreIfValid(client);
    }

    public static void save(Client client, XStream xstream, Writer writer) throws IOException
    {
        var saveState = new LedgerXmlSaveState();

        try
        {
            prepareLedgerXmlSave(client, saveState);
            xstream.toXML(client, writer);
            writer.flush();
        }
        finally
        {
            saveState.restore();
        }
    }

    private static Optional<String> readAttribute(HierarchicalStreamReader reader, String name)
    {
        return Optional.ofNullable(reader.getAttribute(name));
    }

    private static void writeAttribute(HierarchicalStreamWriter writer, String name, Object value)
    {
        if (value != null)
            writer.addAttribute(name, String.valueOf(value));
    }

    private static void writeValue(HierarchicalStreamWriter writer, String nodeName, String value)
    {
        if (value == null)
            return;

        writer.startNode(nodeName);
        writer.setValue(value);
        writer.endNode();
    }

    private static void writeObject(HierarchicalStreamWriter writer, MarshallingContext context, String nodeName,
                    Object value)
    {
        if (value == null)
            return;

        writer.startNode(nodeName);
        context.convertAnother(value);
        writer.endNode();
    }

    private static void writeCollection(HierarchicalStreamWriter writer, MarshallingContext context,
                    String collectionNodeName, String itemNodeName, List<?> values)
    {
        writer.startNode(collectionNodeName);

        for (var value : values)
            writeObject(writer, context, itemNodeName, value);

        writer.endNode();
    }

    private static void writeParameters(HierarchicalStreamWriter writer, MarshallingContext context,
                    List<LedgerParameter<?>> parameters)
    {
        if (!parameters.isEmpty())
            writeCollection(writer, context, "parameters", "ledger-parameter", parameters); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void prepareLedgerXmlSave(Client client, LedgerXmlSaveState saveState) throws IOException
    {
        keepCorporateActionsOnly(client);
        validateLedger(client);

        for (var account : client.getAccounts())
        {
            saveState.removeLedgerBackedTransactions(account.getTransactions());
        }

        for (var portfolio : client.getPortfolios())
        {
            saveState.removeLedgerBackedTransactions(portfolio.getTransactions());
        }
    }

    private static LedgerStructuralValidator.ValidationResult validateLedger(Client client) throws IOException
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());

        if (!result.isOK())
        {
            LedgerProjectionService.logSkipped(client.getLedger(), result);
            throw new IOException(LedgerDiagnosticCode.LEDGER_PERSIST_001
                            .message(MessageFormat.format(Messages.LedgerXmlInvalidLedgerStructure,
                                            LedgerDiagnosticMessageFormatter.formatValidationResult(
                                                            client.getLedger(), result))));
        }

        return result;
    }

    private static void keepCorporateActionsOnly(Client client)
    {
        List.copyOf(client.getLedger().getEntries()).stream() //
                        .filter(entry -> entry.getType() != LedgerEntryType.CORPORATE_ACTION) //
                        .forEach(client.getLedger()::removeEntry);
    }

    private static class LedgerEntryConverter implements Converter
    {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
        {
            return type == LedgerEntry.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
        {
            var entry = (LedgerEntry) source;

            writeAttribute(writer, "type", entry.getType()); //$NON-NLS-1$
            writeAttribute(writer, "dateTime", entry.getDateTime()); //$NON-NLS-1$
            writeAttribute(writer, "updatedAt", entry.getUpdatedAt()); //$NON-NLS-1$
            writeValue(writer, "note", entry.getNote()); //$NON-NLS-1$
            writeValue(writer, "source", entry.getSource()); //$NON-NLS-1$
            writeParameters(writer, context, entry.getParameters());
            writeCollection(writer, context, "postings", "ledger-posting", entry.getPostings()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
        {
            var entry = new LedgerEntry();
            var updatedAt = reader.getAttribute("updatedAt"); //$NON-NLS-1$
            var legacySpinOffTypeCode = false;
            var typeAttribute = reader.getAttribute("type"); //$NON-NLS-1$

            readAttribute(reader, "uuid").ifPresent(entry::setUUID); //$NON-NLS-1$
            if (typeAttribute != null)
            {
                entry.setType(LedgerModelLoadSupport.entryTypeFromPersistedCode(typeAttribute));
                legacySpinOffTypeCode = LedgerModelLoadSupport.isLegacySpinOffTypeCode(typeAttribute);
            }
            readAttribute(reader, "dateTime").map(LocalDateTime::parse).ifPresent(entry::setDateTime); //$NON-NLS-1$

            while (reader.hasMoreChildren())
            {
                reader.moveDown();

                switch (reader.getNodeName())
                {
                    case "uuid" -> entry.setUUID(reader.getValue()); //$NON-NLS-1$
                    case "type" -> { //$NON-NLS-1$
                        var typeCode = reader.getValue();
                        entry.setType(LedgerModelLoadSupport.entryTypeFromPersistedCode(typeCode));
                        legacySpinOffTypeCode |= LedgerModelLoadSupport.isLegacySpinOffTypeCode(typeCode);
                    }
                    case "dateTime" -> entry.setDateTime((LocalDateTime) context.convertAnother(entry, //$NON-NLS-1$
                                    LocalDateTime.class));
                    case "updatedAt" -> updatedAt = reader.getValue(); //$NON-NLS-1$
                    case "note" -> entry.setNote(reader.getValue()); //$NON-NLS-1$
                    case "source" -> entry.setSource(reader.getValue()); //$NON-NLS-1$
                    case "parameters" -> readParameters(reader, context, entry); //$NON-NLS-1$
                    case "postings" -> readPostings(reader, context, entry); //$NON-NLS-1$
                    case "projectionRefs" -> skipChildren(reader); //$NON-NLS-1$
                    default -> {
                        // Ignore unknown LedgerEntry fields to preserve load recovery behavior.
                    }
                }

                reader.moveUp();
            }

            if (updatedAt != null)
                entry.setUpdatedAt(Instant.parse(updatedAt));

            if (legacySpinOffTypeCode)
                LedgerModelLoadSupport.addLegacySpinOffKindIfMissing(entry);

            return entry;
        }

        private void readParameters(HierarchicalStreamReader reader, UnmarshallingContext context, LedgerEntry entry)
        {
            while (reader.hasMoreChildren())
            {
                reader.moveDown();
                entry.addParameter((LedgerParameter<?>) context.convertAnother(entry, LedgerParameter.class));
                reader.moveUp();
            }
        }

        private void readPostings(HierarchicalStreamReader reader, UnmarshallingContext context, LedgerEntry entry)
        {
            while (reader.hasMoreChildren())
            {
                reader.moveDown();
                entry.addPosting((LedgerPosting) context.convertAnother(entry, LedgerPosting.class));
                reader.moveUp();
            }
        }

        private void skipChildren(HierarchicalStreamReader reader)
        {
            while (reader.hasMoreChildren())
            {
                reader.moveDown();
                skipChildren(reader);
                reader.moveUp();
            }
        }
    }

    private static class LedgerPostingConverter implements Converter
    {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
        {
            return type == LedgerPosting.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
        {
            var posting = (LedgerPosting) source;

            writeAttribute(writer, "type", posting.getType()); //$NON-NLS-1$
            writeAttribute(writer, "amount", posting.getAmount()); //$NON-NLS-1$
            writeAttribute(writer, "currency", posting.getCurrency()); //$NON-NLS-1$
            writeAttribute(writer, "forexAmount", posting.getForexAmount()); //$NON-NLS-1$
            writeAttribute(writer, "forexCurrency", posting.getForexCurrency()); //$NON-NLS-1$
            writeAttribute(writer, "exchangeRate", posting.getExchangeRate()); //$NON-NLS-1$
            writeAttribute(writer, "shares", posting.getShares()); //$NON-NLS-1$
            writeAttribute(writer, "semanticRole", posting.getSemanticRole()); //$NON-NLS-1$
            writeAttribute(writer, "direction", posting.getDirection()); //$NON-NLS-1$
            writeAttribute(writer, "corporateActionLeg", posting.getCorporateActionLeg()); //$NON-NLS-1$
            writeAttribute(writer, "unitRole", posting.getUnitRole()); //$NON-NLS-1$
            writeAttribute(writer, "groupKey", posting.getGroupKey()); //$NON-NLS-1$
            writeAttribute(writer, "localKey", posting.getLocalKey()); //$NON-NLS-1$
            writeObject(writer, context, "security", posting.getSecurity()); //$NON-NLS-1$
            writeObject(writer, context, "account", posting.getAccount()); //$NON-NLS-1$
            writeObject(writer, context, "portfolio", posting.getPortfolio()); //$NON-NLS-1$
            writeParameters(writer, context, posting.getParameters());
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
        {
            var posting = new LedgerPosting();

            readAttribute(reader, "uuid").ifPresent(posting::setUUID); //$NON-NLS-1$
            readAttribute(reader, "type").map(LedgerPostingType::valueOf).ifPresent(posting::setType); //$NON-NLS-1$
            readAttribute(reader, "amount").map(Long::parseLong).ifPresent(posting::setAmount); //$NON-NLS-1$
            readAttribute(reader, "currency").ifPresent(posting::setCurrency); //$NON-NLS-1$
            readAttribute(reader, "forexAmount").map(Long::valueOf).ifPresent(posting::setForexAmount); //$NON-NLS-1$
            readAttribute(reader, "forexCurrency").ifPresent(posting::setForexCurrency); //$NON-NLS-1$
            readAttribute(reader, "exchangeRate").map(BigDecimal::new).ifPresent(posting::setExchangeRate); //$NON-NLS-1$
            readAttribute(reader, "shares").map(Long::parseLong).ifPresent(posting::setShares); //$NON-NLS-1$
            readAttribute(reader, "semanticRole").map(LedgerPostingSemanticRole::valueOf) //$NON-NLS-1$
                            .ifPresent(posting::setSemanticRole);
            readAttribute(reader, "direction").map(LedgerPostingDirection::valueOf) //$NON-NLS-1$
                            .ifPresent(posting::setDirection);
            readAttribute(reader, "corporateActionLeg").map(CorporateActionLeg::valueOf) //$NON-NLS-1$
                            .ifPresent(posting::setCorporateActionLeg);
            readAttribute(reader, "unitRole").map(LedgerPostingUnitRole::valueOf) //$NON-NLS-1$
                            .ifPresent(posting::setUnitRole);
            readAttribute(reader, "groupKey").ifPresent(posting::setGroupKey); //$NON-NLS-1$
            readAttribute(reader, "localKey").ifPresent(posting::setLocalKey); //$NON-NLS-1$

            while (reader.hasMoreChildren())
            {
                reader.moveDown();

                switch (reader.getNodeName())
                {
                    case "uuid" -> posting.setUUID(reader.getValue()); //$NON-NLS-1$
                    case "type" -> posting.setType((LedgerPostingType) context.convertAnother(posting, //$NON-NLS-1$
                                    LedgerPostingType.class));
                    case "amount" -> posting.setAmount(Long.parseLong(reader.getValue())); //$NON-NLS-1$
                    case "currency" -> posting.setCurrency(reader.getValue()); //$NON-NLS-1$
                    case "forexAmount" -> posting.setForexAmount(Long.valueOf(reader.getValue())); //$NON-NLS-1$
                    case "forexCurrency" -> posting.setForexCurrency(reader.getValue()); //$NON-NLS-1$
                    case "exchangeRate" -> posting.setExchangeRate(new BigDecimal(reader.getValue())); //$NON-NLS-1$
                    case "security" -> posting.setSecurity((Security) context.convertAnother(posting, //$NON-NLS-1$
                                    Security.class));
                    case "shares" -> posting.setShares(Long.parseLong(reader.getValue())); //$NON-NLS-1$
                    case "account" -> posting.setAccount((Account) context.convertAnother(posting, Account.class)); //$NON-NLS-1$
                    case "portfolio" -> posting.setPortfolio((Portfolio) context.convertAnother(posting, //$NON-NLS-1$
                                    Portfolio.class));
                    case "semanticRole" -> posting.setSemanticRole( //$NON-NLS-1$
                                    (LedgerPostingSemanticRole) context.convertAnother(posting,
                                                    LedgerPostingSemanticRole.class));
                    case "direction" -> posting.setDirection( //$NON-NLS-1$
                                    (LedgerPostingDirection) context.convertAnother(posting,
                                                    LedgerPostingDirection.class));
                    case "corporateActionLeg" -> posting.setCorporateActionLeg( //$NON-NLS-1$
                                    (CorporateActionLeg) context.convertAnother(posting, CorporateActionLeg.class));
                    case "unitRole" -> posting.setUnitRole( //$NON-NLS-1$
                                    (LedgerPostingUnitRole) context.convertAnother(posting,
                                                    LedgerPostingUnitRole.class));
                    case "groupKey" -> posting.setGroupKey(reader.getValue()); //$NON-NLS-1$
                    case "localKey" -> posting.setLocalKey(reader.getValue()); //$NON-NLS-1$
                    case "parameters" -> readParameters(reader, context, posting); //$NON-NLS-1$
                    default -> {
                        // Ignore unknown LedgerPosting fields to preserve load recovery behavior.
                    }
                }

                reader.moveUp();
            }

            return posting;
        }

        private void readParameters(HierarchicalStreamReader reader, UnmarshallingContext context,
                        LedgerPosting posting)
        {
            while (reader.hasMoreChildren())
            {
                reader.moveDown();
                posting.addParameter((LedgerParameter<?>) context.convertAnother(posting, LedgerParameter.class));
                reader.moveUp();
            }
        }
    }

    private static class LedgerParameterConverter implements Converter
    {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
        {
            return type == LedgerParameter.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
        {
            var parameter = (LedgerParameter<?>) source;

            writer.addAttribute("type", parameter.getType().getCode()); //$NON-NLS-1$
            writer.addAttribute("valueKind", parameter.getValueKind().name()); //$NON-NLS-1$

            switch (parameter.getValueKind())
            {
                case STRING, DECIMAL, LONG, BOOLEAN, LOCAL_DATE, LOCAL_DATE_TIME:
                    writer.addAttribute("value", String.valueOf(parameter.getValue())); //$NON-NLS-1$
                    break;
                case MONEY:
                    var money = (Money) parameter.getValue();
                    writer.addAttribute("amount", String.valueOf(money.getAmount())); //$NON-NLS-1$
                    writer.addAttribute("currency", money.getCurrencyCode()); //$NON-NLS-1$
                    break;
                case SECURITY, ACCOUNT, PORTFOLIO:
                    writer.startNode("value"); //$NON-NLS-1$
                    context.convertAnother(parameter.getValue());
                    writer.endNode();
                    break;
                default:
                    throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PERSIST_003
                                    .message(MessageFormat.format(Messages.LedgerParameterUnsupportedValueKind,
                                                    parameter.getValueKind())));
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
        {
            LedgerParameterType type = typeOrNull(reader.getAttribute("type")); //$NON-NLS-1$
            ValueKind valueKind = valueKindOrNull(reader.getAttribute("valueKind")); //$NON-NLS-1$
            var scalarValue = reader.getAttribute("value"); //$NON-NLS-1$
            var amount = reader.getAttribute("amount"); //$NON-NLS-1$
            var currency = reader.getAttribute("currency"); //$NON-NLS-1$
            Object parameterValue = null;

            while (reader.hasMoreChildren())
            {
                reader.moveDown();

                switch (reader.getNodeName())
                {
                    case "type" -> type = typeOrNull(reader.getValue()); //$NON-NLS-1$
                    case "valueKind" -> valueKind = valueKindOrNull(reader.getValue()); //$NON-NLS-1$
                    case "value" -> parameterValue = readValue(reader, context, valueKind); //$NON-NLS-1$
                    default -> {
                        // Ignore unknown LedgerParameter fields to keep XML load recovery tolerant.
                    }
                }

                reader.moveUp();
            }

            if (type == null)
                throw new IllegalArgumentException(
                                LedgerDiagnosticCode.LEDGER_PERSIST_004.message(Messages.LedgerParameterMissingType));

            if (valueKind == null)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PERSIST_005
                                .message(Messages.LedgerParameterMissingValueKind));

            if (parameterValue == null)
                parameterValue = readValue(scalarValue, amount, currency, valueKind);

            return newParameter(type, valueKind, parameterValue);
        }

        private LedgerParameterType typeOrNull(String value)
        {
            if (Strings.isNullOrEmpty(value))
                return null;

            return LedgerParameterType.fromCode(value);
        }

        private ValueKind valueKindOrNull(String value)
        {
            if (Strings.isNullOrEmpty(value))
                return null;

            return ValueKind.valueOf(value);
        }

        private Object readValue(HierarchicalStreamReader reader, UnmarshallingContext context, ValueKind valueKind)
        {
            return switch (valueKind)
            {
                case STRING, DECIMAL, LONG, BOOLEAN, LOCAL_DATE, LOCAL_DATE_TIME -> readValue(reader.getValue(),
                                valueKind);
                case MONEY -> context.convertAnother(null, Money.class);
                case SECURITY -> context.convertAnother(null, Security.class);
                case ACCOUNT -> context.convertAnother(null, Account.class);
                case PORTFOLIO -> context.convertAnother(null, Portfolio.class);
            };
        }

        private Object readValue(String value, String amount, String currency, ValueKind valueKind)
        {
            return switch (valueKind)
            {
                case STRING, DECIMAL, LONG, BOOLEAN, LOCAL_DATE, LOCAL_DATE_TIME -> readValue(require(value, "value"), //$NON-NLS-1$
                                valueKind);
                case MONEY -> Money.of(require(currency, "currency"), Long.parseLong(require(amount, "amount"))); //$NON-NLS-1$ //$NON-NLS-2$
                case SECURITY, ACCOUNT, PORTFOLIO -> throw new IllegalArgumentException(
                                LedgerDiagnosticCode.LEDGER_PERSIST_006
                                                .message(Messages.LedgerParameterReferenceValueMissingValueNode));
            };
        }

        private Object readValue(String value, ValueKind valueKind)
        {
            return switch (valueKind)
            {
                case STRING -> value;
                case DECIMAL -> new BigDecimal(value);
                case LONG -> Long.valueOf(value);
                case BOOLEAN -> Boolean.valueOf(value);
                case LOCAL_DATE -> LocalDate.parse(value);
                case LOCAL_DATE_TIME -> localDateTime(value);
                case MONEY, SECURITY, ACCOUNT, PORTFOLIO -> throw new IllegalArgumentException(
                                LedgerDiagnosticCode.LEDGER_PERSIST_007
                                                .message(MessageFormat.format(
                                                                Messages.LedgerParameterValueKindRequiresStructuredValue,
                                                                valueKind)));
            };
        }

        private LocalDateTime localDateTime(String value)
        {
            try
            {
                return LocalDateTime.parse(value);
            }
            catch (RuntimeException e)
            {
                return LocalDate.parse(value).atStartOfDay();
            }
        }

        private String require(String value, String name)
        {
            if (value == null)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PERSIST_008
                                .message(MessageFormat.format(Messages.LedgerParameterMissingAttribute, name)));

            return value;
        }

        private LedgerParameter<?> newParameter(LedgerParameterType type, ValueKind valueKind, Object value)
        {
            try
            {
                var constructor = LedgerParameter.class.getDeclaredConstructor(LedgerParameterType.class,
                                ValueKind.class, Object.class);
                constructor.setAccessible(true);
                return constructor.newInstance(type, valueKind, value);
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final class LedgerXmlSaveState
    {
        private final List<RemovedListElement> removedElements = new ArrayList<>();

        private void removeLedgerBackedTransactions(List<? extends Transaction> transactions)
        {
            for (var index = transactions.size() - 1; index >= 0; index--)
            {
                var transaction = transactions.get(index);

                if (transaction instanceof LedgerBackedTransaction)
                    remove(transactions, index);
            }
        }

        @SuppressWarnings("rawtypes")
        private void remove(List list, int index)
        {
            removedElements.add(new RemovedListElement(list, index, list.remove(index)));
        }

        private void restore()
        {
            for (var index = removedElements.size() - 1; index >= 0; index--)
                removedElements.get(index).restore();
        }
    }

    private record RemovedListElement(@SuppressWarnings("rawtypes") List list, int index, Object element)
    {
        @SuppressWarnings("unchecked")
        private void restore()
        {
            list.add(index, element);
        }
    }
}

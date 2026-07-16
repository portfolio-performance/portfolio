package name.abuchen.portfolio.model;

import static name.abuchen.portfolio.util.ProtobufUtil.asDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.asLocalDateTime;
import static name.abuchen.portfolio.util.ProtobufUtil.asTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.asUpdatedAtTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.fromLocalDateTime;
import static name.abuchen.portfolio.util.ProtobufUtil.fromTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromUpdatedAtTimestamp;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.Messages;
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
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PLedger;
import name.abuchen.portfolio.model.proto.v1.PLedgerEntry;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameter;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameterValueKind;
import name.abuchen.portfolio.model.proto.v1.PLedgerPosting;
import name.abuchen.portfolio.money.Money;

final class LedgerProtobufPersistenceSupport
{
    private LedgerProtobufPersistenceSupport()
    {
    }

    static void loadLedgerTruth(PClient newClient, Client client, ProtobufWriter.Lookup lookup)
    {
        if (hasLedgerTruth(newClient))
            loadLedger(newClient.getLedger(), client, lookup);
    }

    static void finalizeAfterLoad(Client client)
    {
        keepCorporateActionsOnly(client);
        LedgerProjectionService.restoreIfValid(client);
    }

    static void saveLedger(Client client, PClient.Builder newClient)
    {
        validateLedger(client);

        PLedger.Builder newLedger = PLedger.newBuilder();

        for (LedgerEntry entry : client.getLedger().getEntries())
        {
            if (entry.getType() != LedgerEntryType.CORPORATE_ACTION)
                continue;

            newLedger.addEntries(saveLedgerEntry(entry));
        }

        newClient.setLedger(newLedger);
    }

    static boolean hasLedgerTruth(PClient newClient)
    {
        return newClient.hasLedger() && newClient.getLedger().getEntriesCount() > 0;
    }

    private static void keepCorporateActionsOnly(Client client)
    {
        List.copyOf(client.getLedger().getEntries()).stream() //
                        .filter(entry -> entry.getType() != LedgerEntryType.CORPORATE_ACTION) //
                        .forEach(client.getLedger()::removeEntry);
    }

    private static void loadLedger(PLedger newLedger, Client client, ProtobufWriter.Lookup lookup)
    {
        for (PLedgerEntry newEntry : newLedger.getEntriesList())
        {
            var typeCode = newEntry.getTypeCode();
            LedgerEntry entry = LedgerModelLoadSupport.newEntry(UUID.randomUUID().toString(),
                            LedgerModelLoadSupport.entryTypeFromPersistedCode(typeCode),
                            fromTimestamp(newEntry.getDateTime()));

            if (newEntry.hasNote())
                LedgerModelLoadSupport.setEntryNote(entry, newEntry.getNote());
            if (newEntry.hasSource())
                LedgerModelLoadSupport.setEntrySource(entry, newEntry.getSource());
            if (newEntry.hasUpdatedAt())
                LedgerModelLoadSupport.setEntryUpdatedAt(entry, fromUpdatedAtTimestamp(newEntry.getUpdatedAt()));

            for (PLedgerParameter newParameter : newEntry.getParametersList())
                LedgerModelLoadSupport.addEntryParameter(entry, loadLedgerParameter(newParameter, lookup));

            for (PLedgerPosting newPosting : newEntry.getPostingsList())
                LedgerModelLoadSupport.addPosting(entry, loadLedgerPosting(newPosting, lookup));

            if (newEntry.hasUpdatedAt())
                LedgerModelLoadSupport.setEntryUpdatedAt(entry, fromUpdatedAtTimestamp(newEntry.getUpdatedAt()));

            if (entry.getType() == LedgerEntryType.CORPORATE_ACTION)
                LedgerModelLoadSupport.addEntry(client.getLedger(), entry);
        }
    }

    private static LedgerPosting loadLedgerPosting(PLedgerPosting newPosting, ProtobufWriter.Lookup lookup)
    {
        LedgerPosting posting = LedgerModelLoadSupport.newPosting(UUID.randomUUID().toString(),
                        LedgerPostingType.fromCode(newPosting.getTypeCode()));

        LedgerModelLoadSupport.setPostingAmount(posting, newPosting.getAmount());
        if (newPosting.hasCurrency())
            LedgerModelLoadSupport.setPostingCurrency(posting, newPosting.getCurrency());
        if (newPosting.hasForexAmount())
            LedgerModelLoadSupport.setPostingForexAmount(posting, newPosting.getForexAmount());
        if (newPosting.hasForexCurrency())
            LedgerModelLoadSupport.setPostingForexCurrency(posting, newPosting.getForexCurrency());
        if (newPosting.hasExchangeRate())
            LedgerModelLoadSupport.setPostingExchangeRate(posting, fromDecimalValue(newPosting.getExchangeRate()));
        if (newPosting.hasSecurity())
            LedgerModelLoadSupport.setPostingSecurity(posting, lookup.getSecurity(newPosting.getSecurity()));
        LedgerModelLoadSupport.setPostingShares(posting, newPosting.getShares());
        if (newPosting.hasAccount())
            LedgerModelLoadSupport.setPostingAccount(posting, lookup.getAccount(newPosting.getAccount()));
        if (newPosting.hasPortfolio())
            LedgerModelLoadSupport.setPostingPortfolio(posting, lookup.getPortfolio(newPosting.getPortfolio()));
        if (newPosting.hasSemanticRole())
            posting.setSemanticRole(LedgerPostingSemanticRole.valueOf(newPosting.getSemanticRole()));
        if (newPosting.hasDirection())
            posting.setDirection(LedgerPostingDirection.valueOf(newPosting.getDirection()));
        if (newPosting.hasCorporateActionLeg())
            posting.setCorporateActionLeg(corporateActionLeg(newPosting.getCorporateActionLeg()));
        if (newPosting.hasUnitRole())
            posting.setUnitRole(LedgerPostingUnitRole.valueOf(newPosting.getUnitRole()));
        if (newPosting.hasGroupKey())
            posting.setGroupKey(newPosting.getGroupKey());
        if (newPosting.hasLocalKey())
            posting.setLocalKey(newPosting.getLocalKey());

        for (PLedgerParameter newParameter : newPosting.getParametersList())
            LedgerModelLoadSupport.addPostingParameter(posting, loadLedgerParameter(newParameter, lookup));

        return posting;
    }

    private static CorporateActionLeg corporateActionLeg(String code)
    {
        for (var leg : CorporateActionLeg.values())
            if (leg.getCode().equals(code))
                return leg;

        return CorporateActionLeg.valueOf(code);
    }

    private static LedgerParameter<?> loadLedgerParameter(PLedgerParameter newParameter,
                    ProtobufWriter.Lookup lookup)
    {
        LedgerParameterType type = LedgerParameterType.fromCode(newParameter.getTypeCode());

        switch (newParameter.getValueKind())
        {
            case LEDGER_PARAMETER_VALUE_KIND_STRING:
                return LedgerParameter.ofString(type, newParameter.getStringValue());
            case LEDGER_PARAMETER_VALUE_KIND_DECIMAL:
                return LedgerParameter.ofDecimal(type, fromDecimalValue(newParameter.getDecimalValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LONG:
                return LedgerParameter.ofLong(type, newParameter.getLongValue());
            case LEDGER_PARAMETER_VALUE_KIND_MONEY:
                return LedgerParameter.ofMoney(type,
                                Money.of(newParameter.getMoneyCurrency(), newParameter.getMoneyAmount()));
            case LEDGER_PARAMETER_VALUE_KIND_SECURITY:
                return LedgerParameter.ofSecurity(type, lookup.getSecurity(newParameter.getSecurity()));
            case LEDGER_PARAMETER_VALUE_KIND_ACCOUNT:
                return LedgerParameter.ofAccount(type, lookup.getAccount(newParameter.getAccount()));
            case LEDGER_PARAMETER_VALUE_KIND_PORTFOLIO:
                return LedgerParameter.ofPortfolio(type, lookup.getPortfolio(newParameter.getPortfolio()));
            case LEDGER_PARAMETER_VALUE_KIND_BOOLEAN:
                if (!newParameter.hasBooleanValue())
                    throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PERSIST_009
                                    .message(Messages.LedgerProtobufBooleanParameterMissingBooleanValue));
                return LedgerParameter.ofBoolean(type, Boolean.valueOf(newParameter.getBooleanValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE:
                if (!newParameter.hasLocalDateValue())
                    throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PERSIST_010
                                    .message(Messages.LedgerProtobufLocalDateParameterMissingLocalDateValue));
                return LedgerParameter.ofLocalDate(type, LocalDate.ofEpochDay(newParameter.getLocalDateValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE_TIME:
                return LedgerParameter.ofLocalDateTime(type,
                                fromLocalDateTime(newParameter.getLocalDateTimeValue()));
            case LEDGER_PARAMETER_VALUE_KIND_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException(newParameter.getValueKind().toString());
        }
    }

    private static PLedgerEntry saveLedgerEntry(LedgerEntry entry)
    {
        PLedgerEntry.Builder newEntry = PLedgerEntry.newBuilder();

        newEntry.setTypeCode(entry.getType().getCode());
        newEntry.setDateTime(asTimestamp(entry.getDateTime()));

        if (entry.getNote() != null)
            newEntry.setNote(entry.getNote());
        if (entry.getSource() != null)
            newEntry.setSource(entry.getSource());
        if (entry.getUpdatedAt() != null)
            newEntry.setUpdatedAt(asUpdatedAtTimestamp(entry.getUpdatedAt()));

        for (LedgerParameter<?> parameter : entry.getParameters())
            newEntry.addParameters(saveLedgerParameter(parameter));

        for (LedgerPosting posting : entry.getPostings())
            newEntry.addPostings(saveLedgerPosting(posting));

        return newEntry.build();
    }

    private static PLedgerPosting saveLedgerPosting(LedgerPosting posting)
    {
        PLedgerPosting.Builder newPosting = PLedgerPosting.newBuilder();

        newPosting.setTypeCode(posting.getType().getCode());
        newPosting.setAmount(posting.getAmount());

        if (posting.getCurrency() != null)
            newPosting.setCurrency(posting.getCurrency());
        if (posting.getForexAmount() != null)
            newPosting.setForexAmount(posting.getForexAmount());
        if (posting.getForexCurrency() != null)
            newPosting.setForexCurrency(posting.getForexCurrency());
        if (posting.getExchangeRate() != null)
            newPosting.setExchangeRate(asDecimalValue(posting.getExchangeRate()));
        if (posting.getSecurity() != null)
            newPosting.setSecurity(posting.getSecurity().getUUID());
        newPosting.setShares(posting.getShares());
        if (posting.getAccount() != null)
            newPosting.setAccount(posting.getAccount().getUUID());
        if (posting.getPortfolio() != null)
            newPosting.setPortfolio(posting.getPortfolio().getUUID());
        if (posting.getSemanticRole() != null)
            newPosting.setSemanticRole(posting.getSemanticRole().name());
        if (posting.getDirection() != null)
            newPosting.setDirection(posting.getDirection().name());
        if (posting.getCorporateActionLeg() != null)
            newPosting.setCorporateActionLeg(posting.getCorporateActionLeg().getCode());
        if (posting.getUnitRole() != null)
            newPosting.setUnitRole(posting.getUnitRole().name());
        if (posting.getGroupKey() != null)
            newPosting.setGroupKey(posting.getGroupKey());
        if (posting.getLocalKey() != null)
            newPosting.setLocalKey(posting.getLocalKey());

        for (LedgerParameter<?> parameter : posting.getParameters())
            newPosting.addParameters(saveLedgerParameter(parameter));

        return newPosting.build();
    }

    private static PLedgerParameter saveLedgerParameter(LedgerParameter<?> parameter)
    {
        PLedgerParameter.Builder newParameter = PLedgerParameter.newBuilder();

        newParameter.setTypeCode(parameter.getType().getCode());
        newParameter.setValueKind(toProto(parameter.getValueKind()));

        switch (parameter.getValueKind())
        {
            case STRING:
                newParameter.setStringValue((String) parameter.getValue());
                break;
            case DECIMAL:
                newParameter.setDecimalValue(asDecimalValue((BigDecimal) parameter.getValue()));
                break;
            case LONG:
                newParameter.setLongValue((Long) parameter.getValue());
                break;
            case MONEY:
                Money money = (Money) parameter.getValue();
                newParameter.setMoneyAmount(money.getAmount());
                newParameter.setMoneyCurrency(money.getCurrencyCode());
                break;
            case SECURITY:
                newParameter.setSecurity(((Security) parameter.getValue()).getUUID());
                break;
            case ACCOUNT:
                newParameter.setAccount(((Account) parameter.getValue()).getUUID());
                break;
            case PORTFOLIO:
                newParameter.setPortfolio(((Portfolio) parameter.getValue()).getUUID());
                break;
            case BOOLEAN:
                newParameter.setBooleanValue(((Boolean) parameter.getValue()).booleanValue());
                break;
            case LOCAL_DATE:
                newParameter.setLocalDateValue(((LocalDate) parameter.getValue()).toEpochDay());
                break;
            case LOCAL_DATE_TIME:
                newParameter.setLocalDateTimeValue(asLocalDateTime((java.time.LocalDateTime) parameter.getValue()));
                break;
            default:
                throw new UnsupportedOperationException(parameter.getValueKind().toString());
        }

        return newParameter.build();
    }

    private static void validateLedger(Client client)
    {
        var ledger = client.getLedger();
        var result = LedgerStructuralValidator.validate(ledger);

        if (!result.isOK())
        {
            LedgerProjectionService.logSkipped(ledger, result);
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_PERSIST_002
                                            .message(MessageFormat.format(
                                                            Messages.LedgerProtobufInvalidLedgerPersistenceState,
                                                            LedgerDiagnosticMessageFormatter.formatValidationResult(
                                                                            ledger, result))));
        }
    }

    private static PLedgerParameterValueKind toProto(ValueKind valueKind)
    {
        switch (valueKind)
        {
            case STRING:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_STRING;
            case DECIMAL:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_DECIMAL;
            case LONG:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LONG;
            case MONEY:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_MONEY;
            case SECURITY:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_SECURITY;
            case ACCOUNT:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_ACCOUNT;
            case PORTFOLIO:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_PORTFOLIO;
            case BOOLEAN:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_BOOLEAN;
            case LOCAL_DATE:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE;
            case LOCAL_DATE_TIME:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE_TIME;
            default:
                throw new UnsupportedOperationException(valueKind.toString());
        }
    }
}

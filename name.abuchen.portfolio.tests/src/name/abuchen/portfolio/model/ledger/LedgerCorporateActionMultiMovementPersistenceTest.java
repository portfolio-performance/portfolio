package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Model/persistence proof for repeated SPIN_OFF movement legs.
 * This is low-level service coverage; UI support is intentionally outside this slice.
 */
@SuppressWarnings("nls")
public class LedgerCorporateActionMultiMovementPersistenceTest
{
    private static final byte[] PROTOBUF_SIGNATURE = new byte[] { 'P', 'P', 'P', 'B', 'V', '1' };
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 4);
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    public void testXmlRoundtripPreservesRepeatedSpinOffMovementLegs() throws Exception
    {
        var fixture = spinOffFixture();

        assertRepeatedSpinOff(fixture.entry());
        assertValid(fixture.client());
        assertNativeDefinitionValid(fixture.entry());

        var xml = saveXml(fixture.client());

        assertNoLedgerUuidTruth(xml);

        var loaded = loadXml(xml);
        var loadedEntry = onlyLedgerEntry(loaded);

        assertRepeatedSpinOff(loadedEntry);
        assertValid(loaded);
        assertNativeDefinitionValid(loadedEntry);
    }

    @Test
    public void testProtobufRoundtripPreservesRepeatedSpinOffMovementLegs() throws Exception
    {
        var fixture = spinOffFixture();

        assertRepeatedSpinOff(fixture.entry());
        assertValid(fixture.client());
        assertNativeDefinitionValid(fixture.entry());

        var bytes = ProtobufTestUtilities.save(fixture.client());
        var proto = parseProto(bytes);
        var protoEntry = proto.getLedger().getEntries(0);

        assertThat(proto.getLedger().getEntriesCount(), is(1));
        assertThat(protoEntry.getTypeCode(), is(LedgerEntryType.CORPORATE_ACTION.getCode()));
        assertThat(protoEntry.getPostingsCount(), is(8));
        assertNoCorporateActionSpecificLegacyTransactionType(proto);

        var loaded = ProtobufTestUtilities.load(bytes);
        var loadedEntry = onlyLedgerEntry(loaded);

        assertRepeatedSpinOff(loadedEntry);
        assertValid(loaded);
        assertNativeDefinitionValid(loadedEntry);
    }

    private Fixture spinOffFixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var sourceSecurity = new Security("Source AG", CurrencyUnit.EUR);
        var sourceSecurityB = new Security("Source B AG", CurrencyUnit.EUR);
        var targetSecurityA = new Security("Target A AG", CurrencyUnit.EUR);
        var targetSecurityB = new Security("Target B AG", CurrencyUnit.EUR);

        account.setName("Cash Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        account.setUpdatedAt(UPDATED_AT);
        portfolio.setUpdatedAt(UPDATED_AT);
        sourceSecurity.setUpdatedAt(UPDATED_AT);
        sourceSecurityB.setUpdatedAt(UPDATED_AT);
        targetSecurityA.setUpdatedAt(UPDATED_AT);
        targetSecurityB.setUpdatedAt(UPDATED_AT);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(sourceSecurity);
        client.addSecurity(sourceSecurityB);
        client.addSecurity(targetSecurityA);
        client.addSecurity(targetSecurityB);

        var entry = new LedgerEntry("spin-off-repeated");
        entry.setType(LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(DATE_TIME);
        entry.setSource("SPIN_OFF repeated movement proof");
        entry.setNote("core service proof only");
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF));
        entry.addParameter(LedgerParameter.ofLocalDate(LedgerParameterType.EFFECTIVE_DATE, DATE_TIME.toLocalDate()));

        entry.addPosting(spinOffSecurityPosting(portfolio, sourceSecurity, sourceSecurity, targetSecurityA,
                        LedgerPostingDirection.OUTBOUND, CorporateActionLeg.SOURCE_SECURITY, "main", "source-1",
                        10, 100));
        entry.addPosting(spinOffSecurityPosting(portfolio, sourceSecurityB, sourceSecurityB, targetSecurityB,
                        LedgerPostingDirection.OUTBOUND, CorporateActionLeg.SOURCE_SECURITY, "main", "source-2",
                        4, 40));
        entry.addPosting(spinOffSecurityPosting(portfolio, targetSecurityA, sourceSecurity, targetSecurityA,
                        LedgerPostingDirection.INBOUND, CorporateActionLeg.TARGET_SECURITY, "main", "target-1", 3,
                        60));
        entry.addPosting(spinOffSecurityPosting(portfolio, targetSecurityB, sourceSecurity, targetSecurityB,
                        LedgerPostingDirection.INBOUND, CorporateActionLeg.TARGET_SECURITY, "main", "target-2", 7,
                        40));
        entry.addPosting(spinOffCashPosting(account, "cash-1", "cash-1", 11));
        entry.addPosting(spinOffCashPosting(account, "cash-2", "cash-2", 22));
        entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE,
                        LedgerPostingUnitRole.FEE, CorporateActionLeg.FEE, "cash-1", "fee-1", 2));
        entry.addPosting(unitPosting(LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX,
                        LedgerPostingUnitRole.TAX, CorporateActionLeg.TAX, "cash-1", "tax-1", 4));

        client.getLedger().addEntry(entry);

        return new Fixture(client, entry);
    }

    private LedgerPosting securityPosting(Portfolio portfolio, Security security, LedgerPostingDirection direction,
                    CorporateActionLeg leg, String groupKey, String localKey, long shares, long amount)
    {
        var posting = new LedgerPosting("proof-" + localKey);

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setSecurity(security);
        posting.setShares(Values.Share.factorize(shares));
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(direction);
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(groupKey);
        posting.setLocalKey(localKey);

        return posting;
    }

    private LedgerPosting spinOffSecurityPosting(Portfolio portfolio, Security security, Security sourceSecurity,
                    Security targetSecurity, LedgerPostingDirection direction, CorporateActionLeg leg, String groupKey,
                    String localKey, long shares, long amount)
    {
        var posting = securityPosting(portfolio, security, direction, leg, groupKey, localKey, shares, amount);

        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));
        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY, sourceSecurity));
        posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY, targetSecurity));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE));
        posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR, BigDecimal.TEN));

        return posting;
    }

    private LedgerPosting spinOffCashPosting(Account account, String groupKey, String localKey, long amount)
    {
        var posting = cashPosting(account, LedgerPostingDirection.NEUTRAL, groupKey, localKey, amount);

        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.CASH_COMPENSATION.getCode()));
        posting.addParameter(LedgerParameter.ofMoney(LedgerParameterType.CASH_IN_LIEU_AMOUNT,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount))));

        return posting;
    }

    private LedgerPosting cashPosting(Account account, LedgerPostingDirection direction, String groupKey, String localKey,
                    long amount)
    {
        var posting = new LedgerPosting("proof-" + localKey);

        posting.setType(LedgerPostingType.CASH_COMPENSATION);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH_COMPENSATION);
        posting.setDirection(direction);
        posting.setCorporateActionLeg(CorporateActionLeg.CASH_COMPENSATION);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(groupKey);
        posting.setLocalKey(localKey);

        return posting;
    }

    private LedgerPosting unitPosting(LedgerPostingType type, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingUnitRole unitRole, CorporateActionLeg leg, String groupKey, String localKey,
                    long amount)
    {
        var posting = new LedgerPosting("proof-" + localKey);

        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(semanticRole);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(unitRole);
        posting.setGroupKey(groupKey);
        posting.setLocalKey(localKey);

        return posting;
    }

    private void assertRepeatedSpinOff(LedgerEntry entry)
    {
        assertThat(entry.getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(entry.getPostings().size(), is(8));

        var sourceSecurities = postings(entry, LedgerPostingType.SECURITY, LedgerPostingDirection.OUTBOUND,
                        CorporateActionLeg.SOURCE_SECURITY);
        var targetSecurities = postings(entry, LedgerPostingType.SECURITY, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY);
        var cashMovements = postings(entry, LedgerPostingType.CASH_COMPENSATION, LedgerPostingDirection.NEUTRAL,
                        CorporateActionLeg.CASH_COMPENSATION);
        var descriptors = LedgerDescriptorTestSupport.descriptors(entry);
        var targetDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG)
                        .toList();
        var sourceDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.OLD_SECURITY_LEG)
                        .toList();

        assertThat(sourceSecurities.size(), is(2));
        assertThat(localKeys(sourceSecurities), is(Set.of("source-1", "source-2")));
        assertThat(targetSecurities.size(), is(2));
        assertThat(localKeys(targetSecurities), is(Set.of("target-1", "target-2")));
        assertThat(cashMovements.size(), is(2));
        assertThat(localKeys(cashMovements), is(Set.of("cash-1", "cash-2")));
        assertThat(groupKeys(cashMovements), is(Set.of("cash-1", "cash-2")));
        assertThat(sourceDescriptors.size(), is(2));
        assertThat(sourceDescriptors.stream().map(descriptor -> descriptor.getSemanticInstanceKey().orElseThrow())
                        .collect(Collectors.toSet()), is(Set.of("source-1", "source-2")));
        assertThat(targetDescriptors.size(), is(2));
        assertThat(targetDescriptors.stream().map(descriptor -> descriptor.getSemanticInstanceKey().orElseThrow())
                        .collect(Collectors.toSet()), is(Set.of("target-1", "target-2")));
        var runtimeProjectionIds = targetDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                        .collect(Collectors.toSet());

        assertThat(runtimeProjectionIds.size(), is(2));
        assertTrue(runtimeProjectionIds.stream().anyMatch(id -> id.endsWith(":NEW_SECURITY_LEG:target-1")));
        assertTrue(runtimeProjectionIds.stream().anyMatch(id -> id.endsWith(":NEW_SECURITY_LEG:target-2")));
    }

    private List<LedgerPosting> postings(LedgerEntry entry, LedgerPostingType type, LedgerPostingDirection direction,
                    CorporateActionLeg leg)
    {
        return entry.getPostings().stream()
                        .filter(posting -> posting.getType() == type)
                        .filter(posting -> posting.getDirection() == direction)
                        .filter(posting -> posting.getCorporateActionLeg() == leg)
                        .toList();
    }

    private Set<String> localKeys(List<LedgerPosting> postings)
    {
        return postings.stream().map(LedgerPosting::getLocalKey).collect(Collectors.toSet());
    }

    private Set<String> groupKeys(List<LedgerPosting> postings)
    {
        return postings.stream().map(LedgerPosting::getGroupKey).collect(Collectors.toSet());
    }

    private String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-corporate-action-movement", ".xml");

        try
        {
            ClientFactory.save(client, file);
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadXml(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private PClient parseProto(byte[] bytes) throws IOException
    {
        return PClient.parseFrom(new ByteArrayInputStream(bytes, PROTOBUF_SIGNATURE.length,
                        bytes.length - PROTOBUF_SIGNATURE.length));
    }

    private LedgerEntry onlyLedgerEntry(Client client)
    {
        assertThat(client.getLedger().getEntries().size(), is(1));
        return client.getLedger().getEntries().get(0);
    }

    private void assertValid(Client client)
    {
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).toString(),
                        LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private void assertNativeDefinitionValid(LedgerEntry entry)
    {
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);

        assertTrue(result.format(), result.isOK());
    }

    private void assertNoLedgerUuidTruth(String xml)
    {
        var ledgerXml = ledgerSection(xml);

        assertFalse(ledgerXml.matches("(?s).*<ledger-entry[^>]*\\buuid=.*"));
        assertFalse(ledgerXml.matches("(?s).*<ledger-posting[^>]*\\buuid=.*"));
        assertFalse(ledgerXml.contains("<projectionRefs>"));
        assertFalse(ledgerXml.contains("<ledger-projection-ref"));
        assertFalse(ledgerXml.contains("<projection-membership"));
        assertFalse(ledgerXml.contains("<membership"));
        assertFalse(ledgerXml.contains("projectionUUID"));
        assertFalse(ledgerXml.contains("postingUUID"));
        assertFalse(ledgerXml.contains("primaryPostingUUID"));
        assertFalse(ledgerXml.contains("postingGroupUUID"));
    }

    private String ledgerSection(String xml)
    {
        var start = xml.indexOf("<ledger>");
        var end = xml.indexOf("</ledger>");

        assertTrue(start >= 0);
        assertTrue(end > start);

        return xml.substring(start, end);
    }

    private void assertNoCorporateActionSpecificLegacyTransactionType(PClient proto)
    {
        for (var transaction : proto.getTransactionsList())
            assertFalse("SPIN_OFF".equals(transaction.getType().name()));

        assertFalse(java.util.Arrays.stream(name.abuchen.portfolio.model.proto.v1.PTransaction.Type.values())
                        .map(Enum::name).anyMatch("SPIN_OFF"::equals));
    }

    private record Fixture(Client client, LedgerEntry entry)
    {
    }
}

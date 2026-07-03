package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Model/persistence proof only: current SPIN_OFF definition and descriptor acceptance for
 * repeated target legs is intentionally not asserted here. Projection derivation for repeated
 * target/cash movement legs is a later validator/descriptor/assembler feature.
 */
@SuppressWarnings("nls")
public class LedgerCorporateActionMultiMovementPersistenceTest
{
    private static final byte[] PROTOBUF_SIGNATURE = new byte[] { 'P', 'P', 'P', 'B', 'V', '1' };
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 4);
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    public void testXmlRoundtripPreservesRepeatedCorporateActionMovementLegs() throws Exception
    {
        var fixture = fixture();

        assertMultiMovementProof(fixture.entry());
        assertValid(fixture.client());

        var xml = saveXml(fixture.client());

        assertNoLedgerUuidTruth(xml);

        var loaded = loadXml(xml);
        var loadedEntry = onlyLedgerEntry(loaded);

        assertMultiMovementProof(loadedEntry);
        assertThat(loaded.getAllTransactions().size(), is(0));
        assertValid(loaded);
    }

    @Test
    public void testProtobufRoundtripPreservesRepeatedCorporateActionMovementLegs() throws Exception
    {
        var fixture = fixture();

        assertMultiMovementProof(fixture.entry());
        assertValid(fixture.client());

        var bytes = ProtobufTestUtilities.save(fixture.client());
        var proto = parseProto(bytes);
        var protoEntry = proto.getLedger().getEntries(0);

        assertThat(proto.getLedger().getEntriesCount(), is(1));
        assertThat(protoEntry.getTypeCode(), is(LedgerEntryType.CORPORATE_ACTION_MOVEMENT_CONFIRMATION.getCode()));
        assertThat(protoEntry.getPostingsCount(), is(7));
        assertThat(proto.getTransactionsCount(), is(0));
        assertNoLegacyTransactionTypeForProofEntry();

        var loaded = ProtobufTestUtilities.load(bytes);
        var loadedEntry = onlyLedgerEntry(loaded);

        assertMultiMovementProof(loadedEntry);
        assertThat(loaded.getAllTransactions().size(), is(0));
        assertValid(loaded);
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var sourceSecurity = new Security("Source AG", CurrencyUnit.EUR);
        var targetSecurityA = new Security("Target A AG", CurrencyUnit.EUR);
        var targetSecurityB = new Security("Target B AG", CurrencyUnit.EUR);

        account.setName("Cash Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);
        portfolio.setName("Portfolio");
        portfolio.setUpdatedAt(UPDATED_AT);
        sourceSecurity.setUpdatedAt(UPDATED_AT);
        targetSecurityA.setUpdatedAt(UPDATED_AT);
        targetSecurityB.setUpdatedAt(UPDATED_AT);

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(sourceSecurity);
        client.addSecurity(targetSecurityA);
        client.addSecurity(targetSecurityB);

        var entry = new LedgerEntry("proof-entry");
        entry.setType(LedgerEntryType.CORPORATE_ACTION_MOVEMENT_CONFIRMATION);
        entry.setDateTime(DATE_TIME);
        entry.setSource("seev.036 movement proof");
        entry.setNote("model/persistence proof only");

        entry.addPosting(securityPosting(portfolio, sourceSecurity, LedgerPostingDirection.OUTBOUND,
                        CorporateActionLeg.SOURCE_SECURITY, "main", "source-1", 10, 100));
        entry.addPosting(securityPosting(portfolio, targetSecurityA, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY, "main", "target-1", 3, 60));
        entry.addPosting(securityPosting(portfolio, targetSecurityB, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY, "main", "target-2", 7, 40));
        entry.addPosting(cashPosting(account, LedgerPostingDirection.INBOUND, "cash-1", "cash-1", 11));
        entry.addPosting(cashPosting(account, LedgerPostingDirection.INBOUND, "cash-2", "cash-2", 22));
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

    private void assertMultiMovementProof(LedgerEntry entry)
    {
        assertThat(entry.getType(), is(LedgerEntryType.CORPORATE_ACTION_MOVEMENT_CONFIRMATION));
        assertThat(entry.getPostings().size(), is(7));

        var targetSecurities = postings(entry, LedgerPostingType.SECURITY, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY);
        var cashMovements = postings(entry, LedgerPostingType.CASH_COMPENSATION, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.CASH_COMPENSATION);
        var cashOneUnits = entry.getPostings().stream()
                        .filter(posting -> "cash-1".equals(posting.getGroupKey()))
                        .filter(posting -> posting.getUnitRole() == LedgerPostingUnitRole.FEE
                                        || posting.getUnitRole() == LedgerPostingUnitRole.TAX)
                        .toList();

        assertThat(postings(entry, LedgerPostingType.SECURITY, LedgerPostingDirection.OUTBOUND,
                        CorporateActionLeg.SOURCE_SECURITY).size(), is(1));
        assertThat(targetSecurities.size(), is(2));
        assertThat(localKeys(targetSecurities), is(Set.of("target-1", "target-2")));
        assertThat(cashMovements.size(), is(2));
        assertThat(localKeys(cashMovements), is(Set.of("cash-1", "cash-2")));
        assertThat(groupKeys(cashMovements), is(Set.of("cash-1", "cash-2")));
        assertThat(cashOneUnits.size(), is(2));
        assertThat(cashOneUnits.stream().map(LedgerPosting::getUnitRole).collect(Collectors.toSet()),
                        is(Set.of(LedgerPostingUnitRole.FEE, LedgerPostingUnitRole.TAX)));
        assertThat(entry.getPostings().stream().map(LedgerPosting::getLocalKey).toList(), hasItem("target-1"));
        assertThat(entry.getPostings().stream().map(LedgerPosting::getLocalKey).toList(), hasItem("target-2"));
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

    private void assertNoLegacyTransactionTypeForProofEntry()
    {
        assertFalse(Arrays.stream(name.abuchen.portfolio.model.proto.v1.PTransaction.Type.values())
                        .map(Enum::name)
                        .anyMatch(LedgerEntryType.CORPORATE_ACTION_MOVEMENT_CONFIRMATION.getCode()::equals));
    }

    private record Fixture(Client client, LedgerEntry entry)
    {
    }
}

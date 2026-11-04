package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.rest.PairingService.PairingStatus;
import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

@SuppressWarnings("nls")
public class PairingServiceTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Instant now = Instant.parse("2026-07-20T10:00:00Z");

    private FakeHost host;
    private ClientStore store;
    private PairingService service;

    @Before
    public void setUp()
    {
        host = new FakeHost(List.of());
        store = new ClientStore(tempFolder.getRoot().toPath(), () -> now);
        service = new PairingService(store, host, () -> now);
    }

    private void advanceSeconds(long seconds)
    {
        now = now.plusSeconds(seconds);
    }

    @Test
    public void testCreateIsPendingAndPromptsTheUser()
    {
        var id = service.create("Claude Code");

        assertThat(id, is(notNullValue()));
        assertThat(service.poll(id).status(), is(PairingStatus.PENDING));

        assertThat(host.lastAccessRequest(), is(notNullValue()));
        assertThat(host.lastAccessRequest().getClientName(), is("Claude Code"));
        assertThat(host.lastAccessRequest().getExpiresAt(), is(now.plusSeconds(120)));
    }

    @Test
    public void testAllowAlwaysDeliversPersistentTokenExactlyOnce()
    {
        var id = service.create("Claude Code");
        host.lastAccessRequest().allowAlways();

        var result = service.poll(id);
        assertThat(result.status(), is(PairingStatus.APPROVED));
        assertThat(result.token(), is(notNullValue()));

        var client = store.authenticate(result.token());
        assertThat(client.isPresent(), is(true));
        assertThat(client.get().session(), is(false));
        assertThat(client.get().name(), is("Claude Code"));
        assertThat(result.persistent(), is(true));

        assertStatusIs(id, 404);
    }

    @Test
    public void testAllowForSessionMintsSessionToken()
    {
        var id = service.create("Claude Code");
        host.lastAccessRequest().allowForSession();

        var result = service.poll(id);
        assertThat(result.status(), is(PairingStatus.APPROVED));
        assertThat(store.authenticate(result.token()).get().session(), is(true));
        assertThat(result.persistent(), is(false));
    }

    /**
     * "Allow always" that the client file could not take is a session grant
     * after all, and the client that asked to keep its token has to hear that.
     */
    @Test
    public void testAPersistentGrantThatCouldNotBeWrittenSaysSo() throws Exception
    {
        Files.createDirectories(tempFolder.getRoot().toPath().resolve("api-clients.json"));
        var unwritable = new ClientStore(tempFolder.getRoot().toPath(), () -> now);
        var pairing = new PairingService(unwritable, host, () -> now);

        var id = pairing.create("Claude Code");
        host.lastAccessRequest().allowAlways();

        var result = pairing.poll(id);
        assertThat(result.status(), is(PairingStatus.APPROVED));
        assertThat(result.token(), is(notNullValue()));
        assertThat(result.persistent(), is(false));
        assertThat(unwritable.authenticate(result.token()).isPresent(), is(true));
    }

    @Test
    public void testDeclineIsOneShotAndStartsCooldown()
    {
        var id = service.create("Claude Code");
        host.lastAccessRequest().decline();

        var result = service.poll(id);
        assertThat(result.status(), is(PairingStatus.DENIED));
        assertThat(result.token(), is(nullValue()));
        assertStatusIs(id, 404);

        // cool-down: no new pairing request for 60s
        var tooEarly = assertCreateFails("Again");
        assertThat(tooEarly.getStatus(), is(429));
        assertThat(tooEarly.getType(), is("pairing-cooldown"));
        assertThat(tooEarly.getHeaders().get("Retry-After"), is("60"));

        advanceSeconds(61);
        assertThat(service.create("Again"), is(notNullValue()));
    }

    @Test
    public void testOnlyOnePendingRequestAtATime()
    {
        service.create("Claude Code");

        var rejected = assertCreateFails("Another");
        assertThat(rejected.getStatus(), is(429));
        assertThat(rejected.getType(), is("pairing-pending"));
        assertThat(rejected.getHeaders().get("Retry-After"), is("120"));
    }

    @Test
    public void testPendingRequestExpiresWithoutCooldown()
    {
        var id = service.create("Claude Code");

        advanceSeconds(121);
        assertThat(service.poll(id).status(), is(PairingStatus.EXPIRED));
        assertStatusIs(id, 404);

        // expiry frees the slot and does not punish the client
        assertThat(service.create("Claude Code"), is(notNullValue()));
    }

    /**
     * The token is minted when it is delivered, not when the user approves. A
     * request the client never polls must not leave a client in the store whose
     * token nobody holds - it would be a grant that cannot be revoked by anyone.
     */
    @Test
    public void testAnApprovedRequestThatIsNeverPolledCreatesNoClient()
    {
        var id = service.create("Claude Code");
        host.lastAccessRequest().allowAlways();
        // the approval alone mints nothing: the client is created by the poll
        assertThat(store.listClients(), is(List.of()));

        advanceSeconds(PairingService.OUTCOME_TTL.toSeconds() + 1);

        assertStatusIs(id, 404);
        assertThat(store.listClients(), is(List.of()));
    }

    /** outcomes nobody polled are dropped, so a long-running server does not accumulate them */
    @Test
    public void testPairingsNobodyPollsDoNotAccumulate()
    {
        for (int ii = 0; ii < 120; ii++)
        {
            service.create("Client " + ii);
            host.lastAccessRequest().allowAlways();
            // the expired outcome before it is gone with this decision
            assertThat(service.outcomeCount(), is(1));
            advanceSeconds(PairingService.OUTCOME_TTL.toSeconds() + 1);
            assertThat(store.listClients(), is(List.of()));
        }

        var id = service.create("Last");
        host.lastAccessRequest().allowAlways();

        assertThat(service.poll(id).token(), is(notNullValue()));
        assertThat(store.listClients(), hasSize(1));
    }

    /**
     * Reported in review: requests nobody decides expire into outcomes, and
     * each new request clears the ones past their time - without a decision,
     * which clears them as well.
     */
    @Test
    public void testExpiredRequestsNobodyDecidedDoNotAccumulate()
    {
        for (int ii = 0; ii < 12; ii++)
        {
            service.create("Client " + ii);
            advanceSeconds(PairingService.PENDING_TTL.toSeconds() + PairingService.OUTCOME_TTL.toSeconds() + 1);
        }

        // the last request has expired, the ones before it are gone
        service.create("Last");
        assertThat(service.outcomeCount(), is(1));
    }

    /**
     * Reported in review: an approval whose token could not be minted is not
     * lost - the failing poll leaves it, and the next poll delivers the token.
     */
    @Test
    public void testAnApprovalWhoseMintingFailedCanBePolledAgain() throws Exception
    {
        var failing = new AtomicBoolean();
        var flaky = new ClientStore(tempFolder.newFolder().toPath(), () -> {
            if (failing.get())
                throw new IllegalStateException("minting failed");
            return now;
        });
        var pairing = new PairingService(flaky, host, () -> now);

        var id = pairing.create("Claude Code");
        host.lastAccessRequest().allowAlways();

        failing.set(true);
        try
        {
            pairing.poll(id);
            fail("expected the minting to fail");
        }
        catch (IllegalStateException e)
        {
            assertThat(e.getMessage(), is("minting failed"));
        }
        failing.set(false);

        var result = pairing.poll(id);
        assertThat(result.status(), is(PairingStatus.APPROVED));
        assertThat(flaky.authenticate(result.token()).isPresent(), is(true));
        assertThat(flaky.listClients(), hasSize(1));
    }

    /**
     * Reported in review: a grant whose mint failed keeps its place in the
     * order in which outcomes are dropped - the oldest still goes first, not a
     * newer one behind it.
     */
    @Test
    public void testAFailedMintKeepsTheOutcomeInItsPlace() throws Exception
    {
        var failing = new AtomicBoolean();
        var flaky = new ClientStore(tempFolder.newFolder().toPath(), () -> {
            if (failing.get())
                throw new IllegalStateException("minting failed");
            return now;
        });
        var pairing = new PairingService(flaky, host, () -> now);

        var oldest = pairing.create("Oldest");
        host.lastAccessRequest().allowAlways();
        var next = pairing.create("Next");
        host.lastAccessRequest().allowAlways();

        failing.set(true);
        try
        {
            pairing.poll(oldest);
            fail("expected the minting to fail");
        }
        catch (IllegalStateException e)
        {
            assertThat(e.getMessage(), is("minting failed"));
        }
        failing.set(false);

        for (int ii = 1; ii < PairingService.MAX_OUTCOMES; ii++)
        {
            pairing.create("Client " + ii);
            host.lastAccessRequest().allowAlways();
        }

        // one beyond the bound: the oldest goes, the next one keeps its token
        assertThat(pairing.outcomeCount(), is(PairingService.MAX_OUTCOMES));
        assertThat(pairing.poll(next).token(), is(notNullValue()));
        try
        {
            pairing.poll(oldest);
            fail("expected the oldest outcome to be gone");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
        }
    }

    /**
     * The count bound, apart from the TTL: decisions in a burst keep only the
     * newest MAX_OUTCOMES outcomes, and the oldest one polls 404.
     */
    @Test
    public void testTheOldestOutcomeGoesBeyondTheBound()
    {
        var first = service.create("Client 0");
        host.lastAccessRequest().allowAlways();

        String second = null;
        String middle = null;
        String last = first;
        for (int ii = 1; ii <= PairingService.MAX_OUTCOMES; ii++)
        {
            last = service.create("Client " + ii);
            host.lastAccessRequest().allowAlways();
            if (ii == 1)
                second = last;
            if (ii == PairingService.MAX_OUTCOMES / 2)
                middle = last;
        }

        // bounded by the decisions themselves, before any poll prunes
        assertThat(service.outcomeCount(), is(PairingService.MAX_OUTCOMES));
        assertStatusIs(first, 404);

        // the bound holds after every decision, not only the first beyond it
        var third = service.create("Client extra");
        host.lastAccessRequest().allowAlways();
        assertThat(service.outcomeCount(), is(PairingService.MAX_OUTCOMES));
        assertStatusIs(second, 404);

        // only the oldest go: one from the middle and the newest keep their tokens
        assertThat(service.poll(middle).token(), is(notNullValue()));
        assertThat(service.poll(last).token(), is(notNullValue()));
        assertThat(service.poll(third).token(), is(notNullValue()));
    }

    /** a decision that arrives after the request is gone grants nothing, and says so */
    @Test
    public void testDecisionAfterExpiryIsIgnored()
    {
        var id = service.create("Claude Code");
        var request = host.lastAccessRequest();

        advanceSeconds(121);
        request.allowAlways();

        assertThat(service.poll(id).status(), is(PairingStatus.EXPIRED));
        assertThat(store.listClients(), is(List.of()));
    }

    @Test
    public void testSecondDecisionIsIgnored()
    {
        var id = service.create("Claude Code");
        var request = host.lastAccessRequest();
        request.decline();
        request.allowAlways();

        assertThat(service.poll(id).status(), is(PairingStatus.DENIED));
        assertThat(store.listClients(), is(List.of()));
    }

    @Test
    public void testUnknownIdIs404()
    {
        assertStatusIs("no-such-id", 404);
    }

    /**
     * The wire values are a fixed contract (the OpenAPI enum), so they must not
     * depend on the default locale. Turkish lower-casing turns 'I' into a
     * dotless 'ı', which would corrupt PENDING, DENIED and EXPIRED.
     */
    @Test
    public void testStatusWireValuesAreLocaleIndependent()
    {
        var previous = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(PairingStatus.PENDING.toJsonValue(), is("pending"));
            assertThat(PairingStatus.APPROVED.toJsonValue(), is("approved"));
            assertThat(PairingStatus.DENIED.toJsonValue(), is("denied"));
            assertThat(PairingStatus.EXPIRED.toJsonValue(), is("expired"));
        }
        finally
        {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void testClientNameIsMandatory()
    {
        assertValidationFails(null);
        assertValidationFails("");
        assertValidationFails("   ");
        // nothing left after stripping control characters
        assertValidationFails("\u0007\u001b");
    }

    @Test
    public void testClientNameIsLengthCapped()
    {
        assertThat(service.create("x".repeat(64)), is(notNullValue()));
        // validated before the pending-slot check: 422, not 429
        assertValidationFails("x".repeat(65));
    }

    @Test
    public void testControlCharactersAreStrippedFromTheDisplayedName()
    {
        service.create("Claude Code\r\n");

        assertThat(host.lastAccessRequest().getClientName(), is("Claude Code"));
    }

    private void assertStatusIs(String id, int expectedStatus)
    {
        try
        {
            service.poll(id);
            assertThat("expected ApiException with status " + expectedStatus, false, is(true));
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(expectedStatus));
        }
    }

    private ApiException assertCreateFails(String name)
    {
        try
        {
            service.create(name);
            throw new AssertionError("expected ApiException");
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    private void assertValidationFails(String name)
    {
        var exception = assertCreateFails(name);
        assertThat(exception.getStatus(), is(422));
    }
}

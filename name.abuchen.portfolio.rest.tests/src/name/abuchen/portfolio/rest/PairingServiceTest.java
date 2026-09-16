package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

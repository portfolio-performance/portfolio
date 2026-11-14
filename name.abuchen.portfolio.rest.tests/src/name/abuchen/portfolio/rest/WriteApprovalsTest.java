package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.Test;

import name.abuchen.portfolio.rest.internal.ApiException;
import name.abuchen.portfolio.rest.spi.WriteConfirmation;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * Asking the user before a file is changed.
 *
 * The distinction the whole class hangs on is between two ways a change does not
 * happen: the user said no, and nobody said anything. The first is an answer and
 * it stands - asking again would be arguing with it. The second is an absence,
 * and the caller is pointed at a retry. Either of them tested without the other
 * proves little, so the two sit next to each other here.
 */
@SuppressWarnings("nls")
public class WriteApprovalsTest
{
    private static final WriteApprovals.Facts ASSIGNMENT = new WriteApprovals.Facts("pp_assign_vehicle",
                    "Richard.portfolio", "cash-account", "Bevi", "W\u00e4hrung", "Wertpapierart");

    @Test
    public void aYesLetsTheChangeThroughAndThePromptNamesIt()
    {
        var host = new FakeHost(List.of());
        var approvals = new WriteApprovals(host);

        approvals.await(ASSIGNMENT);

        // the facts travel untouched, because the prompt is written by the
        // application and not by whoever is asking
        var prompt = host.lastWriteRequest();
        assertThat(prompt, notNullValue());
        assertThat(prompt.getOperation(), is("pp_assign_vehicle"));
        assertThat(prompt.getFileLabel(), is("Richard.portfolio"));
        assertThat(prompt.getSubjectType(), is("cash-account"));
        assertThat(prompt.getSubjectName(), is("Bevi"));
        assertThat(prompt.getTargetName(), is("W\u00e4hrung"));
        assertThat(prompt.getDetail(), is("Wertpapierart"));
        assertThat(prompt.getExpiresAt(), is(notNullValue()));

        // and no question is left open behind a change that went through
        assertThat(host.writeRequests(), is(1));
        assertThat(approvals.pendingOperation(), is(nullValue()));
    }

    @Test
    public void grantingForTheSessionAsksAboutThatOperationOnlyOnce()
    {
        var host = new FakeHost(List.of());
        host.setWriteApprovalAutoAllows(false);
        var approvals = new WriteApprovals(host);

        // the first answer grants the operation for the rest of the session
        assertThat(new Running(approvals, ASSIGNMENT).answer(host, WriteConfirmation::allowForSession),
                        is(nullValue()));
        assertThat(approvals.isGrantedForSession("pp_assign_vehicle"), is(true));

        // the same operation again, and no second question
        approvals.await(ASSIGNMENT);
        assertThat(host.writeRequests(), is(1));

        // a different operation is still asked about: a session grant is not a
        // blanket yes, and deleting is not placing
        var other = new Running(approvals, new WriteApprovals.Facts("pp_delete_instrument", "Richard.portfolio",
                        "instrument", "Apple", null, null));
        waitUntil(() -> host.writeRequests() == 2);
        other.answer(host, WriteConfirmation::decline);
    }

    @Test
    public void aDenialStandsAndIsNotOfferedAgain()
    {
        var host = new FakeHost(List.of());
        host.setWriteApprovalAutoAllows(false);
        var approvals = new WriteApprovals(host);

        var refused = asApiException(new Running(approvals, ASSIGNMENT).answer(host, WriteConfirmation::decline));

        assertThat(refused.getStatus(), is(403));
        assertThat(refused.getType(), is("write-declined"));
        assertThat(refused.getDetail(), containsString("pp_assign_vehicle"));
        assertThat(approvals.pendingOperation(), is(nullValue()));
        assertThat(approvals.isGrantedForSession("pp_assign_vehicle"), is(false));
    }

    @Test
    public void nobodyAnsweringIsNotADenial()
    {
        var host = new FakeHost(List.of());
        host.setWriteApprovalAutoAllows(false);
        var approvals = new WriteApprovals(host, Duration.ofMillis(300));

        // nobody is there to answer, and the window closes on its own
        var unanswered = asApiException(new Running(approvals, ASSIGNMENT).answer(host, request ->
        {
            /* the user stays away */
        }));

        // nothing was changed and nobody said no, so this one may be asked again
        assertThat(unanswered.getStatus(), is(423));
        assertThat(unanswered.getType(), is("write-unanswered"));
        assertThat(approvals.pendingOperation(), is(nullValue()));

        // and the prompt that was never answered does not block the next one
        host.setWriteApprovalAutoAllows(true);
        approvals.await(ASSIGNMENT);
        assertThat(host.writeRequests(), is(2));
    }

    @Test
    public void whileOneChangeAwaitsAnAnswerAnotherIsTurnedAway()
    {
        var host = new FakeHost(List.of());
        host.setWriteApprovalAutoAllows(false);
        // long enough that a busy machine cannot answer by timing out first, and
        // short enough to stay a test rather than a wait
        var approvals = new WriteApprovals(host, Duration.ofMillis(2500));

        // parked on purpose: the first request stays open so that the second
        // arrives while it is open
        var parked = new Running(approvals, ASSIGNMENT);
        waitUntil(() -> host.writeRequests() == 1);
        assertThat(approvals.pendingOperation(), is("pp_assign_vehicle"));

        var turnedAway = asApiException(capture(() -> approvals.await(ASSIGNMENT)));
        assertThat(turnedAway.getStatus(), is(429));
        assertThat(turnedAway.getType(), is("write-pending"));

        // only one prompt is ever shown: a second would have the user answer a
        // question whose change is already gone
        assertThat(host.writeRequests(), is(1));

        // left to expire: answer() waits for the end, and the end is unanswered -
        // which is also what guarantees that no thread of this test outlives it
        assertThat(asApiException(parked.answer(host, request ->
        {
            /* nothing: nobody answers */
        })).getStatus(), is(423));
    }

    /** however often a prompt tries to answer, the first answer is the decision */
    @Test
    public void theFirstAnswerWins()
    {
        var host = new FakeHost(List.of());
        host.setWriteApprovalAutoAllows(false);
        var approvals = new WriteApprovals(host);

        var running = new Running(approvals, ASSIGNMENT);
        waitUntil(() -> host.writeRequests() == 1);

        var prompt = host.lastWriteRequest();
        prompt.decline();
        prompt.allowOnce(); // a second answer, which must not undo the first

        assertThat(asApiException(running.join()).getType(), is("write-declined"));
        assertThat(approvals.isGrantedForSession("pp_assign_vehicle"), is(false));
    }

    // ---------------------------------------------------------------- helpers

    /** one request on its own thread, the way a route runs it */
    private static final class Running
    {
        private final Thread thread;
        private volatile Throwable outcome;

        private Running(WriteApprovals approvals, WriteApprovals.Facts facts)
        {
            this.thread = new Thread(() -> this.outcome = capture(() -> approvals.await(facts)));
            this.thread.start();
        }

        /** answers the prompt once it appears, waits for the request to end */
        private Throwable answer(FakeHost host, Consumer<WriteConfirmation> answer)
        {
            waitUntil(() -> host.lastWriteRequest() != null);
            answer.accept(host.lastWriteRequest());
            return join();
        }

        private Throwable join()
        {
            try
            {
                thread.join(10000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            assertThat("the request was still open after ten seconds", thread.isAlive(), is(false));
            return outcome;
        }
    }

    /** runs a call and hands back what it threw instead of throwing it */
    private static Throwable capture(Call body)
    {
        try
        {
            body.run();
            return null;
        }
        catch (Exception | Error e)
        {
            return e;
        }
    }

    /** a call that may throw, which Runnable will not allow */
    private interface Call
    {
        void run() throws Exception;
    }

    private static ApiException asApiException(Throwable t)
    {
        if (t instanceof ApiException e)
            return e;
        throw new AssertionError("expected an ApiException, got: " + t, t);
    }

    private static void waitUntil(BooleanSupplier condition)
    {
        var deadline = System.currentTimeMillis() + 10000;
        while (!condition.getAsBoolean())
        {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("condition not met within ten seconds");
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}

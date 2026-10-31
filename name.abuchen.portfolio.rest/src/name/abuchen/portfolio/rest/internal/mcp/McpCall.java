package name.abuchen.portfolio.rest.internal.mcp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.rest.internal.Progress;
import name.abuchen.portfolio.rest.internal.Response;

/**
 * One tool call answered as an event stream: the {@link Progress} it reports
 * to, and the stream its messages go out on.
 * <p/>
 * A phase or a step becomes a {@code notifications/progress} - only when the
 * client asked for them with a {@code progressToken}, as the protocol says.
 * Heartbeats go out regardless, as SSE comments that no client hands to its
 * model: they keep an idle connection open and are how a client that hung up
 * is noticed, since writing to it fails. Either way the call counts as
 * cancelled from then on, as it does after a {@code notifications/cancelled}.
 * <p/>
 * Writes come from up to three threads - the request, its calculation and the
 * heartbeat - and are made one at a time, under a lock rather than a monitor,
 * so that a virtual thread writing to the socket is not pinned.
 */
/* package */ final class McpCall implements Progress
{
    /** at most this often a step is reported; a report per instrument would be noise */
    private static final long STEP_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private final JsonElement progressToken;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile boolean cancelled;
    private Response.Sink sink;

    /** the last progress value sent; the protocol wants each one larger */
    private long sequence;
    /** where the current run of steps started, or -1 */
    private long stepBase = -1;
    private long lastStepNanos;

    /* package */ McpCall(JsonElement progressToken)
    {
        this.progressToken = progressToken;
    }

    /** from now on, messages go out on this stream */
    /* package */ void attach(Response.Sink newSink)
    {
        lock.lock();
        try
        {
            this.sink = newSink;
        }
        finally
        {
            lock.unlock();
        }
    }

    /* package */ void cancel()
    {
        cancelled = true;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void phase(String message)
    {
        if (progressToken == null)
            return;

        lock.lock();
        try
        {
            // a phase ends whatever run of steps came before it
            stepBase = -1;
            notifyProgress(++sequence, null, message);
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void step(int done, int total, String message)
    {
        if (progressToken == null)
            return;

        lock.lock();
        try
        {
            var now = System.nanoTime();
            if (stepBase >= 0 && now - lastStepNanos < STEP_INTERVAL_NANOS)
                return;

            if (stepBase < 0)
                stepBase = sequence;

            // counted on from the phases before, so that the value keeps rising;
            // the total says how far this run of steps goes
            var value = stepBase + done + 1;
            if (value <= sequence)
                return;

            sequence = value;
            lastStepNanos = now;
            notifyProgress(value, stepBase + total, message);
        }
        finally
        {
            lock.unlock();
        }
    }

    /** a comment line: ignored by every client, but it fails when nobody listens */
    /* package */ void heartbeat()
    {
        write(": keepalive\n\n"); //$NON-NLS-1$
    }

    /** one JSON-RPC message as one SSE event */
    /* package */ void send(JsonObject message)
    {
        // the JSON is compact, so it is one line, and one data line is one event
        write("event: message\ndata: " + message + "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void notifyProgress(long progress, Long total, String message)
    {
        var params = new JsonObject();
        params.add("progressToken", progressToken); //$NON-NLS-1$
        params.addProperty("progress", progress); //$NON-NLS-1$
        if (total != null)
            params.addProperty("total", total); //$NON-NLS-1$
        if (message != null)
            params.addProperty("message", message); //$NON-NLS-1$

        var notification = new JsonObject();
        notification.addProperty("jsonrpc", "2.0"); //$NON-NLS-1$ //$NON-NLS-2$
        notification.addProperty("method", "notifications/progress"); //$NON-NLS-1$ //$NON-NLS-2$
        notification.add("params", params); //$NON-NLS-1$
        send(notification);
    }

    private void write(String text)
    {
        lock.lock();
        try
        {
            if (sink == null || cancelled)
                return;
            sink.send(text);
        }
        catch (IOException e)
        {
            // the client hung up, or stopped reading and the watchdog closed the
            // connection: nobody is left to tell, and nobody to compute for
            cancelled = true;
        }
        finally
        {
            lock.unlock();
        }
    }
}

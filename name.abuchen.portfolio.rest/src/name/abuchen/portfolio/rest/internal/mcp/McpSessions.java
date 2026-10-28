package name.abuchen.portfolio.rest.internal.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;

/**
 * The MCP sessions this endpoint issued: an {@code Mcp-Session-Id} per
 * {@code initialize}, and per session the tool calls still running, so that a
 * {@code notifications/cancelled} - which arrives as a request of its own - can
 * find the call it names.
 * <p/>
 * That is all a session holds. Everything else still travels per request, and
 * a request without a session id is served as before; it only cannot be
 * cancelled. The store is bounded: a client that initializes over and over
 * displaces the session used least recently, whose running calls then simply
 * finish.
 */
final class McpSessions
{
    /** more than a personal API ever has clients */
    /* package */ static final int MAX_SESSIONS = 64;

    /** one session: the calls of its client that are still running, by request id */
    /* package */ static final class Session
    {
        private final Map<String, McpCall> running = new ConcurrentHashMap<>();

        /* package */ void register(JsonElement requestId, McpCall call)
        {
            running.put(key(requestId), call);
        }

        /* package */ void unregister(JsonElement requestId, McpCall call)
        {
            running.remove(key(requestId), call);
        }

        /** @return whether a running call had that id */
        /* package */ boolean cancel(JsonElement requestId)
        {
            var call = running.get(key(requestId));
            if (call == null)
                return false;
            call.cancel();
            return true;
        }

        private void cancelAll()
        {
            running.values().forEach(McpCall::cancel);
        }

        /** the id as JSON text: 1 and "1" are two different requests */
        private static String key(JsonElement requestId)
        {
            return requestId.toString();
        }
    }

    private final LinkedHashMap<String, Session> sessions = new LinkedHashMap<>(16, 0.75f, true)
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Session> eldest)
        {
            return size() > MAX_SESSIONS;
        }
    };

    /** a new session, whose id the {@code initialize} response carries */
    public synchronized String open()
    {
        // random, not counted: knowing one id must not tell anyone another
        var id = UUID.randomUUID().toString();
        sessions.put(id, new Session());
        return id;
    }

    /** the session, or null when this endpoint never issued it or no longer knows it */
    public synchronized Session get(String id)
    {
        return id == null ? null : sessions.get(id);
    }

    public synchronized boolean exists(String id)
    {
        return id != null && sessions.containsKey(id);
    }

    /** ends the session, cancelling what still runs in it; false when there was none */
    public boolean close(String id)
    {
        Session session;
        synchronized (this)
        {
            session = id == null ? null : sessions.remove(id);
        }
        if (session == null)
            return false;
        session.cancelAll();
        return true;
    }

    /** @return whether the session had a running call with that id */
    public boolean cancel(String id, JsonElement requestId)
    {
        var session = get(id);
        return session != null && requestId != null && session.cancel(requestId);
    }
}

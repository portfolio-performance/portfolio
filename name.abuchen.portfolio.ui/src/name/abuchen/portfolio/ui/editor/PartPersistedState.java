package name.abuchen.portfolio.ui.editor;

import java.util.Map;
import java.util.Objects;

/**
 * PartPersistedState is a wrapper around the map that holds persisted state of
 * a MPart. It allows easier injection and contains helper methods to convert to
 * primitives (including error handling).
 */
public class PartPersistedState
{
    private final Map<String, String> state;

    /* package */ PartPersistedState(Map<String, String> persistedState)
    {
        this.state = Objects.requireNonNull(persistedState);
    }

    /**
     * Returns the current value of the integer-valued state with the given
     * name. Returns the value <code>0</code> if there is no value with the
     * given name, or if the current value cannot be treated as an integer.
     */
    public int getInt(String key)
    {
        try
        {
            String v = state.get(key);
            return v == null ? 0 : Integer.parseInt(v);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    public void setValue(String key, int value)
    {
        state.put(key, Integer.toString(value));
    }
}

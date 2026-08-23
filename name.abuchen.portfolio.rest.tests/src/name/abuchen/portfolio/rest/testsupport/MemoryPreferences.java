package name.abuchen.portfolio.rest.testsupport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * In-memory {@link IEclipsePreferences} for the dev server.
 * <p>
 * Only the registry-used parts are implemented; listeners and persistence are
 * no-ops.
 */
@SuppressWarnings("nls")
public class MemoryPreferences implements IEclipsePreferences
{
    private final MemoryPreferences parent;
    private final String name;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, MemoryPreferences> children = new LinkedHashMap<>();
    private boolean removed = false;

    public MemoryPreferences()
    {
        this(null, "");
    }

    private MemoryPreferences(MemoryPreferences parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public Preferences node(String path)
    {
        checkRemoved();

        var slash = path.indexOf('/');
        if (slash == 0)
            return root().node(path.substring(1));
        if (slash > 0)
            return node(path.substring(0, slash)).node(path.substring(slash + 1));
        if (path.isEmpty())
            return this;

        return children.computeIfAbsent(path, key -> new MemoryPreferences(this, key));
    }

    @Override
    public boolean nodeExists(String path)
    {
        var slash = path.indexOf('/');
        if (slash >= 0)
            throw new UnsupportedOperationException("only direct children are supported");
        return path.isEmpty() ? !removed : children.containsKey(path);
    }

    @Override
    public String[] childrenNames()
    {
        checkRemoved();
        return children.keySet().toArray(new String[0]);
    }

    @Override
    public void removeNode()
    {
        checkRemoved();
        removed = true;
        if (parent != null)
            parent.children.remove(name);
    }

    @Override
    public String absolutePath()
    {
        return parent == null ? "/" : parent.absolutePath().replaceAll("/$", "") + "/" + name;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Preferences parent()
    {
        return parent;
    }

    private MemoryPreferences root()
    {
        return parent == null ? this : parent.root();
    }

    private void checkRemoved()
    {
        if (removed)
            throw new IllegalStateException("node " + name + " has been removed");
    }

    @Override
    public void put(String key, String value)
    {
        checkRemoved();
        values.put(key, value);
    }

    @Override
    public String get(String key, String def)
    {
        checkRemoved();
        return values.getOrDefault(key, def);
    }

    @Override
    public void remove(String key)
    {
        checkRemoved();
        values.remove(key);
    }

    @Override
    public void clear()
    {
        checkRemoved();
        values.clear();
    }

    @Override
    public String[] keys()
    {
        checkRemoved();
        return values.keySet().toArray(new String[0]);
    }

    @Override
    public void putInt(String key, int value)
    {
        put(key, Integer.toString(value));
    }

    @Override
    public int getInt(String key, int def)
    {
        return parse(get(key, null), def, Integer::parseInt);
    }

    @Override
    public void putLong(String key, long value)
    {
        put(key, Long.toString(value));
    }

    @Override
    public long getLong(String key, long def)
    {
        return parse(get(key, null), def, Long::parseLong);
    }

    @Override
    public void putBoolean(String key, boolean value)
    {
        put(key, Boolean.toString(value));
    }

    @Override
    public boolean getBoolean(String key, boolean def)
    {
        var value = get(key, null);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    @Override
    public void putFloat(String key, float value)
    {
        put(key, Float.toString(value));
    }

    @Override
    public float getFloat(String key, float def)
    {
        return parse(get(key, null), def, Float::parseFloat);
    }

    @Override
    public void putDouble(String key, double value)
    {
        put(key, Double.toString(value));
    }

    @Override
    public double getDouble(String key, double def)
    {
        return parse(get(key, null), def, Double::parseDouble);
    }

    @Override
    public void putByteArray(String key, byte[] value)
    {
        put(key, java.util.Base64.getEncoder().encodeToString(value));
    }

    @Override
    public byte[] getByteArray(String key, byte[] def)
    {
        var value = get(key, null);
        return value == null ? def : java.util.Base64.getDecoder().decode(value);
    }

    private static <T> T parse(String value, T def, java.util.function.Function<String, T> parser)
    {
        try
        {
            return value == null ? def : parser.apply(value);
        }
        catch (NumberFormatException e)
        {
            return def;
        }
    }

    @Override
    public void flush()
    {
        // nothing to persist
    }

    @Override
    public void sync()
    {
        // nothing to reload
    }

    @Override
    public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException
    {
        if (visitor.visit(this))
        {
            for (var child : children.values().toArray(new MemoryPreferences[0]))
                child.accept(visitor);
        }
    }

    @Override
    public void addNodeChangeListener(INodeChangeListener listener)
    {
        // no listeners
    }

    @Override
    public void removeNodeChangeListener(INodeChangeListener listener)
    {
        // no listeners
    }

    @Override
    public void addPreferenceChangeListener(IPreferenceChangeListener listener)
    {
        // no listeners
    }

    @Override
    public void removePreferenceChangeListener(IPreferenceChangeListener listener)
    {
        // no listeners
    }
}

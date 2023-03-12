package name.abuchen.portfolio.datatransfer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to hold values extracted during the processing of a file (PDF,
 * CSV).
 */
public class DocumentContext implements Map<String, String>
{
    private Map<String, Object> backingMap = new HashMap<>();

    @Override
    public int size()
    {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return backingMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return backingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return backingMap.containsValue(value);
    }

    @Override
    public String get(Object key)
    {
        Object v = backingMap.get(key);
        return v == null ? null : v instanceof String ? (String) v : String.valueOf(v); // NOSONAR
    }

    @Override
    public String put(String key, String value)
    {
        Object v = backingMap.put(key, value);
        return v == null ? null : v instanceof String ? (String) v : String.valueOf(v); // NOSONAR
    }

    @Override
    public String remove(Object key)
    {
        Object v = backingMap.remove(key);
        return v == null ? null : v instanceof String ? (String) v : String.valueOf(v); // NOSONAR
    }

    public <T> void putType(T value)
    {
        backingMap.put(value.getClass().getName(), value);
    }

    public <T> Optional<T> getType(Class<T> key)
    {
        Object v = backingMap.get(key.getName());
        return key.isInstance(v) ? Optional.of(key.cast(v)) : Optional.empty();
    }

    public void removeType(Class<?> key)
    {
        backingMap.remove(key.getName());
    }

    public boolean getBoolean(String key)
    {
        Object answer = backingMap.get(key);

        if (answer == null)
            return false;

        if (answer instanceof Boolean b)
            return b;

        if (answer instanceof String s)
            return Boolean.getBoolean(s);

        throw new IllegalArgumentException(key);
    }

    public void putBoolean(String key, boolean value)
    {
        backingMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m)
    {
        backingMap.putAll(m);
    }

    @Override
    public void clear()
    {
        backingMap.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return backingMap.keySet();
    }

    @Override
    public Collection<String> values()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, String>> entrySet()
    {
        throw new UnsupportedOperationException();
    }
}
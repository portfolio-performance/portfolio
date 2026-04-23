package name.abuchen.portfolio.ui.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debug-only helper to track whether a color property has been applied via CSS.
 * This does not replace the actual color storage. It only records whether a
 * given domain/property combination has been written by a CSS handler during
 * runtime so that debug views can distinguish CSS-backed from fallback values.
 */
public final class ColorSourceTracker
{
    private static final Map<String, Set<String>> CSS_APPLIED = new ConcurrentHashMap<>();

    private ColorSourceTracker()
    {
    }

    public static void markCssApplied(String domain, String property)
    {
        CSS_APPLIED.computeIfAbsent(domain, key -> ConcurrentHashMap.newKeySet()).add(property);
    }

    public static boolean isCssApplied(String domain, String property)
    {
        return CSS_APPLIED.getOrDefault(domain, Collections.emptySet()).contains(property);
    }

    public static boolean hasAnyCssApplied(String domain)
    {
        return !CSS_APPLIED.getOrDefault(domain, Collections.emptySet()).isEmpty();
    }

    public static int countCssApplied(String domain)
    {
        return CSS_APPLIED.getOrDefault(domain, Collections.emptySet()).size();
    }

    public static Set<String> getAppliedProperties(String domain)
    {
        return Collections.unmodifiableSet(CSS_APPLIED.getOrDefault(domain, Collections.emptySet()));
    }

    public static void clear()
    {
        CSS_APPLIED.clear();
    }
}
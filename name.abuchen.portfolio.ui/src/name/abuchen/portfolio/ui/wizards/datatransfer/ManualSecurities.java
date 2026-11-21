package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Security;

/**
 * Securities newly created on a manual entry page. A new security is created
 * in the client only when the items are imported: until then it is listed as
 * a security entry and offered in the transaction dialogs of all manual pages.
 * Transactions which use it depend on its security entry. The class has no
 * user interface.
 */
final class ManualSecurities
{
    private final List<Security> additionalSecurities;
    private final List<ExtractedEntry> entries;
    private final Supplier<Stream<ExtractedEntry>> allManualEntries;

    /**
     * @param additionalSecurities
     *            the securities offered in addition to the ones of the client
     *            (shared by all pages of the wizard)
     * @param entries
     *            the entries of this page
     * @param allManualEntries
     *            the entries of all manual entry pages
     */
    ManualSecurities(List<Security> additionalSecurities, List<ExtractedEntry> entries,
                    Supplier<Stream<ExtractedEntry>> allManualEntries)
    {
        this.additionalSecurities = additionalSecurities;
        this.entries = entries;
        this.allManualEntries = allManualEntries;
    }

    /**
     * Adds a newly created security: it is listed as security entry on top of
     * this page and offered in the transaction dialogs.
     */
    void add(Security security)
    {
        if (!additionalSecurities.contains(security))
            additionalSecurities.add(security);

        entries.add(0, new ExtractedEntry(new Extractor.SecurityItem(security)));
    }

    /**
     * Transactions which use a security newly created on one of the manual
     * pages depend on its security entry, so that they are not imported
     * without the security.
     */
    List<ExtractedEntry> withSecurityDependencies(List<ExtractedEntry> harvested)
    {
        for (var entry : harvested)
        {
            findNewSecurityEntry(entry.getItem().getSecurity()).ifPresent(dependency -> {
                entry.setSecurityDependency(dependency);
                applyReplacement(entry);
            });
        }

        return harvested;
    }

    private Optional<ExtractedEntry> findNewSecurityEntry(Security security)
    {
        if (security == null)
            return Optional.empty();

        return allManualEntries.get() //
                        .filter(e -> e.getItem() instanceof Extractor.SecurityItem
                                        && e.getItem().getSecurity() == security)
                        .findFirst();
    }

    /**
     * Removes the entries. If a newly created security is removed, it is no
     * longer offered, and the transactions which use it are removed as well
     * (transactions on other manual pages are no longer imported because their
     * security entry is excluded). While an editor is open, securities are not
     * removed.
     */
    void remove(List<ExtractedEntry> selected, boolean editorOpen)
    {
        // a security must not be removed while an editor is open: the
        // transaction created by the editor could no longer be linked to it
        var toRemove = editorOpen ? selected.stream()
                        .filter(entry -> !(entry.getItem() instanceof Extractor.SecurityItem)).toList() : selected;

        entries.removeAll(toRemove);

        for (var entry : toRemove)
        {
            if (!(entry.getItem() instanceof Extractor.SecurityItem))
                continue;

            additionalSecurities.remove(entry.getItem().getSecurity());
            entry.setImported(false);
            entries.removeIf(e -> e.getSecurityDependency() == entry);
        }
    }

    /**
     * Applies the existing security which the user chose instead of a newly
     * created one ("use existing security") to the dependent entries of all
     * manual entry pages. The table stores the choice at the security entry,
     * but sets it only for the dependent entries of its own page.
     */
    void applyReplacements()
    {
        allManualEntries.get().filter(entry -> entry.getSecurityDependency() != null)
                        .forEach(ManualSecurities::applyReplacement);
    }

    private static void applyReplacement(ExtractedEntry entry)
    {
        var dependency = entry.getSecurityDependency();
        if (!(dependency.getItem() instanceof Extractor.SecurityItem))
            return;

        entry.setSecurityOverride(dependency.isImported() ? null : dependency.getSecurityOverride());
    }

    /**
     * Returns the items of this page to import. As for the automatically
     * extracted items (see ImportController), the existing security chosen by
     * the user replaces the newly created one.
     */
    List<Extractor.Item> getItemsToImport()
    {
        applyReplacements();

        return entries.stream().filter(ExtractedEntry::isImported).map(entry -> {
            if (entry.getSecurityOverride() != null)
                entry.getItem().setSecurity(entry.getSecurityOverride());
            return entry.getItem();
        }).toList();
    }
}

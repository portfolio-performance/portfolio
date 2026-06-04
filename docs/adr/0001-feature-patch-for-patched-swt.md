# Ship patched SWT via a feature patch in the product build

Portfolio Performance needs to ship patched macOS SWT fragments (TextKit-1 in-place-editor
freeze fix, eclipse-platform/eclipse.platform.ui#1069) with a newer qualifier than the upstream
Eclipse release, so that p2 can update already-installed users. The upstream p2 metadata pins the
native SWT fragments **and** the `org.eclipse.swt` host bundle to exact versions (both the
`org.eclipse.e4.rcp` feature and the `org.eclipse.swt` host IU require the fragments at
`[v,v]`), which prevents re-qualified fragments from resolving cleanly.

**Decision**

Keep consuming the upstream `org.eclipse.e4.rcp` feature and apply an Eclipse **feature patch**
(`org.eclipse.swt.patch`, in `features/`) that redirects e4.rcp's exact requirements on
the SWT host + all platform fragments to re-qualified bundles. The bundles themselves (host + all
9 platform fragments, rebuilt at the bumped qualifier from committed binaries) are published by a
separate update-site in the SWT fork (`org.eclipse.swt.portfolio.bundles`) and consumed via the
target platform.

**Considered Options**

- *Own a copy of the RCP feature*: mirroring upstream RCP content would allow the product build to
  relax the SWT requirements directly, but the copied feature would have to stay synchronized with
  upstream on every platform upgrade. It also would not address exact requirements declared by the
  `org.eclipse.swt` host IU itself.
- *Assemble the feature patch into an update-site in the SWT fork*: not possible — Tycho cannot
  resolve a bare feature patch in a standalone p2 repository, because the patch's requirement on
  the patched feature is `greedy=false` and nothing pulls `org.eclipse.e4.rcp.feature.group` in as
  a root (eclipse-tycho/tycho#893; the old `deployableFeature` workaround was removed in Tycho 5).
- *Apply the feature patch in the product build* (chosen): the product lists `org.eclipse.e4.rcp`
  as a root, so the patch resolves naturally there. This is the idiomatic place for a feature
  patch and keeps the Portfolio-owned content to a single small patch feature.

**Consequences**

- Only one line couples the patch to the platform release: the `org.eclipse.e4.rcp` version in
  `features/org.eclipse.swt.patch/feature.xml`. Bump it on each platform upgrade.
- The SWT fork must republish the host + **all** platform fragments at the bumped qualifier (the
  host exact-pins every fragment at its own version), built with all target environments. Only the
  cocoa fragment carries the code change; the rest are upstream content re-qualified.
- The SWT fork update-site is added as an **available-but-unrooted** p2 repository in
  `portfolio-app/pom.xml` (`<repositories>`), **not** as a target-definition root. This is
  essential: rooting both upstream SWT and the patched SWT fork bundles in the planner-mode target
  would be a singleton conflict (two `org.eclipse.swt` bundles). As an unrooted pom repository the
  patched bundles are merely available, the target stays planner-consistent on the upstream SWT
  (with its transitive deps intact), and the feature patch selects the patched SWT bundles during
  the product build. Materialized products should contain only the patched SWT host and fragments.

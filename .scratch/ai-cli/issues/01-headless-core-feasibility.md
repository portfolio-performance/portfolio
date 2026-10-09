# Can PP core load, calculate and save a client file outside the workbench?

Type: research
Status: open

## Question

Can the `name.abuchen.portfolio` bundle be used from a plain JVM process (no Eclipse workbench, no SWT display) to load a client file via `ClientFactory`, compute calculated figures (snapshots, IRR, TTWROR, gains, currency conversion), apply domain changes, and save it back? Which dependencies (SWT, e4, Equinox, preferences, extension registry, `PortfolioPlugin` activator, PDF bundles) block that, and what is the minimal set needed: a plain classpath, an embedded headless OSGi launch, or an Eclipse headless `IApplication`?

## Comments

Research findings: branch `research/headless-core-feasibility`, file `.scratch/ai-cli/research/headless-core-feasibility.md`.

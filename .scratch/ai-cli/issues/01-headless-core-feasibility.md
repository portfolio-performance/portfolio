# Can PP core load, calculate and save a client file outside the workbench?

Type: research
Status: resolved

## Question

Can the `name.abuchen.portfolio` bundle be used from a plain JVM process (no Eclipse workbench, no SWT display) to load a client file via `ClientFactory`, compute calculated figures (snapshots, IRR, TTWROR, gains, currency conversion), apply domain changes, and save it back? Which dependencies (SWT, e4, Equinox, preferences, extension registry, `PortfolioPlugin` activator, PDF bundles) block that, and what is the minimal set needed: a plain classpath, an embedded headless OSGi launch, or an Eclipse headless `IApplication`?

## Comments

Research findings: branch `research/headless-core-feasibility`, file `.scratch/ai-cli/research/headless-core-feasibility.md`.

## Answer

- Yes: plain JVM classpath works; no OSGi launch, `IApplication`, workbench or SWT `Display` needed.
- Core has no `Bundle-Activator`; providers/feeds use `java.util.ServiceLoader`, not the extension registry.
- `snapshot`, `math`, `money` only touch Eclipse via `IProgressMonitor`, e4 DI annotations on `ExchangeRateProviderFactory`, and `Platform`/`FrameworkUtil` in `ECBExchangeRateProvider`, all harmless outside OSGi. `ClientFactory` load/save only calls `Platform.getOS()`.
- SWT, `equinox.common`, `core.runtime` and `osgi` jars must be on the classpath but need not run; PDFBox not needed.
- Use the built bundle jar (it carries `META-INF/services`), not `target/classes`.
- Caveats: `PortfolioLog` prints to stdout (must be redirected for JSON output); ECB rates fall back to built-in 2015 data unless `update()` fetches online; several online feeds and the login token store require OSGi.
- `name.abuchen.portfolio.tests` already loads XML/binary/encrypted files and computes snapshots/performance headlessly.
- Recommended minimal setup: bundle jar + target-platform jars; `new ExchangeRateProviderFactory(client)` → `CurrencyConverterImpl` → snapshot/performance APIs → `ClientFactory.save`. Embedded Equinox only if persisted ECB rates, online feeds or login token storage are needed.

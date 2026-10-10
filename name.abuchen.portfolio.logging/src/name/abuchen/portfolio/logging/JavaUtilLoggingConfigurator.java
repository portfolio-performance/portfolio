package name.abuchen.portfolio.logging;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Configures java.util.logging before the bundles that log through it start.
 * <p>
 * Apache Aries SpiFly writes one record per registered service provider - five, one per ImageIO
 * provider of the twelvemonkeys bundles - through java.util.logging at {@link Level#INFO}. Nothing
 * else in this application configures java.util.logging, so the JVM default applies: the root logger
 * accepts INFO and passes it to a ConsoleHandler writing to {@code System.err}. The records
 * therefore appeared on the standard error stream of every launch, desktop and command line alike,
 * and never reached the Eclipse log, which only receives what is logged through the OSGi log
 * service.
 * <p>
 * The records are written while the framework starts, before any application code runs, which is why
 * this is a bundle of its own: it has to be started ahead of SpiFly, and it can only do that by
 * having no dependency that would need to start first. Both product definitions give it start level
 * 1, one below SpiFly's 2. Changing either start level brings the records back - visibly, on stderr,
 * which is the failure mode this design prefers.
 */
public class JavaUtilLoggingConfigurator implements BundleActivator
{
    private static final String SPIFLY_LOGGER = "org.apache.aries.spifly"; //$NON-NLS-1$

    /**
     * The LogManager holds a logger only weakly, so a level set on a logger nobody else references
     * yet is lost when it is collected before its first use. This reference keeps the logger, and
     * with it the level, alive until SpiFly creates its own child logger below it.
     */
    private static Logger spiflyLogger; // NOSONAR

    @Override
    public void start(BundleContext context)
    {
        // an explicit configuration wins, so that -Djava.util.logging.config.file remains a way to
        // ask for the records back
        if (LogManager.getLogManager().getProperty(SPIFLY_LOGGER + ".level") != null) //$NON-NLS-1$
            return;

        spiflyLogger = Logger.getLogger(SPIFLY_LOGGER); // NOSONAR
        spiflyLogger.setLevel(Level.WARNING);
    }

    @Override
    public void stop(BundleContext context)
    {
        // nothing to undo: the configuration applies to a logger this bundle does not own, and it
        // has to outlive a stop for as long as SpiFly keeps logging
    }
}

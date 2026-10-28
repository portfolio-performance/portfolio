package name.abuchen.portfolio.cli.command;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.cli.file.ClientLoader;
import name.abuchen.portfolio.cli.file.FileFormat;
import name.abuchen.portfolio.cli.file.LoadedClient;
import name.abuchen.portfolio.cli.file.SafeWrite;
import name.abuchen.portfolio.model.Client;

/**
 * The client file a command works on, as a facet: <code>--file</code>, the
 * base of <code>meta</code> and the codes of a file that can't be read. Its
 * write form adds the safe-write options, meta, warnings and codes. A command
 * has at most one form of it.
 */
public final class ClientFile
{
    /** The file as read, and the date the command runs. */
    public record Read(LoadedClient loaded, LocalDate asOf)
    {
        public Client client()
        {
            return loaded.client();
        }

        public FileFormat format()
        {
            return loaded.format();
        }
    }

    /** Reads the client file; <code>meta</code> gets <code>fileVersion</code> and <code>asOf</code>. */
    public static final Facet<Read> READ = Facet.of(Declaration.empty().options(OptionSpec.FILE)
                    .meta("fileVersion", Schema.integer("Version of the file as read."))
                    .meta("asOf", Schema.string("ISO date the command ran, from the clock."))
                    .codes(ErrorCode.FILE_REQUIRED, ErrorCode.FILE_NOT_FOUND, ErrorCode.FILE_UNREADABLE,
                                    ErrorCode.FILE_VERSION_UNSUPPORTED, ErrorCode.PASSWORD_REQUIRED,
                                    ErrorCode.INCORRECT_PASSWORD)
                    .needingClient().in(Declaration.Slot.ENVELOPE),
                    run -> new Read(run.loaded(), LocalDate.now(run.clock())), (read, out) -> {
                        out.meta("fileVersion", read.client().getFileVersionAfterRead());
                        out.meta("asOf", read.asOf().toString());
                    });

    /**
     * The model the client file becomes, planned from the file opened at the
     * related path; see {@link Writer#replace}.
     */
    @FunctionalInterface
    public interface Replacement<R>
    {
        SafeWrite.Planned<R> plan(ClientLoader.OpenedFile related, Client loaded) throws CliException;
    }

    /**
     * Writes the client file through the safe-write sequence, once per
     * command: opens the file, runs the change and replaces the file, or with
     * <code>--dry-run</code> only previews it. The outcome becomes the write
     * meta and its warnings.
     */
    public static final class Writer
    {
        private final CommandContext run;
        private final SafeWrite.Settings settings;
        private final LocalDate asOf;
        private SafeWrite.Outcome<?> outcome;

        private Writer(CommandContext run, SafeWrite.Settings settings)
        {
            this.run = run;
            this.settings = settings;
            this.asOf = LocalDate.now(settings.clock());
        }

        /** Changes the loaded model and writes it; returns what the change returned. */
        public <R> R write(SafeWrite.Mutation<R> change) throws CliException
        {
            try (var opened = run.openClient())
            {
                return done(SafeWrite.run(opened, settings, change));
            }
        }

        /**
         * Writes another model than the loaded one. The file at
         * <code>related</code> is opened with the client file's password and
         * passes the version guard before the write starts.
         */
        public <R> R replace(Path related, Replacement<R> plan) throws CliException
        {
            try (var opened = run.openClient(); var source = opened.openRelated(related.toString()))
            {
                return done(SafeWrite.replace(opened, settings, loaded -> plan.plan(source, loaded)));
            }
        }

        private <R> R done(SafeWrite.Outcome<R> written)
        {
            if (outcome != null)
                throw new IllegalStateException("The client file was already written by this command");
            outcome = written;
            return written.result();
        }

        private void report(Facet.Report out)
        {
            if (outcome == null)
                throw new IllegalStateException("The command did not write the client file");
            out.meta("fileVersionBefore", outcome.fileVersionBefore());
            out.meta("fileVersionAfter", outcome.fileVersionAfter());
            out.meta("asOf", asOf.toString());
            out.meta("dryRun", outcome.dryRun());
            out.meta("backupPath", outcome.backup().toString());
            outcome.warnings().forEach(out::warn);
        }
    }

    private ClientFile()
    {
    }

    /**
     * Writes the client file; <code>meta</code> gets the file versions before
     * and after, <code>asOf</code>, <code>dryRun</code> and the backup.
     *
     * @param dryRunDescription
     *            the command's help for <code>--dry-run</code>
     */
    public static Facet<Writer> write(String dryRunDescription)
    {
        var dryRun = OptionSpec.flag("dry-run", dryRunDescription);
        var options = new ArrayList<OptionSpec>(List.of(OptionSpec.FILE, dryRun));
        options.addAll(SafeWrite.OPTIONS);

        var declaration = Declaration.empty().options(options.toArray(OptionSpec[]::new))
                        .meta("fileVersionBefore", Schema.integer("Version of the file as read."))
                        .meta("fileVersionAfter", Schema.integer("Version the file has after the write."))
                        .meta("asOf", Schema.string("ISO date the command ran, from the clock."))
                        .meta("dryRun", Schema.bool("True if nothing was written, backup included."))
                        .meta("backupPath", Schema.string(
                                        "Where the backup of the original file is, or with a dry run would be, written."))
                        .warnings(SafeWrite.WARNINGS.toArray(WarningSpec[]::new))
                        .codes(READ.declaration().codes().toArray(ErrorCode[]::new))
                        .codes(ErrorCode.CONSISTENCY_CHECK_FAILED, ErrorCode.FILE_OPEN_IN_APP, ErrorCode.FILE_LOCKED,
                                        ErrorCode.FILE_CHANGED, ErrorCode.WRITE_FAILED)
                        .sample("--" + dryRun.name()).in(Declaration.Slot.ENVELOPE);

        // the file is opened by the write itself, inside the safe-write sequence, not before the handler
        return Facet.of(declaration, run -> new Writer(run, SafeWrite.settings(run, run.flag(dryRun))),
                        (writer, out) -> writer.report(out));
    }
}

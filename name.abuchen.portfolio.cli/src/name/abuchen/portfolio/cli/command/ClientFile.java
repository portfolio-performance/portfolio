package name.abuchen.portfolio.cli.command;

import java.time.LocalDate;

import name.abuchen.portfolio.cli.file.FileFormat;
import name.abuchen.portfolio.cli.file.LoadedClient;
import name.abuchen.portfolio.model.Client;

/**
 * The client file a command works on, as a facet: <code>--file</code>, the
 * base of <code>meta</code> and the codes of a file that can't be read. A
 * command has at most one form of it.
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

    private ClientFile()
    {
    }
}

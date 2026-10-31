package name.abuchen.portfolio.cli.apply;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.file.SafeWrite;

/**
 * <code>pp-cli restore</code>: rolls the client file back to a backup through
 * the same safe-write sequence as <code>apply</code>, so the restore itself
 * leaves a backup and can be undone.
 */
public final class RestoreCommand
{
    private static final OptionSpec BACKUP_PATH = new OptionSpec("backup", "path",
                    "Backup to restore: the backupPath that apply or an earlier restore reported. There is no command that lists backups.")
                                    .asRequired();

    /** The absolute path of the backup of <code>--backup</code>, which exists. */
    static final Facet<Path> BACKUP = Facet.of(
                    Declaration.empty().options(BACKUP_PATH).codes(ErrorCode.BACKUP_NOT_FOUND),
                    RestoreCommand::backupFile);

    private static final Facet<ClientFile.Writer> WRITE = ClientFile.write(
                    "Check the backup and report the would-be backup of the current file without writing any file, backup or lock.");

    private static final Schema DATA = Schema.object("The outcome of the restore.").property("restoredFrom",
                    Schema.string("Absolute path of the backup the client file was restored from."));

    public static final CommandSpec SPEC = CommandSpec.of("restore",
                    "Rolls the client file back to a backup, as written by apply or an earlier restore. Follows the same safe-write sequence as apply, including a backup of the file it replaces, reported in meta.backupPath, so a restore can itself be undone. Refused, with nothing changed, if the backup is missing, unreadable or has a newer file version than this pp-cli supports, if it would introduce a consistency issue that the backup did not already have, or if the file changed on disk since it was read. The file keeps the format of the backup.",
                    DATA, RestoreCommand::execute, BACKUP, WRITE);

    private RestoreCommand()
    {
    }

    private static Object execute(CommandContext run) throws CliException
    {
        var backup = run.get(BACKUP);

        var data = new LinkedHashMap<String, Object>();
        data.put("restoredFrom", backup.toString());

        return run.get(WRITE).replace(backup,
                        (source, loaded) -> new SafeWrite.Planned<>(source.load().client(), data, source.file()));
    }

    private static Path backupFile(CommandContext context) throws CliException
    {
        var raw = context.text(BACKUP_PATH).orElseThrow();
        try
        {
            if (raw.isBlank())
                throw BACKUP_PATH.invalid(raw, "the path of a backup file");

            var path = Path.of(raw).toAbsolutePath();
            if (!Files.isRegularFile(path))
                throw CliException.failure(ErrorCode.BACKUP_NOT_FOUND, "Backup not found",
                                "No backup file at " + path, 404).with("backup", path.toString());
            return path;
        }
        catch (InvalidPathException e)
        {
            throw BACKUP_PATH.invalid(raw, "a valid file path");
        }
    }
}

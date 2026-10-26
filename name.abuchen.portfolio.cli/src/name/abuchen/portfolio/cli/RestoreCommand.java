package name.abuchen.portfolio.cli;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * <code>pp-cli restore</code>: rolls the client file back to a backup through
 * the same safe-write sequence as <code>apply</code>, so the restore itself
 * leaves a backup and can be undone.
 */
final class RestoreCommand
{
    private static final OptionSpec BACKUP = new OptionSpec("backup", "path",
                    "Backup to restore: the backupPath that apply or an earlier restore reported. There is no command that lists backups.")
                                    .asRequired();
    private static final OptionSpec DRY_RUN = OptionSpec.flag("dry-run",
                    "Check the backup and report the would-be backup of the current file without writing any file, backup or lock.");

    private static final Schema DATA = Schema.object("The outcome of the restore.").property("restoredFrom",
                    Schema.string("Absolute path of the backup the client file was restored from."));

    static final CommandSpec SPEC = new CommandSpec("restore",
                    "Rolls the client file back to a backup, as written by apply or an earlier restore. Follows the same safe-write sequence as apply, including a backup of the file it replaces, reported in meta.backupPath, so a restore can itself be undone. Refused, with nothing changed, if the backup is missing, unreadable or has a newer file version than this pp-cli supports, if it would introduce a consistency issue that the backup did not already have, or if the file changed on disk since it was read. The file keeps the format of the backup.",
                    true, restoreOptions(), DATA, ResponseMeta.writeSchema(), SafeWrite.WARNINGS,
                    RestoreCommand::execute);

    private RestoreCommand()
    {
    }

    private static List<OptionSpec> restoreOptions()
    {
        var options = new ArrayList<>(List.of(BACKUP, DRY_RUN));
        options.addAll(SafeWrite.OPTIONS);
        return options;
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var backup = backupFile(context);
        var dryRun = context.flag(DRY_RUN);

        try (var opened = context.openClient(); var source = opened.openRelated(backup.toString()))
        {
            var data = new LinkedHashMap<String, Object>();
            data.put("restoredFrom", backup.toString());

            var outcome = SafeWrite.replace(opened, SafeWrite.settings(context, dryRun),
                            loaded -> new SafeWrite.Planned<>(source.load().client(), data, source.file()));

            var response = Response.of(data, ResponseMeta.written(context, outcome));
            outcome.warnings().forEach(response::warn);
            return response;
        }
    }

    private static Path backupFile(CommandContext context) throws CliException
    {
        var raw = context.text(BACKUP).orElseThrow();
        try
        {
            if (raw.isBlank())
                throw BACKUP.invalid(raw, "the path of a backup file");

            var path = Path.of(raw).toAbsolutePath();
            if (!Files.isRegularFile(path))
                throw CliException.failure("backup-not-found", "Backup not found", "No backup file at " + path, 404)
                                .with("backup", path.toString());
            return path;
        }
        catch (InvalidPathException e)
        {
            throw BACKUP.invalid(raw, "a valid file path");
        }
    }
}

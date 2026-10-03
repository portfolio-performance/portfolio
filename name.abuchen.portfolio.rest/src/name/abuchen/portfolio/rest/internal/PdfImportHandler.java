package name.abuchen.portfolio.rest.internal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFImportAssistant;
import name.abuchen.portfolio.model.Client;

/**
 * The first step of a PDF import: runs the application's PDF extractors on
 * the given files, as the PDF import wizard does ({@link PDFImportAssistant}:
 * the first extractor that recognizes a document wins), and keeps the
 * extracted items for the commit.
 * <p/>
 * The request body is {@code {paths: [absolute path of a .pdf file],
 * targets?}}; {@code targets} (see {@link ImportContext}) only steer the
 * checks of the preview - the commit names its own.
 */
public final class PdfImportHandler
{
    /* package */ static final int MAX_FILES = 100;

    /** runs the extractors on the files and reports unreadable files in {@code errors} */
    @FunctionalInterface
    /* package */ interface Extraction
    {
        List<Extractor.Item> extract(Client client, List<File> files, Map<File, List<Exception>> errors);
    }

    /**
     * The extraction in use. A test seam: the test fragment has no real PDF
     * documents, only the text the extractor tests work on, and replaces this
     * with an extraction from text.
     */
    /* package */ static Extraction extraction = PdfImportHandler::extractWithAssistant;

    private PdfImportHandler()
    {
    }

    /** the validated files of the request body; 422 for missing, non-PDF or unreadable paths */
    public static List<File> files(JsonObject body)
    {
        var json = new Json(body);
        json.ignore("paths", "targets"); //$NON-NLS-1$ //$NON-NLS-2$
        json.rejectUnknownFields();

        var files = new ArrayList<File>();
        var element = body.get("paths"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
        {
            json.add(new ApiException.FieldError("paths", "required", "paths is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        else if (!element.isJsonArray() || element.getAsJsonArray().isEmpty())
        {
            json.add(new ApiException.FieldError("paths", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "paths must be a non-empty array of file paths")); //$NON-NLS-1$
        }
        else if (element.getAsJsonArray().size() > MAX_FILES)
        {
            json.add(new ApiException.FieldError("paths", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("at most {0} files per import", MAX_FILES))); //$NON-NLS-1$
        }
        else
        {
            var array = element.getAsJsonArray();
            for (var ii = 0; ii < array.size(); ii++)
            {
                var file = file(json, "paths[" + ii + "]", array.get(ii), ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                if (file != null)
                    files.add(file);
            }
        }

        json.throwIfErrors();
        return files;
    }

    /**
     * The file a path of the request names: an absolute path of a readable
     * file, with the given extension unless that is null; null (and an error
     * in {@code json}) otherwise.
     */
    /* package */ static File file(Json json, String field, JsonElement element, String extension)
    {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            json.add(new ApiException.FieldError(field, "invalid-type", "a path must be a string")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        var text = element.getAsString();
        Path path;
        try
        {
            path = Path.of(text);
        }
        catch (InvalidPathException e)
        {
            json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} is not a valid path", text))); //$NON-NLS-1$
            return null;
        }

        if (!path.isAbsolute())
        {
            json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} is not an absolute path", text))); //$NON-NLS-1$
            return null;
        }

        var name = path.getFileName() == null ? "" : path.getFileName().toString(); //$NON-NLS-1$
        if (extension != null && !name.toLowerCase(Locale.ROOT).endsWith(extension))
        {
            json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} is not a {1} file", text, extension))); //$NON-NLS-1$
            return null;
        }

        if (!Files.isRegularFile(path) || !Files.isReadable(path))
        {
            json.add(new ApiException.FieldError(field, "file-not-found", //$NON-NLS-1$
                            MessageFormat.format("{0} does not exist or cannot be read", text))); //$NON-NLS-1$
            return null;
        }

        return path.toFile();
    }

    /**
     * Extracts the items; runs on the HTTP worker thread (it reads only the
     * file's instruments, the trade-off the calculation endpoints make too).
     * Files no extractor recognizes are reported in {@code errors}.
     */
    public static List<Extractor.Item> extract(Client client, List<File> files, Map<File, List<Exception>> errors)
    {
        return extraction.extract(client, files, errors);
    }

    private static List<Extractor.Item> extractWithAssistant(Client client, List<File> files,
                    Map<File, List<Exception>> errors)
    {
        var byExtractor = new PDFImportAssistant(client, files).run(new NullProgressMonitor(), errors);

        // a stable order: by extractor, then as extracted
        var items = new ArrayList<Extractor.Item>();
        byExtractor.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getLabel()))
                        .forEach(e -> items.addAll(e.getValue()));
        return items;
    }

    /**
     * The files that could not be read or recognized, one entry per file:
     * {@code [{path, message, details?}]}. Every extractor reports every file
     * it does not recognize (twice, once per PDF library), so the "not a
     * supported document" messages are dropped; {@code details} keeps the
     * distinct messages of the extractors that recognized the bank but not
     * the document, and of read errors.
     */
    public static JsonArray errors(Map<File, List<Exception>> errors)
    {
        var array = new JsonArray();
        errors.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var file = entry.getKey();
            // "{1}: File ''{0}'' is not a supported document" without the bank label
            var notSupported = MessageFormat.format(name.abuchen.portfolio.Messages.PDFMsgFileNotSupported,
                            file.getName(), ""); //$NON-NLS-1$

            var details = new LinkedHashSet<String>();
            for (var exception : entry.getValue())
            {
                var message = exception.getMessage() != null ? exception.getMessage()
                                : exception.getClass().getSimpleName();
                if (!message.endsWith(notSupported))
                    details.add(message);
            }

            var json = new JsonObject();
            json.addProperty("path", file.getAbsolutePath()); //$NON-NLS-1$
            json.addProperty("message", details.size() == 1 ? details.iterator().next() //$NON-NLS-1$
                            : "no extractor recognized the document"); //$NON-NLS-1$
            if (details.size() > 1)
            {
                var list = new JsonArray();
                details.forEach(list::add);
                json.add("details", list); //$NON-NLS-1$
            }
            array.add(json);
        });
        return array;
    }
}

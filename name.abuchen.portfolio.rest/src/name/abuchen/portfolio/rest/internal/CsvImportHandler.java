package name.abuchen.portfolio.rest.internal;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.csv.CSVConfig;
import name.abuchen.portfolio.datatransfer.csv.CSVExtractor;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * The first step of a CSV import: reads a CSV file with an explicit column
 * mapping, as the application's CSV import wizard does after its definition
 * page. The request body is {@code {path, config, targets?}}; {@code config}
 * is the JSON the wizard saves as a CSV configuration
 * ({@link CSVConfig#fromJSON}): {@code {label?, target, delimiter, encoding?,
 * skipLines, isFirstLineHeader, columns: [{label, field?, format?}]}} with
 * {@code target} one of {@code account-transaction},
 * {@code portfolio-transaction}, {@code investment-vehicle},
 * {@code investment-vehicle-price}, {@code portfolio}. The rest of the
 * import - preview items, commit - is the one of the PDF import.
 * <p/>
 * A historical price import yields one {@code price} item per line; the
 * commit adds them to the instrument named by {@code targets.instrument}.
 */
public final class CsvImportHandler
{
    private static final String PRICE_EXTRACTOR = "investment-vehicle-price"; //$NON-NLS-1$
    private static final Pattern LINE_NUMBER = Pattern.compile("\\d[\\d.,'\\u00A0\\u202F]*"); //$NON-NLS-1$

    /**
     * One historical price of a price import. Unlike the extractor's own
     * price item, its instrument is chosen at commit.
     */
    public static final class PriceItem extends Extractor.Item
    {
        private final SecurityPrice price;
        private Security security;

        public PriceItem(SecurityPrice price)
        {
            this.price = price;
        }

        public SecurityPrice getPrice()
        {
            return price;
        }

        @Override
        public Annotated getSubject()
        {
            return security;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public void setSecurity(Security security)
        {
            this.security = security;
        }

        @Override
        public String getTypeInformation()
        {
            return "price"; //$NON-NLS-1$
        }

        @Override
        public LocalDateTime getDate()
        {
            return price.getDate().atStartOfDay();
        }

        @Override
        public void setNote(String note)
        {
            // prices have no notes
        }

        @Override
        public ImportAction.Status apply(ImportAction action, ImportAction.Context context)
        {
            return action.process(security, price);
        }
    }

    private CsvImportHandler()
    {
    }

    /**
     * Reads the CSV file of the request body into import items; 422 for an
     * invalid path or configuration and for lines that cannot be read
     * ({@code csv-parse-error}, one error per line). Runs on the HTTP worker
     * thread.
     */
    public static List<Extractor.Item> extract(Client client, JsonObject body)
    {
        var json = new Json(body);
        json.ignore("path", "config", "targets"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        json.rejectUnknownFields();

        File file = null;
        var path = body.get("path"); //$NON-NLS-1$
        if (path == null || path.isJsonNull())
            json.add(new ApiException.FieldError("path", "required", "path is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        else
            file = PdfImportHandler.file(json, "path", path, null); //$NON-NLS-1$

        var importer = file != null ? new CSVImporter(client, file) : new CSVImporter(client, new File("")); //$NON-NLS-1$
        var config = config(json, body.get("config"), importer); //$NON-NLS-1$
        json.throwIfErrors();

        config.writeTo(importer);

        try
        {
            importer.processFile(false);
        }
        catch (IOException e)
        {
            throw ApiException.validation(List.of(new ApiException.FieldError("path", "file-not-found", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("{0} cannot be read: {1}", file, e.getMessage())))); //$NON-NLS-1$
        }

        var errors = new ArrayList<Exception>();
        var items = importer.createItems(errors);

        if (!errors.isEmpty())
        {
            var fieldErrors = new ArrayList<ApiException.FieldError>();
            for (var error : errors)
            {
                var message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                fieldErrors.add(new ApiException.FieldError(lineField(message), "csv-parse-error", message)); //$NON-NLS-1$
            }
            throw ApiException.validation(fieldErrors);
        }

        if (PRICE_EXTRACTOR.equals(importer.getExtractor().getCode()))
            return priceItems(items);

        return items;
    }

    /** the configuration; problems are recorded as errors of {@code config[.<field>]} */
    private static CSVConfig config(Json json, JsonElement element, CSVImporter importer)
    {
        var config = new CSVConfig();

        if (element == null || element.isJsonNull())
        {
            json.add(new ApiException.FieldError("config", "required", "config is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return config;
        }
        if (!element.isJsonObject())
        {
            json.add(new ApiException.FieldError("config", "invalid-type", "config must be an object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return config;
        }

        var object = element.getAsJsonObject().deepCopy();
        // the application requires a label for its saved configurations only
        if (!object.has("label")) //$NON-NLS-1$
            object.addProperty("label", "REST API"); //$NON-NLS-1$ //$NON-NLS-2$
        // do not depend on the platform's default encoding
        if (!object.has("encoding")) //$NON-NLS-1$
            object.addProperty("encoding", "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

        var errorCount = json.errors().size();
        validate(json, object, importer);
        if (json.errors().size() > errorCount)
            return config;

        try
        {
            config.fromJSON((JSONObject) new JSONParser().parse(object.toString()));
        }
        catch (ParseException | RuntimeException e)
        {
            json.add(new ApiException.FieldError("config", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("config is not a valid CSV configuration: {0}", e.getMessage()))); //$NON-NLS-1$
        }
        return config;
    }

    /** the checks {@link CSVConfig#fromJSON} and {@link CSVConfig#writeTo} would skip silently */
    private static void validate(Json json, JsonObject config, CSVImporter importer)
    {
        var target = config.get("target"); //$NON-NLS-1$
        var extractor = target != null && target.isJsonPrimitive()
                        ? importer.getExtractorByCode(target.getAsString())
                        : Optional.<CSVExtractor>empty();
        if (extractor.isEmpty())
            json.add(new ApiException.FieldError("config.target", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "target must be account-transaction, portfolio-transaction, investment-vehicle, " //$NON-NLS-1$
                                            + "investment-vehicle-price or portfolio")); //$NON-NLS-1$

        var delimiter = config.get("delimiter"); //$NON-NLS-1$
        if (delimiter == null || !delimiter.isJsonPrimitive() || delimiter.getAsString().length() != 1
                        || ",;\t".indexOf(delimiter.getAsString().charAt(0)) < 0) //$NON-NLS-1$
            json.add(new ApiException.FieldError("config.delimiter", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "delimiter must be one of , ; or a tab")); //$NON-NLS-1$

        var skipLines = config.get("skipLines"); //$NON-NLS-1$
        if (skipLines == null || !skipLines.isJsonPrimitive() || !skipLines.getAsJsonPrimitive().isNumber()
                        || !skipLines.getAsString().matches("\\d+")) //$NON-NLS-1$
            json.add(new ApiException.FieldError("config.skipLines", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "skipLines must be a number of lines, 0 or more")); //$NON-NLS-1$

        var header = config.get("isFirstLineHeader"); //$NON-NLS-1$
        if (header == null || !header.isJsonPrimitive() || !header.getAsJsonPrimitive().isBoolean())
            json.add(new ApiException.FieldError("config.isFirstLineHeader", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                            "isFirstLineHeader must be true or false")); //$NON-NLS-1$

        var columns = config.get("columns"); //$NON-NLS-1$
        if (columns == null || !columns.isJsonArray() || columns.getAsJsonArray().isEmpty())
        {
            json.add(new ApiException.FieldError("config.columns", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "columns must be a non-empty array of {label, field?, format?}")); //$NON-NLS-1$
            return;
        }

        var array = columns.getAsJsonArray();
        for (var ii = 0; ii < array.size(); ii++)
        {
            var field = "config.columns[" + ii + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            var column = array.get(ii);
            if (!column.isJsonObject() || !column.getAsJsonObject().has("label")) //$NON-NLS-1$
            {
                json.add(new ApiException.FieldError(field, "invalid-value", "a column is {label, field?, format?}")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var code = column.getAsJsonObject().get("field"); //$NON-NLS-1$
            if (code != null && !code.isJsonNull() && extractor.isPresent() && extractor.get().getFields().stream()
                            .noneMatch(f -> f.getCode().equals(code.getAsString())))
                json.add(new ApiException.FieldError(field + ".field", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("{0} is not a field of {1}", code.getAsString(), //$NON-NLS-1$
                                                extractor.get().getCode())));
        }
    }

    /** {@code line[N]} for a message of the form "Line N: ..." (in any language), else {@code line} */
    private static String lineField(String message)
    {
        var matcher = LINE_NUMBER.matcher(message);
        if (matcher.find())
        {
            var digits = matcher.group().replaceAll("\\D", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (!digits.isEmpty())
                return "line[" + digits + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return "line"; //$NON-NLS-1$
    }

    /** the price extractor yields one instrument holding all prices: one item per price instead */
    private static List<Extractor.Item> priceItems(List<Extractor.Item> items)
    {
        var result = new ArrayList<Extractor.Item>();
        for (var item : items)
        {
            if (item.getSecurity() != null)
                item.getSecurity().getPrices().forEach(price -> result.add(new PriceItem(price)));
        }
        return result;
    }
}

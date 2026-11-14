package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.util.Interval;

/**
 * The overall performance report for one class of one taxonomy, in the same
 * terms as {@code pp_get_overall_performance}: the class is what the report
 * covers instead of the whole file.
 *
 * <p>
 * What a class holds is not only its vehicles: a cash account assigned to a
 * class of the taxonomy counts, and the settlement cash of a securities account
 * is shared out between the classes its holdings belong to. That apportioning is
 * the application's own - the same filter its classification report on screen is
 * built from - so the money of a class here is the money the user sees for it in
 * the table, and the classes of one tree therefore add back up to the whole
 * file.
 */
public final class CategoryPerformanceHandler
{
    private CategoryPerformanceHandler()
    {
    }

    /**
     * @param taxonomyParam
     *            the id of the taxonomy the class belongs to; optional, because a
     *            class id is unique within one and is looked for in all of them -
     *            only refused when several taxonomies define that same id
     */
    public static JsonObject get(Client client, ExchangeRateProviderFactory factory, String taxonomyParam,
                    String classParam, String openingDateParam, String closingDateParam, String currencyParam,
                    String costMethodParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var openingDate = CalcParams.openingDate(openingDateParam, errors);
        var closingDate = CalcParams.closingDate(closingDateParam, errors);
        var currency = CalcParams.reportingCurrency(client, currencyParam, errors);
        var costMethod = CalcParams.costMethod(costMethodParam, errors);
        CalcParams.requireRange(openingDate, closingDate, errors);

        var wanted = taxonomyParam == null || taxonomyParam.isBlank() ? null
                        : TaxonomiesHandler.resolve(taxonomyParam, client, errors);
        var classification = classParam == null || classParam.isBlank() ? null
                        : TaxonomiesHandler.resolveClass(classParam, wanted, client, errors);

        if (classParam == null || classParam.isBlank())
            errors.add(new ApiException.FieldError("class", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "the id of the class to report on"));

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var conversions = new ConversionRecorder(factory);
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, currency));
        var interval = Interval.of(openingDate, closingDate);

        var performance = new ClientPerformanceSnapshot(new ClientClassificationFilter(classification).filter(client),
                        converter, interval, costMethod.useFifo());

        // TTWROR comes from a second engine; as in the file-wide report, a period
        // that lies ahead or held nothing has no return to measure, and null says
        // so rather than 0 %
        var measurable = CalcParams.hasAPast(openingDate);
        var index = measurable ? PerformanceIndex.forClassification(client, converter, classification, interval,
                        new ArrayList<>()) : null;
        if (index != null && !PerformanceHandler.heldAnything(index))
            measurable = false;

        var ttwror = measurable ? index.getFinalAccumulatedPercentage() : Double.NaN;
        var irr = measurable ? performance.getPerformanceIRR() : Double.NaN;

        var report = EntityJson.performance(openingDate, closingDate, currency, costMethod, ttwror, irr, performance);
        report.add("category", categoryJson(classification, ownerOf(classification, client))); //$NON-NLS-1$
        FxWarnings.attach(report, conversions);

        return report;
    }

    /** which class the report is about, named rather than only identified */
    private static JsonObject categoryJson(Classification classification, Taxonomy taxonomy)
    {
        var json = new JsonObject();

        // a taxonomy is identified by an id, not a uuid - the two are different
        // words in this file and the reader looks the class up by both of them
        var tree = new JsonObject();
        tree.addProperty("id", taxonomy.getId()); //$NON-NLS-1$
        tree.addProperty("name", taxonomy.getName()); //$NON-NLS-1$
        json.add("taxonomy", tree); //$NON-NLS-1$

        json.addProperty("id", classification.getId()); //$NON-NLS-1$
        json.addProperty("name", classification.getName()); //$NON-NLS-1$

        var parent = classification.getParent();
        if (parent != null)
            json.addProperty("parentId", parent.getId()); //$NON-NLS-1$
        json.addProperty("assignedCount", classification.getAssignments().size()); //$NON-NLS-1$

        return json;
    }

    /** the taxonomy a class came out of; a class names its parent, never the tree above it */
    private static Taxonomy ownerOf(Classification classification, Client client)
    {
        for (var taxonomy : client.getTaxonomies())
            if (classification == taxonomy.getClassificationById(classification.getId()))
                return taxonomy;

        // unreachable: the class was resolved out of one of these taxonomies
        throw new IllegalStateException("class of no taxonomy: " + classification.getId()); //$NON-NLS-1$
    }
}

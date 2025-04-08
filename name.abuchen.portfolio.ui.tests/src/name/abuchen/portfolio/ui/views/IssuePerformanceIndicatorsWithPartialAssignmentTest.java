package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.Element;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.ElementValueProvider;
import name.abuchen.portfolio.util.Interval;

/**
 * Problem: if the statement of assets is grouped by classification and one
 * security is partially assigned to multiple classifications, the performance
 * indicators also must show partial numbers (e.g. capital gains).
 * <p>
 * The test file contains two taxonomies - one where the security is assigned to
 * one classification, another where the security is assigned to three
 * classifications.
 * <p>
 * See
 * https://www.wertpapier-forum.de/topic/38306-portfolio-performance-mein-neues-programm/?page=122#comment-1149392
 */
public class IssuePerformanceIndicatorsWithPartialAssignmentTest
{
    private static Client CLIENT;

    private static Interval REPORTING_PERIOD = Interval.of(LocalDate.parse("2017-07-14"), //$NON-NLS-1$
                    LocalDate.parse("2018-07-14")); //$NON-NLS-1$

    private static ClientSnapshot SNAPSHOT;

    private static Map<Security, LazySecurityPerformanceRecord> SECURITY2RECORD;

    private Function<Stream<Object>, Object> sum = objects -> objects.map(e -> (Money) e)
                    .collect(MoneyCollectors.sum(SNAPSHOT.getCurrencyConverter().getTermCurrency()));

    private ElementValueProvider function_delta = new ElementValueProvider( //
                    LazySecurityPerformanceRecord::getDelta, sum);
    private ElementValueProvider function_capital_gains = new ElementValueProvider(
                    LazySecurityPerformanceRecord::getCapitalGainsOnHoldings, sum);
    private ElementValueProvider function_capical_gains_percent = new ElementValueProvider(
                    LazySecurityPerformanceRecord::getCapitalGainsOnHoldingsPercent, null);

    @BeforeClass
    public static void setupClient() throws IOException
    {
        CLIENT = ClientFactory.load(IssuePerformanceIndicatorsWithPartialAssignmentTest.class
                        .getResourceAsStream("IssuePerformanceIndicatorsWithPartialAssignment.xml")); //$NON-NLS-1$

        TestCurrencyConverter converter = new TestCurrencyConverter();

        SNAPSHOT = ClientSnapshot.create(CLIENT, converter, LocalDate.parse("2018-07-14")); //$NON-NLS-1$

        var sps = LazySecurityPerformanceSnapshot.create(CLIENT, converter, REPORTING_PERIOD);

        SECURITY2RECORD = sps.getRecords().stream()
                        .collect(Collectors.toMap(LazySecurityPerformanceRecord::getSecurity, r -> r));
    }

    @Test
    public void testPartialAssignments()
    {
        // taxonomy - regions

        Taxonomy taxonomy = CLIENT.getTaxonomy("f8ffb083-774d-499e-a983-5c214eff7297"); //$NON-NLS-1$

        StatementOfAssetsViewer.Model model = new StatementOfAssetsViewer.Model(new PreferenceStore(), CLIENT,
                        ClientFilter.NO_FILTER, SNAPSHOT.getCurrencyConverter(), SNAPSHOT.getTime(), taxonomy);

        model.getElements().stream().map(Element.class::cast).filter(Element::isSecurity).forEach(e -> e
                        .setPerformance(CurrencyUnit.EUR, REPORTING_PERIOD, SECURITY2RECORD.get(e.getSecurity())));

        // check that element has only partial values

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isGroupByTaxonomy).findFirst()
                        .orElseThrow(IllegalArgumentException::new), 262.26, 355.26, 262.26, null);

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isCategory)
                        .filter(e -> e.getCategory().getClassification().getName().equals("Europa")).findFirst() //$NON-NLS-1$
                        .orElseThrow(IllegalArgumentException::new), 62.94, 85.26, 62.94, null);

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isSecurity).filter(
                        e -> e.getValuation().equals(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1156.40))))
                        .findFirst().orElseThrow(IllegalArgumentException::new), 62.94, 85.26, 62.94, 0.0576);
    }

    @Test
    public void testFullAssignments()
    {
        // taxonomy - regions

        Taxonomy taxonomy = CLIENT.getTaxonomy("21baca92-db77-41f2-96d4-64e31ff4b4f5"); //$NON-NLS-1$

        StatementOfAssetsViewer.Model model = new StatementOfAssetsViewer.Model(new PreferenceStore(), CLIENT,
                        ClientFilter.NO_FILTER, SNAPSHOT.getCurrencyConverter(), SNAPSHOT.getTime(), taxonomy);

        model.getElements().stream().map(Element.class::cast).filter(Element::isSecurity).forEach(e -> e
                        .setPerformance(CurrencyUnit.EUR, REPORTING_PERIOD, SECURITY2RECORD.get(e.getSecurity())));

        // check that element has full values

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isGroupByTaxonomy).findFirst()
                        .orElseThrow(IllegalArgumentException::new), 262.26, 355.26, 262.26, null);

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isCategory)
                        .filter(e -> e.getCategory().getClassification().getName().equals("Welt")).findFirst() //$NON-NLS-1$
                        .orElseThrow(IllegalArgumentException::new), 262.26, 355.26, 262.26, null);

        checkValue(model.getElements().stream().map(Element.class::cast).filter(Element::isSecurity).findFirst()
                        .orElseThrow(IllegalArgumentException::new), 262.26, 355.26, 262.26, 0.0576);
    }

    private void checkValue(Element element, double profitLoss, double delta, double capital_gains,
                    Double capital_gains_percent)
    {

        // includes dividend payments
        assertThat((Money) function_delta.getValue(element, CurrencyUnit.EUR, REPORTING_PERIOD),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(delta))));

        assertThat((Money) function_capital_gains.getValue(element, CurrencyUnit.EUR, REPORTING_PERIOD),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(capital_gains))));

        if (capital_gains_percent == null)
        {
            assertThat((Double) function_capical_gains_percent.getValue(element, CurrencyUnit.EUR, REPORTING_PERIOD),
                            nullValue());
        }
        else
        {
            assertThat((Double) function_capical_gains_percent.getValue(element, CurrencyUnit.EUR, REPORTING_PERIOD),
                            closeTo(capital_gains_percent, 0.0001));
        }
    }
}

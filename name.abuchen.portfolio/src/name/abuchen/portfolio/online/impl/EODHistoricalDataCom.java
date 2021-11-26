package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.WebAccess;

public class EODHistoricalDataCom
{
    public static class Fundamentals
    {
        private List<Pair<String, Double>> sectorWeights;
        private List<Pair<String, Double>> worldRegions;

        /* package */ static Fundamentals from(String jsonBlob)
        {
            JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonBlob);

            boolean isETFData = jsonObject.containsKey("ETF_Data"); //$NON-NLS-1$
            boolean isMutualFund = jsonObject.containsKey("MutualFund_Data"); //$NON-NLS-1$

            Fundamentals fundamentals = new Fundamentals();

            if (isETFData)
            {
                JSONObject etfData = (JSONObject) jsonObject.get("ETF_Data"); //$NON-NLS-1$
                fundamentals.sectorWeights = parseETF((JSONObject) etfData.get("Sector_Weights")); //$NON-NLS-1$
                fundamentals.worldRegions = parseETF((JSONObject) etfData.get("World_Regions")); //$NON-NLS-1$
            }
            else if (isMutualFund)
            {
                JSONObject etfData = (JSONObject) jsonObject.get("MutualFund_Data"); //$NON-NLS-1$
                fundamentals.sectorWeights = parseFundSectors((JSONObject) etfData.get("Sector_Weights")); //$NON-NLS-1$
                fundamentals.worldRegions = parseFundRegions((JSONObject) etfData.get("World_Regions")); //$NON-NLS-1$
            }
            else
            {
                fundamentals.sectorWeights = new ArrayList<>();
                fundamentals.worldRegions = new ArrayList<>();

                // check if "General" sections contains some info
                JSONObject etfData = (JSONObject) jsonObject.get("General"); //$NON-NLS-1$
                String gicSector = etfData != null ? (String) etfData.get("GicSector") : null; //$NON-NLS-1$
                if (gicSector != null)
                    fundamentals.sectorWeights.add(new Pair<>(gicSector, 100d));

            }

            return fundamentals;
        }

        private static List<Pair<String, Double>> parseETF(JSONObject jsonObject)
        {
            if (jsonObject == null)
                return Collections.emptyList();

            List<Pair<String, Double>> answer = new ArrayList<>();

            for (Object key : jsonObject.keySet()) // NOSONAR
            {
                JSONObject json = (JSONObject) jsonObject.get(key);
                String percentage = (String) json.get("Equity_%"); //$NON-NLS-1$

                if (percentage != null)
                    answer.add(new Pair<>(key.toString(), Double.parseDouble(percentage)));
            }

            return answer;
        }

        private static List<Pair<String, Double>> parseFundSectors(JSONObject jsonObject)
        {
            if (jsonObject == null)
                return Collections.emptyList();

            List<Pair<String, Double>> answer = new ArrayList<>();

            for (Object group : jsonObject.keySet()) // NOSONAR
            {
                JSONObject json = (JSONObject) jsonObject.get(group);

                for (Object key : json.keySet()) // NOSONAR
                {
                    JSONObject sector = (JSONObject) json.get(key);

                    if (sector == null)
                        continue;

                    String name = (String) sector.get("Type"); //$NON-NLS-1$
                    String percentage = (String) sector.get("Amount_%"); //$NON-NLS-1$
                    if (percentage != null && name != null)
                        answer.add(new Pair<>(name, Double.parseDouble(percentage)));
                }
            }

            return answer;
        }

        private static List<Pair<String, Double>> parseFundRegions(JSONObject jsonObject)
        {
            if (jsonObject == null)
                return Collections.emptyList();

            List<Pair<String, Double>> answer = new ArrayList<>();

            for (Object group : jsonObject.keySet()) // NOSONAR
            {
                // not sure why the market classification (developed vs.
                // emerging markets) comes up as world region, but let's ignore
                // that
                if ("Market Classification".equals(group)) //$NON-NLS-1$
                    continue;

                JSONObject json = (JSONObject) jsonObject.get(group);

                for (Object key : json.keySet()) // NOSONAR
                {
                    JSONObject region = (JSONObject) json.get(key);

                    String name = (String) region.get("Name"); //$NON-NLS-1$
                    String percentage = (String) region.get("Stocks_%"); //$NON-NLS-1$
                    if (percentage != null && name != null)
                        answer.add(new Pair<>(group + ";" + name, Double.parseDouble(percentage))); //$NON-NLS-1$
                }
            }

            return answer;
        }

        public boolean updateCountryAllocation(Taxonomy taxonomy, Security security)
        {
            return updateAllocation(worldRegions, "com.historicaldata.worldregions", s -> s, taxonomy, security); //$NON-NLS-1$
        }

        public boolean updateSectorAllocation(Taxonomy taxonomy, Security security)
        {
            @SuppressWarnings("nls")
            UnaryOperator<String> keyMapper = s -> {
                switch (s)
                {
                    case "Basic Materials":
                        return "MATERIALS";
                    case "Consumer Cyclicals":
                        return "CONSUMER_STAPLES";
                    case "Consumer Defensive":
                        return "CONSUMER_DISCRETIONARY";
                    case "Financial Services":
                        return "FINANCIALS";
                    case "Healthcare":
                        return "HEALTH_CARE";
                    case "Technology":
                        return "INFORMATION_TECHNOLOGY";
                    default:

                        return s.toUpperCase(Locale.US).replace(' ', '_');
                }
            };

            return updateAllocation(sectorWeights, "GICS-sector", keyMapper, taxonomy, security); //$NON-NLS-1$
        }

        private boolean updateAllocation(List<Pair<String, Double>> data, String externalId,
                        UnaryOperator<String> keyMapper, Taxonomy taxonomy, Security security)
        {
            boolean isDirty = false;

            Map<String, Classification> id2classification = taxonomy.getAllClassifications().stream()
                            .filter(c -> c.getData(externalId) != null)
                            .collect(Collectors.toMap(c -> String.valueOf(c.getData(externalId)), c -> c, (r, l) -> {
                                PortfolioLog.error(MessageFormat.format(
                                                "{0}: Two categories with same external id: {1} and {2}", //$NON-NLS-1$
                                                taxonomy.getName(), r.getName(), l.getName()));
                                return r;
                            }));

            Set<Classification> currentClassifications = new HashSet<>(taxonomy.getClassifications(security));

            for (Pair<String, Double> pair : data)
            {
                if (pair.getValue() == 0d)
                    continue;

                String[] keys = pair.getKey().split(";"); //$NON-NLS-1$

                // create classifications as needed
                Classification[] classification = new Classification[] { taxonomy.getRoot() };
                for (String key : keys)
                {
                    String normalizedKey = keyMapper.apply(key);

                    Classification child = id2classification.computeIfAbsent(normalizedKey, k -> {
                        Classification newClassification = new Classification(classification[0],
                                        UUID.randomUUID().toString(), key);
                        newClassification.setData(externalId, normalizedKey);
                        classification[0].addChild(newClassification);
                        return newClassification;
                    });

                    classification[0] = child;
                }

                currentClassifications.remove(classification[0]);

                Optional<Assignment> assignment = classification[0].getAssignments().stream()
                                .filter(a -> security.equals(a.getInvestmentVehicle())).findAny();

                int weight = (int) Math.round(Classification.ONE_HUNDRED_PERCENT * pair.getValue() / 100);

                if (assignment.isPresent())
                {
                    if (assignment.get().getWeight() != weight)
                    {
                        assignment.get().setWeight(weight);
                        isDirty = true;
                    }
                }
                else
                {
                    Assignment newAssignment = new Assignment(security, weight);
                    classification[0].addAssignment(newAssignment);

                    isDirty = true;
                }
            }

            for (Classification classification : currentClassifications)
            {
                new ArrayList<>(classification.getAssignments()).stream()
                                .filter(a -> security.equals(a.getInvestmentVehicle()))
                                .forEach(classification::removeAssignment);

                isDirty = true;
            }

            return isDirty;
        }
    }

    private static final String HOST = "eodhistoricaldata.com"; //$NON-NLS-1$

    private String apiKey;

    public EODHistoricalDataCom(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public Optional<Fundamentals> lookup(String ticker) throws IOException
    {
        try
        {
            WebAccess webAccess = new WebAccess(HOST, "/api/fundamentals/" + ticker) //$NON-NLS-1$
                            .addParameter("api_token", apiKey) //$NON-NLS-1$
                            .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                            + FrameworkUtil.getBundle(EODHistoricalDataCom.class).getVersion()
                                                            .toString());
            return Optional.of(Fundamentals.from(webAccess.get()));
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            throw e;
        }
    }
}

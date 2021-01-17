package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Objects;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public class ETFDataCom
{
    /* package */ static class SymbolInfo
    {
        private String exchange;
        private String symbol;

        static List<SymbolInfo> from(JSONArray listings)
        {
            if (listings == null)
                return Collections.emptyList();

            List<SymbolInfo> res = new ArrayList<>();
            for (Object listingAsObject : listings)
            {
                JSONObject listing = (JSONObject) listingAsObject;

                SymbolInfo m = new SymbolInfo();
                m.exchange = listing.get("exchange").toString().toUpperCase(Locale.US); //$NON-NLS-1$
                m.symbol = listing.get("ticker").toString().toUpperCase(Locale.US); //$NON-NLS-1$
                res.add(m);
            }

            return (res);
        }

        private SymbolInfo()
        {
        }

        public String getExchange()
        {
            return exchange;
        }

        public String getSymbol()
        {
            return symbol;
        }
    }

    /* package */ static class OnlineItem implements ResultItem
    {
        private Double ter;
        private String indexName;
        private String name;
        private String type = "ETF"; //$NON-NLS-1$
        private String isin;
        private String provider;
        private String domicile;
        private String currencyCode;
        private String replicationMethod;
        private String distributionFrequency;
        private String distributionType;

        private List<SymbolInfo> symbols;
        private List<Pair<String, Double>> regions;
        private List<Pair<String, Double>> sectors;

        /* package */ static OnlineItem from(JSONObject jsonObject)
        {
            OnlineItem vehicle = new OnlineItem();

            vehicle.ter = (Double) jsonObject.get("totalFee"); //$NON-NLS-1$
            vehicle.indexName = (String) jsonObject.get("indexName"); //$NON-NLS-1$
            vehicle.name = (String) jsonObject.get("name"); //$NON-NLS-1$
            vehicle.isin = (String) jsonObject.get("isin"); //$NON-NLS-1$
            vehicle.provider = (String) jsonObject.get("provider"); //$NON-NLS-1$
            vehicle.domicile = (String) jsonObject.get("domicile"); //$NON-NLS-1$
            vehicle.currencyCode = (String) jsonObject.get("baseCurrency"); //$NON-NLS-1$
            vehicle.replicationMethod = (String) jsonObject.get("replicationMethod"); //$NON-NLS-1$
            vehicle.distributionFrequency = (String) jsonObject.get("distributionFrequency"); //$NON-NLS-1$
            vehicle.distributionType = (String) jsonObject.get("distributionType"); //$NON-NLS-1$

            vehicle.symbols = SymbolInfo.from((JSONArray) jsonObject.get("listings")); //$NON-NLS-1$

            vehicle.regions = parse((JSONArray) jsonObject.get("regions"), "country"); //$NON-NLS-1$ //$NON-NLS-2$
            vehicle.sectors = parse((JSONArray) jsonObject.get("sectors"), "sector"); //$NON-NLS-1$ //$NON-NLS-2$

            return vehicle;
        }

        private static List<Pair<String, Double>> parse(JSONArray jsonArray, String nameLabel)
        {
            if (jsonArray == null)
                return Collections.emptyList();

            List<Pair<String, Double>> answer = new ArrayList<>();
            for (Object item : jsonArray)
            {
                JSONObject json = (JSONObject) item;

                String label = json.get(nameLabel).toString();
                Double percentage = (Double) json.get("percentage"); //$NON-NLS-1$

                answer.add(new Pair<>(label, percentage));
            }

            return answer;
        }

        private OnlineItem()
        {
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return ""; //$NON-NLS-1$
        }

        @Override
        public String getSymbol()
        {
            return symbols.stream().map(SymbolInfo::getSymbol).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return symbols.stream().map(SymbolInfo::getExchange).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
        }

        @Override
        public boolean hasPrices()
        {
            return false;
        }

        private Map<String, Object> attributeMapping()
        {
            Map<String, Object> attributeMapping = new HashMap<>();

            attributeMapping.put("etf-data.com$ter", ter != null ? Double.valueOf(ter / 100) : null); // $NON-NLS-1$
            attributeMapping.put("etf-data.com$indexName", indexName); //$NON-NLS-1$
            attributeMapping.put("etf-data.com$vendor", provider); //$NON-NLS-1$
            attributeMapping.put("etf-data.com$domicile", domicile); //$NON-NLS-1$
            attributeMapping.put("etf-data.com$replicationMethod", replicationMethod); //$NON-NLS-1$
            attributeMapping.put("etf-data.com$distributionFrequency", distributionFrequency); //$NON-NLS-1$
            attributeMapping.put("etf-data.com$distributionType", distributionType); //$NON-NLS-1$

            return attributeMapping;
        }

        private boolean updateAttributes(Attributes attributes, ClientSettings settings)
        {
            boolean isDirty = false;

            for (Map.Entry<String, Object> id2value : attributeMapping().entrySet())
            {
                AttributeType attribute = settings.getAttributeTypes()
                                .filter(a -> id2value.getKey().equals(a.getSource())) //
                                .findFirst().orElseGet(() -> {
                                    String id = id2value.getKey();

                                    // build a readable name

                                    String attributeName = TextUtil.fromCamelCase(id.substring(id.indexOf('$') + 1));

                                    // create a new attribute

                                    AttributeType newAttribute = new AttributeType(UUID.randomUUID().toString());
                                    newAttribute.setName(attributeName);
                                    newAttribute.setColumnLabel(attributeName);
                                    newAttribute.setSource(id);
                                    newAttribute.setTarget(Security.class);

                                    if (id.endsWith("$ter")) //$NON-NLS-1$
                                    {
                                        newAttribute.setType(Double.class);
                                        newAttribute.setConverter(PercentConverter.class);
                                    }
                                    else
                                    {
                                        newAttribute.setType(String.class);
                                        newAttribute.setConverter(StringConverter.class);
                                    }

                                    // add new attribute to settings

                                    settings.addAttributeType(newAttribute);
                                    return newAttribute;
                                });

                Object newValue = id2value.getValue();

                Object oldValue = attributes.put(attribute, newValue);

                isDirty = isDirty || !Objects.equal(oldValue, newValue);
            }

            return isDirty;
        }

        private void setInfo(Security security)
        {
            security.setName(name);
            security.setIsin(isin);
            security.setTickerSymbol(symbols.stream().map(SymbolInfo::getSymbol).findAny().orElse(null));
            symbols.forEach(symbolInfo -> security.addProperty(new SecurityProperty(SecurityProperty.Type.MARKET,
                            symbolInfo.getExchange(), symbolInfo.getSymbol())));
            security.setCurrencyCode(CurrencyUnit.getInstance(currencyCode).getCurrencyCode());
        }

        @Override
        public String getExtraAttributes()
        {
            return attributeMapping().keySet().stream()
                            .map(id -> TextUtil.fromCamelCase(id.substring(id.indexOf('$') + 1)))
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$
        }

        @Override
        public Security create(ClientSettings settings)
        {
            Security security = new Security();

            setInfo(security);
            updateAttributes(security.getAttributes(), settings);

            return security;
        }

        public boolean update(Security security, ClientSettings settings)
        {
            return updateAttributes(security.getAttributes(), settings);
        }

        public boolean updateCountryAllocation(Taxonomy taxonomy, Security security)
        {
            return updateAllocation(regions, "ISO3166-1-Alpha-3", taxonomy, security); //$NON-NLS-1$
        }

        public boolean updateSectorAllocation(Taxonomy taxonomy, Security security)
        {
            return updateAllocation(sectors, "GICS-sector", taxonomy, security); //$NON-NLS-1$
        }

        private static boolean updateAllocation(List<Pair<String, Double>> data, String externalId, Taxonomy taxonomy,
                        Security security)
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
                Classification classification = id2classification.computeIfAbsent(pair.getKey(), key -> {
                    Classification newClassification = new Classification(taxonomy.getRoot(),
                                    UUID.randomUUID().toString(), key);
                    newClassification.setData(externalId, key);
                    taxonomy.getRoot().addChild(newClassification);
                    return newClassification;
                });

                currentClassifications.remove(classification);

                Optional<Assignment> assignment = classification.getAssignments().stream()
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
                    classification.addAssignment(newAssignment);

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

    public static final String PROVIDER_NAME = "ETF-Data.com"; //$NON-NLS-1$
    private static final String HOST = "api.etf-data.com"; //$NON-NLS-1$

    public List<ResultItem> search(String isin) throws IOException
    {
        try
        {
            WebAccess webAccess = new WebAccess(HOST, "/product/" + isin) //$NON-NLS-1$
                            .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                            + FrameworkUtil.getBundle(ETFDataCom.class).getVersion().toString());
            return readItems(webAccess.get());
        }
        catch (WebAccessException e)
        {
            PortfolioLog.error(e);

            if (e.getHttpErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                throw new WebAccessException(
                                "Unfortunately your free requests have expired. Please feel free to visit again tomorrow, or subscribe to a plan.", //$NON-NLS-1$
                                HttpURLConnection.HTTP_UNAUTHORIZED);
            else
                return (new ArrayList<ResultItem>());
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
            return (new ArrayList<ResultItem>());
        }
    }

    private List<ResultItem> readItems(String jsonBlob)
    {
        List<ResultItem> onlineItems = new ArrayList<>();
        JSONObject response = (JSONObject) JSONValue.parse(jsonBlob);
        onlineItems.add(OnlineItem.from(response));

        return onlineItems;
    }

    public static boolean updateWith(Security security, ClientSettings settings, ResultItem item)
    {
        if (!(item instanceof OnlineItem))
            throw new IllegalArgumentException();

        return ((OnlineItem) item).update(security, settings);
    }

    public static boolean updateCountryAllocation(Security security, Taxonomy taxonomy, ResultItem item)
    {
        if (!(item instanceof OnlineItem))
            throw new IllegalArgumentException();

        return ((OnlineItem) item).updateCountryAllocation(taxonomy, security);
    }

    public static boolean updateSectorAllocation(Security security, Taxonomy taxonomy, ResultItem item)
    {
        if (!(item instanceof OnlineItem))
            throw new IllegalArgumentException();

        return ((OnlineItem) item).updateSectorAllocation(taxonomy, security);
    }
}

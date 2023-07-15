package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * <p>
 * Use the <a href="https://divvydiary.com/">DivvyDiary.com</a> API to search
 * for securities by ISIN. This API can be used with the free API key that you
 * can obtain by registering on
 * <a href="https://divvydiary.com/">DivvyDiary.com</a> .
 * </p>
 * <p>
 * The DivvyDiary REST API is described using Swagger at <a href=
 * "https://api.divvydiary.com/documentation/">https://api.divvydiary.com/documentation/</a>.
 * </p>
 * <p>
 * DivvyDiary returns a security symbol without a suffix that would normally
 * indicate the exchange to be used for getting quotes for the security.
 * Fortunately, DivvyDiary does return the 4-character MIC in the exchange
 * field. The MIC can be mapped to an exchange suffix and appended to the
 * symbol. This mapping is performed by
 * <code>getExchangeSuffixFromIsoMic()</code> for many popular exchanges.
 * </p>
 */
public class DivvyDiarySearchProvider implements SecuritySearchProvider
{
    /**
     * This <code>Result</code> class includes some fields that are not yet used
     * in Portfolio Performance but they are parsed from the DivvyDiary response
     * to make it easier to use them. This includes the <code>currency</code>,
     * <code>dividendCurrency</code> and <code>dividendFrequency</code>.
     */
    static class Result implements ResultItem
    {
        private String symbol;
        private String name;
        private String wkn;
        private String isin;
        private String exchange;
        private String currency;
        private String dividendCurrency;
        private String dividendFrequency;

        public static Optional<Result> from(JSONObject json)
        {
            var symbol = String.valueOf(json.get("symbol")); //$NON-NLS-1$
            if (symbol == null)
                return Optional.empty();

            var name = String.valueOf(json.get("name")); //$NON-NLS-1$

            var wkn = String.valueOf(json.get("wkn")); //$NON-NLS-1$

            var isin = String.valueOf(json.get("isin")); //$NON-NLS-1$

            var exchange = String.valueOf(json.get("exchange")); //$NON-NLS-1$

            // If the symbol does not have an exchange suffix then try to get it
            // from the exchange
            if (!symbol.contains(".") && exchange != null && !exchange.isBlank()) //$NON-NLS-1$
            {
                final var exchangeSuffix = getExchangeSuffixFromIsoMic(exchange);
                if (!exchangeSuffix.isBlank())
                    symbol += "." + exchangeSuffix; //$NON-NLS-1$
            }
            var currency = String.valueOf(json.get("currency")); //$NON-NLS-1$

            var dividendCurrency = String.valueOf(json.get("dividendCurrency")); //$NON-NLS-1$

            var dividendFrequency = String.valueOf(json.get("dividendFrequency")); //$NON-NLS-1$

            return Optional.of(new Result(symbol, name, wkn, isin, exchange, currency, dividendCurrency,
                            dividendFrequency));
        }

        private Result(String symbol, String name, String wkn, String isin, String exchange, String currency,
                        String dividendCurrency, String dividendFrequency)
        {
            this.symbol = symbol;
            this.name = name;
            this.wkn = wkn;
            this.isin = isin;
            this.exchange = exchange;
            this.currency = currency;
            this.dividendCurrency = dividendCurrency;
            this.dividendFrequency = dividendFrequency;
        }

        /* package */ Result(String description)
        {
            this.name = description;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getType()
        {
            return null;
        }

        @Override
        public String getExchange()
        {
            return exchange;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return wkn;
        }

        public String getCurrency()
        {
            return currency;
        }

        public String getDividendCurrency()
        {
            return dividendCurrency;
        }

        public String getDividendFrequency()
        {
            return dividendFrequency;
        }

        @Override
        public Security create(Client client)
        {
            Security security = new Security(name, currency);
            security.setIsin(isin);
            security.setWkn(wkn);
            security.setTickerSymbol(symbol);
            return security;
        }

        /**
         * <a href=
         * "https://stockmarketmba.com/globalstockexchanges.php">https://stockmarketmba.com/globalstockexchanges.php</a>.
         */
        @SuppressWarnings("nls")
        private static String getExchangeSuffixFromIsoMic(String exchangeCode)
        {
            if (exchangeCode == null)
                return "";

            final String exchangeSuffix;

            switch (exchangeCode.trim())
            {
                case "BVMF":
                    exchangeSuffix = "SA";
                    break;
                case "MISX":
                    exchangeSuffix = "MCX";
                    break;
                case "ROCO":
                    exchangeSuffix = "TWO";
                    break;
                case "XASE":
                case "XBAS":
                case "XNYS":
                case "ARCX":
                case "XNMS":
                case "XNCM":
                case "OOTC":
                case "XNGS":
                    exchangeSuffix = "US";
                    break;
                case "XASX":
                    exchangeSuffix = "AU";
                    break;
                case "XATH":
                    exchangeSuffix = "AT";
                    break;
                case "XBER":
                    exchangeSuffix = "BE";
                    break;
                case "XBKK":
                    exchangeSuffix = "BK";
                    break;
                case "XBOM":
                    exchangeSuffix = "BSE";
                    break;
                case "XBUD":
                    exchangeSuffix = "BUD";
                    break;
                case "XBUE":
                    exchangeSuffix = "BA";
                    break;
                case "XCNQ":
                    exchangeSuffix = "CN";
                    break;
                case "XCSE":
                    exchangeSuffix = "CO";
                    break;
                case "XDUB":
                    exchangeSuffix = "IR";
                    break;
                case "XDUS":
                    exchangeSuffix = "DU";
                    break;
                case "XETR":
                    exchangeSuffix = "DE";
                    break;
                case "XFRA":
                    exchangeSuffix = "F";
                    break;
                case "XHAM":
                    exchangeSuffix = "HM";
                    break;
                case "XHAN":
                    exchangeSuffix = "HA";
                    break;
                case "XHEL":
                    exchangeSuffix = "HE";
                    break;
                case "XHKG":
                    exchangeSuffix = "HK";
                    break;
                case "XIDX":
                    exchangeSuffix = "JK";
                    break;
                case "XIST":
                    exchangeSuffix = "IS";
                    break;
                case "XJSE":
                    exchangeSuffix = "JSE";
                    break;
                case "XKAR":
                    exchangeSuffix = "KAR";
                    break;
                case "XKLS":
                    exchangeSuffix = "KLSE";
                    break;
                case "XKRK":
                    exchangeSuffix = "KO";
                    break;
                case "XLIS":
                    exchangeSuffix = "LS";
                    break;
                case "XLON":
                    exchangeSuffix = "LON";
                    break;
                case "XLUX":
                    exchangeSuffix = "L";
                    break;
                case "XMAD":
                    exchangeSuffix = "MC";
                    break;
                case "XMEX":
                    exchangeSuffix = "MX";
                    break;
                case "MTAA":
                case "XMIL":
                    exchangeSuffix = "MI";
                    break;
                case "XMUN":
                    exchangeSuffix = "MU";
                    break;
                case "XNSE":
                    exchangeSuffix = "NSE";
                    break;
                case "XNZE":
                    exchangeSuffix = "NZ";
                    break;
                case "XOSL":
                    exchangeSuffix = "OL";
                    break;
                case "XPAR":
                    exchangeSuffix = "PA";
                    break;
                case "XPHS":
                    exchangeSuffix = "PSE";
                    break;
                case "XSAU":
                    exchangeSuffix = "SR";
                    break;
                case "XSES":
                    exchangeSuffix = "SG";
                    break;
                case "XSHE":
                    exchangeSuffix = "SHE";
                    break;
                case "XSHG":
                    exchangeSuffix = "SHG";
                    break;
                case "XSTC":
                    exchangeSuffix = "VN";
                    break;
                case "XSTO":
                    exchangeSuffix = "ST";
                    break;
                case "XSTU":
                    exchangeSuffix = "STU";
                    break;
                case "XSWX":
                    exchangeSuffix = "SW";
                    break;
                case "XTAE":
                    exchangeSuffix = "TA";
                    break;
                case "XTAI":
                    exchangeSuffix = "TW";
                    break;
                case "XTKS":
                    exchangeSuffix = "TSE";
                    break;
                case "XVTX":
                    exchangeSuffix = "VX";
                    break;
                case "XWAR":
                    exchangeSuffix = "WAR";
                    break;
                case "XWBO":
                    exchangeSuffix = "VI";
                    break;
                default:
                    exchangeSuffix = "";
                    break;
            }
            return exchangeSuffix;
        }
        
        @Override
        public String getSource()
        {
            return NAME;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Result [symbol=").append(symbol) //
                            .append(", description=").append(name) //
                            .append(", wkn=").append(wkn) //
                            .append(", isin=").append(isin) //
                            .append(", exchange=").append(exchange) //
                            .append(", currency=").append(currency) //
                            .append(']');
            return builder.toString();
        }
    }

    private static final String NAME = "DivvyDiary"; //$NON-NLS-1$
    private String apiKey;

    @Override
    public String getName()
    {
        return NAME;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Search for the ISIN provided in the <code>query</code> parameter. The
     * <code>type</code> parameter is not used.
     * </p>
     * <p>
     * If the DivvyDiary API key is null or blank then an empty
     * <code>List</code> is returned. This prevents
     * <code>401 Unauthorized</code> errors for those users who have not
     * configured DivvyDiary.
     * </p>
     * 
     * @param query
     *            ISIN to look up, which must be 12 characters long
     * @param type
     *            not used
     * @return <code>List</code> of the found securities, which will either
     *         contain one security or an error message.
     */
    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        if (apiKey == null || apiKey.isBlank())
            return Collections.emptyList();

        if (query == null || query.trim().length() != 12)
        {
            return Collections.emptyList();
        }
        else
        {
            List<ResultItem> answer = new ArrayList<>();

            addSymbolSearchResults(answer, query);

            return answer;
        }
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        try
        {
            @SuppressWarnings("nls")
            String html = new WebAccess("api.divvydiary.com", "symbols/" + query) //
                            .addHeader("X-API-KEY", apiKey) //
                            .get();

            JSONObject responseData = (JSONObject) JSONValue.parse(html);
            if (responseData != null)
            {
                Result.from(responseData).ifPresent(answer::add);
            }
        }
        catch (WebAccessException ex)
        {
            PortfolioLog.error(ex);
        }
    }
}

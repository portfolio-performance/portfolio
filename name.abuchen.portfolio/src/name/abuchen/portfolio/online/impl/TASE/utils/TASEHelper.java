package name.abuchen.portfolio.online.impl.TASE.utils;

public class TASEHelper
{



    // static final ThreadLocal<DecimalFormat> FMT_PRICE = new
    // ThreadLocal<DecimalFormat>()
    // {
    // @Override
    // protected DecimalFormat initialValue()
    // {
    // DecimalFormat fmt = new DecimalFormat("0.###", new
    // DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
    // fmt.setParseBigDecimal(true);
    // return fmt;
    // }
    // };

//@formatter:off
    /*
     * Indeces list 
     * 1 12 Participating unit          Security 
     * 1 12 Participating unit TASE UP  Security
     * 1 2 Warrants 
     * 1 3 Convertible Bonds            Security
     * 1 4 Government Bonds             Security
     * 1 41 HiTech Fund                 Security
     * 1 42 ETF Fund -Equity            Security
     * 1 43 ETF - Bonds                 Security
     * 1 44 Foreign ETF - Equity        Security
     * 1 45 Foreign ETF -Bonds          Security
     * 1 5 Corporate Bonds              Security
     * 1 5 Corporate Bonds TASE UP      Security 
     * 1 8 Treasury Bill                Security
     * 2 Null Index                     None
     * 4 Null Mutual Fund               Fund
     * 5 Candidate Company 
     * 5 Company 
     * 5 Deleted Company 
     * 5 None Tradable Company 
     * 5 TASE UP 
     * 5 0 Company 
     * 5 1 Company 
     * 7 1 Derivatives 
     * 7 2 Derivatives 
     * 7 4 Derivatives 
     * 10 1 Fund Manager 
     * 13 46 Hedge Fund                 None
     * 14 Deleted Fund 
     * 15 Disclosures 
     * 15 0 Disclosures 
     * 15 1 Disclosures 
     * 16 Form
     */
// @formatting:on
    public enum TaseSecurityType
    {
        SECURITY(1), INDEX(2), MUTUAL_FUND(4), DELETED(5), UNKOWN(-1);

        private final int value;

        TaseSecurityType(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    public enum TaseSecuritySubType
    {
        COMPANY(0), SHARES(1), WARRENTS(2), CONVERTIBLE_BONDS(3), GOVERNMENT_BONDS(4), CORPORATE_BONDS(5), UNKNOWN(-1);

        private final int value;
        
        TaseSecuritySubType(int value)
        {
            this.value = value;
        
        }

        public int getValue()
        {
            return value;
        }
    }

    public enum Language
    {
        HEBREW(0), ENGLISH(1);

        private final int value;

        Language(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }
    
    public enum TaseType
    {
        FUND(1), SECURITY(2), NONE(0);
        
        private final int value;
        
        TaseType(int value)
        {
            this.value = value;
        }
        public int getValue()
        {
            return value;
        }
    }
    
//    public static LocalDate asDateTime(String s)
//    {
//        DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"); //$NON-NLS-1$
//
//        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
//            return null;
//        String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
//        return LocalDate.parse(dt, datetimeFormatter); // $NON-NLS-1$
//    }

//    public static LocalDate asDate(String s)
//    {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$
//
//        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
//            return null;
//        String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
//        return LocalDate.parse(dt, formatter); // $NON-NLS-1$
//    }
    
//    public static Optional<String> extract(String body, int startIndex, String startToken, String endToken)
//    {
//        int begin = body.indexOf(startToken, startIndex);
//
//        if (begin < 0)
//            return Optional.empty();
//
//        int end = body.indexOf(endToken, begin + startToken.length());
//        if (end < 0)
//            return Optional.empty();
//
//        return Optional.of(body.substring(begin + startToken.length(), end));
//    }

//    public static long asPrice(String s)
//    {
//        try 
//        {
//            return asPrice(s, BigDecimal.ONE);
//        }
//        catch (ParseException e) 
//        {
//            return -1l;
//        }
//    }
//
//    static long asPrice(String s, BigDecimal factor) throws ParseException
//    {
//        if ("N/A".equals(s) || "null".equals(s) || "NaN".equals(s) || ".".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//            return LatestSecurityPrice.NOT_AVAILABLE;
//        BigDecimal v = (BigDecimal) FMT_PRICE.get().parse(s);
//        return v.multiply(factor).multiply(Values.Quote.getBigDecimalFactor()).setScale(0, RoundingMode.HALF_UP)
//                        .longValue();
//    }

//    static int asNumber(String s) throws ParseException
//    {
//        if ("N/A".equals(s) || "null".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$
//            return -1;
//        return FMT_PRICE.get().parse(s).intValue();
//    }

    
   

//    public static Map<String, Object> ObjectToMap(Object listing)
//    {
//        Gson gson = GSONUtil.createGson();
//        String json = gson.toJson(listing);
//        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>()
//        {
//        }.getType());
//        return map;
//    }

//    public static long convertILS(long price, String quoteCurrency, String securityCurrency)
//    {
//        if (quoteCurrency != null)
//        {
//            if ("ILA".equals(quoteCurrency) && "ILS".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
//                return price / 100;
//            if ("ILS".equals(quoteCurrency) && "ILA".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
//                return price * 100;
//        }
//        return price;
//    }

//    public static long asLong(String value)
//    {
//        if (value == null)
//            return 0;
//        try
//        {
//            return Long.parseLong(value);
//        }
//        catch (NumberFormatException e)
//        {
//            return 0;
//        }
//    }
}

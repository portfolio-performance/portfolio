package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FundListing
{
    public String FundLongName;
    public String FundShortName;
    private int ManagerId;
    private String ManagerShortName;
    private String ManagerLongName;
    private int TrusteeId;
    private String TrusteeShortName;
    private String TrusteeLongName;
    private String Site;
    private String SharesExposureCd;
    private String SharesExposureDesc;
    private String ForeignCurrencyExposureCd;
    private String ForeignCurrencyExposureDesc;
    private String AssetValue;
    private String AssetAsOfDate;
    private int InsertRates;
    private String TaxRouteDesc;
    private int TaxRouteCd;
    private boolean IsUnrestrictedFund;
    private boolean IsLeveragedFund;
    private boolean IsImitatingFund;
    private boolean IsInvertedExposureFund;
    private boolean IsBankPortfolioTracking;
    private boolean IsRegularDatesFund;
    private boolean IsFundsDenominated;
    private boolean IsForeignInvestmentFund;
    private boolean IsTaxFreeForForeignInvestment;
    private String ForeignCoinName;
    private String ForeignCoinCd;
    private int ProspectusReportCd;
    private String ProspectusPubDate;
    private int AnnualProspectusReportCd;
    private String AnnualProspectusPubDate;
    private int MonthlyReportCode;
    private int MonthlyReportMonth;
    private String MonthlyReportMonthDesc;
    private int MonthlyReportYear;
    private String EstimatedYield;
    private String EstimatedYieldDate;
    private String TradingHaltDesc;
    private String OfferingDay;
    // private FundIndicator[] FundIndicators;
    private String[] Assets;
    // private FundDisclosure[] DisclosureChart;
    // private FundRisk[] AssetRisk;
    // private FundAssetComposition AssetCompostion;
    private String DateValid;
    private int UnitValueIssued;
    private int UnitValueRedeemed;
    private String[] SecurityRedemptionData;
    private int FundId;
    private String FundTypeDesc;
    private int MagnaFundType;
    private String Logo;
    private boolean IsCandidate;
    private String MerLiqStatNm;
    private String MainClassificationCd;
    private String SecondaryClassificationCd;
    private String SubClassificationCd;
    private String MainClassification;
    private String SecondaryClassification;
    private String SubClassification;
    private String Classification;
    private String ExposureProfile;
    private String DistributeProfitPolicy;
    private String ManagementFee;
    private String VariableFee;
    private String TrusteeFee;
    private String SuccessFee;
    private String SharesExposureVal;
    private String ForeignCurrencyExposureVal;
    private int ClassificationCd;
    private String ClassificationDesc;

    private String PurchasePrice;
    private String SellPrice;
    private String CreationPrice;
    private String UnitValuePrice;
    private String UnitValueValidDate;
    private String DayYield;
    private boolean ShowDayYield;
    private String DailyRemark;
    private String PosNegYield;
    private String CorrectTradeDate;
    private boolean IsKosherFund;
    private String MachamTotal;
    private String DisclosureDateOfReport;
    private String RelevantDate;
    private int StockType;
    private int FundType;
    private String RegisteredCapitalPaid;
    private String PersonalFolderLink;
    private String FundSourceCd;
    private String TransitionAt;
    private boolean MonthShowYield;
    private String MonthYield;
    private String MonthPosNeg;

    private String MonthAverage;
    private String MonthDesc;
    private String MonthRemark;
    private boolean YearShowYield;
    private String YearYield;
    private String YearPosNeg;
    private String YearAverage;
    private String YearDesc;
    private String YearRemark;
    private boolean Last12MonthShowYield;
    private String Last12MonthYield;
    private String Last12MonthPosNeg;
    private String Last12MonthAverage;
    private String Last12MonthDesc;
    private String Last12MonthRemark;
    private boolean StandardDeviationShow;
    private String StandardDeviation;
    private String StandardDeviationPosNeg;
    private String StandardDeviationAverage;
    private String StandardDeviationDesc;
    private String StandardDeviationRemark;
    private int IsInternational;
    private String RedemptionFirstOrLast;
    private String RedemptionPeriod;
    private String RedemptionOther;
    private String RedemptionDays;
    private String MinBuyingUnit;
    private String MaxBuyingUnit;
    private String Title;
    private String EngTitle;
    private int TradeActiveDay;
    private String LastTradeDate;
    private String LastTrade;
    private String Icon;
    // private FundMeta MetaTag;

    public String getAssetValue()
    {
        return AssetValue;
    }

    public void setAssetValue(String assetValue)
    {
        AssetValue = assetValue;
    }

    public String getPurchasePrice()
    {
        return PurchasePrice;
    }

    public void setPurchasePrice(String purchasePrice)
    {
        PurchasePrice = purchasePrice;
    }

    public String getSellPrice()
    {
        return SellPrice;
    }

    public void setSellPrice(String sellPrice)
    {
        SellPrice = sellPrice;
    }

    public String getUnitValuePrice()
    {
        return UnitValuePrice;
    }

    public void setUnitValuePrice(String unitValuePrice)
    {
        UnitValuePrice = unitValuePrice;
    }

    public String getUnitValueValidDate()
    {
        return UnitValueValidDate;
    }

    public void setUnitValueValidDate(String unitValueValidDate)
    {
        UnitValueValidDate = unitValueValidDate;
    }


    public FundListing(String longName, String shortName, String asset)
    {
        this.FundLongName = longName;
        this.FundShortName = shortName;
        this.AssetAsOfDate = asset;
    }

    // public Map<String, String> toMap() {
    // Map<String, String> map = parameters(new FundListing());
    // return map;
    // }

    private static Map<String, String> parameters(Object obj)
    {
        Map<String, String> map = new HashMap<>();
        for (Field field : obj.getClass().getDeclaredFields())
        {
            field.setAccessible(true);
            try
            {
                map.put(field.getName(), field.get(obj).toString());
            }
            catch (Exception e)
            {
            }
        }
        return map;
    }
}

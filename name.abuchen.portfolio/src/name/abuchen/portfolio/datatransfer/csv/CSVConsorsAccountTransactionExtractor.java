package name.abuchen.portfolio.datatransfer.csv;

import java.text.ParseException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Header;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

/* package */ class CSVConsorsAccountTransactionExtractor extends CSVAccountTransactionExtractor
{
    /* package */ CSVConsorsAccountTransactionExtractor(Client client)
    {
        super(client, Messages.CSVDefConsorsAccountTransactions);
    }

    CSVConsorsAccountTransactionExtractor(Client client, String label)
    {
        super(client, label);
    }

    @Override
    public int getDefaultSkipLines()
    {
        return 1;
    }

    @Override
    public String getDefaultEncoding()
    {
        return "UTF-8";
    }

    @Override
    public <E extends Enum<E>> EnumMap<E, String> getDefaultEnum(Class<E> enumType)
    {

        if (enumType.equals(AccountTransaction.Type.class))
        {
            //System.err.println(">>>> CSVDibaAccountTransactionExtratctor:getDefaultEnum enumType IF " + enumType.toString());
            final EnumMap<E, String> enumMap = new EnumMap<>(enumType);
            enumMap.put((E) Type.BUY, "Wertpapierkauf");
            enumMap.put((E) Type.SELL, "Wertpapiergutschrift");
            enumMap.put((E) Type.DEPOSIT, "Gutschrift|Gehalt/Rente|Überweisung");
            enumMap.put((E) Type.INTEREST, "Abschluss");
            enumMap.put((E) Type.REMOVAL, "Lastschrift|Dauerauftrag");
            enumMap.put((E) Type.INTEREST_CHARGE, "Zinsen");
            return enumMap;
        }
        else
        {
            //System.err.println(">>>> CSVDibaAccountTransactionExtratctor:getDefaultEnum enumType ELSE " + enumType.toString());
            return null;
        }
    }

    @Override
    
    public Header.Type getDefaultHeadering()
    {
        return Header.Type.DEFAULT;
    }
    
    @Override   
    public String[] getDefaultHeader()
    {
        String[] defaultHeader = {  "",  //0
                                    Messages.CSVColumn_Date, //1
                                    Messages.CSVColumn_SecurityName, //2
                                    Messages.CSVColumn_ISIN, //3
                                    "", //4
                                    Messages.CSVColumn_Type, //5
                                    Messages.CSVColumn_Note, //6
                                    "", //7
                                    "", //8
                                    "", //9
                                    Messages.CSVColumn_Value, //10
                                    "", //11
                                    "" //12
                                                                        };
        //System.err.println(">>>> CSVDibaAccountTransactionExtratctor:DefaultHeader: " + Arrays.toString(defaultHeader));
        return defaultHeader;
    }

    @Override
    protected  Type inferType(String[] rawValues, Map<String, Column> field2column, Security security, Money amount)
                    throws ParseException
    {
        Type type   = super.inferType(rawValues, field2column, security, amount);
        if (type != null)
        {
            if (amount.isNegative())
                switch (type)
                {
                    case DEPOSIT:
                    case DIVIDENDS:
                    case FEES_REFUND:
                    case INTEREST:
                    case TAX_REFUND:
                        type = type.getSibling();
                        break;
                    default:
                }
            else
                switch (type)
                {
                    case REMOVAL:
                    case DIVIDEND_CHARGE:
                    case FEES:
                    case INTEREST_CHARGE:
                    case TAXES:
                        type = type.getSibling();
                        break;
                    default:
                }
        }
        return type;
    }

}

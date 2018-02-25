package name.abuchen.portfolio.datatransfer.csv;

import java.util.EnumMap;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Header;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;

/* package */ class CSVDibaAccountTransactionExtractor extends CSVAccountTransactionExtractor
{
    /* package */ CSVDibaAccountTransactionExtractor(Client client)
    {
        super(client, Messages.CSVDefDibaAccountTransactions);
    }

    @Override
    public int getDefaultSkipLines()
    {
        return 7;
    }

    @Override
    public String getDefaultEncoding()
    {
        return "windows-1252";
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
            enumMap.put((E) Type.DEPOSIT, "Gutschrift");
            enumMap.put((E) Type.INTEREST, "Abschluss");
            enumMap.put((E) Type.REMOVAL, "Überweisung");
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
                                    Messages.CSVColumn_Note, //2
                                    Messages.CSVColumn_Type, //3
                                    Messages.CSVColumn_ISIN, //4
                                    Messages.CSVColumn_Value, //5
                                    Messages.CSVColumn_TransactionCurrency //6 
                                    };
        //System.err.println(">>>> CSVDibaAccountTransactionExtratctor:DefaultHeader: " + Arrays.toString(defaultHeader));
        return defaultHeader;
    }
    
}

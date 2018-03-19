package name.abuchen.portfolio.datatransfer.csv;

import java.util.EnumMap;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Header;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;

/* package */ class CSVConsorsAccountBookingExtractor extends CSVConsorsAccountTransactionExtractor
{
    /* package */ CSVConsorsAccountBookingExtractor(Client client)
    {
        super(client, Messages.CSVDefConsorsAccountBookings);
    }

    @Override
    public String getDefaultEncoding()
    {
        return "windows-1252";
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
                                    Messages.CSVColumn_Value, //7
                                    "", //8
                                    "" //9
                                                                        };
        //System.err.println(">>>> CSVDibaAccountTransactionExtratctor:DefaultHeader: " + Arrays.toString(defaultHeader));
        return defaultHeader;
    }
    
}

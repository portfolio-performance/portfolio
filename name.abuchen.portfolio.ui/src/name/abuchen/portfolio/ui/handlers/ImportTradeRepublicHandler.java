package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.traderepublic.TradeRepublicCSVExtractor;
import name.abuchen.portfolio.model.Client;

public class ImportTradeRepublicHandler extends AbstractBrokerCSVImportHandler
{
    @Override
    protected Extractor createExtractor(Client client)
    {
        return new TradeRepublicCSVExtractor(client);
    }
}

package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.xtb.XTBCSVExtractor;
import name.abuchen.portfolio.model.Client;

public class ImportXTBHandler extends AbstractBrokerCSVImportHandler
{
    @Override
    protected Extractor createExtractor(Client client)
    {
        return new XTBCSVExtractor(client);
    }
}

package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.ConsumerPriceIndex;

public interface CPIFeed
{
    List<ConsumerPriceIndex> getConsumerPriceIndices() throws IOException;
}

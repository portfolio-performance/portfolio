package name.abuchen.portfolio.ui.views.dashboard.event;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.views.dashboard.event.item.EventItem;
import name.abuchen.portfolio.util.TradeCalendarManager;

public final class EventItemSupplier implements Supplier<List<EventItem>>
{

    private final Client client;
    private final EventItemFactory eventItemFactory;
    
    public EventItemSupplier(Client client, EventItemFactory eventItemFactory)
    {
        this.client = client;
        this.eventItemFactory = eventItemFactory;
    }

    @Override
    public List<EventItem> get()
    {
        Stream<EventItem> securityStream = client.getSecurities().stream()
                .map(eventItemFactory::fromSecurity)
                .flatMap(List::stream)
                .distinct();
                
        Stream<EventItem> transactionStream = client.getAccounts().stream()
                .map(eventItemFactory::fromAccount)
                .flatMap(List::stream)
                .distinct();

        Stream<EventItem> holidayStream = Stream.of(TradeCalendarManager.getDefaultInstance())
                .map(eventItemFactory::fromTradeCalendar)
                .flatMap(List::stream)
                .distinct();
        
        return Stream.of(securityStream, transactionStream, holidayStream)
                .flatMap(Function.identity())
                .sorted()
                .collect(toUnmodifiableList());
    }

}

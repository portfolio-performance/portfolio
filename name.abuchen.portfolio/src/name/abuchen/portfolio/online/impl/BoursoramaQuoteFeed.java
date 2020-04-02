package name.abuchen.portfolio.online.impl;

import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Security;

public final class BoursoramaQuoteFeed extends GenericJSONQuoteFeed
{

    public static final String ID = "BOURSORAMA"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelBoursorama;
    }

    @Override
    public Optional<String> getHelpURL()
    {
        return Optional.empty();
    }


    @Override
    protected String feedURL(Security security)
    {
        return "https://www.boursorama.com/bourse/action/graph/ws/GetTicksEOD?symbol={TICKER}&length=7300&period=0&guid="; //$NON-NLS-1$
    }

    @Override
    protected Optional<String> dateProperty(Security security)
    {
        return Optional.of("$.d.QuoteTab[*].d"); //$NON-NLS-1$
    }

    @Override
    protected Optional<String> closeProperty(Security security)
    {
        return Optional.of("$.d.QuoteTab[*].c"); //$NON-NLS-1$
    }

}

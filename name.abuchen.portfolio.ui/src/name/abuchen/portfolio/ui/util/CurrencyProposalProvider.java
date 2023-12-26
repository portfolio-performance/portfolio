package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;

public final class CurrencyProposalProvider implements IContentProposalProvider
{
    private List<CurrencyUnit> currencies;

    public CurrencyProposalProvider(List<CurrencyUnit> currencies)
    {
        this.currencies = currencies;
    }

    @Override
    public IContentProposal[] getProposals(String contents, int position)
    {
        List<IContentProposal> proposals = new ArrayList<>();

        if (!contents.isEmpty())
        {
            String c = contents.toLowerCase();

            for (CurrencyUnit currency : currencies)
            {
                String label = currency.getLabel().toLowerCase();
                if (label.contains(c))
                    proposals.add(new ContentProposal(currency.getCurrencyCode(), currency.getLabel(), null));
            }
        }

        // below matching currencies, add everything for scrolling
        proposals.add(new ContentProposal(String.format("--- %s ---", Messages.LabelAllCurrencies))); //$NON-NLS-1$
        for (CurrencyUnit currency : currencies)
            proposals.add(new ContentProposal(currency.getCurrencyCode(), currency.getLabel(), null));
        return proposals.toArray(new IContentProposal[0]);
    }
}

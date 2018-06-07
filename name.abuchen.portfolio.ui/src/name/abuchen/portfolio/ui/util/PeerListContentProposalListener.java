package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;

import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionModel;

public class PeerListContentProposalListener implements IContentProposalListener
{
    protected AccountTransactionModel model;
    
    public PeerListContentProposalListener(AccountTransactionModel model)
    {
        this.model = model;
    }

    public void proposalAccepted(IContentProposal proposal)
    {
        if (proposal instanceof PeerListContentProposal)
        {
            System.err.println(">>>> PeerListContentProposalListener::proposalAccepted proposal: " + proposal.toString() + " |  peer: " + ((PeerListContentProposal) proposal).getPeer().toString());// TODO: still needed for debug?
            this.model.setPeer(((PeerListContentProposal) proposal).getPeer());
        }
        else
            throw new IllegalArgumentException();
    }
}


package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import name.abuchen.portfolio.model.Peer;
import name.abuchen.portfolio.model.PeerList;

// this class was inspired by http://javawiki.sowas.com/doku.php?id=swt-jface:autocompletefield

public abstract class PeerListContentProposalProvider implements IContentProposalProvider
{
    private PeerList peerList;
    //private PeerListContentProposal contentProposal;

    public class IbanProposal extends PeerListContentProposal
    {
        public IbanProposal(Peer peer)
        {
            super(peer);
        }
        
        @Override
        public String getContent()
        {
            System.err.println(">>>> PeerListContentProvider::IbanProposal::getContent "  + peer.toString());// TODO: still needed for debug?
            return peer.getIban();
        }

        @Override
        public int getCursorPosition()
        {
            System.err.println(">>>> PeerListContentProposal::IbanProposal::getCursorPosition "  + peer.toString());// TODO: still needed for debug?
           return peer.getIban().length();
        }          
    }

    public class PartnerProposal extends PeerListContentProposal
    {
        public PartnerProposal(Peer peer)
        {
            super(peer);
        }

        @Override
        public String getContent()
        {
            System.err.println(">>>> PeerListContentProvider::PartnerProposal::getContent "  + peer.toString());// TODO: still needed for debug?
            return peer.getName();
        }

        @Override
        public int getCursorPosition()
        {
            System.err.println(">>>> PeerListContentProposal::IbanProposal::getCursorPosition "  + peer.toString());// TODO: still needed for debug?
           return peer.getName().length();
        }          
    }

    public PeerListContentProposalProvider(PeerList peerList)
    {
        super();
        System.err.println(">>>> PeerListContentProvider::PeerContentProvider "  + Arrays.toString(peerList.toArray()));// TODO: still needed for debug?
        this.peerList = peerList;
        //this.contentProposal = new PeerListContentProposal();
    }

    public IContentProposal[] getProposals(String contents, int position)
    {
        System.err.println(">>>> PeerListContentProvider::getProposals "  + contents);// TODO: still needed for debug?
        ArrayList proposals = new ArrayList();
        PeerList l = peerList.findPeer(contents);
        if (l != null)
        {
            System.err.println(">>>> PeerListContentProvider::getProposals() PeerList: " + Arrays.toString(l.toArray()));// TODO: still needed for debug?
            for (Peer p : l)
            {
                System.err.println(">>>> PeerListContentProvider::getProposals() peer   : " + p.toString());// TODO: still needed for debug?
                proposals.add(makeContentProposal(p));
            }
        }
        System.err.println(">>>> PeerListContentProvider::getProposals() proposals: " + Arrays.toString(proposals.toArray()));// TODO: still needed for debug?
        return (IContentProposal[]) proposals.toArray(new IContentProposal[proposals.size()]);
    }

//    public void setContentProposalClass(PeerListContentProposal contentProposal)
//    {
//        this.contentProposal = contentProposal;
//    }

    protected IContentProposal makeContentProposal(Peer peer)
    {
        System.err.println(">>>> PeerListContentProvider::makeContentProposal(peer) peer: " + peer.toString());// TODO: still needed for debug?
        return (IContentProposal) new PeerListContentProposal(peer);
    }

}

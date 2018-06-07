package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.fieldassist.IContentProposal;

import name.abuchen.portfolio.model.Peer;

// this class was inspired by http://javawiki.sowas.com/doku.php?id=swt-jface:autocompletefield

public class PeerListContentProposal implements IContentProposal
{
    public Peer peer;
    
    public PeerListContentProposal(final Peer peer)
    {
        this.peer = peer;
    }

    public Peer getPeer()
    {
        System.err.println(">>>> PeerListContentProposal::getPeer "  + peer.toString());// TODO: still needed for debug?
        return peer;
    }

    public String getContent()
    {
        System.err.println(">>>> PeerListContentProposal::getContent "  + peer.toString());// TODO: still needed for debug?
        return "<" + peer.getName() + " [" + peer.getIban() + "] - " + peer.getNote() + ">";
    }
    
    public String getDescription()
    {
        System.err.println(">>>> PeerListContentProposal::getDescription ");// TODO: still needed for debug?
       // Wenn hier was zurückgegeben wird, dann erscheint dieser Text in einem seperatem Fenster
       return null;  
    }

    public String getLabel()
    {
        System.err.println(">>>> PeerListContentProposal::getLabel "  + peer.toString());// TODO: still needed for debug?
       return peer.getName() + " (" + peer.getIban() + ")";
    }

    public int getCursorPosition()
    {
        System.err.println(">>>> PeerListContentProposal::getCursorPosition "  + peer.toString());// TODO: still needed for debug?
       return peer.getName().length();
    }          
}
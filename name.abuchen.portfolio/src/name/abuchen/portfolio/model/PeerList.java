package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PeerList extends ArrayList<Peer>
{
    public PeerList()
    {
        super();
    }

    public PeerList(PeerList peers)
    {
        // TODO: still needed for debug? System.err.println(">>>> PeerList(PeerList) peers   : " + Arrays.toString(peers.toArray()));
        for (Peer peer : peers)
        {
            this.add(peer);
        }
    }

    @Override
    public boolean add(Peer peer)
    {
        // TODO: still needed for debug? System.err.println(">>>> PeerList::addPeer() peer   : " + peer.toString());
        return super.add(peer);
    }

    @Override
    public boolean contains(Object o)
    {
        if (o == null)
            return false;
        Peer peer = (Peer) o;
        if (peer == null)
            return false;
        System.err.println(">>>> PeerList::contains() peer   : " + peer.toString()); // TODO: still needed for debug?
        return super.contains(peer);
    }

    @Override
    public boolean remove(Object o)
    {
        Peer peer = (Peer) o;
        System.err.println(">>>> PeerList::remove() peer   : " + peer.toString()); // TODO: still needed for debug?
        return super.remove(peer);
    }

    public PeerList findPeer(String search)
    {
        // TODO: still needed for debug? System.err.println(">>>> PeerList::findPeer() peers   : " + Arrays.toString(this.toArray()));
        PeerList peerList = new PeerList();
        for (Peer peer : this)
        {
            //System.err.println(">>>> PeerList::findPeer() peer   : " + peer.toString() + " vs. " + search);
            if (peer.getIban() != null && peer.getIban().length() >= search.length()
                            && peer.getIban().toLowerCase().contains(search.toLowerCase()))
                peerList.add(peer);
            else if (peer.getName().length() >= search.length()
                            && peer.getName().toLowerCase().contains(search.toLowerCase()))
                peerList.add(peer);
        }
        if (peerList.size() == 0)
            return null;
        System.err.println(">>>> PeerList::findPeer() matches   : " + peerList.size() + " " + Arrays.toString(peerList.toArray()));
        return peerList;
    }

    public PeerList addAccounts(List<Account> accounts)
    {
        PeerList peerList = new PeerList();
        for (Peer peer : this)
        {
            peerList.add(peer);
        }
        accounts.stream() //
                .filter(a -> a.hasIban()) //
                .sorted(new Account.ByName()) //
                .forEach(a -> {                    
                    Peer peer = a.asPeer();
                    // TODO: still needed for debug? System.err.println(">>>> PeerList::addAccounts() account   : " + a.toString());
                    // TODO: still needed for debug? System.err.println(">>>> PeerList::addAccounts() peer   : " + peer.toString());
                    peerList.add(peer);                    
                });
        // TODO: still needed for debug? System.err.println(">>>> PeerList::addAccounts() peers   : " + Arrays.toString(this.toArray()));
        // TODO: still needed for debug? System.err.println(">>>> PeerList::addAccounts() peerList: " + Arrays.toString(peerList.toArray()));
        return peerList;
    }

    @Override
    public String toString()
    {
        return "PeerList.toString: "+ Arrays.toString(this.toArray());
    }
}

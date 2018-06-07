package name.abuchen.portfolio.datatransfer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.PeerItem;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Peer;

public class PeerCache
{
    private static final Peer DUPLICATE_PEER_MARKER = new Peer();

    private static final List<String> MESSAGES = Arrays.asList(Messages.MsgErrorDuplicateIBAN, Messages.MsgErrorDuplicateName);

    private final Client client;

    private final List<Map<String, Peer>> localMaps = new ArrayList<>();

    public PeerCache(Client client)
    {
        this.client = client;

        List<Peer> lp = ((List<Peer>) client.getPeers().addAccounts(client.getAccounts()));

        this.localMaps.add(lp.stream().filter(p -> p.getIban() != null && !p.getIban().isEmpty())
                        .collect(Collectors.toMap(Peer::getIban, p -> p, (l, r) -> DUPLICATE_PEER_MARKER)));

        this.localMaps.add(client.getPeers().addAccounts(client.getAccounts()).stream().filter(p -> p.getName() != null && !p.getName().isEmpty())
                        .collect(Collectors.toMap(Peer::getName, p -> p, (l, r) -> DUPLICATE_PEER_MARKER)));

        // TODO: still needed for debug? System.err.println(">>>> PeerCache::PeerCache");
    }

    public Peer lookup(String iban, String name,
                    Supplier<Peer> creationFunction)
    {
        System.err.println(">>>> PeerCache::lookup iban: " + iban + " name: " + name); // TODO: still needed for debug?
        List<String> attributes = Arrays.asList(iban, name);

        // first: check the identifying attributes (IBAN)
        for (int ii = 0; ii < 1; ii++)
        {
            String attribute = attributes.get(ii);

            Peer peer = localMaps.get(ii).get(attribute);
            if (peer == DUPLICATE_PEER_MARKER)
                throw new IllegalArgumentException(MessageFormat.format(MESSAGES.get(ii), attribute));
            if (peer != null)
            {
                System.err.println(">>>> PeerCache::lookup peer: " + peer.toString()); // TODO: still needed for debug?
                return peer;
            }
        }

        // second: check the name. But: even if the name matches, we also must
        // check that the identifying attributes do not differ. Why? Investment
        // instruments could have the same name but different ISINs.

        System.err.println(">>>> PeerCache::lookup byNAME"); // TODO: still needed for debug?
        Peer peer = lookupPeerByName(iban, name);
        if (peer != null)
        {
            System.err.println(">>>> PeerCache::lookup peer: " + peer.toString()); // TODO: still needed for debug?
            return peer;
        }

        peer = creationFunction.get();
        peer.setIban(iban);
        peer.setName(name);

        for (int ii = 0; ii < localMaps.size(); ii++)
        {
            String attribute = attributes.get(ii);
            if (attribute != null)
                localMaps.get(ii).put(attribute, peer);
        }

        System.err.println(">>>> PeerCache::lookup =END= peer: " + peer.toString()); // TODO: still needed for debug?
        return peer;
    }

    private Peer lookupPeerByName(String iban, String name)
    {
        Peer peer = localMaps.get(1).get(name);

        // allow imports by duplicate name
        if (peer == null || peer == DUPLICATE_PEER_MARKER)
            return null;

        if (doNotMatchIfGiven(iban, peer.getIban()))
            return null;
        return peer;
    }

    private boolean doNotMatchIfGiven(String attribute1, String attribute2)
    {
        return attribute1 != null && attribute2 != null && !attribute1.equalsIgnoreCase(attribute2);
    }

    /**
     * Returns a list of {@link PeerItem} that are implicitly created when
     * extracting transactions. Do not add all newly created securities as they
     * might be created out of erroneous transactions.
     */
    public Collection<Item> createMissingPeerItems(List<Item> items)
    {
        List<Item> answer = new ArrayList<>();

        // TODO: still needed for debug? System.err.println(">>>> PeerCache::createMissingPeerItems =BEGIN=");

        Set<Peer> available = new HashSet<>();
        available.addAll(client.getPeers().addAccounts(client.getAccounts()));
        items.stream().filter(i -> i instanceof PeerItem).map(Item::getPeer).forEach(available::add);

        for (Item item : items)
        {
            if (item instanceof PeerItem || item.getPeer() == null)
                continue;

            System.err.println(">>>> PeerCache::createMissingPeerItems CHECK item.getPeer: " + item.getPeer() + " vs. " + available); // TODO: still needed for debug?
            if (!available.contains(item.getPeer()))
            {
                answer.add(new PeerItem(item.getPeer()));
                System.err.println(">>>> PeerCache::createMissingPeerItems ADD: " + item.getPeer() ); // TODO: still needed for debug?
                available.add(item.getPeer());
            }
        }
     // TODO: still needed for debug? System.err.println(">>>> PeerCache::createMissingPeerItems ==END==" + answer.toString());
        return answer;
    }
}

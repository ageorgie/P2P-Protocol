package ece454p1;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Peers is a dumb container to hold the peers; the number of peers is fixed,
 * but needs to be set up when a peer starts up; feel free to use some other
 * container class, but that class must have a method that allows it to read the
 * peersFile, since otherwise you have no way of having a calling entity tell
 * your code what the peers are in the system.
 **/
public class Peers implements Serializable {

	/**
	 * The peersFile is the name of a file that contains 
	 * a list of the peers. Its format is as follows: 
	 * in plaintext there are up to maxPeers lines, where
	 * each line is of the form: <IP address> <port number> 
	 * This file should be available on every machine 
	 * on which a peer is started, though you should
	 * exit gracefully if it is absent or incorrectly formatted. 
	 * After execution of this method, the peers should be present.
	 * 
	 * @param peersFile
	 * @return
	 */



    Map<String, Map<String, BitSet>> peerFileMap = new HashMap<String, Map<String, BitSet>>();

    public Peers(Map<String, Map<String, BitSet>> peerFileMap) {
        this.peerFileMap = peerFileMap;
    }

    public void updatePeerFileMap(Chunk chunk) {
        peerFileMap.get(Peer.getHostAndPort());
        updatePeerFileMap(chunk.getPeerFileMap());
    }

    public void updatePeerFileMap(Map<String, Map<String, BitSet>> receivedPeerFileMap) {
        for(Map.Entry<String, Map<String, BitSet>> entry:receivedPeerFileMap.entrySet()) {
            String remoteHost = entry.getKey();
            Map<String, BitSet> bitSetMap = entry.getValue();
            if(peerFileMap.containsKey(remoteHost)) {

                for(Map.Entry<String, BitSet> bitSetEntry: bitSetMap.entrySet()) {
                    String fileName = bitSetEntry.getKey();
                    BitSet receivedBitSet = bitSetEntry.getValue();

                    if(bitSetMap.containsKey(fileName)) {
                        BitSet originalBitSet = bitSetMap.get(fileName);
                        originalBitSet.or(receivedBitSet);
                    } else  {
                        bitSetMap.put(fileName, receivedBitSet);
                    }
                }
            } else {
                peerFileMap.put(remoteHost, bitSetMap);
            }
        }
    }

    public void insertNewFile(String fileName, int numChunks) {
        System.err.println(Peer.getHostAndPort());
        Map<String, BitSet> localBitSetMap = peerFileMap.get(Peer.getHostAndPort());
        if(!localBitSetMap.containsKey(fileName)) {
            BitSet bitSet = new BitSet(numChunks);
            for(int i = 0;i< bitSet.length() ; i++) {
                bitSet.flip(i);
            }
            localBitSetMap.put(fileName, bitSet);
        }
    }

    public List<String> getPeerAddresses() {
        List<String> output = new LinkedList<String>();
        for(Map.Entry entry:peerFileMap.entrySet()) {
            output.add((String) entry.getKey());
        }
        return output;
    }

    public Map<String, Map<String, BitSet>> getPeerFileMap() {
        return peerFileMap;
    }


    //	public abstract int initialize(String peersFile);

//	public abstract Peer getPeer(int i);


//    public void broadcastMap() {}

//	private int numPeers;

//    HashMap<String, BitSet>[] peerBitSetArray = new HashMap[Config.MAX_PEERS];


}

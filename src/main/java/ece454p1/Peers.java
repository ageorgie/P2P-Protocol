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

    Map<String, Map<String, BitSet>> peerFileMap = new HashMap<String, Map<String, BitSet>>();
    Map<String, int[]> replicationMap = new HashMap<String, int[]>();

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
                    BitSet bitSet = bitSetMap.get(fileName);
                    if(bitSetMap.containsKey(fileName)) {

                        receivedBitSet.xor(bitSet);
                    } else  {
                        bitSetMap.put(fileName, receivedBitSet);
                    }

                    for (int i = receivedBitSet.nextSetBit(0); i >= 0; i = receivedBitSet.nextSetBit(i+1)) {
                        bitSet.set(i, true);
//                        String chunkId = String.format("%s,%s", fileName, i);
//                        insertIntoReplicationMap(chunkId);
                    }

                }
            } else {
                peerFileMap.put(remoteHost, bitSetMap);
            }
        }
    }


    public void fillReplicationMap() {
        for(Map<String, BitSet> fileBitSetMap:peerFileMap.values()) {
            for(Map.Entry entry:fileBitSetMap.entrySet()) {
                String fileName = (String) entry.getKey();
                BitSet bitSet = (BitSet) entry.getValue();
                int[] fileReplicationArray;
                if(replicationMap.containsKey(fileName)) {
                    fileReplicationArray = replicationMap.get(fileName);
                } else {
                    fileReplicationArray = new int[bitSet.length()];
                }
                for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
                    fileReplicationArray[i] += 1;
                }
            }
        }
    }


    public void fillPriorityQueues() {
        for(Map<String,>)
    }



//    public void insertIntoReplicationMap(String chunkIdentifier) {
//        for(Map.Entry entry: replicationMap.entrySet()) {
//            Integer replicationFactor = (Integer) entry.getKey();
//            Set<String> set = (HashSet<String>) entry.getValue();
//            if(set.contains(chunkIdentifier)) {
//                set.remove(chunkIdentifier);
//                Set<String> secondSet = (HashSet<String>) replicationMap.get(replicationFactor + 1);
//                if(secondSet == null) {
//                    replicationMap.put(replicationFactor + 1, new HashSet(Arrays.asList(chunkIdentifier)));
//                } else {
//                    secondSet.add(chunkIdentifier);
//                }
//            }
//        }
//    }

    public void insertNewFile(String fileName, int numChunks) {
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

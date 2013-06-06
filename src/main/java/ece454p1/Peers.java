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

        // Go through all entries in the received peer file map
        for(Map.Entry<String, Map<String, BitSet>> entry:receivedPeerFileMap.entrySet()) {
            String receivedRemoteHost = entry.getKey();
            Map<String, BitSet> receivedBitSetMap = entry.getValue();

            // check if out local peer file map contains a key for the address of the remote host who sent us its map
            if(peerFileMap.containsKey(receivedRemoteHost)) {

                // If so, iterate through all received filenames and bitsets, and OR them, if we didn't use to have it, insert them
                for(Map.Entry<String, BitSet> receivedBitSetEntry: receivedBitSetMap.entrySet()) {
                    String receivedFileName = receivedBitSetEntry.getKey();
                    BitSet receivedBitSet = receivedBitSetEntry.getValue();
                    if(peerFileMap.get(receivedRemoteHost).containsKey(receivedFileName)) {
                        peerFileMap.get(receivedRemoteHost).get(receivedFileName).or(receivedBitSet);
                    } else {
                        peerFileMap.get(receivedRemoteHost).put(receivedFileName, receivedBitSet);
                    }
                }
            } else {
                peerFileMap.put(receivedRemoteHost, receivedBitSetMap);
            }

            // Fill ReplicatioMap and Priority Queues
            fillReplicationMap();
            Sender.emptyPriorityQueues();
            fillPriorityQueues();
        }
    }


    public void fillReplicationMap() {
        System.out.println("fillReplicationMapCalled");
        for(Map<String, BitSet> fileNameToBitSetMap:peerFileMap.values()) {
            for(Map.Entry entry:fileNameToBitSetMap.entrySet()) {

                // For each file and its corresponding bitset
                String fileName = (String) entry.getKey();
                BitSet bitSet = (BitSet) entry.getValue();

                //find the length of the bitset
                String bitsetStr = bitSet.toString().replace("{", "").replace("}","");
                int length = 0;
                if(bitsetStr.contains(",")) {
                    length = bitsetStr.split(",").length;
                } else if(!bitsetStr.isEmpty()) {
                    length = 1;
                }
                System.out.printf("bitset size: %d\n", length);

                //Here, we wish to increment the value of a particular chunk in its fileReplicationArray
                // We check if replicationMap is already existing for the file.
                // If not, just create a new array of zeros with the length of the bitset
                // go through all the bits set to true in the bitset and increment the index of the fileReplicationArray
                int[] fileReplicationArray;
                if(replicationMap.containsKey(fileName)) {
                    fileReplicationArray = replicationMap.get(fileName);
                } else {
                    fileReplicationArray = new int[bitSet.toString().split(",").length];
                }
                for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
                    fileReplicationArray[i] += 1;
                }
                replicationMap.put(fileName, fileReplicationArray);
            }
        }
        System.out.printf("fillReplicationMap finished\n");

    }

    public void fillPriorityQueues() {
        System.out.println("fillPriorityQueues called");
        for(Map.Entry<String, int[]> entry:replicationMap.entrySet()) {
            // We go through all the filenames, and create a map of filename to replicationFactorArray
            String fileName = entry.getKey();
            int[] replicationFactorArray = entry.getValue();
            System.out.printf("filename: %s, replicationFactorArray: %s\n", fileName, Arrays.toString(replicationFactorArray));
            Map<String, BitSet> peerToBitSetMap = new HashMap<String, BitSet>();


            // PeerToBitsetMap for the current filename contains a mapping of all peer addresses to the bitsets corresponding
            // to this filename
            for(Map.Entry<String, Map<String, BitSet>> peerFileEntry: peerFileMap.entrySet()) {
                peerToBitSetMap.put(peerFileEntry.getKey(), peerFileEntry.getValue().get(fileName));
            }
            System.out.printf("filename: %s, peerToBitSetMap: %s\n", fileName, peerToBitSetMap);

            for(int i=0; i<replicationFactorArray.length; i++) {
                for(Map.Entry<String, BitSet> peerToBitSetEntry: peerToBitSetMap.entrySet()) {
                    String peerAddress = peerToBitSetEntry.getKey();
                    if(!peerToBitSetEntry.getValue().get(i)) {
                        Sender.insertChunkIntoPriorityQueue(peerAddress, fileName, i, replicationFactorArray[i]);
                    }
                }
           }
        }
        System.out.printf("fillPriorityQueues finished\n");
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
        System.out.printf("num chunks: %d\n", numChunks);
        Map<String, BitSet> localBitSetMap = peerFileMap.get(Peer.getHostAndPort());
        if(!localBitSetMap.containsKey(fileName)) {
            BitSet bitSet = new BitSet(numChunks);
            for(int i = 0;i<numChunks; i++) {
                bitSet.set(i);
            }
            for(int j=0; j<numChunks; j++) {
                if(bitSet.get(j)) {
                    System.out.print("1");
                } else {
                    System.out.print("0");
                }
            }
            System.out.print("\n");
            for(Map.Entry<String, Map<String, BitSet>> entry:peerFileMap.entrySet()) {
                if(!entry.getKey().equals(Peer.getHostAndPort())) {
                    peerFileMap.get(entry.getKey()).put(fileName, new BitSet(numChunks));
                } else {
                    peerFileMap.get(entry.getKey()).put(fileName, bitSet);
                }
            }


        }
    }

    public List<String> getOtherPeerAddresses() {
        List<String> output = new LinkedList<String>();
        for(Map.Entry<String, Map<String, BitSet>> entry:peerFileMap.entrySet()) {
            String[] split = entry.getKey().split(" ");
            if(!split[0].toLowerCase().equals(Peer.host.toLowerCase()) || !(Integer.parseInt(split[1]) == Peer.port)) {
                output.add(entry.getKey());
            }
        }
        return output;
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

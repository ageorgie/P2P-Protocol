package ece454p1;

import javax.sound.midi.SysexMessage;
import java.io.*;
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
    Map<String, Boolean> connectionStateMap = new HashMap<String, Boolean>();
    Map<String, Integer> hostToPortMap = new HashMap<String, Integer>();

    public void setConnectionState(String peerAddress, boolean connected) {
        connectionStateMap.put(peerAddress, connected);
    }

    public boolean isConnected(String peerAddress) {
        return connectionStateMap.get(peerAddress);
    }

    public void setPeerFileMap(Map<String, Map<String, BitSet>> peerFileMap) {
        this.peerFileMap = peerFileMap;
    }

    public Peers() throws IOException {
        for(String address: this.getOtherPeerAddresses()) {
            setConnectionState(address, true);
        }
    }

    public Peers(Map<String, Map<String, BitSet>> peerFileMap) throws IOException {
        this.peerFileMap = peerFileMap;
        for(String address: this.getOtherPeerAddresses()) {
            setConnectionState(address, true);
        }
    }


    public int getPort(String host) {
        return hostToPortMap.get(host.toLowerCase());
    }

    public void updatePeerFileMap(Chunk chunk) {
        System.out.printf("updatePeerFileMap: chunk %s, %d\n", chunk.fileName, chunk.chunkNum);
        BitSet bitSet = peerFileMap.get(Peer.getHostAndPort()).get(chunk.getFileName());
        System.out.printf("bitset: %s\n", bitSet);
        if(bitSet == null) {
            bitSet = new BitSet(chunk.totalNumChunks);
            bitSet.set(chunk.totalNumChunks);
        }
        bitSet.set(chunk.chunkNum);
        System.out.printf("final bitset: %s\n", bitSet);
        peerFileMap.get(Peer.getHostAndPort()).put(chunk.getFileName(), bitSet);
    }

    public void updatePeerFileMap(Map<String, Map<String, BitSet>> receivedPeerFileMap) {
//        System.out.printf("receivedpeerfilemap: %s\n", receivedPeerFileMap);
        // Go through all entries in the received peer file map
        for(Map.Entry<String, Map<String, BitSet>> entry:receivedPeerFileMap.entrySet()) {

            String receivedRemoteHost = entry.getKey().toLowerCase();
            Map<String, BitSet> receivedBitSetMap = entry.getValue();
//            System.out.printf("receivedRemoteHost: %s, receivedBitSetMap: %s \n", receivedRemoteHost, receivedBitSetMap);

            // check if out local peer file map contains a key for the address of the remote host who sent us its map
            if(peerFileMap.containsKey(receivedRemoteHost)) {

                // If so, iterate through all received filenames and bitsets, and OR them, if we didn't use to have it, insert them
                for(Map.Entry<String, BitSet> receivedBitSetEntry: receivedBitSetMap.entrySet()) {
                    String receivedFileName = receivedBitSetEntry.getKey();
                    BitSet receivedBitSet = receivedBitSetEntry.getValue();
                    System.out.printf("In for loop\n");
//                    System.out.printf("receivedRemoteHost: %s, receivedFileName: %s \n", receivedRemoteHost, receivedFileName);
                    if(peerFileMap.get(receivedRemoteHost).containsKey(receivedFileName)) {
                        BitSet bitSet = peerFileMap.get(receivedRemoteHost).get(receivedFileName);
                        bitSet.or(receivedBitSet);
                        peerFileMap.get(receivedRemoteHost).put(receivedFileName, bitSet);
                    } else {
                        peerFileMap.get(receivedRemoteHost).put(receivedFileName, receivedBitSet);
                    }
                }
            } else {
                System.out.printf("Going into else");
                peerFileMap.put(receivedRemoteHost, receivedBitSetMap);
            }

        }
//        System.out.printf("before fillreplicationmap. %s", peerFileMap);
        // Fill ReplicatioMap and Priority Queues
        fillReplicationMap();
        Sender.emptyPriorityQueues();
        fillPriorityQueues();
    }


    public void fillReplicationMap() {
        replicationMap = new HashMap<String, int[]>();
        for(Map<String, BitSet> fileNameToBitSetMap:peerFileMap.values()) {
            for(Map.Entry entry:fileNameToBitSetMap.entrySet()) {

                // For each file and its corresponding bitset
                String fileName = (String) entry.getKey();
                BitSet bitSet = (BitSet) entry.getValue();

//                System.out.printf("filename: %s, bitset:%s, bitset length: %d\n", fileName, bitSet, bitSet.length() - 1);

                //Here, we wish to increment the value of a particular chunk in its fileReplicationArray
                // We check if replicationMap is already existing for the file.
                // If not, just create a new array of zeros with the length of the bitset
                // go through all the bits set to true in the bitset and increment the index of the fileReplicationArray
                int[] fileReplicationArray;
                if(replicationMap.containsKey(fileName)) {
                    fileReplicationArray = replicationMap.get(fileName);
                } else {
                    fileReplicationArray = new int[bitSet.length() - 1];
                }
                for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
                    if(i<=fileReplicationArray.length - 1) {
                        fileReplicationArray[i] += 1;
                    }
                }
                replicationMap.put(fileName, fileReplicationArray);
            }
        }

    }

    public void fillPriorityQueues() {

        for(Map.Entry<String, int[]> entry:replicationMap.entrySet()) {
            // We go through all the filenames, and create a map of filename to replicationFactorArray
            String fileName = entry.getKey();
            int[] replicationFactorArray = entry.getValue();
            Map<String, BitSet> peerToBitSetMap = new HashMap<String, BitSet>();


            // PeerToBitsetMap for the current filename contains a mapping of all peer addresses to the bitsets corresponding
            // to this filename
            for(Map.Entry<String, Map<String, BitSet>> peerFileEntry: peerFileMap.entrySet()) {
                if(!peerFileEntry.getKey().equals(Peer.getHostAndPort())) {
                    peerToBitSetMap.put(peerFileEntry.getKey(), peerFileEntry.getValue().get(fileName));
                }
            }

//            System.out.printf("replicationFactorArray: %s\n", Arrays.toString(replicationFactorArray));
            for(int chunkNum=0; chunkNum<replicationFactorArray.length; chunkNum++) {
                for(Map.Entry<String, BitSet> peerToBitSetEntry: peerToBitSetMap.entrySet()) {
                    String peerAddress = peerToBitSetEntry.getKey();
                    if(Peer.getPeers().isConnected(peerAddress)) {
                        boolean replicationFactor = peerToBitSetEntry.getValue().get(chunkNum);
                        if(!replicationFactor) {
                            Sender.insertChunkIntoPriorityQueue(peerAddress, fileName, chunkNum, replicationFactorArray[chunkNum], replicationFactorArray.length);
                        }
                    }
                }
           }
        }

//        for(Map.Entry<String, PriorityQueue<String>> entry1: Sender.priorityQueueMap.entrySet()) {
//            String address = entry1.getKey();
//            PriorityQueue priorityQueue = entry1.getValue();
//            Iterator iterator = priorityQueue.iterator();
//            System.out.printf("priority queue for %s contains:", entry1.getKey());
//            while(iterator.hasNext()) {
//                System.out.printf("%s,", iterator.next());
//            }
//            System.out.print("\n");
//        }
    }


    public void insertNewFile(String fileName, int numChunks) {
        System.out.printf("gethostandport : %s\n", Peer.getHostAndPort());
        Map<String, BitSet> localBitSetMap = peerFileMap.get(Peer.getHostAndPort());
        System.out.printf("localbitsetmap : %s\n", localBitSetMap);
        if(!localBitSetMap.containsKey(fileName)) {
            BitSet bitSet = new BitSet(numChunks);
            for(int i = 0;i<=numChunks; i++) {
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
                    BitSet emptyBitSet = new BitSet(numChunks);
                    emptyBitSet.set(numChunks);
                    peerFileMap.get(entry.getKey()).put(fileName, emptyBitSet);
                } else {
                    peerFileMap.get(entry.getKey()).put(fileName, bitSet);
                }
            }


        }
    }

    public List<String> getOtherPeerAddresses() throws IOException {
        List<String> output = new LinkedList<String>();
        InputStream in = getClass().getClassLoader().getResourceAsStream("addresses.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.toLowerCase();
//            System.out.printf("lower case peer host: %s\n", Peer.getHost().toLowerCase());
//            System.out.printf("lower case line host: %s\n", line.split(" ")[0].toLowerCase());
            if(!Peer.getHost().toLowerCase().equals(line.split(" ")[0].toLowerCase())) {
                output.add(line);
            }
//            System.out.printf("output: %s\n", output);
//            peerFileMap.put(line, new HashMap<String, BitSet>());
        }
        br.close();
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

    public Map<String, Boolean> getConnectionStateMap() {
        return connectionStateMap;
    }


    //	public abstract int initialize(String peersFile);

//	public abstract Peer getPeer(int i);


//    public void broadcastMap() {}

//	private int numPeers;

//    HashMap<String, BitSet>[] peerBitSetArray = new HashMap[Config.MAX_PEERS];


}

package ece454p1;

import javax.sound.midi.SysexMessage;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Peers is a dumb container to hold the peers; the number of peers is fixed,
 * but needs to be set up when a peer starts up; feel free to use some other
 * container class, but that class must have a method that allows it to read the
 * peersFile, since otherwise you have no way of having a calling entity tell
 * your code what the peers are in the system.
 **/
public class Peers implements Serializable {

    Map<String, Map<String, BitSet>> peerFileMap = new HashMap<String, Map<String, BitSet>>();
    Map<String, int[]> replicationMap = new ConcurrentHashMap<String, int[]>();
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

    public boolean amAlone(){
        for(Map.Entry<String, Boolean> entry: connectionStateMap.entrySet()) {
            if(entry.getKey().equals(Peer.getHostAndPort())){
                continue;
            }
            if(entry.getValue()){
                return false;
            }
        }
        return true;
    }

    public boolean allowedToLeave() {
        if(!amAlone()){
            Map<String, BitSet> myBitSetMap = peerFileMap.get(Peer.getHostAndPort());
            for(Map.Entry<String, BitSet> entry: myBitSetMap.entrySet()) {
                String fileName = entry.getKey();
                BitSet bitSet = entry.getValue();
                int[] replicationArray = replicationMap.get(fileName);
                for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
                    if(i == bitSet.length()-1){
                        break;
                    }
                    if(replicationArray[i] <= 1) {
                        return false;
                    }

                }
            }
        }
        return true;
    }

    public int getPort(String host) {
        return hostToPortMap.get(host.toLowerCase());
    }

    public void updatePeerFileMap(Chunk chunk) {
        System.err.printf("Peers: updatePeerFileMap: chunk %s, %d\n", chunk.fileName, chunk.chunkNum);
        BitSet bitSet = peerFileMap.get(Peer.getHostAndPort()).get(chunk.fileName);
        bitSet.set(chunk.chunkNum);
        peerFileMap.get(Peer.getHostAndPort()).put(chunk.getFileName(), bitSet);
    }

    public void updatePeerFileMap(Map<String, Map<String, BitSet>> receivedPeerFileMap) {
        // Go through all entries in the received peer file map
        for(Map.Entry<String, Map<String, BitSet>> entry:receivedPeerFileMap.entrySet()) {

            String receivedRemoteHost = entry.getKey().toLowerCase();
            Map<String, BitSet> receivedBitSetMap = entry.getValue();

            // check if out local peer file map contains a key for the address of the remote host who sent us its map
            if(peerFileMap.containsKey(receivedRemoteHost)) {
                // If so, iterate through all received filenames and bitsets, and OR them, if we didn't use to have it, insert them
                for(Map.Entry<String, BitSet> receivedBitSetEntry: receivedBitSetMap.entrySet()) {
                    String receivedFileName = receivedBitSetEntry.getKey();
                    BitSet receivedBitSet = receivedBitSetEntry.getValue();
                    if(peerFileMap.get(receivedRemoteHost).containsKey(receivedFileName)) {
                        BitSet bitSet = peerFileMap.get(receivedRemoteHost).get(receivedFileName);
                        bitSet.or(receivedBitSet);
                        peerFileMap.get(receivedRemoteHost).put(receivedFileName, bitSet);
                    } else {
                        peerFileMap.get(receivedRemoteHost).put(receivedFileName, receivedBitSet);
                    }
                }
            } else {
                peerFileMap.put(receivedRemoteHost, receivedBitSetMap);
            }

        }
        // Fill ReplicatioMap and Priority Queues
        fillReplicationMap();
        Sender.emptyPriorityQueues();
        fillPriorityQueues();
    }


    public void fillReplicationMap() {
        Sender.insertPeerFileMapIntoPriorityQueue();
        replicationMap = new HashMap<String, int[]>();
        for(Map<String, BitSet> fileNameToBitSetMap:peerFileMap.values()) {
            for(Map.Entry entry:fileNameToBitSetMap.entrySet()) {

                // For each file and its corresponding bitset
                String fileName = (String) entry.getKey();
                BitSet bitSet = (BitSet) entry.getValue();

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
            Random random = new Random();
            int i = random.nextInt(getPeerAddresses().size());
            List<String> peerAddresses = new ArrayList<String>(peerToBitSetMap.keySet());
            peerAddresses.size();
            for(int chunkNum=0; chunkNum<replicationFactorArray.length; chunkNum++) {
                // The if statement here is added to ensure that you yourself have the chunk you intend so send
                if(!Peer.getPeers().getPeerFileMap().get(Peer.getHostAndPort()).get(fileName).get(chunkNum)) {
                    continue;
                }
//                for(Map.Entry<String, BitSet> peerToBitSetEntry: peerToBitSetMap.entrySet()) {
                int j = 0;
                while(j<peerAddresses.size()) {
                    String peerAddress = peerAddresses.get(i % peerAddresses.size());
                    i++;

                    if(Peer.getPeers().isConnected(peerAddress) && !peerToBitSetMap.get(peerAddress).get(chunkNum)) {
                        Sender.insertChunkIntoPriorityQueue(peerAddress, fileName, chunkNum, replicationFactorArray[chunkNum], replicationFactorArray.length);
                        break;
                    }
                    j++;
                }
//                    String peerAddress = peerToBitSetEntry.getKey();
//                    if(Peer.getPeers().isConnected(peerAddress)) {
//                        boolean replicationFactor = peerToBitSetEntry.getValue().get(chunkNum);
//                        if(!replicationFactor) {
//                            Sender.insertChunkIntoPriorityQueue(peerAddress, fileName, chunkNum, replicationFactorArray[chunkNum], replicationFactorArray.length);
//                        }
//                }
            }
       }

        }



    public void insertNewFile(String fileName, int numChunks) {
        Map<String, BitSet> localBitSetMap = peerFileMap.get(Peer.getHostAndPort());
        if(!localBitSetMap.containsKey(fileName)) {
            BitSet bitSet = new BitSet(numChunks);
            for(int i = 0;i<=numChunks; i++) {
                bitSet.set(i);
            }
            for(int j=0; j<numChunks; j++) {
                if(bitSet.get(j)) {
                    System.err.print("1");
                } else {
                    System.err.print("0");
                }
            }
            System.err.print("\n");
            for(Map.Entry<String, Map<String, BitSet>> entry:peerFileMap.entrySet()) {
                if(!entry.getKey().equals(Peer.getHostAndPort())) {
                    BitSet emptyBitSet = new BitSet(numChunks);
                    emptyBitSet.set(numChunks);
                    peerFileMap.get(entry.getKey()).put(fileName, emptyBitSet);
                } else {
                    peerFileMap.get(entry.getKey()).put(fileName, bitSet);
                }
            }
            fillReplicationMap();
            Sender.emptyPriorityQueues();
            fillPriorityQueues();
        }
    }

    public List<String> getOtherPeerAddresses() throws IOException {
        List<String> output = new LinkedList<String>();
        InputStream in = getClass().getClassLoader().getResourceAsStream("addresses.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.toLowerCase();
            if(!Peer.getHost().toLowerCase().equals(line.split(" ")[0].toLowerCase())) {
                output.add(line);
            }
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


}

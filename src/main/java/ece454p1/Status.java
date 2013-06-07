package ece454p1;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Status is the class that you populate with status data on the state of
 * replication in this peer and its knowledge of the replication level within
 * the system. The thing required in the Status object is the data as specified
 * in the private section The methods shown are examples of methods that we may
 * implement to access such data You will need to create methods to populate the
 * Status data.
 **/
public class Status {

	// This is very cheesy and very lazy, but the focus of this assignment
	// is not on dynamic containers but on the BT p2p file distribution

	/*
	 * The number of files currently in the system, 
	 * as viewed by this peer 
	 */
	private int numFiles;

	/*
	 * The fraction of the file present locally (= chunks on this peer/total
	 * number chunks in the file)
	 */
//	float[] local;
    Map<String, Float> local;

	/*
	 * The fraction of the file present in the system 
	 * (= chunks in the * system/total number chunks in the file) 
	 * (Note that if a chunk is present twice, 
	 * it doesn't get counted twice; this is simply intended to find out
	 * if we have the whole file in the system; 
	 * given that a file must be added at a peer, 
	 * think about why this number would ever not be 1.)
	 */
    Map<String, Float> system;
//	float[] system;

	/*
	 * Sum by chunk over all peers; the minimum of this number is the least
	 * replicated chunk, and thus represents the least level of 
	 * replication of  the file
	 */
//	int[] leastReplication;
    Map<String, Integer> leastReplication;

	/*
	 * Sum all chunks in all peers; 
	 * dived this by the number of chunks in the file; 
	 * this is the average level of replication of the file
	 */
    Map<String, Float> weightedLeastReplication;

    public Status() {
        this.numFiles = Peer.getFileMap().size();
        //System and local
        Map<String, Map<String, BitSet>>  bitsetMap = Peer.getPeers().getPeerFileMap() ;
        Map<String, Integer> numOfChunksInSystem = new HashMap<String, Integer>();
        Map<String, Integer> totalChunksPerFile = new HashMap<String, Integer>();
        for(Map.Entry<String, Map<String, BitSet>> entry : bitsetMap.entrySet()){
            for(Map.Entry<String, BitSet> fileBitSet : entry.getValue().entrySet()){
                if(entry.getKey().equals(Peer.getHostAndPort())){
                    float size = ((float) fileBitSet.getValue().cardinality())/((float) fileBitSet.getValue().length());
                    this.local.put(entry.getKey(), size);
                }
                int numChunks = fileBitSet.getValue().cardinality();
                if(numOfChunksInSystem.containsKey(fileBitSet.getKey())){
                    numChunks += numOfChunksInSystem.get(fileBitSet.getKey());
                }
                if(!totalChunksPerFile.containsKey(fileBitSet.getKey())){
                    totalChunksPerFile.put(fileBitSet.getKey(), (fileBitSet.getValue().length()-1));
                }
                numOfChunksInSystem.put(fileBitSet.getKey(), numChunks);
            }
        }

        for(Map.Entry<String, Integer> entry : numOfChunksInSystem.entrySet()){
            this.system.put(entry.getKey(), ((float) entry.getValue())/((float)totalChunksPerFile.get(entry.getKey())));
        }

        for(Map.Entry<String, int[]> entry : Peer.getPeers().replicationMap.entrySet()){
            int minVal = 1000000;
            for(int i : entry.getValue()){
                if(i < minVal){
                    minVal = i;
                }
            }
            this.leastReplication.put(entry.getKey(), minVal);
            this.weightedLeastReplication.put(entry.getKey(), ((float)minVal)/((entry.getValue().length)));
        }
    }
//	float[] weightedLeastReplication;

    public int numberOfFiles(){
        return numFiles;
    }

    /*Use -1 to indicate if the file requested is not present*/
    public float fractionPresentLocally(String fileName){
        Float localFraction = local.get(fileName);
        if(localFraction == null){
            return -1;
        } else {
            return localFraction;
        }
    }

    /*Use -1 to indicate if the file requested is not present*/
    public float fractionPresent(String fileName){
        Float fraction =  system.get(fileName);
        if(fraction == null){
            return -1;
        } else {
            return fraction;
        }
    }

    /*Use -1 to indicate if the file requested is not present*/
    public int minimumReplicationLevel(String fileName){
        Integer leastReplicated = leastReplication.get(fileName);
        if(leastReplicated == null){
            return -1;
        } else {
            return leastReplicated;
        }
    }

    /*Use -1 to indicate if the file requested is not present*/
    public float averageReplicationLevel(String fileName){
        Float average = weightedLeastReplication.get(fileName);
        if(average == null){
            return -1;
        } else {
            return average;
        }
    }

}

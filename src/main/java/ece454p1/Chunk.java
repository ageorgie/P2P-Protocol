package ece454p1;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Chunk implements Serializable {

    String fileName;
    int chunkNum;
    byte[] byteArray = new byte[Config.CHUNK_SIZE];
    Map<String, Map<String, BitSet>> peerFileMap;
    int totalNumChunks;
    String destination;

    public String getDestination() {
        return destination;
    }

    public Chunk(String fileName, int chunkNum, int totalNumChunks, byte[] byteArray) {
        this.fileName = fileName;
        this.chunkNum = chunkNum;
        this.totalNumChunks = totalNumChunks;
        this.byteArray = byteArray;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public void setChunkNum(int chunkNum) {
        this.chunkNum = chunkNum;
    }

    public int getTotalNumChunks() {
        return totalNumChunks;
    }

    public void setTotalNumChunks(int totalNumChunks) {
        this.totalNumChunks = totalNumChunks;
    }

    public byte[] getByteArray() {

        return byteArray;
    }

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public Map<String, Map<String, BitSet>> getPeerFileMap() {
        return peerFileMap;
    }
}

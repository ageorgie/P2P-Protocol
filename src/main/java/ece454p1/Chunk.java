package ece454p1;

import java.io.*;
import java.util.*;

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
    String destination;

    public String getDestination() {
        return destination;
    }

    public Chunk(String fileName, int chunkNum) throws IOException {
        File file = Peer.getFileMap().get(fileName);
        if(file==null) {
            throw new FileNotFoundException();
        }
        this.fileName = fileName;
        this.chunkNum = chunkNum;
        byte[] tempArray = new byte[0];
        byteArray = (Peer.synchronizedFileOperation(file, chunkNum, true, tempArray));
    }

    public Chunk(String fileName, int chunkNum, byte[]byteArray) {
        this.fileName = fileName;
        this.chunkNum = chunkNum;
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

    public byte[] getByteArray() {

        return byteArray;
    }
}

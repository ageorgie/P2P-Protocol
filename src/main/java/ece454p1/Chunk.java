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
    int totalNumChunks;

    public String getDestination() {
        return destination;
    }

    public Chunk(String fileName, int chunkNum, int totalNumChunks) throws IOException {
        this.totalNumChunks = totalNumChunks;
        File file = Peer.getFileMap().get(fileName);
        if(file==null) {
            throw new FileNotFoundException();
        }
        this.fileName = fileName;
        this.chunkNum = chunkNum;
        List<Byte> byteList = new ArrayList<Byte>();
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file.getAbsolutePath()));

        if(chunkNum!=0) {
            dataInputStream.skipBytes(Config.CHUNK_SIZE*chunkNum);
        }

        try {
            for(int i=0; i<Config.CHUNK_SIZE; i++) {
                byteList.add(dataInputStream.readByte());
            }
        } catch(EOFException e) {

        } finally {
//            System.err.printf("File %s, Chunk %s, bytelist.size: %d\n", fileName, chunkNum, byteList.size());
            byteArray = new byte[byteList.size()];
            for(int i = 0; i<byteList.size(); i++) {
                byteArray[i] = byteList.get(i).byteValue();
//                System.err.print(byteArray[i]);
            }
        }
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

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public Map<String, Map<String, BitSet>> getPeerFileMap() {
        return peerFileMap;
    }
}

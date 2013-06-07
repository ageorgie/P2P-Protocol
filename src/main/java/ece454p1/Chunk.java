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
    Byte[] byteArray;
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
        List<Byte> byteList = new ArrayList<Byte>();
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file.getAbsolutePath()));
        dataInputStream.skipBytes(Config.CHUNK_SIZE*chunkNum);
        try {
            for(int i=0; i<Config.CHUNK_SIZE; i++) {
                byteList.add(dataInputStream.readByte());
            }
        } catch(EOFException e) {
        } finally {
            byteArray = byteList.toArray(new Byte[0]);
        }

    }

    public Chunk(String fileName, int chunkNum, Byte[] byteArray) {
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

    public Byte[] getByteArray() {
        return byteArray;
    }

    public void setByteArray(Byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public Map<String, Map<String, BitSet>> getPeerFileMap() {
        return peerFileMap;
    }
}

package ece454p1;

import sun.misc.IOUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Peer and Status are the classes we really care about Peers is a container;
 * feel free to do a different container
 */
public class Peer {
    static Map<String, File> fileMap;
    static Map<String, BitSet> bitSetMap;

    static String host;
    static int port;

    private Peer(String host, int port) {
        fileMap = new HashMap<String, File>();
        bitSetMap = new HashMap<String, BitSet>();
        this.host = host;
        this.port = port;
    }

    public static String getBasePath() {
        return String.format("%s/%s", Config.basePath, port);
    }

    public static void ReceiveChunk(Chunk chunk) throws IOException {
        String fileName = chunk.getFileName();
        File file;
        if(!fileMap.containsKey(fileName)) {
            file = new File(fileName);
            fileMap.put(fileName, file);
        } else {
            file = fileMap.get(fileName);
        }

        int byteOffset = chunk.getChunkNum()*Config.CHUNK_SIZE;

        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                FileChannel ch = out.getChannel();
                ch.position(byteOffset);
                ch.write(ByteBuffer.wrap(chunk.getByteArray()));
            } finally {
                out.close();
            }
        } catch (IOException ex) {
           throw ex;
        }
    }


	// This is the formal interface and you should follow it
	public static int insert(String filePath) {

        File file = new File(filePath);
        if(!file.isFile() || !file.canRead()) {
            return -1;
        }
        int numChunks = (int) Math.ceil(file.length() / Config.CHUNK_SIZE);
        String[] splitPath = filePath.split("/");
        String fileName = splitPath[splitPath.length - 1];
        BitSet bitSet = new BitSet(numChunks);
        for(int i = 0;i< bitSet.length() ; i++) {
            bitSet.flip(i);
        }

        bitSetMap.put(fileName, bitSet);

        fileMap.put(fileName, file);
        return 0;
    };

//	public int query(Status status){};


	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
//	public int join(){};

//	public int leave(){};

	/*
	 * TODO: Feel free to hack around with the private data, 
	 * since this is part of your design.
	 * This is intended to provide some exemplars to help; 
	 * ignore it if you don't like it.
	 */

	private enum State {
		connected, disconnected, unknown
	};

	private State currentState;
	private Peers peers;

    public static void main(String[] args) {
        Peer peer = new Peer("129.97.125.24", 31422);
        peer.insert("/Users/omidmortazavi/Documents/Books/copy.pdf");
        System.out.println(peer.fileMap.get("copy.pdf"));
    }

}

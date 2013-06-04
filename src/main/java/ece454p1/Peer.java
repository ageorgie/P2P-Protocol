package ece454p1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Peer and Status are the classes we really care about Peers is a container;
 * feel free to do a different container
 */
public class Peer {
    static Map<String, File> fileMap;
    static Peers peers;
    static String host;
    static int port;
    static ExecutorService executorService = Executors.newFixedThreadPool(2);


    private Peer(String host, int port) throws IOException {
        fileMap = new HashMap<String, File>();
//        initializePeers(Config.basePath + "peerFileName.txt");

        initializePeers(getClass().getClassLoader().getResourceAsStream("addresses.txt"));
        this.host = host;
        this.port = port;
    }



    public static void initializePeers(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        Map<String, Map<String, BitSet>> bitSetMap = new HashMap<String, Map<String, BitSet>>();
        while ((line = br.readLine()) != null) {
            bitSetMap.put(line, new HashMap<String, BitSet>());
        }
        br.close();
        peers = new Peers(bitSetMap);
    }

    public static String getHostAndPort() {
        return host + " " + port;
    }

    public static String getBasePath() {
        return String.format("%s/%s", Config.basePath, port);
    }

    public static Peers getPeers() {
        return peers;
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

        peers.updatePeerFileMap(chunk);
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
        peers.insertNewFile(fileName, numChunks);
        fileMap.put(fileName, file);
        return 0;
    };

	public static int query(Status status){return -1;};


	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
	public static int join() throws IOException, InterruptedException {

        executorService.submit(new Receiver(port));
        executorService.submit(new Sender());
        return 1;
    };

	public static int leave(){
        return -1;
    };

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

    public static void main(String[] args) throws IOException, InterruptedException {
//        new Peer(args[0], Integer.parseInt(args[1]));
//        System.out.println(Peer.peers.getPeerFileMap());
//        Peer.join();

        while(true) {
            System.out.print(">>");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                executeCommand(br.readLine());
            } catch (IOException ioe) {
                System.out.println("Error while buffering input");
                System.exit(1);
            }
        }
    }

    public static void executeCommand(String command) throws IOException, InterruptedException {
        String[] split = command.split(" ");
        Command commandEnum = Command.valueOf(split[0]);
        switch (commandEnum) {
            case sethost:
                if(split.length <3) {
                    System.out.println("Please enter correct host and port.");
                } else {
                    String host = split[1];
                    int port = Integer.parseInt(split[2]);
                    new Peer(host, port);
                }
            case join:
                Peer.join();
            case leave:
                Peer.leave();
            case insert:
                Peer.insert(split[1]);
            case query:
                Peer.query(null);
            default:
                System.out.println("Command not supported");
        }

    }


     public enum Command {
         sethost,
         join,
         leave,
         insert,
         query
     }
}

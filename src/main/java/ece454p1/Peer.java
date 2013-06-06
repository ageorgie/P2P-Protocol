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
    static ExecutorService executorService = Executors.newFixedThreadPool(5);


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
            bitSetMap.put(line.toLowerCase(), new HashMap<String, BitSet>());
        }
        br.close();
        System.out.printf("initializepeers bitsetmap: %s\n", bitSetMap);
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

//        peers.updatePeerFileMap(chunk);
    }


	// This is the formal interface and you should follow it
	public static int insert(String filePath) {

        File file = new File(filePath);
        System.out.println(file);
        if(!file.isFile() || !file.canRead()) {
            System.out.println("Can't read DOG!");
            return -1;
        }
        int numChunks = (int) Math.ceil(file.length() * 1.00 / Config.CHUNK_SIZE * 1.00);
        System.out.printf("file length: %d\n", file.length());
        System.out.printf("numchunks: %d\n", numChunks );
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
        executorService.submit(new StateBroadcaster());

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

    public static Map<String, File> getFileMap() {
        return fileMap;
    }

    public static void executeCommand(String command) throws IOException, InterruptedException {
        if(command.isEmpty()) {
            return;
        }
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
                break;
            case join:
                Peer.join();
                break;
            case leave:
                Peer.leave();
                break;
            case insert:
                System.out.println("It's amazing");
                Peer.insert(split[1]);
                break;
            case query:
                Peer.query(null);
                break;
            default:
                System.out.println("Command not supported");
                break;
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

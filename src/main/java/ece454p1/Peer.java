package ece454p1;

import java.io.*;
import java.net.InetAddress;
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


    private Peer() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("addresses.txt");
        host = InetAddress.getLocalHost().getHostName();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        Map<String, Map<String, BitSet>> bitSetMap = new HashMap<String, Map<String, BitSet>>();
        while ((line = br.readLine()) != null) {
            String[] split = line.split(" ");
            if(host.equals(split[0])) {
                port = Integer.parseInt(split[1]);
            }
            bitSetMap.put(line.toLowerCase(), new HashMap<String, BitSet>());
        }
        br.close();
        peers = new Peers(bitSetMap);
        fileMap = new HashMap<String, File>();
        File theDir = new File(String.format("%s/ECE454_Downloads/%s-%d/", System.getProperty("user.home"), host, port));
        if (!theDir.exists()) theDir.mkdir();
    }


//
//    public static void initializePeers(InputStream in) throws IOException {
//        BufferedReader br = new BufferedReader(new InputStreamReader(in));
//        String line;
//        Map<String, Map<String, BitSet>> bitSetMap = new HashMap<String, Map<String, BitSet>>();
//        while ((line = br.readLine()) != null) {
//            bitSetMap.put(line.toLowerCase(), new HashMap<String, BitSet>());
//        }
//    }

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
//        System.out.printf("In ReceiveChunk for %s: %d\n", chunk.getFileName(), chunk.getChunkNum());
        String fileName = chunk.getFileName();
        File file;

        if(!fileMap.containsKey(fileName)) {
            String[] split = Peer.getHostAndPort().split(" ");
            file = new File(String.format("%s/ECE454_Downloads/%s-%s/%s", System.getProperty("user.home"), split[0], split[1], fileName));
            if(file.exists()){
                file.delete();
            }
            fileMap.put(fileName, file);
        } else {
            file = fileMap.get(fileName);
        }
        int byteOffset = chunk.getChunkNum()*Config.CHUNK_SIZE;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            try {
                raf.seek(byteOffset);
                raf.write(chunk.getByteArray());
                peers.updatePeerFileMap(chunk);
                String chunkStr = new String(chunk.byteArray);
//                System.err.println(chunkStr);
            } catch (Exception e){
                System.out.println("Error while writing to file");
            }
        } catch (IOException ex) {
           throw ex;
        }
    }


	// This is the formal interface and you should follow it
	public static int insert(String filePath) {

        File file = new File(filePath);
        if(!file.isFile() || !file.canRead()) {
//            System.out.println("Can't read DOG!");
            return -1;
        }
        int numChunks = (int) Math.ceil((file.length() * 1.0000)/(Config.CHUNK_SIZE * 1.0000));
//        System.out.printf("file length: %d\n", file.length());
//        System.out.printf("numchunks: %d\n", numChunks );
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
        new Peer();
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
//            case sethost:
//                if(split.length <3) {
//                    System.out.println("Please enter correct host and port.");
//                } else {
//                    String host = split[1];
//                    int port = Integer.parseInt(split[2]);
//                    new Peer(host, port);
//                }
//                break;
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
//         sethost,
         join,
         leave,
         insert,
         query
     }
}

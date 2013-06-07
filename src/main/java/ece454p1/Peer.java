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
    static ExecutorService executorService = Executors.newFixedThreadPool(15);
    static boolean leaveFlag = false;


    public static String getHost() {
        return host;
    }

    private Peer() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("addresses.txt");
        host = InetAddress.getLocalHost().getHostName().toLowerCase();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        Map<String, Map<String, BitSet>> peerFileMap = new HashMap<String, Map<String, BitSet>>();
        peers = new Peers();
        while ((line = br.readLine()) != null) {
            line = line.toLowerCase();
            String[] split = line.split(" ");
            if(!split[0].toLowerCase().equals(host)) {
                peers.hostToPortMap.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
                peers.setConnectionState(line.toLowerCase(), true);
            }
            if(host.toLowerCase().equals(split[0].toLowerCase())) {
                port = Integer.parseInt(split[1]);
            }
            peerFileMap.put(line.toLowerCase(), new HashMap<String, BitSet>());
        }
        br.close();
        peers.setPeerFileMap(peerFileMap);
        System.out.printf("Peer created. Host: %s Port: %d", host, port);
        fileMap = new HashMap<String, File>();
        String basePath =String.format("%s/ECE454_Downloads/", System.getProperty("user.home"));
        String path = String.format("%s/%s-%d/", basePath, host, port);
        System.out.println(path);

        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            boolean mkdirOut = baseDir.mkdir();
            System.out.printf("mkdir returned %s\n", mkdirOut);
        }
        baseDir.setReadable(true);
        baseDir.setWritable(true);
        File theDir = new File(path);
        if (!theDir.exists()) {
            boolean mkdirOut = theDir.mkdir();
            System.out.printf("mkdir returned %s\n", mkdirOut);
        }
        theDir.setReadable(true);
        theDir.setWritable(true);
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
        return host.toLowerCase() + " " + port;
    }

    public static String getBasePath() {
        return String.format("%s/%s", Config.basePath, port);
    }

    public static Peers getPeers() {
        return peers;
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
        leaveFlag = true;

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
//                System.out.println("It's amazing");
                Peer.insert(split[1]);
                break;
            case query:
                Peer.query(null);
                break;
            default:
//                System.out.println("Command not supported");
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

package ece454p1;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sender implements Callable<Integer> {

    List<Socket> sockets;

    public static void send(Socket socket, Serializable object) throws IOException {
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(object);
        oos.close();
        os.close();
    }

    public Sender() throws IOException, InterruptedException {
        List<String> peerAddresses = Peer.getPeers().getPeerAddresses();
        sockets = new LinkedList<Socket>();
        for(String peerAddress: peerAddresses) {
            String[] split = peerAddress.split(" ");
            String host = split[0];
            int port = Integer.parseInt(split[1]);
            Boolean connectionAccepted = false;
            while(!connectionAccepted) {
                try {
                    sockets.add(new Socket(host, port));
                    connectionAccepted = true;
                    System.out.printf("Connection accepted : Ready for transfer");
                } catch (ConnectException e) {
                    System.out.printf("Connection refused for %s : %d ... retrying\n", host, port);
                    Thread.sleep(5000);
                }
            }
        }
        for(Socket socket:sockets) {
            send(socket, (Serializable) Peer.getPeers().getPeerFileMap());
        }
    }

    public Integer call() throws Exception {
       try {
            while(true) {
                Thread.sleep(1000);
                for(Socket socket:sockets) {
                    send(socket, (Serializable) Peer.getPeers().getPeerFileMap());
                }
            }
       } catch(Exception e) {
            throw  e;
       } finally {
           for(Socket socket:sockets) {
               socket.close();
           }
       }
    }
}

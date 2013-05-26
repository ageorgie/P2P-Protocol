package ece454p1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SocketWorker implements Callable<Integer> {
    Socket socket;

    public SocketWorker(Socket socket) {
        this.socket = socket;
    }

    public Integer call() throws Exception {
        ObjectInputStream ois;
        Object obj = new Object();
        SocketChannel channel = socket.getChannel();
        try {
            ois = new ObjectInputStream(Channels.newInputStream(channel));
            obj = ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if(obj.getClass().isAssignableFrom(Chunk.class)) {
            Chunk chunk = (Chunk) obj;
            Peer.ReceiveChunk(chunk);
        } else if (obj.getClass().isAssignableFrom(Map.class)) {
            Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
        } else {
            throw new Exception("Received object type is not recognized");
        }

    }
}

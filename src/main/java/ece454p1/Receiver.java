package ece454p1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Receiver implements Callable<Integer> {
    ServerSocket serverSocket;
    static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Receiver(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public Integer call() throws Exception {
        try {
            while(true) {
                Socket client = serverSocket.accept();
                executorService.submit(new Updater(client));
            }
        } finally {
            serverSocket.close();
        }
    }
}

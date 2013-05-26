package ece454p1;

import sun.tools.jconsole.*;
import sun.tools.jconsole.Worker;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Listener implements Callable<Integer> {
    int port;
    ServerSocket serverSocket;

    public Listener(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    public Integer call() throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        while(true) {
            Socket socket = serverSocket.accept();
            pool.submit(new SocketWorker(socket));
        }
    }
}

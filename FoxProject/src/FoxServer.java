import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FoxServer class implements an application that accepts connection from
 * clients and receives data. This class uses {@link java.net.ServerSocket}
 * class to accept incoming connections. Every incoming connection will be
 * handled by a thread. The maximum queue length for incoming connection is 100.
 * If a connection arrives when the queue is full, the connection is refused.
 */
public class FoxServer {
    private static ExecutorService executor = null;
    private static ServerSocket serverSocket = null;
    private static ArrayList<Future<Integer>> futureList = null;

    /**
     * @param args Not used.
     */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                }catch(InterruptedException ex) {
                    Logger.getLogger(FoxServer.class.getName()).
                            log(Level.SEVERE, null, ex);
                    executor.shutdownNow();
                    try {
                        Thread.sleep(1000);
                    }catch(InterruptedException ex1) {
                        Logger.getLogger(FoxServer.class.getName()).
                                log(Level.SEVERE, null, ex1);
                    }
                }
                Future<Integer> future;
                Iterator<Future<Integer>> iterator = futureList.iterator();
                while(iterator.hasNext()) {
                    future = iterator.next();
                    try {
                        future.get(100, TimeUnit.MILLISECONDS);
                    }catch(InterruptedException ex) {
                        Logger.getLogger(FoxServer.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }catch(ExecutionException ex) {
                        Logger.getLogger(FoxServer.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }catch(TimeoutException ex) {
                        Logger.getLogger(FoxServer.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        try {
            serverSocket = new ServerSocket(50101, 50);
        }catch(IOException ioe) {
            ioe.printStackTrace();
            System.err.println(
                    "An I/O error occured when opening the socket.");
            System.exit(0);
        }catch(SecurityException se) {
            se.printStackTrace();
            System.err.println(
                    "Socket can not be opened, permission denied.");
            System.exit(0);
        }
        futureList = new ArrayList<Future<Integer>>();
        executor = Executors.newFixedThreadPool(100);

        Socket clientSocket = null;

        while(true) {
            boolean safe = false;
            try {
                clientSocket = serverSocket.accept();
                safe = true;
            }catch(IOException ioe) {
                ioe.printStackTrace();
                System.err.println(
                        "I/O error occured when waiting for a connection.");
                System.exit(0);
            }catch(SecurityException se) {
                se.printStackTrace();
                System.err.println(
                        "Connection can not be accepted, permission denied.");
                System.exit(0);
            }
            if(safe)
                futureList.add(executor.submit(
                        new FoxServiceThread(clientSocket)));
        }
    }
}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPool;
    private static final int AUTH_TIMEOUT = 10000; //ms
    //TODO must have proper sequence iterator
    private BlockingQueue<Runnable> queue;

    private static final Map<String, String> database = new HashMap<>();

    static {
        database.put("Vadim", "password");
        database.put("Helper", "i need help");
        database.put("Hater", "hate you");
        database.put("Me", "who am i?");
    }

    //TODO add more settings?
    public Server(int port, int maximumUsersAmount, int queueLength, int extra) {
        RejectedExecutionHandler handler = (r, executor) -> {
            ((ClientHandler)r).sendQueueOverflow();
        };
        try {
            serverSocket = new ServerSocket(port, maximumUsersAmount + queueLength);

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        queue = new LinkedBlockingQueue<>(queueLength);
        threadPool = new ThreadPoolExecutor(maximumUsersAmount,
                maximumUsersAmount,
                10,
                TimeUnit.MINUTES,
                queue,
                handler);
    }

    //TODO send state-codes, not strings
    @Override
    public void run() {
        System.out.println("server started");
        Thread queueChecker = new Thread(() -> {
            while (true) {
                long start = System.currentTimeMillis();
                Iterator<Runnable> it = queue.iterator();
                int queueNum = 1;
                while (it.hasNext()) {
                    ClientHandler handler = ((ClientHandler) it.next());
                    if (handler.getState() == State.NEW) {
                        handler.sendQueueNum(queueNum++);
                    }
                }
                try {
                    sleep(start + 5000 - System.currentTimeMillis());
                } catch (Exception ignored) {
                }
            }
        });
        queueChecker.start();

        while (!isInterrupted()) {
            Socket socket = null;
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                socket = serverSocket.accept();

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                long startTime = System.currentTimeMillis();

                String login = null;
                String password = null;

                while (!in.ready() || (login = in.readLine()) == null) {
                    if (System.currentTimeMillis() - startTime > AUTH_TIMEOUT) {
                        if (in.ready()) login = in.readLine();
                        break;
                    }
                }
                if (login == null) {
                    out.println("Время ожидания аутентификации истекло.");
                    throw new IOException("Время ожидания аутентификации истекло.");
                }

                while (!in.ready() || (password = in.readLine()) == null) {
                    if (System.currentTimeMillis() - startTime > AUTH_TIMEOUT) {
                        if (in.ready()) password = in.readLine();
                        break;
                    }
                }
                if (password == null) {
                    out.println("Время ожидания аутентификации истекло.");
                    throw new IOException("Время ожидания аутентификации истекло.");
                }

                //TODO attach database
                if (password.equals(database.get(login))) {
                    out.println("Вы успешно авторизованы.");
                    threadPool.execute(new ClientHandler(new User(login), in, out, socket));
                } else {
                    out.println("Неверные данные для входа.");
                    throw new IOException("Неверные данные для входа.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        queueChecker.interrupt();
        threadPool.shutdownNow().forEach((a) -> ((ClientHandler) a).sendShutdownAndClose());
        System.out.println("server stopped");
    }
}

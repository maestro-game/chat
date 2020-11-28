import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Room {
    public final int MES_BUF_SIZE = 30;
    private volatile int bufPos = 0;
    public final AtomicReferenceArray<Message> messageBuffer = new AtomicReferenceArray<>(MES_BUF_SIZE);

    private List<ClientHandler> clientHandlers = new LinkedList<>();

    private Thread queueChecker;

    private String name;

    //must have proper sequence iterator
    private BlockingQueue<Runnable> queue;
    private static RejectedExecutionHandler handler = (r, executor) -> {
        ((ClientHandler)r).sendQueueOverflow();
    };
    private ThreadPoolExecutor threadPool;

    public Room(String name, int queueLength, int maximumUsersAmount) {
        queue = new LinkedBlockingQueue<>(queueLength);
        //TODO corePoolSize may affect
        threadPool = new ThreadPoolExecutor(0,
                maximumUsersAmount,
                10,
                TimeUnit.MINUTES,
                queue,
                handler);
        queueChecker = new Thread(() -> {
            while (true) {
                long start = System.currentTimeMillis();
                Iterator<Runnable> it = queue.iterator();
                int queueNum = 1;
                while (it.hasNext()) {
                    ClientHandler handler = ((ClientHandler) it.next());
                    if (handler.getState() == Thread.State.NEW) {
                        handler.sendQueueNum(queueNum++);
                    }
                }
                try {
                    Thread.sleep(start + 5000 - System.currentTimeMillis());
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        queueChecker.start();

        this.name = name;
    }

    public int getUsersAmount() {
        return clientHandlers.size();
    }

    public int getMaximumUsersAmount() {
        return threadPool.getMaximumPoolSize();
    }

    public String getName() {
        return name;
    }

    public int getBufPos() {
        return bufPos;
    }

    public List<User> getUsers() {
        return clientHandlers.stream().map(ClientHandler::getUser).collect(Collectors.toList());
    }

    public void generateMessage(User user, String message) {
        generateMessage(user, message, false);
    }

    public void generateMessage(User user, String message, boolean isSignal) {
        synchronized (messageBuffer) {
            messageBuffer.set(bufPos, new Message(message, user, isSignal));
            if (bufPos == MES_BUF_SIZE - 1) {
                bufPos = 0;
            } else {
                bufPos++;
            }
        }
    }

    public void attachClientHandler(ClientHandler clientHandler) {
        clientHandlers.add(clientHandler);
        threadPool.execute(clientHandler);
    }

    public void shutDown() {
        queueChecker.interrupt();
        threadPool.shutdownNow().forEach((a) -> ((ClientHandler) a).sendShutdownAndClose());
        System.out.println("closed room " + name);
    }
}

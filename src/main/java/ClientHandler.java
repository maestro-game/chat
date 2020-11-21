import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ClientHandler extends Thread {

    private static final int MES_BUF_SIZE = 30;
    private static volatile int globalBufPos = 0;
    private static final AtomicReferenceArray<Message> messageBuffer = new AtomicReferenceArray<>(MES_BUF_SIZE);
    private static List<ClientHandler> list = new LinkedList<>();

    private String closeMessage;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Socket socket;
    private final User user;

    private int localBufPos;

    public ClientHandler(User user, BufferedReader in, PrintWriter out, Socket socket) {
        super("Thread-user-" + user.toString());
        this.user = user;
        this.in = in;
        this.out = out;
        this.socket = socket;
    }

    public void sendQueueNum(int num) {
        out.println("Сервер полон. Ваша позиция в очереди: " + num);
    }

    public void sendShutdownAndClose() {
        closeMessage = "Сервер отключился.";
        interrupt();
    }

    public void sendQueueOverflow() {
        out.println("Сервер и очередь переполнена. Да, тут аншлаг)");
        closeResources();
    }

    private void sendHistory() {
        synchronized (messageBuffer) {
            localBufPos = globalBufPos + 1;
            if (localBufPos == MES_BUF_SIZE) {
                localBufPos = 0;
            }
            while (localBufPos != globalBufPos) {
                if (messageBuffer.get(localBufPos) != null) {
                    out.println(messageBuffer.get(localBufPos).toString());
                }
                if (++localBufPos == MES_BUF_SIZE) {
                    localBufPos = 0;
                }
            }
        }
    }

    private void generateSignalMessage(String message) {
        synchronized (messageBuffer) {
            messageBuffer.set(globalBufPos, new Message(message, user, true));
            if (globalBufPos == MES_BUF_SIZE - 1) {
                globalBufPos = 0;
            } else {
                globalBufPos++;
            }
        }
    }

    private void actualizeMessages() {
        while (localBufPos != globalBufPos) {
            out.println(messageBuffer.get(localBufPos).toString());
            if (++localBufPos == MES_BUF_SIZE) {
                localBufPos = 0;
            }
        }
    }

    private void closeResources() {
        out.close();
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        out.println("Добро пожаловать в чат!");
        generateSignalMessage("подключился к чату.");
        sendHistory();
        while (true) {
            try {
                if (in.ready()) {
                    String text;
                    try {
                        text = in.readLine();
                    } catch (SocketException e) {
                        break;
                    }
                    if (text != null) {
                        if (text.length() > 500) {
                            out.println("Сообщение должно быть короче 500 символов");
                        } else {
                            if (text.length() == 0) {
                                out.println("Сообщение не должно быть пустым");
                            } else {
                                if (text.charAt(0) == '#') {
                                    //TODO spec fun
                                } else {
                                    synchronized (messageBuffer) {
                                        messageBuffer.set(globalBufPos, new Message(text, user));
                                        if (globalBufPos == MES_BUF_SIZE - 1) {
                                            globalBufPos = 0;
                                        } else {
                                            globalBufPos++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    out.println();
                    if (out.checkError()) {
                        break;
                    }
                }
                actualizeMessages();
                if (interrupted()) {
                    out.println(closeMessage == null ? "Вы были отключены от сервера." : closeMessage);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("disconnected " + user.toString());
        closeResources();
        generateSignalMessage("Пользователь отключён.");
    }
}

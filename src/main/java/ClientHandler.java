import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

public class ClientHandler extends Thread {
    private Room room;
    private int localBufPos;

    private static List<ClientHandler> list = new LinkedList<>();

    private String closeMessage;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Socket socket;
    private final User user;

    public User getUser() {
        return user;
    }

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
        synchronized (room.messageBuffer) {
            localBufPos = room.getBufPos() + 1;
            if (localBufPos == room.MES_BUF_SIZE) {
                localBufPos = 0;
            }
            while (localBufPos != room.getBufPos()) {
                if (room.messageBuffer.get(localBufPos) != null) {
                    out.println(room.messageBuffer.get(localBufPos).toString());
                }
                if (++localBufPos == room.MES_BUF_SIZE) {
                    localBufPos = 0;
                }
            }
        }
    }

    private void actualizeMessages() {
        while (localBufPos != room.getBufPos()) {
            out.println(room.messageBuffer.get(localBufPos).toString());
            if (++localBufPos == room.MES_BUF_SIZE) {
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
        out.println("Пользователи:");
        for (User user : room.getUsers()) {
            out.println("  " + user.getUsername());
        }
        room.generateMessage(user, "подключился к чату.", true);
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
                                    room.generateMessage(user, text);
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
        room.generateMessage(user, "Пользователь отключён.", true);
    }
}

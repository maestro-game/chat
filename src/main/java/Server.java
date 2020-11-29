import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private static final int AUTH_TIMEOUT = 500; //ms

    private final HashMap<String, Room> rooms;
    private Room defaultRoom;

    private static final Map<String, String> database = new HashMap<>();

    static {
        database.put("u1", "p1");
        database.put("u2", "p2");
        database.put("u3", "p3");
        database.put("u4", "p4");
    }

    //TODO add more settings?
    public Server(int port, int queueLength, String baseRoomName, int queueLengthForRoom, int maximumUsersAmount) {
        try {
            serverSocket = new ServerSocket(port, queueLength);
            serverSocket.setSoTimeout(100);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        rooms = new HashMap<>();
        addRoom(baseRoomName, queueLengthForRoom, maximumUsersAmount, true);
    }

    //TODO send state-codes, not strings
    @Override
    public void run() {
        System.out.println("server started");

        while (!isInterrupted()) {
            Socket socket = null;
            BufferedReader in;
            PrintWriter out;
            try {
                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    continue;
                }

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                long startTime = System.currentTimeMillis();

                String login = null;
                String password = null;

                while (System.currentTimeMillis() - startTime < AUTH_TIMEOUT) {
                    if (in.ready()) {
                        if (login == null) {
                            login = in.readLine();
                        } else {
                            password = in.readLine();
                            break;
                        }
                    }
                }
                if (password == null) {
                    out.println("Время ожидания аутентификации истекло.");
                    throw new IOException("Время ожидания аутентификации истекло.");
                }

                //TODO attach database
                if (!password.equals(database.get(login))) {
                    out.println("Неверные данные для входа.");
                    throw new IOException("Неверные данные для входа.");
                }

                out.println("Вы успешно авторизованы.");

                out.println("Комнаты:");
                for (Room room : rooms.values()) {
                    out.println("  " + room.getName() + " " + room.getUsersAmount() + "/" + room.getMaximumUsersAmount());
                }
                defaultRoom.attachClientHandler(new ClientHandler(defaultRoom, new User(login), in, out, socket));
            } catch (IOException e) {
                e.printStackTrace();
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        for (String name : rooms.keySet()) {
            rooms.get(name).shutDown();
        }
        System.out.println("server stopped");
    }

    public void addRoom(String name, int queueLength, int maximumUsersAmount, boolean defaulted) {
        Room room = new Room(name, queueLength, maximumUsersAmount);
        rooms.put(name, room);
        if (defaulted || defaultRoom == null) {
            defaultRoom = room;
        }
    }

    public void closeRoom(String name) {
        rooms.remove(name).shutDown();
    }
}

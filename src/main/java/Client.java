import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static final int CONNECTION_TIMEOUT = 60_000;//ms
    private static final int SERVER_TIMEOUT = 10_000;//ms

    private static long serverTimeout;

    private static void closeResources() {
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

    public static void main(String args[]) {
        while (true) {
            try {
                Scanner sc = new Scanner(System.in);

                System.out.println("введите хост:");
                String host = sc.nextLine();

                System.out.println("введите порт:");
                int port = Integer.parseInt(sc.nextLine());

                System.out.println("введите логин:");
                String login = sc.nextLine();

                System.out.println("введите пароль:");
                String password = sc.nextLine();

                socket = new Socket(host, port);
                System.out.println("сервер найден");

                out = new PrintWriter(socket.getOutputStream(), true);
                out.println(login);
                out.println(password);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

                long start = System.currentTimeMillis();
                String resp;
                while (!in.ready() || (resp = in.readLine()) == null) {
                    if (System.currentTimeMillis() - start > CONNECTION_TIMEOUT) {
                        System.out.println("время ожидания подключения истекло");
                    }
                }
                System.out.println(resp);
                serverTimeout = System.currentTimeMillis() + SERVER_TIMEOUT;
                while (serverTimeout > System.currentTimeMillis()) {
                    String message;
                    if (in.ready() && (resp = in.readLine()) != null) {
                        if (resp.length() != 0) {
                            System.out.println(resp);
                        }
                        serverTimeout = System.currentTimeMillis() + SERVER_TIMEOUT;
                    }
                    if (userIn.ready() && (message = userIn.readLine()) != null) {
                        out.println(message);
                    }
                }
                System.out.println("Время ожидания ответа от сервера истекло.");
            } catch (IOException e) {
                System.out.println("Не удалось подключится к серверу");
            } finally {
                closeResources();
            }
        }
    }

    public static void stop() {

    }
}

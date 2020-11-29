import java.util.Scanner;

public class Main{

    public static void main(String[] args) {
        Server server = new Server(9000, 10, "home", 2, 3);
        server.start();
        Scanner sc = new Scanner(System.in);

        String command;
        while (!(command = sc.next()).equals("stop")){
            switch (command) {
                case "new" -> server.addRoom(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextBoolean());
                case "close" -> server.closeRoom(sc.next());
            }
        }
        System.out.println("stopping server");
        server.interrupt();
    }
}
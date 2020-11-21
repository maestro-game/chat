import java.util.Scanner;

public class Main{

    public static void main(String[] args) {
        Server server = new Server(9000, 3, 10, 5);
        server.start();
        Scanner sc = new Scanner(System.in);
        while (!sc.hasNext() || !sc.next().equals("3"));
        System.out.println("stopping server");
        server.interrupt();
    }
}
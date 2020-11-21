//чисто data класс из kotlin
public class Message {
    private final boolean isSignal;
    private final String text;
    private final User from;

    public Message(String text, User from) {
        this(text, from, false);
    }

    public Message(String text, User from, boolean isSignal) {
        this.text = text;
        this.isSignal = isSignal;
        this.from = from;
    }

    public boolean isSignal() {
        return isSignal;
    }

    public String getText() {
        return text;
    }

    public User getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return (isSignal ? "[SERVER] [" : "[") + from.toString() + "] " + text;
    }
}

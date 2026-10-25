import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Lightweight stack wrapper so the deliverable includes a dedicated stack module.
public class Stack<T> {
    private final Deque<T> data = new ArrayDeque<>();

    public void push(T value) {
        data.push(value);
    }

    public T pop() {
        return data.pop();
    }

    public T peek() {
        return data.peek();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }

    @Override
    public String toString() {
        List<T> snapshot = new ArrayList<>(data);
        return snapshot.toString();
    }
}


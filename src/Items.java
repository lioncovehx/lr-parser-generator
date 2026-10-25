import java.util.*;

public class Items {
    String lhs;
    List<String> rhs;
    int dotPosition;

    public Items(String lhs, List<String> rhs, int dotPosition) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.dotPosition = dotPosition;
    }

    // This helps us print the state, e.g., Expr -> Expr . + Term
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs).append(" -> ");
        for (int i = 0; i < rhs.size(); i++) {
            if (i == dotPosition) sb.append(". ");
            sb.append(rhs.get(i)).append(" ");
        }
        if (dotPosition == rhs.size()) sb.append(".");
        return sb.toString();
    }

    // Critical for Set operations to avoid duplicate items in a state
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Items)) return false;
        Items other = (Items) o;
        return dotPosition == other.dotPosition && lhs.equals(other.lhs) && rhs.equals(other.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, dotPosition);
    }
}
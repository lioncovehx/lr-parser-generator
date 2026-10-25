import java.util.*;

public class LR1Items {
    String lhs;
    List<String> rhs;
    int dotPosition;
    String lookahead; // The specific terminal that allows a reduction

    public LR1Items(String lhs, List<String> rhs, int dotPosition, String lookahead) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.dotPosition = dotPosition;
        this.lookahead = lookahead;
    }

    // Equals and HashCode must include the lookahead!
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LR1Items)) return false;
        LR1Items other = (LR1Items) o;
        return dotPosition == other.dotPosition && 
               lhs.equals(other.lhs) && 
               rhs.equals(other.rhs) && 
               lookahead.equals(other.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, dotPosition, lookahead);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(lhs).append(" -> ");
        for (int i = 0; i < rhs.size(); i++) {
            if (i == dotPosition) sb.append(". "); // The dot representing progress
            sb.append(rhs.get(i)).append(" ");
        }
        if (dotPosition == rhs.size()) sb.append(". ");
        sb.append(", ").append(lookahead).append("]"); // The LR(1) lookahead
        return sb.toString();
    }


}
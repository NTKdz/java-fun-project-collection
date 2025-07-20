// src/Cell.java
import java.io.Serializable;

public class Cell implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization
    public boolean isMine;
    public boolean isRevealed;
    public boolean isFlagged;
    public int adjacentMines;

    public Cell() {
        this.isMine = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.adjacentMines = 0;
    }

    @Override
    public String toString() {
        if (isFlagged) return "F";
        if (!isRevealed) return "■";
        if (isMine) return "*";
        if (adjacentMines == 0) return " ";
        return String.valueOf(adjacentMines);
    }
}
package improve.project.chess.entity;

import eu.hansolo.toolboxfx.geom.Poi;

import java.util.List;

public abstract class Piece {
    private int x, y;
    private List<Point> availableMoves;
    public Piece(int x, int y) {
        this.x = x;
        this.y = y;
    }

    abstract void calculateAvailableMoves();

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public List<Point> getAvailableMoves() {
        return availableMoves;
    }

    protected void setAvailableMoves(List<Point> moves) {
        this.availableMoves = moves;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " at (" + x + ", " + y + ")";
    }
}

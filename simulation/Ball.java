class Ball {
    int x, y;
    double dx, dy;

    public Ball(int x, int y, int dx, int dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void move() {
        x += (int) dx;
        y += (int) dy;
    }
}
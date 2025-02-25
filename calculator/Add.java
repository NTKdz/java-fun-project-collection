public class Add extends Number {
    Number a;
    Number b;

    public Add(Number a, Number b) {
        super();
        this.a = a;
        this.b = b;
    }

    @Override
    public int eval() {
        return a.eval() + b.eval();
    }
}

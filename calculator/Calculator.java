public class Calculator {
    public static void main(String[] args) {
        Number number = new Number(1);
        Number number2 = new Number(2);

        Add add = new Add(number,number2);
        Add add2 = new Add(number2,number2);
        Add add3 = new Add(add,add2);
        System.out.println(add3.eval());
    }
}

import java.util.*;

public class Main {
    public static int evaluate(String expression) {
        // Convert infix expression to postfix (Reverse Polish Notation)
        List<String> postfix = infixToPostfix(expression);

        // Evaluate the postfix expression
        return evaluatePostfix(postfix);
    }

    // Convert infix to postfix using Shunting Yard Algorithm
    private static List<String> infixToPostfix(String expression) {
        Stack<Character> operators = new Stack<>();
        List<String> output = new ArrayList<>();
        StringBuilder numberBuffer = new StringBuilder();

        for (char c : expression.toCharArray()) {
            if (Character.isDigit(c)) {
                numberBuffer.append(c);
            } else {
                if (!numberBuffer.isEmpty()) {
                    output.add(numberBuffer.toString());
                    numberBuffer.setLength(0);
                }

                if (c == '(') {
                    operators.push(c);
                } else if (c == ')') {
                    while (!operators.isEmpty() && operators.peek() != '(') {
                        output.add(String.valueOf(operators.pop()));
                    }
                    operators.pop(); // Remove '('
                } else if ("+-*/".indexOf(c) != -1) {
                    while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(c)) {
                        output.add(String.valueOf(operators.pop()));
                    }
                    operators.push(c);
                }
            }
        }

        if (!numberBuffer.isEmpty()) {
            output.add(numberBuffer.toString());
        }

        while (!operators.isEmpty()) {
            output.add(String.valueOf(operators.pop()));
        }

        return output;
    }

    // Evaluate postfix expression
    private static int evaluatePostfix(List<String> postfix) {
        System.out.println(postfix);
        Stack<Integer> stack = new Stack<>();
        for (String token : postfix) {
            if (token.matches("\\d+")) {
                stack.push(Integer.parseInt(token));
            } else {
                int b = stack.pop();
                int a = stack.pop();
                switch (token.charAt(0)) {
                    case '+': stack.push(a + b); break;
                    case '-': stack.push(a - b); break;
                    case '*': stack.push(a * b); break;
                    case '/': stack.push(a / b); break;
                }
            }
        }
        return stack.pop();
    }

    // Define operator precedence
    private static int precedence(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }

    public static void main(String[] args) {
        String expression = "9+ (1 + 2)*3+9";
        System.out.println(expression);
        expression = expression.replaceAll("\\s", ""); // Remove spaces
        int result = evaluate(expression);
        System.out.println("Result: " + result);
    }
}

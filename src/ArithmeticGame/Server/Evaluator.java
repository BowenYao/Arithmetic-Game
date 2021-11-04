package ArithmeticGame.Server;

import java.util.Arrays;
import java.util.Stack;

public class Evaluator {
    //This class parses a string and evaluates it as an arithmetic expression
    //Also makes sure the expression uses the correct set of numbers
    boolean onTarget=false,numSetCompliant=false;
    Fraction target;
    Integer[] numSet;

    //constructors
    public Evaluator(){
        target = new Fraction(24);
        numSet = new Integer[]{1,1,1,8};
    }
    public Evaluator(Fraction target, Integer[] numSet){
        this.target = target;
        this.numSet = numSet;
    }

    public static boolean isOperator(char c){
        return c=='+'||c=='-'||c=='*'||c=='/';
    }
    private static Fraction calculate(Fraction a, Fraction b, char op){
        switch(op){
            case '+': return a.add(b);
            case '-': return a.subtract(b);
            case '/': return a.divide(b);
            case '*': return a.multiply(b);
            default: throw new IllegalArgumentException(op + " isn't an included operator"); //Default statement should be unreachable
        }
    }
    public Fraction evaluate(String input) throws IllegalArgumentException{
        //evaluates string in a single pass using two stacks
        Stack<Character> charStack = new Stack<>();
        Stack<Fraction> numStack = new Stack<>();

        //Numbers count and numbers used to determine if solution is numset compliant
        //if the number of numbers used is correct and every number is used then we are compliant
        int numbersCount=0;
        Integer[] numbersUsed = new Integer[numSet.length];

        boolean opPrev =false; //opPrev provides a bit of garbage protection by checking if there are two operators in a row
        for(int i = 0;i < input.length(); i++){

            char c = input.charAt(i);

            if(c=='('){
                // ( characters are pushed onto the operator stack to be evaluated when a ) is found
                charStack.push(c);
                opPrev = false;
            }else if(Character.isDigit(c)) {
                // if a character is a digit find the end of the integer and parse the whole integer
                int j = i + 1;
                while (j < input.length() && Character.isDigit(input.charAt(j))) {
                    j++;
                }
                int number = Integer.parseInt(input.substring(i, j));

                System.out.println("Number: " + number);

                //If we haven't already used too many numbers add the current number to our used number set
                if (numbersCount < numbersUsed.length)
                    numbersUsed[numbersCount] = number;
                numbersCount++;

                if (!charStack.isEmpty() && (charStack.peek() == '*' || charStack.peek() == '/')) {
                    //If the last operator we parsed was multiplication or division aka a 'priority' operator
                    // we can evaluate it right now and push the result on the stack
                    numStack.push(calculate(numStack.pop(), new Fraction(number), charStack.pop()));
                } else if (!charStack.isEmpty() && charStack.peek() == '-') {
                    //We convert subtraction into adding the negative since we evaluate addition right to left
                    //when popping from our stack and this prevents order issues
                    charStack.pop();
                    charStack.push('+');
                    numStack.push(new Fraction(-number));
                } else
                    numStack.push(new Fraction(number)); //otherwise just push the number to be evaluated later
                i = j - 1;
                opPrev = false;
            }
            else if (isOperator(c)) {
                if(opPrev) //If we have two operators in a row throw exception
                    throw new IllegalArgumentException("Malformed function");
                charStack.push(c); //we can't evaluate operators until we have the number after it so push to stack for now
                opPrev = true;
            }
            else if(c==')'){
                if(opPrev) //If we have an operator and then a ) throw exception
                    throw new IllegalArgumentException("Malformed function");

                //Once we find a ) we can pop operators and numbers off our stack until we find the corresponding (
                char op = charStack.pop();
                while(op!='('){
                    Fraction b = numStack.pop();
                    Fraction a = numStack.pop();
                    numStack.push(calculate(a,b,op));
                    if(charStack.isEmpty())//If we have too many )s charStack will be prematurely empty and throw exception
                        throw new IllegalArgumentException("Parenthesis Mismatch");
                    op = charStack.pop();
                }
                //If the preceding operation to a ( was a priority operation we can evaluate right away continuing until we reach a + or -
                while(!charStack.isEmpty()&&(charStack.peek()=='*'||charStack.peek()=='/')){
                    Fraction b = numStack.pop();
                    Fraction a = numStack.pop();
                    numStack.push(calculate(a,b,charStack.pop()));
                }
            }else if(c==' '){
                //Do nothing
            }else
                throw new IllegalArgumentException(input + " is not recognized by the parser");
        }
        //After we reach the end of the string only addition operators should remain
        //We can pop the operator and number stacks to calculate the final result
        while(!charStack.isEmpty()){
            char op = charStack.pop();
            if(!isOperator(op)) //If there are too many (s they will remain until the string is done parsing and throw exception
                throw new IllegalArgumentException("Parenthesis Mismatch");
            Fraction b = numStack.pop();
            Fraction a = numStack.pop();
            numStack.push(calculate(a,b,op));
        }
        //verifies if we are numset compliant
        if(numbersCount==numbersUsed.length) {
            Arrays.sort(numbersUsed);
            numSetCompliant = Arrays.equals(numbersUsed,numSet);
            System.out.println(Arrays.toString(numbersUsed) + " | " + Arrays.toString(numSet));
        }
        Fraction result = numStack.pop();
        onTarget = result.equals(target);

        return result;
    }

    //Getters and setters
    public boolean isOnTarget(){return onTarget;}
    public boolean isNumSetCompliant(){return numSetCompliant;}
    public Fraction getTarget(){return target;}

    public void setTarget(Fraction target){
        this.target = target;
    }
    //Reminder that numSet needs to be sorted
    public void setNumSet(Integer[] numSet){
        Arrays.sort(numSet);
        this.numSet=numSet;
    }
    public void setNumSet(int[] numSet){
        Arrays.sort(numSet);
        this.numSet = new Integer[numSet.length];
        for(int i = 0; i < numSet.length;i++)
        {
            this.numSet[i]=numSet[i];
        }
    }
}

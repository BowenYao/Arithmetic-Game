package com.company;

import java.util.Arrays;
import java.util.Stack;

public class Evaluator {
    boolean onTarget=false,numSetCompliant=false;
    Fraction target;
    Integer[] numSet;
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
            default: throw new IllegalArgumentException(op + " isn't an included operator");
        }
    }
    public Fraction evaluate(String input){
        Stack<Character> charStack = new Stack<>();
        Stack<Fraction> numStack = new Stack<>();
        int numbersCount=0;
        Integer[] numbersUsed = new Integer[numSet.length];
        for(int i = 0;i < input.length(); i++){
            char c = input.charAt(i);
            if(c=='('){
                charStack.push(c);
            }else if(Character.isDigit(c)){
                int j = i+1;
                while(j < input.length() && Character.isDigit(input.charAt(j))){
                    j++;
                }
                int number = Integer.parseInt(input.substring(i,j));
                System.out.println("Number: "+ number);
                if(numbersCount<numbersUsed.length)
                    numbersUsed[numbersCount]=number;
                numbersCount++;
                    if (!charStack.isEmpty()&&(charStack.peek() == '*' || charStack.peek() == '/')) {
                        numStack.push(calculate(numStack.pop(), new Fraction(number), charStack.pop()));
                    }else if(!charStack.isEmpty()&&charStack.peek()=='-'){
                        charStack.pop();
                        charStack.push('+');
                        numStack.push(new Fraction(-number));
                    }else
                        numStack.push(new Fraction(number));
                i=j-1;
            }
            else if (isOperator(c)) {
                charStack.push(c);
            }
            else if(c==')'){
                char op = charStack.pop();
                while(op!='('){
                    Fraction b = numStack.pop();
                    Fraction a = numStack.pop();
                    numStack.push(calculate(a,b,op));
                    op = charStack.pop();
                }
                while(!charStack.isEmpty()&&(charStack.peek()=='*'||charStack.peek()=='/')){
                    Fraction b = numStack.pop();
                    Fraction a = numStack.pop();
                    numStack.push(calculate(a,b,charStack.pop()));
                }
            }else if(c==' '){
                //Do nothing
            }else
                throw new IllegalArgumentException(c + " is not recognized by the parser");
        }
        while(!charStack.isEmpty()){
            char op = charStack.pop();
            System.out.println(op);
            Fraction b = numStack.pop();
            Fraction a = numStack.pop();
            System.out.println(a + " " + b);
            numStack.push(calculate(a,b,op));
        }
        if(numbersCount==numbersUsed.length) {
            Arrays.sort(numbersUsed);
            numSetCompliant = Arrays.equals(numbersUsed,numSet);
            System.out.println(Arrays.toString(numbersUsed) + " | " + Arrays.toString(numSet));
        }
        Fraction result = numStack.pop();
        onTarget = result.equals(target);
        return result;
    }
    public Fraction getTarget(){return target;}
    public boolean isOnTarget(){return onTarget;}
    public boolean isNumSetCompliant(){return numSetCompliant;}
    public void setTarget(Fraction target){
        this.target = target;
    }
    public void setNumSet(Integer[] numSet){this.numSet=numSet;}
    public void setNumset(int[] numSet){
        this.numSet = new Integer[numSet.length];
        for(int i = 0; i < numSet.length;i++)
        {
            this.numSet[i]=numSet[i];
        }
    }
}

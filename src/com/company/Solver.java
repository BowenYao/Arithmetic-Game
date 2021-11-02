package com.company;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Solver {
    /*private static <Generic extends Comparable<Generic>> Generic[] minmax(Generic a, Generic b){
        Generic[] array = Array.newInstance(Generic.class, 2);
        Generic[] generics = a.compareTo(b) == 1 ?(Generic[]) Array.newInstance(Generic,2) : new Generic[]{a, b};
        return generics;
    }*/
    private static int[] minmax(int a, int b){
        return a > b ? new int[]{b,a}:new int[]{a,b};
    }
    private static Fraction[] minmax(Fraction a,Fraction b){
        return a.compareTo(b)==1 ? new Fraction[]{b,a}:new Fraction[]{a,b};
    }
    public static Set<Fraction> combineTwo(Fraction a, Fraction b){
        HashSet<Fraction> twoSet = new HashSet<Fraction>();
        if(b.equals(0)) {
          //  Fraction[] minmax = minmax(a,a.negate());
            twoSet.add(a);
            twoSet.add(a.negate());
            twoSet.add(Fraction.ZERO);
           // return new Fraction[]{minmax[0],Fraction.ZERO,minmax[1]};
        }else if(a.equals(0)){
            twoSet.add(b);
            twoSet.add(b.negate());
            twoSet.add(Fraction.ZERO);
        }
        else{
            twoSet.add(a.add(b));
          //  System.out.println(a.add(b));
            twoSet.add(a.subtract(b));
          //  System.out.println(a.subtract(b));
            twoSet.add(b.subtract(a));
          //  System.out.println(b.subtract(a));
            twoSet.add(a.multiply(b));
            twoSet.add(a.divide(b));
            twoSet.add(b.divide(a));
        }
       // Fraction[] numSet = new Fraction[]{a.add(b),a.subtract(b),b.subtract(a),a.multiply(b),a.divide(b),b.divide(a)};
        //Arrays.sort(numSet);
        return twoSet;
    }
    private static Set<Fraction> ping(Fraction[] numbers, HashMap<Fraction[],Set<Fraction>> hashMap){
        if(hashMap.containsKey(numbers))
            return hashMap.get(numbers);
        else if(numbers.length==2) {
            Set<Fraction> twoSet = combineTwo(numbers[0],numbers[1]);
            hashMap.put(numbers,twoSet);
            return twoSet;
        }else{
            Set<Fraction> resultSet = new HashSet<Fraction>();
            for(int i = 0; i < numbers.length;i++){
                for(int j = i+1;j<numbers.length;j++){
                    Set<Fraction> twoSet = ping(new Fraction[]{numbers[i],numbers[j]},hashMap);
                    for(Fraction newNum:twoSet){
                        Fraction[] reducedList = new Fraction[numbers.length-1];
                        int count = 0;
                        boolean notPlaced = true;
                        int x = 0;
                        if(i == 0){
                            if(j == 1)
                                x=2;
                            else
                                x=1;
                        }
                        while(x<numbers.length){
                            if(notPlaced && numbers[x].compareTo(newNum) > 0){
                                reducedList[count]=newNum;
                                notPlaced = false;
                                count++;
                            }
                         //   System.out.println(i+" " + j);
                           // System.out.println(numbers[x]+" : " + x);
                            reducedList[count]=numbers[x];
                            count++;
                            x++;
                            if(x==i)
                                x++;
                            if(x==j)
                                x=j+1;
                        }
                        if(notPlaced)
                            reducedList[count]=newNum;
                        resultSet.addAll(ping(reducedList,hashMap));
                    }
                }
            }
            hashMap.put(numbers,resultSet);
            return resultSet;
        }
    }
    public static boolean solve(int[] numbers,int target){
        return solve(numbers,new Fraction(target));
    }
    public static boolean solve(int[] numbers, Fraction target){
        Arrays.sort(numbers);
        HashMap<Fraction[],Set<Fraction>> hashMap = new HashMap<>();
        Fraction[] fractions = Fraction.toFraction(numbers);
        Set<Fraction> solutions = ping(fractions,hashMap);
        return solutions.contains(target);
    }
}

package ArithmeticGame.Server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/*This class does the calculations to determine if a number set is solvable for a given target
//TODO: Currently this solver only returns if the set is solvable and doesn't provide an actual solution which could be nice
   this is pretty painful to implement and computationally expensive however
    On that note this solver can definitely be optimized but i'm not smart enough to figure it out yet
    Parallelization could be an option? The problem is upper bounded by https://oeis.org/A140606 and it's definitely NP*/
public class Solver {
    /*The core concept of this solver is that a list of N numbers can be reduced to N-1 numbers by performing an operation on two of them
    this generates a branching tree of different number sets which can be explored and reduced until all the solutions are found
    There are certain solutions which can only be determined by fractions which is why all numbers are stored as Fractions*/

    //combineTwo takes two numbers and returns the set of numbers which can be made with the numbers
    public static Set<Fraction> combineTwo(Fraction a, Fraction b){
        HashSet<Fraction> twoSet = new HashSet<>(); //HashSet used to prevent duplicate numbers
        //0 cases to prevent divide by zero error
        if(b.equals(0)) {
            twoSet.add(a);
            twoSet.add(a.negate());
            twoSet.add(Fraction.ZERO);
        }else if(a.equals(0)){
            twoSet.add(b);
            twoSet.add(b.negate());
            twoSet.add(Fraction.ZERO);
        }
        else{
            twoSet.add(a.add(b));
            twoSet.add(a.subtract(b));
            twoSet.add(b.subtract(a));
            twoSet.add(a.multiply(b));
            twoSet.add(a.divide(b));
            twoSet.add(b.divide(a));
        }
        return twoSet;
    }
    //For lack of a better term the main recursive body is named "ping"
    //A hashmap is used to store solution sets that have already been calculated to save some computation time
    //Because of this the number set needs to be kept in sorted order so that all number sets with the same numbers are the same
    //possible FIXME: would an actual set save time? I remember there was a reason I decided on an array. Faster access time?
    private static Set<Fraction> ping(Fraction[] numbers, HashMap<Fraction[],Set<Fraction>> hashMap){
        if(hashMap.containsKey(numbers)) //If a specific number set has already been calculated return it
            return hashMap.get(numbers);
        else if(numbers.length==2) {
            //base case combineTwo call
            Set<Fraction> twoSet = combineTwo(numbers[0],numbers[1]);
            hashMap.put(numbers,twoSet);
            return twoSet;
        }else{
            //If numbers array has more than two numbers generate every possible combination of two numbers and reduce accordingly
            Set<Fraction> resultSet = new HashSet<>();
            for(int i = 0; i < numbers.length;i++){
                for(int j = i+1;j<numbers.length;j++){
                    //i and j are the indices of the two combining numbers
                    Set<Fraction> twoSet = ping(new Fraction[]{numbers[i],numbers[j]},hashMap);
                    //twoSet is the set generated by combineTwo() we call ping so that we don't have to recalculate if we've already stored it in our hashmap
                    for(Fraction newNum:twoSet){
                        //For every number in our new set create a new 'reduced list' while maintaining sorted order
                        Fraction[] reducedList = new Fraction[numbers.length-1];
                        int count = 0;//count maintains the index of the new array
                        boolean notPlaced = true;
                        int x = 0; //x is the index of the old array

                        //we need to skip over the 'removed' portion of the old arrays.
                        //these statements handle if the removed numbers include the first or second indices
                        if(i == 0){
                            if(j == 1)
                                x=2;
                            else
                                x=1;
                        }

                        while(x<numbers.length){
                            if(notPlaced && numbers[x].compareTo(newNum) > 0){
                                //if we find the place our new number belongs place it and change notPlaced to false
                                reducedList[count]=newNum;
                                notPlaced = false;
                                count++;
                            }
                            //otherwise place the current number in our new array
                            reducedList[count]=numbers[x];
                            count++;
                            x++;

                            //skip over i and j
                            if(x==i)
                                x++;
                            if(x==j)
                                x=j+1;
                        }
                        //if not placed yet put our new number at the end of hte array
                        if(notPlaced)
                            reducedList[count]=newNum;
                        resultSet.addAll(ping(reducedList,hashMap));
                        //Add all the recursive results of our reduced list to our set of solutions
                        // and move to the next pair of numbers
                    }
                }
            }
            hashMap.put(numbers,resultSet); //After we collect all the numbers store in hashmap so we can ping it later if we need it
            return resultSet;
        }
    }
    public static boolean solve(int[] numbers,int target){
        return solve(numbers,new Fraction(target));
    }

    public static boolean solve(int[] numbers, Fraction target){
        //runs our recursive algorithm to get all solutions and sees if our target is in the solution set
        Arrays.sort(numbers);
        HashMap<Fraction[],Set<Fraction>> hashMap = new HashMap<>();
        Fraction[] fractions = Fraction.toFraction(numbers);
        Set<Fraction> solutions = ping(fractions,hashMap);
        return solutions.contains(target);
    }
}
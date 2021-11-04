package ArithmeticGame.Server;


public class Fraction implements Comparable<Fraction> {
    //This class stores a fraction as two integer and defines all arithmetic behavior for fractions
    //All fractions are reduced to make things simpler
    public static Fraction ZERO = new Fraction(0,1);
    private final int numerator;
    private final int denominator;

    //Static function which converts an integer array to a fraction array
    public static Fraction[] toFraction(int[] integerArr){
        Fraction[] fractionArr = new Fraction[integerArr.length];
        for(int i = 0; i < integerArr.length; i++){
            fractionArr[i] = new Fraction(integerArr[i]);
        }
        return fractionArr;
    }

    //Unused default constructor. Not useful since Fractions are not mutable
    public Fraction() {
        this.numerator = 1;
        this.denominator = 1;
    }
    //Creates fraction from int. NOTE: this is preferable over Fraction(integer,1) since no reducing is done
    public Fraction(int integer){
        this.numerator=integer;
        this.denominator=1;
    }
    //Creates fraction from a numerator and denominator
    public Fraction(int numerator,int denominator){
        if(denominator==0)
            throw new IllegalArgumentException("Denominator cannot be zero");
        //Following ensures that the denominator is never negative which makes keeping track of the sign simpler
        if(denominator<0) {
            numerator *= -1;
            denominator *=-1;
        }
        //reduces fraction
        int[] reduced = reduce(numerator, denominator);
        this.numerator = reduced[0];
        this.denominator = reduced[1];
    }
    //gcd function using Euclid's algorithm https://en.wikipedia.org/wiki/Euclidean_algorithm
    private static int gcd(int a, int b){
        while(b!=0){
            int temp = b;
            b= a%b;
            a= temp;
        }
        return Math.abs(a);
    }
    //lcm(a,b) = |a*b|/gcd(a,b)
    private static int lcm(int a, int b){
        if(a==0&&b==0)
            return 0;
        a = Math.abs(a);
        b = Math.abs(b);
        return (a/gcd(a,b))*b;
    }
    //reduce function returns resulting numerator and denominator as an array
    private int[] reduce(int numerator, int denominator) {
        int gcd = gcd(numerator, denominator);
        return new int[]{numerator / gcd, denominator / gcd};
    }

    //Returns a new Fraction that is the sum of this and the passed fraction
    public Fraction add(Fraction fraction){
        if(denominator==fraction.denominator)
            return new Fraction(numerator+fraction.numerator,denominator);
        int lcm = lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2=lcm/fraction.denominator;
        return new Fraction(numerator*m1+fraction.numerator*m2,lcm);
    }
    //Subtracts passed fraction from this and returns new fraction
    public Fraction subtract(Fraction fraction){
        if(denominator==fraction.denominator)
            return new Fraction(numerator-fraction.numerator,denominator);
        int lcm = lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2 = lcm/fraction.denominator;
        return new Fraction(numerator*m1-fraction.numerator*m2,lcm);
    }
    /*TODO: It's possible that it's slightly more efficient for add and subtract
        to just multiply both fractions by the product of the denominators and save
        one gcd calculation but this results in smaller numbers so I'm not sure */

    public Fraction multiply(Fraction fraction){
        return new Fraction(numerator*fraction.numerator,denominator*fraction.denominator);
    }

    public Fraction divide(Fraction fraction){
        return new Fraction(numerator*fraction.denominator,denominator*fraction.numerator);
    }

    public Fraction negate(){
        return new Fraction(-numerator,denominator);
    }

    //We need to override hashCode and equals functions to create proper HashMaps and the like
    @Override
    public int hashCode(){
        final int prime = 17;
        int hash = 29;
        hash=prime*hash+numerator;
        hash = prime*hash+denominator;
        return hash;
    }
    @Override
    public boolean equals(Object o){
        if(this==o)
            return true;
        if(o == null)
            return false;
        if(o.getClass()==this.getClass()){
            Fraction other = (Fraction) o;
            return numerator == other.numerator && denominator== other.denominator;
        }else if(o.getClass()==Integer.class){ //Provides functionality for Fraction.equals(Integer) which I actually use once or twice
            int other = (int) o;
            return numerator==other&&denominator==1;
        }
        return false;
    }

    //We need to override compareTo for Arrays.sort()
    @Override
    public int compareTo(Fraction fraction){
        if(this.equals(fraction))
            return 0;
        int lcm =  lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2 = lcm/fraction.denominator;
        return m1*numerator>m2*fraction.numerator ? 1:0;
    }

    //Simple toString override
    @Override
    public String toString(){
        if(denominator==1)
            return numerator+"";
        return numerator+"/"+denominator;
    }
}

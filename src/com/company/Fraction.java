package com.company;


public class Fraction implements Comparable<Fraction> {
    public static Fraction ZERO = new Fraction(0,1);
    private final int numerator;
    private final int denominator;
    public Fraction() {
        this.numerator = 1;
        this.denominator = 1;
    }
    public Fraction(int integer){
        this.numerator=integer;
        this.denominator=1;
    }
    public Fraction(int numerator,int denominator){
        if(denominator==0)
            throw new IllegalArgumentException("Denominator cannot be zero");
        if(denominator<0) {
            numerator *= -1;
            denominator *=-1;
        }
        int[] reduced = reduce(numerator, denominator);
        this.numerator = reduced[0];
        this.denominator = reduced[1];
    }
    public Fraction add(Fraction fraction){
        if(denominator==fraction.denominator)
            return new Fraction(numerator+fraction.numerator,denominator);
        int lcm = lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2=lcm/fraction.denominator;
        return new Fraction(numerator*m1+fraction.numerator*m2,lcm);
    }
    public Fraction subtract(Fraction fraction){
        if(denominator==fraction.denominator)
            return new Fraction(numerator-fraction.numerator,denominator);
        int lcm = lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2 = lcm/fraction.denominator;
        return new Fraction(numerator*m1-fraction.numerator*m2,lcm);
    }
    public Fraction multiply(Fraction fraction){
        return new Fraction(numerator*fraction.numerator,denominator*fraction.denominator);
    }
    public Fraction divide(Fraction fraction){
        return new Fraction(numerator*fraction.denominator,denominator*fraction.numerator);
    }
    public Fraction negate(){
        return new Fraction(-numerator,denominator);
    }
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
        }else if(o.getClass()==Integer.class){
            int other = (int) o;
            return numerator==other&&denominator==1;
        }
        return false;
    }
    @Override
    public String toString(){
        if(denominator==1)
            return numerator+"";
        return numerator+"/"+denominator;
    }
    @Override
    public int compareTo(Fraction fraction){
        if(this.equals(fraction))
            return 0;
        int lcm =  lcm(denominator,fraction.denominator);
        int m1 = lcm/denominator,m2 = lcm/fraction.denominator;
        return m1*numerator>m2*fraction.numerator ? 1:0;
    }
    private int[] reduce(int numerator, int denominator) {
        int gcd = gcd(numerator, denominator);
        return new int[]{numerator / gcd, denominator / gcd};
    }
    private static int gcd(int a, int b){
        while(b!=0){
            int temp = b;
            b= a%b;
            a= temp;
        }
        return Math.abs(a);
    }
    private static int lcm(int a, int b){
        if(a==0&&b==0)
            return 0;
        a = Math.abs(a);
        b = Math.abs(b);
        return (a/gcd(a,b))*b;
    }
    public static Fraction[] toFraction(int[] integerArr){
        Fraction[] fractionArr = new Fraction[integerArr.length];
        for(int i = 0; i < integerArr.length; i++){
            fractionArr[i] = new Fraction(integerArr[i]);
        }
        return fractionArr;
    }
}

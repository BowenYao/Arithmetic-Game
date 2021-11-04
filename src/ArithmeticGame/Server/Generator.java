package ArithmeticGame.Server;

public class Generator {
    //This is a simple class which randomly generates a number set of given size
    private int low,high,size=4;
    public Generator(){
        low = 1;
        high=13;
    }
    public Generator(int low,int high){
        this.low = low;
        this.high =high;
    }
    public Generator(int low, int high, int size){
        this.low = low;
        this.high = high;
        this.size = size;
    }
    public int[] generate(){
        int[] numSet = new int[size];
        for(int i = 0; i < size; i++){
            numSet[i] = (int) (Math.random()*(high-low)+low);
        }
        return numSet;
    }

    public int getLow(){
        return low;
    }
    public int getHigh(){
        return size;
    }
    public int getSize(){
        return size;
    }
}

package aqua.common.msgtypes;

import java.io.Serializable;

public class SnapshotSum implements Serializable {
    private int fishSum;

    public SnapshotSum (int fishSum) {
        this.fishSum = fishSum;
    }

    public int getFishSum(){return fishSum;}

    public void setFishSum(int fishSum) {this.fishSum = fishSum;}
}

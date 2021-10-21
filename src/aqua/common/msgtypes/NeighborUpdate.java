package aqua.common.msgtypes;
import aqua.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {
    private Direction direction;
    private String neighbor;

    public NeighborUpdate(Direction direction, String neighbor) {
        this.direction = direction;
        this.neighbor = neighbor;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getNeighbor() {
        return neighbor;
    }

    public void setDirection(Direction direction){
        this.direction = direction;
    }

    public void setNeighbor(String neighbor){
        this.neighbor=neighbor;
    }

}

package aqua.common.msgtypes;
import aqua.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {
    private final InetSocketAddress neighbor;
    private final Direction direction;


    public NeighborUpdate(InetSocketAddress neighbor, Direction direction) {
        this.neighbor = neighbor;

        this.direction = direction;
    }

    public InetSocketAddress getNeighbor() {
        return neighbor;
    }

    public  Direction getDirection() {return direction;}
}

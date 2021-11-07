package aqua.common.msgtypes;
import aqua.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighbourUpdate implements Serializable {
    private final InetSocketAddress neighbour;
    private final Direction direction;


    public NeighbourUpdate(InetSocketAddress neighbour, Direction direction) {
        this.neighbour = neighbour;
        this.direction = direction;
    }

    public InetSocketAddress getNeighbour() {
        return neighbour;
    }

    public Direction getDirection() {return direction;}
}

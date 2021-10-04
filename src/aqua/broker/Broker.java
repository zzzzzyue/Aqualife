package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.DeregisterRequest;
import aqua.common.msgtypes.HandoffRequest;
import aqua.common.msgtypes.RegisterRequest;
import aqua.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

public class Broker {

    Endpoint endpoint = new Endpoint(4711);
    ClientCollection clients = new ClientCollection();

    public void broker(){
        Boolean finish = false;

        while(!finish){
            Message message = endpoint.blockingReceive();
            if(message.getPayload() instanceof RegisterRequest){
                register(message);
            }

            if(message.getPayload() instanceof DeregisterRequest){
                deregister(message);
            }

            if(message.getPayload() instanceof HandoffRequest){
                HandoffRequest handoffRequest = (HandoffRequest) message.getPayload();
                InetSocketAddress inetSocketAddress = message.getSender();
                handoff(handoffRequest, inetSocketAddress);
            }

        }

    }

    public void register(Message msg){
        String name = "tank" + clients.size();
        clients.add(name, msg.getSender());
        endpoint.send(msg.getSender(), new RegisterResponse(name));


    }

    public void deregister(Message msg){
        clients.remove(clients.indexOf(((DeregisterRequest) msg.getPayload()).getId()));
    }

    public void handoff(HandoffRequest handoff, InetSocketAddress socketAddress){
        int index = clients.indexOf(socketAddress);
        FishModel fish = handoff.getFish();
        Direction direction = fish.getDirection();

        InetSocketAddress neighbor;
        if(direction == Direction.LEFT){

            neighbor=(InetSocketAddress) clients.getLeftNeighorOf(index);
        } else {
            neighbor=(InetSocketAddress) clients.getRightNeighorOf(index);
        }

        endpoint.send(neighbor, handoff);


    }

    public static void main(String[] args){
        Broker broker = new Broker();
        broker.broker();
    }


}

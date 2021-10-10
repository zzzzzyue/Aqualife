package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.DeregisterRequest;
import aqua.common.msgtypes.HandoffRequest;
import aqua.common.msgtypes.RegisterRequest;
import aqua.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    int THREADSNUM = 5;
    Endpoint endpoint = new Endpoint(4711);
    ClientCollection clients = new ClientCollection();
    ExecutorService executor = Executors.newFixedThreadPool(THREADSNUM);
    ReadWriteLock rw = new ReentrantReadWriteLock();
    boolean stopRequest = false;




    public void broker(){
        //TODO: not work yet
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null,"Press OK button to stop server");
                stopRequest=true;
            }
        });




        while(!stopRequest){
            Message message = endpoint.blockingReceive();
            BrokerTask brokerTask = new BrokerTask();
            executor.execute(() -> brokerTask.brokerTask(message));

        }

        executor.shutdown();

    }

    private class BrokerTask {
        public void brokerTask(Message msg){
            if(msg.getPayload() instanceof RegisterRequest) {
                //TODO::vielleicht synchronized ??
                register(msg);
            }
            if(msg.getPayload() instanceof DeregisterRequest) {
                //TODO::vielleicht synchronized ??
                deregister(msg);
            }
            if(msg.getPayload() instanceof HandoffRequest) {
                //TODO: use read write lock to handle the handoff request
                HandoffRequest handoffRequest = (HandoffRequest) msg.getPayload();
                InetSocketAddress inetSocketAddress = msg.getSender();
                handoff(handoffRequest, inetSocketAddress);
            }

            if(msg.getPayload() instanceof  PoisonPill) {
                System.exit(0);
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

package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    int tankCount = 0;
    int THREADSNUM = 5;
    Endpoint endpoint = new Endpoint(4711);
    ClientCollection<InetSocketAddress> clients = new ClientCollection<>();
    ExecutorService executor = Executors.newFixedThreadPool(THREADSNUM);
    ReadWriteLock rw = new ReentrantReadWriteLock();
    volatile boolean stopRequest = false;
    boolean hasToken = true;

    private String tankID(int number){return "tank" + number; }




    public void broker(){

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
                synchronized (clients) {register(msg);}
            }
            if(msg.getPayload() instanceof DeregisterRequest) {
                synchronized (clients) {deregister(msg);}
            }
            if(msg.getPayload() instanceof  PoisonPill) {
                System.exit(0);
            }

        }

    }

    public void register(Message msg){
        //TODO:where is tank0?
        tankCount++;
        clients.add(tankID(tankCount), msg.getSender());

        int index = clients.indexOf(tankID(tankCount));

        endpoint.send(msg.getSender(), new NeighbourUpdate(clients.getLeftNeighorOf(index), Direction.LEFT));
        endpoint.send(msg.getSender(), new NeighbourUpdate(clients.getRightNeighorOf(index), Direction.RIGHT));
        endpoint.send( clients.getLeftNeighorOf(index), new NeighbourUpdate(msg.getSender(), Direction.RIGHT));
        endpoint.send(clients.getRightNeighorOf(index), new NeighbourUpdate(msg.getSender(), Direction.LEFT));
        //new client register
        endpoint.send(msg.getSender(), new RegisterResponse(tankID(tankCount)));

        if(this.hasToken) {
            this.endpoint.send(msg.getSender(), new Token());
            hasToken=false;
        }

    }

    public void deregister(Message msg){
        int index = clients.indexOf(tankID(tankCount));
        endpoint.send((InetSocketAddress) clients.getLeftNeighorOf(index), new NeighbourUpdate(msg.getSender(),  Direction.RIGHT));
        endpoint.send((InetSocketAddress) clients.getRightNeighorOf(index),new NeighbourUpdate(msg.getSender(),  Direction.LEFT));
        clients.remove(clients.indexOf(((DeregisterRequest) msg.getPayload()).getId()));
    }

    /**
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
     **/

    public static void main(String[] args){
        Broker broker = new Broker();
        broker.broker();
    }




}

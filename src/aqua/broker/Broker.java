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

    // noch frage zur Aufgabe2: die Joptionpane legt immer unter allem Fenster und wenn man das Fenster zumacht, dann kommt kein
    //
    int fishCount = 0;
    int THREADSNUM = 5;
    Endpoint endpoint = new Endpoint(4711);
    ClientCollection clients = new ClientCollection();
    ExecutorService executor = Executors.newFixedThreadPool(THREADSNUM);
    ReadWriteLock rw = new ReentrantReadWriteLock();
    volatile boolean stopRequest = false;




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
            if(msg.getPayload() instanceof HandoffRequest) {
                rw.writeLock().lock();
                HandoffRequest handoffRequest = (HandoffRequest) msg.getPayload();
                InetSocketAddress inetSocketAddress = msg.getSender();
                handoff(handoffRequest, inetSocketAddress);
                rw.writeLock().unlock();
            }

            if(msg.getPayload() instanceof  PoisonPill) {
                System.exit(0);
            }

        }

    }

    public void register(Message msg){
        String name = "tank" + (fishCount++);
        clients.add(name, msg.getSender());
        endpoint.send(msg.getSender(), new RegisterResponse(name));

        InetSocketAddress leftNeighbor = (InetSocketAddress) this.clients.getLeftNeighorOf(clients.size());
        InetSocketAddress rightNeighbor = (InetSocketAddress) this.clients.getRightNeighorOf(clients.size());
        endpoint.send(leftNeighbor, new NeighborUpdate(msg.getSender(), Direction.LEFT));
        endpoint.send(rightNeighbor,new NeighborUpdate(msg.getSender(), Direction.RIGHT));
        //TODO: Direction?

    }

    public void deregister(Message msg){
        clients.remove(clients.indexOf(((DeregisterRequest) msg.getPayload()).getId()));
        InetSocketAddress leftNeighbor = (InetSocketAddress) this.clients.getLeftNeighorOf(clients.size());
        InetSocketAddress rightNeighbor = (InetSocketAddress) this.clients.getRightNeighorOf(clients.size());
        endpoint.send(leftNeighbor, new NeighborUpdate(msg.getSender(),  Direction.LEFT));
        endpoint.send(rightNeighbor,new NeighborUpdate(msg.getSender(),  Direction.RIGHT));
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

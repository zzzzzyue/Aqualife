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
    private static final int THREADSNUM = 5;
    Endpoint endpoint;
    ClientCollection<InetSocketAddress> clients;
    ExecutorService executor;
    ReadWriteLock rw = new ReentrantReadWriteLock();
    volatile boolean stopRequest;
    boolean hasToken;

    private String tankID(int number){return "tank" + number; }

    public Broker() {
        this.clients = new ClientCollection<>();
        this.endpoint = new Endpoint(4711);
        this.stopRequest = false;
        this.hasToken = true;
        this.executor = Executors.newFixedThreadPool(THREADSNUM);
        executor.execute(new StopRequested());
    }

    public void broker(){
        Message msg;
        while(!stopRequest){
            if((msg = endpoint.nonBlockingReceive()) != null) {
                executor.execute(new BrokerTask(msg));
            }
        }
        executor.shutdown();
    }

    class StopRequested implements Runnable {
        @Override
        public void run() {
            JOptionPane.showMessageDialog(null,"Press OK button to stop server");
            stopRequest=true;
        }
    }

    class BrokerTask implements Runnable {
        Message msg;

        private BrokerTask(Message msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            if(msg.getPayload() instanceof RegisterRequest) {
                register(msg);
            }
            if(msg.getPayload() instanceof DeregisterRequest) {
                deregister(msg);
            }
            if(msg.getPayload() instanceof  PoisonPill) {
                stopRequest = true;
            }
        }
    }

    public void register(Message msg){
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
        endpoint.send(clients.getLeftNeighorOf(index), new NeighbourUpdate(msg.getSender(),  Direction.RIGHT));
        endpoint.send( clients.getRightNeighorOf(index),new NeighbourUpdate(msg.getSender(),  Direction.LEFT));
        clients.remove(clients.indexOf(((DeregisterRequest) msg.getPayload()).getId()));
    }

    public static void main(String[] args){
        Broker broker = new Broker();
        broker.broker();
    }
}

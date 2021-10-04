package messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Endpoint {
    private final DatagramSocket socket;

    public Endpoint() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException var2) {
            throw new RuntimeException(var2);
        }
    }

    public Endpoint(int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException var3) {
            throw new RuntimeException(var3);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(payload);
            byte[] bytes = baos.toByteArray();
            DatagramPacket datagram = new DatagramPacket(bytes, bytes.length, receiver);
            this.socket.send(datagram);
        } catch (Exception var7) {
            throw new RuntimeException(var7);
        }
    }

    private Message readDatagram(DatagramPacket datagram) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datagram.getData()));
            return new Message((Serializable)ois.readObject(), (InetSocketAddress)datagram.getSocketAddress());
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }

    public Message blockingReceive() {
        DatagramPacket datagram = new DatagramPacket(new byte[1024], 1024);

        try {
            this.socket.receive(datagram);
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }

        return this.readDatagram(datagram);
    }

    public Message nonBlockingReceive() {
        DatagramPacket datagram = new DatagramPacket(new byte[1024], 1024);

        try {
            this.socket.setSoTimeout(1);
        } catch (SocketException var7) {
            throw new RuntimeException(var7);
        }

        boolean timeoutExpired;
        try {
            this.socket.receive(datagram);
            timeoutExpired = false;
        } catch (SocketTimeoutException var5) {
            timeoutExpired = true;
        } catch (IOException var6) {
            throw new RuntimeException(var6);
        }

        try {
            this.socket.setSoTimeout(0);
        } catch (SocketException var4) {
            throw new RuntimeException(var4);
        }

        return timeoutExpired ? null : this.readDatagram(datagram);
    }
}
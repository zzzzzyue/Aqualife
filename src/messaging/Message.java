package messaging;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Message {
    private final Serializable payload;
    private final InetSocketAddress sender;

    public Message(Serializable payload, InetSocketAddress sender) {
        this.payload = payload;
        this.sender = sender;
    }

    public Serializable getPayload() {
        return this.payload;
    }

    public InetSocketAddress getSender() {
        return this.sender;
    }
}
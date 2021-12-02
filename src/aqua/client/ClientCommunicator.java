package aqua.client;

import java.net.InetSocketAddress;
import java.time.temporal.TemporalAccessor;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.Location;
import aqua.common.Properties;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress neighbour) {
			endpoint.send(neighbour, new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress receiver, Token token) {
			endpoint.send(receiver ,token);
		}

		public void sendMarker(InetSocketAddress receiver, SnapshotMarker snapshotMarker) {
			endpoint.send(receiver ,snapshotMarker);
		}

		public void sendSum(InetSocketAddress receiver, SnapshotSum snapshotSum) {
			endpoint.send(receiver ,snapshotSum);
		}

		public void sendLocationRequest(InetSocketAddress receiver, LocationRequest locationRequest) {endpoint.send(receiver, locationRequest);}

		public void sendNameResolutionRequest(NameResolutionRequest nameResolutionRequest) {
			endpoint.send(broker, nameResolutionRequest);
		}

		public void sendCurrentLocation(InetSocketAddress homeLocation, String fishID) {
			endpoint.send(homeLocation, new LocationUpdate(fishID));
		}

	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighbourUpdate) {
					NeighbourUpdate update = ((NeighbourUpdate) msg.getPayload());
					if(update.getDirection() == Direction.LEFT){
						tankModel.setLeftNeighbor(update.getNeighbour());
					} else {
						tankModel.setRightNeighbor(update.getNeighbour());
					}
				}
				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveMarker(msg.getSender(), (SnapshotMarker) msg.getPayload());
				}

				if (msg.getPayload() instanceof SnapshotSum) {
					tankModel.receiveSum((SnapshotSum) msg.getPayload());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getFishId());
				}

				if (msg.getPayload() instanceof NameResolutionResponse) {
					InetSocketAddress homeLocation = ((NameResolutionResponse) msg.getPayload()).getAddress();
					String fishId = ((NameResolutionResponse) msg.getPayload()).getRequestID();
					tankModel.handleResponse(homeLocation, fishId);
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					String fishId = ((LocationUpdate) msg.getPayload()).getFishId();
					InetSocketAddress currentLocation = msg.getSender();
					tankModel.updateCurrentLocation(fishId, currentLocation);
				}

			}

			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}

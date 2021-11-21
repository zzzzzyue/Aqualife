package aqua.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.Token;
import org.xml.sax.helpers.AttributesImpl;

enum RecordStates {IDLE, RIGHT, LEFT, BOTH}

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress leftNeighbor;
	protected InetSocketAddress rightNeighbor;
	protected boolean hasToken;
	protected Timer timer;
	protected RecordStates recordState = RecordStates.IDLE;
	private  int fishSum = 0;
	protected int globalSnapshot = 0;
	protected boolean showDialog;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
		this.timer = new Timer();
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		if(fish.getDirection() == Direction.RIGHT){
			if(recordState == RecordStates.BOTH || recordState == RecordStates.LEFT) {
				fishSum++;
			}
		}
		if(fish.getDirection() == Direction.LEFT){
			if(recordState == RecordStates.BOTH || recordState == RecordStates.RIGHT) {
				fishSum++;
			}
		}
	}

	public void receiveToken() {
		this.hasToken = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				hasToken = false;
				forwarder.sendToken(leftNeighbor);
			}
		};

		this.timer.schedule(task, 2000L );

	}

	//added in receiver
	public void receiveMarker(InetSocketAddress sender) {
		if(recordState==RecordStates.IDLE){
			fishSum = fishies.size();
			if (sender==leftNeighbor) recordState=RecordStates.RIGHT;
			else if (sender == rightNeighbor) recordState = RecordStates.LEFT;
			forwarder.sendMarker(leftNeighbor);
			forwarder.sendMarker(rightNeighbor);
		}
		else {
			if(sender.equals(leftNeighbor)) {
				if(recordState == RecordStates.LEFT) {

				}
			} else if (sender.equals(rightNeighbor)) {

			}


		}
	}

	//aufgabe4 begins
	public void initiateSnapshot(InetSocketAddress neighbor) {
		fishSum = fishies.size();
		recordState = RecordStates.BOTH;
		forwarder.sendMarker(leftNeighbor);
		forwarder.sendMarker(rightNeighbor);

	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if(hasToken) {
					if(fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(fish, getLeftNeighbor());
					} else {
						forwarder.handOff(fish, getRightNeighbor());
					}
				} else {
					fish.reverse();
				}
			}

			if (fish.disappears())
				it.remove();
		}
	}


	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public InetSocketAddress getLeftNeighbor() {
		return leftNeighbor;
	}

	public InetSocketAddress getRightNeighbor() {
		return rightNeighbor;
	}

	public boolean hasToken(){
		return this.hasToken;
	}

	public void setLeftNeighbor(InetSocketAddress leftNeighbor){
		this.leftNeighbor = leftNeighbor;
	}

	public void setRightNeighbor(InetSocketAddress rightNeighbor) {
		this.rightNeighbor = rightNeighbor;
	}
}
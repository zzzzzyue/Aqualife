package aqua.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.Location;
import aqua.common.RecordStates;
import aqua.common.msgtypes.*;
import org.xml.sax.helpers.AttributesImpl;


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
	protected boolean initiator = false;
	private  int fishSum = 0;
	protected boolean waitIDLE=false;
	protected int globalSnapshot = 0;
	protected boolean showDialog;
	public final int THREADPOOL = 5;
	ExecutorService executor = Executors.newFixedThreadPool(THREADPOOL);
	Map<String, InetSocketAddress> homeAgent = new HashMap<>();

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
			homeAgent.put(fish.getId(), null);
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
		//update the location of fish,in which tank id
		String fishID = fish.getId();
		if(homeAgent.containsKey(fishID)) {
			homeAgent.put(fishID, null);
		} else {
			this.forwarder.sendNameResolutionRequest(new NameResolutionRequest(fish.getTankId(), fishID));
		}

	}

	public void receiveToken() {
		this.hasToken = true;
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				hasToken = false;
				forwarder.sendToken(leftNeighbor, new Token());
			}
		};

		this.timer.schedule(task, 2000L );

	}



	//aufgabe4 begins
	public void initiateSnapshot(InetSocketAddress neighbor) {
		if(recordState == RecordStates.IDLE) {
			fishSum = fishies.size();
			recordState = RecordStates.BOTH;
			initiator = true;
			forwarder.sendMarker(leftNeighbor, new SnapshotMarker());
			forwarder.sendMarker(rightNeighbor, new SnapshotMarker());
		}
	}

	//added in receiver
	public void receiveMarker(InetSocketAddress sender, SnapshotMarker snapshotMarker) {
		if(recordState==RecordStates.IDLE){
			fishSum = fishies.size();
			//one window
			if(leftNeighbor.equals(rightNeighbor)) {
				recordState = RecordStates.BOTH;
				forwarder.sendMarker(leftNeighbor, snapshotMarker);
			} else {
				if(sender.equals(leftNeighbor)) {
					recordState = RecordStates.RIGHT;
				} else if(sender.equals(rightNeighbor)) {
					recordState = RecordStates.LEFT;
				}
				forwarder.sendMarker(leftNeighbor, snapshotMarker);
				forwarder.sendMarker(rightNeighbor, snapshotMarker);
			}

		} else {
			if(leftNeighbor.equals(rightNeighbor)) {
				recordState = RecordStates.IDLE;
			} else  {
				if(sender.equals(leftNeighbor)) {
					if(recordState == RecordStates.BOTH){
						recordState = RecordStates.RIGHT;
					}
					if(recordState == RecordStates.LEFT){
						recordState = RecordStates.IDLE;
					}
				} else if(sender.equals(rightNeighbor)) {
					if(recordState == RecordStates.BOTH){
						recordState = RecordStates.LEFT;
					}
					if(recordState == RecordStates.RIGHT){
						recordState = RecordStates.IDLE;
					}
				}
			}

		}

		if(initiator && recordState == RecordStates.IDLE) {
			//start collecting sum
			forwarder.sendSum(leftNeighbor, new SnapshotSum(fishSum));
		}
	}

	public void receiveSum(SnapshotSum snapshotSum) {
		waitIDLE = true;
		if(initiator) {
			initiator = false;
			showDialog = true;
			System.out.println(snapshotSum.getFishSum() + "fishes");
			globalSnapshot = snapshotSum.getFishSum();
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while (waitIDLE == true) {
					if(recordState == RecordStates.IDLE) {
						int currentFish = snapshotSum.getFishSum();
						int currentState = currentFish + fishSum;
						forwarder.sendSum(leftNeighbor, new SnapshotSum(currentState));
						waitIDLE = false;
					}
				}
			}
		});
	}

	public void locateFishGlobally(String fishId) {
		if(homeAgent.get(fishId) == null) {
			locateFishLocally(fishId);
		} else {
			InetSocketAddress currentLocation = homeAgent.get(fishId);
			forwarder.sendLocationRequest(currentLocation, new LocationRequest(fishId));
		}
	}

	private void locateFishLocally(String fishId) {
		for(FishModel fish : this.fishies){
			if(fish.getId().equals(fishId)) {
				fish.toggle();
			}
		}

	}

	public void handleResponse(InetSocketAddress homeLocation, String fishID) {
		forwarder.sendCurrentLocation(homeLocation, fishID);
	}

	public void updateCurrentLocation(String fishID, InetSocketAddress currentLocation) {
		homeAgent.put(fishID, currentLocation);
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
			if (fish.disappears()) {
				it.remove();
			}
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
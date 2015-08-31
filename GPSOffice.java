import edu.rit.ds.registry.AlreadyBoundException;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventGenerator;
import edu.rit.ds.RemoteEventListener;

/**
 * Class GPSOffice sets up a GPS office with the name of the office and the X
 * and Y coordinates of the office's location. This class binds the GPS office
 * to the registry and computes its 3 nearest neighbors computed by the
 * Euclidean distance. Whenever a new office is added, the neighbors of each
 * office is recomputed. It also consists of various event generators to be send
 * to the event listeners in Class Customer and Class Headquarters.
 * 
 * Usage: java Start GPSOffice host port name X Y host = Registry Server's host
 * port = Registry Server's port name = Name of the GPSOffice X = X coordinate
 * of the GPSOffice Y = Y coordinate of the GPSOffice
 */
public class GPSOffice implements GPSOfficeRef {

	/**
	 * Registry Proxy's name
	 */
	private String host;

	/**
	 * Registry Proxy's port.
	 */
	private int port;

	/**
	 * This GPS office's name.
	 */
	private String name;

	/**
	 * This GPS office location's X coordinate.
	 */
	private Double X;

	/**
	 * This GPS office location's Y coordinate.
	 */
	private Double Y;

	private boolean flag;

	/**
	 * HashMap to store this GPSOffice object's neighbor names and their
	 * respective distance.
	 */
	private HashMap<String, Double> N;

	/**
	 * HashMap to store this GPSOffice object's neighbor names and their
	 * respective X & Y coordinates.
	 */
	private HashMap<String, Double[]> XY;

	/**
	 * ArrayList to store this GPSOffice object's neighbor names.
	 */
	private ArrayList<String> neigh_names = new ArrayList<String>();

	private RegistryProxy registry;
	private ExecutorService reaper;
	private calculate calc;
	private String nextcity;
	private RemoteEventGenerator<NodeEvent> eventGenerator;

	/**
	 * Constructs a new GPSOffice object. Binds the new office object in the
	 * registry. Also constructs the neighbors of the object by iterating the
	 * office object's already registered in the registry.
	 * 
	 * The command line arguments are: args[0] = Registry Server's host. args[1]
	 * = Registry Server's port. args[2] = Name of the GPS office. args[3] = X
	 * Coordinate of the office's location. args[4] = Y Coordinate of the
	 * office's location.
	 * 
	 * @param args
	 *            Command line arguments.
	 * 
	 * @exception IllegalArgumentException
	 *                (unchecked exception) Thrown if there was a problem with
	 *                the command line arguments.
	 * @exception IOException
	 *                Thrown if an I/O error or a remote error occurred.
	 */

	public GPSOffice(String args[]) throws IOException, NotBoundException {

		N = new HashMap<String, Double>();
		XY = new HashMap<String, Double[]>();

		if (args.length != 5) {
			usage();
			System.exit(0);
		}
		host = args[0];
		port = parseInt(args[1]);
		name = args[2];
		X = parseDouble(args[3]);
		Y = parseDouble(args[4]);

		nextcity = name;

		// Get a proxy for the Registry Server.
		registry = new RegistryProxy(host, port);

		// Creates a new thread pool.
		reaper = Executors.newCachedThreadPool();

		calc = new calculate();

		// Throws events for package arrival.
		eventGenerator = new RemoteEventGenerator<NodeEvent>();

		// Export this node.
		UnicastRemoteObject.exportObject(this, 0);

		// Bind this GPS office into the Registry Server.
		try {
			registry.bind(name, this);
		} catch (AlreadyBoundException exc) {
			try {
				// Unbinding it from the registry.
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException exc2) {
			}
			throw new IllegalArgumentException(name + " GPSOffice "
					+ "already exists");
		} catch (RemoteException exc) {
			try {
				UnicastRemoteObject.unexportObject(this, true);
			} catch (NoSuchObjectException exc2) {
			}
		}
	}

	/**
	 * Print a usage message and exit.
	 */
	private static void usage() {
		System.err.println("Usage: java Customer <host> <port> <name> <X> <Y>");
		System.err.println("<host> = Registry Server's host");
		System.err.println("<port> = Registry Server's port (integer)");
		System.err.println("<name> = Name of the originating node");
		System.err.println("<X> = X coordinate of the destination (double)");
		System.err.println("<Y> = Y coordinate of the destination (double)");
		System.exit(1);
	}

	/**
	 * Returns the X coordinate of the current GPS office object.
	 */
	public Double getX() throws RemoteException {
		return X;
	}

	/**
	 * Returns the Y coordinate of the current GPS office object.
	 */
	public Double getY() throws RemoteException {
		return Y;
	}

	/**
	 * Returns the name of the current GPS office object.
	 */
	public String getName() throws RemoteException {
		return name;
	}

	/**
	 * Returns the hashmap which contains the neighbors and the corresponding
	 * distances of this current GPS office object.
	 */
	public HashMap<String, Double> getHashMap() throws RemoteException {
		return N;
	}

	/**
	 * Returns the hashmap which contains the neighbors and the corresponding X
	 * & Y coordinate values of this current GPS office object.
	 */
	public HashMap<String, Double[]> getNameXY() throws RemoteException {
		return XY;
	}

	/**
	 * Returns the arraylist which contains the neighbor names of the current
	 * GPS office object.
	 */
	public ArrayList<String> getNeighNames() throws RemoteException {
		return neigh_names;
	}

	/**
	 * Computes the neighbors of the calling object with the object in the
	 * parameter.
	 * 
	 * @param GPSOfficeRef
	 *            node
	 * 
	 * @return void
	 * 
	 * @exception RemoteException
	 *                Thrown when a remote error occurs.
	 */
	public void neighbors(GPSOfficeRef office) throws RemoteException {

		if (office.getName().equals(this.getName())) {
			return;
		}
		if (this.getHashMap().containsKey(office.getName()))
			return;
		Double distance = Math.sqrt(Math.pow((this.getX() - office.getX()), 2)
				+ Math.pow((this.getY() - office.getY()), 2));

		// Checking if this object has less than 3 neighbors i.e. it has
		// less than 3 values in the hashmap.
		if (this.getHashMap().size() < 3) {
			this.getHashMap().put(office.getName(), distance);
			this.getNeighNames().add(office.getName());
			Double[] tempXY = new Double[2];
			tempXY[0] = office.getX();
			tempXY[1] = office.getY();
			this.getNameXY().put(office.getName(), tempXY);
		} else if (this.getHashMap().size() >= 3) {
			boolean flag = false;
			Double max = 0.0;
			String neighbor_name = null;
			for (Map.Entry<String, Double> entry1 : this.getHashMap()
					.entrySet()) {
				if (entry1.getValue() > max) {
					max = entry1.getValue();
					neighbor_name = entry1.getKey();
				}
			}
			for (Map.Entry<String, Double> entry1 : this.getHashMap()
					.entrySet()) {
				if (distance <= entry1.getValue()) {
					flag = true;
					break;
				}
			}
			// If flag is true, then the new GPS office's distance is
			// less than the distance of one of the already present
			// neighbors of the current GPS office.
			if (flag == true) {
				this.getHashMap().remove(neighbor_name);
				this.getNameXY().remove(neighbor_name);
				this.getNeighNames().remove(neighbor_name);
				this.getHashMap().put(office.getName(), distance);
				Double[] tempXY = new Double[2];
				tempXY[0] = office.getX();
				tempXY[1] = office.getY();
				this.getNameXY().put(office.getName(), tempXY);
				this.getNeighNames().add(office.getName());
			}
			if (flag == false) {
				this.getHashMap().remove(office.getName());
				this.getNameXY().remove(office.getName());
				this.getNeighNames().remove(office.getName());
			}
		}
	}

	/**
	 * Parse an integer command line argument.
	 * 
	 * @param arg
	 *            Command line argument.
	 * 
	 * @return Integer value of arg.
	 * 
	 * @exception NumberFormatException
	 *                Thrown if arg cannot be parsed as an integer.
	 */
	private static int parseInt(String arg) {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException exc) {
			throw new IllegalArgumentException("Port: " + arg
					+ " cannot be parsed as an integer.");
		}
	}

	/**
	 * Parse a double command line argument.
	 * 
	 * @param arg
	 *            Command line argument.
	 * 
	 * @return Double value of arg.
	 * 
	 * @exception NumberFormatException
	 *                Thrown if arg cannot be parsed as a double.
	 */
	private static Double parseDouble(String arg) {
		try {
			return Double.parseDouble(arg);
		} catch (NumberFormatException exc) {
			throw new IllegalArgumentException("Coordinate: " + arg
					+ " cannot be parsed as a double.");
		}
	}

	/**
	 * This function returns the tracking number of a package given by the
	 * origin node.
	 * 
	 * @return long
	 */
	public long giveTrackingNumber() {
		return System.currentTimeMillis();
	}

	/**
	 * This function is called from the Customer Class and is used to forward
	 * the packet through the network.
	 * 
	 * @param Customer
	 *            query. Consists of the unique tracking number, origin node, X
	 *            & Y coordinates of the destination.
	 * 
	 * @return void
	 * 
	 * @exception NotBoundException
	 *                Thrown when not bounded correctly.
	 * @throws RemoteException
	 */
	public void forwardPacket(final Customer request,
			final RemoteEventListener<NodeEvent> nodeListener)
			throws NotBoundException, RemoteException {
		try {
			List<String> nodes = registry.list();
			Iterator<String> node1 = nodes.iterator();
			while (node1.hasNext()) {
				GPSOfficeRef someNode = (GPSOfficeRef) registry.lookup(node1
						.next());
				this.neighbors(someNode);
			}
		} catch (RemoteException exc) {
		}

		// Throws an event that the packet has arrived at this GPS office
		eventGenerator.reportEvent(new NodeEvent("Package number "
				+ request.trackingnumber + " arrived at " + this.name
				+ " office", request, 1));
		nodeListener.report(0, new NodeEvent("Package number "
				+ request.trackingnumber + " arrived at " + this.name
				+ " office", request, 1));
		final String thisOfficeName = this.name;

		// Wait three seconds.
		slowDown();

		/**
		 * This block calls the calculate Class to compute the next destination
		 * of the current request i.e. packet.
		 */
		try {
			nextcity = calc.check(request.X, request.Y, this);
			if (nextcity.equals(this.getName())) {
				eventGenerator.reportEvent(new NodeEvent("Package number "
						+ request.trackingnumber + " delivered " + "from "
						+ nextcity + " office to (" + request.X + ","
						+ request.Y + ")", request, 2));
				nodeListener.report(0, new NodeEvent("Package number "
						+ request.trackingnumber + " delivered " + "from "
						+ nextcity + " office to (" + request.X + ","
						+ request.Y + ")", request, 2));
				return;
			}

			eventGenerator.reportEvent(new NodeEvent("Package number "
					+ request.trackingnumber + " departed from " + this.name
					+ " office", request, 3));
			nodeListener.report(0, new NodeEvent("Package number "
					+ request.trackingnumber + " departed from " + this.name
					+ " office", request, 3));
			final GPSOfficeRef node;
			try {
				node = (GPSOfficeRef) registry
						.lookup(nextcity);
				// Thread pool for forwarding of the paNotBoundExceptionckage.
				reaper.execute(new Runnable() {
					public void run() {
						try {
							node.forwardPacket(request, nodeListener);
						} catch (RemoteException ex) {

							try {
								recomputeNeighbors(nextcity);
							} catch (RemoteException e) {
							} catch (NotBoundException e) {
							}

							eventGenerator.reportEvent(new NodeEvent(
									"Package number " + request.trackingnumber
											+ " lost by " + thisOfficeName
											+ " office", request, 4));
							try {
								nodeListener.report(0, new NodeEvent(
										"Package number "
												+ request.trackingnumber
												+ " lost by " + thisOfficeName
												+ " office", request, 4));
							} catch (RemoteException e) {
							}

						} catch (NotBoundException e) {
							// TODO Auto-generated catch block
							
						}
					}
				});
			} catch (NotBoundException e) {
				this.getHashMap().remove(nextcity);
				this.getNeighNames().remove(nextcity);
				this.getNameXY().remove(nextcity);
				nextcity = calc.check(request.X, request.Y, this);
				GPSOfficeRef node1 = (GPSOfficeRef) registry
						.lookup(nextcity);
				node1.forwardPacket(request, nodeListener);
			}

		} catch (Exception e) {
		}
		return;
	}

	/**
	 * This function re computes the neighbors of a GPS office with other GPS
	 * offices in case of failures in the system.
	 * 
	 * @param String
	 *            nextcity
	 * 
	 * @return void
	 * 
	 * @exception RemoteException
	 *                Thrown when a remote error occurs.
	 * @exception NotBoundException
	 *                Thrown when a bound error occurs.
	 */
	public void recomputeNeighbors(String nextcity) throws RemoteException,
			NotBoundException {

		this.getHashMap().clear();
		this.getNameXY().clear();
		this.getNeighNames().clear();
		for (int i = 0; i < registry.list().size(); i++) {
			if (registry.list().get(i).equals(nextcity)) {
				continue;
			}
			GPSOfficeRef someNode = (GPSOfficeRef) registry.lookup(registry
					.list().get(i));
			someNode.getHashMap().clear();
			someNode.getNameXY().clear();
			someNode.getNeighNames().clear();
			this.neighbors(someNode);
			someNode.neighbors(this);
		}
	}

	/**
	 * Creates a delay of 3 seconds for the thread.
	 */
	private void slowDown() {
		try {
			Thread.sleep(3000L);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Adds a listener for packages arriving at a GPS office.
	 * 
	 * @param RemoteEventListener
	 *            <NodeEvent>
	 * 
	 * @return Lease
	 * 
	 * @exception RemoteException
	 *                Thrown when a remote error occurs.
	 */
	public Lease addListener(RemoteEventListener<NodeEvent> listener)
			throws RemoteException {
		return eventGenerator.addListener(listener);
	}

}

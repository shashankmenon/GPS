import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class Customer contains a customer request to deliver a package in the GPS
 * tracking system. Customer request is uniquely identified by a tracking number
 * which is the current timestamp in the system. Customer request also contains
 * the X and Y coordinates of the destination to which the packet to be
 * delivered.
 * 
 * Usage: java Customer host port name X Y host = Registry Server's host port =
 * Registry Server's port name = Name of the originating node X = X coordinate
 * of the destination Y = Y coordinate of the destination
 */
public class Customer extends Thread implements Serializable {

	/**
	 * This customer package's trackingnumber.
	 */
	public final long trackingnumber;

	/**
	 * This customer package's destination X coordinate.
	 */
	public final Double X;

	/**
	 * This customer package's destination Y coordinate.
	 */
	public final Double Y;

	/**
	 * This customer package's origin node name.
	 */
	public final String originalNode;

	/**
	 * The object of the RegistryProxy.
	 */
	private static RegistryProxy registry;
	private static RemoteEventListener<NodeEvent> nodeListener;
	private static long trackingNumber;

	/**
	 * Creates a new customer request for packet delivery.
	 * 
	 * @param trackingnumber
	 *            Tracking number
	 * @param X
	 *            Destination X coordinate
	 * @param Y
	 *            Destination Y coordinate
	 * @param originNode
	 *            Originating node name.
	 */
	Customer(long trackingnumber, Double X, Double Y, String originalNode) {
		this.trackingnumber = trackingnumber;
		this.X = X;
		this.Y = Y;
		this.originalNode = originalNode;

	}

	/**
	 * Customer main program.
	 * 
	 * @param String
	 *            [] args
	 */
	public static void main(String[] args) throws Exception {

		// Parse Command line Arguments
		if (args.length != 5)
			usage();
		String host = args[0];
		int port = parseInt(args[1]);
		String name = args[2];
		Double x = parseDouble(args[3]);
		Double y = parseDouble(args[4]);

		// Look up node ID in the Registry Server and originate the query.
		registry = new RegistryProxy(host, port);
		GPSOfficeRef node = (GPSOfficeRef) registry.lookup(name);
		trackingNumber = node.giveTrackingNumber();

		/**
		 * Calls the Customer constructor and creates a new customer request
		 * with unique trackingnumber.
		 */
		final Customer customer = new Customer(trackingNumber, x, y, name);
		

		/**
		 * Export a remote event listener object for receiving notifications
		 * about arrivals from GPSOffice objects.
		 */
		nodeListener = new RemoteEventListener<NodeEvent>() {
			public void report(long seqnum, NodeEvent event) {
				System.out.println(event.officename);
				if (event.delivery == 2 || event.delivery == 4) {
					System.exit(1);
				}

			}
		};
		UnicastRemoteObject.exportObject(nodeListener, 0);
		
		node.forwardPacket(customer, nodeListener);
	}
	
	/**
	 * Gets the packet's tracking number from the GPS office.
	 * @return long
	 */
	public long getTrackingNumber() {
		return trackingnumber;
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
			System.err.println(arg + " cannot be parsed as an integer");
			usage();
			return 0;
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
			System.err.println(arg + " cannot be parsed as a double");
			usage();
			return 0.0;
		}
	}
}
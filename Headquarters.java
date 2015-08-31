import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryEventFilter;
import edu.rit.ds.registry.RegistryEventListener;
import edu.rit.ds.registry.RegistryProxy;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

/**
 * Class Headquarters prints the messages of all the packages
 * in transit of all the customers.
 * 
 * Usage: java Headquarters host port
 * host = Registry Server's host
 * port = Registry Server's port
 */
public class Headquarters {

	private static RegistryProxy registry;
	private static RegistryEventListener registryListener;
	private static RegistryEventFilter registryFilter;
	private static RemoteEventListener<NodeEvent> nodeListener;

	public static void main(String[] args) throws Exception {
		
		// Parse command line arguments.
		if (args.length != 2) usage();
		String host = args[0];
		int port = parseInt(args[1]);

		// Get proxy for the Registry Server.
		registry = new RegistryProxy(host, port);

		// Export a remote event listener object for receiving notifications
		// from the Registry Server.
		registryListener = new RegistryEventListener() {
			public void report(long seqnum, RegistryEvent event) {
				listenOfficeEvents(event.objectName());
			}
		};
		UnicastRemoteObject.exportObject(registryListener, 0);

		
		/** 
		 * Export a remote event listener object for receiving notifications
		 * about arrivals from GPSOffice objects.
		 */		
		nodeListener = new RemoteEventListener<NodeEvent>() {
			public void report(long seqnum, NodeEvent event) {
				System.out.println(event.officename);
			}
		};
		UnicastRemoteObject.exportObject(nodeListener, 0);
		
		
		// Tell the Registry Server to notify us when a new GPSOffice object is
		// bound.
		registryFilter = new RegistryEventFilter();
		registryFilter.reportType("GPSOffice").reportBound();
		registry.addEventListener(registryListener, registryFilter);

		// Tell all existing GPSOffice objects to notify the customer of requests.
		List<String> offices = registry.list();
		Iterator<String> officeObjects = offices.iterator();
		while (officeObjects.hasNext()) {
			String someOffice = officeObjects.next();
			listenOfficeEvents(someOffice);			
		}
	}

	
	/**
	 * Tell the given GPSOffice object to notify the customer of requests.
	 *
	 * @param  objectName  GPSOffice object's name.
	 *
	 * @exception  RemoteException
	 *     Thrown if a remote error occurred.
	 */
	private static void listenOfficeEvents(String objectName) {
		try {
			GPSOfficeRef node = (GPSOfficeRef) registry.lookup(objectName);
			node.addListener(nodeListener);
		} catch (NotBoundException exc) {
			
		} catch (RemoteException exc) {
			
		}
	}

	
	/**
	 * Print a usage message and exit.
	 */
	private static void usage() {
		System.err.println("Usage: java Headquarters <host> <port>");
		System.err.println("<host> = Registry Server's host");
		System.err.println("<port> = Registry Server's port");
		System.exit(1);
	}

	
	/**
	 * Parse an integer command line argument.
	 *
	 * @param  arg  Command line argument.
	 *
	 * @return  Integer value of arg.
	 * 
	 * @exception  NumberFormatException
	 *     Thrown if arg cannot be parsed as an integer.
	 */
	private static int parseInt(String arg) {
		try {
			return Integer.parseInt(arg);
		} catch (NumberFormatException exc) {
			System.err.println("Headquarters: " + arg + " cannot be parsed as an integer");
			usage();
			return 0;
		}
	}
}

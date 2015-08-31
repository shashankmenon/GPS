import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import edu.rit.ds.Lease;
import edu.rit.ds.RemoteEventListener;
import edu.rit.ds.registry.NotBoundException;

public interface GPSOfficeRef extends Remote {

	/**
	 * Accessor to get the X coordinate of the GPS office.
	 * 
	 * @return Double
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public Double getX() throws RemoteException;

	/**
	 * Accessor to get the Y coordinate of the GPS office.
	 * 
	 * @return Double
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public Double getY() throws RemoteException;

	/**
	 * Function to set the neighbors of a GPS office.
	 * 
	 * @return void
	 * 
	 * @param GPSOfficeRef
	 *            gpsOffice
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public void neighbors(GPSOfficeRef gpsOffice) throws RemoteException;

	/**
	 * Accessor to get the name of the GPS office.
	 * 
	 * @return String
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public String getName() throws RemoteException;

	/**
	 * Function which returns the neighbors and theirs corresponding distances
	 * from the destination for a GPS office.
	 * 
	 * @return HashMap<String,Double>
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public HashMap<String, Double> getHashMap() throws RemoteException;

	/**
	 * Function which returns the names of the neighbors of a GPS office.
	 * 
	 * @return ArrayList<String>
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public ArrayList<String> getNeighNames() throws RemoteException;

	/**
	 * Function which returns the name of the neighbors and their corresponding
	 * X & Y coordinates of a GPS office.
	 * 
	 * @return HashMap<String,Double[]>
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public HashMap<String, Double[]> getNameXY() throws RemoteException;

	/**
	 * Function to forward the package to its next neighbor.
	 * 
	 * @return void
	 * 
	 * @param Customer
	 *            query
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public void forwardPacket(Customer request,
			RemoteEventListener<NodeEvent> listener) throws RemoteException,
			NotBoundException;

	/**
	 * Adds a listener for node arrivals.
	 * 
	 * @return Lease
	 * 
	 * @param RemoteEventListener
	 *            <NodeEvent> listener
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public Lease addListener(RemoteEventListener<NodeEvent> listener)
			throws RemoteException;

	/**
	 * Function which returns the tracking number for each package.
	 * 
	 * @return long
	 * 
	 * @exception RemoteException
	 *                Thrown if a remote error occurred.
	 */
	public long giveTrackingNumber() throws RemoteException;
}

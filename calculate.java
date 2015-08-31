import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.rit.ds.registry.NotBoundException;
import edu.rit.ds.registry.RegistryProxy;

/**
 * Class calculate computes the distances between this GPS office
 * and its neighbors and decides which office will be the next hop.
 */
public class calculate {
	
	public calculate() {
	}
	
	/**
	 * This function checks which office should be the next
	 * hop for the package.
	 * 
	 * @param X
	 * @param Y
	 * @param xyz
	 * @return String
	 * @throws RemoteException
	 * 		Thrown when a remote error occurs.
	 * @throws NotBoundException
	 * 		Thrown when a bound error occurs.
	 */
	public String check(Double X, Double Y, GPSOffice xyz) throws RemoteException, NotBoundException{
		Double src_dest_dist;
		Double[] neigh_dest_distances = new Double[3];
		neigh_dest_distances[0] = 100000000.0;
		neigh_dest_distances[1] = 100000000.0;
		neigh_dest_distances[2] = 100000000.0;
		Double min = 1000000000.0;
		boolean flag = true;
		String finalneighbor = xyz.getName();
		HashMap<String, Double[]> neighbors = xyz.getNameXY();
		ArrayList<String> neigh_names = new ArrayList<String>(neighbors.keySet());
		if(neighbors.size() == 0) {
			return xyz.getName();
		}
		
		/**
		 * Getting the distance and the name of the office which is 
		 * closest to the destination.
		 */
		for(int i=0;i<neigh_names.size();i++) {
			Double neigh_xy[] = new Double[2];
			neigh_xy = neighbors.get(neigh_names.get(i));
			neigh_dest_distances[i] = Math.sqrt(Math.pow((X - neigh_xy[0]),2) + Math.pow((Y - neigh_xy[1]), 2));
			if(neigh_dest_distances[i] <= min) {
				min = neigh_dest_distances[i];
				finalneighbor = neigh_names.get(i);
			}
		}
		
		/**
		 * Checking if the distance from this gps office till the destination
		 * is less than the distance of any of its neighbors to the destination.
		 */
		try {
			src_dest_dist = Math.sqrt(Math.pow((X - xyz.getX()),2) + Math.pow((Y - xyz.getY()), 2));
			if(src_dest_dist <= neigh_dest_distances[0] &&
					src_dest_dist <= neigh_dest_distances[1] &&
							src_dest_dist <= neigh_dest_distances[2]){
				return xyz.getName();
			}
			for(int i=0;i<3;i++) {
				if(src_dest_dist >= neigh_dest_distances[i]){
					flag = false;
					break;
				}
			}
			/**
			 * If flag is true, means that this gps office is closest to 
			 * the destination.
			 */
			if(flag == true){
				return xyz.getName();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		// Returns the name of the neighbor to which the package is
		// to be forwarded next.
		return finalneighbor;
		
	}	
}

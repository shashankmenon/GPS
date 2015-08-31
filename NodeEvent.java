import edu.rit.ds.RemoteEvent;

/**
 * Class NodeEvent encapsulates a remote event generated by a GPS office.
 * The node events are used to report the Customer class and Headquarters
 * class about the event when a package leaves a GPS office.
 */
public class NodeEvent
   extends RemoteEvent
   {
   public final String officename;
   public final Customer request;
   
   public final int delivery;

   /**
    * Create a new node event.
    *
    * @param  officename  Name of the GPS office that issued this package.
    * @param  query   Package that was issued.
    */
   public NodeEvent
      (String officename,
       Customer request, 
       int delivery)
      {
      this.officename = officename;
      this.request = request;
      this.delivery = delivery;
      }
   }
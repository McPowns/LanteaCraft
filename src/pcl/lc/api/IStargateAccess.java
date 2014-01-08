/**
 * This file is part of the official LanteaCraft API. Please see the usage guide and
 * restrictions on use in the package-info file.
 */
package pcl.lc.api;

/**
 * Provides an interface for external code to interact with Stargate structures. You should
 * avoid interacting with Stargates outside of this API.
 * 
 * @author AfterLifeLochie
 */
public interface IStargateAccess {

	/**
	 * Fetches the busy state of the Stargate. If the Stargate is in currently dialling a
	 * connection, receiving a connection, in a connection or closing a connection, this
	 * returns true. If the gate is completely idle (that is, doing nothing), this returns
	 * false.
	 * 
	 * @return If the Stargate is currently engaged in any operation.
	 */
	public boolean isBusy();

	/**
	 * Fetches the state of the Stargate's iris. If the Stargate has no iris installed, or no
	 * valid iris is configured, this will return false. If the iris is currently engaged, this
	 * will return true. All other conditions return false.
	 * 
	 * @return If the Stargate has a valid iris and if the iris is currently engaged.
	 */
	public boolean isIrisActive();

	/**
	 * Determines if the current dialled connection is an outgoing one (that is, initiated from
	 * this Stargate). If there is no connection, this returns false. If this Stargate
	 * initiated the connection, this will return true. All other conditions return false.
	 * 
	 * @return If the Stargate initiated the current connection.
	 */
	public boolean isOutgoingConnection();

	/**
	 * Obtains the address of the current Stargate connection, regardless of which Stargate
	 * created the connection. If no connection is currently present, this returns null.
	 * 
	 * @return The current Stargate's connected address, or null if no connection is active.
	 */
	public String getConnectionAddress();

	/**
	 * Requests this Stargate attempt to dial the provided address.
	 * 
	 * @param address
	 *            The address to attempt to dial.
	 * @return Returns true if the dialling has started successfully. Returns false if the
	 *         address is invalid, if there is not enough energy available to open the
	 *         connection, or if some other condition currently prevents this Stargate from
	 *         initiating a connection.
	 */
	public boolean connect(String address);

	/**
	 * Gets the total quantity of energy which is immediately available to this Stargate.
	 * 
	 * @return The total quantity of energy which is immediately available to this Stargate,
	 *         measured in the arbitrary unit 'naquadah units'.
	 */
	public double getAvailableEnergy();

	/**
	 * Gets the total remaining dial requests which are currently available, based on the
	 * currently immediately available energy sources.
	 * 
	 * @return The number of dials which are currently available. Derived by calculating the
	 *         cost of a dial request, relative to the quantity of energy which is immediately
	 *         available to the Stargate.
	 */
	public double getRemainingDials();

	/**
	 * Gets the total remaining number of ticks which the gate can remain open for, based on
	 * the currently immediately available energy sources.
	 * 
	 * @return The number of ticks which the Stargate can remain open for, ignoring the costs
	 *         associated with opening the Stargate if it is not already opened. Derived by
	 *         calculating the energy consumption per second, relative to the quantity of
	 *         energy which is immediately available to the Stargate.
	 */
	public double getRemainingConnectionTime();

}

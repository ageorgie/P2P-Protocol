package ece454p1;

/**
 * Peer and Status are the classes we really care about Peers is a container;
 * feel free to do a different container
 */
public abstract class Peer {
	// This is the formal interface and you should follow it
	public abstract int insert(String filename);

	public abstract int query(Status status);

	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
	public abstract int join();

	public abstract int leave();

	/*
	 * TODO: Feel free to hack around with the private data, 
	 * since this is part of your design.
	 * This is intended to provide some exemplars to help; 
	 * ignore it if you don't like it.
	 */

	private enum State {
		connected, disconnected, unknown
	};

	private State currentState;
	private Peers peers;

}

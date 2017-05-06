package com.chivvon.mapviewer.component;

public final class Resource extends Cacheable {
	
	public int dataType;
	public byte buffer[];
	public int ID;
	boolean incomplete;
	int loopCycle;

	public Resource() {
		incomplete = true;
	}
}

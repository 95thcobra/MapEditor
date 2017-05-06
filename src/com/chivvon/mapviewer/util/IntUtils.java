package com.chivvon.mapviewer.util;

public final class IntUtils {
	
	private IntUtils() {
		
	}
	
	public static int regionIdToXPosition(int regionId) {
		return (regionId >> 8) << 6;
	}
	
	public static int regionIdToYPosition(int regionId) {
		return (regionId & 0xFF) << 6;
	}
	
	public static int coordinateToRegionId(int x, int y) {
		int rx = x >> 6;
		int ry = y >> 6;
		
		return rx * 256 + ry;
	}

}

package com.chivvon.mapviewer.component;

public class StaticLogic {
	
    public static int[] BIT_MASKS;

    static {
    	BIT_MASKS = new int[32];
		int i = 2;
		for(int k = 0; k < 32; k++)
		{
			BIT_MASKS[k] = i - 1;
			i += i;
		}
    }
}
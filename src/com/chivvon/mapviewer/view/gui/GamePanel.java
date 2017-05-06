package com.chivvon.mapviewer.view.gui;

import javax.swing.JPanel;
import com.chivvon.mapviewer.view.View;

@SuppressWarnings("serial")
public class GamePanel extends View {
	
	public void createGamePanel (JPanel gamePanel) {
		
		init(); 
		gamePanel.add(this);
		System.out.println("initialized");
	
	}  
	
}

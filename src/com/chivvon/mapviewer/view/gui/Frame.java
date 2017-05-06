package com.chivvon.mapviewer.view.gui;

import java.awt.Color;
import java.io.IOException;

import javax.swing.JPanel;

import com.chivvon.mapviewer.Configuration;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Frame extends Application {
	
    /**
     * 
    to do make this work somehow gamePanel.createGamePanel:
    
 	private GamePanel gamePanel;
 	
     */
	
    @Override
    public void start(Stage stage) {
    	try { 		        
	        Parent root = FXMLLoader.load(getClass().getResource("/view/Builder.fxml"));
	        Scene scene = new Scene(root, 940, 537);       
	        
	        stage.setTitle(Configuration.CLIENT_NAME);
	        stage.setResizable(false);
	        stage.setScene(scene);
	        stage.show();
	        
	        Platform.setImplicitExit(true);
	        stage.setOnCloseRequest((ae) -> {
	            Platform.exit();
	            System.exit(0);
	        });
	        
	        JPanel gamePanel = new JPanel();
	        SwingNode swingNode = (SwingNode) root.lookup("#swingNode");
	        gamePanel.setBackground(Color.BLUE);
	        swingNode.setContent(gamePanel);
	        
	        //this.gamePanel.createGamePanel(gamePanel);   
	        
         
    	} catch (IOException e) {
            e.printStackTrace();
        }
    }

}
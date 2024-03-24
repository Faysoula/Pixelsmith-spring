package com.example.pixelsmith;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SpriteSelector extends Application {
    private ListView<String> spriteListView = new ListView<>();
    private Integer userId;


    public SpriteSelector(Integer userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        VBox layout = new VBox(10);
        Button createSpriteButton = new Button("Create Sprite");
        createSpriteButton.setOnAction(e -> createAndOpenNewSprite());
        layout.getChildren().addAll(spriteListView,createSpriteButton);
        Scene scene = new Scene(layout, 300, 400);
        primaryStage.setTitle("Select a Sprite");
        primaryStage.setScene(scene);
        primaryStage.show();

        populateSpriteList();
    }
    private void createAndOpenNewSprite() {
        try {
            PixelArtEditor editor = PixelArtEditor.getInstance();
            Stage editorStage = new Stage();
            editor.createNewSpriteEditor("New Sprite");
        } catch (Exception ex) {
            ex.printStackTrace(); // Handle exception
        }
    }




    private void populateSpriteList() {
        String query = "SELECT Name, PathDirect FROM spritedata JOIN Sprites ON spritedata.SpriteID = Sprites.SpriteID WHERE UserId = ?";

        try (Connection conn = PixelArtEditor.getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String spriteName = rs.getString("Name");
                    String pathDirect = rs.getString("PathDirect");
                    spriteListView.getItems().add(spriteName); // Add the sprite name to the list view

                    // Handle list item selection
                    spriteListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            openSpriteInEditor(newSelection, pathDirect); // Open the selected sprite
                        }
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void openSpriteInEditor(String spriteName, String pathToSprite) {
        Integer spriteId = getSpriteIdFromNameAndPath(spriteName, pathToSprite);
        if (spriteId != null) {
            Platform.runLater(() -> {
                try {
                    PixelArtEditor editor = PixelArtEditor.getInstance();
                    Stage editorStage = new Stage();
                    editor.openSprite(spriteId, pathToSprite, editorStage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private Integer getSpriteIdFromNameAndPath(String spriteName, String path) {
        String query = "SELECT S.SpriteID FROM Sprites S JOIN spritedata SD ON S.SpriteID = SD.SpriteID WHERE S.Name = ? AND SD.PathDirect = ?";

        try (Connection conn = PixelArtEditor.getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, spriteName);
            pstmt.setString(2, path);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("SpriteID");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(); // Handle exception properly
        }
        return null; // Return null if sprite ID not found or in case of an exception
    }

    // Other necessary methods...
}

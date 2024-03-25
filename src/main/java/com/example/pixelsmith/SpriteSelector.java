package com.example.pixelsmith;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SpriteSelector extends Application {
    private Integer userId;
    private TilePane tilePane = new TilePane();

    public SpriteSelector(Integer userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        tilePane.setPadding(new Insets(15, 15, 15, 15));
        tilePane.setVgap(15);
        tilePane.setHgap(15);
        tilePane.setAlignment(Pos.CENTER);
        tilePane.setStyle("-fx-background-color: #444444; -fx-background-insets: 5;"); // Set the background color of tilePane

        // Create the ScrollPane for the tilePane
        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #444444; -fx-background-color: #444444;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            tilePane.getChildren().clear(); // Clear the current tiles
            populateSpriteList(); // Repopulate the list with updated sprites
        });
        refreshButton.setStyle("-fx-background-color: #555555; -fx-text-fill: #fff; -fx-font-weight: bold; -fx-padding: 10;");


        // Set an image on the createSpriteButton
        Button createSpriteButton = new Button();
        Image buttonImage = new Image("createnew.png");
        ImageView imageView = new ImageView(buttonImage);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        createSpriteButton.setGraphic(imageView);
        createSpriteButton.setOnAction(e -> createAndOpenNewSprite());

        // Create the layout and add the ScrollPane and createSpriteButton
        VBox layout = new VBox(10);
        layout.setStyle("-fx-background-color: #444444;");
        layout.getChildren().addAll(scrollPane, createSpriteButton, refreshButton);


        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setTitle("Select a Sprite");
        primaryStage.setScene(scene);
        Image applicationIcon = new Image("icon.png");
        primaryStage.getIcons().add(applicationIcon);
        primaryStage.show();

        tilePane.getChildren().clear();
        populateSpriteList();
    }
    private void createAndOpenNewSprite() {
        try {
            PixelArtEditor editor = PixelArtEditor.getInstance();
            Stage editorStage = new Stage();
            editor.createNewSpriteEditor("New Sprite");

            // After creating a new sprite, refresh the list of sprites
            tilePane.getChildren().clear(); // Clear the current tiles
            populateSpriteList(); // Repopulate the list to include the new sprite
        } catch (Exception ex) {
            ex.printStackTrace();
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

                    // Skip this sprite if the path is null
                    if (pathDirect == null) {
                        System.out.println("Skipping sprite with null path: " + spriteName);
                        continue;
                    }

                    System.out.println("Loading sprite: " + spriteName + " at path: " + pathDirect); // Debug output

                    Image spriteImage = new Image(new File(pathDirect).toURI().toString(), true);
                    ImageView imageView = new ImageView(spriteImage);
                    imageView.setFitHeight(100); // Set thumbnail size
                    imageView.setFitWidth(100);

                    Text spriteText = new Text(spriteName);
                    VBox spriteBox = new VBox(5, imageView, spriteText);
                    String vboxStyle = "-fx-padding: 10; " +
                            "-fx-border-style: solid inside; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-insets: 5; " +
                            "-fx-border-radius: 5; " +
                            "-fx-border-color: #555; " +
                            "-fx-background-color: #333333; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);";
                    spriteBox.setStyle(vboxStyle);
                    spriteBox.setAlignment(Pos.CENTER);
                    spriteBox.setOnMouseClicked(e -> openSpriteInEditor(spriteName, pathDirect));

                    tilePane.getChildren().add(spriteBox);
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
        return null;
    }
}

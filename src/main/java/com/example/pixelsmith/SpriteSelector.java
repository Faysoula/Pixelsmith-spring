package com.example.pixelsmith;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class SpriteSelector extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private Integer userId;
    private TilePane tilePane = new TilePane();
    private Map<Integer, String> spritePaths = new HashMap<>();

    public SpriteSelector(Integer userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        tilePane.setPadding(new Insets(15, 15, 15, 15));
        tilePane.setVgap(15);
        tilePane.setHgap(15);
        tilePane.setAlignment(Pos.CENTER);
        tilePane.setStyle("-fx-background-color: #444444; -fx-background-insets: 5;");

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #444444; -fx-background-color: #444444;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            tilePane.getChildren().clear();
            populateSpriteList();
        });
        refreshButton.setStyle("-fx-background-color: #555555; -fx-text-fill: #fff; -fx-font-weight: bold; -fx-padding: 10;");

        Button createSpriteButton = new Button();
        Image buttonImage = new Image("createnew.png");
        ImageView imageView = new ImageView(buttonImage);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        createSpriteButton.setGraphic(imageView);
        createSpriteButton.setOnAction(e -> createAndOpenNewSprite());

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
            tilePane.getChildren().clear();
            populateSpriteList();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void populateSpriteList() {
        String url = BASE_URL + "/sprites/user/" + userId;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONArray sprites = new JSONArray(response.body());
                for (int i = 0; i < sprites.length(); i++) {
                    JSONObject sprite = sprites.getJSONObject(i);
                    int spriteId = sprite.getInt("spriteId");  // Assume there is an "id" field in the JSON
                    String spriteName = sprite.getString("name");
                    String pathDirect = sprite.getJSONObject("spriteData").getString("pathDirect");

                    spritePaths.put(spriteId, pathDirect);  // Store by ID

                    Image spriteImage = new Image(new File(pathDirect).toURI().toString(), true);
                    ImageView imageView = new ImageView(spriteImage);
                    imageView.setFitHeight(100);
                    imageView.setFitWidth(100);

                    Text spriteText = new Text(spriteName);
                    VBox spriteBox = new VBox(5, imageView, spriteText);
                    spriteBox.setStyle("-fx-padding: 10; " +
                            "-fx-border-style: solid inside; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-insets: 5; " +
                            "-fx-border-radius: 5; " +
                            "-fx-border-color: #555; " +
                            "-fx-background-color: #333333; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");
                    spriteBox.setAlignment(Pos.CENTER);
                    spriteBox.setOnMouseClicked(e -> openSpriteInEditor(spriteId));  // Pass ID
                    tilePane.getChildren().add(spriteBox);
                }
            }
        } catch (Exception ex) {
            System.out.println("HTTP Request failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openSpriteInEditor(int spriteId) {
        String pathToSprite = spritePaths.get(spriteId);
        if (pathToSprite != null && !pathToSprite.isEmpty()) {
            Platform.runLater(() -> {
                try {
                    PixelArtEditor editor = PixelArtEditor.getInstance();
                    Stage editorStage = new Stage();
                    editor.openSprite(spriteId, pathToSprite, editorStage);
                } catch (Exception e) {
                    System.out.println("Error opening sprite editor: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("Path not found for sprite ID: " + spriteId);
        }
    }


}
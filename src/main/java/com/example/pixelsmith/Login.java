package com.example.pixelsmith;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class Login extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";

    @Override
    public void start(Stage primaryStage) {
        // Email and Password fields
        TextField emailField = new TextField();
        emailField.setPromptText("EMAIL");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("PASSWORD");

        Button loginButton = new Button("Log In");
        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String enteredPassword = passwordField.getText();
            try {
                Optional<Integer> userId = authenticate(email, enteredPassword);
                if (userId.isPresent()) {
                    UserSession.setCurrentUserId(userId.get());
                    new SpriteSelector(userId.get()).start(new Stage());
                    primaryStage.close();
                } else {
                    showAlert("Login Failed", "Invalid email or password.");
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        Label titleLabel = new Label("SMITH IN");
        GridPane.setHalignment(titleLabel, HPos.CENTER);

        VBox layout = new VBox(10, titleLabel,emailField, passwordField, loginButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login");
        primaryStage.show();

        emailField.getStyleClass().add("text-field");
        passwordField.getStyleClass().add("text-field");
        loginButton.getStyleClass().add("button");
        titleLabel.getStyleClass().add("title");

        // Set up the layout
        layout.getStyleClass().add("vbox");

        // Create the scene and apply the CSS
        scene.getStylesheets().add("login.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("Login");
        Image applicationIcon = new Image("icon.png");
        primaryStage.getIcons().add(applicationIcon);
        primaryStage.show();
    }

    private Optional<Integer> authenticate(String email, String password) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = BASE_URL + "/users/login";
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("passwordhash", password);  // Use "passwordhash" to align with server-side field

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response JSON: " + response.body());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return Optional.of(jsonResponse.getInt("userId"));
        } else {
            return Optional.empty();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

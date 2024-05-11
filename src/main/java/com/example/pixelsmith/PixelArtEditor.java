package com.example.pixelsmith;
// PixelArtEditor.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class PixelArtEditor extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static int CANVAS_WIDTH = 2000;
    private static int CANVAS_HEIGHT = 2000;
    private static final int GRID_SIZE = 16;
    private static int ROWS = CANVAS_HEIGHT / GRID_SIZE;
    private static int COLS = CANVAS_WIDTH / GRID_SIZE;
    private Color[][] pixels = new Color[ROWS][COLS];
    private GraphicsContext gc;
    private ColorPicker colorPicker;
    private Tool currentTool;
    private Integer currentSpriteId = null; // Null indicates a new sprite
    private String currentSpritePath = null; // Path to the saved sprite image
    private final int[] toolSizes = new int[]{1, 2, 3, 4};

    //external methods
    public void openSprite(int spriteId, String pathToSprite,Stage primaryStage) {
        this.currentSpriteId = spriteId;
        this.currentSpritePath = pathToSprite;
        start(primaryStage);
        openSpriteForEditing(spriteId, pathToSprite);
    }



    // singleton
    private static PixelArtEditor instance;

    private PixelArtEditor() {
    }

    public static PixelArtEditor getInstance() {
        if (instance == null) {
            instance = new PixelArtEditor();
        }
        return instance;
    }

    private void saveCurrentSprite(Stage primaryStage) {
        Image spriteSheet = renderSpriteSheet();
        if (currentSpriteId == null) {  // First-time save
            TextInputDialog dialog = new TextInputDialog("New Sprite");
            dialog.setTitle("Save Sprite");
            dialog.setHeaderText("Enter a name for your sprite:");
            dialog.setContentText("Name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(spriteName -> {
                String path = saveSpriteSheet(spriteSheet, primaryStage);
                if (path != null) {
                    currentSpritePath = path;
                    int userId = UserSession.getCurrentUserId();
                    createNewSprite(spriteName, userId, path);
                }
            });
        } else {  // Update existing sprite
            updateExistingSprite(currentSpriteId, currentSpritePath, spriteSheet);
        }
    }

    private String fetchSpriteNameById(Integer spriteId) {
        String url = BASE_URL + "/sprites/" + spriteId + "/name";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body(); // Returns the sprite name
            } else {
                System.err.println("Failed to fetch sprite name. Status: " + response.statusCode());
                return null; // Handle error appropriately
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching sprite name: " + e.getMessage());
            return null; // Handle exception appropriately
        }
    }

    void createNewSprite(String spriteName, int userId, String pathToSprite) {
        String url = "http://localhost:8080/api/sprites/create";
        HttpClient client = HttpClient.newHttpClient();
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", spriteName);
        JSONObject userObject = new JSONObject().put("userId", userId);
        requestBody.put("user", userObject);
        JSONObject spriteDataObject = new JSONObject().put("pathDirect", pathToSprite);
        requestBody.put("spriteData", spriteDataObject);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response JSON: " + response.body());
            if (response.statusCode() == 201) {
                JSONObject jsonResponse = new JSONObject(response.body());
                currentSpriteId = jsonResponse.getInt("spriteId");
            } else {
                System.err.println("Failed to create sprite. Server responded with status: " + response.statusCode());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void updateExistingSprite(Integer spriteId, String pathDirect, Image spriteSheet) {
        // Prompt user for the new sprite name
        TextInputDialog dialog = new TextInputDialog(); // Default text is empty
        dialog.setTitle("Update Sprite Name");
        dialog.setHeaderText("Updated sprite name:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent() || result.get().trim().isEmpty()) {
            System.out.println("Sprite update cancelled by user.");
            return; // Exit if user cancels or submits empty name
        }
        String newSpriteName = result.get();

        // Proceed with update
        String url = BASE_URL + "/sprites/" + spriteId;
        HttpClient client = HttpClient.newHttpClient();
        JSONObject requestBody = new JSONObject();
        requestBody.put("spriteId", spriteId);
        requestBody.put("name", newSpriteName);  // Use the new name provided by the user
        requestBody.put("pathDirect", pathDirect);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        saveUpdatedSpriteSheet(spriteSheet, pathDirect);

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        System.out.println("Sprite updated successfully.");
                    } else {
                        System.err.println("Failed to update sprite. Server responded with status: " + statusCode);
                    }
                })
                .join(); // Note: Using join() is generally not recommended on the UI thread
    }




    private void saveUpdatedSpriteSheet(Image spriteSheet, String filePath) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(spriteSheet, null), "png", new File(filePath));
        } catch (IOException e) {
            System.out.println("Error saving the updated sprite sheet: " + e.getMessage());
        }
    }

    private void openSpriteForEditing(int spriteId, String pathToSprite) {
        try {
            File spriteFile = new File(pathToSprite);
            if (!spriteFile.exists()) {
                System.out.println("Sprite file not found: " + pathToSprite);
                return;
            }

            // Load the sprite image
            Image spriteImage = new Image(new FileInputStream(spriteFile));
            PixelReader pixelReader = spriteImage.getPixelReader();

            // Determine the size of the sprite image
            int spriteSheetRows = (int) spriteImage.getHeight();
            int spriteSheetCols = (int) spriteImage.getWidth();

            // Resize the canvas and pixel array to match the sprite image size
            CANVAS_WIDTH = spriteSheetCols * GRID_SIZE;
            CANVAS_HEIGHT = spriteSheetRows * GRID_SIZE;
            ROWS = spriteSheetRows;
            COLS = spriteSheetCols;
            pixels = new Color[ROWS][COLS];

            // Update the pixel array based on the loaded image
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Color color = pixelReader.getColor(col, row);
                    pixels[row][col] = color;
                    renderPixel(row, col);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error loading the sprite file: " + e.getMessage());
        }
    }



    // Tool interface
    interface Tool {
        void apply(int row, int col);

        default void setToolSize(int size) {
        }
    }

    // Pen tool
    class PenTool implements Tool {
        private int size = 1;

        public void apply(int row, int col) {
            for (int r = row - size + 1; r < row + size; r++) {
                for (int c = col - size + 1; c < col + size; c++) {
                    if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                        pixels[r][c] = colorPicker.getValue();
                        renderPixel(r, c);
                    }
                }
            }
        }

        public void setToolSize(int size) {
            this.size = size;
        }
    }

    class EyeDropperTool implements Tool {
        public void apply(int row, int col) {
            colorPicker.setValue(pixels[row][col]);
        }
    }

    //square tool
    class SquareTool implements Tool {
        private int startX, startY; // Starting coordinates

        @Override
        public void apply(int row, int col) {
        }

        // Start drawing the square
        public void onMousePressed(int row, int col) {
            startX = col;
            startY = row;
        }

        // Finish drawing the square
        public void onMouseReleased(int row, int col) {
            // Calculate the square's boundaries
            int minX = Math.min(startX, col);
            int maxX = Math.max(startX, col);

            int minY = Math.min(startY, row);
            int maxY = Math.max(startY, row);

            // Draw the square
            for (int r = minY; r <= maxY; r++) {
                for (int c = minX; c <= maxX; c++) {
                    if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                        pixels[r][c] = colorPicker.getValue();
                        renderPixel(r, c);
                    }
                }
            }
        }
    }

    // Eraser tool
    class EraserTool implements Tool {
        private int size = 1;

        public void apply(int row, int col) {
            for (int r = row - size + 1; r < row + size; r++) {
                for (int c = col - size + 1; c < col + size; c++) {
                    if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                        pixels[r][c] = getCheckerboardColor(r, c);
                        renderPixel(r, c);
                    }
                }
            }
        }

        public void setToolSize(int size) {
            this.size = size;
        }
    }

    class FillTool implements Tool {
        @Override
        public void apply(int row, int col) {
            Color targetColor = pixels[row][col];
            Color replacementColor = colorPicker.getValue();

            // Don't fill if the selected color is the same as the target color
            if (targetColor.equals(replacementColor)) {
                return;
            }

            floodFill(row, col, targetColor, replacementColor);
        }

        private void floodFill(int startRow, int startCol, Color targetColor, Color replacementColor) {
            Queue<int[]> queue = new LinkedList<>();
            queue.add(new int[]{startRow, startCol});

            while (!queue.isEmpty()) {
                int[] position = queue.remove();
                int row = position[0], col = position[1];

                // Check boundaries and whether to continue filling
                if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
                    continue;
                }

                Color currentColor = pixels[row][col];
                if (!isFillableColor(currentColor, targetColor)) {
                    continue;
                }

                pixels[row][col] = replacementColor;
                renderPixel(row, col);

                // Add neighboring pixels to the queue
                queue.add(new int[]{row, col - 1}); // Left
                queue.add(new int[]{row, col + 1}); // Right
                queue.add(new int[]{row - 1, col}); // Above
                queue.add(new int[]{row + 1, col}); // Below
            }
        }

        private boolean isFillableColor(Color currentColor, Color targetColor) {
            // Check if the current color is either the target color or part of the checkerboard pattern
            return currentColor.equals(targetColor) || currentColor.equals(getCheckerboardColor(0, 0)) || currentColor.equals(getCheckerboardColor(0, 1));
        }
    }

    //line tool
    class LineTool implements Tool {
        private int startX, startY; // Starting coordinates
        private boolean drawing = false;

        @Override
        public void apply(int row, int col) {
            if (!drawing) {
                startX = col;
                startY = row;
                drawing = true;
            } else {
                drawBresenhamLine(startX, startY, col, row);
                drawing = false;
            }
        }
    }

    private void drawBresenhamLine(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;
        int e2;

        while (true) {
            if (x1 >= 0 && x1 < COLS && y1 >= 0 && y1 < ROWS) {
                pixels[y1][x1] = colorPicker.getValue();
                renderPixel(y1, x1);
            }

            if (x1 == x2 && y1 == y2) {
                break;
            }

            e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    // Initialize the grid with a checkerboard pattern
    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = COLS - 1; col >= 0; col--) {
                pixels[row][col] = getCheckerboardColor(row, col);
            }
        }
    }

    // Render the grid based on the pixel data structure
    private void renderGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = COLS - 1; col >= 0; col--) {
                renderPixel(row, col);
            }
        }
    }

    // Render a single pixel
    private void renderPixel(int row, int col) {
        gc.setFill(pixels[row][col]);
        gc.fillRect(col * GRID_SIZE, row * GRID_SIZE, GRID_SIZE, GRID_SIZE);
    }

    private Image renderSpriteSheet() {
        WritableImage spriteSheet = new WritableImage(COLS, ROWS);
        PixelWriter pixelWriter = spriteSheet.getPixelWriter();

        for (int row = 0; row < ROWS; row++) {
            for (int col = COLS - 1; col >= 0; col--) {
                Color pixelColor = pixels[row][col];
                // If the pixel color matches the checkerboard color, write black color
                if (pixelColor.equals(getCheckerboardColor(row, col))) {
                    pixelWriter.setColor(col, row, Color.TRANSPARENT);
                } else {
                    pixelWriter.setColor(col, row, pixelColor);
                }
            }
        }

        return spriteSheet;
    }

    // Determine the checkerboard pattern color based on the position
    private Color getCheckerboardColor(int row, int col) {
        if ((row + col) % 2 == 0) {
            return Color.rgb(160, 160, 160); // Light check
        } else {
            return Color.rgb(96, 96, 96); // Dark check
        }
    }

    // Create a new sprite editor
    void createNewSpriteEditor(String spriteName) {
        Stage newSpriteStage = new Stage();
        newSpriteStage.setTitle(spriteName);
        PixelArtEditor editor = new PixelArtEditor();
        CANVAS_HEIGHT = 2000;
        CANVAS_WIDTH = 2000;
        editor.start(newSpriteStage); // Start the new editor on a new stage
    }

    // Load and display a sprite sheet from a file
    private void loadAndDisplaySpriteSheet(File file) {
        try {
            clearCanvas();
            // Load the sprite sheet with no scaling or smoothing so the sprite aesthetic doesnt get ruined
            Image spriteSheet = new Image(new FileInputStream(file));
            PixelReader pixelReader = spriteSheet.getPixelReader();

            // Determine the size of the sprite sheet in terms of the grid
            int spriteSheetRows = (int) spriteSheet.getHeight();
            int spriteSheetCols = (int) spriteSheet.getWidth();

            // Resize the canvas and pixel array to match the sprite sheet size
            CANVAS_WIDTH = spriteSheetCols;
            CANVAS_HEIGHT = spriteSheetRows;
            ROWS = CANVAS_HEIGHT;
            COLS = CANVAS_WIDTH;
            pixels = new Color[ROWS][COLS];

            // Update the pixel array based on the loaded image
            for (int row = 0; row < spriteSheetRows; row++) {
                for (int col = spriteSheetCols - 1; col >= 0; col--) {
                    // Read the color of the pixel
                    Color color = pixelReader.getColor(col, row);

                    // Update the pixels array with the color from the sprite sheet
                    pixels[row][col] = color;
                    renderPixel(row, col);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("error loading the sprite file :(");
        }
    }

    private String saveSpriteSheet(Image spriteSheet, Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sprite Sheet");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png")
        );
        File file = fileChooser.showSaveDialog(primaryStage);

        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(spriteSheet, null), "png", file);
                return file.getAbsolutePath(); // Return the saved file path
            } catch (IOException e) {
                System.out.println("Error saving the sprite sheet :(");
            }
        }

        return null;
    }

    private void clearCanvas() {
        initializeGrid();
        renderGrid();
    }

    @Override
    public void start(Stage primaryStage) {
        initializeGrid();
        BorderPane root = new BorderPane();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        renderGrid();

        // Initialize color picker
        colorPicker = new ColorPicker(Color.BLACK);

        // Initialize toolbar and tools
        ToolBar toolBar = new ToolBar();
        ToggleGroup toolsGroup = new ToggleGroup();

        ToggleButton penToolButton = new ToggleButton();
        ToggleButton eraserToolButton = new ToggleButton();
        ToggleButton fillToolButton = new ToggleButton();
        ToggleButton squareToolButton = new ToggleButton();
        ToggleButton lineToolButton = new ToggleButton();
        Button saveProgressButton = new Button();

        SquareTool squareTool = new SquareTool();

        penToolButton.setToggleGroup(toolsGroup);
        eraserToolButton.setToggleGroup(toolsGroup);
        fillToolButton.setToggleGroup(toolsGroup);
        squareToolButton.setToggleGroup(toolsGroup);
        saveProgressButton.setOnAction(e -> saveCurrentSprite(primaryStage));

        penToolButton.setSelected(true); // Pen tool is selected by default
        currentTool = new PenTool(); // Default tool

        penToolButton.setOnAction(e -> currentTool = new PenTool());
        eraserToolButton.setOnAction(e -> currentTool = new EraserTool());
        fillToolButton.setOnAction(e -> currentTool = new FillTool());
        squareToolButton.setOnAction(e -> currentTool = squareTool);

        lineToolButton.setToggleGroup(toolsGroup);
        lineToolButton.setOnAction(e -> currentTool = new LineTool());

        root.setCenter(canvas);

        //clear button
        Button clearCanvasButton = new Button();
        clearCanvasButton.setOnAction(e -> clearCanvas());

        canvas.setOnMouseClicked(e -> {
            if (e.isPrimaryButtonDown()) {
                applyTool(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                applyTool(e.getX(), e.getY());
            }
        });

        canvas.setOnMousePressed(e -> {
            if (currentTool instanceof SquareTool) {
                squareTool.onMousePressed((int) e.getY() / GRID_SIZE, (int) e.getX() / GRID_SIZE);
            } else {
                applyTool(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (currentTool instanceof SquareTool) {
                squareTool.onMouseReleased((int) e.getY() / GRID_SIZE, (int) e.getX() / GRID_SIZE);
            }
        });

        Button createSpriteButton = new Button();
        createSpriteButton.setOnAction(e -> {
            createNewSpriteEditor("new sprite");
        });

        Slider sizeSlider = new Slider(0, toolSizes.length - 1, 0);
        sizeSlider.setMajorTickUnit(1);
        sizeSlider.setSnapToTicks(true);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMinorTickCount(0);
        sizeSlider.setBlockIncrement(1);

        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int sizeIndex = newValue.intValue();
            int selectedSize = toolSizes[sizeIndex];
            if (currentTool instanceof PenTool || currentTool instanceof EraserTool) {
                currentTool.setToolSize(selectedSize);
            }
        });
        // Add size controls to the toolbar
        Label sizeLabel = new Label("Tool Size:");

        Button importSpriteButton = new Button("");
        importSpriteButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Sprite Sheet");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                loadAndDisplaySpriteSheet(selectedFile);
            }
        });

        canvas.setOnScroll(event -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 1 / zoomFactor;
            }

            canvas.setScaleX(canvas.getScaleX() * zoomFactor);
            canvas.setScaleY(canvas.getScaleY() * zoomFactor);

            // Adjust the position of the canvas to center the zoom on the cursor
            double mouseX = event.getX();
            double mouseY = event.getY();

            double adjustX = (zoomFactor - 1) * (canvas.getTranslateX() - mouseX);
            double adjustY = (zoomFactor - 1) * (canvas.getTranslateY() - mouseY);

            canvas.setTranslateX(canvas.getTranslateX() - adjustX);
            canvas.setTranslateY(canvas.getTranslateY() - adjustY);

            event.consume();
        });

        Button exportButton = new Button();
        exportButton.setOnAction(e -> {
            Image spriteSheet = renderSpriteSheet();
            String savedPath = saveSpriteSheet(spriteSheet, primaryStage);
            if (savedPath != null) {
                System.out.println("Saved Sprite Sheet at: " + savedPath);
            }
        });

        //eyedropper tool
        ToggleButton eyeDropperToolButton = new ToggleButton();
        eyeDropperToolButton.setToggleGroup(toolsGroup);
        eyeDropperToolButton.setOnAction(e -> currentTool = new EyeDropperTool());

        // Add the tools to the toolbar
        toolBar.getItems().addAll(penToolButton, eraserToolButton, fillToolButton, eyeDropperToolButton, colorPicker, sizeLabel, sizeSlider, squareToolButton,
                lineToolButton, createSpriteButton, importSpriteButton, exportButton, saveProgressButton, clearCanvasButton);

        root.setTop(toolBar);

        Scene scene = new Scene(root, CANVAS_WIDTH + 100, CANVAS_HEIGHT);
        final double[] lastKnownPosition = new double[2];

        canvas.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown()) {
                // Record the last position when the right mouse button is pressed
                lastKnownPosition[0] = e.getSceneX();
                lastKnownPosition[1] = e.getSceneY();
            } else if (currentTool instanceof SquareTool) {
                squareTool.onMousePressed((int) e.getY() / GRID_SIZE, (int) e.getX() / GRID_SIZE);
            } else {
                applyTool(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown()) {
                // Calculate the change in mouse position
                double deltaX = e.getSceneX() - lastKnownPosition[0];
                double deltaY = e.getSceneY() - lastKnownPosition[1];

                // Update the last position to the new position
                lastKnownPosition[0] = e.getSceneX();
                lastKnownPosition[1] = e.getSceneY();

                // Apply the translation to the canvas
                canvas.setTranslateX(canvas.getTranslateX() + deltaX);
                canvas.setTranslateY(canvas.getTranslateY() + deltaY);
            } else {
                applyTool(e.getX(), e.getY());
            }
        });

        //styling for all buttons
        scene.getStylesheets().add("dark-theme.css");
        penToolButton.getStyleClass().add("pen-tool-button");
        eraserToolButton.getStyleClass().add("ear-tool-button");
        fillToolButton.getStyleClass().add("fill-tool-button");
        eyeDropperToolButton.getStyleClass().add("eye");
        squareToolButton.getStyleClass().add("square");
        lineToolButton.getStyleClass().add("line");
        createSpriteButton.getStyleClass().add("createnew");
        importSpriteButton.getStyleClass().add("importimage");
        exportButton.getStyleClass().add("exportimage");
        saveProgressButton.getStyleClass().add("save");
        clearCanvasButton.getStyleClass().add("clear");

        primaryStage.setTitle("Pixel Art Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setWidth(800); // Set the width to 800 pixels
        primaryStage.setHeight(600);
        primaryStage.centerOnScreen(); // Center the stage on the screen
        Image applicationIcon = new Image("icon.png");
        primaryStage.getIcons().add(applicationIcon);
//      primaryStage.setFullScreen(true);
//        primaryStage.fullScreenProperty().addListener((observable, wasFullScreen, isNowFullScreen) -> {
//            if (wasFullScreen) {
//                primaryStage.setWidth(800); // Set the width to 800 pixels
//                primaryStage.setHeight(600); // Set the height to 600 pixels
//                primaryStage.centerOnScreen(); // Center the stage on the screen
//            }
//        });
        primaryStage.show();
    }

    private void applyTool(double x, double y) {
        int col = (int) x / GRID_SIZE;
        int row = (int) y / GRID_SIZE;

        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            currentTool.apply(row, col);
        }
    }

    public static void main(String[] args) {
        instance = new PixelArtEditor();
        launch(args);
    }
}


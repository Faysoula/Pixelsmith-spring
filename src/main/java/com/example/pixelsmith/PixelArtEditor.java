package com.example.pixelsmith;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;


public class PixelArtEditor extends Application {
    private static int CANVAS_WIDTH = 2000;
    private static int CANVAS_HEIGHT = 2000;
    private static final int GRID_SIZE = 16;
    private static int ROWS = CANVAS_HEIGHT / GRID_SIZE;
    private static int COLS = CANVAS_WIDTH / GRID_SIZE;
    private Color[][] pixels = new Color[ROWS][COLS];
    private GraphicsContext gc;
    private ColorPicker colorPicker;
    private Tool currentTool;
    //private double scale = 1.0;

    private final int[] toolSizes = new int[]{1, 2, 3, 4};

    /* sql stuff */
    private Connection getDBConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3307/pixelsmith", "root", "13102004");
    }

    private void saveSpriteToDB(String spriteName, int userId, String pathToSprite) {
        String insertSpriteQuery = "INSERT INTO Sprites (UserId, Name, CreationDate, LastModifiedDate) VALUES (?, ?, NOW(), NOW())";
        String insertSpriteDataQuery = "INSERT INTO spritedata (SpriteID, PathDirect) VALUES (?, ?)";

        try (Connection conn = getDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSpriteQuery, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, spriteName);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int spriteId = generatedKeys.getInt(1);
                    try (PreparedStatement pstmtData = conn.prepareStatement(insertSpriteDataQuery)) {
                        pstmtData.setInt(1, spriteId);
                        pstmtData.setString(2, pathToSprite);
                        pstmtData.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(); // Handle exception properly in production code
        }
    }



    private void openSpriteForEditing(int spriteId, String pathToSprite) {
        try {
            File spriteFile = new File(pathToSprite);
            if (!spriteFile.exists()) {
                // Handle the case where the file does not exist
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
            // For the square tool, apply doesn't do anything on a single click
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

    // Initialize the grid with a checkerboard pattern
    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                pixels[row][col] = getCheckerboardColor(row, col);
            }
        }
    }

    // Render the grid based on the pixel data structure
    private void renderGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                renderPixel(row, col);
            }
        }
    }

    // Render a single pixel
    private void renderPixel(int row, int col) {
        gc.setFill(pixels[row][col]);
        gc.fillRect(col * GRID_SIZE, row * GRID_SIZE, GRID_SIZE, GRID_SIZE);
    }

    // Determine the checkerboard pattern color based on the position
    private Color getCheckerboardColor(int row, int col) {
        if ((row + col) % 2 == 0) {
            return Color.rgb(160, 160, 160); // Light check
        } else {
            return Color.rgb(96, 96, 96); // Dark check
        }
    }

    private void createNewSpriteEditor(String spriteName) {
        Stage newSpriteStage = new Stage();
        newSpriteStage.setTitle(spriteName);

        PixelArtEditor editor = new PixelArtEditor();
        CANVAS_HEIGHT = 2000;
        CANVAS_WIDTH = 2000;
        editor.start(newSpriteStage); // Start the new editor on a new stage
    }

    private void loadAndDisplaySpriteSheet(File file) {
        try {
            // Load the sprite sheet with no scaling or smoothing
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
                for (int col = 0; col < spriteSheetCols; col++) {
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

    private Image renderSpriteSheet() {
        WritableImage spriteSheet = new WritableImage(COLS, ROWS);
        PixelWriter pixelWriter = spriteSheet.getPixelWriter();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
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

        ToggleButton penToolButton = new ToggleButton("Pen");
        ToggleButton eraserToolButton = new ToggleButton("Eraser");
        ToggleButton fillToolButton = new ToggleButton("Fill");
        ToggleButton squareToolButton = new ToggleButton("Square");

        SquareTool squareTool = new SquareTool();

        penToolButton.setToggleGroup(toolsGroup);
        eraserToolButton.setToggleGroup(toolsGroup);
        fillToolButton.setToggleGroup(toolsGroup);
        squareToolButton.setToggleGroup(toolsGroup);

        penToolButton.setSelected(true); // Pen tool is selected by default
        currentTool = new PenTool(); // Default tool

        penToolButton.setOnAction(e -> currentTool = new PenTool());
        eraserToolButton.setOnAction(e -> currentTool = new EraserTool());
        fillToolButton.setOnAction(e -> currentTool = new FillTool());
        squareToolButton.setOnAction(e -> currentTool = squareTool);

        // Add the toolbar on the left and canvas in the center
        root.setCenter(canvas);

        // Keep track of the previous tool

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

        Button createSpriteButton = new Button("Create Sprite");
        createSpriteButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("New Sprite");
            dialog.setTitle("Create New Sprite");
            dialog.setHeaderText("Enter the name for the new sprite:");
            dialog.setContentText("Name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(spriteName -> {
                String savedPath = saveSpriteSheet(renderSpriteSheet(), primaryStage);
                if (savedPath != null) {
                    // Assuming you have a way to get the userId of the logged-in user
                    int userId = UserSession.getCurrentUserId();
                    saveSpriteToDB(spriteName, userId, savedPath);
                }
                createNewSpriteEditor(spriteName);
            });
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


        Button importSpriteButton = new Button("Import Sprite Sheet");
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

        Button exportButton = new Button("Export Sprite Sheet");
        exportButton.setOnAction(e -> {
            Image spriteSheet = renderSpriteSheet();
            String savedPath = saveSpriteSheet(spriteSheet, primaryStage);
            if (savedPath != null) {
                System.out.println("Saved Sprite Sheet at: " + savedPath);
                // 'savedPath' in a variable for further use
            }
        });

        //eyedropper tool
        ToggleButton eyeDropperToolButton = new ToggleButton("Eye Dropper");
        eyeDropperToolButton.setToggleGroup(toolsGroup);
        eyeDropperToolButton.setOnAction(e -> currentTool = new EyeDropperTool());

        toolBar.getItems().addAll(penToolButton, eraserToolButton, fillToolButton, eyeDropperToolButton, squareToolButton, createSpriteButton,
                importSpriteButton, colorPicker, sizeLabel, sizeSlider, exportButton);
        root.setTop(toolBar);

        Scene scene = new Scene(root, CANVAS_WIDTH + 100, CANVAS_HEIGHT);
        final double[] lastKnownPosition = new double[2];

        canvas.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown()) {
                // Record the last position when the right mouse button is pressed
                lastKnownPosition[0] = e.getSceneX();
                lastKnownPosition[1] = e.getSceneY();
            } else if (currentTool instanceof SquareTool) {
                // Your existing tool logic
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

        scene.getStylesheets().add("dark-theme.css");
        primaryStage.setTitle("Pixel Art Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setFullScreen(true);
        primaryStage.fullScreenProperty().addListener((observable, wasFullScreen, isNowFullScreen) -> {
            if (wasFullScreen) {
                primaryStage.setWidth(800); // Set the width to 800 pixels
                primaryStage.setHeight(600); // Set the height to 600 pixels
                primaryStage.centerOnScreen(); // Center the stage on the screen
            }
        });
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
        launch(args);
    }
}

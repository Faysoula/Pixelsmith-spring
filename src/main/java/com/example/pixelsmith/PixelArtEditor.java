package com.example.pixelsmith;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class PixelArtEditor extends Application {

    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 800;
    private static final int GRID_SIZE = 16;
    private static final int ROWS = CANVAS_HEIGHT / GRID_SIZE;
    private static final int COLS = CANVAS_WIDTH / GRID_SIZE;
    private final Color[][] pixels = new Color[ROWS][COLS];
    private GraphicsContext gc;
    private ColorPicker colorPicker;
    private Tool currentTool;

    private int penSize = 1;
    private int eraserSize = 1;

    // Tool interface
    interface Tool {
        void apply(int row, int col);
        default void setToolSize(int size){}
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
            int endX = col;
            int endY = row;

            // Calculate the square's boundaries
            int minX = Math.min(startX, endX);
            int maxX = Math.max(startX, endX);
            int minY = Math.min(startY, endY);
            int maxY = Math.max(startY, endY);

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
                if (!isFillableColor(currentColor, targetColor, replacementColor)) {
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

        private boolean isFillableColor(Color currentColor, Color targetColor, Color replacementColor) {
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

        PixelArtEditor editor = new PixelArtEditor(); // Create a new instance of the editor
        editor.start(newSpriteStage); // Start the new editor on a new stage
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
        squareToolButton.setOnAction(e-> currentTool = squareTool);
        toolBar.getItems().addAll(penToolButton, eraserToolButton, fillToolButton,squareToolButton, colorPicker);

        // Add the toolbar on the left and canvas in the center
        root.setLeft(toolBar);
        root.setCenter(canvas);

        // Handle the drawing on canvas
        canvas.setOnMouseClicked(e -> applyTool(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> applyTool(e.getX(), e.getY()));

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
            result.ifPresent(this::createNewSpriteEditor);
        });
        Slider penSizeSlider = new Slider(1, GRID_SIZE, 1);
        penSizeSlider.setOrientation(Orientation.HORIZONTAL);
        penSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            penSize = newValue.intValue();
            if (currentTool instanceof PenTool) {
                currentTool.setToolSize(penSize);
            }
        });

        Slider eraserSizeSlider = new Slider(1, GRID_SIZE, 1);
        eraserSizeSlider.setOrientation(Orientation.HORIZONTAL);
        eraserSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            eraserSize = newValue.intValue();
            if (currentTool instanceof EraserTool) {
                currentTool.setToolSize(eraserSize);
            }
        });

        Label penSizeLabel = new Label("Pen Size:");
        Label eraserSizeLabel = new Label("Eraser Size:");

        // Add size controls to the toolbar
        toolBar.getItems().addAll(penSizeLabel, penSizeSlider, eraserSizeLabel, eraserSizeSlider);

        // Add Create Sprite button to the toolBar
        toolBar.getItems().add(createSpriteButton);

        // Scene with styling
        Scene scene = new Scene(root, CANVAS_WIDTH + 100, CANVAS_HEIGHT);
        primaryStage.setTitle("Pixel Art Editor");
        primaryStage.setScene(scene);
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

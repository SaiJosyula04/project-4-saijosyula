/**
 * @author Sai Josyula
 */
package cryptogram;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;

/**
 * JavaFX GUI view for the Cryptogram game.
 * <p>
 * Layout:
 * <ul>
 *   <li>BorderPane root</li>
 *   <li>Center: ScrollPane(GridPane board) — a 30-column grid of cells</li>
 *   <li>Right: VBox controls — New Puzzle, Hint, letter-frequency checkbox/table</li>
 * </ul>
 * Each board cell is a VBox with a 1-char {@link TextField} above a {@link Label} for the cipher letter.
 * Non-letter cells show the punctuation/space in both places and are disabled.
 * <p>
 * Uses Observer so the model can notify this view when a specific cipher letter changes.
 * Only fields for that cipher letter are updated on each change.
 * Also provides immediate visual feedback: green when the guess is correct, red when wrong.
 */
@SuppressWarnings("deprecation")
public class CryptogramGUIView extends Application implements java.util.Observer {

    /** The shared model. */
    private CryptogramModel model;

    /** The shared controller. */
    private CryptogramController controller;

    /** Board grid container. */
    private GridPane grid;

    /** Map from cipher letter to all TextFields representing it. */
    private Map<Character, List<TextField>> fieldsByCipher;

    /** Locked after win to prevent further edits until New Puzzle. */
    private boolean locked = false;

    /** Container for the frequency table. */
    private VBox freqBox;

    /** Toggle to show/hide the frequency table. */
    private CheckBox freqToggle;

    /**
     * JavaFX entry point for the GUI.
     *
     * @param stage the primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        model = new CryptogramModel();
        controller = new CryptogramController(model);
        model.addObserver(this);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(8);
        grid.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        root.setCenter(scroll);

        VBox right = new VBox(12);
        right.setAlignment(Pos.TOP_CENTER);
        right.setPadding(new Insets(12));

        Button newBtn = new Button("New Puzzle");
        Button hintBtn = new Button("Hint");
        freqToggle = new CheckBox("Show letter frequencies");

        // Color legend for correctness
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            swatch("#d4edda"), new Label("correct"),
            swatch("#f8d7da"), new Label("wrong")
        );

        freqBox = new VBox(6);
        freqBox.setPadding(new Insets(8));
        freqBox.setVisible(false);
        freqBox.setManaged(false);
        freqBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 8; -fx-padding: 6;");

        newBtn.setOnAction(_ -> { controller.newPuzzle(); locked = false; });
        hintBtn.setOnAction(_ -> { if (!locked) { controller.hint(); checkWin(); }});
        freqToggle.setOnAction(_ -> rebuildFreqBox());

        right.getChildren().addAll(newBtn, hintBtn, freqToggle, freqBox, legend);
        root.setRight(right);

        controller.newPuzzle();

        Scene scene = new Scene(root, 1200, 720);
        stage.setScene(scene);
        stage.setTitle("Cryptogram – CSC 335 Project 4");
        stage.show();
    }

    // --------------------- Private helpers ---------------------

    /**
     * Creates a small colored square used in the right-side legend.
     *
     * @param color a CSS color (e.g., {@code #d4edda})
     * @return a region styled to appear as a color swatch
     */
    private Region swatch(String color) {
        Region r = new Region();
        r.setPrefSize(14, 14);
        r.setStyle("-fx-background-color:" + color + "; -fx-border-color:#bbb;");
        return r;
    }

    /**
     * Rebuilds the entire board from the model's encrypted quote.
     * <p>
     * Called on a new puzzle. Builds 30 columns worth of cells, each containing
     * a {@link TextField} (for guess input) and a {@link Label} (cipher letter).
     * Non-letter cells are shown disabled with the punctuation/space.
     * Key handlers are attached to letter cells to capture single-letter input and
     * propagate guesses via the controller.
     */
    private void rebuildBoard() {
        grid.getChildren().clear();
        fieldsByCipher = new HashMap<>();

        String enc = model.getEncryptedQuote();
        final int cols = 30;
        int col = 0, row = 0;

        for (int i = 0; i < enc.length(); i++) {
            char ch = enc.charAt(i);

            VBox cell = new VBox(6);
            cell.setAlignment(Pos.CENTER);

            // Larger, tile-like TextField
            TextField tf = new TextField();
            tf.setPrefColumnCount(1);
            tf.setAlignment(Pos.CENTER);
            tf.setFont(Font.font(10));
            tf.setPrefWidth(50);
            tf.setPrefHeight(50);
            tf.setStyle("-fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#ccc;");

            Label lbl = new Label(String.valueOf(ch));
            lbl.setFont(Font.font(16));

            if (!Character.isLetter(ch)) {
                tf.setText(String.valueOf(ch));
                tf.setDisable(true);
                tf.setStyle("-fx-background-color:#f5f5f5; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#ccc;");
            } else {
                char cipher = Character.toUpperCase(ch);
                fieldsByCipher.computeIfAbsent(cipher, __ -> new ArrayList<>()).add(tf);

                Character g = model.getGuess(cipher);
                tf.setText(g == null ? "" : String.valueOf(g));
                styleField(tf, cipher);  // initial correctness color

                tf.setOnKeyPressed(ev -> {
                    if (locked) { ev.consume(); return; }
                    switch (ev.getCode()) {
                        case BACK_SPACE:
                        case DELETE:
                            controller.guess(cipher, null);
                            ev.consume(); // prevent stray characters
                            break;
                        default:
                            String txt = ev.getText();
                            if (txt != null && txt.length() == 1 && Character.isLetter(txt.charAt(0))) {
                                char plain = Character.toUpperCase(txt.charAt(0));
                                controller.guess(cipher, plain);
                                ev.consume(); // enforce single-letter input
                                checkWin();
                            }
                    }
                });
            }

            cell.getChildren().addAll(tf, lbl);
            grid.add(cell, col, row);
            if (++col >= cols) { col = 0; row++; }
        }
    }

    /**
     * Shows or hides the frequency panel on the right depending on the checkbox state.
     * The panel is rebuilt each time it becomes visible to reflect current puzzle.
     */
    private void rebuildFreqBox() {
        boolean show = freqToggle.isSelected();
        if (!show) {
            freqBox.setVisible(false);
            freqBox.setManaged(false);
            freqBox.getChildren().clear();
            return;
        }
        freqBox.setVisible(true);
        freqBox.setManaged(true);
        freqBox.getChildren().clear();

        GridPane table = new GridPane();
        table.setHgap(12);
        table.setVgap(6);

        int r = 0;
        table.add(new Label("Letter"), 0, r);
        table.add(new Label("Count"), 1, r);
        r++;

        Map<Character, Integer> freq = new TreeMap<>(model.getEncryptedFrequencies());
        for (Map.Entry<Character, Integer> e : freq.entrySet()) {
            table.add(new Label(String.valueOf(e.getKey())), 0, r);
            table.add(new Label(String.valueOf(e.getValue())), 1, r);
            r++;
        }
        freqBox.getChildren().add(table);
    }

    /**
     * Applies a visual style to a TextField representing a given cipher letter
     * based on whether the current guess is correct.
     *
     * @param tf     the text field to style
     * @param cipher the cipher letter this field represents
     */
    private void styleField(TextField tf, char cipher) {
        Character g = model.getGuess(cipher);
        if (g == null) {
            tf.setStyle("-fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#ccc;");
            return;
        }
        if (model.isCorrectGuess(cipher)) {
            tf.setStyle("-fx-background-color:#d4edda; -fx-background-insets:0; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#b2d8b9;");
        } else {
            tf.setStyle("-fx-background-color:#f8d7da; -fx-background-insets:0; -fx-background-radius:10; -fx-border-radius:10; -fx-border-color:#e0aeb4;");
        }
    }

    /**
     * Checks for a solved puzzle and, if solved, shows a modal alert and disables editing
     * until the user starts a new puzzle.
     */
    private void checkWin() {
        if (controller.isSolved()) {
            locked = true;
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.initModality(Modality.APPLICATION_MODAL);
            a.setTitle("You solved it!");
            a.setHeaderText("Nice work.");
            a.setContentText(model.getOriginalQuote());
            a.showAndWait();

            if (fieldsByCipher != null) {
                for (List<TextField> list : fieldsByCipher.values()) {
                    for (TextField tf : list) tf.setDisable(true);
                }
            }
        }
    }

    /**
     * Observer callback. Called by the model when either:
     * <ul>
     *   <li>{@code arg == null}: new puzzle installed; rebuild the entire board and (if needed) the frequency panel.</li>
     *   <li>{@code arg instanceof Character}: only that cipher letter changed; update just its fields and re-style them.</li>
     * </ul>
     *
     * @param o   the observable model
     * @param arg {@code null} or a {@link Character} cipher letter
     */
    @Override
    public void update(java.util.Observable o, Object arg) {
        Platform.runLater(() -> {
            if (arg == null) {
                rebuildBoard();
                rebuildFreqBox();
                return;
            }
            if (arg instanceof Character) {
                char cipher = (Character) arg;
                Character guess = model.getGuess(cipher);
                List<TextField> fields = fieldsByCipher.get(Character.toUpperCase(cipher));
                if (fields != null) {
                    String text = (guess == null) ? "" : String.valueOf(guess);
                    for (TextField tf : fields) {
                        tf.setText(text);
                        styleField(tf, cipher);
                    }
                }
                if (freqToggle.isSelected()) rebuildFreqBox();
            }
        });
    }
}

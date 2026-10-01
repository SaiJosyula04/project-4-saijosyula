/**
 * @author Sai Josyula
 */
package cryptogram;

/**
 * Entry point for the Cryptogram application.
 * <p>
 * Launches either the JavaFX GUI (default) or the text-based UI (when -text is passed).
 * The -window flag forces GUI explicitly. This class performs no game logic by itself;
 * it only wires the correct view to a fresh {@link CryptogramController} and {@link CryptogramModel}.
 */
public class Cryptogram {
    /**
     * Program entry point. Use {@code -text} for console UI, {@code -window} for GUI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        boolean text = false;
        for (String a : args) {
            if ("-text".equalsIgnoreCase(a)) text = true;
            if ("-window".equalsIgnoreCase(a)) text = false;
        }
        if (text) {
            CryptogramModel model = new CryptogramModel();
            CryptogramController controller = new CryptogramController(model);
            CryptogramTextView view = new CryptogramTextView(controller, model);
            view.run();
        } else {
            javafx.application.Application.launch(CryptogramGUIView.class, args);
        }
    }
}

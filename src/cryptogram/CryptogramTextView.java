/**
 * @author Sai Josyula
 */
package cryptogram;

import java.util.Scanner;

/**
 * Minimal console view for the Cryptogram game.
 * <p>
 * Demonstrates that the same {@link CryptogramController} and {@link CryptogramModel}
 * can drive both a text UI and a GUI (shared logic). Accepts simple commands:
 * <ul>
 *   <li>{@code A B} — map cipher A to plain B</li>
 *   <li>{@code H} — reveal a hint</li>
 *   <li>{@code Q} — quit the session</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public class CryptogramTextView implements java.util.Observer {

    /** Shared controller. */
    private final CryptogramController controller;

    /**
     * Constructs the text view and subscribes it to model changes.
     *
     * @param controller the shared controller
     * @param model      the model to observe
     */
    public CryptogramTextView(CryptogramController controller, CryptogramModel model) {
        this.controller = controller;
        model.addObserver(this);
    }

    /**
     * Runs a simple REPL loop for the text UI.
     * Prints the encrypted quote on updates and accepts commands until solved or quit.
     */
    public void run() {
        controller.newPuzzle();
        Scanner sc = new Scanner(System.in);
        System.out.println("Type: A B  (maps cipher A to plain B), H for hint, Q to quit");
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("Q")) break;
            if (line.equalsIgnoreCase("H")) {
                controller.hint();
            } else if (line.matches("[A-Za-z]\\s+[A-Za-z]")) {
                char c = Character.toUpperCase(line.charAt(0));
                char p = Character.toUpperCase(line.charAt(2));
                controller.guess(c, p);
            }
            if (controller.isSolved()) {
                System.out.println("Solved!\n" + controller.getModel().getOriginalQuote());
                break;
            }
        }
        sc.close();
    }

    /**
     * Observer callback. Prints the current encrypted quote on every model change.
     *
     * @param o   the observable model
     * @param arg {@code null} for new puzzle or the cipher letter that changed
     */
    @Override
    public void update(java.util.Observable o, Object arg) {
        CryptogramModel m = (CryptogramModel) o;
        System.out.println("Encrypted: " + m.getEncryptedQuote());
    }
}

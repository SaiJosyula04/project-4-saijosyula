/**
 * @author Sai Josyula
 */
package cryptogram;

import java.util.*;

/**
 * Controller for the Cryptogram game.
 * <p>
 * This class coordinates model operations and encapsulates puzzle setup, encryption,
 * hinting, and guess updates. It contains no UI code and is shared by both GUI and text UIs.
 */
public class CryptogramController {

    /** The model instance being controlled. */
    private final CryptogramModel model;

    /** Random source for quote/key selection. */
    private final Random rng = new Random();

    /** A small built-in list of quotes; you can expand this if desired. */
    private static final String[] QUOTES = {
        "The only limit to our realization of tomorrow is our doubts of today.",
        "Education is not the learning of facts, but the training of the mind to think.",
        "Life is really simple, but we insist on making it complicated.",
        "To be yourself in a world that is constantly trying to make you something else is the greatest accomplishment."
    };

    /**
     * Constructs a controller bound to the given model.
     *
     * @param model the model that stores the puzzle state
     */
    public CryptogramController(CryptogramModel model) {
        this.model = model;
    }

    /**
     * Creates a new puzzle by selecting a random quote and a monoalphabetic substitution key,
     * encrypting the quote, and installing the result in the model.
     */
    public void newPuzzle() {
        String quote = QUOTES[rng.nextInt(QUOTES.length)];
        Map<Character, Character> plainToCipher = buildRandomKey();

        // Invert to cipher -> true plain for model storage and hinting.
        Map<Character, Character> cipherToPlainTrue = new HashMap<>();
        for (Map.Entry<Character, Character> e : plainToCipher.entrySet()) {
            cipherToPlainTrue.put(e.getValue(), e.getKey());
        }

        String encrypted = encrypt(quote, plainToCipher);
        model.setNewPuzzle(quote, cipherToPlainTrue, encrypted);
    }

    /**
     * Submits a guess for a cipher letter, or clears it when plainOrNull is {@code null}.
     *
     * @param cipher      the cipher letter being guessed
     * @param plainOrNull the guessed plaintext letter or {@code null} to clear
     */
    public void guess(char cipher, Character plainOrNull) {
        model.setGuess(cipher, plainOrNull);
    }

    /**
     * Requests a single hint from the model.
     *
     * @return the cipher letter that was revealed, or {@code null} if none
     */
    public Character hint() {
        return model.revealOneHint();
    }

    /**
     * @return whether the entire puzzle is solved per the model
     */
    public boolean isSolved() {
        return model.isSolved();
    }

    /**
     * Exposes the model (useful to read immutable values in a view).
     *
     * @return the bound model instance
     */
    public CryptogramModel getModel() {
        return model;
    }

    // --------------------- Helpers (private) ---------------------

    /**
     * Builds a random monoalphabetic substitution key mapping plain->cipher with no fixed points.
     *
     * @return a map from plaintext letters (A..Z) to ciphertext letters (A..Z)
     */
    private Map<Character, Character> buildRandomKey() {
        List<Character> letters = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) letters.add(c);

        List<Character> shuffled = new ArrayList<>(letters);
        Collections.shuffle(shuffled, rng);

        // Avoid fixed points by swapping with the next position if needed.
        for (int i = 0; i < letters.size(); i++) {
            if (Objects.equals(letters.get(i), shuffled.get(i))) {
                int j = (i + 1) % letters.size();
                Collections.swap(shuffled, i, j);
            }
        }

        Map<Character, Character> key = new HashMap<>();
        for (int i = 0; i < 26; i++) key.put(letters.get(i), shuffled.get(i));
        return key;
    }

    /**
     * Encrypts a quote using the given plain->cipher mapping, preserving punctuation and case.
     *
     * @param quote the plaintext quote
     * @param key   mapping from plaintext letter to ciphertext letter
     * @return the encrypted quote string
     */
    private String encrypt(String quote, Map<Character, Character> key) {
        StringBuilder sb = new StringBuilder();
        for (char ch : quote.toCharArray()) {
            if (Character.isLetter(ch)) {
                boolean lower = Character.isLowerCase(ch);
                char enc = key.get(Character.toUpperCase(ch));
                sb.append(lower ? Character.toLowerCase(enc) : enc);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}

/**
 * @author Sai Josyula
 */
package cryptogram;

import java.util.*;

/**
 * Model for the Cryptogram game.
 * <p>
 * Holds the puzzle state: the original quote, its encrypted version, the true key
 * (cipher letter to true plaintext letter), and the user's current guesses.
 * Uses {@link java.util.Observable}/{@link java.util.Observer} (per course requirement)
 * to notify views when guesses change or a new puzzle is set.
 */
@SuppressWarnings("deprecation")
public class CryptogramModel extends Observable {

    /** The original plaintext quote. */
    private String originalQuote = "";

    /** The encrypted quote shown to the user. */
    private String encryptedQuote = "";

    /** User guesses: cipher letter (A..Z) -> guessed plain letter (A..Z). */
    private final Map<Character, Character> cipherToGuess = new HashMap<>();

    /** True key: cipher letter (A..Z) -> true plain letter (A..Z). */
    private Map<Character, Character> cipherToTruePlain = new HashMap<>();

    /**
     * Installs a fresh puzzle into the model and clears all guesses.
     * Notifies observers with {@code null} to indicate full rebuild.
     *
     * @param plain                the original plaintext quote
     * @param keyCipherToPlain     mapping from cipher letter to true plaintext letter
     * @param encrypted            the encrypted quote string
     */
    public void setNewPuzzle(String plain,
                             Map<Character, Character> keyCipherToPlain,
                             String encrypted) {
        this.originalQuote = plain;
        this.cipherToTruePlain = keyCipherToPlain;
        this.encryptedQuote = encrypted;
        this.cipherToGuess.clear();
        setChanged();
        notifyObservers(null);
    }

    /**
     * @return the original plaintext quote
     */
    public String getOriginalQuote() {
        return originalQuote;
    }

    /**
     * @return the encrypted quote shown to the user
     */
    public String getEncryptedQuote() {
        return encryptedQuote;
    }

    /**
     * Retrieves the user's current guess for a particular cipher letter.
     *
     * @param cipher the cipher letter (case-insensitive)
     * @return the guessed plaintext letter, or {@code null} if not guessed
     */
    public Character getGuess(char cipher) {
        return cipherToGuess.get(Character.toUpperCase(cipher));
    }

    /**
     * Sets or clears a guess for a cipher letter and notifies observers with that letter.
     * <p>
     * Observers should update only the text fields for this specific cipher letter.
     *
     * @param cipher       the cipher letter being guessed
     * @param plainOrNull  the guessed plain letter; {@code null} to clear the guess
     */
    public void setGuess(char cipher, Character plainOrNull) {
        char c = Character.toUpperCase(cipher);
        if (plainOrNull == null) cipherToGuess.remove(c);
        else cipherToGuess.put(c, Character.toUpperCase(plainOrNull));
        setChanged();
        notifyObservers(Character.valueOf(c));
    }

    /**
     * Reveals one correct mapping not currently satisfied (either missing or wrong),
     * updates the guess, and notifies observers with that cipher letter.
     *
     * @return the cipher letter that was revealed, or {@code null} if all were already correct
     */
    public Character revealOneHint() {
        for (Map.Entry<Character, Character> e : cipherToTruePlain.entrySet()) {
            char cipher = e.getKey();
            char truth = Character.toUpperCase(e.getValue());
            Character g = cipherToGuess.get(cipher);
            if (g == null || g != truth) {
                setGuess(cipher, truth);
                return cipher;
            }
        }
        return null;
    }

    /**
     * Returns whether the user's current guess for a cipher letter matches the true plaintext letter.
     *
     * @param cipher the cipher letter to evaluate
     * @return {@code true} if the current guess is exactly correct; {@code false} otherwise
     */
    public boolean isCorrectGuess(char cipher) {
        char c = Character.toUpperCase(cipher);
        Character g = cipherToGuess.get(c);
        if (g == null) return false;
        Character truth = cipherToTruePlain.get(c);
        return truth != null && Character.toUpperCase(truth) == g;
    }

    /**
     * Builds a frequency table of encrypted letters (A..Z) in the current encrypted quote.
     *
     * @return a sorted map Letter -> Count
     */
    public Map<Character, Integer> getEncryptedFrequencies() {
        Map<Character, Integer> freq = new TreeMap<>();
        for (char ch : encryptedQuote.toCharArray()) {
            if (Character.isLetter(ch)) {
                char up = Character.toUpperCase(ch);
                freq.put(up, freq.getOrDefault(up, 0) + 1);
            }
        }
        return freq;
    }

    /**
     * Determines whether the entire puzzle is solved: every position that is a letter
     * decrypts to the original letter, and punctuation/spaces match exactly.
     *
     * @return {@code true} if solved; {@code false} otherwise
     */
    public boolean isSolved() {
        if (originalQuote == null || encryptedQuote == null) return false;
        if (originalQuote.length() != encryptedQuote.length()) return false;
        for (int i = 0; i < encryptedQuote.length(); i++) {
            char enc = encryptedQuote.charAt(i);
            char orig = originalQuote.charAt(i);
            if (Character.isLetter(enc)) {
                Character g = cipherToGuess.get(Character.toUpperCase(enc));
                if (g == null || Character.toUpperCase(orig) != g) return false;
            } else if (enc != orig) {
                return false;
            }
        }
        return true;
    }
}

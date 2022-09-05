package gurdle;

import util.Observer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The model for the wordle game
 */
public class Model {

    /**
     * Possible game states
     */
    public enum GameState { ONGOING, WON, LOST, ILLEGAL_WORD }

    // ******** The Observable Section ********

    private final List< Observer< Model, String > > observers =
            new LinkedList<>();

    public void addObserver( Observer< Model, String > obs ) {
        this.observers.add( obs );
    }

    public void notifyObservers( String message ) {
        for ( Observer< Model, String > obs : this.observers ) {
            obs.update( this, message );
        }
    }

    // ******** The Guirdle Model ********

    /**
     * The required word length (normally 5)
     */
    public static final int WORD_SIZE = 5;

    /**
     * The number of attempts a player gets before they lose
     */
    public static final int NUM_TRIES = 6;

    /**
     * The source of the legal words
     */
    public static final String WORD_FILE_NAME = "data/morewords.txt";

    /**
     * What attempt no. is this (0-based), or
     * how many attempts have been completed already
     */
    private int attemptNum;

    /**
     * The next character to fill in the current attempt.
     * Probably only used by a GUI view.
     */
    private int charPos;

    /**
     * A list (really a multiset) of letters used in all the guesses.
     */
    List< Character > lettersUsed;

    /**
     * The word the player is trying to guess
     */
    private String secret;

    /**
     * The grid of guesses' letters.
     * (View uses this in conjunction with attempt number and position.)
     */
    private final CharChoice[][] guessLetters;

    /**
     * Words from which the next secret is chosen, and used to check for
     * illegal words entered by the player
     */
    private final List< String > legalWords;

    /**
     * Game's current state
     */
    private GameState gameState;

    private static final EnumMap< Model.GameState, String > STATE_MSGS =
            new EnumMap<>( Map.of(
                    Model.GameState.WON, "You won!",
                    Model.GameState.LOST, "You lost 😥.",
                    Model.GameState.ONGOING, "Make a guess!",
                    Model.GameState.ILLEGAL_WORD, "Illegal word."
            ) );

    /**
     * Used to randomly pick the next secret word.
     */
    private final Random rng;

    /**
     * Read in the list of words, intiailize the random number generator,
     * and allocate space to record the letters of the guesses.
     * A game is not ready to be played until {@link #newGame()} or
     * {@link #newGame(String)} is called.
     */
    public Model() {
        this.legalWords = new ArrayList<>( 2000 );
        try ( BufferedReader wFile = new BufferedReader(
                new FileReader( WORD_FILE_NAME )
        )
        ) {
            String word;
            while ( ( word = wFile.readLine() ) != null ) {
                this.legalWords.add( word );
            }
        }
        catch( IOException ioe ) {
            System.err.println( "Cannot read word file." );
            System.exit( 1 );
        }
        this.rng = new Random();
        this.guessLetters = new CharChoice[ NUM_TRIES ][ WORD_SIZE ];
        this.lettersUsed = new LinkedList<>();
    }

    /**
     * Start a new game: pick new word, clear out all previous attempts.
     */
    public void newGame() {
        this.secret =
                this.legalWords.get( rng.nextInt( this.legalWords.size() ) );
        newGameUtil();
    }

    /**
     * Start a new game: clear out all previous attempts.
     * This is mainly for testing.
     *
     * @param mandatedSecret the secret word to be used
     */
    public void newGame( String mandatedSecret ) {
        this.secret = mandatedSecret;
        newGameUtil();
    }

    /**
     * What must be done regardless of which newGame method was called
     */
    private void newGameUtil() {
        this.attemptNum = 0;
        this.charPos = 0;
        this.lettersUsed.clear();
        for ( int attemptNum = 0; attemptNum  < NUM_TRIES; ++attemptNum  ) {
            for ( int pos =0; pos  < WORD_SIZE; ++pos  ) {
                this.guessLetters[ attemptNum ][ pos ] = new CharChoice();
            }
        }
        this.gameState = GameState.ONGOING;
        this.notifyObservers( Model.STATE_MSGS.get( this.gameState ) );
    }

    // ******** character-by-character guesses

    /**
     * Controller tells model that one more character in a guess has been
     * provided by the player.
     *
     * @param guessChar the letter the player has chosen
     */
    public void enterNewGuessChar( char guessChar ) {
        // Ignore extra letters
        if ( this.gameState == GameState.ONGOING &&
             this.charPos < Model.WORD_SIZE ) {

            this.guessLetters[ attemptNum ][ this.charPos ]
                    .setChar( guessChar );
            this.lettersUsed.add( guessChar );
            this.notifyObservers( Model.STATE_MSGS.get( this.gameState ) );
            this.charPos += 1;
        }
    }

    /**
     * An improper guess word was entered. Clear out the current word,
     * reset the counters, and notify the observers to re-display.
     */
    private void illegalWordCleanup() {
        this.gameState = GameState.ILLEGAL_WORD;
        for ( int p =0; p  < Model.WORD_SIZE; ++p  ) {
            this.lettersUsed.remove(
                    Character.valueOf(
                            this.guessLetters[ this.attemptNum ][ p ].getChar()
                    )
            );
            this.guessLetters[ this.attemptNum ][ p ] = new CharChoice();
        }
        this.charPos = 0;
        this.notifyObservers( Model.STATE_MSGS.get( this.gameState ) );
        this.gameState = GameState.ONGOING;
    }

    /**
     * Controller tells model that the player has indicated they have entered
     * all the letters of a guess, and that the guess should be evaluated.
     */
    public void confirmGuess() {
        if ( this.gameState != GameState.WON &&
             this.gameState != GameState.LOST ) { // Ongoing game
            if ( this.charPos != Model.WORD_SIZE ) { // unfinished guess
                illegalWordCleanup();
            }
            else { // correct size guess
                final CharChoice[] attempt =
                        this.guessLetters[ this.attemptNum ];
                // Check if legal
                StringBuilder attemptSB = new StringBuilder();
                for ( CharChoice ch : attempt )
                    attemptSB.append( ch.getChar() );
                String attemptStr = String.valueOf( attemptSB );
                if ( this.legalWords.contains( attemptStr ) ) { // legal guess
                    // For each letter in the guess...
                    for ( int c =0; c  < WORD_SIZE; ++c  ) {
                        final CharChoice attemptCh = attempt[ c ];
                        // Level 1: See if the secret word contains the letter.
                        for ( int s =0; s  < WORD_SIZE; ++s  ) {
                            if ( attemptCh.getChar() ==
                                 this.secret.charAt( s ) ) {
                                attemptCh.setStatus(
                                        CharChoice.Status.WRONG_POS );
                            }
                        }
                        // Level 2: See if this letter is in the right spot.
                        if ( attemptCh.getChar() == this.secret.charAt( c ) ) {
                            attemptCh.setStatus( CharChoice.Status.RIGHT_POS );
                        }
                    }

                    // Level 3: See if all the letters match exactly.
                    boolean match = Arrays.stream(
                                        this.guessLetters[ this.attemptNum ] )
                                  .map( choice ->
                                                choice.getStatus() ==
                                                CharChoice.Status.RIGHT_POS
                                  )
                                  .reduce( true, ( x, y ) -> x && y );

                    if ( match ) {
                        this.gameState = GameState.WON;
                    }
                    else if ( this.attemptNum == Model.NUM_TRIES - 1 ) {
                        // This was the last guess.
                        this.gameState = GameState.LOST;
                    }
                    else {
                        // Legal guess, but not done with game.
                        this.gameState = GameState.ONGOING;
                    }

                    this.charPos = 0;
                    this.attemptNum += 1;
                    this.notifyObservers(
                            Model.STATE_MSGS.get( this.gameState )
                    );
                }
                else { // illegal word entered by user
                    this.illegalWordCleanup();
                }

            }
        }
    }

    // ******** full-string-at-once guesses ********

    /**
     * The player has, through the UI, entered a complete guess all at once.
     *
     * @param guess the full guess
     */
    public void enterNewGuess( String guess ) {
        /*
         * TODO
         * Check the game's state (this.gameState).
         * If the game state is ongoing (not won/lost),
         *   if the length of the guess is not the expected word size,
         *     Set the game's state to ILLEGAL_WORD.
         *     Notify the observers of this object.
         *     Set the game state back to ONGOING.:
         *   else,
         *     Using this.attemptNum as the row number, make a loop to copy
         *       each character of the guess into the CharChoice instance at
         *       the appropriate spot in the guess matrix this.guessLetters.
         *       (Do not mess with the CharChoice status here.)
         *       Add each character to the List this.lettersUsed
         *           (useful to the GUI).
         *     this.charPos = guess.length(); // => a word has been entered.
         *     this.confirmGuess(); // <-- checks the guess
         */
        if (this.gameState == GameState.ONGOING){
            if (guess.length() != 5){
                this.gameState = GameState.ILLEGAL_WORD;
                notifyObservers("Illegal guess.");
                this.gameState = GameState.ONGOING;
            } else {
                for (int i = 0; i < 5; i++){
                    guessLetters[this.attemptNum][i].setChar(guess.charAt(i));
                    this.lettersUsed.add(this.guessLetters[this.attemptNum][i].getChar());
                }
                this.charPos = guess.length();
                this.confirmGuess();
            }
        }
    }

    // ******** Queries, for View ********

    /**
     * How's the game going?
     * (May not be needed since the game state is sent as client data.)
     *
     * @return the current state
     */
    public GameState gameState() {
        return this.gameState;
    }

    /**
     * Find out what was typed at a specific point in this game.
     *
     * @param guessNum which guess ("row")
     * @param pos      which letter position ("column")
     * @return the letter at the specified position
     */
    public CharChoice get( int guessNum, int pos ) {
        return this.guessLetters[ guessNum ][ pos ];
    }

    /**
     * Has this letter been used on any legal guess in this game?
     * @return true only if character is involved in an enter-new-guess
     * method that was not later cleared because it was in an illegal word
     */
    public boolean usedLetter( char ch ) {
        return this.lettersUsed.contains( ch );
    }

    /**
     * What's the secret word?
     *
     * @return the word chosen for this game
     */
    public String secret() {
        return this.secret;
    }

    /**
     * How many valid guesses/attempts has the player made in this game?
     *
     * @return the 1-based number of the last valid attempt or 0 if none
     */
    public int numAttempts() {
        return attemptNum;
    }

}

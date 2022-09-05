package gurdle.ptui;

import gurdle.CharChoice;
import gurdle.Model;
import util.Observer;
import util.ptui.ConsoleApplication;

import java.io.PrintWriter;
import java.util.*;

/**
 * A Plain-Text user interface for the RIT CS Wordle program
 *
 * @see ConsoleApplication
 * @author RIT CS
 */
public class Turdle extends ConsoleApplication
        implements Observer< Model, String > {

    /** View/Controller access to model */
    private Model model;

    /**
     * Used to prevent this class displaying any info before the UI
     * has been completely set up.
     * Scenario:
     * <ol>
     *     <li>This class creates the model and registers with it.</li>
     *     <li>Model initializes itself and updates its observers.</li>
     *     <li>
     *         This class attempts to display information, but
     *         {@link #start(PrintWriter)} has not yet been called,
     *         therefore the output stream has not yet been established.
     *     </li>
     *     <li>Pandemonium ensues.</li>
     * </ol>
     */
    private boolean initialized;

    /** Where this class's messages must be sent */
    private PrintWriter out;

    /**
     * Create the Wordle model and register this object as an observer
     * of it. If there was a command line argument, use that as the first
     * secret word.
     */
    @Override public void init() throws Exception {
        this.initialized = false;
        this.model = new Model();
        this.model.addObserver( this );

        List< String > paramStrings = super.getArguments();
        if ( paramStrings.size() == 1 ) {
            final String firstWord = paramStrings.get( 0 );
            if ( firstWord.length() == Model.WORD_SIZE ) {
                this.model.newGame( firstWord );
            }
            else {
                throw new Exception(
                        String.format(
                                "\"%s\" is not the required word length (%d)." +
                                System.lineSeparator(),
                                firstWord, Model.WORD_SIZE
                        )
                );
            }
        }
        else {
            this.model.newGame();
        }
    }

    /** Markers to indicate whether letters are in the word or not */
    private static final EnumMap< CharChoice.Status, Character > CHAR_FILL =
            new EnumMap<>( Map.of(
                    CharChoice.Status.RIGHT_POS, '^',
                    CharChoice.Status.WRONG_POS, '*',
                    CharChoice.Status.WRONG, ' ',
                    CharChoice.Status.EMPTY, ' '
            ) );

    /**
     * Tell the model to start a new game.
     */
    private void newGame() {
        this.model.newGame();
    }

    /**
     * Set up the PTUI.
     * Here, the handlers for "guess", "cheat", and "new" are created.
     * @param out the output stream to use from now on
     */
    public void start( PrintWriter out ) {
        this.out = out;
        this.initialized = true;
        super.setOnCommand( "guess", 1, "<word>: Make a guess",
                            args -> this.model.enterNewGuess( args[ 0 ] )
        );
        super.setOnCommand( "cheat", 0, ": Show the secret word",
                            args -> this.cheat()
        );
        super.setOnCommand("new", 0, ": Start a new game", args -> this.newGame());
    }


    /**
     * The model -- the subject -- has some changes.
     * Query the model to find out what's going on and display the current
     * state of all the legitimate guesses.
     * Print the provided message.
     * If the user lost, display the secret word.
     * @param model the observed subject of this observer
     * @param message the message the model wants conveyed to the user
     */
    @Override
    public void update( Model model, String message ) {
        if ( !this.initialized ) return; // Too soon; no PTUI set up yet.

        for ( int guessNum = 0; guessNum < model.numAttempts(); ++guessNum ) {
            for ( int charPos = 0; charPos < Model.WORD_SIZE; ++charPos ) {
                CharChoice cc = model.get( guessNum, charPos );
                final char ch = cc.getChar();
                this.out.print( ch );
            }
            this.out.println();
            for ( int charPos = 0; charPos < Model.WORD_SIZE; ++charPos ) {
                CharChoice cc = model.get( guessNum, charPos );
                final CharChoice.Status ccStatus = cc.getStatus();
                this.out.print( Turdle.CHAR_FILL.get( ccStatus ) );
            }
            this.out.println();
        }
        final Model.GameState gamestate = model.gameState();
        if ( gamestate != Model.GameState.ONGOING ) {
            this.out.print( message );
        }
        if ( gamestate == Model.GameState.LOST ) {
            this.out.print( " The secret word was " );
            cheat();
        }
        else {
            this.out.println();
        }
    }

    /**
     * Display the answer. (for testing purposes)
     */
    private void cheat() {
        this.out.println( this.model.secret() );
    }

    /**
     * Start up the console application.
     * @param args a single, optional word, to use as the first secret word
     */
    public static void main( String[] args ) {
        if ( args.length > 1 ) {
            System.err.println( "Usage: java Gurdle [1st-secret-word]" );
        }
        else {
            ConsoleApplication.launch( Turdle.class, args );
        }
    }
}

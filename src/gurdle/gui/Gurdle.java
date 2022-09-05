package gurdle.gui;

import gurdle.CharChoice;
import gurdle.Model;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import util.Observer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;

/**
 * The graphical user interface to the Wordle game model in
 * {@link Model}.
 *
 * @author Jake Edelstein
 */
public class Gurdle extends Application
        implements Observer< Model, String > {
    private Model model;
    private Scene scene;
    private boolean initialized;
    // adding strings to loop through to create the keyboard
    private String qwertyuiop = "QWERTYUIOP";
    private String asdfghjkl = "ASDFGHJKL";
    private String zxcvbnm = "ZXCVBNM";
    private int charPos = 0;
    // flags to update text at the top of the screen
    private boolean cheating = false;
    private boolean previousWordIllegal;
    // set height and width of window
    private final static int HEIGHT = 800;
    private final static int WIDTH = 500;
    // initialize layout components
    private Text topText;
    private BorderPane bpTop;
    private BorderPane bpBottom;
    private GridPane charGrid;
    // store elements in 2d arrays so they can be manipulated
    private Label[][] charGridArray = new Label[5][6];
    private CharChoice[][] charChoices = new CharChoice[5][6];
    private Button[][] keyboardArray = new Button[10][3];


    @Override public void init() throws Exception{
        // print message to console and initialize model
        System.out.println( "Starting Gurdle...");
        this.initialized = false;
        this.model = new Model();
        this.model.addObserver(this);
        // check for command line argument
        List< String > paramStrings = super.getParameters().getRaw();
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

    @Override
    public void start( Stage mainStage ) {
        // build GUI elements
        mainStage.setWidth(WIDTH);
        mainStage.setHeight(HEIGHT);
        mainStage.setTitle("Gurdle");
        mainStage.setResizable(false);
        this.bpTop = new BorderPane();
        this.scene =
                new Scene(bpTop);
        this.topText = new Text();
        topText.setText(model.numAttempts() + " guesses used, Make a guess!");
        topText.setStyle("-fx-font: 20px Menlo");
        bpTop.setTop(topText);
        BorderPane.setAlignment(topText, Pos.CENTER);
        this.charGrid = makeCharGrid();
        bpTop.setCenter(charGrid);
        this.bpBottom = new BorderPane();
        bpTop.setBottom(bpBottom);
        bpBottom.setLeft(makeKeyboard());
        bpBottom.setRight(makeUtilButtons());
        for (int c = 0; c < Model.WORD_SIZE; c++){
            for (int r = 0; r < Model.NUM_TRIES; r++){
                charChoices[c][r] = new CharChoice();
            }
        }
        mainStage.setScene( scene );
        mainStage.show();
        this.initialized = true;
    }

    /**
     * Create a box for a guessed letter to go in
     * @return a white box with a thin black border
     */
    public Label makeCharBox(String letter){
        Label charBox = new Label(letter);
        // set dimensions of charBox and make a box
        charBox.setPrefWidth(60);
        charBox.setPrefHeight(87);
        charBox.setStyle( """
                -fx-font: 48px Menlo;
                -fx-padding: 2;
                -fx-border-style: solid inside;
                -fx-border-width: 2;
                -fx-border-radius: 2;
                -fx-border-color: black;
                -fx-base: white;
                """);
        charBox.setAlignment(Pos.CENTER);
        return charBox;
    }

    /**
     * Create a 6x5 grid of boxes for the game
     * @return a grid of boxes
     */
    public GridPane makeCharGrid(){
        GridPane charGrid = new GridPane();
        // gaps between boxes
        charGrid.setHgap(15);
        charGrid.setVgap(10);
        int row;
        int col;
        // add charBoxes to a 2d array
        for (col = 0; col < 5; col++){
            for (row = 0; row < 6; row++){
                Label l = makeCharBox("");
                charGrid.add(l, col, row);
                charGridArray[col][row] = l;
            }
        }
        charGrid.setAlignment(Pos.CENTER);
        return charGrid;
    }

    /**
     * Make a key for the virtual keyboard
     * @return a button representing a key
     */
    public Button makeKeyboardKey(String letter){
        // make button for a key and set button style
        Button b = new Button(letter);
        b.setPrefSize(32, 32);
        b.setStyle("""
                -fx-font: 11px Menlo;
                -fx-border-style: solid inside;
                -fx-border-width: 2;
                -fx-border-radius: 2;
                -fx-border-color: black;
                """);
        // add an ID to a button so it can be looked up by letter
        b.setId(letter);
        // do the following when the button is pressed
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                previousWordIllegal = false;
                if (model.gameState() != Model.GameState.LOST && model.gameState() != Model.GameState.WON) {
                    // add the letter pressed to guessLetters in model
                    model.enterNewGuessChar(letter.charAt(0));
                    // only do the following if the row is not full
                    if (charPos < Model.WORD_SIZE){
                        // change layout based on letter pressed
                        charGridArray[charPos][model.numAttempts()].setText(letter);
                        charChoices[charPos][model.numAttempts()].setChar(letter.charAt(0));
                        // move to next box over for next character
                        charPos++;
                    }
                }
            }
        });
        // return the button
        return b;
    }

    /**
     * Create the layout for a QWERTY keyboard
     * @return a GridPane of buttons representing a keyboard
     */
    public GridPane makeKeyboard(){
        // make lists of strings for each row of the keyboard
        String[] row0 = qwertyuiop.split("");
        String[] row1 = asdfghjkl.split("");
        String[] row2 = zxcvbnm.split("");
        // make a GridPane to store the keys
        GridPane keyboard = new GridPane();
        // make small gaps in between keys
        keyboard.setVgap(2);
        keyboard.setHgap(2);
        int col;
        int row;
        // build the keyboard
        for (row = 0; row < 3; row++){
            for (col = 0; col < 10; col++){
                if (row == 0){
                    Button newKey = makeKeyboardKey(row0[col]);
                    keyboard.add(newKey, col, row);
                    keyboardArray[col][row] = newKey;
                }
                else if (row == 1 && col < 9){
                    Button newKey = makeKeyboardKey(row1[col]);
                    keyboard.add(newKey, col, row);
                    keyboardArray[col][row] = newKey;
                }
                else if (row == 2 && col < 7){
                    Button newKey = makeKeyboardKey(row2[col]);
                    keyboard.add(newKey, col, row);
                    keyboardArray[col][row] = newKey;
                }
            }
        }
        keyboard.setAlignment(Pos.BOTTOM_LEFT);
        keyboard.setStyle("""
                -fx-border-style: solid inside;
                -fx-border-width: 4;
                -fx-border-radius: 4;
                -fx-border-color: transparent;
                """);
        return keyboard;
    }

    /**
     * Makes a GridPane for the restart, cheat, and enter guess buttons
     * @return a GridPane of buttons
     */
    public GridPane makeUtilButtons(){
        // create the buttons
        GridPane utilButtons = new GridPane();
        Button enter = new Button("ENTER");
        Button cheat = new Button("CHEAT");
        Button newGame = new Button("NEW GAME");
        enter.setPrefSize(105, 33);
        enter.setStyle("-fx-font: 15px Menlo");
        // event for enter button
        enter.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // move cursor back to leftmost box in row
                charPos = 0;
                // check guess validity and correctness
                model.confirmGuess();
                // update view accordingly
                update(model, "Guess submitted");
            }
        });
        cheat.setPrefSize(105, 33);
        cheat.setStyle("-fx-font: 15px Menlo");
        // event for cheat button
        cheat.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // set cheating flag to true and now display secret word at top of window
                cheating = true;
                previousWordIllegal = false;
                update(model, "Now cheating");
            }
        });
        newGame.setPrefSize(105, 33);
        newGame.setStyle("-fx-font: 15px Menlo");
        // event for new game button
        newGame.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String[] aplhabet = "QWERTYUIOPASDFGHJKLZXCVBNM".split("");
                // reset flags
                cheating = false;
                previousWordIllegal = false;
                model.newGame();
                // reset colors and contents of all charBoxes
                for (int c = 0; c < Model.WORD_SIZE; c++){
                    for (int r = 0; r < Model.NUM_TRIES; r++){
                        charChoices[c][r] = new CharChoice();
                        charGridArray[c][r].setText("");
                    }
                }
                // make all keys white
                for (String letter : aplhabet){
                    scene.lookup("#" + letter).setStyle("""
                            -fx-font: 11px Menlo;
                            -fx-border-style: solid inside;
                            -fx-border-width: 2;
                            -fx-border-radius: 2;
                            -fx-border-color: black;
                            -fx-base: white;
                            """);
                }
            }
        });
        // add buttons to scene
        utilButtons.add(enter, 0, 0);
        utilButtons.add(cheat, 0, 1);
        utilButtons.add(newGame, 0, 2);
        utilButtons.setAlignment(Pos.BOTTOM_RIGHT);
        utilButtons.setStyle("""
                -fx-border-style: solid inside;
                -fx-border-width: 4;
                -fx-border-radius: 4;
                -fx-border-color: transparent;
                """);
        return utilButtons;
    }

    /**
     * Refresh the colors of labels and keys based on the CharChoice state
     */
    public void refreshLabelsAndKeys(){
        // loop through every charBox on the board
        for (int c = 0; c < Model.WORD_SIZE; c++){
            for (int r = 0; r < Model.NUM_TRIES; r++){
                charChoices[c][r] = model.get(r, c);
                // make the box and keyboard key green if correct
                if (charChoices[c][r].getStatus() == CharChoice.Status.RIGHT_POS){
                    charGridArray[c][r].setBackground
                            (new Background( new BackgroundFill
                                    (Color.LIGHTGREEN, new CornerRadii(2), Insets.EMPTY)));
                    scene.lookup("#" + charChoices[c][r].getChar()).setStyle("""
                            -fx-font: 11px Menlo;
                            -fx-border-style: solid inside;
                            -fx-border-width: 2;
                            -fx-border-radius: 2;
                            -fx-border-color: black;
                            -fx-base: lightgreen;
                            """);
                }
                // make the box and keyboard key gold if wrong spot
                else if (charChoices[c][r].getStatus() == CharChoice.Status.WRONG_POS){
                    charGridArray[c][r].setBackground
                            (new Background( new BackgroundFill
                                    (Color.GOLD, new CornerRadii(2), Insets.EMPTY)));
                    // do not make keyboard key gold if it is already green
                    if (!scene.lookup("#" + charChoices[c][r].getChar()).getStyle()
                            .contains("-fx-base: lightgreen")){
                        scene.lookup("#" + charChoices[c][r].getChar()).setStyle("""
                                -fx-font: 11px Menlo;
                                -fx-border-style: solid inside;
                                -fx-border-width: 2;
                                -fx-border-radius: 2;
                                -fx-border-color: black;
                                -fx-base: gold;
                                """);
                    }
                }
                // make the box and keyboard key gray if wrong
                // model is bugged so this doesn't work
                else if (charChoices[c][r].getStatus() == CharChoice.Status.WRONG){
                    charGridArray[c][r].setBackground
                            (new Background( new BackgroundFill
                                    (Color.DARKGRAY, new CornerRadii(2), Insets.EMPTY)));
                    // do not make keyboard key gray if it is already green or gold
                    if (!scene.lookup("#" + charChoices[c][r].getChar()).getStyle()
                            .contains("-fx-base: lightgreen") &&
                            (!scene.lookup("#" + charChoices[c][r].getChar()).getStyle()
                            .contains("-fx-base: gold"))){
                        scene.lookup("#" + charChoices[c][r].getChar()).setStyle("""
                                -fx-font: 11px Menlo;
                                -fx-border-style: solid inside;
                                -fx-border-width: 2;
                                -fx-border-radius: 2;
                                -fx-border-color: black;
                                -fx-base: darkgrey;
                                """);
                    }
                }
                // make the keyboard key white if the letter is not used
                else {
                    charGridArray[c][r].setBackground(Background.EMPTY);
                }
            }
        }
    }

    @Override
    public void update( Model model, String message ) {
        // do not update if scene has not been initialized yet
        if (!this.initialized){
            return;
        }
        refreshLabelsAndKeys();
        // update text at top
        if (model.gameState() == Model.GameState.ONGOING){
            if (cheating){
                topText.setText(model.numAttempts() + " guesses used, Make a guess!" + "\t SECRET: " + model.secret());
                if (previousWordIllegal){
                    topText.setText("Illegal word, try again");
                }
            }
            else{
                topText.setText(model.numAttempts() + " guesses used, Make a guess!");
                if (previousWordIllegal){
                    topText.setText("Illegal word, try again");
                }
            }
        }
        // clear the current row if the word is illegal
        if (model.gameState() == Model.GameState.ILLEGAL_WORD){
            previousWordIllegal = true;
            for (int c = 0; c < Model.WORD_SIZE; c++){
                charChoices[c][model.numAttempts()] = new CharChoice();
                charGridArray[c][model.numAttempts()].setText("");
            }
        }
        // game win text
        if (model.gameState() == Model.GameState.WON){
            topText.setText("Congratulations, you won!");
        }
        // game lose text
        if (model.gameState() == Model.GameState.LOST){
            topText.setText("You lost :(  The secret word was " + model.secret());
        }
    }

    public static void main( String[] args ) {
        if ( args.length > 1 ) {
            System.err.println( "Usage: java Gurdle [1st-secret-word]" );
        }
        Application.launch( args );
    }
}
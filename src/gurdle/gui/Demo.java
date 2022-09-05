package gurdle.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Show how to make a grid of colorful labels
 * @author RIT CS
 */
public class Demo extends Application {

    public static final int HEIGHT = 2;
    public static final int WIDTH = 3;

    private static final Background[] colors = {
        new Background( new BackgroundFill( Color.LIGHTGREEN, null, null ) ),
        new Background( new BackgroundFill( Color.BURLYWOOD, null, null ) ),
        new Background( new BackgroundFill( Color.LIGHTGREY, null, null ) ),
        new Background( new BackgroundFill( Color.WHITE, null, null ) )
    };

    private static final int COLOR_COUNT = colors.length;

    private Label[][] labels;

    private int row;
    private int col;
    private int colorNum;

    @Override
    public void init() {
        this.row = -1;
        this.col = WIDTH - 1;
        this.colorNum = COLOR_COUNT - 1;
        this.labels = new Label[ Demo.HEIGHT ][ Demo.WIDTH ];
    }

    @Override
    public void start( Stage mainStage ) {
        BorderPane pane = new BorderPane();
        pane.setCenter( this.makeGrid() );
        Button cycle = new Button( "CYCLE" );
        cycle.setOnAction( e -> this.colorCycle() );
        pane.setBottom( cycle );
        Scene scene = new Scene( pane );
        mainStage.setScene( scene );
        mainStage.show();
    }

    private GridPane makeGrid() {
        GridPane result = new GridPane();
        char ch = 'A';
        for ( int r = 0; r < Demo.HEIGHT; ++r ) {
            for ( int c = 0; c < Demo.WIDTH; ++c ) {
                this.labels[r][c] = new Label( String.valueOf( ch ) );
                result.add( this.labels[r][c], c, r );
                ch = (char)( ch + 1 );
            }
        }
        result.setStyle( "-fx-font: 32px Menlo" );
        result.setAlignment( Pos.CENTER );
        return result;
    }

    private void colorCycle() {
        this.col += 1;
        if ( this.col == Demo.WIDTH ) {
            this.col = 0;
            this.row = ( this.row + 1 ) % Demo.HEIGHT;
        }
        this.colorNum = ( this.colorNum + 1 ) % Demo.COLOR_COUNT;

        this.labels[ this.row ][ this.col ].setBackground(
                Demo.colors[ this.colorNum ]
        );
    }

    public static void main( String[] args ) {
        Application.launch( args );
    }
}

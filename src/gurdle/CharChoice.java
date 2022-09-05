package gurdle;

public class CharChoice {

    public enum Status { WRONG, WRONG_POS, RIGHT_POS, EMPTY }

    private Status status;

    private char ch;

    public CharChoice() {
        this.status = Status.EMPTY;
        this.ch =' ';
    }

    @Override
    public String toString() {
        return String.valueOf( this.ch );
    }

    public Status getStatus() { return this.status; }

    public char getChar() { return this.ch; }

    public void setChar( char ch ) {
        this.ch = ch;
    }

    public void setStatus( Status status ) {
        this.status = status;
    }
}

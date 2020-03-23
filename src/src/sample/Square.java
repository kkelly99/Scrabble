package sample;
public class Square {

    private int letterMuliplier;
    private int wordMultiplier;
    private boolean isOccupied;
    private Tile tile;

    Square(int letterMultiplier, int wordMultiplier) {
        isOccupied = false;
        this.letterMuliplier = letterMultiplier;
        this.wordMultiplier = wordMultiplier;
    }

    public int getLetterMuliplier() {
        return letterMuliplier;
    }

    public int getWordMultiplier() {
        return wordMultiplier;
    }

    public boolean isDoubleLetter() {
        return letterMuliplier == 2;
    }

    public boolean isTripleLetter() {
        return letterMuliplier == 3;
    }

    public boolean isDoubleWord() {
        return wordMultiplier == 2;
    }

    public boolean isTripleWord() {
        return wordMultiplier == 3;
    }

    public void add(Tile tile) {
        isOccupied = true;
        this.tile = tile;
    }
    public void remove() {
        isOccupied = false;
        this.tile = null;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    // getTile pre-condition: isOccupied must be true
    public Tile getTile() {
        return tile;
    }

}

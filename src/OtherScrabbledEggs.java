import java.util.ArrayList;
import java.util.HashSet;

public class OtherScrabbledEggs implements BotAPI {

    //OTHER-SCRABBLED-EGGS Bot
    //Made by Luke Connelly, Karen Kelly and Sean Jevens

    // The public API of Bot must not change
    // This is ONLY class that you can edit in the program
    // Rename Bot to the name of your team. Use camel case.
    // Bot may not alter the state of the game objects
    // It may only inspect the state of the board and the player objects

    private PlayerAPI me;
    private OpponentAPI opponent;
    private BoardAPI board;
    private UserInterfaceAPI info;
    private DictionaryAPI dictionary;
    private int turnCount = 0;
    private int pool = 100;         //variable used to store the pool size
    private boolean hasPool = false;    //used to query game for pool size
    private String AllInfo="";      //used to store everything we know about the game state as getLatestInfo() wasnt working

    OtherScrabbledEggs(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary) {
        this.me = me;
        this.opponent = opponent;
        this.board = board;
        this.info = ui;
        this.dictionary = dictionary;
    }

    public String getCommand() {
        // Add your code here to input your commands
        String command = "";
        if (turnCount==0) {
            command = "NAME Noreen";    //my gran's name, she loves scrabble
        }
        else if (board.isFirstPlay()) {
            command = makeFirstWord(me.getFrameAsString());
        }
        else if(!hasPool)       //ie. opponent has just moved
        {
            if(shouldChallenge())       //returns a boolean true if a word the opponent placed was incorrect
            {
                command="CHALLENGE";
            }
            else {
                command = "POOL";
                hasPool = true;     // we know now the pool size so we should pass this loop
            }
        }
        else {
            pool=getPool();     //update pool size
            command = makeWord(me.getFrameAsString());
            hasPool=false;  //we no longer know the pool size, will have to update next time the opponent moves
        }
        turnCount++;
        return command;
    }

    public boolean shouldChallenge() {
        String currInfo = info.getAllInfo().substring(AllInfo.length());    //shortening to just the newest info
        String[] InfoArray = currInfo.split("\n");  //splitting by line
        int i=InfoArray.length-1;   //start at the latest info and work back to find commands
        while(true) {
            if(InfoArray[i].contains(">"))
            {
                if(!InfoArray[i].contains("NAME"))      //to stop us breaking at the first word after we set our name
                {
                    break;
                }
            }
            i--;
            if (i == -1) {
                return false; //some error - dont challenge
            }
        }
        if(InfoArray[i].toUpperCase().equals("> CHALLENGE") || InfoArray[i].toUpperCase().equals("> PASS") || InfoArray[i].toUpperCase().equals("> P") || InfoArray[i].toUpperCase().matches("[>][ ]EXCHANGE( )+([A-Z_]){1,7}") || InfoArray[i].toUpperCase().matches("[>][ ]X( )+([A-Z_]){1,7}"))
        {
            return false; // not a move
        }
        String[] parts = InfoArray[i].toUpperCase().substring(1).trim().split("( )+");  //split the move into its parts
        String gridText = parts[0];
        int column = ((int) gridText.charAt(0)) - ((int) 'A');
        String rowText = parts[0].substring(1);
        int row = Integer.parseInt(rowText)-1;
        String directionText = parts[1];
        boolean isHorizontal = directionText.equals("A");
        String letters = parts[2];
        Word word;  //now create the word using the parsed parts
        if (parts.length == 3) {
            word = new Word(row, column, isHorizontal, letters);
        } else {
            String designatedBlanks = parts[3];
            word = new Word(row, column, isHorizontal, letters, designatedBlanks);
        }
        ArrayList<Word> words = new ArrayList<>();
        for(Word w : getAllWordsChallenge(word))        //uses a slightly different method as tiles are on the board
        {
            words.add(w);
        }
        AllInfo=info.getAllInfo();      //update our info
        return !dictionary.areWords(words);
    }

    public int getPool()
    {
        String currInfo = info.getAllInfo().substring(AllInfo.length());
        String[] InfoArray = currInfo.split("\n");
        int i=InfoArray.length-1;
        while(!InfoArray[i].contains("Pool has ")) {        //work backwards to find line with pool size
            i--;
            if (i == -1) {
                return 100; //some error - assume happens at start
            }
        }
        AllInfo=info.getAllInfo();  //update info
        return Integer.parseInt(InfoArray[i].replaceAll("\\D+",""));    //return parsed size
    }

    public String exchange(String frame){
        if(board.isFirstPlay())
        {
            return "X "+frame.replaceAll("[ZXQJ_S]","");    //keep any valuable tiles
        }
        if (pool<7)     //not enough tiles to exchange
        {
            return "pass";
        } if (pool<15) {        //TODO test with greater than or less than 15
            return "X "+frame.replaceAll("[^ZXQJVWGKBFHCD]", "");//start to get rid of high value tiles
        }if (pool<30) {        //TODO test with greater than or less than 15
            return "X "+frame.replaceAll("[^JVWGKBFHCD]", "");//start to get rid of difficult to use tiles
        }
        return "X "+frame.replaceAll("[ZXQJ_S]","");//keep best tiles
    }

    public String makeFirstWord(String myFrame)
    {
        myFrame = myFrame.replaceAll("[^A-Z_]", "");//turning frame into string of 7 letters
        String command = exchange(myFrame);//preparing an exchange command if we find no words
        Word bestWord = new Word(0,0,false,"neverever");//is never used, just a placeholder
        int maxScore=0;
        String blanks="";
        ArrayList<String> combinations = getCombinations(myFrame);    //find every combination of the letters
        ArrayList<Word> found = new ArrayList<Word>();
        for (int i=1; i<8; i++) {
            for (String combination : combinations) {
                if(combination.contains("_"))
                {
                    ArrayList<String> combinationsWithoutBlanks = new ArrayList<String>();
                    addStringsWithoutBlanks(combination, combinationsWithoutBlanks);
                    for (String combinationWithoutBlanks : combinationsWithoutBlanks) {
                        Word word = new Word(7,i,true,combinationWithoutBlanks);
                        found.add(word);
                        if (dictionary.areWords(found))
                        {
                            Word wordWithBlanks = new Word(7,i,true,combination);
                            if((getFirstWordPoints(wordWithBlanks)>maxScore||(getFirstWordPoints(wordWithBlanks)==maxScore&&wordWithBlanks.length()<bestWord.length()))&&i+wordWithBlanks.length()>6)
//if word is a word, beats current best score, reaches double word we have a new best word
                            {
                                if(getFirstWordPoints(word)>35) {
                                    bestWord = wordWithBlanks;
                                    maxScore = getFirstWordPoints(word);
                                    blanks = " ";
                                    for (int o = 0; o < combination.length(); o++) {
                                        if (combination.charAt(o) == '_') {
                                            blanks += combinationWithoutBlanks.charAt(o);
                                        }
                                    }
                                }
                            }
                        }
                        found.remove(word);
                    }
                }
                else{
                    Word word = new Word(7,i,true,combination);
                    found.add(word);
                    if (dictionary.areWords(found))
                    {
                        if((getFirstWordPoints(word)>maxScore||(getFirstWordPoints(word)==maxScore&&word.length()<bestWord.length()))&&i+word.length()>6)
//if word is a word, beats current best score, reaches double word  we have a new best word
                        {
                            if(word.toString().length() - word.toString().replaceAll("[ZQXJ]","").length() >0)
                            {
                                if(getFirstWordPoints(word)>30)
                                {
                                    bestWord = word;
                                    maxScore = getFirstWordPoints(word);
                                    blanks = "";
                                }
                            }
                            else {
                                bestWord = word;
                                maxScore = getFirstWordPoints(word);
                                blanks = "";
                            }
                        }
                    }
                    found.remove(word);
                }
            }
        }
        if(maxScore != 0)
        {
            command = Character.toString((char) (bestWord.getFirstColumn()+'A')) + Integer.toString(bestWord.getFirstRow()+1);
            command += bestWord.isHorizontal() ? " A ":" D ";
            command += bestWord.toString(); //creates command for the best word
            command += blanks;
        }
        return command;
    }

    public String makeWord(String myFrame)
    {
        myFrame = myFrame.replaceAll("[^A-Z_]", "");//turning frame into string of 7 letters
        String command = exchange(myFrame);//preparing a pass command if we find no words
        Word bestWord = new Word(0,0,false,"neverever");//is never used, just a placeholder
        int maxScore=0;
        String blanks="";
        ArrayList<String> combinations = getCombinations(myFrame);    //find every combination of the letters
        HashSet<IntPair> hooks = new HashSet<IntPair> ();
        for (int r=0; r<15; r++)  {
            for (int c=0; c<15; c++)   {
                if (isHook(r, c))
                {
                    hooks.add(new IntPair(r,c));       //square where we can place to
                }
            }
        }
        HashSet<GADDAG> gaddags = new HashSet<GADDAG>();
        for(IntPair i : hooks)
        {
            generateGaddags(i.row, i.col, gaddags); //finds shapes for words where ? represents empty squares
        }
        Frame frame = new Frame();
        ArrayList<Tile> alt = new ArrayList<>();
        for(int i=0; i<me.getFrameAsString().replaceAll("[^A-Z_]", "").length();i++)
        {
            alt.add(new Tile(me.getFrameAsString().replaceAll("[^A-Z_]", "").charAt(i)));
        }
        frame.addTiles(alt);        //so we can use is legal
        for(GADDAG g : gaddags)     //for each move shape
        {
            int sr=g.start.row, sc=g.start.col;//finding the start of the word using the GADDAG
            if(g.isHorizontal)
            {
                sc-=g.prefix.length();
            }
            else {
                sr-=g.prefix.length();
            }
            HashSet<String> GADDAGcombos = new HashSet<>();
            getGADDAGFrameCombinations(g, combinations,GADDAGcombos);
            for(String s : GADDAGcombos)
            {
                if(s.contains("_"))
                {
                    for(int i=0; i<26; i++)
                    {
                        if(s.length()-s.replaceAll("_","").length()==2) {//process for two blanks (very unlikely)
                            for (int j=0; j<26;j++) {
                                Word temp = new Word(sr, sc, g.isHorizontal, s, Character.toString((char) ((char) i+'A'))+Character.toString((char) ((char) j+'A')));
                                ArrayList<Word> tempwords = new ArrayList<>();
                                tempwords.add(temp);
                                if(board.isLegalPlay(frame, temp)) {    //checking isLegal - sometimes we get a bad GADDAG and just making sure none throw isLegal() errors for the board
                                    if (dictionary.areWords(tempwords)) {   //check main word is legal
                                        tempwords.remove(temp);
                                        for (Word w : getAllWords(temp)) {
                                            tempwords.add(w);
                                        }
                                        if (dictionary.areWords(tempwords)) {
                                            int score = 0;
                                            score = getAllPoints(tempwords, g);
                                            if (usedLetters(temp).length() - usedLetters(temp).replaceAll("[ZQX]","").length() >0){     //here we have strategy for if it uses high value letters
                                                if(pool>20)     //TODO experiment with these numbers
                                                {
                                                    if(score>maxScore&&score>60){
                                                        bestWord = temp;
                                                        maxScore = score;
                                                        blanks = " "+Character.toString((char) ((char) i + 'A')) + Character.toString((char) ((char) j + 'A'));
                                                    }
                                                }
                                                else if(score>maxScore)
                                                {
                                                    bestWord = temp;
                                                    maxScore = score;
                                                    blanks = " "+Character.toString((char) ((char) i + 'A')) + Character.toString((char) ((char) j + 'A'));
                                                }
                                            }
                                            else if(pool>20)        //if we're under 80% of the way in we want to try and save blanks
                                            {
                                                if(score>maxScore&&score>50){
                                                    bestWord = temp;
                                                    maxScore = score;
                                                    blanks = " "+Character.toString((char) ((char) i + 'A')) + Character.toString((char) ((char) j + 'A'));
                                                }
                                            }
                                            else if (score > maxScore) {//otherwise just use them as we can
                                                bestWord = temp;
                                                maxScore = score;
                                                blanks = " "+Character.toString((char) ((char) i + 'A')) + Character.toString((char) ((char) j + 'A'));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else{   //one blank strategy, basically the same but with lower minimum point thresholds
                            Word temp = new Word(sr, sc, g.isHorizontal, s, Character.toString((char) ((char) i+'A')));
                            ArrayList<Word> tempwords = new ArrayList<>();
                            tempwords.add(temp);
                            if(board.isLegalPlay(frame, temp)) {
                                if (dictionary.areWords(tempwords)) {
                                    tempwords.remove(temp);
                                    for (Word w : getAllWords(temp)) {
                                        tempwords.add(w);
                                    }
                                    if (dictionary.areWords(tempwords)) {
                                        int score = 0;
                                        score = getAllPoints(tempwords, g);
                                        if (usedLetters(temp).length() - usedLetters(temp).replaceAll("[ZQX]","").length() >0){
                                            if(pool>20) //TODO experiment with these numbers
                                            {
                                                if(score>maxScore&&score>50){
                                                    bestWord = temp;
                                                    maxScore = score;
                                                    blanks = " "+Character.toString((char) ((char) i + 'A'));
                                                }
                                            }
                                            else if(score>maxScore)
                                            {
                                                bestWord = temp;
                                                maxScore = score;
                                                blanks = " "+Character.toString((char) ((char) i + 'A'));
                                            }
                                        }
                                        else if(pool>20)
                                        {
                                            if(score>maxScore&&score>40){
                                                bestWord = temp;
                                                maxScore = score;
                                                blanks = " "+Character.toString((char) ((char) i + 'A'));
                                            }
                                        }
                                        else if (score > maxScore) {
                                            bestWord = temp;
                                            maxScore = score;
                                            blanks = " "+Character.toString((char) ((char) i + 'A'));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {  //word with no blanks
                    Word temp = new Word(sr, sc, g.isHorizontal, s);
                    ArrayList<Word> tempwords = new ArrayList<>();
                    tempwords.add(temp);
                    if(board.isLegalPlay(frame, temp)) {
                        if (dictionary.areWords(tempwords)) {
                            tempwords.remove(temp);
                            for (Word w : getAllWords(temp)) {
                                tempwords.add(w);
                            }
                            if (dictionary.areWords(tempwords)) {
                                int score = 0;
                                score = getAllPoints(tempwords, g);
                                if (usedLetters(temp).length() - usedLetters(temp).replaceAll("[ZQX]","").length() >0){
                                    if(pool>20) //TODO experiment with these numbers
                                    {//if its using a high value tile we want to try and ensure a minimum score for the first 80% of the game - strategy
                                        if(score>maxScore&&score>25){
                                            bestWord = temp;
                                            maxScore = score;
                                            blanks = "";
                                        }
                                    }
                                    else if(score>maxScore)
                                    {
                                        bestWord = temp;
                                        maxScore = score;
                                        blanks = "";
                                    }
                                }
                                else if (score > maxScore) {
                                    bestWord = temp;
                                    maxScore = score;
                                    blanks = "";
                                }
                            }
                        }
                    }
                }
            }
        }
        if((maxScore != 0&&pool<1)||maxScore>5)//TODO check what size pool and min score works best here
        {//minimum score of 5 while we arent trying to finish the game
            command = "    "+Character.toString((char) (bestWord.getFirstColumn()+'A')) + Integer.toString(bestWord.getFirstRow()+1)+"   ";
            command += bestWord.isHorizontal() ? " A ":" D ";
            command += "   "+bestWord.toString(); //creates command for the best word
            command += blanks;  //the spaces break other peoples code (dirty trick for the competition)
        }
        System.out.println(command);
        return command;
    }


    //method that returns any characters used by us in a word ie. any not current on the board that are in word
    public String usedLetters(Word word){
        String s="";
        for (int i=0; i<word.toString().length(); i++){
            if(word.isHorizontal()){
                if(!board.getSquareCopy(word.getFirstRow(), word.getFirstColumn()+i).isOccupied()){ //if its not occupied we are placing it
                    s+=Character.toString(word.getLetter(i));
                }
            }
            else{
                if(!board.getSquareCopy(word.getFirstRow()+i, word.getFirstColumn()).isOccupied()){
                    s+=Character.toString(word.getLetter(i));
                }
            }
        }
        return s;
    }

    public ArrayList<Word> getAllWords(Word mainWord) {     //altered version of chris code
        ArrayList<Word> words = new ArrayList<>();
        words.add(mainWord);
        int r = mainWord.getFirstRow();
        int c = mainWord.getFirstColumn();
        for (int i=0; i<mainWord.length(); i++) {
            if (!board.getSquareCopy(r,c).isOccupied()) {
                if (isAdditionalWord(r, c, mainWord.isHorizontal())) {
                    words.add(getAdditionalWord(r, c, mainWord.isHorizontal(), mainWord.getDesignatedLetter(i)));
                }
            }
            if (mainWord.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        return words;
    }

    private boolean isAdditionalWord(int r, int c, boolean isHorizontal) {//altered version of chris code
        if ((isHorizontal &&
                ((r>0 && board.getSquareCopy(r-1,c).isOccupied()) || (r< Board.BOARD_SIZE-1 && board.getSquareCopy(r+1,c).isOccupied()))) ||
                (!isHorizontal &&
                        ((c>0 && board.getSquareCopy(r,c-1).isOccupied()) || (c< Board.BOARD_SIZE-1 && board.getSquareCopy(r,c+1).isOccupied()))) ) {
            return true;
        }
        return false;
    }

    private Word getAdditionalWord(int mainWordRow, int mainWordCol, boolean mainWordIsHorizontal, char letter) {//altered version of chris code
        int firstRow = mainWordRow;
        int firstCol = mainWordCol;
        // search up or left for the first letter
        while (firstRow >= 0 && firstCol >= 0 && (board.getSquareCopy(firstRow,firstCol).isOccupied()||(firstRow==mainWordRow&&firstCol==mainWordCol))) {
            if (mainWordIsHorizontal) {
                firstRow--;
            } else {
                firstCol--;
            }
        }
        // went too far
        if (mainWordIsHorizontal) {
            firstRow++;
        } else {
            firstCol++;
        }
        // collect the letters by moving down or right
        String letters = "";
        int r = firstRow;
        int c = firstCol;
        while (r< Board.BOARD_SIZE && c< Board.BOARD_SIZE && (board.getSquareCopy(r,c).isOccupied()||(r==mainWordRow&&c==mainWordCol))) {
            if(r==mainWordRow&&c==mainWordCol)
            {
                letters = letters + letter;
            }
            else{
                letters = letters + board.getSquareCopy(r,c).getTile().getLetter();
            }
            if (mainWordIsHorizontal) {
                r++;
            } else {
                c++;
            }
        }
        return new Word (firstRow, firstCol, !mainWordIsHorizontal, letters);
    }

    public ArrayList<Word> getAllWordsChallenge(Word mainWord) {//version of chris code altered for challenging
        ArrayList<Word> words = new ArrayList<>();
        words.add(mainWord);
        int r = mainWord.getFirstRow();
        int c = mainWord.getFirstColumn();
        for (int i=0; i<mainWord.length(); i++) {
            if (board.getSquareCopy(r,c).isOccupied()) {
                if (isAdditionalWord(r, c, mainWord.isHorizontal())) {
                    words.add(getAdditionalWordChallenge(r, c, mainWord.isHorizontal()));
                }
            }
            if (mainWord.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        return words;
    }

    private Word getAdditionalWordChallenge(int mainWordRow, int mainWordCol, boolean mainWordIsHorizontal) {//version of chris code altered for challenging
        int firstRow = mainWordRow;
        int firstCol = mainWordCol;
        // search up or left for the first letter
        while (firstRow >= 0 && firstCol >= 0 && board.getSquareCopy(firstRow,firstCol).isOccupied()) {
            if (mainWordIsHorizontal) {
                firstRow--;
            } else {
                firstCol--;
            }
        }
        // went too far
        if (mainWordIsHorizontal) {
            firstRow++;
        } else {
            firstCol++;
        }
        // collect the letters by moving down or right
        String letters = "";
        int r = firstRow;
        int c = firstCol;
        while (r< Board.BOARD_SIZE && c< Board.BOARD_SIZE && board.getSquareCopy(r,c).isOccupied()) {
            letters = letters + board.getSquareCopy(r, c).getTile().getLetter();
            if (mainWordIsHorizontal) {
                r++;
            } else {
                c++;
            }
        }
        return new Word (firstRow, firstCol, !mainWordIsHorizontal, letters);
    }

    public ArrayList<String> getCombinations(String s)  //returns all combinations of a string - used for the fram
    {
        ArrayList<String> allCombinations = new ArrayList<String>();
        permute(s, 0, s.length()-1, allCombinations);
        addShortened(allCombinations);
        return allCombinations;
    }

    private static void permute(String str, int l, int r, ArrayList<String> al)//finds permutations of length str
    {
        if (l == r)
            al.add(str);
        else
        {
            for (int i = l; i <= r; i++)
            {
                str = swap(str,l,i);
                permute(str, l+1, r, al);
                str = swap(str,l,i);
            }
        }
    }

    public static String swap(String a, int i, int j)   //swaps two chars in a string
    {
        char temp;
        char[] charArray = a.toCharArray();
        temp = charArray[i] ;
        charArray[i] = charArray[j];
        charArray[j] = temp;
        return String.valueOf(charArray);
    }

    public static void addShortened(ArrayList<String> al){  //adds shortened versions of all permutations thereby including all permutations of shorter lengths
        int size = al.size();
        HashSet<String> h = new HashSet<>();
        for(int i=0; i<size; i++)
        {
            for(int j=al.get(0).length(); j>1; j--)
            {
                h.add(al.get(i).substring(0, j - 1));
            }
        }
        for(String s : h)
        {
            al.add(s);
        }
    }

    private int getFirstWordPoints(Word word) {     //gets points specifically for the first word
        int wordValue = 0;
        int wordMultipler = 1;
        int r = word.getFirstRow();
        int c = word.getFirstColumn();
        for (int i = 0; i<word.length(); i++) {
            Tile tile = new Tile(word.getLetter(i));
            int letterValue = tile.getValue();
            wordValue = wordValue + letterValue * board.getSquareCopy(r, c).getLetterMuliplier();
            wordMultipler = wordMultipler * board.getSquareCopy(r, c).getWordMultiplier();
            if (word.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        if(word.length()==7)
        {
            wordValue+=50;
        }
        return wordValue * wordMultipler;
    }

    private void addStringsWithoutBlanks(String s, ArrayList<String> al)    //adds all possibilities of blank(s) in a string to an arrayList
    {
        for(int i=0;i<s.length();i++) {
            if (s.charAt(i) == '_') {
                for (int j = 0; j < 26; j++) {
                    s = s.substring(0, i) + Character.toString((char) ('A' + j)) + s.substring(i + 1);
                    if (s.substring(i + 1).contains("_")) {
                        addStringsWithoutBlanks(s, al);
                    } else {
                        al.add(s);
                    }
                }
            }
        }
    }


    public boolean isHook(int r, int c){        //finds if a square can be used to place a word on
        Square s = board.getSquareCopy(r,c);
        boolean isHook=false;
        if(!s.isOccupied())
        {
            //check if has next door tile
            if(r>=1){
                if(board.getSquareCopy(r-1,c).isOccupied())
                {
                    isHook=true;
                }
            }
            if(c>=1){
                if(board.getSquareCopy(r,c-1).isOccupied())
                {
                    isHook=true;
                }
            }
            if(r<=13){
                if(board.getSquareCopy(r+1,c).isOccupied())
                {
                    isHook=true;
                }
            }
            if(c<=13){
                if(board.getSquareCopy(r,c+1).isOccupied())
                {
                    isHook=true;
                }
            }
        }
        return isHook;
    }

    public void generateGaddags(int r, int c, HashSet<GADDAG> hs){
        GADDAG acrossMaster = new GADDAG(board, r, c, true);    //we start with the lines, and then reduce them to words only using the number of letters in the frame
        GADDAG downMaster = new GADDAG(board, r, c, false);
        ArrayList<String> suffixes = new ArrayList<>();
        ArrayList<String> prefixes = new ArrayList<>();
        String suf = ""+acrossMaster.suffix.charAt(0);
        int i=0;
        //finds all length suffixes with up to frame length empty squares
        while(suf.length()-suf.replaceAll("[?]", "").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()&&acrossMaster.start.col+i<14)
        {
            if(acrossMaster.suffix.charAt(i+1)=='?'&&suf.length()>1)
            {
                suffixes.add(suf);
            }
            i++;
            suf+=acrossMaster.suffix.charAt(i);
        }
        suffixes.add(suf);
        i=0;
        String pre=acrossMaster.prefix.length()==0 ? "" : ""+acrossMaster.prefix.charAt(0);
        //finds all length prefixes with up to frame length empty squares
        while(pre.length()-pre.replaceAll("[?]", "").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()&&acrossMaster.start.col-i-1>0)
        {
            if(acrossMaster.prefix.charAt(i+1)=='?')
            {
                prefixes.add(pre);
            }
            i++;
            pre+=acrossMaster.prefix.charAt(i);
        }
        prefixes.add(pre);
        //combines all prefixes and suffixes together
        for(String s : suffixes){
            if(acrossMaster.prefix.length()!=0) {
                if(acrossMaster.prefix.charAt(0)=='?') {
                    hs.add(new GADDAG("", s, acrossMaster.start.row, acrossMaster.start.col, true));
                }
            }
            for(String p : prefixes)
            {
                if ((s+p).length()-(s+p).replaceAll("[?]","").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()) {
                    hs.add(new GADDAG(p, s, acrossMaster.start.row, acrossMaster.start.col, true));
                }
            }
        }
        for(String p : prefixes)
        {
            hs.add(new GADDAG(p, ""+acrossMaster.suffix.charAt(0), acrossMaster.start.row, acrossMaster.start.col, true));
        }

        //repeated process for down
        suffixes = new ArrayList<>();
        prefixes = new ArrayList<>();
        suf = ""+downMaster.suffix.charAt(0);
        i=0;
        while(suf.length()-suf.replaceAll("[?]", "").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()&&downMaster.start.row+i<14)
        {
            if(downMaster.suffix.charAt(i+1)=='?'&&suf.length()>1)
            {
                suffixes.add(suf);
            }
            i++;
            suf+=downMaster.suffix.charAt(i);
        }
        suffixes.add(suf);
        i=0;
        pre=downMaster.prefix.length()==0 ? "" : ""+downMaster.prefix.charAt(0);
        while(pre.length()-pre.replaceAll("[?]", "").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()&&downMaster.start.row-i-1>0)
        {
            if(downMaster.prefix.charAt(i+1)=='?')
            {
                prefixes.add(pre);
            }
            i++;
            pre+=downMaster.prefix.charAt(i);
        }
        prefixes.add(pre);
        for(String s : suffixes){
            if(downMaster.prefix.length()!=0) {
                if(downMaster.prefix.charAt(0)=='?') {
                    hs.add(new GADDAG("", s, downMaster.start.row, downMaster.start.col, false));
                }
            }
            for(String p : prefixes)
            {
                if ((s+p).length()-(s+p).replaceAll("[?]","").length()<me.getFrameAsString().replaceAll("[^A-Z_]","").length()) {
                    hs.add(new GADDAG(p, s, downMaster.start.row, downMaster.start.col, false));
                }
            }
        }
        for(String p : prefixes)
        {
            hs.add(new GADDAG(p, ""+downMaster.suffix.charAt(0), downMaster.start.row, downMaster.start.col, false));
        }
    }

    public void getGADDAGFrameCombinations(GADDAG g, ArrayList<String> combinations, HashSet<String> GADDAGcombos){//maps a frame permutation onto a gaddag
        for(String s : combinations)
        {
            String c = g.toString();
            if(s.length()==c.length()-c.replaceAll("[?]","").length())
            {
                int j=0;
                for (int i=0;i<c.length();i++)
                {
                    if(c.charAt(i)=='?')
                    {
                        c=c.substring(0, i)+s.charAt(j)+c.substring(i+1);//replace ? with next char in string
                        j++;
                    }
                }
                GADDAGcombos.add(c);//add the mapping to the list
            }
        }
    }

    private int getWordPoints(Word word) {//based on chris' code
        int wordValue = 0;
        int wordMultipler = 1;
        int r = word.getFirstRow();
        int c = word.getFirstColumn();
        for (int i = 0; i < word.length(); i++) {
            int letterValue = board.getSquareCopy(r, c).isOccupied()? board.getSquareCopy(r, c).getTile().getValue() : new Tile(word.getLetters().charAt(i)).getValue();
            if (!board.getSquareCopy(r, c).isOccupied()) {
                wordValue = wordValue + letterValue * board.getSquareCopy(r, c).getLetterMuliplier();
                wordMultipler = wordMultipler * board.getSquareCopy(r, c).getWordMultiplier();
            } else {
                wordValue = wordValue + letterValue;
            }
            if (word.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        return wordValue * wordMultipler;
    }

    public int getAllPoints(ArrayList<Word> words, GADDAG g) {//based on chris code
        int points = 0;
        for (Word word : words) {
            points = points + getWordPoints(word);
        }
        if (g.toString().length()-g.toString().replaceAll("[?]","").length() == Frame.MAX_TILES) {
            points = points + 50;
        }
        return points;
    }

    //might be an idea to write a method that determines the best thing to exchange
    //for example AEILNRST are considered to be the most useful letters
    //and q's and z's will score lots of points

    private class GADDAG {
        public String prefix;
        public String suffix;
        public IntPair start;
        public boolean isHorizontal;
        GADDAG(String pre, String suf, int r, int c, boolean b){//almost a word definition
            this.prefix = pre;
            this.suffix = suf;
            this.start = new IntPair(r,c);
            this.isHorizontal = b;
        }
        GADDAG(BoardAPI board, int row, int col, boolean isHorizontal){
            //produces a gaddag starting at a tile on that line where ? represents empty squares - max seven ?'s in suffix or prefix
            this.prefix = "";
            this.suffix = "";
            this.start = new IntPair(row,col);
            this.isHorizontal = isHorizontal;
            int ctemp=col, rtemp=row;
            if(isHorizontal){
                while (ctemp<15) {
                    Square temp = board.getSquareCopy(row, ctemp);
                    if(temp.isOccupied()){
                        suffix=suffix+temp.getTile().getLetter();
                    }
                    else{
                        suffix+="?";
                        if(suffix.length()-suffix.replace("?", "").length()>=me.getFrameAsString().replaceAll("[^A-Z_]", "").length())
                        {
                            break;
                        }
                    }
                    ctemp++;
                }
                ctemp=col-1;
                while (ctemp>-1) {
                    Square temp = board.getSquareCopy(row, ctemp);
                    if(temp.isOccupied()){
                        prefix=prefix+temp.getTile().getLetter();
                    }
                    else{
                        prefix+="?";
                        if(prefix.length()-prefix.replace("?", "").length()>=me.getFrameAsString().replaceAll("[^A-Z_]", "").length())
                        {
                            break;
                        }
                    }
                    ctemp--;
                }
            }
            else
            {
                while (rtemp<15) {
                    Square temp = board.getSquareCopy(rtemp, col);
                    if(temp.isOccupied()){
                        suffix=suffix+temp.getTile().getLetter();
                    }
                    else{
                        suffix+="?";
                        if(suffix.length()-suffix.replace("?", "").length()>=me.getFrameAsString().replaceAll("[^A-Z_]", "").length())
                        {
                            break;
                        }
                    }
                    rtemp++;
                }
                rtemp=row-1;
                while (rtemp>-1) {
                    Square temp = board.getSquareCopy(rtemp, col);
                    if(temp.isOccupied()){
                        prefix=prefix+temp.getTile().getLetter();
                    }
                    else{
                        prefix+="?";
                        if(prefix.length()-prefix.replace("?", "").length()>=me.getFrameAsString().replaceAll("[^A-Z_]", "").length())
                        {
                            break;
                        }
                    }
                    rtemp--;
                }
            }
        }

        public String reverse(String s) {//used to reverse a prefix because gaddags store prefixes backwards
            String c = "";
            for(int i=s.length()-1; i>=0; i--)
            {
                c+=s.charAt(i);
            }
            return c;
        }

        public String toString(){
            return reverse(prefix)+suffix;
        }
    }

    private class IntPair {//just something we found useful for squares
        public int row;
        public int col;
        IntPair(int r, int c){
            row = r;
            col = c;
        }
    }

}
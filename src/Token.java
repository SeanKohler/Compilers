public class Token {
    int linenumber;
    int lineposition;
    int number;
    String character;
    String tokenType;

    //We can utilize overloaded methods to account for:
    //Numbers: linenum, linepos, number, tokenType                  (0-9)
    //Variables: linenum, linepos, character, tokenType             (a-z) 
    //Special Characters: linenum, linepos, character, tokenType    ({,},+,=,!=,==,etc.)

    public Token(int linenumber, int lineposition, int number, String tokenType){
        this.linenumber = linenumber;
        this.lineposition = lineposition;
        this.number = number;
        this.tokenType = tokenType;
    }
    public Token(int linenumber, int lineposition, String character, String tokenType){
        this.linenumber = linenumber;
        this.lineposition = lineposition;
        this.character = character;
        this.tokenType = tokenType;
    }
    public int getLinenumber(){
        return linenumber;
    }
    public int getLineposition(){
        return lineposition;
    }
    public int getNumber(){
        return number;
    }
    public String getTknType(){
        return tokenType;
    }
    public String getCharacter(){
        return character;
    }
}

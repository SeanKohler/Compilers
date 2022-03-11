import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
public class Lexer {
    public static ArrayList<Token> Lex(String filename, int linenumber,int lineposition, int prognum){
        ArrayList<Token>tokenStream = new ArrayList<Token>();
        File file = new File(filename);
        ArrayList<String>words = new ArrayList<String>();
        String regex="[a|c|d|e|g|h|j|k|l|m|n|o|q|r|u|v|x|y|z]";
        String keyregex="[b|f|i|p|s|t|w]";
        ArrayList<String>matches = new ArrayList<String>();
        matches.add("boolean");
        matches.add("false");
        matches.add("if");
        matches.add("int");
        matches.add("print");
        matches.add("string");
        matches.add("true");
        matches.add("while");
        String catchup="";
        int increment =0;
          try {
            Scanner input = new Scanner(file);
            while(increment<linenumber){//We do this to get the input variable back to the next scanned program
                catchup = input.nextLine();
                increment+=1;
            }
            boolean moreOnLine=false;
            if(lineposition<catchup.length()-1){
                moreOnLine=true;
            }
            int errorCount=0;
            String currentStr="";
            String nums="[0|1|2|3|4|5|6|7|8|9]";
            prnt("Lexing Program "+prognum,"", 0,0,0,"INFO", tokenStream);
            while (input.hasNextLine()||(!input.hasNextLine()&&lineposition<catchup.length()-1)){
                String line;
                if(moreOnLine==true){
                    line=catchup;
                }else{
                    line = input.nextLine();
                    linenumber = linenumber+1;
                }
                String word="";
                for(int i=0; i<line.length(); i++){
                    if(moreOnLine==true){
                        i=lineposition;
                        moreOnLine=false;
                    }else{
                        //keep it i
                    }
                    char character = line.charAt(i);
                    String charToString = String.valueOf(character);
                    word = word + charToString.trim();
                    currentStr = charToString;//This is so the scope of the variable can be expanded
                    //Test to see if character is a key char ex. (, ), {, }, =, +, !, <, >, $

                    //All Valid parts of the language
                    /*
                    All Single character variables lowercase a-z
                    Single digit 0-9
                    {, }, (, ), "", ==, !=, +
                    print, while, if, int, string, boolean, false, true
                    //All single char variables that dont start with keyword letters (p,w,i,s,b,f,t)
                    */
                    charToString = charToString.trim();
                    word = word.trim();
                    if(isValidchar(charToString)||isValidchar(word)||charToString.equals("")){                    
                    //Due to the = symbol being both a single (=) and double (==) character we should test for that before any other special characters
                    if(charToString.equals("=")){
                        if(i<line.length()-1){//There is a valid next character in the line
                            char temp = line.charAt(i+1);
                            String nextstring = String.valueOf(temp);
                            if(nextstring.equals("=")){//This should catch a double ==
                                word="==";
                                i+=1;
                                specialChar(word, linenumber, i+1,line.length(), true, tokenStream);
                                word="";
                            }else{//The single = goes here
                                specialChar(word, linenumber, i+1,line.length(), true, tokenStream);
                                word="";
                            }
                        }else{//A line shouldn't end in an = but technecally it should pass the lex phase
                            if(input.hasNextLine()){
                                line = input.nextLine();
                                linenumber = linenumber+1;
                                int saveinc = i;
                                i=0;
                                char temp = line.charAt(i);
                                String tempStr = String.valueOf(temp);
                                //System.out.println(tempStr);
                                if(tempStr.equals("=")){
                                    word="==";
                                    specialChar(word, linenumber, i+1,line.length(), true, tokenStream);
                                    word="";
                                }else{//We checked the first symbol on the next line and it was not another =
                                    word="=";
                                    specialChar(word, linenumber-1, saveinc+1,line.length(), true, tokenStream);//Print the =
                                    word="";
                                    if(specialChar(tempStr, linenumber, i+1, line.length(), false, tokenStream)){
                                       specialChar(tempStr, linenumber, i+1,line.length(), true, tokenStream);//Print the first character of next line
                                        //word="";
                                    }else if(tempStr.matches(nums)){
                                        //word="";
                                        //System.out.println(tempStr);
                                        prnt("NUM", tempStr, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                    }else if(tempStr.matches(keyregex)){
                                        i=lineWrap(matches, tempStr, word, i, linenumber, line, tokenStream);
                                    }else{
                                        //word="";
                                        prnt("ID", tempStr, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                    }
                                    //System.out.println(tempStr+" "+i);
                                }
                            }else{//last line ended with an =
                                specialChar(charToString, linenumber, i+1, line.length(),true, tokenStream);
                            }
                        }
                    }else if(charToString.equals("!")){
                        if(i<line.length()-1){//There is a valid next character in the line
                            char temp = line.charAt(i+1);
                            String nextstring = String.valueOf(temp);
                            if(nextstring.equals("=")){//This should catch a double !!=
                                word="!=";
                                i+=1;
                                specialChar(word, linenumber, i+1,line.length(), true, tokenStream);
                                word="";
                            }else{//The single ! goes here
                                prnt("Unrecognized Token ! "+"("+linenumber+":"+i+")","",0, 0,line.length(), "ERROR", tokenStream);
                                word="";
                                errorCount+=1;
                            }
                        }else{//A line shouldn't end in an = but technecally it should pass the lex phase
                            if(input.hasNextLine()){
                                line = input.nextLine();
                                linenumber = linenumber+1;
                                int saveinc = i;
                                i=0;
                                char temp = line.charAt(i);
                                String tempStr = String.valueOf(temp);
                                if(tempStr.equals("=")){
                                    word="!=";
                                    specialChar(word, linenumber, i+1,line.length(), true, tokenStream);
                                    word="";
                                }else{//We checked the first symbol on the next line and it was not a =
                                    word="";
                                    prnt("Unrecognized Token ! "+"("+(linenumber-1)+":"+(saveinc+1)+")","",0, 0,line.length(), "ERROR", tokenStream);
                                    if(tempStr.matches(keyregex)){
                                        i=lineWrap(matches, tempStr, word, i, linenumber, line, tokenStream);
                                    }else{
                                        //word="";
                                        prnt("ID", tempStr, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                    }
                                }
                            }else{//last line ended with an =
                                prnt("Unrecognized Token ! "+"("+linenumber+":"+i+")","",0, 0,line.length(), "ERROR", tokenStream);
                            }
                        }
                    }else if(specialChar(charToString, linenumber, i+1,line.length(), true, tokenStream)==true){//This handles printing for all special characters of length 1
                        word="";                        
                    }else if(word.equals("/*")){//This will ignore anything inside of a comment
                        word="";
                        boolean terminateComment = false;
                        while(terminateComment==false){
                            if(line.length()==0){
                                if(input.hasNextLine()){//If there is another line, reset counters and keep looking for end comment
                                    line=input.nextLine();
                                    linenumber+=1;
                                    i=0;
                                }else{//This is only reached if there are no more lines & characters and we never got a end comment
                                    prnt("Comment Never Terminated, Expected - */ ("+linenumber+":"+(line.length()-1)+")","",0, 0,line.length(), "ERROR", tokenStream);
                                    errorCount+=1;
                                    terminateComment=true;//We have to do this to break out of the loop. Comment was never terminated
                                }
                            }else{
                                char c = line.charAt(i);
                                String conv = String.valueOf(c);
                                if(conv.equals("*")){
                                    if(i+1<line.length()){
                                        char next = line.charAt(i+1);
                                        String convnext = String.valueOf(next);
                                        if(convnext.equals("/")){//This is reached if the character after a * is a / meaning its the end of a comment
                                            terminateComment=true;
                                        }
                                    }
                                }
                                if(i==line.length()-1){
                                    if(input.hasNextLine()){//If there is another line, reset counters and keep looking for end comment
                                        line=input.nextLine();
                                        linenumber+=1;
                                        i=0;
                                    }else{//This is only reached if there are no more lines & characters and we never got a end comment
                                        prnt("Comment Never Terminated, Expected - */ ("+linenumber+":"+(line.length()-1)+")","",0, 0,line.length(), "ERROR", tokenStream);
                                        errorCount+=1;
                                        terminateComment=true;
                                    }
                                }else{//If there are more characters still in the line, inc to the next one
                                    i+=1; 
                                }
                            }    
                        }
                    }else if(Character.isWhitespace(character)){//Once we remove special characters out we should break into words based on spaces
                       if(!word.equals("")){
                        words.add(word);
                        if(word.equals("*/")){
                            prnt("Unrecognized Token: "+word+" ("+linenumber+":"+(i+1)+")","",0, 0,line.length(), "ERROR",tokenStream);
                            errorCount+=1;
                        }else{
                            prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                        }
                        word=" ";
                       }
                    }else if(charToString.matches(nums)){//Numbers 0-9 are caught here
                        words.add(word);
                        prnt("NUM", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                        word=" ";
                    }
                    else if(word.equals("print")||word.equals("while")||word.equals("if")){//print, while, if, int, string, boolean, false, true
                        specialChar(word, linenumber, i,line.length(), true, tokenStream);
                        word="";
                    }else if(word.equals("int")||word.equals("string")||word.equals("boolean")||word.equals("false")||word.equals("true")){
                        specialChar(word, linenumber, i,line.length(), true, tokenStream);
                        word="";
                    }else{
                        if(i==line.length()-1){
                            if(!word.equals("")){//If the line ends with a variable name and there are no spaces following it
                                words.add(word);
                                if(word.equals("*/")){
                                    prnt("Unrecognized Token: "+word+" ("+linenumber+":"+(i+1)+")","",0, 0,line.length(), "ERROR",tokenStream);
                                    errorCount+=1;          
                                }else{
                                    prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                }
                                
                                word="";
                            }
                        }else{
                            char next = line.charAt(i+1);
                            String convert = String.valueOf(next);
                            if(charToString.matches(regex)&&word.length()==1) {//(p,w,i,s,b,f,t) letters of keywords
                                if(!word.equals("")){
                                    words.add(word);
                                    prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                    word="";
                                }
                            }else if(charToString.matches(keyregex)){//If the string matches the beginning of a keyword
                                 ArrayList<String>match = new ArrayList<String>();
                                 for(int x=0; x<matches.size(); x++){
                                     String current = matches.get(x);
                                     if(current.contains(charToString)){
                                         match.add(current);
                                     }
                                 }
                                 String concat=charToString;
                                 boolean equals = false;
                                 int inc=1;
                                 for(int x=0; x<match.size(); x++){
                                     while(match.get(x).contains(concat)&&equals==false){
                                        if(concat.length()<=match.get(x).length()){
                                            if(match.get(x).equals(concat)){
                                                equals=true;
                                                word=concat;
                                                i+=inc-1;
                                                specialChar(word, linenumber, i,line.length(), true, tokenStream);
                                                word="";
                                            }else{
                                               char temp=line.charAt(i+inc);
                                               concat+=String.valueOf(temp);
                                               inc+=1;
                                            }
                                        }
                                     }
                                 }
                                 if(equals==false){
                                    word=charToString;
                                    words.add(word);
                                    prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                    word="";
                                 }
                            }
                            if(charToString.equals("\"")){
                                prnt("BeginningQuote", charToString, linenumber, i+1,line.length(),"DEBUG", tokenStream);
                                i+=1;
                                word="";
                                boolean foundEnd=false;
                                String cur="";
                                    while(i<line.length()&&foundEnd==false){
                                        char temp = line.charAt(i);
                                        String tempstr = String.valueOf(temp);
                                        cur = tempstr;
                                        if(tempstr.equals("\"")){
                                            prnt("EndQuote", tempstr, linenumber, i+1,line.length(),"DEBUG", tokenStream);
                                            //i+=1;
                                            foundEnd = true;
                                            word="";
                                        }else if(isValidStringChar(tempstr)){
                                            if(tempstr.equals(" ")){
                                                tempstr="__SPACE__";
                                            }
                                            prnt("CHAR", tempstr, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                            i+=1;
                                            word="";
                                        }else{
                                            prnt("Unrecognized Token: "+tempstr+" ("+linenumber+":"+(i+1)+")","",0, 0,line.length(), "ERROR",tokenStream);
                                            errorCount+=1;
                                            i+=1;
                                            word="";
                                        }
                                        //i+=1;
                                    }
                                    if(foundEnd==false){
                                        prnt("Quote Never Terminated, Expected - \" ("+linenumber+":"+line.length()+")","",0, 0,line.length(), "ERROR", tokenStream);
                                        errorCount+=1;
                                        Token tkn = new Token(linenumber, i+1, line.length(), 0, "__ERROR__");
                                        tokenStream.add(tkn);
                                        //return tokenStream;
                                    }
                            }else if(specialChar(convert, linenumber, i,line.length(), false, tokenStream)){//This checks if the next character is a special character
                                if(!word.equals("")){
                                words.add(word);
                                //if word = */
                                prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
                                word="";
                                }
                            }
                        }
                    }
                    if(charToString.equals("$")){
                        prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,line.length(),"INFO", tokenStream);
                        if(errorCount>0){
                            errorCount=0;
                            if(!input.hasNextLine()&&i<line.length()){
                                Token tkn = new Token(linenumber, i+1, line.length(), 0, "__ERROR__");
                                tokenStream.add(tkn);
                                return tokenStream;
                            }else if(input.hasNextLine()){
                                Token tkn = new Token(linenumber, i+1, line.length(), 0, "__ERROR__");
                                tokenStream.add(tkn);
                                return tokenStream;
                            }else{
                                prnt("No more lines found in file","", 0,0,line.length(),"INFO", tokenStream);  
                            }
                        }else{
                            for(int x=0; x<tokenStream.size(); x++){
                                Token temp = tokenStream.get(x);
                                //Here is our token stream
                                //System.out.println(temp.getTknType()+" "+temp.getCharacter()+" "+temp.getLinenumber()+" "+temp.getLineposition());
                            }
                            return tokenStream;
                        }
                        tokenStream.clear();
                        if(input.hasNextLine()){
                            prognum += 1;
                            System.out.println();//Separate progs with a space in terminal
                            prnt("Lexing Program "+prognum,"", 0,0,line.length(),"INFO", tokenStream);
                        }else if(i<line.length()-1&&!input.hasNextLine()){//A program ened on the last line.. but there are more characters following the $
                            //Should we allow a program to start and end in this scenario?
                        }else if(i<line.length()-1){//If the program doesnt start on the first position of the line
                            prognum += 1;
                            System.out.println();//Separate progs with a space in terminal
                            prnt("Lexing Program "+prognum,"", 0,0,line.length(),"INFO", tokenStream);
                        }
                    }
                    lineposition=i;//Used to extend scope of i TO DO: [REPLACE INSTANCES OF i WITH lineposition]
                    }else{
                        prnt("Unrecognized Token: "+charToString+" ("+linenumber+":"+(i+1)+")","",0, 0,line.length(), "ERROR",tokenStream);
                        word="";
                        errorCount+=1;
                    }
                }
                if(!input.hasNextLine()&&lineposition==line.length()-1){//Testing last position of last line of file
                    if(!currentStr.equals("$")){
                        Token tkn = new Token(linenumber, lineposition, line.length(), 0, "__ERROR__");
                        tokenStream.add(tkn);
                        prnt("Program Never Terminated, Expected - $ ("+linenumber+":"+(lineposition+1)+")","",0, 0,line.length(), "ERROR", tokenStream);
                        errorCount+=1;
                        prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,line.length(),"INFO", tokenStream);
                        return tokenStream;
                    }else{
                        if(errorCount > 0){
                            Token tkn = new Token(linenumber, lineposition, line.length(), 0, "__ERROR__");
                            tokenStream.add(tkn);
                            return tokenStream;
                        }
                    }
                }
            }
            input.close();//Closes File after we read all contents
          }
          catch(Exception e) {
            System.out.println(e);
          }
        return tokenStream;
    }
    public static boolean specialChar(String character, int linenumber, int i, int linelen, boolean print, ArrayList<Token> tokenStream){
        String text="";
        if(character.equals("(")){
            text="LeftParen";
        }else if(character.equals(")")){
            text="RightParen";
        }else if(character.equals("{")){
            text="LeftCurlBrace";
        }else if(character.equals("}")){
            text="RightCurlBrace";
        }else if(character.equals("=")){
            text="Assignment";
        }else if(character.equals("+")){
            text="Addition";
        }else if(character.equals("!=")){
            text="Inequality";
        }else if(character.equals("==")){
            text="Equality";
        }else if(character.equals("/*")){
            text="BeginningComment";
        }else if(character.equals("*/")){
            text="EndComment";
        }else if(character.equals("int")){
            text="INT_TYPE";
        }else if(character.equals("string")){
            text="STR_TYPE";
        }else if(character.equals("boolean")){
            text="BOOL_TYPE";
        }else if(character.equals("print")){
            text="PRNT_STMT";
        }else if(character.equals("while")){
            text="WHILE_STMT";
        }else if(character.equals("if")){
            text="IF_STMT";
        }else if(character.equals("false")){
            text="BOOL_F";
        }else if(character.equals("true")){
            text="BOOL_T";
        }else if(character.equals("$")){
            text="EOP";
        }else{
            return false;
        }
        /*
        print, while, if, int, string, boolean, false, true
        All single char variables that dont start with keyword letters (p,w,i,s,b,f,t)
        */
        if(print ==true){//Sometimes we may want to check if a special character is next and not print it as the current character
            prnt(text, character, linenumber, i, linelen, "DEBUG",tokenStream);
        }
        return true; 
    }
    public static void prnt(String name, String character, int linenumber, int i, int linelen, String type, ArrayList<Token> tokenStream){
        character = character.trim();
        if(type.equals("DEBUG")){
            if(character.equals("__SPACE__")){
                character=" ";
            }
            System.out.println("DEBUG Lexer - "+name +" [ "+character+" ] Found At ("+linenumber+":"+i+")");
            if(character.matches("[0-9]")){
                int number = Integer.valueOf(character);
                Token tkn = new Token(linenumber, i, linelen, number, name);
                tokenStream.add(tkn);
            }else{
                Token tkn = new Token(linenumber, i, linelen, character, name);
                tokenStream.add(tkn);
            }
        }else if(type.equals("INFO")){
            System.out.println("INFO Lexer - "+name);
        }else if(type.equals("ERROR")){//I decided against warnings because any warning would fail in the next stages of the Compiler (and i dont want to add to the code that was input). If i detect something, i dont believe it should be passed to the next stage
            System.out.println("ERROR Lexer - "+name);
        }
    }
    public static boolean isValidchar(String charToString){//Regex for valid characters in the language
        String regex="[a-z0-9||(|)|{|}|=|+|!|/|*|\"|$]";
        if(charToString.matches(regex)){
            return true;
        }else{
            return false;
        }
    }
    public static boolean isValidStringChar(String charToString){//Specifically for inside strings
        String regex="[a-z| ]";
        if(charToString.matches(regex)){
            return true;
        }else{
            return false;
        }
    }
    public static int lineWrap(ArrayList<String> matches,String tempStr,String word, int i, int linenumber, String line, ArrayList<Token>tokenStream){
        ArrayList<String>match = new ArrayList<String>();
        for(int x=0; x<matches.size(); x++){
            String current = matches.get(x);
            if(current.contains(tempStr)){
                match.add(current);
            }
        }
        String concat=tempStr;
        boolean equals = false;
        int inc=1;
        for(int x=0; x<match.size(); x++){
            while(match.get(x).contains(concat)&&equals==false){
                if(concat.length()<=match.get(x).length()){
                    if(match.get(x).equals(concat)){
                        equals=true;
                        word=concat;
                        i+=inc-1;
                        specialChar(word, linenumber, i,line.length(), true, tokenStream);
                        word="";
                    }else{
                    char t=line.charAt(i+inc);
                    concat+=String.valueOf(t);
                    inc+=1;
                    }
                }
            }
        }
        if(equals==false){
            word=tempStr;
            prnt("ID", word, linenumber, i+1,line.length(), "DEBUG", tokenStream);
            word="";
        }
        return i;
    }
}

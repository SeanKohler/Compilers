import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
public class Lexer {
    public static int Lex(String arg, int linenumber, int prognum){
        String filename = arg;
        File file = new File(filename);
        ArrayList<String>words = new ArrayList<String>();
        int increment=0;
          try {
            Scanner input = new Scanner(file);
            while(increment<linenumber){//We do this to get the input variable back to the next scanned program
                String catchup = input.nextLine();
                increment+=1;
            }
            //int linenumber =0;
            int lineposition=0;
            int errorCount=0;
            String currentStr="";
            String nums="[0|1|2|3|4|5|6|7|8|9]";
            prnt("Lexing Program "+prognum,"", 0,0,"INFO");
            while (input.hasNextLine()){
                String line = input.nextLine();
                linenumber = linenumber+1;
                String word="";
                for(int i=0; i<line.length(); i++){
                    
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
                    //Due to the = symbol being both a single (=) and double (==) character we should test for that before any other special characters
                    if(charToString.equals("=")){
                        if(i<line.length()-1){//There is a valid next character in the line
                            char temp = line.charAt(i+1);
                            String nextstring = String.valueOf(temp);
                            if(nextstring.equals("=")){//This should catch a double ==
                                word="==";
                                i+=1;
                                specialChar(word, linenumber, i+1, true);
                                word="";
                            }else{//The single = goes here
                                specialChar(word, linenumber, i+1, true);
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
                                if(tempStr.equals("=")){
                                    word="==";
                                    specialChar(word, linenumber, i+1, true);
                                }else{//We checked the first symbol on the next line and it was not another =
                                    word="=";
                                    specialChar(word, linenumber-1, saveinc, true);
                                }
                            }else{//last line ended with an =
                                specialChar(charToString, linenumber, i+1, true);
                            }
                        }
                    }else if(charToString.equals("!")){
                        if(i<line.length()-1){//There is a valid next character in the line
                            char temp = line.charAt(i+1);
                            String nextstring = String.valueOf(temp);
                            if(nextstring.equals("=")){//This should catch a double !s=
                                word="!=";
                                i+=1;
                                specialChar(word, linenumber, i+1, true);
                                word="";
                            }else{//The single ! goes here
                                prnt("Unrecognized Token ! "+"("+linenumber+":"+i+")","",0, 0, "ERROR");
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
                                    specialChar(word, linenumber, i+1, true);
                                    word="";
                                }else{//We checked the first symbol on the next line and it was not a =
                                    word="";
                                    prnt("Unrecognized Token ! "+"("+(linenumber-1)+":"+saveinc+")","",0, 0, "ERROR");
                                    prnt("ID", tempStr, linenumber, (i+1), "DEBUG");
                                }
                            }else{//last line ended with an =
                                prnt("Unrecognized Token ! "+"("+linenumber+":"+i+")","",0, 0, "ERROR");
                            }
                        }
                    }
                    else if(specialChar(charToString, linenumber, i+1, true)==true){//This handles printing for all special characters of length 1
                        word="";                        
                    }else if(word.equals("/*")){//This will ignore anything inside of a comment
                        word="";
                        boolean terminateComment = false;
                        while(terminateComment==false){
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
                                    prnt("Comment Never Terminated","",0, 0, "ERROR");
                                    errorCount+=1;
                                    prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,"INFO");
                                    terminateComment=true;
                                }
                            }else{//If there are more characters still in the line, inc to the next one
                               i+=1; 
                            }
                        }
                    }else if(Character.isWhitespace(character)){//Once we remove special characters out we should break into words based on spaces
                       if(!word.equals("")){
                        words.add(word);
                        prnt("ID", word, linenumber, i+1, "DEBUG");
                        word=" ";
                       }
                    }else if(charToString.matches(nums)){//Numbers 0-9 are caught here
                        words.add(word);
                        prnt("NUM", word, linenumber, i+1, "DEBUG");
                        word=" ";
                    }
                    else if(word.equals("print")||word.equals("while")||word.equals("if")){//print, while, if, int, string, boolean, false, true
                        specialChar(word, linenumber, i, true);
                        word="";
                    }else if(word.equals("int")||word.equals("string")||word.equals("boolean")||word.equals("false")||word.equals("true")){
                        specialChar(word, linenumber, i, true);
                        word="";
                    }else{
                        if(i==line.length()-1){
                            if(!word.equals("")){//If the line ends with a variable name and there are no spaces following it
                                words.add(word);
                                prnt("ID", word, linenumber, i+1, "DEBUG");
                                word="";
                            }
                        }else{
                            char next = line.charAt(i+1);
                            String convert = String.valueOf(next);
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
                            if(charToString.matches(regex)&&word.length()==1) {//(p,w,i,s,b,f,t) letters of keywords
                                if(!word.equals("")){
                                    words.add(word);
                                    prnt("ID", word, linenumber, i+1, "DEBUG");
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
                                                specialChar(word, linenumber, i, true);
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
                                    prnt("ID", word, linenumber, i+1, "DEBUG");
                                    word="";
                                 }
                            }
                            if(charToString.equals("\"")){
                                prnt("BeginningQuote", charToString, linenumber, i+1,"DEBUG");
                                next = line.charAt(i+1);
                                convert = String.valueOf(next);
                                boolean found=false;
                                while(!convert.equals("\"")&&found==false){
                                    if(i<line.length()-1){
                                        next = line.charAt(i+1);
                                        convert = String.valueOf(next);
                                        i+=1;
                                        if(convert.equals("\"")){
                                            prnt("EndQuote", convert, linenumber, i+1,"DEBUG");
                                            word="";
                                            found=true;
                                        }else{
                                            prnt("ID", convert, linenumber, i+1,"DEBUG");
                                            word="";
                                        }
                                    }else{
                                        prnt("Quote Never Terminated","",0, 0, "ERROR");
                                        word="";
                                        errorCount+=1;
                                        found=true;//Not really but we gotta leave the while loop
                                    }
                                }
                            }
                            if(specialChar(convert, linenumber, i, false)){//This checks if the next character is a special character
                                if(!word.equals("")){
                                words.add(word);
                                prnt("ID", word, linenumber, i+1, "DEBUG");
                                word="";
                                }
                            }
                        }
                    }
                    if(charToString.equals("$")){
                        prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,"INFO");
                        prognum+=1;
                        input.close();
                        //Rather than lex all programs at the same time we are fully compiling each program before the next
                        //Each part of the compiler will work on program 1 until program 2 reaches the lexer
                        return linenumber;


                        //I really liked this solution to my initial problem and refuse to remove it from the code
                        // if(input.hasNextLine()){
                        //     prognum += 1;
                        //     System.out.println();//Separate progs with a space in terminal
                        //     prnt("Lexing Program "+prognum,"", 0,0,"INFO");
                        // }else if(i<line.length()-1){//If the program doesnt start on the first position of the line
                        //     prognum += 1;
                        //     System.out.println();//Separate progs with a space in terminal
                        //     prnt("Lexing Program "+prognum,"", 0,0,"INFO");
                        // }
                    }
                    lineposition=i;//Used to extend scope of i TO DO: [REPLACE INSTANCES OF i WITH lineposition]
                }
                if(!input.hasNextLine()&&lineposition==line.length()-1){//Testing last position of last line of file
                    if(!currentStr.equals("$")){
                        prnt("Program Never Terminated, Expected - $ ("+linenumber+":"+(lineposition+1)+")","",0, 0, "ERROR");
                        errorCount+=1;
                        prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,"INFO");
                    }
                }
            }
            input.close();//Closes File after we read all contents
          }
          catch(Exception e) {
            System.out.println(e);
          }
        return 404;//This should never be reached
    }
    public static boolean specialChar(String character, int linenumber, int i, boolean print){
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
            prnt(text, character, linenumber, i, "DEBUG");
        }
        return true; 
    }
    public static void prnt(String name, String character, int linenumber, int i, String type){
        character = character.trim();
        if(type.equals("DEBUG")){
            System.out.println("DEBUG Lexer - "+name +" [ "+character+" ] Found At ("+linenumber+":"+i+")");
        }else if(type.equals("INFO")){
            System.out.println("INFO Lexer - "+name);
        }else if(type.equals("ERROR")){
            System.out.println("ERROR Lexer - "+name);
        }
    }
}
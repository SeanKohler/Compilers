import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
public class Compiler{
    public static void main(String[] args){
        for(int i=0; i<args.length; i++){
            System.out.println("["+i+"] = "+args[i]);
        }
        String filename = args[0];
        File file = new File(filename);
        ArrayList<String>words = new ArrayList<String>();
        int prognum=1;
          try {
            Scanner input = new Scanner(file);
            int linenumber =0;
            int lineposition=0;
            int errorCount=0;
            //System.out.println("Starting Lex 1");
            prnt("Lexing Program "+prognum,"", 0,0,"INFO");
            while (input.hasNextLine()){
                String line = input.nextLine();
                linenumber = linenumber+1;
                String word="";
                for(int i=0; i<line.length(); i++){
                    char character = line.charAt(i);
                    String charToString = String.valueOf(character);
                    word = word + charToString.trim();
                    //Test to see if character is a key char ex. (, ), {, }, =, +, !, <, >, $
                    charToString = charToString.trim();
                    word = word.trim();
                    if(specialChar(charToString, linenumber, i+1, true)==true){//This handles printing for all special characters of length 1
                        word="";                        
                    }else if(word.equals("/*")){
                        word="";
                        boolean terminateComment = false;
                        while(terminateComment==false){
                            char c = line.charAt(i);
                            String conv = String.valueOf(c);
                            if(conv.equals("*")){
                                if(i+1<line.length()){
                                    char next = line.charAt(i+1);
                                    String convnext = String.valueOf(next);
                                    if(convnext.equals("/")){
                                        terminateComment=true;
                                    }

                                }

                            }
                            if(i==line.length()-1){
                                if(input.hasNextLine()){
                                    line=input.nextLine();
                                    i=0;
                                }else{
                                    prnt("Comment Never Terminated","",0, 0, "ERROR");
                                    errorCount+=1;
                                    prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,"INFO");
                                    terminateComment=true;
                                }
                            }else{
                               i+=1; 
                            }
                            
                        }

                    }else if(Character.isWhitespace(character)){//Once we remove special characters out we should break into words based on spaces
                       if(!word.equals("")){
                        words.add(word);
                        //System.out.println("DEBUG Lexer - ID [ "+word+" ] Found At ("+linenumber+":"+(i+1)+")");
                        prnt("ID", word, linenumber, i+1, "DEBUG");
                        //System.out.println(word);
                        word=" ";
                       }
                    }else{
                        char next = line.charAt(i+1);
                        String convert = String.valueOf(next);
                        if(specialChar(convert, linenumber, i, false)){//This checks if the next character is a special character
                            if(!word.equals("")){
                                words.add(word);
                                //System.out.println("DEBUG Lexer - ID [ "+word+" ] Found At ("+linenumber+":"+(i+1)+")");
                                prnt("ID", word, linenumber, i+1, "DEBUG");
                                word="";
                               }
                        } 
                    }
                    if(charToString.equals("$")){
                        prnt("Lex Completed with: "+errorCount+" Errors","", 0,0,"INFO");
                        if(input.hasNextLine()){
                            prognum += 1;
                            System.out.println();//Separate progs with a space
                            prnt("Lexing Program "+prognum,"", 0,0,"INFO");
                        }else if(i<line.length()-1){//If the program doesnt start on the first position of the line
                            prognum += 1;
                            System.out.println();//Separate progs with a space
                            prnt("Lexing Program "+prognum,"", 0,0,"INFO");
                        }
                    }
                }
            }
            input.close();//Closes File after we read all contents
          }
          catch(Exception e) {
            System.out.println(e);
          }
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
        }else if(character.equals("!")){
            text="Negation";
        }else if(character.equals("<")){
            text="LessThan";
        }else if(character.equals(">")){
            text="GreaterThan";
        }else if(character.equals("/*")){
            text="BeginningComment";
        }else if(character.equals("*/")){
            text="EndComment";
        }else if(character.equals("$")){
            text="EOP";
        }else{
            return false;
        }
        if(print ==true){//Sometimes we may want to check if a special character is next and not print it as the current character
            //System.out.println("DEBUG Lexer - "+text +" [ "+character+" ] Found At ("+linenumber+":"+i+")");
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
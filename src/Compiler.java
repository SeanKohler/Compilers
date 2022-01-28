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
        
          try {
            Scanner input = new Scanner(file);
            int linenumber =0;
            int lineposition=0;
            while (input.hasNextLine()){
                String line = input.nextLine();
                //System.out.println(line);
                linenumber = linenumber+1;
                String word="";
                for(int i=0; i<line.length(); i++){
                    char character = line.charAt(i);
                    String charToString = String.valueOf(character);
                    word = word + charToString.trim();
                    //Test to see if character is a key char ex. (, ), {, }, =, +, !, $
                    /*
                    Code goes here
                    */

                    //Once we remove special characters out we should break into words based on spaces
                    if(Character.isWhitespace(character)||charToString.equals("$")){
                       if(!word.equals("")){
                        words.add(word);
                        System.out.println("DEBUG Lexer -["+word+"] Found At ("+linenumber+":"+i+")");
                        word="";
                       }
                    }else{
                        //System.out.println("DEBUG Lexer -["+character+"] Found At ("+linenumber+":"+i+")");
                    }
                    
                }
            }
            input.close();//Closes File after we read all contents
          }
          catch(Exception e) {
            System.out.println(e);
          }
    }

}
import java.io.File;
import java.util.Scanner;
public class Compiler{
    public static void main(String[] args){
        for(int i=0; i<args.length; i++){
            System.out.println("["+i+"] = "+args[i]);
        }
        String filename = args[0];
        File file = new File(filename);

          try {
            Scanner input = new Scanner(file);
            int linenumber =0;
            while (input.hasNextLine()){
                String line = input.nextLine();
                //System.out.println(line);
                linenumber = linenumber+1;

                for(int i=0; i<line.length(); i++){
                    char character = line.charAt(i);
                    if(Character.isWhitespace(character)){
                       //Do nothing if whitespace is found 
                    }else{
                        System.out.println("DEBUG Lexer -["+character+"] Found At ("+linenumber+":"+i+")");
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
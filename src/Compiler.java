import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Compiler {
    public static void main(String[] args) throws FileNotFoundException {
        for (int i = 0; i < args.length; i++) {
            System.out.println("[" + i + "] = " + args[i]);
        }
        int linenumber = 0;
        int lineposition=0;
        int linelen =0;
        int prognum = 1;
        while (hasMoreLines(linenumber, args[0]) == true||(!hasMoreLines(linenumber, args[0]) == true && lineposition < linelen-1)) {
            ArrayList<Token> tokenStream = Lexer.Lex(args[0], linenumber,lineposition, prognum);
            if (tokenStream.size() > 0) {
                if (tokenStream.get(tokenStream.size() - 1).getTknType().equals("__ERROR__")) {
                    System.out.println();// Separate for readability
                    linenumber = tokenStream.get(tokenStream.size() - 1).getLinenumber();
                    lineposition = tokenStream.get(tokenStream.size() - 1).getLineposition();
                    linelen = tokenStream.get(tokenStream.size() -1 ).getLinelen();
                    prognum += 1;
                } else {
                    for (int i = 0; i < tokenStream.size(); i++) {
                        // System.out.println("----Token----");
                        Token temp = tokenStream.get(i);
                        // System.out.println(temp.getLinenumber());
                        // System.out.println(temp.getLineposition());
                        // System.out.println(temp.getCharacter());
                        // System.out.println(temp.getTknType());
                    }
                    /*
                     * Parser is next and should be called here
                     */
                    
                    
                    linenumber = tokenStream.get(tokenStream.size() - 1).getLinenumber();
                    lineposition = tokenStream.get(tokenStream.size() - 1).getLineposition();
                    linelen = tokenStream.get(tokenStream.size() -1 ).getLinelen();
                    //System.out.println(lineposition+" "+linelen+" "+linenumber);
                    System.out.println();// Separate for readability
                    prognum += 1;
                }
            }else{
                linenumber+=1;//Needed to break out of the while loop as the file has ended
            }
            // linenumber = tokenStream.get(tokenStream.size() - 1).getLinenumber();
            // lineposition = tokenStream.get(tokenStream.size() - 1).getLineposition();
            // linelen = tokenStream.get(tokenStream.size() -1 ).getLinelen();
            // System.out.println(lineposition+" "+linelen);

        }
    }

    public static boolean hasMoreLines(int linenumber, String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner input = new Scanner(file);
        int inc = 0;
        while (input.hasNextLine() && inc < linenumber) {
            String line = input.nextLine();
            inc += 1;
        }
        if (input.hasNextLine()) {
            input.close();
            return true;
        } else {
            input.close();
            return false;
        }

    }
}
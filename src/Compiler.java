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
        int prognum = 1;
        while (hasMoreLines(linenumber, args[0]) == true) {
            ArrayList<Token> tokenStream = Lexer.Lex(args[0], linenumber, prognum);
            if (tokenStream.size() > 0) {
                if (tokenStream.get(tokenStream.size() - 1).getTknType().equals("__ERROR__")) {
                    System.out.println();// Separate for readability
                    linenumber = tokenStream.get(tokenStream.size() - 1).getLinenumber();
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
                    // System.out.println("-------"+prognum+"------");
                    System.out.println();// Separate for readability
                    prognum += 1;
                }
            }else{
                linenumber+=1;//Needed to break out of the while loop as the file has ended
            }

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
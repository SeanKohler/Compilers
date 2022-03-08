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
            /*
            * Lexer is first and is called here
            */
            ArrayList<Token> tokenStream = Lexer.Lex(args[0], linenumber,lineposition, prognum);
            if (tokenStream.size() > 0) {
                if (tokenStream.get(tokenStream.size() - 1).getTknType().equals("__ERROR__")) {
                    System.out.println("---Lex Errors. Don't Parse---");
                    System.out.println();// Separate for readability
                    linenumber = tokenStream.get(tokenStream.size() - 1).getLinenumber();
                    lineposition = tokenStream.get(tokenStream.size() - 1).getLineposition();
                    linelen = tokenStream.get(tokenStream.size() -1 ).getLinelen();
                    prognum += 1;
                } else {
                    int ln = tokenStream.get(tokenStream.size()-1).getLinenumber();
                    int lp = tokenStream.get(tokenStream.size()-1).getLineposition();
                    int ll = tokenStream.get(tokenStream.size()-1).getLinelen();
                    ArrayList<Token> temp= new ArrayList<Token>();
                    temp = tokenStream;


                    /*
                     * Parser is next and should be called here
                     */
                    Tree CST =Parser.Parse(temp, prognum);
                    Node n =CST.getRoot(CST);//Root node of our CST
                    //System.out.println(n.getName(n));
                    if(n.getName(n)=="__ERROR__"){
                        System.out.println("---Parse Errors. Don't Do Semantic Analysis---");
                    }else{
                    /**
                     * 
                     * Semantic Analysis is next and should be called here
                     * 
                     */ 





                    }
                    

                    linenumber = ln;//It gets altered in parse (this fixes that issue)
                    lineposition = lp;
                    linelen =ll;
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
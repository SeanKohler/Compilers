import java.util.ArrayList;

public class SemanticAnalysis {
    public static Tree Semantic_Analysis(ArrayList<Token> tknStream, int prognum){
        prnt("Processing Program Number: "+prognum, "INFO");
        Tree t = new Tree();
        for(int i=0; i<tknStream.size(); i++){
            System.out.println(tknStream.get(i).getCharacter());
            Token currentTkn = tknStream.get(i);
            String currentChar = tknStream.get(i).getCharacter();

        }
        return t;
    }
    public static void prnt(String text,String method){
        System.out.println(method+" SEMANTIC ANALYSIS: "+text);
    }
}

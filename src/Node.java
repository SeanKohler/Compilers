import java.util.ArrayList;
public class Node {
    ArrayList<Node> children = new ArrayList<Node>();
    Node Parent = null;
    String name = "";
    Token associated;//Each node is associated to a token. This will save the token in the node
    int scope;

    public Node(Token associated, String name, int scope){
        this.associated = associated;
        this.name = name; 
        this.scope = scope;
    }
    public String getName(Node n){
        return n.name;
    }
    public int getScope(){
        return scope;
    }
}

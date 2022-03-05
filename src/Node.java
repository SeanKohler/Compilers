import java.util.ArrayList;
public class Node {
    ArrayList<Node> children = new ArrayList<Node>();
    Node Parent = null;
    String name = "";
    Token associated;//Each node is associated to a token. This will save the token in the node

    public Node(Token associated, String name){
        this.associated = associated;
        this.name = name; 
    }
    public String getName(Node n){
        return n.name;
    }
}

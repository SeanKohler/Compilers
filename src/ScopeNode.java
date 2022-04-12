import java.util.ArrayList;
import java.util.HashMap;

public class ScopeNode {
    ScopeNode Parent = null;
    ArrayList<ScopeNode> children = new ArrayList<ScopeNode>();
    HashMap<String, SymbolObj> hashmap;
    int scope;
    public ScopeNode(HashMap<String, SymbolObj>  hm, int scope){
        this.hashmap = hm;
        this.scope = scope;
    }
    public HashMap<String, SymbolObj> getMap(){
        return hashmap;
    }
}

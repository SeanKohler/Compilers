import java.util.HashMap;

public class ScopeTree {
    ScopeNode root = null;
    ScopeNode current = null;
    public void addScope(HashMap<String, SymbolObj> hm, int scope){
        ScopeNode n = new ScopeNode(hm, scope);
        if(this.root == null){
            this.root = n;//If there is no root, make it the root
            n.Parent = null;//From slides
        }else{
            n.Parent = current;//The previous node (this.current) is the parent node of the new node
            current.children.add(n);
            //this.current.children.add(n);//The new node is the child of the previous node
        }
        current = n;
    }
    public void moveUp(String location){
        this.current = this.current.Parent;
    }
    public ScopeNode getRoot(ScopeTree t){
        return t.root;
    }
}

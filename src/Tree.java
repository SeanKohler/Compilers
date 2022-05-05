public class Tree {
    Node root = null;
    Node current = null;
    ScopeTree tree = null;
    public void addNode(Token tkn, String type, String name, int scope){
        Node n = new Node(tkn, name, scope);//This is the new node we want to add
        if(this.root == null){
            this.root = n;//If there is no root, make it the root
            n.Parent = null;//From slides
        }else{
            n.Parent = current;//The previous node (this.current) is the parent node of the new node
            current.children.add(n);
            //this.current.children.add(n);//The new node is the child of the previous node
        }
        if(!type.equals("leaf")){
            current=n;
        }
    }
    public void moveUp(String location){
        this.current = this.current.Parent;
        System.out.println(location);
    }
    public Node getRoot(Tree t){
        return t.root;
    }
}

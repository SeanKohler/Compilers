import java.util.ArrayList;

public class SymbolObj {
    String type;
    boolean init;
    boolean used;
    int scope;
    ArrayList<String> errors = new ArrayList<String>();
    Token associated;
    public SymbolObj(String type, boolean init, boolean used, int scope){
        this.type = type;
        this.init = init;
        this.used = used;
        this.scope = scope;
    }
    public String getType(){
        return type;
    }
    public boolean getInit(){
        return init;
    }
    public boolean getUsed(){
        return used;
    }
    public int getScope(){
        return scope;
    }
    public ArrayList<String> getErrors(){
        return errors;
    }
    public Token getAssociated(){
        return associated;
    }
}

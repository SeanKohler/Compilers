import java.util.ArrayList;
import java.util.HashMap;

public class SemanticAnalysis {
    public static Tree Semantic_Analysis(ArrayList<Token> tokenStream, int prognum) {
        Tree tree = new Tree();
        prnt("Parsing Program: " + prognum,"INFO");
        prnt("parse()","DEBUG");
        ArrayList<Token> temp = tokenStream;// This way we dont detroy the origin tokenStream
        boolean valid = true;
        ParseProgram(temp, tree, valid);
        HashMap<String,SymbolObj> hm = new HashMap<String,SymbolObj>();
        ScopeTree st = new ScopeTree();
        st.addScope(hm, 0);
        if (validtest(temp)) {
            prnt("Parse Completed with: 0 Errors","INFO");
            System.out.println("");
            System.out.println("PRINT AST:");
            st =prntTree(tree.root, 0,1,1, st);
            System.out.println("SEMANTIC ANALYSIS: COMPLETE AST, PRINT ANY ERRORS:");
            boolean founderr = false;
            founderr = prntErr(st.getRoot(st),founderr);
            if(founderr==false){
                System.out.println("NO ERRORS. PRINT SYMBOL TABLE:");
                System.out.println("Name \t Type \t Scope \t Line");
                prntTable(st.getRoot(st));
                prntWarnings(st.getRoot(st));
            }
        } else {
            System.out.println("------------------------");
            System.out.println("SEMANTIC ANALYSIS FAILED");
            System.out.println("------------------------");
            //prnt("---Parse Errors, Dont create CST---","INFO");
            tree.getRoot(tree).name ="__ERROR__";
        }
        return tree;
    }

    public static ArrayList<Token> ParseProgram(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseProgram()","DEBUG");
        tree.addNode(tknStream.get(0), "root", "program",0);//Program Root
        int scope=0;
        ParseBlock(tknStream, tree,scope, valid);
        valid = validtest(tknStream);
        if (valid) {
            Match("$", tknStream, tree, scope, valid);
        }else{
            prnt("Invalid in ParseProgram","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseBlock(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        prnt("parseBlock()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "Block",scope);//Each new block is a branch
        Match("{", tknStream, tree, scope, valid);
        scope++;
        ParseStatementList(tknStream, tree, scope, valid);
        Match("}", tknStream, tree, scope, valid);
        scope--;
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSEBLOCK","ERROR");
        }
        if(tknStream.size()==1){
            //Dont move up Remaining tkn is $
        }else{
            tree.moveUp("Block");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseStatementList(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        prnt("parseStatementList()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "StatementList");//Each new StatementList is a branch
        ParseStatement(tknStream, tree, scope, valid);
        if (!isValidStatement(tknStream)) {
            //tree.moveUp("StmtList");
            return tknStream;
        } else {
            ParseStatementList(tknStream, tree, scope, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STATEMENT LIST","ERROR");
        }
        //tree.moveUp("StmtList");
        return tknStream;

    }

    public static ArrayList<Token> ParseStatement(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        prnt("parseStatement()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "Statement");//Each new Statement is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("PRNT_STMT")) {// Print Statement
            ParsePrintStatement(tknStream, tree, scope, valid);
        } else if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            tree.addNode(tknStream.get(0), "branch", "Var Decl",scope);
            TypeCheck(tknStream, type, tree, scope, valid);
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()),scope);
            Match("ID", tknStream, tree, scope, valid);
            tree.moveUp("VarDecl");
        } else if (type.equals("ID")) {// Assignment Statement
            tree.addNode(tknStream.get(0), "branch", "Assignment",scope);
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()),scope);//a = 1 + a
            Match("ID", tknStream, tree, scope, valid);
            Match("Assignment", tknStream, tree, scope, valid);
            ParseExpression(tknStream, tree, scope, valid,"Assignment");
            //tree.moveUp("Assignment");
        } else if (type.equals("WHILE_STMT")) {// While Statement
            ParseWhileStatement(tknStream, tree, scope, valid,"WhileStmt");
        } else if (type.equals("IF_STMT")) {// If Statement
            ParseIfStatement(tknStream, tree, scope, valid,"IfStmt");
        } else if (type.equals("LeftCurlBrace")) {// Block
            ParseBlock(tknStream, tree, scope, valid);
        } else if (type.equals("RightCurlBrace")) {
            tree.moveUp("ε");
            return tknStream;
        } else {
            if(type.equals("__ERROR__")){
                //Do Nothing, we saw the error
            }else{
                prnt("TYPE: " + type + " Invalid for STATEMENT AT - ("+next.getLinenumber()+":"+next.getLineposition()+")","ERROR");
                tknStream = emptyStream(tknStream);// Removes contents and adds an Error Token    
            }
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STATEMENT","ERROR");
        }
        //tree.moveUp("STMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseExpression(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid, String from) {
        prnt("parseExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "Expression",0);//Each new Expression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("NUM")) {// IntExpr
            ParseIntExpr(tknStream, tree,scope, valid,from);
        } else if (type.equals("BeginningQuote")) {// StringExpr (Starts with a ")
            ParseStringExpression(tknStream, tree,scope, valid, from);
        } else if (type.equals("LeftParen")||type.equals("BOOL_T")||type.equals("BOOL_F")) { // BoolExpr
            ParseBooleanExpression(tknStream, tree,scope, valid,from);
        } else if (type.equals("ID")) {// MatchID
            tree.addNode(tknStream.get(0), "leaf",tknStream.get(0).getCharacter(), scope);
            Match("ID", tknStream, tree,scope, valid);
            if(from.equals("Assignment")||from.equals("ParseBool")||from.equals("PrintStmt")){
                tree.moveUp("ASSN");
            }
        }else{
            prnt("INVALID IN EXPRESSION GOT: "+next.getCharacter(),"ERROR");
            emptyStream(tknStream);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE EXPRESSION","ERROR");
        }
        //tree.moveUp("EXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseIntExpr(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid, String from) {
        prnt("parseIntExpression()","DEBUG");
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if(type.equals("NUM")){
            boolean exists=false;
            int sync = 1;
            int count=0;
            if(from.equals("PrintStmt")){
                count=-1;
            }
            while(tknStream.get(sync).getTknType().equals("Addition")){
                count++;
                exists=true;
                tree.addNode(tknStream.get(sync), "branch", tknStream.get(sync).getCharacter(),scope);
                
                if(sync==1){
                    if(tknStream.get(0).getTknType().equals("ID")){
                        tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()),scope);
                    }else{
                        tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()),scope);  
                    }
                   Match("NUM", tknStream, tree, scope, valid); 
                }
                Match("+", tknStream, tree, scope, valid);
                if(tknStream.get(0).getTknType().equals("ID")){
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()),scope);
                    Match("ID", tknStream, tree, scope, valid);
                }else if(!tknStream.get(0).getTknType().equals("NUM")){
                    System.out.println("ERROR: EXPECTED TYPE NUM GOT TYPE: "+tknStream.get(0).getTknType()+" On line: "+tknStream.get(0).getLinenumber());
                }else{
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()),scope);
                    Match("NUM", tknStream, tree, scope, valid); 
                }
                
                sync=0;
                if(tknStream.get(sync).getTknType().equals("__ERROR__")){
                    tknStream.get(sync).character = "__ERROR__";
                }
            }
            if(exists==false){
                tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()), scope);
                Match("NUM", tknStream, tree, scope, valid);
            }
            if(from.equals("Assignment")){
                tree.moveUp("ASSN");
            }
            for(int i=0; i<count; i++){
                tree.moveUp("Addition");
            }
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE INT EXPRESSION","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseStringExpression(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid, String from) {
        prnt("parseStringExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "StringExpression");//Each new StringExpression is a branch
        Match("BeginningQuote", tknStream, tree, scope, valid);
        ParseCharList(tknStream, tree, scope, valid, from);
        Match("EndQuote", tknStream, tree, scope, valid);
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STRING EXPRESSION","ERROR");
        }
        //tree.moveUp("STREXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParsePrintStatement(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        prnt("parsePrintStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "PrintStatement",scope);//Each new PrintStatement is a branch
        Match("print", tknStream, tree, scope, valid);
        Match("(", tknStream, tree, scope, valid);
        ParseExpression(tknStream, tree, scope, valid,"PrintStmt");
        Match(")", tknStream, tree, scope, valid);
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE PRINT STATEMENT","ERROR");
        }
        //tree.moveUp("PRNTSTMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseWhileStatement(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid,String from) {
        prnt("parseWhileStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "WhileStatement",0);//Each new WhileStatement is a branch
        Match("while", tknStream, tree, scope, valid);
        ParseBooleanExpression(tknStream, tree,scope,valid,from);
        ParseBlock(tknStream, tree,scope, valid);
        //tree.moveUp("WHILESTMT");
        return tknStream;
    }   

    public static ArrayList<Token> ParseBooleanExpression(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid, String from) {
        prnt("parseBooleanExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "BooleanExpression",0);//Each new BooleanExpression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("LeftParen")) {
            Match("(", tknStream, tree,scope, valid);
            int inc=0;
            while(!(tknStream.get(inc).getTknType().equals("Inequality")||tknStream.get(inc).getTknType().equals("Equality"))){
                inc++;
                if(tknStream.get(inc).getTknType().equals("NUM")){
                    //skip
                }else if(tknStream.get(inc).getTknType().equals("Inequality")||tknStream.get(inc).getTknType().equals("Equality")){
                    tree.addNode(tknStream.get(inc), "branch",tknStream.get(inc).getCharacter(), scope);
                }
            }
            tknStream.remove(inc);
            ParseExpression(tknStream, tree,scope, valid,"ParseBool1");
            ParseExpression(tknStream, tree,scope, valid,"ParseBool");
            Match(")", tknStream, tree,scope, valid);
            if(from.equals("WhileStmt")){

            }else{
                tree.moveUp("BOOLOP");  
            }
            
        }else if(type.equals("BOOL_T")){
            tree.addNode(tknStream.get(0), "leaf",tknStream.get(0).getCharacter(), scope);
            Match("true", tknStream, tree,scope, valid);
            if(from.equals("Assignment")||from.equals("ParseBool")){
                tree.moveUp("ASSN");
            }
        }else if(type.equals("BOOL_F")){
            tree.addNode(tknStream.get(0), "leaf",tknStream.get(0).getCharacter(), scope);
            Match("false", tknStream, tree,scope, valid);
            if(from.equals("Assignment")||from.equals("ParseBool")){
                tree.moveUp("ASSN");
            }
        }else{
            prnt("EXPECTED: BooleanExpression GOT: "+next.getCharacter(),"ERROR");
            tknStream =emptyStream(tknStream);
        }
        //tree.moveUp("BOOLEXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseIfStatement(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid,String from) {
        prnt("parseIfStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "IfStatement",scope);//Each new IfStatement is a branch
        Match("if", tknStream, tree, scope, valid);
        ParseBooleanExpression(tknStream, tree, scope, valid,from);
        ParseBlock(tknStream, tree, scope, valid);
        //tree.moveUp("IFSTMT");
        return tknStream;
    }

    public static ArrayList<Token> TypeCheck(ArrayList<Token> tknStream, String type, Tree tree, int scope, boolean valid) {
        if (type.equals("INT_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "int",scope);
            Match("int", tknStream, tree, scope, valid);
        } else if (type.equals("STR_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "string",scope);
            Match("string", tknStream, tree, scope, valid);
        } else if (type.equals("BOOL_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "boolean",scope);
            Match("boolean", tknStream, tree, scope, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN TYPE CHECK","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseCharList(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid, String from) {
        prnt("parseCharList()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "CharList");//Each new CharList is a branch
        Token current = tknStream.get(0);
        String type = current.getTknType();
        String full="";
        while (!current.getCharacter().equals("\"")) {
            full+=current.getCharacter();
            Match("CHAR", tknStream, tree, scope, valid);
            current = tknStream.get(0);
        }
        tree.addNode(tknStream.get(0), "leaf",'"'+full+'"',scope);//Each new CharList is a leaf
        if(from.equals("BoolOp")){

        }else{
           tree.moveUp("CharList"); 
        }
        
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE CHAR LIST","ERROR");
        }
        //tree.moveUp("CHARLIST");
        return tknStream;
    }

    public static ArrayList<Token> ParseBoolOp(ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        prnt("parseBoolOp()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "BoolOp");//Each new BoolOp is a branch
        Token next = tknStream.get(0);
        if (next.tokenType.equals("Inequality")) {// Inequality
            tree.addNode(tknStream.get(0), "branch", String.valueOf(tknStream.get(0).getCharacter()),scope);
            Match("!=", tknStream, tree, scope, valid);
        } else if (next.tokenType.equals("Equality")) {// Equality
            tree.addNode(tknStream.get(0), "branch", String.valueOf(tknStream.get(0).getCharacter()),scope);
            Match("==", tknStream, tree, scope, valid);
        }
        tree.moveUp("BOOLOP");
        return tknStream;
    }

    public static ArrayList<Token> Match(String character, ArrayList<Token> tknStream, Tree tree, int scope, boolean valid) {
        if (validtest(tknStream)) {
            Token next = tknStream.get(0);
            if (next.tokenType.equals("NUM")) {
                String regex = "[0-9]";
                int num = next.getNumber();
                if (String.valueOf(num).matches(regex)&&character.equals("NUM")) {
                    prnt(character + " Matched: " + next.getNumber() + " - (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","DEBUG");
                    //tree.addNode(tknStream.get(0), "leaf", character+" , "+num);//If it matched Add it as a leaf node
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: "+character+" GOT: [0-9] AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }
            } else if (character.equals("ID") || character.equals("CHAR")) {
                if(isValidStringChar(next.getCharacter())){
                    prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    //tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                }else{
                    valid = false;
                    prnt("INVALID - EXPECTED: [a-z| ] GOT: "+next.getCharacter()+" AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }              
            } else if (character.equals(next.getCharacter())) {//Matches exact characters
                prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                //tree.addNode(tknStream.get(0), "leaf", character);//If it matched Add it as a leaf node
            } else if (character.equals("Assignment")) {
                if (next.getCharacter().equals("=")) {
                    prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    //tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [=] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }
            } else if (character.equals("BeginningQuote") || character.equals("EndQuote")) {
                if (next.character.equals("\"")) {
                    prnt(character + "Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    //tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [\"] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }

            } else if (next.tokenType.equals("__ERROR__")) {//We got an error in match()
                valid = false;
                tknStream.get(0).valid = false;
                return tknStream;
            } else {
                prnt("INVALID - EXPECTED: [" + character + "] GOT: " + next.getCharacter() + " AT ("+ next.getLinenumber() + ":" + next.getLineposition() + ")","ERROR");
                valid = false;
                emptyStream(tknStream);
            }
        } else {
            emptyStream(tknStream);
        }

        /**
         * 
         * 
         * Now that we have categorized the current token, we should remove it and look
         * to the next one
         * 
         */
        if (tknStream.size() > 1) {
            Token temp = tknStream.get(0);
            tknStream.remove(temp);
        } else {
            //System.out.println("Ended");
        }
        if (valid == false) {
            tknStream.get(0).valid = false;
        }
        return tknStream;
    }

    public static boolean isValidStatement(ArrayList<Token> tknStream) {
        boolean valid = false;
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("PRNT_STMT")) {// Print Statement
            valid=true;
        } else if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            valid=true;
        } else if (type.equals("ID")) {// Assignment Statement
            valid=true;
        } else if (type.equals("WHILE_STMT")) {// While Statement
            valid=true;
        } else if (type.equals("IF_STMT")) {// If Statement
            valid=true;
        } else if (type.equals("LeftCurlBrace")) {// Block
            valid=true;
        }
        return valid;
    }

    public static ArrayList<Token> emptyStream(ArrayList<Token> tknStream) {
        int linenum = tknStream.get(0).getLinenumber();
        int linepos = tknStream.get(0).getLineposition();
        while(tknStream.size()>0){// Once parse is invalid. We should empty tknStream to exit the parse
            tknStream.remove(0);
        }
        Token tkn = new Token(linenum, linepos, 0, 0, "__ERROR__");//We add this so we know there was an error
        tkn.valid = false;
        tknStream.add(tkn);
        return tknStream;
    }

    public static boolean validtest(ArrayList<Token> tknStream) {
        boolean valid = tknStream.get(0).getValidStatus();
        return valid;
    }

    public static void prnt(String method, String type) {
        if(type.equals("DEBUG")){
            //System.out.println("DEBUG Parser: " + method);
        }else if(type.equals("ERROR")){
            //System.out.println("ERROR Parser: " + method);
        }else if(type.equals("INFO")){
            //System.out.println("INFO Parser: " + method);
        }
    }
    public static boolean isValidStringChar(String charToString){//Specifically for inside strings
        String regex="[a-z| ]";
        if(charToString.matches(regex)){
            return true;
        }else{
            return false;
        }
    }
    public static ScopeTree prntTree(Node node, int indent, int currentscope,int tracker, ScopeTree st){
        currentscope = node.getScope();
        while(st.current.scope > currentscope){
            st.moveUp("Adjust");
        }
        HashMap<String,SymbolObj> hm = st.current.getMap();
        String spacing = "";
        for(int i=0;i<indent; i++){
            spacing+="-";
        }         
        if(node.children.size() > 0){//Branches have children, leaf nodes do not
            spacing+="<" + node.name + ">";
            System.out.println(spacing);//This line prints the branches
            if(node.name.equals("Var Decl")){
                Node type=node.children.get(0);
                Node id = node.children.get(1);
                String idName = id.getName(id);
                hm = st.current.getMap();
                if(hm.containsKey(idName)){
                    //Do errors, Redeclaring an existing variable
                    addErrorMsg(hm,node.associated,"ERROR: Var: "+idName+" is already Defined in scope: "+node.getScope());
                }else{
                    SymbolObj obj = new SymbolObj(type.associated.getTknType(), false, false,currentscope);//All values we want to associate with an ID
                    obj.associated = node.associated;
                    hm.put(idName,obj); 
                    st.current.hashmap = hm;
                }
            }else if(node.name.equals("Block")){
                HashMap<String,SymbolObj> tmp = new HashMap<String, SymbolObj>();
                st.addScope(tmp, node.getScope()+1);
                hm = st.current.getMap();
            }else if(node.name.equals("Assignment")){
                hm = st.current.getMap();
                int linenum = node.associated.getLinenumber();
                Node beingassigned = node.children.get(0);
                Node next = node.children.get(1);
                if(hm.containsKey(beingassigned.getName(beingassigned))){
                    hm.get(beingassigned.getName(beingassigned)).init = true;
                    String type =hm.get(beingassigned.getName(beingassigned)).getType();
                    int inc=0;
                    boolean founderr = false;
                    String compType = next.associated.getTknType();
                    //String compType = next.getToken().getTknType();
                    if(compType.equals("ID")){//If it a variable being assigned to a variable.. get the type of the second variable Ex. a=b
                        if(hm.containsKey(next.getName(next))){
                            compType=hm.get(next.getName(next)).getType();
                        }else{
                            addErrorMsg(hm,next.associated, next.getName(next)+" was used before being defined");
                        }
                    }
                    if(compType.equals("NUM")){
                        compType = "int";
                    }
                    if(type.equals("INT_TYPE")){
                        type = "int";
                    }
                    if(type.equals("string")&&compType.equals("EndQuote")){
                        //Valid
                        if(hm.containsKey(next.getName(next))){
                           hm.get(next.getName(next)).init = true; 
                        }else{
                            //Set used on variable from previous scope
                            checkPrevScope(st.current,next.getName(next),false);
                        }
                    }else if(type.equals("boolean")&&compType.equals("BOOL_F")){
                        //Valid
                        if(hm.containsKey(next.getName(next))){
                            hm.get(next.getName(next)).init = true; 
                         }else{
                            //Set used on variable from previous scope
                            checkPrevScope(st.current,next.getName(next),false);
                         }
                    }else if(type.equals("boolean")&&compType.equals("BOOL_T")){
                        //Valid
                        if(hm.containsKey(next.getName(next))){
                            hm.get(next.getName(next)).init = true; 
                         }else{
                             //Set used on variable from previous scope
                             checkPrevScope(st.current,next.getName(next),false);
                         }
                    }else if(type.equals("int")&&compType.equals("int")){
                        //Valid
                        if(hm.containsKey(next.getName(next))){
                            hm.get(next.getName(next)).init = true; 
                         }else{
                             //Set used on variable from previous scope
                             checkPrevScope(st.current,next.getName(next),false);
                         }
                    }
                    else if(compType.equals("Addition")){
                        //Skip this case
                    }else if(compType.equals("Equality")||compType.equals("Inequality")){
                        //Skip
                    }else{
                        String filler = compType;
                        if(filler.equals("EndQuote")){
                            filler="string";
                        }else if(filler.equals("STR_TYPE")){
                            filler="string";
                        }else if(filler.equals("BOOL_TYPE")||filler.equals("BOOL_T")||filler.equals("BOOL_F")){
                            filler = "boolean";
                        }else if(filler.equals("INT_TYPE")){
                            filler = "int";
                        }
                        if(type.equals("STR_TYPE")){
                            type="string";
                        }else if(type.equals("BOOL_TYPE")||type.equals("BOOL_T")||type.equals("BOOL_F")){
                            type = "boolean";
                        }else if(type.equals("INT_TYPE")){
                            type = "int";
                        }
                        if(type.equals(filler)){
                            //Valid
                        }else{
                            addErrorMsg(hm,next.associated,beingassigned.getName(beingassigned)+": Cannot assign type: "+type+" to a type: "+filler+" on line: "+linenum);
                        }
                    }
                    while(inc < next.children.size()&&founderr==false){
                        Node current = next.children.get(inc);
                        String chr = current.getName(current);
                        if(current.associated.getTknType().equals("NUM")&&type.equals("int")){
                            if(hm.containsKey(chr)){
                                hm.get(chr).used = true; 
                            }
                        }else if(current.associated.getTknType().equals("CHAR")&&type.equals("string")){
                            if(hm.containsKey(chr)){
                                hm.get(chr).used = true; 
                            }
                        }else if(current.associated.getTknType().matches("BOOL")&&type.equals("boolean")){
                            //System.out.println("VALID");
                            if(hm.containsKey(chr)){
                                hm.get(chr).used = true; 
                            }
                        }else if(current.associated.getTknType().equals("Addition")){
                            //Skip this case
                        }else if(current.associated.getTknType().equals("ID")){
                            String ret="NDF";
                            if(hm.containsKey(current.associated.getCharacter())){
                                ret =hm.get(current.associated.getCharacter()).getType();
                            }else if(checkPrevScope(st.current, current.associated.getCharacter(),false)){
                                ret = getPrevType(st.current,current.associated.getCharacter(),"used");
                            }
                            if(ret.equals("INT_TYPE")){
                                ret = "int";
                            }else if(ret.equals("STR_TYPE")){
                                ret = "string";
                            }else if(ret.equals("BOOL_TYPE")||ret.equals("BOOL_T")||ret.equals("BOOL_F")){
                                ret = "boolean";
                            }
                            if(type.equals("STR_TYPE")){
                                type="string";
                            }else if(type.equals("BOOL_TYPE")||type.equals("BOOL_T")||type.equals("BOOL_F")){
                                type = "boolean";
                            }else if(type.equals("INT_TYPE")){
                                type = "int";
                            }
                            if(ret.equals(type)){
                                //Valid
                            }else{
                                addErrorMsg(hm,current.associated,"Cannot associate type: "+ret+" with type: "+type+" On line: "+current.associated.getLinenumber());
                            }
                        }else if(current.associated.getTknType().equals("Equality")){

                        }else{
                            if(current.associated.getTknType().equals("BOOL_T")||current.associated.getTknType().equals("BOOL_F")){
                                if(type.equals("boolean")){
                                    //This is expected
                                }else{
                                    String tmp = current.associated.getTknType();
                                    if(tmp.equals("EndQuote")){
                                        tmp = "string";
                                    }
                                    addErrorMsg(hm,current.associated,"FAILED TO MATCH TYPE: "+tmp+" TYPE: "+type+" On line: "+current.associated.getLinenumber());
                                }
                            }else{
                                String tmp = current.associated.getTknType();
                                if(tmp.equals("EndQuote")){
                                    tmp = "string";
                                }
                                addErrorMsg(hm,current.associated,"FAILED TO MATCH TYPE: "+tmp+" TYPE: "+type+" On line: "+current.associated.getLinenumber());
                            }
                        }
                        inc++;
                        if(current.children.size()>0){
                            next = current;
                            inc = 0;
                        }
                    }
                }else{
                    if(findVar(st.root,beingassigned.name,st.current.scope, beingassigned.name,false)){
                        String type = retType(st.root,beingassigned.name,st.current.scope, beingassigned.name,"");
                        String comptype = next.associated.getTknType();
                        if(comptype.equals("ID")){
                            comptype = retType(st.root,next.name,st.current.scope,beingassigned.name,"");
                        }
                        if(comptype.equals("Addition")){
                            comptype = "int";
                        }else if(comptype.equals("INT_TYPE")||comptype.equals("NUM")){
                            comptype = "int";
                        }else if(comptype.equals("BOOL_T")||comptype.equals("BOOL_F")||comptype.equals("BOOL_TYPE")){
                            comptype = "boolean";
                        }else if(comptype.equals("STR_TYPE")||comptype.equals("EndQuote")){
                            comptype = "string";
                        }

                        if(type.equals("INT_TYPE")||type.equals("NUM")){
                            type = "int";
                        }else if(type.equals("BOOL_T")||type.equals("BOOL_F")||type.equals("BOOL_TYPE")){
                            type = "boolean";
                        }else if(type.equals("STR_TYPE")||comptype.equals("EndQuote")){
                            type = "string";
                        }
                        if(type.equals(comptype)){
                            //Valid
                        }else{
                            addErrorMsg(hm, node.associated,"TYPE: "+type+" Cannot be associated with TYPE: "+comptype+" On line: "+node.associated.getLinenumber());
                        }
                    }else{
                        addErrorMsg(hm, node.associated,beingassigned.name+": Has not been declared On line: "+node.associated.getLinenumber()); 
                    }
                }  
            }else if(node.name.equals("!=")||node.name.equals("==")){
                ArrayList<String> types = new ArrayList<String>();
                for(int i=0; i<node.children.size(); i++){
                    if(node.children.get(i).associated.getTknType().equals("ID")){
                        hm = st.current.getMap();
                        if(findVar(st.root,node.children.get(i).name,node.scope,"NOTASSIGNED",false)){
                            //Valid
                            String type = retType(st.root,node.children.get(i).name,node.scope,"NOTASSIGNED","");
                            if(type.equals("INT_TYPE")||type.equals("NUM")){
                                type = "int";
                            }else if(type.equals("BOOL_TYPE")||type.equals("BOOL_T")||type.equals("BOOL_F")){
                                type = "boolean";
                            }else if(type.equals("STR_TYPE")||type.equals("EndQuote")){
                                type = "string";
                            }
                            types.add(type);
                        }else{
                            addErrorMsg(hm,node.children.get(i).associated,"Var: "+node.children.get(i).name+" was not defined On line: "+node.associated.getLinenumber());
                        }
                    }else{
                        String type =node.children.get(i).associated.getTknType();
                        if(type.equals("INT_TYPE")||type.equals("NUM")){
                            type = "int";
                        }else if(type.equals("BOOL_TYPE")||type.equals("BOOL_T")||type.equals("BOOL_F")){
                            type = "boolean";
                        }else if(type.equals("STR_TYPE")||type.equals("EndQuote")){
                            type = "string";
                        }
                        if(!(type.equals("Inequality")||type.equals("Equality")||type.equals("LeftCurlBrace")||type.equals("Addition"))){
                            types.add(type);
                        }
                    }
                }
                if(types.size()>0){
                    String first = types.get(0);
                    for(int i=0; i<types.size(); i++){
                        if(first.equals(types.get(i))){
                            //This is valid
                        }else{
                            //removed because it was throwing the errors twice (somewhere else already does it)
                            //addErrorMsg(hm,node.children.get(i).associated,"TYPE: "+first+" Cannot associate with TYPE: "+types.get(i)+" On line: "+node.associated.getLinenumber());
                        }
                    }
                }
            }else if(node.name.equals("IfStatement")||node.name.equals("WhileStatement")){
                Node child = node.children.get(0);
                if(child.children.size()>1){
                    Node first = child.children.get(0);
                    Node second = child.children.get(1);
                    if(first.name.equals("+")){
                        first = first.children.get(0);
                    }
                    if(second.name.equals("+")){
                        second = second.children.get(0);
                    }
                    String IDregex = "[a-z]";
                    String NUMregex ="[0-9]";
                    boolean equal = false;
                    String firstName = first.name;
                    String secondName = second.name;
                    String firstType = "undeclared Var";
                    String secondType ="undeclared Var";
                    if(firstName.matches(IDregex)){
                        if(hm.containsKey(firstName)){
                            firstType = hm.get(firstName).type;
                        }else if(checkPrevScope(st.current,firstName, false)){
                            firstType = getPrevType(st.current,firstName,"used");
                        }else if(findVar(st.root,first.name,node.scope,first.name,false)){
                            firstType=retType(st.root,first.name,node.scope,first.name,"");
                        }
                    }
                    if(secondName.matches(IDregex)){
                        if(hm.containsKey(secondName)){
                            secondType = hm.get(secondName).type;
                        }else if(checkPrevScope(st.current,secondName, false)){
                            secondType = getPrevType(st.current,secondName,"used");
                        }else if(findVar(st.root,second.name,node.scope,second.name,false)){
                            secondType=retType(st.root,second.name,node.scope,second.name,"");
                        }
                    }
                    if(firstName.equals("true")||firstName.equals("false")){
                        firstType = "boolean";
                    }else if(firstName.matches(NUMregex)){
                        firstType = "int";
                    }else if(first.associated.getTknType().equals("EndQuote")){
                        firstType = "string";
                    }else if(firstType.equals("INT_TYPE")){
                        firstType = "int";
                    }else if(firstType.equals("BOOL_TYPE")||firstType.equals("BOOL_T")||firstType.equals("BOOL_F")){
                        firstType = "boolean";
                    }else if(firstType.equals("STR_TYPE")){
                        firstType = "string";
                    }
                    
                    if(secondName.equals("true")||secondName.equals("false")){
                        secondType = "boolean";
                    }else if(secondName.matches(NUMregex)){
                        secondType = "int";
                    }else if(second.associated.getTknType().equals("EndQuote")){
                        secondType = "string";
                    }else if(secondType.equals("INT_TYPE")){
                        secondType = "int";
                    }else if(secondType.equals("BOOL_TYPE")||secondType.equals("BOOL_T")||secondType.equals("BOOL_F")){
                        secondType = "boolean";
                    }else if(secondType.equals("STR_TYPE")){
                        secondType = "string";
                    }
                    if(firstType.equals(secondType)){
                        //This is valid
                    }else{
                        addErrorMsg(hm,first.associated,"Cannot associate type: "+firstType+" with type: "+secondType+" On line: "+first.associated.getLinenumber());
                    }

                }
            }
            for(int j=0; j<node.children.size(); j++){
                prntTree(node.children.get(j), indent+1, currentscope, tracker, st);//We add one to create separation for its child nodes
            }
        }else{
            if(node.name.equals("Statement")||node.name.equals("CharList")){//If these have no children, they are the espilon case
                node.name ="epsilon";
            }
            spacing+="[" + node.name + "] ";
            String regex = "[a-z]";
            if(node.name.matches(regex)){
                if(node.Parent.name.equals("Var Decl")){
                    //Ignore as this is from the declaration
                }else if(node.Parent.name.equals("Assignment")){
                    Node first = node.Parent.children.get(0);
                    String firstName = first.getName(first);
                    if(findVar(st.root,node.name,st.current.scope,firstName,false)){
                    }else{
                        addErrorMsg(hm, node.associated,node.name+": Has not been declared On line: "+node.associated.getLinenumber());
                    }
                }else if(node.Parent.name.equals("WhileStatement")||node.Parent.name.equals("IfStatement")||node.Parent.name.equals("Assignment")){
                    if(hm.containsKey(node.name)){
                        if(hm.get(node.name).getScope()>currentscope){
                            //The defined scope is unacessable from the current scope
                            hm = addErrorMsg(hm,node.associated,"SCOPE ERROR: "+node.name+" Was defined at scope: "+hm.get(node.name).getScope()+" Cannot access it in the scope: "+currentscope);
                        }else{
                            hm.get(node.name).used = true;
                            getPrevType(st.current,node.name,"used");
                        }
                    }else{
                        if(checkPrevScope(st.current,node.name,false)){
                            getPrevType(st.current,node.name,"used");
                        }else{
                            for(int i=0; i<st.current.children.size(); i++){
                                if(st.current.children.get(i).scope==currentscope){
                                    HashMap<String,SymbolObj> map = st.current.children.get(i).getMap();
                                    if(map.containsKey(node.name)){

                                    }else{
                                        addErrorMsg(hm, node.associated,node.name+": Has not been declared On line: "+node.associated.getLinenumber());
                                    }
                                }
                            }
                        }                        
                    }
                }else if(node.Parent.name.equals("PrintStatement")){
                    String name = "DONT MATCH SHOULD BE USED NOT INIT";
                    int adjScope =st.current.scope;
                    if(adjScope==0){//For some reason it will think the scope is 0 even though that is not a valid scope
                        adjScope = 1;
                    }
                    if(findVar(st.root,node.name,adjScope,name, false)){
                        //System.out.println("USED: "+node.name);
                    }else{
                        addErrorMsg(hm,node.associated,"Var: "+node.name+" has not been declared On line: "+node.associated.getLinenumber());
                    }
                }
            }
            System.out.println(spacing);//This line prints the leaf nodes
        }
        return st;
    }
    public static boolean findVar(ScopeNode current, String val,int scope,String firstName, boolean found){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            //spacing+="<" + node.name + ">";
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String type = hm.get(key).getType();
                if(key.equals(val)&&current.scope<=scope){
                    found = true;
                    if(key.equals(firstName)){
                        hm.get(key).init = true;
                    }else{
                        hm.get(key).used = true;
                    }
                }
            }
            for(int j=0; j<current.children.size(); j++){
                found =findVar(current.children.get(j),val,scope,firstName,found);//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String type = hm.get(key).getType();
                if(key.equals(val)&&current.scope<=scope){
                    found = true;
                    if(key.equals(firstName)){
                        hm.get(key).init = true;
                    }else{
                        hm.get(key).used = true;
                    }
                }
            }
        }
        return found;
    }
    public static String retType(ScopeNode current, String val,int scope,String firstName, String type){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            //spacing+="<" + node.name + ">";
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String t = hm.get(key).getType();
                if(key.equals(val)&&current.scope<=scope){
                    type = t;
                    if(key.equals(firstName)){
                        hm.get(key).init = true;
                    }else{
                        hm.get(key).used = true;
                    }
                }
            }
            for(int j=0; j<current.children.size(); j++){
                type =retType(current.children.get(j),val,scope,firstName,type);//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String t = hm.get(key).getType();
                if(key.equals(val)&&current.scope<=scope){
                    type = t;
                    if(key.equals(firstName)){
                        hm.get(key).init = true;
                    }else{
                        hm.get(key).used = true;
                    }
                }
            }
        }
        return type;
    }
    public static void prntTable(ScopeNode current){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            //spacing+="<" + node.name + ">";
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String type = hm.get(key).getType();
                if(type.equals("boolean")){//Shorten to fit inside the tab for symbol table
                    type = "bool";
                }else if(type.equals("INT_TYPE")){
                    type = "int";
                }else if(type.equals("BOOL_TYPE")){
                    type = "bool";
                }else if(type.equals("STR_TYPE")){
                    type = "string";
                }
                System.out.println(key+"\t " + type+"\t "+ + hm.get(key).getScope()+"\t "+hm.get(key).associated.getLinenumber());
            }
            for(int j=0; j<current.children.size(); j++){
                prntTable(current.children.get(j));//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            for (String key: hm.keySet()) {
                String type = hm.get(key).getType();
                if(type.equals("boolean")){//Shorten to fit inside the tab for symbol table
                    type = "bool";
                }else if(type.equals("INT_TYPE")){
                    type = "int";
                }else if(type.equals("BOOL_TYPE")){
                    type = "bool";
                }else if(type.equals("STR_TYPE")){
                    type = "string";
                }
                System.out.println(key+"\t " + type+"\t "+ + hm.get(key).getScope()+"\t "+hm.get(key).associated.getLinenumber());
            }
        }
    }
    public static void prntWarnings(ScopeNode current){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String key: hm.keySet()){
                boolean init = hm.get(key).getInit();
                boolean used = hm.get(key).getUsed();
                if(init==false&&used==false){
                    System.out.println("WARNING: "+key+" Is neither initalized or used On line: "+hm.get(key).associated.getLinenumber());
                }else if(init==true&&used==false){
                    System.out.println("WARNING: "+key+" Was initalized but not used On line: "+hm.get(key).associated.getLinenumber());
                }else if(init==false&&used==true){
                    System.out.println("WARNING: "+key+" Was used without being initalized On line: "+hm.get(key).associated.getLinenumber());
                }
            }
            for(int j=0; j<current.children.size(); j++){
                prntWarnings(current.children.get(j));//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String key: hm.keySet()){
                boolean init = hm.get(key).getInit();
                boolean used = hm.get(key).getUsed();
                if(init==false&&used==false){
                    System.out.println("WARNING: "+key+" Is neither initalized or used On line: "+hm.get(key).associated.getLinenumber());
                }else if(init==true&&used==false){
                    System.out.println("WARNING: "+key+" Was initalized but not used On line: "+hm.get(key).associated.getLinenumber());
                }else if(init==false&&used==true){
                    System.out.println("WARNING: "+key+" Was used without being initalized On line: "+hm.get(key).associated.getLinenumber());
                }
            }
        }
    }
    public static boolean prntErr(ScopeNode current, boolean founderr){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            HashMap<String,SymbolObj> hm = current.getMap();
            if(hm.containsKey("__ERROR__")){
                founderr = true;
                ArrayList<String> ers =hm.get("__ERROR__").getErrors();
                for(int i=0; i<ers.size(); i++){
                    System.out.println(ers.get(i));
                }
            }
            for(int j=0; j<current.children.size(); j++){
                founderr =prntErr(current.children.get(j), founderr);//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            if(hm.containsKey("__ERROR__")){
                founderr = true;
                ArrayList<String> ers =hm.get("__ERROR__").getErrors();
                for(int i=0; i<ers.size(); i++){
                    System.out.println(ers.get(i));
                }
            }
        }
        return founderr;
    }
    public static String getPrevType(ScopeNode current, String key,String from){
        String prevtype="";
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String h: hm.keySet()){
                if(h.equals(key)){
                    prevtype = hm.get(h).getType();
                    if(from.equals("init")){
                        hm.get(h).init = true;
                    }else{
                        hm.get(h).used = true; 
                    }
                    
                    return prevtype;
                }
            }
            if(hm.containsKey(key)){
                prevtype = hm.get(key).getType();
                if(from.equals("init")){
                    hm.get(key).init = true; 
                }else{
                    hm.get(key).used = true;
                }
            }
            for(int j=0; j<current.children.size(); j++){
                prevtype =getPrevType(current.children.get(j), key,from);//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            if(hm.containsKey(key)){
                prevtype = hm.get(key).getType();
                if(from.equals("init")){
                    hm.get(key).init = true; 
                }else{
                    hm.get(key).used = true;
                }
                
            }
        }
        return prevtype;
    }
    public static boolean checkPrevScope(ScopeNode current, String key,boolean found){
        if(current.children.size() > 0){//Branches have children, leaf nodes do not
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String k: hm.keySet()){
                if(k.equals(key)){
                    found=true;
                }
            }
            for(int j=0; j<current.children.size(); j++){
                found =checkPrevScope(current.children.get(j),key, found);//We add one to create separation for its child nodes
            }
        }else{
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String k: hm.keySet()){
                if(k.equals(key)){
                    found=true;
                }
            }
        }
        return found;
    }
    public static boolean chkprev(ScopeNode current, String key, boolean found, String type){
        while(current.Parent!=null){
            HashMap<String,SymbolObj> hm = current.getMap();
            for(String k: hm.keySet()){
                if(k.equals(key)){
                    found=true;
                    if(type.equals("init")){
                        hm.get(k).init = true;
                    }else if(type.equals("used")){
                        hm.get(k).used = true;
                    }
                }
            }
            current = current.Parent;
        }
        return found;
    }
    public static HashMap<String, SymbolObj> addErrorMsg(HashMap<String,SymbolObj> hm, Token tkn, String msg){
        if(hm.containsKey("__ERROR__")){
            hm.get("__ERROR__").errors.add(msg);
        }else{
            SymbolObj er = new SymbolObj("__ERROR__",false,false,0);
            er.associated = tkn;
            er.errors.add(msg);
            hm.put("__ERROR__", er);
        }
        return hm;
    }
}

import java.util.ArrayList;

public class SemanticAnalysis {
    public static Tree Semantic_Analysis(ArrayList<Token> tokenStream, int prognum) {
        Tree tree = new Tree();
        prnt("Parsing Program: " + prognum,"INFO");
        prnt("parse()","DEBUG");
        ArrayList<Token> temp = tokenStream;// This way we dont detroy the origin tokenStream
        boolean valid = true;
        ParseProgram(temp, tree, valid);

        if (validtest(temp)) {
            prnt("Parse Completed with: 0 Errors","INFO");
            prntTree(tree.root, 0);
        } else {
            System.out.println("------------");
            System.out.println("PARSE FAILED");
            System.out.println("------------");
            //prnt("---Parse Errors, Dont create CST---","INFO");
            tree.getRoot(tree).name ="__ERROR__";
        }
        return tree;
    }

    public static ArrayList<Token> ParseProgram(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseProgram()","DEBUG");
        tree.addNode(tknStream.get(0), "root", "program");//Program Root
        ParseBlock(tknStream, tree, valid);
        valid = validtest(tknStream);
        if (valid) {
            Match("$", tknStream, tree, valid);
        }else{
            prnt("Invalid in ParseProgram","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseBlock(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseBlock()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "Block");//Each new block is a branch
        Match("{", tknStream, tree, valid);
        ParseStatementList(tknStream, tree, valid);
        Match("}", tknStream, tree, valid);
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

    public static ArrayList<Token> ParseStatementList(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStatementList()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "StatementList");//Each new StatementList is a branch
        ParseStatement(tknStream, tree, valid);
        if (!isValidStatement(tknStream)) {
            //tree.moveUp("StmtList");
            return tknStream;
        } else {
            ParseStatementList(tknStream, tree, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STATEMENT LIST","ERROR");
        }
        //tree.moveUp("StmtList");
        return tknStream;

    }

    public static ArrayList<Token> ParseStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStatement()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "Statement");//Each new Statement is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("PRNT_STMT")) {// Print Statement
            ParsePrintStatement(tknStream, tree, valid);
        } else if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            tree.addNode(tknStream.get(0), "branch", "Var Decl");
            TypeCheck(tknStream, type, tree, valid);
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
            Match("ID", tknStream, tree, valid);
            tree.moveUp("VarDecl");
        } else if (type.equals("ID")) {// Assignment Statement
            tree.addNode(tknStream.get(0), "branch", "Assignment");
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));//a = 1 + a
            Match("ID", tknStream, tree, valid);
            Match("Assignment", tknStream, tree, valid);
            ParseExpression(tknStream, tree, valid,"Assignment");
        } else if (type.equals("WHILE_STMT")) {// While Statement
            ParseWhileStatement(tknStream, tree, valid);
        } else if (type.equals("IF_STMT")) {// If Statement
            ParseIfStatement(tknStream, tree, valid);
        } else if (type.equals("LeftCurlBrace")) {// Block
            ParseBlock(tknStream, tree, valid);
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

    public static ArrayList<Token> ParseExpression(ArrayList<Token> tknStream, Tree tree, boolean valid, String from) {
        prnt("parseExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "Expression");//Each new Expression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        try {
            if(next.getCharacter().equals("==")||next.getCharacter().equals("!=")){
                //tree.addNode(tknStream.get(0), "branch", String.valueOf(tknStream.get(0).getCharacter()));
                tknStream.remove(0);
                next = tknStream.get(0);
                type = next.getTknType();
                if(tknStream.get(1).getTknType().equals("Addition")){
                    //Dont want to print before op
                }else if(type.equals("NUM")){
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()));
                    System.out.println("ADDED NODE");
                    System.out.println(tknStream.get(0).getNumber());
                }else{
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
                }
            }  
        } catch (Exception e) {
            //TODO: handle exception
        }
        //1 + a
        System.out.println(type);
        if (type.equals("NUM")) {// IntExpr
            ParseIntExpr(tknStream, tree, valid);
            tree.moveUp("IntExpr");
        } else if (type.equals("BeginningQuote")) {// StringExpr (Starts with a ")
            ParseStringExpression(tknStream, tree, valid);
        } else if (type.equals("LeftParen")||type.equals("BOOL_T")||type.equals("BOOL_F")) { // BoolExpr
            ParseBooleanExpression(tknStream, tree, valid);
            if(from.equals("Assignment")||from.equals("ParseBool")){
                tree.moveUp("BoolExpr");
            }
        } else if (type.equals("ID")) {// MatchID
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
            Match("ID", tknStream, tree, valid);
            if(from.equals("Assignment")||from.equals("ParseBool")){
                tree.moveUp("Assignment");
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

    public static ArrayList<Token> ParseIntExpr(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseIntExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "IntExpression");//Each new IntExpression is a branch
        //1 + a
        Token next = tknStream.get(0);
        String type = next.getTknType();
        System.out.println(next.getCharacter()+"----");
        System.out.println(next.getNumber());
        System.out.println(type);

        if(type.equals("NUM")){
            boolean exists=false;
            int sync = 1;
            int count=0;
            while(tknStream.get(sync).getCharacter().equals("+")){
                count++;
                exists=true;
                System.out.println(sync);
                tree.addNode(tknStream.get(sync), "branch", tknStream.get(sync).getCharacter());
                System.out.println(tknStream.get(1).getCharacter());
                
                if(sync==1){
                    if(tknStream.get(0).getTknType().equals("ID")){
                        tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
                    }else{
                        tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()));  
                    }
                   Match("NUM", tknStream, tree, valid); 
                }
                Match("+", tknStream, tree, valid);
                //tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
                if(tknStream.get(0).getTknType().equals("ID")){
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
                    Match("ID", tknStream, tree, valid);
                }else{
                    tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()));
                    Match("NUM", tknStream, tree, valid);  
                }
                
                sync=0;
            }
            if(exists==false){
                Match("NUM", tknStream, tree, valid);
            }
            for(int i=0; i<count; i++){
                tree.moveUp("Addition");
            }
        }

        // if (type.equals("NUM")) {// Match Digit (Num)
        //     tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getNumber()));
        //     Match("NUM", tknStream, tree, valid);
        //     next = tknStream.get(0);
        //     type = next.getTknType();
        //     System.out.println(String.valueOf(tknStream.get(0).getCharacter()));
        //     if (tknStream.size() > 1 && type.equals("Addition")) {// If next is +
        //         tree.addNode(tknStream.get(0), "branch", tknStream.get(0).getCharacter());
        //         Match("+", tknStream, tree, valid);// Match(+)
        //         ParseExpression(tknStream, tree, valid,"IntExpr"); // ParseExpr
        //     } else {
        //         // else do nothing (epsilon)
        //     }
        // }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE INT EXPRESSION","ERROR");
        }
        //tree.moveUp("INTEXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseStringExpression(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStringExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "StringExpression");//Each new StringExpression is a branch
        Match("BeginningQuote", tknStream, tree, valid);
        ParseCharList(tknStream, tree, valid);
        Match("EndQuote", tknStream, tree, valid);
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STRING EXPRESSION","ERROR");
        }
        //tree.moveUp("STREXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParsePrintStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parsePrintStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "PrintStatement");//Each new PrintStatement is a branch
        Match("print", tknStream, tree, valid);
        Match("(", tknStream, tree, valid);
        ParseExpression(tknStream, tree, valid,"PrintStmt");
        Match(")", tknStream, tree, valid);
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE PRINT STATEMENT","ERROR");
        }
        tree.moveUp("PRNTSTMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseWhileStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseWhileStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "WhileStatement");//Each new WhileStatement is a branch
        Match("while", tknStream, tree, valid);
        String type = tknStream.get(0).getTknType();
        int inc=0;
        while(!(type.equals("Equality")||type.equals("Inequality"))){
            type=tknStream.get(inc).getTknType();
            System.out.println(type);
            inc++;
        }
        tree.addNode(tknStream.get(inc-1), "branch", String.valueOf(tknStream.get(inc-1).getCharacter()));
        ParseBooleanExpression(tknStream, tree, valid);
        ParseBlock(tknStream, tree, valid);
        //tree.moveUp("WHILESTMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseBooleanExpression(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseBooleanExpression()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "BooleanExpression");//Each new BooleanExpression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("LeftParen")) {
            Match("(", tknStream, tree, valid);
            System.out.println(tknStream.get(0).getCharacter());
            if(!(tknStream.get(0).getCharacter()==null)){
                if(tknStream.get(0).getCharacter().equals("\"")){
                    int inc=1;
                    while(!tknStream.get(inc).getCharacter().equals("\"")){
                        System.out.println(tknStream.get(inc).getCharacter()+"HERE");
                        inc++;
                    }
                    System.out.println("ADD NODE: "+tknStream.get(inc+1).getCharacter());
                    tree.addNode(tknStream.get(inc+1), "branch", tknStream.get(inc+1).getCharacter());
                    tknStream.remove(inc+1);
                }
            }
            System.out.println("--------------------------------");
            System.out.println(tknStream.get(0).getTknType());
            System.out.println(tknStream.get(1).getTknType());
            System.out.println(tknStream.get(2).getTknType());
            System.out.println("--------------------------------");
            System.out.println(tknStream.get(0).getCharacter());
            System.out.println(tknStream.get(1).getCharacter());
            ParseExpression(tknStream, tree, valid,"ParseBool1");//We dont want to move up (the tree) on the first of these
            ParseExpression(tknStream, tree, valid,"ParseBool");
            Match(")", tknStream, tree, valid);
        }else if(type.equals("BOOL_T")){
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
            Match("true", tknStream, tree, valid);
            //tree.moveUp("BOOL_T");
        }else if(type.equals("BOOL_F")){
            tree.addNode(tknStream.get(0), "leaf", String.valueOf(tknStream.get(0).getCharacter()));
            Match("false", tknStream, tree, valid);
            //tree.moveUp("BOOL_F");
        }else{
            prnt("EXPECTED: BooleanExpression GOT: "+next.getCharacter(),"ERROR");
            tknStream =emptyStream(tknStream);
        }
        //tree.moveUp("BOOLEXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseIfStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseIfStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "IfStatement");//Each new IfStatement is a branch
        Match("if", tknStream, tree, valid);
        String type = tknStream.get(0).getTknType();
        int inc=0;
        while(!(type.equals("Equality")||type.equals("Inequality"))){
            type=tknStream.get(inc).getTknType();
            System.out.println(type);
            inc++;
        }
        tree.addNode(tknStream.get(inc-1), "branch", String.valueOf(tknStream.get(inc-1).getCharacter()));
        ParseBooleanExpression(tknStream, tree, valid);
        ParseBlock(tknStream, tree, valid);
        //tree.moveUp("IFSTMT");
        return tknStream;
    }

    public static ArrayList<Token> TypeCheck(ArrayList<Token> tknStream, String type, Tree tree, boolean valid) {
        if (type.equals("INT_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "int");
            Match("int", tknStream, tree, valid);
        } else if (type.equals("STR_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "string");
            Match("string", tknStream, tree, valid);
        } else if (type.equals("BOOL_TYPE")) {
            tree.addNode(tknStream.get(0), "leaf", "boolean");
            Match("boolean", tknStream, tree, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN TYPE CHECK","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseCharList(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseCharList()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "CharList");//Each new CharList is a branch
        Token current = tknStream.get(0);
        String type = current.getTknType();
        String full="";
        while (!current.getCharacter().equals("\"")) {
            full+=current.getCharacter();
            Match("CHAR", tknStream, tree, valid);
            current = tknStream.get(0);
        }
        tree.addNode(tknStream.get(0), "leaf",'"'+full+'"');//Each new CharList is a leaf
        tree.moveUp("CharList");
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE CHAR LIST","ERROR");
        }
        //tree.moveUp("CHARLIST");
        return tknStream;
    }

    public static ArrayList<Token> ParseBoolOp(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseBoolOp()","DEBUG");
        //tree.addNode(tknStream.get(0), "branch", "BoolOp");//Each new BoolOp is a branch
        Token next = tknStream.get(0);
        if (next.tokenType.equals("Inequality")) {// Inequality
            tree.addNode(tknStream.get(0), "branch", String.valueOf(tknStream.get(0).getCharacter()));
            Match("!=", tknStream, tree, valid);
        } else if (next.tokenType.equals("Equality")) {// Equality
            tree.addNode(tknStream.get(0), "branch", String.valueOf(tknStream.get(0).getCharacter()));
            Match("==", tknStream, tree, valid);
        }
        tree.moveUp("BOOLOP");
        return tknStream;
    }

    public static ArrayList<Token> Match(String character, ArrayList<Token> tknStream, Tree tree, boolean valid) {
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
            for(int i=0; i<tknStream.size(); i++){
                //System.out.println(tknStream.get(i).getCharacter());
            }
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
            System.out.println("DEBUG Parser: " + method);
        }else if(type.equals("ERROR")){
            System.out.println("ERROR Parser: " + method);
        }else if(type.equals("INFO")){
            System.out.println("INFO Parser: " + method);
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
    public static void prntTree(Node node, int indent){
        String spacing = "";
        for(int i=0;i<indent; i++){
            spacing+="-";
        }         
        if(node.children.size() > 0){//Branches have children, leaf nodes do not
            spacing+="<" + node.name + ">";
            System.out.println(spacing);//This line prints the branches
            for(int j=0; j<node.children.size(); j++){
                prntTree(node.children.get(j), indent+1);//We add one to create separation for its child nodes
            }
        }else{
            if(node.name.equals("Statement")||node.name.equals("CharList")){//If these have no children, they are the espilon case
                node.name ="epsilon";
            }
            spacing+="[" + node.name + "] ";
            System.out.println(spacing);//This line prints the leaf nodes
        }
    }
}

import java.util.ArrayList;

public class Parser {
    public static Tree Parse(ArrayList<Token> tokenStream, int prognum) {
        Tree tree = new Tree();
        prnt("Parsing Program: " + prognum,"INFO");
        prnt("parse()","DEBUG");
        ArrayList<Token> temp = tokenStream;// This way we dont detroy the origin tokenStream
        boolean valid = true;
        ParseProgram(temp, tree, valid);
        if (validtest(temp)) {
            System.out.println("------------");
            System.out.println("PARSE SUCCESS");
            System.out.println("PRINTING CST");
            System.out.println("------------");
            prntTree(tree.root, 0);
        } else {
            System.out.println("------------");
            System.out.println("PARSE FAILED");
            System.out.println("------------");
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
        tree.moveUp("Block");
        return tknStream;
    }

    public static ArrayList<Token> ParseStatementList(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStatementList()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "StatementList");//Each new StatementList is a branch
        ParseStatement(tknStream, tree, valid);
        if (!isValidStatement(tknStream)) {
            tree.moveUp("StmtList");
            return tknStream;
        } else {
            ParseStatementList(tknStream, tree, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STATEMENT LIST","ERROR");
        }
        tree.moveUp("StmtList");
        return tknStream;

    }

    public static ArrayList<Token> ParseStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "Statement");//Each new Statement is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("PRNT_STMT")) {// Print Statement
            ParsePrintStatement(tknStream, tree, valid);
        } else if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            TypeCheck(tknStream, type, tree, valid);
            Match("ID", tknStream, tree, valid);
        } else if (type.equals("ID")) {// Assignment Statement
            Match("ID", tknStream, tree, valid);
            Match("Assignment", tknStream, tree, valid);
            ParseExpression(tknStream, tree, valid);
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
        tree.moveUp("STMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseExpression(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseExpression()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "Expression");//Each new Expression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("NUM")) {// IntExpr
            ParseIntExpr(tknStream, tree, valid);
        } else if (type.equals("BeginningQuote")) {// StringExpr (Starts with a ")
            ParseStringExpression(tknStream, tree, valid);
        } else if (type.equals("LeftParen")||type.equals("BOOL_T")||type.equals("BOOL_F")) { // BoolExpr
            ParseBooleanExpression(tknStream, tree, valid);
        } else if (type.equals("ID")) {// MatchID
            Match("ID", tknStream, tree, valid);
        }else{
            prnt("INVALID IN EXPRESSION GOT: "+next.getCharacter(),"ERROR");
            emptyStream(tknStream);
        }

        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE EXPRESSION","ERROR");
        }
        tree.moveUp("EXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseIntExpr(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseIntExpression()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "IntExpression");//Each new IntExpression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("NUM")) {// Match Digit (Num)
            Match("NUM", tknStream, tree, valid);
            next = tknStream.get(0);
            type = next.getTknType();
            //System.out.println(type);
            if (tknStream.size() > 1 && type.equals("Addition")) {// If next is +
                Match("+", tknStream, tree, valid);// Match(+)
                ParseExpression(tknStream, tree, valid); // ParseExpr
            } else {
                // else do nothing (epsilon)
            }
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE INT EXPRESSION","ERROR");
        }
        tree.moveUp("INTEXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseStringExpression(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseStringExpression()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "StringExpression");//Each new StringExpression is a branch
        Match("BeginningQuote", tknStream, tree, valid);
        ParseCharList(tknStream, tree, valid);
        Match("EndQuote", tknStream, tree, valid);
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STRING EXPRESSION","ERROR");
        }
        tree.moveUp("STREXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParsePrintStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parsePrintStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "PrintStatement");//Each new PrintStatement is a branch
        Match("print", tknStream, tree, valid);
        Match("(", tknStream, tree, valid);
        ParseExpression(tknStream, tree, valid);
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
        ParseBooleanExpression(tknStream, tree, valid);
        ParseBlock(tknStream, tree, valid);
        tree.moveUp("WHILESTMT");
        return tknStream;
    }

    public static ArrayList<Token> ParseBooleanExpression(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseBooleanExpression()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "BooleanExpression");//Each new BooleanExpression is a branch
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("LeftParen")) {
            Match("(", tknStream, tree, valid);
            ParseExpression(tknStream, tree, valid);
            ParseBoolOp(tknStream, tree, valid);
            ParseExpression(tknStream, tree, valid);
            Match(")", tknStream, tree, valid);
        }else if(type.equals("BOOL_T")){
            Match("true", tknStream, tree, valid);
        }else if(type.equals("BOOL_F")){
            Match("false", tknStream, tree, valid);
        }else{
            prnt("EXPECTED: BooleanExpression GOT: "+next.getCharacter(),"ERROR");
            tknStream =emptyStream(tknStream);
        }
        tree.moveUp("BOOLEXPR");
        return tknStream;
    }

    public static ArrayList<Token> ParseIfStatement(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseIfStatement()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "IfStatement");//Each new IfStatement is a branch
        Match("if", tknStream, tree, valid);
        ParseBooleanExpression(tknStream, tree, valid);
        ParseBlock(tknStream, tree, valid);
        tree.moveUp("IFSTMT");
        return tknStream;
    }

    public static ArrayList<Token> TypeCheck(ArrayList<Token> tknStream, String type, Tree tree, boolean valid) {
        if (type.equals("INT_TYPE")) {
            Match("int", tknStream, tree, valid);
        } else if (type.equals("STR_TYPE")) {
            Match("string", tknStream, tree, valid);
        } else if (type.equals("BOOL_TYPE")) {
            Match("boolean", tknStream, tree, valid);
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN TYPE CHECK","ERROR");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseCharList(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseCharList()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "CharList");//Each new CharList is a branch
        Token current = tknStream.get(0);
        String type = current.getTknType();
        while (!current.getCharacter().equals("\"")) {
            Match("CHAR", tknStream, tree, valid);
            current = tknStream.get(0);
        }
        /**
         * 
         * 
         * ::== char CharList ::== space CharList ::== ε
         */
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE CHAR LIST","ERROR");
        }
        tree.moveUp("CHARLIST");
        return tknStream;
    }

    public static ArrayList<Token> ParseBoolOp(ArrayList<Token> tknStream, Tree tree, boolean valid) {
        prnt("parseBoolOp()","DEBUG");
        tree.addNode(tknStream.get(0), "branch", "BoolOp");//Each new BoolOp is a branch
        Token next = tknStream.get(0);
        if (next.tokenType.equals("Inequality")) {// Inequality
            Match("!=", tknStream, tree, valid);
        } else if (next.tokenType.equals("Equality")) {// Equality
            Match("==", tknStream, tree, valid);
        }
        tree.moveUp("BOOLOP");
        return tknStream;
    }

    public static ArrayList<Token> Match(String character, ArrayList<Token> tknStream, Tree tree, boolean valid) {
        if (validtest(tknStream)) {
            Token next = tknStream.get(0);
            // System.out.println();
            // System.out.println("TKNTYPE: "+next.getTknType());
            // System.out.println("TKN CHARACTER: "+next.getCharacter());
            // System.out.println("TKN NUM: "+next.getNumber());
            // System.out.println("PASSED CHARACTER: "+character);
            // System.out.println();
            if (next.tokenType.equals("NUM")) {
                String regex = "[0-9]";
                int num = next.getNumber();
                if (String.valueOf(num).matches(regex)&&character.equals("NUM")) {
                    prnt(character + " Matched: " + next.getNumber() + " - (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","DEBUG");
                    tree.addNode(tknStream.get(0), "leaf", character+" , "+num);//If it matched Add it as a leaf node
                    //tree.moveUp("MATCH-Num");
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: "+character+" GOT: [0-9] AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }
            } else if (character.equals("ID") || character.equals("CHAR")) {
                if(isValidStringChar(next.getCharacter())){
                    prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                }else{
                    valid = false;
                    prnt("INVALID - EXPECTED: [a-z| ] GOT: "+next.getCharacter()+" AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }
                // System.out.println(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")");
                // tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                // //tree.moveUp("Match-ID/Char");                
            } else if (character.equals(next.getCharacter())) {//Matches exact characters
                prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                tree.addNode(tknStream.get(0), "leaf", character);//If it matched Add it as a leaf node
                //tree.moveUp("Match: "+character);
                if(character.equals("$")){
                    //tree.moveUp("Match: "+character);
                }else{
                    //tree.moveUp("Match: "+character);
                }
            } else if (character.equals("Assignment")) {
                if (next.getCharacter().equals("=")) {
                    prnt(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                    //tree.moveUp("Match: Assn (=)");
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [=] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }
            } else if (character.equals("BeginningQuote") || character.equals("EndQuote")) {
                if (next.character.equals("\"")) {
                    prnt(character + "Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")","DEBUG");
                    tree.addNode(tknStream.get(0), "leaf", character+" , "+next.getCharacter());//If it matched Add it as a leaf node
                    //tree.moveUp("Match Quote");
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [\"] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")","ERROR");
                    emptyStream(tknStream);
                }

            } else if (next.tokenType.equals("__ERROR__")) {
                //System.out.println("ERROR IN MATCH");
                valid = false;
                tknStream.get(0).valid = false;
                return tknStream;
            } else {
                prnt("INVALID - EXPECTED: [" + character + "] GOT: " + next.getCharacter() + " AT ("+ next.getLinenumber() + ":" + next.getLineposition() + ")","ERROR");
                valid = false;
                emptyStream(tknStream);
            }
        } else {
            //System.out.println("ERRORS DONT MATCH");
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
        /*
         * Match("BeginningQuote", tknStream); //CharList Match("EndQuote", tknStream);
         */
        if (valid == false) {
            tknStream.get(0).valid = false;
        }
        //tree.moveUp("Match");
        //System.out.println("HERE"+tknStream.size());
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

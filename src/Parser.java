import java.util.ArrayList;

public class Parser {
    public static ArrayList<Token> Parse(ArrayList<Token> tokenStream, int prognum) {
        System.out.println("Parsing Program: " + prognum);
        prnt("parse()");
        // System.out.println(tokenStream);
        ArrayList<Token> temp = tokenStream;// This way we dont detroy the origin tokenStream
        boolean valid = true;
        ParseProgram(temp, valid);
        System.out.println("-------");
        if (validtest(temp)) {
            System.out.println("PARSE SUCCESS");
        } else {
            System.out.println("PARSE FAILED");
        }
        System.out.println("-------");
        return temp;
    }

    public static ArrayList<Token> ParseProgram(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseProgram()");
        ParseBlock(tknStream, valid);
        if (validtest(tknStream)) {
            Match("$", tknStream, valid);
        }
        if (!validtest(tknStream)) {
            System.out.println("Invalid in ParseProgram");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseBlock(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseBlock()");
        Match("{", tknStream, valid);
        ParseStatementList(tknStream, valid);
        // System.out.println("MADE IT OUT");
        Match("}", tknStream, valid);
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSEBLOCK");
        }
        //System.out.println(tknStream.size());
        return tknStream;
    }

    public static ArrayList<Token> ParseStatementList(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseStatementList()");
        ParseStatement(tknStream, valid);
        if (tknStream.size() > 1) {
            if (!isValidStatement(tknStream)) {
                return tknStream;
            } else {
                ParseStatementList(tknStream, valid);
            }

        }
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE STATEMENT LIST");
        }
        return tknStream;

    }

    public static ArrayList<Token> ParseStatement(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseStatement()");
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("PRNT_STMT")) {// Print Statement
            ParsePrintStatement(tknStream, valid);
        } else if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            TypeCheck(tknStream, type, valid);
            Match("ID", tknStream, valid);
        } else if (type.equals("ID")) {// Assignment Statement
            Match("ID", tknStream, valid);
            Match("Assignment", tknStream, valid);
            ParseExpression(tknStream, valid);
        } else if (type.equals("WHILE_STMT")) {// While Statement
            ParseWhileStatement(tknStream, valid);
        } else if (type.equals("IF_STMT")) {// If Statement
            ParseIfStatement(tknStream, valid);
        } else if (type.equals("LeftCurlBrace")) {// Block
            ParseBlock(tknStream, valid);
        } else if (type.equals("RightCurlBrace")) {
            // System.out.println("Empty Statement List");
            return tknStream;
        } else {
            prnt("TYPE: " + type + " Invalid for STATEMENT");
            tknStream = emptyStream(tknStream);// Removes contents and adds an Error Token
        }
        if (!validtest(tknStream)) {
            prnt("INVALID IN PARSE STATEMENT");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseExpression(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseExpression()");
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("NUM")) {// IntExpr
            ParseIntExpr(tknStream, valid);
        } else if (type.equals("BeginningQuote")) {// StringExpr (Starts with a ")
            ParseStringExpression(tknStream, valid);
        } else if (type.equals("LeftParen")||type.equals("BOOL_T")||type.equals("BOOL_F")) { // BoolExpr
            ParseBooleanExpression(tknStream, valid);
        } else if (type.equals("ID")) {// MatchID
            Match("ID", tknStream, valid);
        }

        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE EXPRESSION");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseIntExpr(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseIntExpression()");
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("NUM")) {// Match Digit (Num)
            Match("NUM", tknStream, valid);
            next = tknStream.get(0);
            type = next.getTknType();
            //System.out.println(type);
            if (tknStream.size() > 1 && type.equals("Addition")) {// If next is +
                Match("Addition", tknStream, valid);// Match(+)
                ParseExpression(tknStream, valid); // ParseExpr
            } else {
                // else do nothing (epsilon)
            }
        }
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE INT EXPRESSION");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseStringExpression(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseStringExpression()");
        Match("BeginningQuote", tknStream, valid);
        ParseCharList(tknStream, valid);
        Match("EndQuote", tknStream, valid);
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE STRING EXPRESSION");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParsePrintStatement(ArrayList<Token> tknStream, boolean valid) {
        prnt("parsePrintStatement()");
        Match("print", tknStream, valid);
        Match("(", tknStream, valid);
        ParseExpression(tknStream, valid);
        Match(")", tknStream, valid);
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE PRINT STATEMENT");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseWhileStatement(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseWhileStatement()");
        Match("while", tknStream, valid);
        ParseBooleanExpression(tknStream, valid);
        ParseBlock(tknStream, valid);
        return tknStream;
    }

    public static ArrayList<Token> ParseBooleanExpression(ArrayList<Token> tknStream, boolean valid) {
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("LeftParen")) {
            Match("(", tknStream, valid);
            ParseExpression(tknStream, valid);
            ParseBoolOp(tknStream, valid);
            ParseExpression(tknStream, valid);
            Match(")", tknStream, valid);
        }else if(type.equals("BOOL_T")){
            Match("true", tknStream, valid);
        }else if(type.equals("BOOL_F")){
            Match("false", tknStream, valid);
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseIfStatement(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseIfStatement()");
        Match("if", tknStream, valid);
        ParseBooleanExpression(tknStream, valid);
        ParseBlock(tknStream, valid);
        return tknStream;
    }

    public static ArrayList<Token> TypeCheck(ArrayList<Token> tknStream, String type, boolean valid) {
        if (type.equals("INT_TYPE")) {
            Match("int", tknStream, valid);
        } else if (type.equals("STR_TYPE")) {
            Match("string", tknStream, valid);
        } else if (type.equals("BOOL_TYPE")) {
            Match("boolean", tknStream, valid);
        }
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN TYPE CHECK");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseCharList(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseCharList()");
        Token current = tknStream.get(0);
        String type = current.getTknType();
        while (!current.getCharacter().equals("\"")) {
            Match("CHAR", tknStream, valid);
            current = tknStream.get(0);
        }
        /**
         * 
         * 
         * ::== char CharList ::== space CharList ::== ε
         */
        if (!validtest(tknStream)) {
            System.out.println("INVALID IN PARSE CHAR LIST");
        }
        return tknStream;
    }

    public static ArrayList<Token> ParseBoolOp(ArrayList<Token> tknStream, boolean valid) {
        prnt("parseBoolOp()");
        Token next = tknStream.get(0);
        if (next.tokenType.equals("Inequality")) {// Inequality
            Match("!=", tknStream, valid);
        } else if (next.tokenType.equals("Equality")) {// Equality
            Match("==", tknStream, valid);
        }
        return tknStream;
    }

    public static ArrayList<Token> Match(String character, ArrayList<Token> tknStream, boolean valid) {
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
                    System.out.println(character + " Matched: " + next.getNumber() + " - (" + next.getLinenumber() + ":"+ next.getLineposition() + ")");
                    // Matched character
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: "+character+" GOT: [0-9] AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")");
                    emptyStream(tknStream);
                }
            } else if (character.equals("ID") || character.equals("CHAR")) {
                String regex = "[a-z]";
                //System.out.println(character + " " + next.getCharacter());
                if (next.getCharacter().matches(regex)) {
                    System.out.println(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")");
                    // Matched Character
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [a-z] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")");
                    emptyStream(tknStream);
                }
            } else if (character.equals(next.getCharacter())) {//Matches exact characters
                System.out.println(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")");
            } else if (character.equals("Assignment")) {
                if (next.getCharacter().equals("=")) {
                    System.out.println(character + " Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")");
                    // Matched
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [=] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")");
                    emptyStream(tknStream);
                }
            } else if (character.equals("BeginningQuote") || character.equals("EndQuote")) {
                if (next.character.equals("\"")) {
                    System.out.println(character + "Matched: " + next.getCharacter() + " - (" + next.getLinenumber()+ ":" + next.getLineposition() + ")");
                    // Matched
                } else {
                    valid = false;
                    prnt("INVALID - EXPECTED: [\"] GOT: " + next.getCharacter() + " AT (" + next.getLinenumber() + ":"+ next.getLineposition() + ")");
                    emptyStream(tknStream);
                }

            } else if (next.tokenType.equals("__ERROR__")) {
                //System.out.println("ERROR IN MATCH");
                valid = false;
                tknStream.get(0).valid = false;
                return tknStream;
            } else {
                prnt("INVALID - EXPECTED: [" + character + "] GOT: " + next.getCharacter() + " AT ("+ next.getLinenumber() + ":" + next.getLineposition() + ")");
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
        return tknStream;
    }

    public static boolean isValidStatement(ArrayList<Token> tknStream) {
        boolean valid = false;
        Token next = tknStream.get(0);
        String type = next.getTknType();
        if (type.equals("INT_TYPE") || type.equals("STR_TYPE") || type.equals("BOOL_TYPE")) {// Var DECL
            valid = true;
        } else if (type.equals("ID")) {// Assignment Statement
            valid = true;
        }
        return valid;
    }

    public static ArrayList<Token> emptyStream(ArrayList<Token> tknStream) {
        int linenum = tknStream.get(0).getLinenumber();
        int linepos = tknStream.get(0).getLineposition();
        for (int i = 0; i < tknStream.size(); i++) {// Once parse is invalid. We should empty tknStream to exit the parse
            tknStream.remove(i);
        }
        Token tkn = new Token(linenum, linepos, 0, 0, "__ERROR__");//We add this so we know there was an error
        tknStream.add(tkn);
        return tknStream;
    }

    public static boolean validtest(ArrayList<Token> tknStream) {
        boolean valid = tknStream.get(0).getValidStatus();
        return valid;
    }

    public static void prnt(String method) {
        System.out.println("PARSER: " + method);
    }
}

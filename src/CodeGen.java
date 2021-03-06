import java.util.ArrayList;
import java.util.HashMap;

public class CodeGen {
    static ScopeTree st;
    public static void CodeGen(Tree AST){

        Tree t= AST;
        st = t.tree;
        //System.out.println("PRINT TREE AGAIN");
        TableObj tabobj = new TableObj();
        ArrayList<String> env = tabobj.getEnv();
        /*
        * We want to initalize the env from 00 - FF (256 bytes)
        */
        for(int i=0; i<255; i++){
            env.add("--");
        }
        String strt = "true";
        String strf = "false";
        strt = '"'+strt+'"';
        int trueposint =tabobj.setHeap(strt);
        String truepointer=String.format("%02X", trueposint);
        strf = '"'+strf+'"';
        int falseposint =tabobj.setHeap(strf);
        String falsepointer=String.format("%02X", falseposint);

        tabobj.truepos = truepointer;
        tabobj.falsepos = falsepointer;
        //ScopeTree st = AST.tree;
        //prntTable(st.getRoot(st));
        tabobj.env = env;
        boolean fromifwhile = false;
        prntTree(tabobj,AST.root,0,AST, fromifwhile,0);
        //System.out.println(env.size());
        tabobj.setEnv("00");//After the last syscall we need to add a 00
        //Generate(tabobj,AST);

        //tables(tabobj);
        ArrayList<String> errs = tabobj.getErrors();
        int ecount = tabobj.errors.size();
        if(ecount>0){
            System.out.println("DEBUG: CODE GEN FOUND ERRORS: ");
            printErrors(errs);
        }else{
            //We must check for errors after each on of these
            resolveTMP(tabobj);

            errs = tabobj.getErrors();
            ecount = tabobj.errors.size();
            if(ecount>0){
                System.out.println("DEBUG: CODE GEN FOUND ERRORS: ");
                printErrors(errs);
            }else{
                resolveJMP(tabobj);
                errs = tabobj.getErrors();
                ecount = tabobj.errors.size();
                if(ecount>0){
                    System.out.println("DEBUG: CODE GEN FOUND ERRORS: ");
                    printErrors(errs);
                }else{
                    fillgap(tabobj);
                }
            }
        }
    }
    public static void printErrors(ArrayList<String> errs){
        for(int i=0; i<errs.size(); i++){
            System.out.println(errs.get(i));
        }
    }
    public static int prntTable(ScopeNode current,String varname, int scope, boolean found){
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
               // System.out.println(key+"\t " + type+"\t "+ + hm.get(key).getScope()+"\t "+hm.get(key).associated.getLinenumber());

                if(key.equals(varname)&&hm.get(key).getScope()==scope){
                    found=true;
                   // System.out.println("YOU ARE LOOKING FOR VAR: "+varname+" IN SCOPE: "+scope);
                }
            }
            for(int j=0; j<current.children.size(); j++){
                prntTable(current.children.get(j),varname,scope,found);//We add one to create separation for its child nodes
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
               // System.out.println(key+"\t " + type+"\t "+ + hm.get(key).getScope()+"\t "+hm.get(key).associated.getLinenumber());
                if(key.equals(varname)&&hm.get(key).getScope()==scope){
                    found=true;
                   // System.out.println("YOU ARE LOOKING FOR VAR: "+varname+" IN SCOPE: "+scope);
                }
            }
        }
        return 0;
    }
    public static TableObj Generate(TableObj obj){
        ArrayList<String>env = obj.getEnv();
        for(int i=0; i<env.size(); i++){
            System.out.print(env.get(i)+" ");
        }
        System.out.println();
        return obj;
    }
    public static TableObj prntTree(TableObj obj,Node node, int indent, Tree AST, boolean fromifwhile, int counter){
        //System.out.println(node.scope);
        //System.out.println("CURRENT CHILDREN: "+node.children.size());
        String spacing = "";
        for(int i=0;i<indent; i++){
            spacing+="-";
        }         
        if(node.children.size() > 0){//Branches have children, leaf nodes do not
            spacing+="<" + node.name + ">";
            //System.out.println(spacing);//This line prints the branches

            /*
            *
            * For each different type of branch, add it to the env
            *
            */
            //System.out.println("COUNTER SIZE: "+obj.counter);
            if(node.name.equals("Var Decl")){
                obj = addVarDecl(obj,node);
                resolveJump(obj);
            }else if(node.name.equals("Assignment")){
                obj = addAssign(obj,node);
                resolveJump(obj);
            }else if(node.name.equals("PrintStatement")){
                obj = addPrint(obj,node, AST);
                //System.out.println("here");
                resolveJump(obj);
            }else if(node.name.equals("IfStatement")){
                resolveJump(obj);
                obj = addIf(obj,node, AST);
                obj.ifscope=node.scope;
                // System.out.println(obj.ifscope);
                // System.out.println("IF CHILDREN: ");
                for(int i=0; i<node.children.size(); i++){
                    //System.out.println(node.children.get(i).name);
                    if(node.children.get(i).name.equals("Block")){
                        Node cur = node.children.get(i);
                        int blockchild=0;
                        for(int j=0; j<cur.children.size(); j++){
                            //System.out.println("BLK CHILD: "+cur.children.get(j).name);
                            blockchild++;
                        }
                        //Now for each existing jump counter we must add to them the children of the new block
                        HashMap<String,String> jc = obj.jumpcount;
                        for(String key:jc.keySet()){
                            int curnum = Integer.parseInt(jc.get(key));
                            curnum+=blockchild;
                            obj.jumpcount.put(key,String.valueOf(curnum));
                        }
                        //We then add a new jump count for the new if block
                        obj.setJumpCount(obj.curjump,blockchild);
                    }
                }
            }else if(node.name.equals("WhileStatement")){
                obj = addWhile(obj,node,AST);
                resolveJump(obj);
                //System.out.println("WHILE CHILDREN: ");
                for(int i=0; i<node.children.size(); i++){
                    //System.out.println("*"+node.children.get(i).name);
                    if(node.children.get(i).name.equals("Block")){
                        Node cur = node.children.get(i);
                        int blockchild=0;
                        for(int j=0; j<cur.children.size(); j++){
                            //System.out.println("BLK CHILD: "+cur.children.get(j).name);
                            blockchild++;
                        }
                        //Now for each existing jump counter we must add to them the children of the new block
                        HashMap<String,String> jc = obj.jumpcount;
                        for(String key:jc.keySet()){
                            int curnum = Integer.parseInt(jc.get(key));
                            curnum+=blockchild;
                            obj.jumpcount.put(key,String.valueOf(curnum));
                        }
                        //We then add a new jump count for the new while block
                        obj.setJumpCount(obj.curjump,blockchild);
                        //System.out.println("THIS:::::: "+obj.curjump);
                    }
                }
            }else if(node.name.equals("Block")){
                if(fromifwhile == true){
                    for(int i=0; i<node.children.size(); i++){
                        //System.out.println("BLOCK CHILD: "+node.children.get(i).name);
                        counter++;
                        obj.counter++;
                    }
                }
            }

            for(int j=0; j<node.children.size(); j++){
                obj =prntTree(obj, node.children.get(j), indent+1, AST, fromifwhile, counter);//We add one to create separation for its child nodes
            }
        }else{
            if(node.name.equals("Statement")||node.name.equals("CharList")){//If these have no children, they are the espilon case
                node.name ="epsilon";
            }
            spacing+="[" + node.name + "] ";
            //System.out.println(spacing);//This line prints the leaf nodes
        }
        return obj;
    }
    public static void tables(TableObj obj){
        HashMap<String,String> tmp = obj.getTempTable();
        HashMap<String,String> jmp = obj.getJumpTable();
        for(String k: tmp.keySet()){
            //System.out.println(k+" "+tmp.get(k));
        }
        System.out.println();
        for(String j: jmp.keySet()){
            //System.out.println(j+" "+jmp.get(j));
        }
    }
    public static TableObj plus(TableObj obj, Node plusNode, String varname, String from){
        //System.out.println("PLUS");
        String regex = "[a-z]";
        String numregex = "[0-9]";
        //System.out.println(plusNode.children.size());
        String addr="";
        String tempnum="";
        String vaddr="";
        if(from.equals("ASSN")){
            addr = lookupAddr(obj, varname, plusNode);
            tempnum = addr.substring(0,2);//T1
            vaddr = addr.substring(2,addr.length());//XX
        }
        // for(int i=0; i<plusNode.children.size(); i++){
        //     System.out.println(plusNode.children.get(i).name);
        // }
        //a+11+1+1+1+1+1
        if(plusNode.children.size()==3){
            Node current = plusNode;
            String child1 = current.children.get(0).name;
            String child2 = current.children.get(1).name;
            current = current.children.get(2);
            //Load the first child and store it in memory
            //this is only valid if both children are nums... Fix that
            if(child1.matches(numregex)){
                obj.setEnv("A9");
                obj.setEnv("0"+child1);
            }else if(child1.matches(regex)){
                obj.setEnv("AD");
                String varaddr = lookupAddr(obj, child1, current.children.get(0));
                String n = varaddr.substring(0,2);//T1
                String ad = varaddr.substring(2,varaddr.length());//XX
                obj.setEnv(n);
                obj.setEnv(ad);
            }
            
            obj.setEnv("8D");

            int num =obj.getTempTable().size();
            if(child1.matches(numregex)){
                obj.setTemp("0"+child1, "T"+num, "XX");
            }else if(child1.matches(regex)){
                obj.setTemp(child1, "T"+num, "XX");
            }
            obj.ifaddr = "T"+num+"XX";
            

            obj.setEnv("T"+num);
            obj.setEnv("XX");
            //Now load the second child to acc and ADC the mem address of child1
            if(child2.matches(numregex)){
                obj.setEnv("A9");
                obj.setEnv("0"+child2);
            }else if(child2.matches(regex)){
                obj.setEnv("AD");
                String varaddr = lookupAddr(obj, child2, current.children.get(1));
                String n = varaddr.substring(0,2);//T1
                String ad = varaddr.substring(2,varaddr.length());//XX
                obj.setEnv(n);
                obj.setEnv(ad);
            }
            obj.setEnv("6D");
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            //Store the result back in the temp addr
            obj.setEnv("8D");
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            //System.out.println("()()()()("+current.children.size());
            while(current.children.size()==2){   
                //System.out.println("()()()()(");             
                String child = current.children.get(0).name;
                //Add the child to the acc. Then ADC the temp addr and store it back in the temp addr
                current = current.children.get(1);
                if(child.matches(numregex)){
                    obj.setEnv("A9");
                    obj.setEnv("0"+child);
                }else if(child.matches(regex)){
                    obj.setEnv("AD");
                    String varaddr = lookupAddr(obj, child, current.children.get(0));
                    String n = varaddr.substring(0,2);//T1
                    String ad = varaddr.substring(2,varaddr.length());//XX
                    obj.setEnv(n);
                    obj.setEnv(ad);
                }
                obj.setEnv("6D");
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                //Store the result back in the temp addr
                obj.setEnv("8D");
                obj.setEnv("T"+num);
                obj.setEnv("XX");
            }
            child1 = current.children.get(0).name;
            if(child1.matches(numregex)){
                obj.setEnv("A9");
                obj.setEnv("0"+child1);
            }else if(child1.matches(regex)){
                obj.setEnv("AD");
                String varaddr = lookupAddr(obj, child1, current.children.get(0));
                String n = varaddr.substring(0,2);//T1
                String ad = varaddr.substring(2,varaddr.length());//XX
                obj.setEnv(n);
                obj.setEnv(ad);
            }
            obj.setEnv("6D");
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            //Store the result back in the temp addr
            obj.setEnv("8D");
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            //This is the end of the addition
            //We now store the result of the addition back in the init var
            //The current result is still in the acc. So we can directly store it in the addr
            if(from.equals("IF")){
                //Skip in here
            }else if(from.equals("ASSN")){//This puts the result of the addition back into the addr of the first var
                obj.setEnv("8D");
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
            }else if(from.equals("PRNT")){
                obj.setEnv("AC");
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                obj.setEnv("A2");
                obj.setEnv("01");
                obj.setEnv("FF");
            }else if(from.equals("WHILE")){
                //nothing here. We know the last temp addr is what we need
            }
        }else{
            if(plusNode.children.size()==2){
                //System.out.println("IN ELSE:::");
                String child1 = plusNode.children.get(0).name;
                String child2 = plusNode.children.get(1).name;
                if(child1.matches(numregex)){
                    obj.setEnv("A9");
                    obj.setEnv("0"+child1);
                }else if(child1.matches(regex)){
                    obj.setEnv("AD");
                    String varaddr = lookupAddr(obj, child1, plusNode.children.get(0));
                    String n = varaddr.substring(0,2);//T1
                    String ad = varaddr.substring(2,varaddr.length());//XX
                    obj.setEnv(n);
                    obj.setEnv(ad);
                }
                obj.setEnv("8D");
    
                int num =obj.getTempTable().size();
                if(child1.matches(numregex)){
                    obj.setTemp("0"+child1, "T"+num, "XX");
                }else if(child1.matches(regex)){
                    obj.setTemp(child1, "T"+num, "XX");
                }
                obj.ifaddr = "T"+num+"XX";
    
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                //Now load the second child to acc and ADC the mem address of child1
                if(child2.matches(numregex)){
                    obj.setEnv("A9");
                    obj.setEnv("0"+child2);
                }else if(child2.matches(regex)){
                    obj.setEnv("AD");
                    String varaddr = lookupAddr(obj, child2, plusNode.children.get(1));
                    String n = varaddr.substring(0,2);//T1
                    String ad = varaddr.substring(2,varaddr.length());//XX
                    obj.setEnv(n);
                    obj.setEnv(ad);
                }
                obj.setEnv("6D");
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                //Store the result back in the temp addr
                obj.setEnv("8D");
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                if(from.equals("IF")){
                    //Skip in here
                }else if(from.equals("ASSN")){//This puts the result of the addition back into the addr of the first var
                    obj.setEnv("8D");
                    obj.setEnv(tempnum);
                    obj.setEnv(vaddr);
                }else if(from.equals("PRNT")){
                    obj.setEnv("AC");
                    obj.setEnv("T"+num);
                    obj.setEnv("XX");
                    obj.setEnv("A2");
                    obj.setEnv("01");
                    obj.setEnv("FF");
                }
            }else{
                //System.out.println("SIZEEEE"+plusNode.children.size());
            }
        }
        return obj;
    }
    public static TableObj addWhileEnd(TableObj obj, String startofWhile){
        //Every while loop.. Add 0 to acc, Store it in temp, Load a 1 in x reg, compare it to the 0 making it always branch
        obj.setEnv("A9");
        obj.setEnv("00");
        obj.setEnv("8D");
        int num =obj.getTempTable().size();
        obj.setTemp("00", "T"+num, "XX");
        //We then add the T0 and XX to the env
        obj.setEnv("T"+num);
        obj.setEnv("XX");
        obj.setEnv("A2");
        obj.setEnv("01");
        obj.setEnv("EC");
        obj.setEnv("T"+num);
        obj.setEnv("XX");
        obj.setEnv("D0");
        //Now we have to jump the distance back to where the loop started... This will be at the startofWhile var address
        ArrayList<String> env = obj.getEnv();
        int i=0;
        while(!env.get(i).equals("--")){
            i++;
            if(i==env.size()-2){
                //obj.setErr("DEBUG: ERROR STACK RAN INTO HEAP");
                ArrayList<String> e = obj.getErrors();
                e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                //System.out.println("1OOOO "+obj.errors.size());
                //obj.errors.add("DEBUG: ERROR STACK RAN INTO HEAP");
                return obj;
            }
        }
        // System.out.println("CURRENT POSITION: "+i);
        // System.out.println("WHILE STARTED AT: "+startofWhile);
        int toEnd = 255 - i;
        int toStartofLoop = toEnd + Integer.valueOf(startofWhile);
        //System.out.println("TO END: "+toEnd+" TO START OF LOOP: "+toStartofLoop);
        String pointer=String.format("%02X", toStartofLoop);
        obj.setEnv(pointer);
        return obj;
    }
    public static TableObj addWhile(TableObj obj, Node whileNode, Tree AST){
        //System.out.println("IN WHILE:::");
        String name = whileNode.children.get(0).name;
        Node whilechild = whileNode.children.get(0);
        if(name.equals("!=")||name.equals("==")){
            String child = whilechild.children.get(0).name;
            String child2 = whilechild.children.get(1).name;
            String regex = "[a-z]";
            String nums = "[0-9]";
            String startofWhile="";
            ArrayList<String> env = obj.getEnv();
            int inc=0;
            while(!env.get(inc).equals("--")){
                inc++;
                if(inc==env.size()-2){
                    ArrayList<String> e = obj.getErrors();
                    e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                   // System.out.println("2OOOO "+obj.errors.size());
                    return obj;
                }
            }
            startofWhile = String.valueOf(inc);
            if(child.matches(regex)&&child2.matches(regex)){
                obj.setEnv("AE");
                String addr = lookupAddr(obj, child, whilechild.children.get(0));
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now that we have loaded the first var into x reg. We need to compare the value of the second to the value in the x reg
                obj.setEnv("EC");
                addr = lookupAddr(obj, child2, whilechild.children.get(1));
                tempnum = addr.substring(0,2);//T1
                vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now the z flag will be set to 1 if they are equal.
                //For the while statement we must first set the acc to 0
                // obj.setEnv("A9");
                // obj.setEnv("00");
                // //Now if the z flag was set branch over the next 2 bytes where we set the acc to 1
                // obj.setEnv("D0");
                // obj.setEnv("02");
                // //This will be skipped if the branch is sucessful
                // obj.setEnv("A9");
                // obj.setEnv("01");
                // //Now that we have conditionally set the acc. We set the x reg to 00 and compare the acc to it
                // obj.setEnv("A2");
                // obj.setEnv("00");
                // obj.setEnv("8D");
                // int num =obj.getTempTable().size();
                // obj.setTemp("00", "T"+num, "XX");
                // //We then add the T0 and XX to the env
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");

                // obj.setEnv("EC");
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");



                if(name.equals("!=")){
                    resNE(obj);
                }


                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);
            }else if(child.matches(regex)&&child2.matches(nums)){
                //The var is a number not a var
                //Becuase it is a number. We can add the number to the reg and compare the var to it
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child2);
                obj.setEnv("EC");
                String addr = lookupAddr(obj, child, whilechild.children.get(0));
                //System.out.println(addr);
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now the z flag will be set to 1 if they are equal.
                //If the z flag is 0 we need to branch as the if condition was not met
                // obj.setEnv("A9");
                // obj.setEnv("00");
                // //Now if the z flag was set branch over the next 2 bytes where we set the acc to 1
                // obj.setEnv("D0");
                // obj.setEnv("02");
                // //This will be skipped if the branch is sucessful
                // obj.setEnv("A9");
                // obj.setEnv("01");
                // //Now that we have conditionally set the acc. We set the x reg to 00 and compare the acc to it
                // obj.setEnv("A2");
                // obj.setEnv("00");
                // obj.setEnv("8D");
                // int num =obj.getTempTable().size();
                // obj.setTemp("00", "T"+num, "XX");
                // //We then add the T0 and XX to the env
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");

                // obj.setEnv("EC");
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");


                if(name.equals("!=")){
                    resNE(obj);
                }



                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);
            }else if(child.matches(nums)&&child2.matches(regex)){
                //The var is a number not a var
                //Becuase it is a number. We can add the number to the reg and compare the var to it
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child);
                obj.setEnv("EC");
                String addr = lookupAddr(obj, child2, whilechild.children.get(0));
                //System.out.println(addr);
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now the z flag will be set to 1 if they are equal.
                //If the z flag is 0 we need to branch as the if condition was not met
                // obj.setEnv("A9");
                // obj.setEnv("00");
                // //Now if the z flag was set branch over the next 2 bytes where we set the acc to 1
                // obj.setEnv("D0");
                // obj.setEnv("02");
                // //This will be skipped if the branch is sucessful
                // obj.setEnv("A9");
                // obj.setEnv("01");
                // //Now that we have conditionally set the acc. We set the x reg to 00 and compare the acc to it
                // obj.setEnv("A2");
                // obj.setEnv("00");
                // obj.setEnv("8D");
                // int num =obj.getTempTable().size();
                // obj.setTemp("00", "T"+num, "XX");
                // //We then add the T0 and XX to the env
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");

                // obj.setEnv("EC");
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");


                if(name.equals("!=")){
                    resNE(obj);
                }





                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);
            }else if(child.matches(nums)&&child2.matches(nums)){
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child);
                //Now we need to store child2 in memory so we can compare a memory to x reg
                obj.setEnv("A9");
                obj.setEnv("0"+child2);
                //Now store the acc in memory
                obj.setEnv("8D");
                //We then need to add the next unused temp var Ex.) T0 XX , T1 XX , T2 XX etc...
                int num =obj.getTempTable().size();
                obj.setTemp("0"+child2, "T"+num, "XX");
                //We then add the T0 and XX to the env
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                //Now compare the byte in memory to the x reg
                obj.setEnv("EC");
                String addr =lookupAddr(obj, "0"+child2, whilechild.children.get(1));
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now if the z flag was set we can branch
                // obj.setEnv("A9");
                // obj.setEnv("00");
                // //Now if the z flag was set branch over the next 2 bytes where we set the acc to 1
                // obj.setEnv("D0");
                // obj.setEnv("02");
                // //This will be skipped if the branch is sucessful
                // obj.setEnv("A9");
                // obj.setEnv("01");
                // //Now that we have conditionally set the acc. We set the x reg to 00 and compare the acc to it
                // obj.setEnv("A2");
                // obj.setEnv("00");
                // obj.setEnv("8D");
                // num =obj.getTempTable().size();
                // obj.setTemp("00", "T"+num, "XX");
                // //We then add the T0 and XX to the env
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");

                // obj.setEnv("EC");
                // obj.setEnv("T"+num);
                // obj.setEnv("XX");



                if(name.equals("!=")){
                    resNE(obj);
                }


                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);
            }else if(child.equals("+")&&child2.equals("+")){
                //String child = whilechild.children.get(0).name;
                //String child2 = whilechild.children.get(1).name;
                plus(obj, whilechild.children.get(0), child, "WHILE");
                //We know the highest temp var is the current one being used to store the result of child 1
                int numchild1 =obj.getTempTable().size();
                numchild1-=1;//This is the pos of the result from the first child addition

                //Now do the addition on the second child
                plus(obj,whilechild.children.get(1),child2, "WHILE");

                int numchild2 =obj.getTempTable().size();
                numchild2-=1;//This is the pos of the result from the first child addition

                //Now we load the first child into the X reg and compare the result of child2 to it
                obj.setEnv("AE");
                obj.setEnv("T"+numchild1);
                obj.setEnv("XX");
                obj.setEnv("EC");
                obj.setEnv("T"+numchild2);
                obj.setEnv("XX");
                if(name.equals("!=")){
                    resNE(obj);
                }


                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);

            }else if(child.equals("+")){
                plus(obj, whilechild.children.get(0), child, "WHILE");
                //We know the highest temp var is the current one being used to store the result of child 1
                int numchild1 =obj.getTempTable().size();
                numchild1-=1;//This is the pos of the result from the first child addition
                //child.matches(nums)&&child2.matches(regex)
                if(child2.matches(regex)){
                    obj.setEnv("AE");
                    String addr = lookupAddr(obj, child2, whilechild.children.get(1));
                    String tempnum = addr.substring(0,2);//T1
                    String vaddr = addr.substring(2,addr.length());//XX
                    obj.setEnv(tempnum);
                    obj.setEnv(vaddr);
                }else if(child2.matches(nums)){
                    obj.setEnv("A2");
                    obj.setEnv("0"+child2);
                }
                obj.setEnv("EC");
                obj.setEnv("T"+numchild1);
                obj.setEnv("XX");
                if(name.equals("!=")){
                    resNE(obj);
                }


                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);

            }else if(child2.equals("+")){
                plus(obj,whilechild.children.get(1),child2, "WHILE");

                int numchild2 =obj.getTempTable().size();
                numchild2-=1;//This is the pos of the result from the first child addition


                if(child.matches(regex)){
                    obj.setEnv("AE");
                    String addr = lookupAddr(obj, child, whilechild.children.get(0));
                    String tempnum = addr.substring(0,2);//T1
                    String vaddr = addr.substring(2,addr.length());//XX
                    obj.setEnv(tempnum);
                    obj.setEnv(vaddr);
                }else if(child.matches(nums)){
                    obj.setEnv("A2");
                    obj.setEnv("0"+child);
                }

                obj.setEnv("EC");
                obj.setEnv("T"+numchild2);
                obj.setEnv("XX");
                if(name.equals("!=")){
                    resNE(obj);
                }


                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
                obj.whilejumps.put("J"+size, startofWhile);


            }
        }else if(name.equals("true")){
            //TODO
            String startofWhile="";
            ArrayList<String> env = obj.getEnv();
            int inc=0;
            while(!env.get(inc).equals("--")){
                inc++;
                if(inc==env.size()-2){
                    ArrayList<String> e = obj.getErrors();
                    e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                  //  System.out.println("3OOOO "+obj.errors.size());
                    return obj;
                }
            }
            startofWhile = String.valueOf(inc);
            obj.setEnv("A9");
            obj.setEnv("00");
            obj.setEnv("8D");
            //We then need to add the next unused temp var Ex.) T0 XX , T1 XX , T2 XX etc...
            int num =obj.getTempTable().size();
            obj.setTemp("00", "T"+num, "XX");
            //We then add the T0 and XX to the env
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            obj.setEnv("A2");
            obj.setEnv("00");

            //Now compare 00 to 00 which will always be true making an infinite loop
            obj.setEnv("EC");
            obj.setEnv("T"+num);
            obj.setEnv("XX");

            obj.setEnv("D0");
            HashMap<String,String> jmp =obj.getJumpTable();
            int size = jmp.size();
            obj.setJump("J"+size,"XX");
            obj.setEnv("J"+size);
            obj.curjump ="J"+size;
            obj.whilejumps.put("J"+size, startofWhile);
        }else if(name.equals("false")){
            //TODO
            String startofWhile="";
            ArrayList<String> env = obj.getEnv();
            int inc=0;
            while(!env.get(inc).equals("--")){
                inc++;
                if(inc==env.size()-2){
                    ArrayList<String> e = obj.getErrors();
                    e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                   // System.out.println("4OOOO "+obj.errors.size());
                    return obj;
                }
            }
            startofWhile = String.valueOf(inc);


            obj.setEnv("A9");
            obj.setEnv("01");
            obj.setEnv("8D");
            //We then need to add the next unused temp var Ex.) T0 XX , T1 XX , T2 XX etc...
            int num =obj.getTempTable().size();
            obj.setTemp("00", "T"+num, "XX");
            //We then add the T0 and XX to the env
            obj.setEnv("T"+num);
            obj.setEnv("XX");
            obj.setEnv("A2");
            obj.setEnv("00");

            //Now compare 01 to 00 which will always be true making an infinite loop
            obj.setEnv("EC");
            obj.setEnv("T"+num);
            obj.setEnv("XX");

            obj.setEnv("D0");
            HashMap<String,String> jmp =obj.getJumpTable();
            int size = jmp.size();
            obj.setJump("J"+size,"XX");
            obj.setEnv("J"+size);
            obj.curjump ="J"+size;
            obj.whilejumps.put("J"+size, startofWhile);
        }
        return obj;
    }
    public static TableObj addIf(TableObj obj, Node ifNode, Tree AST){
        /*
        if(a==b){

        }
        Load the X register with the
        contents of a.
        Compare the X register to the
        contents of b.

        AE T0 XX 
        EC T1 XX 
        D0 J0

        */
        String name = ifNode.children.get(0).name;
        Node ifchild = ifNode.children.get(0);
        if(name.equals("!=")||name.equals("==")){
            String child = ifchild.children.get(0).name;
            String child2 = ifchild.children.get(1).name;
            String regex = "[a-z]";
            String nums = "[0-9]";
            if(child.matches(regex)&&child2.matches(regex)){
                obj.setEnv("AE");
                String addr = lookupAddr(obj, child, ifchild.children.get(0));
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now that we have loaded the first var into x reg. We need to compare the value of the second to the value in the x reg
                obj.setEnv("EC");
                addr = lookupAddr(obj, child2, ifchild.children.get(1));
                tempnum = addr.substring(0,2);//T1
                vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                if(name.equals("!=")){
                    resNE(obj);
                }
                //Now the z flag will be set to 1 if they are equal.
                //If the z flag is 0 we need to branch as the if condition was not met
                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
            }else if(child.equals("+")){
                //System.out.println("FIRST CHILD::");
                plus(obj,ifchild.children.get(0),child,"IF");
                String firstaddr = obj.ifaddr;
                //System.out.println("IFADDR:::"+firstaddr);

                if(ifchild.children.get(1).name.equals("+")){
                    plus(obj,ifchild.children.get(1),ifchild.children.get(1).name,"IF");
                    String secondaddr = obj.ifaddr;
                    //Now we have the result of the first and second. Add first to X Reg and compare to second addr then jump
                    //TODO
                    obj.setEnv("AE");
                    String f = firstaddr.substring(0,2);//T1
                    String s = firstaddr.substring(2,firstaddr.length());//XX
                    obj.setEnv(f);
                    obj.setEnv(s);
                    obj.setEnv("EC");
                    String sc = secondaddr.substring(0,2);//T1
                    String nd = secondaddr.substring(2,secondaddr.length());//XX
                    obj.setEnv(sc);
                    obj.setEnv(nd);
                    if(name.equals("!=")){
                        resNE(obj);
                    }
                    //Now Add the jump
                    obj.setEnv("D0");
                    HashMap<String,String> jmp =obj.getJumpTable();
                    int size = jmp.size();
                    obj.setJump("J"+size,"XX");
                    obj.setEnv("J"+size);
                    obj.curjump ="J"+size;
                }else{
                    String c2 = ifchild.children.get(1).name;
                    if(c2.matches("[0-9]")){
                        obj.setEnv("A2");
                        obj.setEnv("0"+c2);
                    }else if(c2.matches("[a-z]")){
                        obj.setEnv("AE");
                        String varaddr = lookupAddr(obj, c2, ifchild.children.get(1));
                        String n = varaddr.substring(0,2);//T1
                        String ad = varaddr.substring(2,varaddr.length());//XX
                        obj.setEnv(n);
                        obj.setEnv(ad);
                    }
                    //Now that we have loaded the second into X reg and compare the first to it
                    obj.setEnv("EC");
                    String fr = firstaddr.substring(0,2);//T1
                    String st = firstaddr.substring(2,firstaddr.length());//XX
                    obj.setEnv(fr);
                    obj.setEnv(st);
                    if(name.equals("!=")){
                        resNE(obj);
                    }
                    obj.setEnv("D0");
                    HashMap<String,String> jmp =obj.getJumpTable();
                    int size = jmp.size();
                    obj.setJump("J"+size,"XX");
                    obj.setEnv("J"+size);
                    obj.curjump ="J"+size;
                }
            }else if(child2.equals("+")){
                //System.out.println("SECOND CHILD::");
                plus(obj,ifchild.children.get(1),child,"IF");
                String secondaddr = obj.ifaddr;
                if(child.matches("[0-9]")){
                    obj.setEnv("A2");
                    obj.setEnv("0"+child);
                }else if(child.matches("[a-z]")){
                    obj.setEnv("AE");
                    String varaddr = lookupAddr(obj, child, ifchild.children.get(0));
                    String n = varaddr.substring(0,2);//T1
                    String ad = varaddr.substring(2,varaddr.length());//XX
                    obj.setEnv(n);
                    obj.setEnv(ad);
                }
                obj.setEnv("EC");
                String fr = secondaddr.substring(0,2);//T1
                String st = secondaddr.substring(2,secondaddr.length());//XX
                obj.setEnv(fr);
                obj.setEnv(st);
                if(name.equals("!=")){
                    resNE(obj);
                }
                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
            }else if(child.matches(regex)&&child2.matches(nums)){
                //The var is a number not a var
                //Becuase it is a number. We can add the number to the reg and compare the var to it
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child2);
                obj.setEnv("EC");
                String addr = lookupAddr(obj, child, ifchild.children.get(0));
                //System.out.println(addr);
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now the z flag will be set to 1 if they are equal.
                //If the z flag is 0 we need to branch as the if condition was not met
                if(name.equals("!=")){
                    resNE(obj);
                }
                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
            }else if(child.matches(nums)&&child2.matches(regex)){
                //The var is a number not a var
                //Becuase it is a number. We can add the number to the reg and compare the var to it
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child);
                obj.setEnv("EC");
                String addr = lookupAddr(obj, child2, ifchild.children.get(0));
                //System.out.println(addr);
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now the z flag will be set to 1 if they are equal.
                //If the z flag is 0 we need to branch as the if condition was not met
                if(name.equals("!=")){
                    resNE(obj);
                }
                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
            }else if(child.matches(nums)&&child2.matches(nums)){
                obj.setEnv("A2");//Load a constant
                obj.setEnv("0"+child);
                //Now we need to store child2 in memory so we can compare a memory to x reg
                obj.setEnv("A9");
                obj.setEnv("0"+child2);
                //Now store the acc in memory
                obj.setEnv("8D");
                //We then need to add the next unused temp var Ex.) T0 XX , T1 XX , T2 XX etc...
                int num =obj.getTempTable().size();
                obj.setTemp("0"+child2, "T"+num, "XX");
                //We then add the T0 and XX to the env
                obj.setEnv("T"+num);
                obj.setEnv("XX");
                //Now compare the byte in memory to the x reg
                obj.setEnv("EC");
                String addr =lookupAddr(obj, "0"+child2, ifchild.children.get(1));
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Now if the z flag was set we can branch
                if(name.equals("!=")){
                    resNE(obj);
                }
                obj.setEnv("D0");
                HashMap<String,String> jmp =obj.getJumpTable();
                int size = jmp.size();
                obj.setJump("J"+size,"XX");
                obj.setEnv("J"+size);
                obj.curjump ="J"+size;
            }
            
        }else if(name.equals("true")){
            //TODO
        }else if(name.equals("false")){
            //TODO
        }
        // Node blk = new Node(null, name, 0);
        // for(int i=0; i<ifNode.children.size(); i++){
        //     System.out.println("CHILDREN: "+ifNode.children.get(i).name);
        //     if(ifNode.children.get(i).name.equals("Block")){
        //         blk = ifNode.children.get(i);
        //     }
        // }
        // for(int i=0; i<blk.children.size(); i++){
        //     System.out.println("BLK CHILD: "+blk.children.get(i).name);
        // }
        return obj;
    }
    public static TableObj resNE(TableObj obj){
        //System.out.println("ACTUALLY A !=");
        obj.setEnv("D0");
        obj.setEnv("0C");//Dist of flipper
        obj.setEnv("A9");
        obj.setEnv("00");
        obj.setEnv("8D");
        int num =obj.getTempTable().size();
        obj.setTemp("00", "T"+num, "XX");
        //We then add the T0 and XX to the env
        obj.setEnv("T"+num);
        obj.setEnv("XX");
        obj.setEnv("A2");
        obj.setEnv("01");
        obj.setEnv("EC");
        obj.setEnv("T"+num);
        obj.setEnv("00");
        obj.setEnv("D0");
        obj.setEnv("0A");//Dist of next portion
        //Now set a 0 to a 1
        obj.setEnv("A9");
        obj.setEnv("00");
        obj.setEnv("8D");
        obj.setEnv("T"+num);
        obj.setEnv("00");
        obj.setEnv("A2");
        obj.setEnv("00");
        obj.setEnv("EC");
        obj.setEnv("T"+num);
        obj.setEnv("00");
        return obj;
    }
    public static TableObj addPrint(TableObj obj, Node printNode, Tree AST){
        // System.out.println("NNNNNN: "+printNode.children.size()+printNode.children.get(0).name);
        // for(int i=0; i<printNode.children.size(); i++){
        //     System.out.println("PRINT NODE!!!: "+printNode.children.get(i).name);
        // }
        if(printNode.children.size()==1){
            String regex ="[a-z]";
            //System.out.println(printNode.children.get(0).name);
            if(printNode.children.get(0).name.matches(regex)){
                //System.out.println("PRINT VAR ");
                //print(a)
                //We must first load the Y register from memory with the value we want to print
                obj.setEnv("AC");
                //Get the addr of the variable and add them to env
                String var = printNode.children.get(0).name;
                //HashMap<String,String> hm = obj.getTempTable();
                String addr = lookupAddr(obj,var,printNode.children.get(0));
                //String addr = hm.get(var);
                String tempnum = addr.substring(0,2);//T1
                String vaddr = addr.substring(2,addr.length());//XX

                obj.setEnv(tempnum);
                obj.setEnv(vaddr);
                //Y register is now loaded with mem
                //We must put a 1 in the X register so on a Sys call (FF) it will print the contents of the Y reg
                //Load the X reg with constant 1
                obj.setEnv("A2");
                //If it is a string load a 2 if it is an int load a 1
                ScopeTree st = AST.tree;
                //prntTable(st.getRoot(st));
                //TODO USE SYMBOL TABLE TO FIGURE OUT IF VAR IS A STRING OR AN INT
                String type = retType(st.root,printNode.children.get(0).name,printNode.children.get(0).scope,printNode.children.get(0).name,"");
               // System.out.println("PRINTNODE: "+type);
                if(type.equals("STR_TYPE")){
                    obj.setEnv("02");
                }else if(type.equals("INT_TYPE")){
                    obj.setEnv("01");
                }else if(type.equals("BOOL_TYPE")){
                    obj.setEnv("02");
                }
                //TODO
                //Sys call
                obj.setEnv("FF");
            }else{
                String name = printNode.children.get(0).name;
                if(name.matches("[0-9]")){
                    //System.out.println("PRINT NUM");
                    //print(1)
                    //For this we load the Y register with a constant being the number we wish to print
                    obj.setEnv("A0");
                    obj.setEnv("0"+printNode.children.get(0).name);
                    //We then load the X register with a 1 so on a sys call it prints the Y reg
                    obj.setEnv("A2");
                    obj.setEnv("01");
                    //Sys call
                    obj.setEnv("FF");
                }else if(name.contains("\"")){
                    obj.setEnv("A0");
                    //Now we need to add the string to the heap
                    int frontpos = obj.setHeap(name);
                    String pointer=String.format("%02X", frontpos);
                    obj.setEnv(pointer);
                    //Now load a 2 in the X reg to print the 00 terminated string
                    obj.setEnv("A2");
                    obj.setEnv("02");
                    obj.setEnv("FF");
                }else if(name.equals("+")){
                    plus(obj,printNode.children.get(0),printNode.children.get(0).name,"PRNT");
                }else if(name.equals("true")){
                    //name = '"'+name+'"';//This is a bit hacky ill admit. BUT.. If we add the quotes, we can use the same function that will strip them and add the string to the heap.
                    obj.setEnv("A0");
                    obj.setEnv(obj.truepos);
                    //Now load a 2 in the X reg to print the 00 terminated string
                    obj.setEnv("A2");
                    obj.setEnv("02");
                    obj.setEnv("FF");
                }else if(name.equals("false")){
                    //name = '"'+name+'"';//This is a bit hacky ill admit. BUT.. If we add the quotes, we can use the same function that will strip them and add the string to the heap.
                    obj.setEnv("A0");
                    obj.setEnv(obj.falsepos);
                    //Now load a 2 in the X reg to print the 00 terminated string
                    obj.setEnv("A2");
                    obj.setEnv("02");
                    obj.setEnv("FF");
                }else if(name.equals("==")||name.equals("!=")){
                    //String name = printNode.children.get(0).name;
                    Node current = printNode.children.get(0);
                    // System.out.println("OH NO BOOLEAN HEALL");
                    // for(int i=0; i<current.children.size(); i++){
                    //     System.out.println(current.children.get(i).name);
                    // }
                    String child1 = current.children.get(0).name;
                    String child2 = current.children.get(1).name;
                    String vars ="[a-z]";
                    String nums ="[0-9]";
                    if(child1.matches(vars)){
                        obj.setEnv("AE");
                        //We need to lookup the addr of the var
                        //HashMap<String, String> hm = obj.getTempTable();
                        String varAddr =lookupAddr(obj,child1,current.children.get(0));
                        String tempnum = varAddr.substring(0,2);//T1
                        String addr = varAddr.substring(2,varAddr.length());//XX
                        obj.setEnv(tempnum);//T1
                        obj.setEnv(addr);//XX
                    }else if(child1.matches(nums)){
                        obj.setEnv("A2");
                        obj.setEnv("0"+child1);
                    }else if(child1.equals("true")){
                        obj.setEnv("A2");
                        obj.setEnv(obj.truepos);
                    }else if(child1.equals("false")){
                        obj.setEnv("A2");
                        obj.setEnv(obj.falsepos);
                    }
                    //Now load child 2 into memory and EC them
                    //obj.setEnv("8D");
                    int num =obj.getTempTable().size();
                    obj.setTemp("00", "T"+num, "XX");
                    //We then add the T0 and XX to the env
                    //obj.setEnv("T"+num);
                    //obj.setEnv("XX");

                    if(child2.matches(vars)){
                        obj.setEnv("AD");
                        //We need to lookup the addr of the var
                        //HashMap<String, String> hm = obj.getTempTable();
                        String varAddr =lookupAddr(obj,child2,current.children.get(1));
                        String tempnum = varAddr.substring(0,2);//T1
                        String addr = varAddr.substring(2,varAddr.length());//XX
                        obj.setEnv(tempnum);//T1
                        obj.setEnv(addr);//XX
                        obj.setEnv("8D");
                        obj.setEnv("T"+num);
                        obj.setEnv("XX");
                    }else if(child2.matches(nums)){
                        obj.setEnv("A9");
                        obj.setEnv("0"+child2);
                        obj.setEnv("8D");
                        obj.setEnv("T"+num);
                        obj.setEnv("XX");
                    }else if(child2.equals("true")){
                        obj.setEnv("A9");
                        obj.setEnv(obj.truepos);
                        obj.setEnv("8D");
                        obj.setEnv("T"+num);
                        obj.setEnv("XX");
                    }else if(child2.equals("false")){
                        obj.setEnv("A9");
                        obj.setEnv(obj.falsepos);
                        obj.setEnv("8D");
                        obj.setEnv("T"+num);
                        obj.setEnv("XX");
                    }
                    //If they are equal. Print true then skip a bit for the false logic
                    obj.setEnv("EC");
                    obj.setEnv("T"+num);
                    obj.setEnv("XX");
                    if(name.equals("!=")){
                        resNE(obj);
                    }
                    obj.setEnv("D0");
                    obj.setEnv("11");
                    obj.setEnv("A0");
                    //Now we need to add the string to the heap
                    obj.setEnv(obj.truepos);
                    obj.setEnv("A2");
                    obj.setEnv("02");
                    obj.setEnv("FF");
                    //Now reset z flag to 0
                    obj.setEnv("A9");
                    obj.setEnv("00");
                    obj.setEnv("8D");
                    // int num =obj.getTempTable().size();
                    // obj.setTemp("00", "T"+num, "XX");
                    //We then add the T0 and XX to the env
                    obj.setEnv("T"+num);
                    obj.setEnv("XX");
                    obj.setEnv("A2");
                    obj.setEnv("01");
                    obj.setEnv("EC");
                    obj.setEnv("T"+num);
                    obj.setEnv("XX");

                    //Now do that for false
                    obj.setEnv("D0");
                    obj.setEnv("05");

                    obj.setEnv("A0");
                    obj.setEnv(obj.falsepos);
                    obj.setEnv("A2");
                    obj.setEnv("02");
                    obj.setEnv("FF");

                
                }

            }
        }else{
            //TODO
            //print(a+1)
            //System.out.println("GGGGGGG");
            plus(obj,printNode.children.get(0),printNode.children.get(0).name,"PRNT");

        }
        return obj;
    }
    public static TableObj addVarDecl(TableObj obj, Node vardecl){
        //System.out.println(vardecl.children.size());
        for(int i=0; i<vardecl.children.size(); i++){
            //System.out.println(vardecl.children.get(i).name);
        }
        String var = vardecl.children.get(1).name;
        //A9 00 8D T0 XX
        //First we add the A9 00 because in our grammar we will always init as 00
        obj.setEnv("A9");
        obj.setEnv("00");
        //We then Store in the accumulator with 8D
        obj.setEnv("8D");
        //We then need to add the next unused temp var Ex.) T0 XX , T1 XX , T2 XX etc...
        int num =obj.getTempTable().size();
        obj.setTemp(var, "T"+num, "XX");
        //We then add the T0 and XX to the env
        obj.setEnv("T"+num);
        obj.setEnv("XX");
        return obj;
    }
    public static TableObj addAssign(TableObj obj, Node AssnNode){
        Node beingAssigned = AssnNode.children.get(0);
        Node next = AssnNode.children.get(1);


        //a=3 A9 03 8D T0 XX
        //a=b AD T1 XX 8D T0 XX


        if(next.name.equals("+")){
            plus(obj,next,beingAssigned.name,"ASSN");
        }else{
            String vars = "[a-z]";
            String nums = "[0-9]";
            if(next.name.matches(vars)){
                //this is the case for a=b
                //We need to load the acc with the variable being assigned
                obj.setEnv("AD");
                //We need to lookup the addr of the var
                //HashMap<String, String> hm = obj.getTempTable();
                String varAddr =lookupAddr(obj,next.name,AssnNode);
                String tempnum = varAddr.substring(0,2);//T1
                String addr = varAddr.substring(2,varAddr.length());//XX
                obj.setEnv(tempnum);//T1
                obj.setEnv(addr);//XX
                //We now store the acc in memory where the first var is located a in a=b
                obj.setEnv("8D");
                //We now add the addr of that var
                varAddr =lookupAddr(obj,beingAssigned.name,AssnNode);//THIS IS NOW THE a
                tempnum = varAddr.substring(0,2);//T0
                addr = varAddr.substring(2,varAddr.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(addr);
            }else if(next.name.matches(nums)){
                //The is the case for a=3

                //We load the acc with the constant 
                obj.setEnv("A9");
                obj.setEnv("0"+next.name);
                //We then store the acc at the location of the var being assigned
                obj.setEnv("8D");
                //We then add the address to the env
                //HashMap<String,String> hm = obj.getTempTable();
                String loc = lookupAddr(obj,beingAssigned.name,AssnNode);
                String tempnum = loc.substring(0,2);//T0
                String addr = loc.substring(2,loc.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(addr);
            }else if(next.name.contains("\"")){
                //System.out.println("FOUNDSTR__");
                obj.setEnv("A9");
                //Now we need to add the string to the heap
                int frontpos = obj.setHeap(next.name);
                String pointer=String.format("%02X", frontpos);
                // System.out.println("FRONTPOS: "+frontpos);
                // System.out.println("POINTER: "+pointer);
                // System.out.println("FRONT POS: "+obj.env.get(frontpos));
                // System.out.println("TYPE: "+next.associated.getTknType());
                obj.setEnv(pointer);
                //Now store it in mem
                obj.setEnv("8D");
                String loc = lookupAddr(obj,beingAssigned.name,AssnNode);
                String tempnum = loc.substring(0,2);//T0
                String addr = loc.substring(2,loc.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(addr);
            }else if(next.name.equals("true")){
                //next.name = '"'+next.name+'"';//This is a bit hacky ill admit. BUT.. If we add the quotes, we can use the same function that will strip them and add the string to the heap.
                obj.setEnv("A9");
                obj.setEnv(obj.truepos);
                //Now store it in mem
                obj.setEnv("8D");
                String loc = lookupAddr(obj,beingAssigned.name,AssnNode);
                String tempnum = loc.substring(0,2);//T0
                String addr = loc.substring(2,loc.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(addr);
            }else if(next.name.equals("false")){
                //next.name = '"'+next.name+'"';//This is a bit hacky ill admit. BUT.. If we add the quotes, we can use the same function that will strip them and add the string to the heap.
                obj.setEnv("A9");
                obj.setEnv(obj.falsepos);
                //Now store it in mem
                obj.setEnv("8D");
                String loc = lookupAddr(obj,beingAssigned.name,AssnNode);
                String tempnum = loc.substring(0,2);//T0
                String addr = loc.substring(2,loc.length());//XX
                obj.setEnv(tempnum);
                obj.setEnv(addr);
            }
        }

        return obj;
    }
    public static String lookupAddr(TableObj obj ,String varname, Node node){
        String addr="";
        HashMap<String,String> tmp = obj.getTempTable();
        for(String k: tmp.keySet()){
            if(varname.equals(tmp.get(k))){
                addr = k;
            }
            //System.out.println("THIS "+k+ " "+tmp.get(k)+ " "+varname);
        }
        //System.out.println(varname+" "+addr);
        return addr;
    }
    public static TableObj fillgap(TableObj obj){
        ArrayList<String> env = obj.getEnv();
        for(int i=0; i<env.size(); i++){
            String current = env.get(i);
            if(current.equals("XX")||current.equals("!!")||current.equals("--")){
                env.set(i,"00");
            }
        }
        // System.out.println();
        // System.out.println("FINAL");
        System.out.println();
        for(int i=0; i<env.size(); i++){
            System.out.print(env.get(i)+" ");
        }
        return obj;
    }
    public static TableObj resolveJMP(TableObj obj){
        //System.out.println();
        HashMap<String,String> jmp = obj.getJumpTable();
        ArrayList<String> env = obj.getEnv();
        for(String key: jmp.keySet()){
            for(int i=0; i<env.size(); i++){
                if(env.get(i).equals(key)){
                    // System.out.println("FOUND: "+key+" AT: "+i);
                    // System.out.println("JUMP TO: "+jmp.get(key));
                    String form = jmp.get(key);
                    System.out.println("DEBUG: BACKPATCHING - "+key+" AT: "+String.format("%02X", i));
                    int to = Integer.parseInt(jmp.get(key));
                    int dist = to-i-1;//We need to subtract an additional 1 because the jump distance assumes we skip the last place
                    String fin =String.format("%02X", dist);
                    env.set(i,fin);
                }
            }
        }
        return obj;
    }
    public static TableObj resolveJump(TableObj obj){
        //System.out.println("YEPRIGHTHERE");
        //HashMap<String,String> jmp = obj.getJumpTable();
        // for(String key: jmp.keySet()){
        //     System.out.println(key+ " "+jmp.get(key));
        // }
        // System.out.println("---------------");
        HashMap<String,String> jcount = obj.jumpcount;
        for(String key: jcount.keySet()){
            //System.out.println(key+" "+jcount.get(key));
            int num = Integer.parseInt(jcount.get(key));
            num-=1;
            obj.jumpcount.put(key,String.valueOf(num));
            if(num==0){
                int c=0;
                ArrayList<String> env = obj.getEnv();
                while(!env.get(c).equals("--")){
                    c++;
                    if(c==env.size()-2){
                        ArrayList<String> e = obj.getErrors();
                        e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                        return obj;
                    }
                }
                obj.jump.put(key,String.valueOf(c));
                //System.out.println("PUTTING: "+key+" AT: "+c);
                HashMap<String,String> wj = obj.getWhileJumps();
                for(String k: wj.keySet()){
                    if(k.equals(key)){
                        addWhileEnd(obj,wj.get(k));
                        HashMap<String,String> j = obj.getJumpTable();
                        String curval = j.get(key);
                        int cur = Integer.valueOf(curval);
                        cur = cur+12; //The end of while loop is always the same size
                        obj.jump.put(key,String.valueOf(cur));
                    }
                }
            }
        }
        //System.out.println("END");
        return obj;
    }
    public static TableObj resolveTMP(TableObj obj){
        int spot=0;
        ArrayList<String> env = obj.getEnv();
        boolean err=false;
        //Generate(obj);
        while(!env.get(spot).equals("--")){
            spot++;
            if(spot==env.size()-2){
                ArrayList<String> e = obj.getErrors();
                e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                return obj;
            }
        }
        // System.out.println(spot);
        // System.out.println(String.format("%02X", spot));
        // System.out.println(env.get(spot));
        if(err==true){
            //skip
        }else{
            for(String key: obj.getTempTable().keySet()){
                //String data = obj.getTempTable().get(key);
                // System.out.println("KEY: "+key);
                // System.out.println("DATA: "+data);
                String tempnum = key.substring(0,2);//T1
                for(int i=0; i<env.size(); i++){
                    if(env.get(i).equals(tempnum)){
                        //System.out.println("DEBUG: BACKPATCHING - "+tempnum+" AT - "+spot);
                        env.set(i,String.format("%02X", spot));
                    }
                }
                System.out.println("DEBUG: BACKPATCHING - "+tempnum+" AT - "+String.format("%02X", spot));
                if(env.get(spot).equals("--")){
                    env.set(spot,"!!");
                    spot++;
                }else{
                    ArrayList<String> e = obj.getErrors();
                    e.add("DEBUG: ERROR STACK RAN INTO HEAP");
                    return obj;
                }
            }
        }
        // for(int i=0; i<obj.getEnv().size(); i++){
        //     System.out.print(obj.getEnv().get(i)+" ");
        // }
        return obj;
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
}

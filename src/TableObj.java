import java.util.ArrayList;
import java.util.HashMap;

public class TableObj {
    
    ArrayList<String> env = new ArrayList<String>();
    ArrayList<String> errors = new ArrayList<String>();
    HashMap<String,String> temp = new HashMap<String,String>();
    HashMap<String,String> jump = new HashMap<String,String>();
    HashMap<String,String> jumpcount = new HashMap<String,String>();
    HashMap<String,String> whilejumps = new HashMap<String,String>();
    int ifscope;
    String curjump;
    int counter=0;
    String ifaddr="";
    public void setEnv(String opCode){
        boolean foundspot=false;
        for(int i=0; i<env.size(); i++){
            if(foundspot==false){
                if(env.get(i).equals("--")){
                    env.set(i,opCode);
                    foundspot = true;
                }
            }
        }
    }
    public int setHeap(String str){
        int lastpos = env.size()-1;
        int frontpos = lastpos;
        String rawstr="";
        for(int i=0; i<str.length(); i++){
            char character = str.charAt(i);
            String charToString = String.valueOf(character);
            if(charToString.equals("\"")){
                //Skip this
            }else{
                rawstr+=charToString;
            }
        }
        //Now that we have removed the quotes from the str, add it to the heap
        while(!env.get(lastpos).equals("--")){
            lastpos--;
        }
        //Now we know the first empty space in the heap.. remove the size of the string we want to add
        lastpos -= rawstr.length();
        frontpos = lastpos;
        for(int i=0; i<rawstr.length(); i++){
            char character = rawstr.charAt(i);
            //String charToString = String.valueOf(character);  
            int ascii = (int) character;
            String astr = String.format("%02X", ascii);
            //String fin =String.format("%02X", astr);
            env.set(lastpos,astr);
            lastpos++;
        }
        env.set(lastpos, "00");
        return frontpos;
    }
    public void setTemp(String var, String addr, String addr2){
        this.temp.put(addr+addr2,var);
    }
    public void setJump(String var, String dist){
        this.jump.put(var,dist);
    }
    public void setJumpCount(String j, int num){
        String len = String.valueOf(num);
        if(len.length()==1){
            len = "0"+len;
        }
        this.jumpcount.put(j,len);
    }
    public ArrayList<String> getEnv(){
        return env;
    }
    public HashMap<String,String> getTempTable(){
        return temp;
    }
    public HashMap<String,String> getJumpTable(){
        return jump;
    }
    public HashMap<String,String> getWhileJumps(){
        return whilejumps;
    }
}

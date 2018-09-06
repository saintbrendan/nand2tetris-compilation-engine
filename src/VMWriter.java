import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VMWriter {
    private static final Map<Character, String> cmdFromOp;
    static {
        Map<Character, String> aMap = new HashMap<>();
        aMap.put('+',"add");
        aMap.put('-',"sub");
        aMap.put('-',"neg");
        aMap.put('=',"eq");
        aMap.put('>',"gt");
        aMap.put('<',"lt");
        aMap.put('&',"and");
        aMap.put('|',"or");
        aMap.put('~',"not");
        cmdFromOp = Collections.unmodifiableMap(aMap);
    }
    private PrintWriter writer;

    VMWriter (PrintWriter writer) {
        this.writer = writer;
    }
    void push(Segment segment, int index) {
        writer.println("push " + segment.name().toLowerCase() + " " + index);
    }
    void pop(Segment segment, int index) {
        writer.println("pop " + segment.name().toLowerCase() + " " + index);
    }
    void arithmetic(char op) {
        writer.println(cmdFromOp.get(op));
    }
    void label (String label) {
        writer.println("label " + label);
    }
    void gotoo(String label) {
        writer.println("goto " + label);
    }
    void ifg(String label) {
        writer.println("if-goto " + label);
    }
    void call(String name, int args) {
        writer.println("call "+ name + " " + args);
    }
    void function(String name, int nLocals){
        writer.println("function " + name + " " + nLocals);
    }
    void ret() {
        writer.println("return");
    }
    void close() {
        writer.flush();
    }

}

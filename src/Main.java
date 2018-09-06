import java.io.*;
public class Main {
    public static String fileName = "C:\\Users\\saint\\IdeaProjects\\CompilationEngine\\out\\production\\CompilationEngine\\";

    public static void main(String[] args) throws IOException {
        if (args.length >= 1) {
            JackTokenizer.fileName = args[0];
        }
        File file = new File(JackTokenizer.fileName);
        Reader reader = new FileReader(file);

        StreamTokenizer streamTokenizer = JackTokenizer.tokenizerOf(reader);
        while(streamTokenizer.nextToken() != StreamTokenizer.TT_EOF){

            if(streamTokenizer.ttype == StreamTokenizer.TT_WORD) {
                System.out.println(streamTokenizer.sval);
            } else if(streamTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                System.out.println(streamTokenizer.nval);
            } else if(streamTokenizer.ttype == StreamTokenizer.TT_EOL) {
                System.out.println();
            } else {
                System.out.println("xxx"+Character.toString((char)streamTokenizer.ttype));
            }
        }
    }

}

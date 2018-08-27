import java.io.*;
public class Main {
    ////public static String fileName = "C:\\Users\\saint\\OneDrive\\Documents\\nand2tetris\\projects\\10\\Square\\SquareGame.jack";


    public static void main(String[] args) throws IOException {
        if (args.length >= 1) {
            JackTokenizer.fileName = args[0];
        }
        File file = new File(JackTokenizer.fileName);
        Reader reader = new FileReader(file);

        StreamTokenizer streamTokenizer = JackTokenizer.tokenizerFromReader(reader);
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

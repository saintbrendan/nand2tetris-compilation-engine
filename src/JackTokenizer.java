import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class JackTokenizer {
    public static final List<String> KEYWORDS = asList("class", "constructor", "function", "method", "field", "static", "var",
            "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return");
    private StreamTokenizer tokenizer;
    private TokenType tokenType;
    private long intVal;
    private KeyWord keyword;
    private String identifier;
    private char symbol;
    private String stringVal;

    public static String fileName = "C:\\Users\\saint\\IdeaProjects\\CompilationEngine\\out\\production\\CompilationEngine\\";
    ///public static String fileName = "C:\\Users\\saint\\IdeaProjects\\CompilationEngine\\out\\production\\CompilationEngine\\SquareGame.jack";
    ////public static String fileName = "C:\\Users\\saint\\OneDrive\\Documents\\testtext.txt";

    private static final Map<TokenType, String> xmlFromType;

    static {
        Map<TokenType, String> map = new HashMap<>();
        map.put(TokenType.KEYWORD, "keyword");
        map.put(TokenType.SYMBOL, "symbol");
        map.put(TokenType.IDENTIFIER, "identifier");
        map.put(TokenType.INT_CONST, "integerConstant");
        map.put(TokenType.STRING_CONST, "stringConstant");
        xmlFromType = Collections.unmodifiableMap(map);
    }

    public JackTokenizer(File file) throws FileNotFoundException {
        Reader reader = new FileReader(file);
        this.tokenizer = tokenizerOf(reader);
    }

    public String stringVal() {
        return stringVal;
    }

    public TokenType tokenType() {
        return tokenType;
    }

    public long intVal() {
        return intVal;
    }

    public KeyWord keyWord() {
        return keyword;
    }

    public String identifier() {
        return identifier;
    }

    public char symbol() {
        return symbol;
    }

    public static void main(String[] args) throws IOException {
        if (args.length >= 1) {
            fileName = args[0];
        }
        File file = new File(fileName);
        if (file.isDirectory()) {
            tokenizeDirectory(file);
        } else {
            tokenizeFile(fileName);
        }
    }

    private static void tokenizeDirectory(File directory) throws IOException {
        File[] filesx = DirUtil.getFilesFromDir(directory, "jack");
        for (File file : DirUtil.getFilesFromDir(directory, "jack")) {
            tokenizeFile(file.getAbsolutePath());
        }
    }

    private static void tokenizeFile(String fileName) throws IOException {
        String outputFileName = DirUtil.stripExtension(fileName) + "T.xml";
        File file = new File(fileName);
        JackTokenizer jackTokenizer = new JackTokenizer(file);
        System.out.println("about to write to " + outputFileName);
        PrintStream printStream = new PrintStream(outputFileName);
        printStream.println("<tokens>");
        while (jackTokenizer.hasMoreTokens()) {
            jackTokenizer.advance();
            printStream.println(jackTokenizer.toXml());
        }
        printStream.println("</tokens>");
    }

    Boolean hasMoreTokens() throws IOException {
        if (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            tokenizer.pushBack();
            return true;
        }
        return false;
    }

    void advance() throws IOException {
        int result = tokenizer.nextToken();
        switch (result) {
            case StreamTokenizer.TT_NUMBER:
                tokenType = TokenType.INT_CONST;
                intVal = Math.round(tokenizer.nval);
                stringVal = Long.toString(intVal);
                break;
            case StreamTokenizer.TT_WORD:
                if (KEYWORDS.contains(tokenizer.sval)) {
                    tokenType = TokenType.KEYWORD;
                    keyword = KeyWord.valueOf(tokenizer.sval.toUpperCase());
                } else {
                    tokenType = TokenType.IDENTIFIER;
                    identifier = tokenizer.sval;
                }
                stringVal = tokenizer.sval;
                break;
            case '"':
                tokenType = TokenType.STRING_CONST;
                stringVal = tokenizer.sval;
                break;
            default:
                tokenType = TokenType.SYMBOL;
                symbol = (char) tokenizer.ttype;
                stringVal = Character.toString(symbol);
        }
    }

    public String toXml() {
        String xmlElement = xmlFromType.get(tokenType);
        String xmlString = stringVal;
        if (tokenType == TokenType.SYMBOL) {
            if (symbol == '>') {
                xmlString = "&gt;";
            } else if (symbol == '<') {
                xmlString = "&lt;";
            } else if (symbol == '&') {
                xmlString = "&amp;";
            }
        }
        return "<" + xmlElement + "> " + xmlString + " </" + xmlElement + ">";
    }

    public static StreamTokenizer tokenizerOf(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        // ignore
        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);
        tokenizer.eolIsSignificant(false);

        tokenizer.ordinaryChar('{');
        tokenizer.ordinaryChar('}');
        tokenizer.ordinaryChar('(');
        tokenizer.ordinaryChar(')');
        tokenizer.ordinaryChar('+');
        tokenizer.ordinaryChar('~');
        tokenizer.ordinaryChar('*');
        tokenizer.ordinaryChar('/');
        tokenizer.ordinaryChar('<');
        tokenizer.ordinaryChar('>');
        tokenizer.ordinaryChar('[');
        tokenizer.ordinaryChar(']');
        tokenizer.ordinaryChar('.');
        tokenizer.ordinaryChar(',');
        tokenizer.ordinaryChar('&');
        tokenizer.ordinaryChar('|');
        tokenizer.ordinaryChar(';');
        tokenizer.ordinaryChar('=');
        tokenizer.ordinaryChar('-');

        tokenizer.quoteChar('"');

        // text identifier
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('_', '_');

        return tokenizer;
    }
}

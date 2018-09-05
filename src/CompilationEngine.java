import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static java.util.Arrays.asList;

public class CompilationEngine {
    public static final List<KeyWord> STATIC_FIELD = asList(KeyWord.STATIC, KeyWord.FIELD);
    public static final List<KeyWord> CONSTRUCTOR_FUNCTION_METHOD = asList(KeyWord.CONSTRUCTOR, KeyWord.FUNCTION, KeyWord.METHOD);
    public static final List<String> KEYWORD_CONSTANTS = asList("true", "false", "null", "this");
    public static final List<KeyWord> TYPES = asList(KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN, KeyWord.VOID);
    public static final List<Character> OPERATORS = asList('+', '-', '*', '/', '&', '|', '<', '>', '=');
    public static final List<Character> UNARY_OPERATORS = asList('-', '~');
    private SymbolTable symbolTable = new SymbolTable();
    private PrintWriter printWriter;
    private JackTokenizer tokenizer;

    /**
     * class: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() throws IOException {
        // 'class'
        tokenizer.advance();
        printWriter.println("<class>");
        parseKeyword("class");
        parseIdentifier();
        parseSymbol('{');
        while (STATIC_FIELD.contains(tokenizer.keyWord())) {
            compileClassVarDec();
        }
        printWriter.flush();
        while (CONSTRUCTOR_FUNCTION_METHOD.contains(tokenizer.keyWord())) {
            compileSubroutine();
        }
        parseSymbol('}');
        if (tokenizer.hasMoreTokens()) {
            throw new IllegalStateException("Unexpected tokens");
        }
        printWriter.println("</class>");
        printWriter.close();
    }

    /**
     * Compiles a static declaration or a field declaration
     * classVarDec ('static'|'field') type varName (','varNAme)* ';'
     */
    private void compileClassVarDec() throws IOException {
        printWriter.println("<classVarDec>");
        Kind kind = parseStaticOrField();
        printWriter.flush();
        String type = compileType();
        // varName (',' varName)*
        do {
            String classVarName = parseIdentifier();
            symbolTable.put(classVarName, type, kind);
            symbolTable.put(classVarName, type, kind);
            if (!isSymbol(',')) {
                break;
            }
            parseSymbol(',');
        } while (true);
        parseSymbol(';');
        printWriter.print("</classVarDec>\n");
    }

    /**
     * Compiles a complete method, function, or constructor
     * ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
     */
    private void compileSubroutine() throws IOException {
        symbolTable.startSubroutine();
        printWriter.print("<subroutineDec>\n");
        parseKeyword();
        compileType();
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            error("subroutineName");
        }
        parseIdentifier();
        printWriter.flush();
        parseSymbol('(');
        compileParameterList();
        parseSymbol(')');
        compileSubroutineBody();
        printWriter.print("</subroutineDec>\n");
    }

    /**
     * '{'  varDec* statements '}'
     */
    private void compileSubroutineBody() throws IOException {
        printWriter.print("<subroutineBody>\n");
        parseSymbol('{');
        printWriter.flush();
        while (isKeyword("var")) {
            compileVarDec();
        }
        compileStatements();
        parseSymbol('}');
        printWriter.print("</subroutineBody>\n");
    }

    /**
     * Compiles a var declaration
     * 'var' type varName (',' varName)*;
     */
    public void compileVarDec() throws IOException {
        xmlout("varDec");
        printWriter.flush();
        parseKeyword("var");
        String type = compileType();
        do {
            if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
                error("identifier");
            }
            String varName = parseIdentifier();
            symbolTable.put(varName, type, Kind.VAR);
            /*////
            printWriter.println(tokenizer.toXml());
            tokenizer.advance();
            ////*/
            if (!isSymbol(',')) {
                break;
            }
            parseSymbol(',');
        } while (true);
        parseSymbol(';');
        xmlout("/varDec");
    }

    public void compileStatements() throws IOException {
        xmlout("statements");
        while (!isSymbol('}')) {
            switch (tokenizer.keyWord()) {
                case LET:
                    compileLet();
                    break;
                case WHILE:
                    compilesWhile();
                    break;
                case IF:
                    compileIf();
                    break;
                case DO:
                    compileDo();
                    break;
                case RETURN:
                    compileReturn();
                    break;
                default:
                    error("'let'|'if'|'while'|'do'|'return'");
            }
        }
        xmlout("/statements");
    }

    public CompilationEngine(File inFile, File outFile) {
        try {
            tokenizer = new JackTokenizer(inFile);
            printWriter = new PrintWriter(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compiles a type
     */
    private String compileType() throws IOException {
        if (!isType()) {
            error("int|char|boolean|void|className");
        }
        String type = tokenizer.stringVal();
        printWriter.println(tokenizer.toXml());
        tokenizer.advance();
        return type;
    }

    /**
     * Compiles a (possibly empty) parameter list
     * not including the enclosing "()"
     * ((type varName)(',' type varName)*)?
     */
    private void compileParameterList() throws IOException {
        xmlout("parameterList");
        printWriter.flush();
        if (isType()) {
            do {
                String type = compileType();
                String argName = parseIdentifier();
                symbolTable.put(argName, type, Kind.ARG);
                if (tokenizer.tokenType() != TokenType.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ')')) {
                    error("',' or ')'");
                }
                if (!isSymbol(',')) {
                    break;
                }
                parseSymbol(',');
            } while (true);
        }
        xmlout("/parameterList");
    }

    /**
     * Compiles a do statement
     * 'do' subroutineCall ';'
     */
    private void compileDo() throws IOException {
        printWriter.print("<doStatement>\n");
        parseKeyword("do");
        String identifier = parseIdentifier();
        compileSubroutineCall(identifier);
        parseSymbol(';');
        printWriter.print("</doStatement>\n");
    }

    /**
     * Compiles a let statement
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    private void compileLet() throws IOException {
        printWriter.print("<letStatement>\n");
        parseKeyword("let");
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            error("varName");
        }
        parseIdentifier();
        //'[' or '='
        if (!(isSymbol('[') || isSymbol('='))) {
            error(" '[' | '=' ");
        }
        if (isSymbol('[')) {
            parseSymbol('[');
            compileExpression();
            parseSymbol(']');
        }
        parseSymbol('=');
        compileExpression();
        parseSymbol(';');
        printWriter.print("</letStatement>\n");
    }

    /**
     * 'while' '(' expression ')' '{' statements '}'
     */
    private void compilesWhile() throws IOException {
        printWriter.print("<whileStatement>\n");
        parseKeyword("while");
        parseSymbol('(');
        compileExpression();
        parseSymbol(')');
        parseSymbol('{');
        compileStatements();
        parseSymbol('}');
        printWriter.print("</whileStatement>\n");
    }

    /**
     * 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    private void compileIf() throws IOException {
        printWriter.println("<ifStatement>");
        parseKeyword("if");
        parseSymbol('(');
        compileExpression();
        printWriter.flush();
        parseSymbol(')');
        parseSymbol('{');
        compileStatements();
        parseSymbol('}');
        if (isKeyword("else")) {
            parseKeyword("else");
            parseSymbol('{');
            compileStatements();
            parseSymbol('}');
        }
        printWriter.println("</ifStatement>");
    }

    /**
     * 'return’ expression? ';'
     */
    private void compileReturn() throws IOException {
        printWriter.println("<returnStatement>");
        parseKeyword("return");
        if (!isSymbol(';')) {
            compileExpression();
        }
        parseSymbol(';');
        printWriter.println("</returnStatement>");
    }

    /**
     * term (op term)*
     */
    private void compileExpression() throws IOException {
        printWriter.println("<expression>");
        compileTerm();
        while (isOp()) {
            compileOp();
            compileTerm();
        }
        printWriter.println("</expression>");
    }

    private void compileTerm() throws IOException {
        xmlout("term");
        if (isConstant()) {
            printWriter.println(tokenizer.toXml());
            tokenizer.advance();
        } else if (isUnaryOp()){
            parseSymbol();
            compileTerm();
        } else if (isSymbol('(')) {
            parseSymbol('(');
            compileExpression();
            parseSymbol(')');
        } else {
            printWriter.flush();
            // Identifier
            String identifier = parseIdentifier();
            // [               subscript
            // .               subroutine call
            // (               subroutine call
            // anything else   varName
            if (isSymbol('[')) {
                parseSymbol('[');
                compileExpression();
                parseSymbol(']');
            } else if (isSymbol('.') || isSymbol('(')) {
                compileSubroutineCall(identifier);
            }
        }
        xmlout("/term");
    }

    public void compileSubroutineCall(String identifier) throws IOException {
        // identifier already parsed
        if (isSymbol('.')) {
            parseSymbol('.');
            parseIdentifier();
        }
        parseSymbol('(');
        compileExpressionList();
        parseSymbol(')');
    }

    private void compileExpressionList() throws IOException {
        xmlout("expressionList");
        if (!isSymbol(')')) {  // This is kinda kludgy.  Depends on ExpressionList always being in parens
            compileExpression();
            while (isSymbol(',')) {
                parseSymbol(',');
                compileExpression();
            }
        }
        xmlout("/expressionList");
    }

    private void compileOp() throws IOException {
        if (isOp()) {
            parseSymbol();
        } else {
            error("'+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='");
        }
    }

    private void parseSymbol() throws IOException {
        parseSymbol(tokenizer.symbol());
    }

    private void parseSymbol(char symbol) throws IOException {
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == symbol) {
            printWriter.println(tokenizer.toXml());
        } else {
            error("'" + symbol + "'");
        }
        tokenizer.advance();
    }

    private void parseStringConstant() {
        if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            xmlout("stringConstant");

            xmlout("/stringConstant");
        }
    }

    private void parseKeywordConstant() throws IOException {
        if (KEYWORD_CONSTANTS.contains(tokenizer.stringVal())) {
            parseKeyword();
        } else {
            String expected = String.join(" | ", KEYWORD_CONSTANTS);
            error (expected);
        }
    }

    private Kind parseStaticOrField() throws IOException {
        KeyWord keyWord = KeyWord.valueOf(parseKeyword().toUpperCase());
        if (!STATIC_FIELD.contains(keyWord)) {
            error("static|field");
        }
        return Kind.valueOf(keyWord.name());
    }

    private String parseKeyword() throws IOException {
        return parseKeyword(tokenizer.stringVal());
    }

    private String parseKeyword(String keyword) throws IOException {
        String stringVal = tokenizer.stringVal();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.stringVal().equals(keyword)) {
            printWriter.println(tokenizer.toXml());
        } else {
            error("'" + keyword + "'");
        }
        tokenizer.advance();
        return stringVal;
    }

    private String parseIdentifier() throws IOException {
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            printWriter.println(tokenizer.toXml());
        } else {
            error("identifier");
        }
        String identifier = tokenizer.stringVal();
        tokenizer.advance();
        return identifier;
    }

    private boolean isIdentifier() {
        return tokenizer.tokenType() == TokenType.IDENTIFIER;
    }

    private boolean isSymbol(char symbol) {
        return (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == symbol));
    }

    private boolean isOp() {
        return tokenizer.tokenType() == TokenType.SYMBOL && OPERATORS.contains(tokenizer.symbol());
    }

    private boolean isUnaryOp() {
        return tokenizer.tokenType() == TokenType.SYMBOL && UNARY_OPERATORS.contains(tokenizer.symbol());
    }

    private boolean isKeyword(String keyword) {
        return (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.stringVal().equals(keyword)));
    }

    private boolean isType() {
        if (tokenizer.tokenType() == TokenType.KEYWORD && TYPES.contains(tokenizer.keyWord())) {
            return true;
        }
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            return true;
        }
        return false;
    }

    private boolean isConstant() {
        if (tokenizer.tokenType() == TokenType.INT_CONST) {
            return true;
        }
        if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            return true;
        }
        if (tokenizer.tokenType() == TokenType.KEYWORD && KEYWORD_CONSTANTS.contains(tokenizer.stringVal())) {
            return true;
        }
        return false;
    }

    private void error(String val) {
        throw new IllegalStateException("Missing token.  Expected token:[" + val + "] Current token:" + tokenizer.stringVal());
    }

    private void xmlout(String xml) {
        printWriter.println("<" + xml + ">");
    }
}
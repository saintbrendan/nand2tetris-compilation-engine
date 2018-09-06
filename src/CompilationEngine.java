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
    private VMWriter vmWriter;
    private JackTokenizer tokenizer;
    private String currentClassName = "";
    private String currentSubroutineName = "";
    private int whileCount = 0;
    private int ifCount = 0;

    /**
     * class: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() throws IOException {
        // 'class'
        tokenizer.advance();
        printWriter.println("<class>");
        parseKeyword("class");
        currentClassName = parseIdentifier();
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
        vmWriter.close();
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
        whileCount = 0;
        printWriter.print("<subroutineDec>\n");
        parseKeyword();
        String type = compileType();
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            error("subroutineName");
        }
        currentSubroutineName = parseIdentifier();
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
    private int compileSubroutineBody() throws IOException {
        printWriter.print("<subroutineBody>\n");
        parseSymbol('{');
        printWriter.flush();
        int varCount = 0;
        while (isKeyword("var")) {
            varCount += compileVarDec();
        }
        vmWriter.function(currentClassName + "." + currentSubroutineName, varCount);
        compileStatements();
        parseSymbol('}');
        printWriter.print("</subroutineBody>\n");
        return varCount;
    }

    /**
     * Compiles a var declaration
     * 'var' type varName (',' varName)*;
     */
    public int compileVarDec() throws IOException {
        int varCount = 0;
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
            varCount++;
            if (!isSymbol(',')) {
                break;
            }
            parseSymbol(',');
        } while (true);
        parseSymbol(';');
        xmlout("/varDec");
        return varCount;
    }

    public void compileStatements() throws IOException {
        xmlout("statements");
        while (!isSymbol('}')) {
            switch (tokenizer.keyWord()) {
                case LET:
                    compileLet();
                    break;
                case WHILE:
                    compileWhile();
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

    public CompilationEngine(File inFile, File outFile, File outVmFile) {
        try {
            tokenizer = new JackTokenizer(inFile);
            printWriter = new PrintWriter(outFile);
            PrintWriter writer = new PrintWriter(outVmFile);
            vmWriter = new VMWriter(writer);
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
        vmWriter.pop(Segment.TEMP, 0);
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
        String varName = parseIdentifier();
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
        int index = symbolTable.indexOf(varName);
        Segment segment = Segment.segmentOf(symbolTable.kindOf(varName));
        vmWriter.pop(segment, index);
        parseSymbol(';');
        printWriter.print("</letStatement>\n");
    }

    /**
     * 'while' '(' expression ')' '{' statements '}'
     */
    private void compileWhile() throws IOException {
        String whileExpression = "WHILE_EXP" + whileCount;
        String end = "WHILE_END" + whileCount;
        whileCount++;
        printWriter.print("<whileStatement>\n");
        parseKeyword("while");
        vmWriter.label(whileExpression);
        parseSymbol('(');
        compileExpression();
        parseSymbol(')');
        vmWriter.arithmetic('~');
        vmWriter.ifg(end);
        parseSymbol('{');
        compileStatements();
        parseSymbol('}');
        vmWriter.gotoo(whileExpression);
        vmWriter.label(end);
        printWriter.print("</whileStatement>\n");
        whileCount--;
    }

    /**
     * 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    private void compileIf() throws IOException {
        String ifTrue = "IF_TRUE" + ifCount;
        String ifFalse = "IF_FALSE" + ifCount;
        String end = "IF_END" + ifCount;
        ifCount++;
        printWriter.println("<ifStatement>");
        parseKeyword("if");
        parseSymbol('(');
        vmWriter.flush();
        compileExpression();
        vmWriter.flush();
        parseSymbol(')');
        vmWriter.ifg(ifTrue);
        vmWriter.gotoo(ifFalse);
        vmWriter.label(ifTrue);
        parseSymbol('{');
        compileStatements();
        parseSymbol('}');
        vmWriter.gotoo(end);
        vmWriter.label(ifFalse);
        if (isKeyword("else")) {
            parseKeyword("else");
            parseSymbol('{');
            compileStatements();
            parseSymbol('}');
        }
        vmWriter.label(end);
        printWriter.println("</ifStatement>");
        ifCount--;
    }

    /**
     * 'return’ expression? ';'
     */
    private void compileReturn() throws IOException {
        printWriter.println("<returnStatement>");
        parseKeyword("return");
        if (!isSymbol(';')) {
            compileExpression();
        } else {
            // Not returning anything.  Must be void.  So push 0.
            vmWriter.push(Segment.CONSTANT, 0);
        }
        parseSymbol(';');
        vmWriter.ret();
        printWriter.println("</returnStatement>");
    }

    /**
     * term (op term)*
     */
    private void compileExpression() throws IOException {
        printWriter.println("<expression>");
        compileTerm();
        while (isOp()) {
            char op = compileOp();
            compileTerm();
            switch (op) {
                case '*':
                    vmWriter.call("Math.multiply", 2);
                    break;
                case '/':
                    vmWriter.call("Math.divide 2", 2);
                    break;
                default:
                    vmWriter.arithmetic(op);
            }
        }
        printWriter.println("</expression>");
    }

    private void compileTerm() throws IOException {
        xmlout("term");
        if (isConstant()) {
            parseConstant();
        } else if (isUnaryOp()){
            char unaryOp = parseSymbol();
            compileTerm();
            vmWriter.unaryOp(unaryOp);
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
            } else {
                // straight variable
                int index = symbolTable.indexOf(identifier);
                Segment segment = Segment.segmentOf(symbolTable.kindOf(identifier));
                vmWriter.push(segment, index);
            }
        }
        xmlout("/term");
    }

    public void compileSubroutineCall(String identifier) throws IOException {
        // identifier already parsed
        String subroutineName = identifier;
        if (isSymbol('.')) {
            parseSymbol('.');
            String memberSubroutineName = parseIdentifier();
            subroutineName += "." + memberSubroutineName;
        }
        parseSymbol('(');
        int parameterCount = compileExpressionList();
        // compile ExpressionList should have left the expressions on the stack
        vmWriter.call(subroutineName, parameterCount);
        parseSymbol(')');
    }

    private int compileExpressionList() throws IOException {
        int expressionCount = 0;
        xmlout("expressionList");
        if (!isSymbol(')')) {  // This is kinda kludgy.  Depends on ExpressionList always being in parens
            compileExpression();
            expressionCount++;
            while (isSymbol(',')) {
                parseSymbol(',');
                compileExpression();
                expressionCount++;
            }
        }
        xmlout("/expressionList");
        return expressionCount;
    }

    private char compileOp() throws IOException {
        char op = tokenizer.symbol();
        if (isOp()) {
            parseSymbol();
        } else {
            error("'+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='");
        }
        return op;
    }

    private char parseSymbol() throws IOException {
        return parseSymbol(tokenizer.symbol());
    }

    private char parseSymbol(char symbol) throws IOException {
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == symbol) {
            printWriter.println(tokenizer.toXml());
        } else {
            error("'" + symbol + "'");
        }
        tokenizer.advance();
        return symbol;
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

    private void parseConstant() throws IOException {
        String tokenString = tokenizer.stringVal();
        switch (tokenizer.tokenType()) {
            case INT_CONST:
                vmWriter.push(Segment.CONSTANT, (int) tokenizer.intVal());
                break;
            case STRING_CONST:
                /// construct and push string constant
                break;
            case KEYWORD:
                if (KEYWORD_CONSTANTS.contains(tokenString)) {
                    /*
                    null	constant 0
                    false	constant 0
                    true	constant -1
                            push constant 0
                            not
                    */
                    if ("false".equals(tokenString) || "null".equals(tokenString)) {
                        vmWriter.push(Segment.CONSTANT, 0);
                    } else if ("true".equals(tokenString)) {
                        vmWriter.push(Segment.CONSTANT, 0);
                        vmWriter.arithmetic('~');
                    }
                } else {
                    error("true|false|null");
                }
                break;
            default:
                error("int constant | String constant | true | false | null");
        }
        tokenizer.advance();
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
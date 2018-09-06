import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    Map<String, Symbol> classSymbols = new HashMap<>();
    Map<Kind, Integer> classIndexes = new EnumMap<Kind, Integer>(Kind.class);
    Map<String, Symbol> methodSymbols = null;
    Map<Kind, Integer> methodIndexes = new EnumMap<Kind, Integer>(Kind.class);

    public SymbolTable() {}

    public int getClassSize() {
        return (int)classSymbols.values().stream().filter(symbol -> symbol.kind == Kind.FIELD).count();
    }

    public void put(String name, String type, Kind kind) {
        Map<String, Symbol> symbols;
        Map<Kind, Integer> indexes;

        if (methodSymbols == null) {
            // class symbols
            symbols = classSymbols;
            indexes = classIndexes;
        } else {
            symbols = methodSymbols;
            indexes = methodIndexes;
        }
        int index = indexes.getOrDefault(kind, 0);
        symbols.put(name, new Symbol(name, type, kind, index));
        indexes.put(kind, index + 1);
    }

    public void startSubroutine() {
        methodSymbols = new HashMap<>();
        methodIndexes = new EnumMap<Kind, Integer>(Kind.class);
    }
    public int varCount(Kind kind) {
        if (methodIndexes != null) {
            if (methodIndexes.containsKey(kind)) {
                return methodIndexes.get(kind);
            }
        }
        return classIndexes.getOrDefault(kind, 0);
    }

    public Kind kindOf(String name) {
        Symbol symbol = methodSymbols.get(name);
        if (symbol == null) {
            symbol = classSymbols.get(name);
        }
        return symbol.kind;
    }
    public String typeOf(String name) {
        Symbol symbol = methodSymbols.get(name);
        if (symbol == null) {
            symbol = classSymbols.get(name);
        }
        return symbol.type;
    }
    public int indexOf(String name) {
        Symbol symbol = methodSymbols.get(name);
        if (symbol == null) {
            symbol = classSymbols.get(name);
        }
        if (symbol == null) {
            throw new NullPointerException(name + " should not be null");
        }
        return symbol.index;
    }
    public boolean contains(String identifier) {
        return classSymbols.containsKey(identifier) || (methodSymbols != null && methodSymbols.containsKey(identifier));
    }
    private static class Symbol {
        String name;
        String type;
        Kind kind;
        int index;
        Symbol(String name, String type, Kind kind, int index) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
        @Override
        public String toString() {
            return "Symbol{name:"+name+" type:"+type+" kind:"+kind.name()+" index:"+index+"}";
        }
    }
}

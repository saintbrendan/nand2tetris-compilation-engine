import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    Map<String, Symbol> classSymbols = new HashMap<>();
    Map<String, Symbol> methodSymbols = null;
    Map<Kind, Integer> indexes = new EnumMap<Kind, Integer>(Kind.class);

    public SymbolTable() {}
    public void put(String name, String type, Kind kind) {
        if (methodSymbols != null) {

        } else {
            int index = indexes.getOrDefault(kind, 0);
            classSymbols.put(name, new Symbol(name, type, kind, index));
            indexes.put(kind, index + 1);
        }
    }
    public void startSubroutine() {
        methodSymbols = new HashMap<>();
        indexes = new EnumMap<Kind, Integer>(Kind.class);
    }
    public int varCount(Kind kind) {
        return indexes.getOrDefault(kind, 0);
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
        return symbol.index;
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

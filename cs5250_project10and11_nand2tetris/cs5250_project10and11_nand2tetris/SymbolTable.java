package cs5250_project10and11_nand2tetris;

import java.util.HashMap;
import java.util.Map;

// PROJECT 11 SESSION
// Reference: https://github.com/zhixiangli/nand2tetris/blob/main/projects/11/symbol_table.py

public class SymbolTable {
    public enum SymbolKind {
        STATIC("static"),
        FIELD("this"),
        ARGUMENT("argument"),
        LOCAL("local");

        private final String vmSegment;

        SymbolKind(String vmSegment) {
            this.vmSegment = vmSegment;
        }

        public String getVMSegment() {
            return vmSegment;
        }
    }

    private static class Symbol {
        final String type;
        final SymbolKind kind;
        final int index;

        Symbol(String type, SymbolKind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private final Map<String, Symbol> classScope;
    private final Map<String, Symbol> subroutineScope;

    public SymbolTable() {
        classScope = new HashMap<>();
        subroutineScope = new HashMap<>();
    }

    public void startSubroutine() {
        subroutineScope.clear();
    }

    public int varCount(SymbolKind kind) {
        int count = 0;

        if (kind == SymbolKind.STATIC || kind == SymbolKind.FIELD) {
            for (Symbol symbol : classScope.values()) {
                if (symbol.kind == kind) count++;
            }
        } else if (kind == SymbolKind.ARGUMENT || kind == SymbolKind.LOCAL) {
            for (Symbol symbol : subroutineScope.values()) {
                if (symbol.kind == kind) count++;
            }
        }

        return count;
    }

    public void define(String name, String type, SymbolKind kind) {
        int index = varCount(kind);
        Symbol symbol = new Symbol(type, kind, index);

        if (kind == SymbolKind.STATIC || kind == SymbolKind.FIELD) {
            classScope.put(name, symbol);
        } else if (kind == SymbolKind.ARGUMENT || kind == SymbolKind.LOCAL) {
            subroutineScope.put(name, symbol);
        } else {
            throw new IllegalArgumentException("Invalid kind: " + kind);
        }
    }

    public SymbolKind kindOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).kind;
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).kind;
        } else {
            return null;
        }
    }

    public String typeOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).type;
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).type;
        } else {
            return null;
        }
    }

    public int indexOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).index;
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).index;
        } else {
            return -1;
        }
    }

    // This method is used to resolve the symbol name in the current scope.
    // It first checks the subroutine scope, and if not found, it checks the class scope.
    private Symbol resolve(String name) {
        // GetOrDefault is used to return the value if present, or null if not found.
        return subroutineScope.getOrDefault(name, classScope.get(name));
    }
}


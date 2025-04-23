package cs5250_project10and11_nand2tetris;

import java.io.*;
import java.util.*;

// Reference: https://github.com/zhixiangli/nand2tetris/blob/main/projects/11/parser.py

public class CompilationEngine {
    private final Tokenizer tokenizer;
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable = new SymbolTable();
    private String className;
    private String subroutineName;
    private String subroutineType;
    private int labelCounter = 0;

    public CompilationEngine(File inputFile, File outputFile) throws IOException {
        this.tokenizer = new Tokenizer(inputFile);
        this.vmWriter = new VMWriter(new PrintWriter(outputFile));
        tokenizer.advance(); // Start with first token
        compileClass();
        vmWriter.close();
    }

    private void compileClass() {
        tokenizer.advance(); // 'class'
        className = tokenizer.identifier();
        tokenizer.advance(); // className
        tokenizer.advance(); // '{'

        while (tokenizer.tokenType().equals("KEYWORD") &&
               (tokenizer.keyWord().equals("static") || tokenizer.keyWord().equals("field"))) {
            compileClassVarDec();
        }

        while (tokenizer.tokenType().equals("KEYWORD") &&
               (tokenizer.keyWord().equals("constructor") ||
                tokenizer.keyWord().equals("function") ||
                tokenizer.keyWord().equals("method"))) {
            compileSubroutine();
        }

        tokenizer.advance(); // '}'
    }

    private void compileClassVarDec() {
        String kind = tokenizer.keyWord(); // 'static' or 'field'
        tokenizer.advance();
        String type = tokenizer.identifier(); // type
        tokenizer.advance();
        String name = tokenizer.identifier(); // varName
        tokenizer.advance();

        symbolTable.define(name, type, SymbolTable.SymbolKind.valueOf(kind.toUpperCase()));

        while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
            tokenizer.advance(); // ','
            name = tokenizer.identifier();
            tokenizer.advance();
            symbolTable.define(name, type, SymbolTable.SymbolKind.valueOf(kind.toUpperCase()));
        }

        tokenizer.advance(); // ';'
    }

    private void compileSubroutine() {
        // Debug
        // System.out.println("Found subroutine in class " + className + ": " + tokenizer.getCurrentToken());

        symbolTable.startSubroutine();

        subroutineType = tokenizer.keyWord(); // constructor/function/method
        tokenizer.advance(); // return type
        tokenizer.advance(); // subroutine name
        subroutineName = tokenizer.identifier();
        tokenizer.advance(); // '('

        if (subroutineType.equals("method")) {
            symbolTable.define("this", className, SymbolTable.SymbolKind.ARGUMENT);
        }

        compileParameterList();
        tokenizer.advance(); // ')'
        compileSubroutineBody();

        tokenizer.advance(); // '}'
    }

    private void compileParameterList() {
        if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(')')) return;

        String type = tokenizer.identifier();
        tokenizer.advance();
        String name = tokenizer.identifier();
        tokenizer.advance();
        symbolTable.define(name, type, SymbolTable.SymbolKind.ARGUMENT);

        while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
            tokenizer.advance(); // ','
            type = tokenizer.identifier();
            tokenizer.advance();
            name = tokenizer.identifier();
            tokenizer.advance();
            symbolTable.define(name, type, SymbolTable.SymbolKind.ARGUMENT);
        }
    }

    private void compileSubroutineBody() {
        tokenizer.advance(); // '{'
        while (tokenizer.tokenType().equals("KEYWORD") && tokenizer.keyWord().equals("var")) {
            compileVarDec();
        }

        System.out.println("Writing VM function: " + className + "." + subroutineName);
        vmWriter.writeFunction(className + "." + subroutineName, symbolTable.varCount(SymbolTable.SymbolKind.LOCAL));        

        if (subroutineType.equals("constructor")) {
            int fields = symbolTable.varCount(SymbolTable.SymbolKind.FIELD);
            vmWriter.writePush("constant", fields);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        } else if (subroutineType.equals("method")) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);
        }

        compileStatements();
        tokenizer.advance(); // '}'
    }

    private void compileVarDec() {
        tokenizer.advance(); // 'var'
        String type = tokenizer.identifier();
        tokenizer.advance();
        String name = tokenizer.identifier();
        tokenizer.advance();
        symbolTable.define(name, type, SymbolTable.SymbolKind.LOCAL);

        while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
            tokenizer.advance(); // ','
            name = tokenizer.identifier();
            tokenizer.advance();
            symbolTable.define(name, type, SymbolTable.SymbolKind.LOCAL);
        }

        tokenizer.advance(); // ';'
    }
    
    private void compileStatements() {
        while (tokenizer.tokenType().equals("KEYWORD")) {
            String keyWord = tokenizer.keyWord();

            // Debug
            // System.out.println("Begin compiling statements");
            // System.out.println("Current token type: " + tokenizer.tokenType());
            // System.out.println("Current token: " + tokenizer.getCurrentToken());

            if (keyWord.equals("let")) {
                compileLet();
                System.out.println("Next token in compileStatements: " + tokenizer.getCurrentToken());
            } else if (keyWord.equals("if")) {
                compileIf();
            } else if (keyWord.equals("while")) {
                compileWhile();
            } else if (keyWord.equals("do")) {
                compileDo();
            } else if (keyWord.equals("return")) {
                compileReturn();
            } else {
                break; // No more statements to compile
            }
        }
    }

    private void compileLet() {
        tokenizer.advance(); // 'let'
        String name = tokenizer.identifier();
        tokenizer.advance(); // varName

        boolean isArray = false;
        if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs('[')) {
            isArray = true;
            tokenizer.advance(); // '['
            compileExpression();
            tokenizer.advance(); // ']'
            SymbolTable.SymbolKind kind = symbolTable.kindOf(name);
            int index = symbolTable.indexOf(name);
            vmWriter.writePush(kind.getVMSegment(), index);
            vmWriter.writeArithmetic("add");
        }

        tokenizer.advance(); // '='
        compileExpression();
        tokenizer.advance(); // ';'

        SymbolTable.SymbolKind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);

        if (isArray) {
            vmWriter.writePop("temp", 0);
            vmWriter.writePop("pointer", 1);
            vmWriter.writePush("temp", 0);
            vmWriter.writePop("that", 0);
        } else {
            vmWriter.writePop(kind.getVMSegment(), index);
        }
    }

    private void compileIf() {
        int label = labelCounter++;
        String labelTrue = "IF_TRUE" + label;
        String labelFalse = "IF_FALSE" + label;
        String labelEnd = "IF_END" + label;

        tokenizer.advance(); // 'if'
        tokenizer.advance(); // '('
        compileExpression();
        tokenizer.advance(); // ')'

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);

        tokenizer.advance(); // '{'
        compileStatements();
        tokenizer.advance(); // '}'

        if (tokenizer.tokenType().equals("KEYWORD") && tokenizer.keyWord().equals("else")) {
            vmWriter.writeGoto(labelEnd);
            vmWriter.writeLabel(labelFalse);
            tokenizer.advance(); // 'else'
            tokenizer.advance(); // '{'
            compileStatements();
            tokenizer.advance(); // '}'
            vmWriter.writeLabel(labelEnd);
        } else {
            vmWriter.writeLabel(labelFalse);
        }
    }

    private void compileWhile() {
        int label = labelCounter++;
        String labelExp = "WHILE_EXP" + label;
        String labelEnd = "WHILE_END" + label;

        vmWriter.writeLabel(labelExp);

        tokenizer.advance(); // 'while'
        tokenizer.advance(); // '('
        compileExpression();
        tokenizer.advance(); // ')'

        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(labelEnd);

        tokenizer.advance(); // '{'
        compileStatements();
        tokenizer.advance(); // '}'

        vmWriter.writeGoto(labelExp);
        vmWriter.writeLabel(labelEnd);
    }

    private void compileDo() {
        tokenizer.advance(); // 'do'
        String name = tokenizer.identifier();
        tokenizer.advance();

        int nArgs = 0;
        if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs('.')) {
            tokenizer.advance(); // '.'
            String method = tokenizer.identifier();
            tokenizer.advance();
            name = name + "." + method;
        } else {
            name = className + "." + name;
            vmWriter.writePush("pointer", 0);
            nArgs++;
        }

        tokenizer.advance(); // '('
        nArgs += compileExpressionList();
        tokenizer.advance(); // ')'
        tokenizer.advance(); // ';'

        vmWriter.writeCall(name, nArgs);
        vmWriter.writePop("temp", 0); // discard return value
    }

    private void compileReturn() {
        tokenizer.advance(); // 'return'
        if (!(tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(';'))) {
            compileExpression();
        } else {
            vmWriter.writePush("constant", 0);
        }

        tokenizer.advance(); // ';'
        vmWriter.writeReturn();
    }

    private int compileExpressionList() {
        int count = 0;
        if (!(tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(')'))) {
            compileExpression();
            count++;

            while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
                tokenizer.advance(); // ','
                compileExpression();
                count++;
            }
        }
        
        return count;
    }

    private void compileExpression() {
        compileTerm();
        while (tokenizer.tokenType().equals("SYMBOL") &&
               "+-*/&|<>=~".contains(String.valueOf(tokenizer.symbol()))) {
            String op = tokenizer.getCurrentToken();
            tokenizer.advance();
            compileTerm();

            if (op.equals("+")) {
                vmWriter.writeArithmetic("add");
            } else if (op.equals("-")) {
                vmWriter.writeArithmetic("sub");
            } else if (op.equals("*")) {
                vmWriter.writeCall("Math.multiply", 2);
            } else if (op.equals("/")) {
                vmWriter.writeCall("Math.divide", 2);
            } else if (op.equals("&")) {
                vmWriter.writeArithmetic("and");
            } else if (op.equals("|")) {
                vmWriter.writeArithmetic("or");
            } else if (op.equals("<")) {
                vmWriter.writeArithmetic("lt");
            } else if (op.equals(">")) {
                vmWriter.writeArithmetic("gt");
            } else if (op.equals("=")) {
                vmWriter.writeArithmetic("eq");
            }
        }
    }

    private void compileTerm() {
        String type = tokenizer.tokenType();
        if (type.equals("INT_CONST")) {
            vmWriter.writePush("constant", tokenizer.intVal());
            tokenizer.advance();
        } else if (type.equals("STRING_CONST")) {
            String value = tokenizer.stringVal();
            tokenizer.advance();
            vmWriter.writePush("constant", value.length());
            vmWriter.writeCall("String.new", 1);
            for (char c : value.toCharArray()) {
                vmWriter.writePush("constant", (int) c);
                vmWriter.writeCall("String.appendChar", 2);
            }
        } else if (type.equals("KEYWORD")) {
            String keyword = tokenizer.keyWord();
            switch (keyword) {
                case "true" -> {
                    vmWriter.writePush("constant", 0);
                    vmWriter.writeArithmetic("not");
                }
                case "false", "null" -> vmWriter.writePush("constant", 0);
                case "this" -> vmWriter.writePush("pointer", 0);
            }
            tokenizer.advance();
        } else if (type.equals("IDENTIFIER")) {
            String name = tokenizer.identifier();
            tokenizer.advance();

            if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs('[')) {
                tokenizer.advance();
                compileExpression();
                tokenizer.advance();
                SymbolTable.SymbolKind kind = symbolTable.kindOf(name);
                int index = symbolTable.indexOf(name);
                vmWriter.writePush(kind.getVMSegment(), index);
                vmWriter.writeArithmetic("add");
                vmWriter.writePop("pointer", 1);
                vmWriter.writePush("that", 0);
            } else if (tokenizer.tokenType().equals("SYMBOL") &&
                       (tokenizer.symbolIs('(') || tokenizer.symbolIs('.'))) {
                String fullName = name;
                int nArgs = 0;
                if (tokenizer.symbolIs('.')) {
                    tokenizer.advance();
                    fullName += "." + tokenizer.identifier();
                    tokenizer.advance();
                } else {
                    fullName = className + "." + name;
                    vmWriter.writePush("pointer", 0);
                    nArgs++;
                }
                tokenizer.advance(); // '('
                nArgs += compileExpressionList();
                tokenizer.advance(); // ')'
                vmWriter.writeCall(fullName, nArgs);
            } else {
                SymbolTable.SymbolKind kind = symbolTable.kindOf(name);
                int index = symbolTable.indexOf(name);
                vmWriter.writePush(kind.getVMSegment(), index);
            }
        } else if (type.equals("SYMBOL") && tokenizer.symbolIs('(')) {
            tokenizer.advance(); // '('
            compileExpression();
            tokenizer.advance(); // ')'
        } else if (type.equals("SYMBOL") && (tokenizer.symbolIs('-') || tokenizer.symbolIs('~'))) {
            String unaryOp = tokenizer.getCurrentToken();
            tokenizer.advance();
            compileTerm();
            vmWriter.writeArithmetic(unaryOp.equals("-") ? "neg" : "not");
        }
    }
}

package cs5250_project10and11;

import java.io.*;

// Code Generation & Syntax Analysis
// Ref: https://github.com/zhixiangli/nand2tetris/blob/main/projects/10/parser.py
public class Parser {
	private Tokenizer tokenizer;
    private PrintWriter writer;

    public Parser(File input, File output) throws IOException {
        tokenizer = new Tokenizer(input);	// Import Tokenizer.class
        writer = new PrintWriter(output);	// Print output to file
        
        tokenizer.advance(); // Start the first token
        compileClass();
        writer.close();
    }

    private void writeTag(String tag, String content) {
        writer.printf("<%s> %s </%s>%n", tag, escapeXml(content), tag);
    }

    private void writeToken() {
        String type = tokenizer.tokenType();
        String value = switch (type) {
            case "KEYWORD"     -> tokenizer.keyWord();
            case "SYMBOL"      -> String.valueOf(tokenizer.symbol());
            case "IDENTIFIER"  -> tokenizer.identifier();
            case "INT_CONST"   -> String.valueOf(tokenizer.intVal());
            case "STRING_CONST"-> tokenizer.stringVal();
            default            -> "";
        };

        writeTag(xmlTag(type), value);
        tokenizer.advance();
    }

    private void compileClass() {
        writer.println("<class>");

        writeToken(); // 'class'
        writeToken(); // className (identifier)
        writeToken(); // '{'

        while (tokenizer.tokenType().equals("KEYWORD") &&
              (tokenizer.keyWord().equals("static") || tokenizer.keyWord().equals("field"))) {
            compileClassVarDec();
        }

        while (tokenizer.tokenType().equals("KEYWORD") &&
              (tokenizer.keyWord().equals("constructor") || tokenizer.keyWord().equals("function") || tokenizer.keyWord().equals("method"))) {
            compileSubroutine();
        }

        writeToken(); // '}'
        writer.println("</class>");
    }

    
    // Compile variable declarations.
    private void compileVarDec() {
        writer.println("<varDec>");
        while (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs(';')) {
            writeToken();
        }
        writeToken(); // ';'
        writer.println("</varDec>");
    }

    private void compileClassVarDec() {
        writer.println("<classVarDec>");
        while (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs(';')) {
            writeToken();
        }
        writeToken(); // ';'
        writer.println("</classVarDec>");
    }


    private void compileSubroutine() {
        writer.println("<subroutineDec>");
        while (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs('(')) {
            writeToken();
        }
        writeToken(); // (
        compileParameterList();
        writeToken(); // )
    
        writer.println("<subroutineBody>");
        writeToken(); // {
        while (tokenizer.tokenType().equals("KEYWORD") && tokenizer.keyWord().equals("var")) {
            compileVarDec();
        }
        compileStatements(); 
        writeToken(); // }
        writer.println("</subroutineBody>");
        writer.println("</subroutineDec>");
    }

    private void compileParameterList() {
        writer.println("<parameterList>");
        if (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs(')')) {
            writeToken(); // type
            writeToken(); // varName
    
            while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
                writeToken(); // ,
                writeToken(); // type
                writeToken(); // varName
            }
        }
        writer.println("</parameterList>");
    }

    
    private void compileStatements() {
        writer.println("<statements>");
        while (tokenizer.tokenType().equals("KEYWORD")) {
            String kw = tokenizer.keyWord();
            if (kw.equals("let")) {
                compileLet();
            } else if (kw.equals("if")) {
                compileIf();
            } else if (kw.equals("while")) {
                compileWhile();
            } else if (kw.equals("do")) {
                compileDo();
            } else if (kw.equals("return")) {
                compileReturn();
            } else {
                return; // Ignore
            }
        }
        writer.println("</statements>");
    }

    private void compileLet() {
        writer.println("<letStatement>");
        writeToken(); // 'let'
        writeToken(); // varName
    
        if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs('[')) {
            writeToken(); // '['
            compileExpression();
            writeToken(); // ']'
        }
    
        writeToken(); // '='
        compileExpression();
        writeToken(); // ';'
        writer.println("</letStatement>");
    }

    private void compileIf() {
        writer.println("<ifStatement>");
        writeToken(); // 'if'
        writeToken(); // '('
        compileExpression();
        writeToken(); // ')'
        writeToken(); // '{'
        compileStatements();
        writeToken(); // '}'
    
        if (tokenizer.tokenType().equals("KEYWORD") && tokenizer.keyWord().equals("else")) {
            writeToken(); // 'else'
            writeToken(); // '{'
            compileStatements();
            writeToken(); // '}'
        }
    
        writer.println("</ifStatement>");
    }

    private void compileWhile() {
        writer.println("<whileStatement>");
        writeToken(); // 'while'
        writeToken(); // '('
        compileExpression();
        writeToken(); // ')'
        writeToken(); // '{'
        compileStatements();
        writeToken(); // '}'
        writer.println("</whileStatement>");
    }

    private void compileDo() {
        writer.println("<doStatement>");
        writeToken(); // 'do'
        writeToken(); // subroutineName or className or varName
        if (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs('.')) {
            writeToken(); // '.'
            writeToken(); // subroutineName
        }
        writeToken(); // '('
        compileExpressionList();
        writeToken(); // ')'
        writeToken(); // ';'
        writer.println("</doStatement>");
    }

    private void compileReturn() {
        writer.println("<returnStatement>");
        writeToken(); // 'return'
        if (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs(';')) {
            compileExpression();
        }
        writeToken(); // ';'
        writer.println("</returnStatement>");
    }    


    private void compileTerm() {
        writer.println("<term>");
    
        String type = tokenizer.tokenType();
    
        if (type.equals("INT_CONST")) {
            writeToken();
        } else if (type.equals("STRING_CONST")) {
            writeToken();
        } else if (type.equals("KEYWORD")) {
            writeToken(); // true, false, null, this
        } else if (type.equals("IDENTIFIER")) {
            writeToken(); // varName or subroutineName or className
    
            if (tokenizer.tokenType().equals("SYMBOL")) {
                if (tokenizer.symbolIs('[')) {
                    writeToken(); // '['
                    compileExpression();
                    writeToken(); // ']'
                } else if (tokenizer.symbolIs('(')) {
                    writeToken(); // '('
                    compileExpressionList();
                    writeToken(); // ')'
                } else if (tokenizer.symbolIs('.')) {
                    writeToken(); // '.'
                    writeToken(); // subroutineName
                    writeToken(); // '('
                    compileExpressionList();
                    writeToken(); // ')'
                }
            }
        } else if (type.equals("SYMBOL") && tokenizer.symbolIs('(')) {
            writeToken(); // '('
            compileExpression();
            writeToken(); // ')'
        } else if (type.equals("SYMBOL") && (tokenizer.symbolIs('-') || tokenizer.symbolIs('~'))) {
            writeToken(); // unaryOp
            compileTerm();
        }
    
        writer.println("</term>");
    }    


    private void compileExpression() {
        writer.println("<expression>");
        compileTerm();
        while (tokenizer.tokenType().equals("SYMBOL") &&
               "+-*/&|<>=~".contains(String.valueOf(tokenizer.symbol()))) {
            writeToken(); // operator
            compileTerm();
        }
        writer.println("</expression>");
    }   
    
    private void compileExpressionList() {
        writer.println("<expressionList>");
        if (!tokenizer.tokenType().equals("SYMBOL") || !tokenizer.symbolIs(')')) {
            compileExpression();
            while (tokenizer.tokenType().equals("SYMBOL") && tokenizer.symbolIs(',')) {
                writeToken(); // ','
                compileExpression();
            }
        }
        writer.println("</expressionList>");
    }    

    private static String xmlTag(String type) {
    	if (type.equals("INT_CONST")) {
            return "integerConstant";
        } else if (type.equals("STRING_CONST")) {
            return "stringConstant";
        } else if (type.equals("KEYWORD")) {
            return "keyword";
        } else if (type.equals("SYMBOL")) {
            return "symbol";
        } else if (type.equals("IDENTIFIER")) {
            return "identifier";
        } else {
            return "unknown";
        }
    }

    private static String escapeXml(String input) {
        if (input.contains("&")) {
            input = input.replace("&", "&amp;");
        }
        else if (input.contains("<")) {
            input = input.replace("<", "&lt;");
        }
        else if (input.contains(">")) {
            input = input.replace(">", "&gt;");
        }
        else if (input.contains("\"")) {
            input = input.replace("\"", "&quot;");
        }
        return input;
    }

}

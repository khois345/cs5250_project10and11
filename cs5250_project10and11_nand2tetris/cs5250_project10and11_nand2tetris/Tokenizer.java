package cs5250_project10and11_nand2tetris;

import java.io.*;
import java.util.*;
import java.util.regex.*;

// Lexical Analysis
public class Tokenizer {
    private List<String> tokens;
    private int currentIndex = -1;
    private String currentToken;
    
    private static final Set<String> keywords = Set.of(
    	"class",
    	"constructor", 
    	"function", 
    	"method", 
    	"field", 
    	"static", 
    	"var",
        "int", 
        "char", 
        "boolean", 
        "void", 
        "true", 
        "false", 
        "null", 
        "this",
        "let", 
        "do", 
        "if", 
        "else", 
        "while", 
        "return"
    );
    
    private static final Set<Character> symbols = Set.of(
            '{', '}', '(', ')', '[', ']', '.', ',', ';',
            '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'
        );
    
    private static final Pattern tokenPattern = Pattern.compile(
            "\"[^\"]*\"|\\d+|[a-zA-Z_][a-zA-Z_0-9]*|\\p{Punct}"
        );
    
    public Tokenizer(File inputFile) throws IOException {
    	tokens = new ArrayList<>();
        StringBuilder rawCode = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rawCode.append(line).append("\n");
            }
        }

        // Remove all comments from the code
        String cleaned = removeComments(rawCode.toString());

        // Match and store all valid tokens
        Matcher matcher = tokenPattern.matcher(cleaned);
        while (matcher.find()) {
            String token = matcher.group();
            tokens.add(token);
        }
    }

    // Remove all line and block comments using regex
    private static String removeComments(String input) {
        return input.replaceAll("//.*", "")
                    .replaceAll("/\\*.*?\\*/", "")          // inline block
                    .replaceAll("/\\*([\\s\\S]*?)\\*/", ""); // multiline block
    }

    public boolean hasMoreTokens() {
        return currentIndex < tokens.size() - 1;
    }

    public void advance() {
    	if (hasMoreTokens()) {
            currentIndex++;
            currentToken = tokens.get(currentIndex);
        } else {
            currentToken = null;
        }
    }

    public String tokenType() {
        if (currentToken == null) 
        	return "NULL";

        if (keywords.contains(currentToken)) 
        	return "KEYWORD";
        else if (currentToken.length() == 1 && symbols.contains(currentToken.charAt(0))) 
        	return "SYMBOL";
        else if (currentToken.matches("^\\d+$")) 
        	return "INT_CONST";
        else if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) 
        	return "STRING_CONST";
        else 
        	return "IDENTIFIER";
    }
    
    // Returns the current token as a string.
    public String keyWord() { 
    	return currentToken; 
    }
    
    // Returns the current token as a char, if it is a symbol.
    public char symbol() { 
    	return currentToken.charAt(0); 
    }
    
    // Return user-defined identifier as a string.
    // This includes class names, variable names, and subroutine names.
    public String identifier() { 
    	return currentToken; 
    }
    
    //  Returns the current token as an int, if it is an integer constant.
    public int intVal() { 
    	return Integer.parseInt(currentToken); 
    }
    
    // Returns the current token as a string, if it is a string constant.
    public String stringVal() {
        return currentToken.substring(1, currentToken.length() - 1);
    }

    public String getCurrentToken() {
        return currentToken;
    }
    
    // Returns true if the current token is a keyword.
    public boolean keywordIs(String keyword) {
        return currentToken.equals(keyword);
    }

    // Returns true if the current token is a symbol.
    public boolean symbolIs(String symbol) {
        return currentToken.equals(symbol);
    }

    // Returns true if the current token is a symbol.
    // This is used for single character symbols.
    public boolean symbolIs(char c) {
        return currentToken.length() == 1 && currentToken.charAt(0) == c;
    }
    
    // Exporting XML file for Tokenizers
    public static void exportTokensToXml(File inputFile, File outputFile) {
        try {
            Tokenizer tokenizer = new Tokenizer(inputFile);
            PrintWriter writer = new PrintWriter(outputFile);
            writer.println("<tokens>");

            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                String type = tokenizer.tokenType();
                String value;

                if (type.equals("KEYWORD")) {
                    value = tokenizer.keyWord();
                    writer.printf("<keyword> %s </keyword>%n", escapeXml(value));
                } else if (type.equals("SYMBOL")) {
                    value = String.valueOf(tokenizer.symbol());
                    writer.printf("<symbol> %s </symbol>%n", escapeXml(value));
                } else if (type.equals("INT_CONST")) {
                    value = String.valueOf(tokenizer.intVal());
                    writer.printf("<integerConstant> %s </integerConstant>%n", value);
                } else if (type.equals("STRING_CONST")) {
                    value = tokenizer.stringVal();
                    writer.printf("<stringConstant> %s </stringConstant>%n", value);
                } else if (type.equals("IDENTIFIER")) {
                    value = tokenizer.identifier();
                    writer.printf("<identifier> %s </identifier>%n", value);
                }
            }

            writer.println("</tokens>");
            writer.close();
            System.out.println("Token output written to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to export tokens: " + e.getMessage());
        }
    }

    private static String escapeXml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
        			.replace("\'", "&apos;");

    }
}

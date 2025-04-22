package cs5250_project10and11;

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
        StringBuilder cleaned = new StringBuilder();	// Mutable version of String
        
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            boolean inBlockComment = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                
                // Block comment start
                if (line.startsWith("/*") || line.startsWith("/**")) {
                    inBlockComment = true;
                    continue;
                }
                if (inBlockComment) {
                    if (line.endsWith("*/")) inBlockComment = false;
                    continue;
                }
                
                // Single-line comment
                if (line.startsWith("//")) continue;
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                
                cleaned.append(line).append(" ");
            }
            
            reader.close();
        }
        
        Matcher matcher = tokenPattern.matcher(cleaned.toString());
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
    }

    public boolean hasMoreTokens() {
        return currentIndex < tokens.size() - 1;
    }

    public void advance() {
        if (hasMoreTokens()) {
            currentIndex++;
            currentToken = tokens.get(currentIndex);
        }
    }

    public String tokenType() {
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
}

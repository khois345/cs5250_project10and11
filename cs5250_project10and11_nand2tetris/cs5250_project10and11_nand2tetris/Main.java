package cs5250_project10and11_nand2tetris;
import java.util.*;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
    	Scanner scanner = new Scanner(System.in);
    	
    	System.out.println("Welcome to Jack Compiler");
    	
    	while(true)
    	{
            System.out.print("Enter the .jack file or folder name (or type '/exit' to quit): ");
            String inputPath  = scanner.nextLine().trim();
            
            if (inputPath .equalsIgnoreCase("/exit")) {
                System.out.println("Exiting...");
                scanner.close();
                break;
            }
            
            File inputFile = new File(inputPath);
            System.out.println("Checking: " + inputFile.getAbsolutePath());
            if (!inputFile.isAbsolute()) {
                File projectRoot = new File(System.getProperty("user.dir"));
                inputPath = inputPath.startsWith("/") ? inputPath.substring(1) : inputPath;
                inputFile = new File(projectRoot, inputPath);
            }

            if (!inputFile.exists()) {
                System.out.println("File or folder does not exist.");
                continue;
            }
            
            List<File> jackFiles = new ArrayList<>();
            if (inputFile.isDirectory()) {
                for (File file : Objects.requireNonNull(inputFile.listFiles())) {
                    if (file.getName().endsWith(".jack")) {
                        jackFiles.add(file);
                    }
                }
            } else if (inputFile.getName().endsWith(".jack")) {
                jackFiles.add(inputFile);
            } else {
                System.out.println("This is not a .jack file.");
                continue;
            }

            for (File jack : jackFiles) {
                try {
                	String tokenXmlName = jack.getName().replace(".jack", "T.xml");
                	File tokenOutput = new File(jack.getParentFile(), tokenXmlName);
                	Tokenizer.exportTokensToXml(jack, tokenOutput);

                    String outputName = jack.getName().replace(".jack", ".xml");
                    File outputFile = new File(jack.getParentFile(), outputName);
                    new Parser(jack, outputFile); // Parses and writes XML
                    System.out.println("Saving XML to: " + outputFile.getAbsolutePath());
                    System.out.println("Parsed: " + outputFile.getName());
                } catch (IOException e) {
                    System.err.println("Error parsing file: " + jack.getName());
                    e.printStackTrace();
                }
            }            
        }
        scanner.close();
    }
}


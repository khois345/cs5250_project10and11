package cs5250_project10and11;
import java.util.Scanner;
import java.io.File;

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
            
            File inputFile  = new File(inputPath);
            if (!inputFile.isAbsolute()) {
                File projectRoot = new File(System.getProperty("user.dir"));
                inputPath = inputPath.startsWith("/") ? inputPath.substring(1) : inputPath;
                inputFile = new File(projectRoot, inputPath);
            }
            
            if (!inputFile.exists()) {
            	System.out.print("Error: File or directory is not found");
            	continue;
            }
            
            // To-Do
    	}
    }
}

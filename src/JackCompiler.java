import java.io.File;
import java.io.IOException;

public class JackCompiler {
    public static String fileName = "C:\\temp\\11\\Seven";
    //public static String fileName = "C:\\Users\\saint\\IdeaProjects\\CompilationEngine\\out\\production\\CompilationEngine\\";

    public static void main(String[] args) throws IOException {
        if (args.length >= 1) {
            fileName = args[0];
        }
        File inFile = new File(fileName);
        if (inFile.isDirectory()) {
            compileDirectory(inFile);
        } else {
            compileFile(inFile);
        }
    }

    private static void compileFile(File inFile) throws IOException {
        String inputFileName = inFile.getAbsolutePath();
        String outputFileName = DirUtil.stripExtension(inputFileName) + ".xml";
        String outputVmFileName = DirUtil.stripExtension(inputFileName) + ".vm";
        System.out.println("     fileName:" + fileName + "\ninputFileName:" + inputFileName);
        File outFile = new File(outputFileName);
        File outVmFile = new File(outputVmFileName);
        CompilationEngine c = new CompilationEngine(inFile, outFile, outVmFile);
        c.compileClass();
    }

    private static void compileDirectory(File directory) throws IOException {
        File[] filesx = DirUtil.getFilesFromDir(directory, "jack");
        for (File file : DirUtil.getFilesFromDir(directory, "jack")) {
            compileFile(file);
        }
    }


}
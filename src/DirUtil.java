import java.io.File;
import java.io.FilenameFilter;

public class DirUtil {
    static File[] getFilesFromDir(File directory, String extension) {
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // TODO prepend extension with '.', if not already there
                return name.endsWith(extension);
            }
        });
        return files;
    }

    static String getFileBase(String filename) {
        int iDot = filename.lastIndexOf('.');
        if (iDot > 0) {
            filename = filename.substring(0, iDot);
        }
        String[] pathParts = filename.split("\\\\");
        String pathPart = pathParts[pathParts.length - 1];
        pathParts = pathPart.split("/");
        return pathParts[pathParts.length - 1];
    }

    static String stripExtension(String filename) {
        int iDot = filename.lastIndexOf('.');
        if (iDot > 0) {
            filename = filename.substring(0, iDot);
        }
        return filename;
    }
}

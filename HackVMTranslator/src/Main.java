import java.io.File;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Main {
    public static void main(String[] args) {

        File input = new File(args[0]);
        if (!(input.exists() && input.isFile())) {
            System.out.println("Fail to open the file: " + args[0]);
            return ;
        }

        File out = new File(getSAMPath(args[0]));

        Parser p = new Parser(input);
        final CodeWriter writer = new CodeWriter(out);

        for (Command command : p) {
            writer.write(command);
        }
        writer.close();
    }

    // Xxx.vm to Xxx.asm
    private static String getSAMPath(String vmPath) {
        final int pos = vmPath.lastIndexOf('.');
        String rst = vmPath.substring(0, pos);
        return rst+".asm";
    }
}

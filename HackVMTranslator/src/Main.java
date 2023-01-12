import java.io.File;
import java.io.IOException;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Main {
    public static void main(String[] args){

        File input = new File(args[0]);
        if (!(input.exists())) {
            System.out.println("Fail to open the file/dir: " + args[0]);
            return ;
        }

        final File[] vmFiles;
        if (input.isDirectory()) {
            vmFiles = input.listFiles((dir, name) -> name.matches(".*\\.vm$"));
        } else { // input.isFile()
            vmFiles = new File[] {input};
        }

        File out = new File(getSAMPath(input));
        try(CodeWriter writer = new CodeWriter(out)) {
            assert vmFiles != null;
            writer.writeInit();
            for (File vmFile : vmFiles) {
                writer.setClassName(getClassName(vmFile.getName()));
                try (Parser p = new Parser(vmFile)){
                    while(p.hasNext()) {
                        writer.write(p.next());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Xxx.vm to Xxx
    private static String getClassName(String name) {
        final int pos = name.lastIndexOf('.');
        return name.substring(0, pos);
    }

    /**
     * assume in.exists()
     * Xxx or Xxx.vm to Xxx.asm
     */
    private static String getSAMPath(File in) {
        if(in.isFile()) {
            final int pos = in.getName().lastIndexOf('.');
            String rst = in.getName().substring(0, pos);
            return rst+".asm";
        } else if (in.isDirectory()) {
            return in.getPath() + File.separator + in.getName() + ".asm";
        } else throw new IllegalArgumentException();
    }
}

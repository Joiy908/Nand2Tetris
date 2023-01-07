import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Parser implements Iterable<Command> {

    private static class CommandIterator implements Iterator<Command> {
        private BufferedReader in;
        private String currLine;
        private Command currCommand;

        public CommandIterator(File input) {
            try {
                in = new BufferedReader(new FileReader(input));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasNext() {
            try {
                while ((currLine = in.readLine()) != null) {
                    currLine = rmComment(currLine);
                    if (currLine == null)
                        continue;
                    advance();
                    return true;
                }
                // if it runs out of Commands, close the Reader
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public Command next() {
            return currCommand;
        }

        /**
         * remove Comment and white space
         * assume line != null
         * @return null if line contains no command
         */
        private String rmComment(String line) {
            if (line.isEmpty()){
                return null;
            }
            // else
            line = line.trim();
            if (line.isEmpty()) {
                return null;
            }
            else if (line.charAt(0) == '/')
                return null;
            else { // has command
                int pos = line.indexOf('/');
                if (pos == -1) {
                    return line;
                } else {
                    return line.substring(0, pos).trim();
                }
            }
        }

        /**
         * parse currLine to currComment
         */
        private void advance() {
            currCommand = new Command(currLine.split(" "));
        }

    }

    private final File in;
    public Parser(File in) {
        this.in = in;
    }

    @Override
    public Iterator<Command> iterator() {
        return new CommandIterator(in);
    }

}

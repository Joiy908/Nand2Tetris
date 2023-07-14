import java.io.*;
import java.util.Iterator;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Parser implements Iterator<Command>, Closeable {

        private BufferedReader in;
        private String currLine;
        private Command currCommand;

        public Parser(File input) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public Command next() {
            return currCommand;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
        /**
         * remove Comment and white space
         * assume line != null
         * @return null if line contains no command
         */
        private String rmComment(String line) {
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

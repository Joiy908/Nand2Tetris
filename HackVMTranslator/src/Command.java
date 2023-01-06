import java.util.Arrays;
import java.util.List;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Command {
    enum CommandType {
        ARITHMETIC,
        PUSH,
        POP
    }
    public final static List<String> ARITHMETIC_COMMANDS = Arrays.asList("add", "sub", "ng",
            "and", "or", "not", "eq", "gt", "lt");

    public final static List<String> COMMON_SEGMENT = Arrays.asList("local", "argument", "this", "that");


    public CommandType type;
    public String name;
    public String arg1;
    public String arg2;

    public Command(String[] tokens) {
        if(tokens.length == 0)
            throw new IllegalArgumentException(Arrays.toString(tokens) + " is illegal");
        setNameAndType(tokens[0]);
        switch (tokens.length) {
            case 1:
                return;
            case 3:
                arg1 = tokens[1];
                arg2 = tokens[2];
                break;
            default:
                throw new IllegalArgumentException(Arrays.toString(tokens) +
                        " length is illegal");
        }
    }

    private void setNameAndType(String name) {
        this.name = name;
        if (name.equals("push")) {
            type = CommandType.PUSH;
        } else if (name.equals("pop")) {
            type = CommandType.POP;
        } else if (ARITHMETIC_COMMANDS.contains(name)) {
            type = CommandType.ARITHMETIC;
        } else {
            throw new IllegalArgumentException(name + "is an illegal command name.");
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", arg1='" + arg1 + '\'' +
                ", arg2='" + arg2 + '\'' +
                '}';
    }
}

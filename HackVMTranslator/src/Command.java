import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class Command {
    enum CommandType {
        ARITHMETIC,
        PUSH,
        POP,
        BRANCHING,
        METHOD
    }

    public final static List<String> ARITHMETIC_COMMANDS = Arrays.asList("add", "sub", "neg",
            "and", "or", "not", "eq", "gt", "lt");

    public final static List<String> BRANCHING_COMMANDS = Arrays.asList("if-goto", "goto", "label");
    public final static List<String> METHOD_COMMANDS = Arrays.asList("call", "function", "return");

    public final static HashMap<String, String> COMMON_SEGMENT = new HashMap<String, String>() {{
        put("local", "LCL");
        put("argument", "ARG");
        put("this", "THIS");
        put("that", "THAT");
    }};


    public final static HashMap<String, String> BINARY_OPERATIONS = new HashMap<String, String>() {{
        put("add", "D+M");
        put("sub", "M-D");
        put("and", "D&M");
        put("or", "D|M");
    }};

    public final static HashMap<String, String> COMPARE_OPERATIONS = new HashMap<String, String>() {{
        put("eq", "JEQ");
        put("lt", "JLT");
        put("gt", "JGT");
    }};

    public CommandType type;
    public String name;
    public String arg1;
    public String arg2;

    public Command(String[] tokens) {
        if (tokens.length == 0)
            throw new IllegalArgumentException(Arrays.toString(tokens) + " is illegal");
        setNameAndType(tokens[0]);
        switch (tokens.length) {
            case 1:
                return;
            case 2:
                arg1 = tokens[1];
                break;
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
        } else if (name.equals("return")) {
            type = CommandType.METHOD;
        } else if (ARITHMETIC_COMMANDS.contains(name)) {
            type = CommandType.ARITHMETIC;
        } else if (BRANCHING_COMMANDS.contains(name)) {
            type = CommandType.BRANCHING;
        } else if (METHOD_COMMANDS.contains(name)) {
            type = CommandType.METHOD;
        } else {
            throw new IllegalArgumentException(name + " is an illegal command name.");
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Joiy908
 * @date 2022/12/31
 */

public class CodeWriter {
    private FileOutputStream out;
    private final Charset OUT_FILE_CHARSET = StandardCharsets.US_ASCII;
    private final String HackClassName;
    private int staticCount;

    public CodeWriter(File f) {
        try {
            out = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // get HackClassName
        final int pos = f.getName().indexOf('.');
        HackClassName = f.getName().substring(0, pos);
    }

    public void write(Command command) {
        try {
            switch (command.type) {
                case ARITHMETIC:
                    writeArithmetic(command.name);
                    break;
                case PUSH:
                    writePush(command.arg1, command.arg2);
                    break;
                case POP:
                    writePop(command.arg1, command.arg2);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + command.type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePush(String seg, String i) throws IOException {
        if (Command.COMMON_SEGMENT.contains(seg)) {
            letR13EqAddr(seg, i);
            decrSP();
            letGoLabelXEqGoLabelY("R13", "SP");
        } else if (seg.equals("constant")) {
            // *SP = i
            String lines = "@" + i + "\nD=A\n@SP\nA=M\nM=D\n";
            out.write(lines.getBytes(OUT_FILE_CHARSET));
            incrSP();
        } else if (seg.equals("static")) {
            // *SP = @Xxx.i
            letGoLabelXEqAtY("SP", HackClassName + "." + i);
            incrSP();
        } else if (seg.equals("temp")) {
            int addr = 5 + Integer.parseInt(i);
            letGoLabelXEqAtY("SP", String.valueOf(addr));
            incrSP();
        } else if (seg.equals("pointer")) {
            if (i.equals("0")) {
                // "0" = this = R[3]
                letGoLabelXEqAtY("SP", "R3");
            } else if (i.equals("1")) {
                // "1" = that = R[4]
                letGoLabelXEqAtY("SP", "R4");
            } else throw new IllegalArgumentException("pointer i = " + i);
            incrSP();
        } else throw new IllegalArgumentException("seg = " + seg);
    }

    private void writePop(String seg, String i) throws IOException {
        if (Command.COMMON_SEGMENT.contains(seg)) {
            letR13EqAddr(seg, i);
            letGoLabelXEqGoLabelY("SP", "R13");
            incrSP();
        } else if (seg.equals("static")) {
            decrSP();
            // @Xxx.i = *SP
            letAtXEqGoLabelY(HackClassName + "." + i, "SP");
        } else if (seg.equals("temp")) {
            decrSP();
            int addr = 5 + Integer.parseInt(i);
            // @5+i = *SP
            letAtXEqGoLabelY(String.valueOf(addr), "SP");
        } else if (seg.equals("pointer")) {
            decrSP();
            if (i.equals("0")) {
                // "0" = this = R[3]
                letAtXEqGoLabelY("R3", "SP");
            } else if (i.equals("1")) {
                // "1" = that = R[4]
                letAtXEqGoLabelY("R4", "SP");
            } else throw new IllegalArgumentException("pointer i = " + i);
        } else throw new IllegalArgumentException("seg = " + seg);
    }

    private void writeArithmetic(String name) throws IOException {
        if (Command.BINARY_OPERATIONS.containsKey(name)) {
            writeBinaryOperation(name);
        } else if (name.equals("neg") || name.equals("not")) {
            writeUnaryOperation(name);
        } else if (Command.COMPARE_OPERATIONS.containsKey(name)) {
            writeCompareOperation(name);
        } else throw new IllegalArgumentException("command.name =" + name);

    }

    // ========== helper methods: asm basic blocks
    private void incrSP() throws IOException {
        out.write("@SP\nM=M+1\n".getBytes(OUT_FILE_CHARSET));
    }

    private void decrSP() throws IOException {
        out.write("@SP\nM=M-1\n".getBytes(OUT_FILE_CHARSET));
    }

    /**
     * use R13 as address temp variable
     * R13 = *seg_label + i, like R13 = *LCL + i
     */
    private void letR13EqAddr(String seg, String i) throws IOException {
        String label;
        switch (seg) {
            case "local":
                label = "LCL";
                break;
            case "argument":
                label = "ARG";
                break;
            case "this":
                label = "THIS";
                break;
            case "that":
                label = "THAT";
                break;
            default:
                throw new IllegalArgumentException();
        }
        // D=i, D=D+*label, *R13=D
        String lines = "@" + i + "\nD=A\n@" + label + "\nD=D+M\n@R13\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // *labelX=*labelY
    private void letGoLabelXEqGoLabelY(String labelX, String labelY) throws IOException {
        String lines = "@" + labelY + "\nA=M\nD=M\n@" + labelX + "\nA=M\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // *labelX=@Y like *SP = @Xxx.i
    private void letGoLabelXEqAtY(String labelX, String Y) throws IOException {
        String lines = "@" + Y + "\nD=M\n@" + labelX + "\nA=M\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // @X = *Y
    private void letAtXEqGoLabelY(String X, String labelY) throws IOException {
        String lines = "@" + labelY + "\nA=M\nD=M\n@" + X + "\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }


    // ========== Arithmetic blocks

    // assume Command.BINARY_OPERATIONS.containsKey(name)
    private void writeBinaryOperation(String name) throws IOException {
        Character symbol = Command.BINARY_OPERATIONS.get(name);
        decrSP();
        letAtXEqGoLabelY("R13", "SP");
        decrSP();
        String lines = "@SP\nA=M\nD=M\n@R13\nM=D" + symbol + "M\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
        letGoLabelXEqAtY("SP", "R13");
        incrSP();
    }

    // assume name is "neg" or "not"
    private void writeUnaryOperation(String name) throws IOException {
        char symbol;
        if (name.equals("neg"))
            symbol = '-';
        else if (name.equals("not"))
            symbol = '!';
        else throw new IllegalArgumentException("command.name =" + name);

        decrSP();
        String lines = "@SP\nA=M\nM=" + symbol + "M\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
        incrSP();
    }

    // assume Command.COMPARE_OPERATIONS.containsKey(name)
    private void writeCompareOperation(String name) throws IOException {
        String pushTrueLabel = HackClassName.toUpperCase() + "_PUSH_TURE_" + staticCount;
        String jumpOperation = Command.COMPARE_OPERATIONS.get(name);
        String endOfCompareLabel = HackClassName.toUpperCase() + "_END_COMP_" + staticCount;
        ++staticCount;

        decrSP();
        letAtXEqGoLabelY("R13", "SP");
        decrSP();
        String lines = "@SP\nA=M\nD=M\n@R13\nD=M-D\n@" + pushTrueLabel +
                "\nD;" + jumpOperation +
                "\n@SP\nA=M\nM=0\n@" + endOfCompareLabel + "\n0;JMP\n("
                + pushTrueLabel + ")\n@SP\nA=M\nM=-1\n(" + endOfCompareLabel
                // incrSP;
                + ")\n@SP\nM=M+1\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }
}

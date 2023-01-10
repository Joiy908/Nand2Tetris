import java.io.Closeable;
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

// CodeWriter need to explicitly close
public class CodeWriter implements Closeable {
    private FileOutputStream out;
    private final Charset OUT_FILE_CHARSET = StandardCharsets.US_ASCII;
    private int staticCount;
    private String currHackClassName;
    private String currFuncName;
    private int currCallReturnCount;

    public CodeWriter(File f) {
        try {
            out = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        currCallReturnCount = 0;
    }

    public void writeInit() throws IOException{
        String lines = "@256\nD=A\n@SP\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
        writeCall("Sys.init", "0");
    }

    public void setClassName(String name) {
        currHackClassName = name;
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
                case BRANCHING:
                    writeBranching(command.name, command.arg1);
                    break;
                case FUNCTION:
                    writeFunction(command);
                    break;
                default:
                    throw new IllegalStateException("Unexpected type: " + command.type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private void writePush(String seg, String i) throws IOException {
        if (Command.COMMON_SEGMENT.containsKey(seg)) {
            letGoSPEqGoAddr(seg, i);
            incrSP();
        } else if (seg.equals("constant")) {
            // *SP = i
            String lines = "@" + i + "\nD=A\n@SP\nA=M\nM=D\n";
            out.write(lines.getBytes(OUT_FILE_CHARSET));
            incrSP();
        } else if (seg.equals("static")) {
            // *SP = @Xxx.i
            letGoLabelXEqAtY("SP", currHackClassName + "." + i);
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
        if (Command.COMMON_SEGMENT.containsKey(seg)) {
            if (i.equals("0")) {
                String label = Command.COMMON_SEGMENT.get(seg);
                // SP--, *label=*SP
                String lines = "@SP\nAM=M-1\nD=M\n@" + label + "\nA=M\nM=D\n";
                out.write(lines.getBytes(OUT_FILE_CHARSET));
            } else {
                letR13EqAddr(seg, i);
                decrSPAndLetGoLabelXEqGoSP("R13");
            }

        } else if (seg.equals("static")) {
            // SP--, @Xxx.i = *SP
            decrSPAndLetAtXEqGoSP(currHackClassName + "." + i);
        } else if (seg.equals("temp")) {
            int addr = 5 + Integer.parseInt(i);
            // SP--, @5+i = *SP
            decrSPAndLetAtXEqGoSP(String.valueOf(addr));
        } else if (seg.equals("pointer")) {
            if (i.equals("0")) { // "0" = this = R[3]
                // SP--, @R3 = *SP
                decrSPAndLetAtXEqGoSP("R3");
            } else if (i.equals("1")) { // "1" = that = R[4]
                // SP--, @R4 = *SP
                decrSPAndLetAtXEqGoSP("R4");
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

    private void writeBranching(String name, String label) throws IOException {
        if (currFuncName != null)
            label = currFuncName + '$' + label;
        String lines;
        switch (name) {
            case "label":
                lines = "(" + label + ")\n";
                break;
            case "if-goto":
                lines = "@SP\nAM=M-1\nD=M\n@" + label + "\nD;JNE\n";
                break;
            case "goto":
                lines = "@" + label + "\n0;JMP\n";
                break;
            default:
                throw new IllegalArgumentException("command.name =" + name);
        }
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    private void writeFunction(Command command) throws IOException {
        switch (command.name) {
            case "call":
                writeCall(command.arg1, command.arg2);
                ++currCallReturnCount;
                break;
            case "function":
                currFuncName = command.arg1;
                currCallReturnCount = 0;
                writeFunc(command.arg1, command.arg2);
                break;
            case "return":
                writeReturn();
                break;
            default:
                throw new IllegalArgumentException("command.name =" + command.name);
        }
    }

    private void writeCall(String calledFuncName, String nArgs) throws IOException {
        String returnAddrLabel = currFuncName + "$ret." + currCallReturnCount;
        String lines = "@" + returnAddrLabel + "\nD=A\n@SP\nA=M\nM=D\n"
                // push LCL/ARG...
                + "@LCL\nD=M\n@SP\nAM=M+1\nM=D\n"
                + "@ARG\nD=M\n@SP\nAM=M+1\nM=D\n"
                + "@THIS\nD=M\n@SP\nAM=M+1\nM=D\n"
                + "@THAT\nD=M\n@SP\nAM=M+1\nM=D\n"
                // SP++, ARG = SP-(5+nArgs)
                + "@" + (5+Integer.parseInt(nArgs))
                + "\nD=A\n@SP\nM=M+1\nD=M-D\n@ARG\nM=D\n"
                + "@SP\nD=M\n@LCL\nM=D\n"
                + "@" + calledFuncName + "\n0;JMP\n"
                + "(" + returnAddrLabel + ")\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // assume currFuncName is updated to funcName
    private void writeFunc(String funcName, String mVar) throws IOException {
        StringBuilder lines = new StringBuilder("(" + funcName + ")\n");
        int m = Integer.parseInt(mVar);
        if (m != 0) {
            for (int i = 0; i < m; i++) {
                if (i == 0) lines.append("@SP\nA=M\nM=0\n");
                else lines.append("M=0\n");
                lines.append("@SP\nAM=M+1\n");
            }
        }
        out.write(lines.toString().getBytes(OUT_FILE_CHARSET));
    }

    // set R13 as endFrame and R14 as retAddr
    private void writeReturn() throws IOException {
        // R13 = LCL; R14 = *(R13-5)
        String lines = "@LCL\nD=M\n@R13\nM=D\n@5\nD=A\n@R13\nA=M-D\nD=M\n@R14\nM=D\n"
                // *ARG=pop() => *ARG=*(--SP)
                + "@SP\nAM=M-1\nD=M\n@ARG\nA=M\nM=D\n"
                // SP = ARG + 1
                + "@ARG\nD=M\n@SP\nM=D+1\n"
                // THAT = *(R13-1)
                + "@R13\nA=M-1\nD=M\n@THAT\nM=D\n"
                // R13--; THIS = *(R13-1) # THAT = *(R13-2)
                + "@R13\nM=M-1\nA=M-1\nD=M\n@THIS\nM=D\n"
                + "@R13\nM=M-1\nA=M-1\nD=M\n@ARG\nM=D\n"
                + "@R13\nM=M-1\nA=M-1\nD=M\n@LCL\nM=D\n"
                // goto R14
                + "@R14\nA=M\n0;JMP\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // ========== helper methods: asm basic blocks
    private void incrSP() throws IOException {
        out.write("@SP\nM=M+1\n".getBytes(OUT_FILE_CHARSET));
    }

    /**
     * assume Command.COMMON_SEGMENT.containsKey(seg)
     * use R13 as address temp variable
     * R13 = *seg_label + i, like R13 = *LCL + i
     */
    private void letR13EqAddr(String seg, String i) throws IOException {
        String label = Command.COMMON_SEGMENT.get(seg);
        // D=i, D=D+*label, *R13=D
        String lines = "@" + i + "\nD=A\n@" + label + "\nD=D+M\n@R13\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    /**
     * assume Command.COMMON_SEGMENT.containsKey(seg)
     * *SP=*(seg_label + i)
     */
    private void letGoSPEqGoAddr(String seg, String i) throws IOException {
        String label = Command.COMMON_SEGMENT.get(seg);
        String lines;
        if (i.equals("0")) {
            // *SP = *label
            lines = "@" + label + "\nA=M\nD=M\n@SP\nA=M\nM=D\n";
        } else {
            // D=i, A=D+@label, *SP=M
            lines = "@" + i + "\nD=A\n@" + label + "\nA=D+M\nD=M\n@SP\nA=M\nM=D\n";
        }
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }


    // SP--, *labelX = *SP
    private void decrSPAndLetGoLabelXEqGoSP(String labelX) throws IOException {
        String lines = "@SP\nAM=M-1\nD=M\n@" + labelX + "\nA=M\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // *labelX=@Y like *SP = @Xxx.i
    private void letGoLabelXEqAtY(String labelX, String Y) throws IOException {
        String lines = "@" + Y + "\nD=M\n@" + labelX + "\nA=M\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }


    // sp--, @X = *sp
    private void decrSPAndLetAtXEqGoSP(String X) throws IOException {
        String lines = "@SP\nAM=M-1\nD=M\n@" + X + "\nM=D\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // ========== Arithmetic blocks

    // assume Command.BINARY_OPERATIONS.containsKey(name)
    private void writeBinaryOperation(String name) throws IOException {
        String expression = Command.BINARY_OPERATIONS.get(name);

        String lines = "@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=" + expression
                + "\nM=D\n@SP\nM=M+1\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }

    // assume name is "neg" or "not"
    private void writeUnaryOperation(String name) throws IOException {
        char symbol;
        if (name.equals("neg"))
            symbol = '-';
        else if (name.equals("not"))
            symbol = '!';
        else throw new IllegalArgumentException("command.name =" + name);

        String lines = "@SP\nAM=M-1\nM=" + symbol + "M\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
        incrSP();
    }

    // assume Command.COMPARE_OPERATIONS.containsKey(name)
    private void writeCompareOperation(String name) throws IOException {
        String pushTrueLabel = currHackClassName.toUpperCase() + "_PUSH_TURE_" + staticCount;
        String jumpOperation = Command.COMPARE_OPERATIONS.get(name);
        String endOfCompareLabel = currHackClassName.toUpperCase() + "_END_COMP_" + staticCount;
        ++staticCount;

        String lines = "@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=M-D\n@" + pushTrueLabel +
                "\nD;" + jumpOperation +
                "\n@SP\nA=M\nM=0\n@" + endOfCompareLabel + "\n0;JMP\n("
                + pushTrueLabel + ")\n@SP\nA=M\nM=-1\n(" + endOfCompareLabel
                // incrSP;
                + ")\n@SP\nM=M+1\n";
        out.write(lines.getBytes(OUT_FILE_CHARSET));
    }
}

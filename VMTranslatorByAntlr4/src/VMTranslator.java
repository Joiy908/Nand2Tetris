import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Joiy908
 * @date 2024/2/18
 */

public class VMTranslator {

    public static class Listener extends HackVMBaseListener {
        private final FileOutputStream out;
        int staticCount;
        private final String currHackClassName;
        private String currFuncName;
        private int currCallReturnCount;

        public Listener(File f, int staticCount, FileOutputStream out) {
            this.out = out;
            this.staticCount = staticCount;
            currCallReturnCount = 0;
            currHackClassName = getClassName(f.getName());
        }

        private static String getClassName(String name) {
            final int pos = name.lastIndexOf('.');
            return name.substring(0, pos);
        }

        void writeInit() {
            String lines = "@256\nD=A\n@SP\nM=D\n";
            write(lines);
            writeCall("Sys.init", "0");
        }


        private void write(String s) {
            try {
                out.write(s.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalArgumentException("write err");
            }
        }

        private static final HashMap<String, String> BINARY_OPERATIONS = new HashMap<>() {{
            put("add", "D+M");
            put("sub", "M-D");
            put("and", "D&M");
            put("or", "D|M");
        }};

        public final static HashMap<String, String> COMPARE_OPERATIONS = new HashMap<>() {{
            put("eq", "JEQ");
            put("lt", "JLT");
            put("gt", "JGT");
        }};
        public final static HashMap<String, String> COMMON_SEGMENT = new HashMap<>() {{
            put("local", "LCL");
            put("argument", "ARG");
            put("this", "THIS");
            put("that", "THAT");
        }};


        @Override
        public void enterBinaryCmd(HackVMParser.BinaryCmdContext ctx) {
            String op = ctx.getText();
            String expr = BINARY_OPERATIONS.get(op);
            String lines = "@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=" + expr
                    + "\nM=D\n@SP\nM=M+1\n";
            write(lines);
        }

        @Override
        public void enterUnaryCmd(HackVMParser.UnaryCmdContext ctx) {
            String name = ctx.getText();
            char symbol = name.equals("neg") ? '-' : '!';

            String lines = "@SP\nAM=M-1\nM=" + symbol + "M\n";
            write(lines);
            incrSP();
        }

        @Override
        public void enterCmpCmd(HackVMParser.CmpCmdContext ctx) {
            String pushTrueLabel = currHackClassName.toUpperCase() + "_PUSH_TURE_" + staticCount;
            String jumpOperation = COMPARE_OPERATIONS.get(ctx.getText());
            String endOfCompareLabel = currHackClassName.toUpperCase() + "_END_COMP_" + staticCount;
            staticCount++;

            String lines = "@SP\nAM=M-1\nD=M\n@SP\nAM=M-1\nD=M-D\n@" + pushTrueLabel +
                    "\nD;" + jumpOperation +
                    "\n@SP\nA=M\nM=0\n@" + endOfCompareLabel + "\n0;JMP\n("
                    + pushTrueLabel + ")\n@SP\nA=M\nM=-1\n(" + endOfCompareLabel
                    // incrSP;
                    + ")\n@SP\nM=M+1\n";
            write(lines);
        }

        @Override
        public void enterPushConst(HackVMParser.PushConstContext ctx) {
            final int i = Integer.parseInt(ctx.INT().getText());
            // *SP = i
            String lines = "@" + i + "\nD=A\n@SP\nA=M\nM=D\n";
            write(lines);
            incrSP();
        }

        @Override
        public void enterPushComm(HackVMParser.PushCommContext ctx) {
            letGoSPEqGoAddr(ctx.COMM_SEG().getText(), ctx.INT().getText());
            incrSP();
        }

        @Override
        public void enterPushBase(HackVMParser.PushBaseContext ctx) {
            int base = 3;
            if (ctx.BASE_SEG().getText().equals("temp")) {
                base = 5;
            }
            int addr = base + Integer.parseInt(ctx.INT().getText());
            letGoLabelXEqAtY("SP", String.valueOf(addr));
            incrSP();
        }

        @Override
        public void enterPushStatic(HackVMParser.PushStaticContext ctx) {
            // *SP = @Xxx.i
            letGoLabelXEqAtY("SP", currHackClassName + "." + ctx.INT().getText());
            incrSP();
        }

        @Override
        public void enterPopComm(HackVMParser.PopCommContext ctx) {
            String i = ctx.INT().getText();
            String seg = ctx.COMM_SEG().getText();
            if (i.equals("0")) {
                String label = COMMON_SEGMENT.get(seg);
                // SP--, *label=*SP
                String lines = "@SP\nAM=M-1\nD=M\n@" + label + "\nA=M\nM=D\n";
                write(lines);
            } else {
                letR13EqAddr(seg, i);
                decrSPAndLetGoLabelXEqGoSP("R13");
            }
        }

        @Override
        public void enterPopBase(HackVMParser.PopBaseContext ctx) {
            int base = 3;
            if (ctx.BASE_SEG().getText().equals("temp")) {
                base = 5;
            }
            int addr = base + Integer.parseInt(ctx.INT().getText());
            // SP--, @5+i = *SP
            decrSPAndLetAtXEqGoSP(String.valueOf(addr));
        }

        @Override
        public void enterPopStatic(HackVMParser.PopStaticContext ctx) {
            // SP--, @Xxx.i = *SP
            decrSPAndLetAtXEqGoSP(currHackClassName + "." + ctx.INT().getText());
        }

        @Override
        public void enterBranch_cmd(HackVMParser.Branch_cmdContext ctx) {
            String branch = ctx.BRANCH().getText();
            String label = ctx.ID().getText();
            if (currFuncName != null)
                label = currFuncName + '$' + label;
            String lines;
            switch (branch) {
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
                    throw new IllegalStateException("Unexpected value: " + branch);
            }
            write(lines);
        }

        @Override
        public void enterFuncDef(HackVMParser.FuncDefContext ctx) {
            currFuncName = ctx.ID().getText();
            currCallReturnCount = 0;
            final String mVar = ctx.INT().getText();
            StringBuilder lines = new StringBuilder("(" + currFuncName + ")\n");
            int m = Integer.parseInt(mVar);
            if (m != 0) {
                for (int i = 0; i < m; i++) {
                    if (i == 0) lines.append("@SP\nA=M\nM=0\n");
                    else lines.append("M=0\n");
                    lines.append("@SP\nAM=M+1\n");
                }
            }
            write(lines.toString());
        }

        @Override
        public void enterCall(HackVMParser.CallContext ctx) {
            final String nArgs = ctx.INT().getText();
            final String calledFuncName = ctx.ID().getText();
            writeCall(calledFuncName, nArgs);
        }

        @Override
        public void enterReturn(HackVMParser.ReturnContext ctx) {
            // set R13 as endFrame and R14 as retAddr
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
            write(lines);
        }

        private void writeCall(String calledFuncName, String nArgs) {
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
            write(lines);
            currCallReturnCount++;
        }

        // ========== helper methods: asm basic blocks
        private void incrSP() {
            write("@SP\nM=M+1\n");
        }

        /**
         * assume COMMON_SEGMENT.containsKey(seg)
         * use R13 as address temp variable
         * R13 = *seg_label + i, like R13 = *LCL + i
         */
        private void letR13EqAddr(String seg, String i) {
            String label = COMMON_SEGMENT.get(seg);
            // D=i, D=D+*label, *R13=D
            String lines = "@" + i + "\nD=A\n@" + label + "\nD=D+M\n@R13\nM=D\n";
            write(lines);
        }

        /**
         * assume COMMON_SEGMENT.containsKey(seg)
         * *SP=*(seg_label + i)
         */
        private void letGoSPEqGoAddr(String seg, String i) {
            String label = COMMON_SEGMENT.get(seg);
            String lines;
            if (i.equals("0")) {
                // *SP = *label
                lines = "@" + label + "\nA=M\nD=M\n@SP\nA=M\nM=D\n";
            } else {
                // D=i, A=D+@label, *SP=M
                lines = "@" + i + "\nD=A\n@" + label + "\nA=D+M\nD=M\n@SP\nA=M\nM=D\n";
            }
            write(lines);
        }


        // SP--, *labelX = *SP
        private void decrSPAndLetGoLabelXEqGoSP(String labelX) {
            String lines = "@SP\nAM=M-1\nD=M\n@" + labelX + "\nA=M\nM=D\n";
            write(lines);
        }

        // *labelX=@Y like *SP = @Xxx.i
        private void letGoLabelXEqAtY(String labelX, String Y) {
            String lines = "@" + Y + "\nD=M\n@" + labelX + "\nA=M\nM=D\n";
            write(lines);
        }


        // sp--, @X = *sp
        private void decrSPAndLetAtXEqGoSP(String X) {
            String lines = "@SP\nAM=M-1\nD=M\n@" + X + "\nM=D\n";
            write(lines);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage: VMTranslator <dir | file.vm>");
            return;
        }
        File input = new File(args[0]);
        if (!(input.exists())) {
            System.out.println("Fail to open the file/dir: " + args[0]);
            return;
        }


        final File[] vmFiles;
        if (input.isDirectory()) {
            vmFiles = input.listFiles((dir, name) -> name.matches(".*\\.vm$"));
        } else { // input.isFile()
            vmFiles = new File[] {input};
        }


        ParseTreeWalker walker = new ParseTreeWalker();

        int staticCount = 0;

        boolean isInit = false;
        FileOutputStream out = new FileOutputStream(getASMPath(input));
        assert vmFiles != null;
        for (File f : vmFiles) {
            final Listener listener = new Listener(f, staticCount, out);
            if (!isInit) {
                listener.writeInit();
                isInit = true;
            }
            walker.walk(listener, getTree(f));
            staticCount = listener.staticCount;
        }
        out.close();
    }

    private static HackVMParser.FileContext getTree(File f) throws IOException {
        FileInputStream is = new FileInputStream(f);
        ANTLRInputStream ais = new ANTLRInputStream(is);
        HackVMLexer lexer = new HackVMLexer(ais);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HackVMParser parser = new HackVMParser(tokens);
        parser.setBuildParseTree(true);
        return parser.file();
    }
    /**
     * assume in.exists()
     * Xxx or Xxx.vm to Xxx.asm
     */
    private static String getASMPath(File in) {
        if(in.isFile()) {
            final int pos = in.getName().lastIndexOf('.');
            String rst = in.getName().substring(0, pos);
            return rst+".asm";
        } else if (in.isDirectory()) {
            return in.getPath() + File.separator + in.getName() + ".asm";
        } else throw new IllegalArgumentException();
    }
}

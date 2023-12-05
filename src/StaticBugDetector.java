import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

//https://www.youtube.com/watch?v=HfargWnOxO0

public class StaticBugDetector {
    public static void main(String[] args) throws IOException {
        //Step 1 Parse C code and generate a ParseTree
        CharStream codeCharStream = CharStreams.fromFileName("./src/example2.c");
        CLexer lexer = new CLexer(codeCharStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CParser parser = new CParser(tokens);
        ParseTree tree = parser.compilationUnit();

        System.out.println("ParseTree:");
        System.out.println(tree.toStringTree(parser) + "\n");

        // Step 2 Parse ParseTree and create a CFG of it.
        CFGBuilderVisitor visitor = new CFGBuilderVisitor();
        visitor.visit(tree);
        ControlFlowGraph cfg = visitor.getCFG();
        System.out.println("CFG:");
        cfg.printGraph();

        // Step 3 Extract and keep track of all the variables like the pointers
        System.out.println("\nVariables:");
        visitor.printVariableNames();

        // Step 4 run dataflow analysis and show results.
        System.out.println("\nResult:");
        visitor.analyzeCFG(cfg);
        visitor.checkForNullDereferences(cfg);
    }
}
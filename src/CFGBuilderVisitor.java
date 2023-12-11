import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CFGBuilderVisitor extends CBaseVisitor<Void> {
    private ControlFlowGraph cfg = new ControlFlowGraph();
    private CFGNode lastNode = null;
    private Map<String, Variable> variables = new HashMap<>();
    public ControlFlowGraph getCFG() {
        return cfg;
    }
    private Stack<CFGNode> branchStack = new Stack<>();

    // Analyzes the CFG to update the state of variables, especially pointers
    public void analyzeCFG(ControlFlowGraph cfg) {
        Map<String, Variable> mainState = new HashMap<>(); // Main flow state
        Map<String, Variable> ifState = new HashMap<>(); // State at the start of 'if'
        boolean inElseBranch = false;
        CFGNode elseNode = null; // Node representing the 'else' statement

        for (CFGNode node : cfg.getAllNodes()) {
            String code = node.getCode();
            String varName = extractVariableName(node.getContext());

            // Save state at the start of 'if' branch
            if (code.startsWith("if(")) {
                ifState = new HashMap<>(mainState);
            } else if (code.equals("else")) {
                inElseBranch = true;
                elseNode = node;
                mainState = new HashMap<>(ifState); // Use 'if' state for 'else' branch
            }

            // Update state based on code
            if (varName != null) {
                boolean isPointer = code.contains("*");
                Variable.PointerState state = code.contains("NULL") ? Variable.PointerState.NULL : Variable.PointerState.ASSIGNED;
                mainState.put(varName, new Variable(varName, isPointer, state));
            }

            // Check if we are exiting the 'if-else' structure
            if (inElseBranch && (elseNode == null || !elseNode.getSuccessors().contains(node))) {
                inElseBranch = false;
                elseNode = null;
                // Merge states from 'if' and 'else' branches
                for (String key : ifState.keySet()) {
                    Variable ifVarState = ifState.get(key);
                    Variable elseVarState = mainState.getOrDefault(key, new Variable(key, false, Variable.PointerState.UNDEFINED));

                    if (ifVarState.state != elseVarState.state) {
                        elseVarState.state = Variable.PointerState.POTENTIALLY_NULL;
                    }
                    mainState.put(key, elseVarState);
                }
            }

            // Update global state if not in 'else' branch
            if (!inElseBranch) {
                variables.putAll(mainState);
            }
        }
    }

    // Checks the CFG for potential null pointer dereferences
    public void checkForNullDereferences(ControlFlowGraph cfg) {
        boolean found = false;
        for (CFGNode node : cfg.getAllNodes()) {
            String code = node.getCode();
            Pattern p = Pattern.compile("\\*\\s*(\\w+)");
            Matcher m = p.matcher(code);

            // Check each dereference to see if the pointer might be null based on analyzeCFG method
            while (m.find()) {
                String varName = m.group(1);
                if (variables.containsKey(varName)) {
                    Variable varInfo = variables.get(varName);

                    // Warn if a null pointer is dereferenced
                    if (varInfo.isPointer && varInfo.state == Variable.PointerState.NULL &&
                            !code.matches("\\s*int\\s*\\*\\s*" + varName + "\\s*=.*") && !code.contains(varName + " = NULL")) {
                        System.out.println("Potential null pointer dereference detected at: " + code);
                        found = true;
                    }
                }
            }
        }
        if (!found){
            System.out.println("No potential null pointer deferences found");
        }
    }

    // Prints the names and states of variables in each node of the CFG
    public void printVariableNames() {
        for (CFGNode node : cfg.getAllNodes()) {
            String code = node.getCode();
            String variableName = extractVariableName(node.getContext());
            if (variableName != null && variables.containsKey(variableName)) {
                Variable varInfo = variables.get(variableName);
                String pointerState = varInfo.isPointer ? varInfo.state.toString() : "Not a pointer";
                System.out.println("Node: " + code + " - Variable: " + variableName + " - State: " + pointerState);
            } else {
                System.out.println("Node: " + code + " - No variable");
            }
        }
    }

    // Handles declaration statements
    @Override
    public Void visitDeclaration(CParser.DeclarationContext ctx) {
        // Extract the variable name from the declaration context
        String varName = extractVariableName(ctx);
        // If the declared variable is a pointer
        boolean isPointer = ctx.getText().contains("*");
        // Set the initial state of the pointer (NULL or ASSIGNED)
        Variable.PointerState state = ctx.getText().contains("NULL") ? Variable.PointerState.NULL : Variable.PointerState.ASSIGNED;
        // Add variable information in the variables map
        variables.put(varName, new Variable(varName, isPointer, state));

        // Add this declaration statement as a node in the CFG
        addNodeToCFG(ctx.getText(), ctx);
        return super.visitDeclaration(ctx);
    }

    // Handles expressions statements like assignments to variables.
    @Override
    public Void visitExpressionStatement(CParser.ExpressionStatementContext ctx) {
        // Retrieve the full code of the expression statement
        String code = ctx.getText();
        // Get the variable name involved in the expression
        String varName = extractVariableName(ctx);

        // If a variable is found in the expression
        if (varName != null) {
            // Check if the variable is already in the map. if not, initialize its info
            if (!variables.containsKey(varName)) {
                variables.put(varName, new Variable(varName, false, Variable.PointerState.ASSIGNED)); // Assuming it's not a pointer
            }

            // Retrieve variable info from the map
            Variable varInfo = variables.get(varName);
            // If the variable is a pointer, update its state based on the assignment
            if (varInfo.isPointer) {
                if (code.matches(varName + "\\s*=\\s*NULL")) {
                    varInfo.state = Variable.PointerState.NULL;
                } else if (code.matches(varName + "\\s*=\\s*.*")) {
                    varInfo.state = Variable.PointerState.ASSIGNED;
                }
            }
        }

        // Add this expression statement as a node in the CFG
        addNodeToCFG(code, ctx);
        return super.visitExpressionStatement(ctx);
    }

    // Handles things like return
    @Override
    public Void visitJumpStatement(CParser.JumpStatementContext ctx) {
        // Retrieve the full code of the jump statement
        String jumpStatement = ctx.getText();

        // Check if the jump statement includes an expression (like a return value)
        if (ctx.getChildCount() > 1 && ctx.getChild(1) instanceof CParser.ExpressionContext) {
            // Extract the variable name involved in the expression
            String varName = extractVariableName((CParser.ExpressionContext) ctx.getChild(1));
            // Retrieve variable info from the map
            Variable varInfo = variables.get(varName);
            // If the variable is a pointer and its state is NULL
            if (varInfo != null && varInfo.isPointer && varInfo.state == Variable.PointerState.NULL) {
                System.out.println("Warning: Returning a null pointer in " + jumpStatement);
            }
        }

        // Add this jump statement as a node in the CFG
        addNodeToCFG(ctx.getText(), ctx);
        return super.visitJumpStatement(ctx);
    }

    // Handles selection statements like 'if-else'
    @Override
    public Void visitSelectionStatement(CParser.SelectionStatementContext ctx) {
        if (ctx.If() != null) {
            String ifCondition = ctx.expression().getText();
            addNodeToCFG("if(" + ifCondition + ")", ctx);

            // Save the current state of variables before visiting 'if' block
            Map<String, Variable> preIfState = new HashMap<>(variables);

            visit(ctx.statement(0)); // Visit 'if' block

            if (ctx.Else() != null) {
                // Restore variables to state before 'if' block
                variables = new HashMap<>(preIfState);

                addNodeToCFG("else", ctx);
                visit(ctx.statement(1)); // Visit 'else' block
            }
        }
        return null; // Return null to avoid visiting children automatically
    }

    private void addNodeToCFG(String code, ParserRuleContext ctx) {
        CFGNode currentNode = new CFGNode(code, ctx);

        if (cfg.startNode == null) {
            cfg.startNode = currentNode;
        }

        if (lastNode != null && !lastNode.equals(currentNode)) {
            if (!lastNode.getCode().startsWith("if(") && !lastNode.getCode().equals("else")) {
                lastNode.addSuccessor(currentNode);
            }

            if (lastNode.getCode().startsWith("if(")) {
                // Link 'if' node to the current node (start of the 'if' block)
                lastNode.addSuccessor(currentNode);
                // Push the 'if' node onto the branch stack
                branchStack.push(currentNode);
            } else if (lastNode.getCode().equals("else")) {
                // Link 'else' node to the current node (start of the 'else' block)
                lastNode.addSuccessor(currentNode);
                if (!branchStack.isEmpty()) {
                    // Pop the corresponding 'if' node from the stack
                    CFGNode ifNode = branchStack.pop();
                }
            }
        }

        lastNode = currentNode;
    }

    private String extractVariableName(ParserRuleContext ctx) {
        // declaration
        if (ctx instanceof CParser.DeclarationContext) {
            CParser.DeclarationContext declCtx = (CParser.DeclarationContext) ctx;
            // Check if the declaration has an initializer list and is not empty
            if (declCtx.initDeclaratorList() != null && !declCtx.initDeclaratorList().initDeclarator().isEmpty()) {
                // Return the name of the first variable declared
                return declCtx.initDeclaratorList().initDeclarator(0).declarator().directDeclarator().getText();
            }
        }

        // expression (like in an assignment)
        if (ctx instanceof CParser.ExpressionStatementContext) {
            CParser.ExpressionStatementContext exprStmtCtx = (CParser.ExpressionStatementContext) ctx;
            String code = exprStmtCtx.getText();
            // If the expression contains an assignment operator
            if (code.contains("=")) {
                // Return the variable name on the left side of the "="
                return code.substring(0, code.indexOf("=")).trim();
            }
            // Handle cases where the assignment expression is more complex
            List<CParser.AssignmentExpressionContext> assignExprList = exprStmtCtx.expression().assignmentExpression();
            if (!assignExprList.isEmpty()) {
                CParser.AssignmentExpressionContext assignCtx = assignExprList.get(0);
                // Extract the variable name from the unary expression
                if (assignCtx.unaryExpression() != null) {
                    CParser.PostfixExpressionContext postfixCtx = assignCtx.unaryExpression().postfixExpression();
                    if (postfixCtx != null) {
                        // Return the primary expression text, which should be the variable name
                        return postfixCtx.primaryExpression().getText();
                    }
                }
            }
        }

        // jump statement (like return)
        if (ctx instanceof CParser.JumpStatementContext) {
            CParser.JumpStatementContext jumpCtx = (CParser.JumpStatementContext) ctx;
            if (jumpCtx.expression() != null) {
                List<CParser.AssignmentExpressionContext> assignExprList = jumpCtx.expression().assignmentExpression();
                if (!assignExprList.isEmpty()) {
                    CParser.AssignmentExpressionContext assignCtx = assignExprList.get(0);
                    if (assignCtx.unaryExpression() != null) {
                        CParser.PostfixExpressionContext postfixCtx = assignCtx.unaryExpression().postfixExpression();
                        if (postfixCtx != null) {
                            // Return the variable name involved in the return statement
                            return postfixCtx.primaryExpression().getText();
                        }
                    }
                }
            }
        }

        return null;
    }
}
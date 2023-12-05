import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CFGBuilderVisitor extends CBaseVisitor<Void> {
    private ControlFlowGraph cfg = new ControlFlowGraph();
    private CFGNode lastNode = null;
    private Map<String, Variable> variables = new HashMap<>();
    public ControlFlowGraph getCFG() {
        return cfg;
    }

    public void analyzeCFG(ControlFlowGraph cfg) {
        for (CFGNode node : cfg.getAllNodes()) {
            String code = node.getCode();
            String varName = extractVariableName(node.getContext());

            if (varName != null && variables.containsKey(varName)) {
                Variable varInfo = variables.get(varName);

                if (varInfo.isPointer) {
                    if (code.contains(varName + " = NULL")) {
                        varInfo.state = Variable.PointerState.NULL;
                    } else if (code.matches(varName + " = .*") && !code.contains(varName + " = NULL")) {
                        varInfo.state = Variable.PointerState.ASSIGNED;
                    }
                }
            }
        }
    }

    public void checkForNullDereferences(ControlFlowGraph cfg) {
        for (CFGNode node : cfg.getAllNodes()) {
            String code = node.getCode();
            Pattern p = Pattern.compile("\\*\\s*(\\w+)");
            Matcher m = p.matcher(code);

            while (m.find()) {
                String varName = m.group(1);
                if (variables.containsKey(varName)) {
                    Variable varInfo = variables.get(varName);

                    if (varInfo.isPointer && varInfo.state == Variable.PointerState.NULL) {
                        System.out.println("Potential null pointer dereference detected at: " + code);
                    }
                }
            }
        }
    }

    public void printVariableNames() {
        for (CFGNode node : cfg.getAllNodes()) {
            String variableName = extractVariableName(node.getContext());
            if (variableName != null) {
                System.out.println("Node: " + node.getCode() + " - Variable: " + variableName);
            } else {
                System.out.println("Node: " + node.getCode() + " - No variable");
            }
        }
    }
    // Handles declaration statements
    @Override
    public Void visitDeclaration(CParser.DeclarationContext ctx) {
        String varName = extractVariableName(ctx);
        boolean isPointer = ctx.getText().contains("*");
        Variable.PointerState state = ctx.getText().contains("NULL") ? Variable.PointerState.NULL : Variable.PointerState.ASSIGNED;
        variables.put(varName, new Variable(varName, isPointer, state));

        addNodeToCFG(ctx.getText(), ctx);
        return super.visitDeclaration(ctx);
    }

    // Handles expressions statements like assignments to variables.
    @Override
    public Void visitExpressionStatement(CParser.ExpressionStatementContext ctx) {
        String varName = extractVariableName(ctx);
        Variable varInfo = variables.get(varName);
        if (varInfo != null && varInfo.isPointer) {
            varInfo.state = ctx.getText().contains("NULL") ? Variable.PointerState.NULL : Variable.PointerState.ASSIGNED;
        }

        addNodeToCFG(ctx.getText(), ctx);
        return super.visitExpressionStatement(ctx);
    }

    // Handles things like return
    @Override
    public Void visitJumpStatement(CParser.JumpStatementContext ctx) {
        String jumpStatement = ctx.getText();

        if (ctx.getChildCount() > 1 && ctx.getChild(1) instanceof CParser.ExpressionContext) {
            String varName = extractVariableName((CParser.ExpressionContext) ctx.getChild(1));
            Variable varInfo = variables.get(varName);
            if (varInfo != null && varInfo.isPointer && varInfo.state == Variable.PointerState.NULL) {
                System.out.println("Warning: Returning a null pointer in " + jumpStatement);
            }
        }

        addNodeToCFG(ctx.getText(), ctx);
        return super.visitJumpStatement(ctx);
    }


    private void addNodeToCFG(String code, ParserRuleContext ctx) {
        CFGNode currentNode = new CFGNode(code, ctx);

        if (cfg.startNode == null) {
            cfg.startNode = currentNode;
        }

        if (lastNode != null && !lastNode.equals(currentNode)) {
            lastNode.addSuccessor(currentNode);
        }

        lastNode = currentNode;
    }

    private String extractVariableName(ParserRuleContext ctx) {
        // declaration
        if (ctx instanceof CParser.DeclarationContext) {
            CParser.DeclarationContext declCtx = (CParser.DeclarationContext) ctx;
            if (declCtx.initDeclaratorList() != null && !declCtx.initDeclaratorList().initDeclarator().isEmpty()) {
                return declCtx.initDeclaratorList().initDeclarator(0).declarator().directDeclarator().getText();
            }
        }

        // expression (like in an assignment)
        if (ctx instanceof CParser.ExpressionStatementContext) {
            CParser.ExpressionStatementContext exprStmtCtx = (CParser.ExpressionStatementContext) ctx;
            List<CParser.AssignmentExpressionContext> assignExprList = exprStmtCtx.expression().assignmentExpression();
            if (!assignExprList.isEmpty()) {
                CParser.AssignmentExpressionContext assignCtx = assignExprList.get(0);
                if (assignCtx.unaryExpression() != null) {
                    CParser.PostfixExpressionContext postfixCtx = assignCtx.unaryExpression().postfixExpression();
                    if (postfixCtx != null) {
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
                            return postfixCtx.primaryExpression().getText();
                        }
                    }
                }
            }
        }

        return null;
    }
}
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class CFGNode {
    String code; // represents the code at this node
    ParserRuleContext context; // store the context for analysis
    List<CFGNode> successors; // nodes to which control may pass

    public CFGNode(String code, ParserRuleContext context) {
        this.code = code;
        this.context = context;
        this.successors = new ArrayList<>();
    }

    void addSuccessor(CFGNode node) {
        successors.add(node);
    }

    public List<CFGNode> getSuccessors(){
        return successors;
    }

    public ParserRuleContext getContext() {
        return context;
    }

    public String getCode() {
        return code;
    }
}

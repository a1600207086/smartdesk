package com.smartdesk.knowledge;

import com.smartdesk.agent.AgentContext;
import com.smartdesk.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeSearchTool implements AgentTool {

    private final KnowledgeRetrievalService retrievalService;

    public KnowledgeSearchTool(KnowledgeRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public String name() {
        return "searchKnowledge";
    }

    @Override
    public String description() {
        return "Search the tenant knowledge base. Argument: query and optional topK";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments, AgentContext context) {
        Object queryValue = arguments.get("query");
        if (queryValue == null || queryValue.toString().isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        Integer topK = arguments.get("topK") instanceof Number number ? number.intValue() : null;
        List<KnowledgeSearchResult> matches = retrievalService.search(
                context.tenantId(),
                queryValue.toString(),
                topK
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", queryValue.toString());
        result.put("matches", matches);
        return result;
    }
}
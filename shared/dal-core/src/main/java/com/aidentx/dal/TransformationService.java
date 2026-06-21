package com.aidentx.dal;

import com.aidentx.iam.contracts.dto.policy.MappingRuleDTO;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A reusable, stateless engine for executing data transformations based on
 * mapping rules.
 * This service is fully decoupled and operates only on the data passed to it.
 */
@Service
public class TransformationService {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final Map<String, ScriptEngine> scriptEngineMap;

    public TransformationService(UnifiedLoggerFactory loggerFactory, ObjectMapper objectMapper) {
        this.logger = loggerFactory.getLogger("TransformationService");
        this.objectMapper = objectMapper;
        this.scriptEngineMap = new ConcurrentHashMap<>();
        initializeScriptEngines();
    }

    private void initializeScriptEngines() {
        ScriptEngineManager manager = new ScriptEngineManager();

        // Initialize GraalJS Engine (with permissive host access)
        try {
            Context.Builder contextBuilder = Context.newBuilder("js").allowHostAccess(HostAccess.ALL);
            ScriptEngine graalJsEngine = GraalJSScriptEngine.create(null, contextBuilder);
            scriptEngineMap.put("graal.js", graalJsEngine);
            logger.info().message("GraalJS ScriptEngine initialized successfully.").log();
        } catch (Exception e) {
            logger.error(e).message("Failed to initialize GraalJS ScriptEngine.").log();
        }

        // Initialize Groovy Engine
        ScriptEngine groovyEngine = manager.getEngineByName("groovy");
        if (groovyEngine != null) {
            scriptEngineMap.put("groovy", groovyEngine);
            logger.info().message("Groovy ScriptEngine initialized successfully.").log();
        } else {
            logger.warn().message("Groovy ScriptEngine not found. Groovy scripts will not be supported.").log();
        }

        // Initialize BeanShell Engine
        ScriptEngine beanShellEngine = manager.getEngineByName("beanshell");
        if (beanShellEngine != null) {
            scriptEngineMap.put("beanshell", beanShellEngine);
            logger.info().message("BeanShell ScriptEngine initialized successfully.").log();
        } else {
            logger.warn().message("BeanShell ScriptEngine not found. BeanShell scripts will not be supported.").log();
        }
    }

    /**
     * Executes a transformation defined by a rule DTO against a set of source
     * attributes.
     *
     * @param ruleDTO          The mapping rule DTO. For script transforms, it MUST
     *                         contain the script content.
     * @param sourceAttributes A JsonNode representing the complete set of
     *                         attributes from the source object.
     * @return The transformed value as an Object, or null.
     */
    public Object getTransformedValue(MappingRuleDTO ruleDTO, JsonNode sourceAttributes) {
        JsonNode sourceValueNode = (ruleDTO.sourceAttributeName() != null
                && sourceAttributes.has(ruleDTO.sourceAttributeName()))
                        ? sourceAttributes.get(ruleDTO.sourceAttributeName())
                        : null;

        Object unwrappedSourceValue = unwrapValue(sourceValueNode);

        try {
            return switch (ruleDTO.transform().toLowerCase()) {
                case "direct" -> unwrappedSourceValue;
                case "lowercase" -> (unwrappedSourceValue instanceof String s) ? s.toLowerCase() : unwrappedSourceValue;
                case "constant" -> ruleDTO.constantValue();
                case "script" -> executeScript(ruleDTO, sourceAttributes); // LOGIC CHANGE: Script content is now
                                                                           // required
                default -> {
                    logger.warn().message("Unknown transform type in rule.").detail("transform", ruleDTO.transform())
                            .log();
                    yield null;
                }
            };
        } catch (Exception e) {
            logger.error(e).message("Error executing transformation for rule.").detail("rule", ruleDTO.toString())
                    .log();
            return null;
        }
    }

    private Object unwrapValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        // If the node is an array with exactly one element, return that element.
        if (valueNode.isArray() && valueNode.size() == 1) {
            return objectMapper.convertValue(valueNode.get(0), Object.class);
        }
        // Otherwise, return the node as is (or its value).
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        if (valueNode.isNumber()) {
            return valueNode.numberValue();
        }
        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        }

        // For arrays with more than one element or objects, convert them as is.
        return objectMapper.convertValue(valueNode, Object.class);
    }

    /**
     * Executes a script-based transformation. The rule DTO is now expected to have
     * the script content.
     */
    private Object executeScript(MappingRuleDTO ruleDTO, JsonNode sourceAttributes) throws ScriptException {
        if (ruleDTO.scriptContent() == null || ruleDTO.scriptContent().isBlank()) {
            logger.error().message("Script transform failed: scriptContent was not provided in the MappingRuleDTO.")
                    .detail("ruleId", ruleDTO.id()).log();
            return null;
        }

        String language = (ruleDTO.scriptLanguage() != null && !ruleDTO.scriptLanguage().isBlank())
                ? ruleDTO.scriptLanguage()
                : "graal.js";
        ScriptEngine engine = scriptEngineMap.get(language.toLowerCase());

        if (engine == null) {
            logger.error().message("No script engine found for requested language.").detail("language", language).log();
            return null;
        }

        // Convert the source JsonNode to a Map for easy, language-agnostic access in
        // scripts.
        Map<String, Object> attributesAsMap = objectMapper.convertValue(sourceAttributes, new TypeReference<>() {
        });
        Bindings bindings = new SimpleBindings(attributesAsMap);

        return engine.eval(ruleDTO.scriptContent(), bindings);
    }
}
package com.example.shelldemo.parser.storedproc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.shelldemo.exception.StoredProcedureParseException;
import com.example.shelldemo.validate.StoredProcedureValidator;

public final class StoredProcedureParser {
    private static final Logger logger = LogManager.getLogger(StoredProcedureParser.class);
    private static final String PARAM_DELIMITER = ",";
    private static final String FIELD_DELIMITER = ":";
    private static final Pattern PROCEDURE_PATTERN = Pattern.compile(
        "CREATE\\s+(OR\\s+REPLACE\\s+)?(FUNCTION|PROCEDURE)\\s+([^(]+)\\(([^)]*)\\)",
        Pattern.CASE_INSENSITIVE
    );

    private StoredProcedureParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    public static List<ProcedureParam> parse(String inputParams, String outputParams, String ioParams) {
        List<ProcedureParam> params = new ArrayList<>();
        try {
            if (inputParams != null && !inputParams.trim().isEmpty()) {
                params.addAll(parseParamString(inputParams, ParamType.IN));
            }
            if (outputParams != null && !outputParams.trim().isEmpty()) {
                params.addAll(parseParamString(outputParams, ParamType.OUT));
            }
            if (ioParams != null && !ioParams.trim().isEmpty()) {
                params.addAll(parseParamString(ioParams, ParamType.INOUT));
            }
            
            return Collections.unmodifiableList(params);
        } catch (Exception e) {
            String errorMessage = "Failed to parse procedure parameters";
            throw new StoredProcedureParseException(errorMessage, e);
        }
    }

    private static List<ProcedureParam> parseParamString(String paramString, ParamType type) {
        List<ProcedureParam> params = new ArrayList<>();
        String[] paramPairs = paramString.trim().split(PARAM_DELIMITER);
        
        for (int i = 0; i < paramPairs.length; i++) {
            String pair = paramPairs[i].trim();
            if (pair.isEmpty()) {
                continue;
            }

            String[] parts = pair.split(FIELD_DELIMITER);
            StoredProcedureValidator.validateParts(parts, type, i + 1);

            ProcedureParam param = new ProcedureParam.Builder()
                .name(parts[0].trim())
                .type(parts[1].trim())
                .value(parts.length > 2 ? parts[2].trim() : null)
                .paramType(type)
                .build();
                
            params.add(param);
        }
        
        return params;
    }

    public static StoredProcedureInfo parse(String definition) {
        logger.debug("Parsing stored procedure definition");
        StoredProcedureValidator.validateDefinition(definition);
        
        Matcher matcher = PROCEDURE_PATTERN.matcher(definition);
        if (!matcher.find()) {
            String errorMessage = "Invalid stored procedure definition format";
            throw new StoredProcedureParseException(
                errorMessage,
                StoredProcedureParseException.ERROR_CODE_INVALID_FORMAT
            );
        }

        String procedureName = matcher.group(3).trim();
        String parameters = matcher.group(4).trim();
        
        logger.debug("Successfully parsed stored procedure: {}", procedureName);
        return new StoredProcedureInfo(procedureName, parameters);
    }
} 
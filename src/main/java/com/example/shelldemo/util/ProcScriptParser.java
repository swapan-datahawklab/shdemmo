package com.example.shelldemo.util;
import java.util.ArrayList;
import java.util.List;

public class ProcScriptParser {
    public ProcScriptParser() {
        // Constructor
    }

    public static class ProcedureParam {
        private String name;
        private String type;
        private String value;
        private ParamType paramType;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public ParamType getParamType() {
            return paramType;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ParamType {
        IN, OUT, INOUT
    }

    public static List<ProcedureParam> parse(String inputParams, String outputParams, String ioParams) {
        List<ProcedureParam> params = new ArrayList<>();
        if (inputParams != null) {
            parseParamString(inputParams, params, ParamType.IN);
        }
        if (outputParams != null) {
            parseParamString(outputParams, params, ParamType.OUT);
        }
        if (ioParams != null) {
            parseParamString(ioParams, params, ParamType.INOUT);
        }
        return params;
    }

    private static void parseParamString(String paramString, List<ProcedureParam> params, ParamType type) {
        String[] paramPairs = paramString.split(",");
        for (String pair : paramPairs) {
            String[] parts = pair.split(":");
            if (parts.length >= 2) {
                ProcedureParam param = new ProcedureParam();
                param.name = parts[0];
                param.type = parts[1];
                param.value = parts.length > 2 ? parts[2] : null;
                param.paramType = type;
                params.add(param);
            }
        }
    }
} 
package com.example.shelldemo.sqlhandling;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class ResultSetProcessor {
    private final ResultSetMapper mapper;

    public ResultSetProcessor() {
        this.mapper = new ResultSetMapper();
    }

    public List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()) {
            results.add(mapper.mapRow(rs));
        }
        return results;
    }

    public Map<String, Object> processRow(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        Map<String, Object> row = new LinkedHashMap<>();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            if (columnName == null || columnName.isEmpty()) {
                columnName = metaData.getColumnName(i);
            }
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }
        
        return row;
    }

    public void processResultSetWithStreamer(ResultSet rs, ResultSetStreamer streamer, int batchSize) throws SQLException, IOException {
        BatchProcessor<Map<String, Object>> processor = new BatchProcessor<>(batchSize,
            batch -> {
                try {
                    streamer.stream(rs, batchSize);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        
        while (rs.next()) {
            processor.add(processRow(rs));
        }
        processor.flush();
    }

    private static class ResultSetMapper {
        public Map<String, Object> mapRow(ResultSet rs) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnLabel(i);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = metaData.getColumnName(i);
                }
                row.put(columnName, rs.getObject(i));
            }
            return row;
        }
    }
}

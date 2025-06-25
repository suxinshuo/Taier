/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.taier.datasource.plugin.kyuubi.client;

import com.dtstack.taier.datasource.api.dto.UpsertColumnMetaDTO;
import com.dtstack.taier.datasource.api.dto.source.RdbmsSourceDTO;
import com.dtstack.taier.datasource.api.source.DataSourceType;
import com.dtstack.taier.datasource.api.dto.source.ISourceDTO;
import com.dtstack.taier.datasource.api.exception.SourceException;
import com.dtstack.taier.datasource.plugin.rdbms.AbsTableClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class KyuubiTableClient extends AbsTableClient {

    private static final String TABLE_IS_VIEW_SQL = "desc formatted %s";
    private static final String ADD_COLUMN_SQL = "alter table %s add columns(%s %s comment '%s')";

    @Override
    protected DataSourceType getSourceType() {
        return DataSourceType.KYUUBI;
    }

    @Override
    public Boolean isView(ISourceDTO source, String schema, String tableName) {
        checkParamAndSetSchema(source, schema, tableName);
        String sql = String.format(TABLE_IS_VIEW_SQL, tableName);
        List<Map<String, Object>> result = executeQuery(source, sql);
        if (CollectionUtils.isEmpty(result)) {
            throw new SourceException(String.format("Execute to determine whether the table is a view sql result is empty，sql：%s", sql));
        }
        String tableType = "";
        for (Map<String, Object> row : result) {
            String colName = MapUtils.getString(row, "col_name");
            if (org.apache.commons.lang.StringUtils.containsIgnoreCase(colName, "Table Type")) {
                tableType = MapUtils.getString(row, "data_type");
                break;
            }
        }
        if (org.apache.commons.lang.StringUtils.isEmpty(tableType)) {
            for (Map<String, Object> row : result) {
                String colName = MapUtils.getString(row, "col_name");
                if (org.apache.commons.lang.StringUtils.containsIgnoreCase(colName, "Type")) {
                    tableType = MapUtils.getString(row, "data_type");
                    break;
                }
            }
        }
        log.info("table schema :{},table name:{}, type:{}", schema, tableName, tableType);
        return StringUtils.containsIgnoreCase(tableType, "VIEW");
    }

    protected Boolean addTableColumn(ISourceDTO source, UpsertColumnMetaDTO columnMetaDTO) {
        RdbmsSourceDTO rdbmsSourceDTO = (RdbmsSourceDTO) source;
        String schema = org.apache.commons.lang.StringUtils.isNotBlank(columnMetaDTO.getSchema()) ? columnMetaDTO.getSchema() : rdbmsSourceDTO.getSchema();
        String comment = org.apache.commons.lang.StringUtils.isNotBlank(columnMetaDTO.getColumnComment()) ? columnMetaDTO.getColumnComment() : "";
        String sql = String.format(ADD_COLUMN_SQL, transferSchemaAndTableName(schema, columnMetaDTO.getTableName()), columnMetaDTO.getColumnName(), columnMetaDTO.getColumnType(), comment);
        return executeSqlWithoutResultSet(source, sql);
    }
}
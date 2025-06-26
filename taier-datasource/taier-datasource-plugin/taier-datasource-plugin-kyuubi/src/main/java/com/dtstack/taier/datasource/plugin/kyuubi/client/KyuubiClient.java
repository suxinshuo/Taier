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

import com.alibaba.fastjson.JSONObject;
import com.dtstack.taier.datasource.api.downloader.IDownloader;
import com.dtstack.taier.datasource.api.dto.ColumnMetaDTO;
import com.dtstack.taier.datasource.api.dto.Table;
import com.dtstack.taier.datasource.api.dto.source.KyuubiSourceDTO;
import com.dtstack.taier.datasource.api.source.DataSourceType;
import com.dtstack.taier.datasource.api.dto.source.ISourceDTO;
import com.dtstack.taier.datasource.api.dto.SqlQueryDTO;
import com.dtstack.taier.datasource.api.exception.SourceException;
import com.dtstack.taier.datasource.plugin.common.utils.DBUtil;
import com.dtstack.taier.datasource.plugin.kyuubi.downloader.KyuubiJsonTextDownload;
import com.dtstack.taier.datasource.plugin.kyuubi.util.HdfsUtils;
import com.dtstack.taier.datasource.plugin.rdbms.AbsRdbmsClient;
import com.dtstack.taier.datasource.plugin.rdbms.ConnFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class KyuubiClient extends AbsRdbmsClient {

    private static final String SHOW_TABLE_SQL = "show tables";
    private static final String SHOW_TABLE_LIKE_SQL = "show tables like '%s'";

    @Override
    protected ConnFactory getConnFactory() {
        return new KyuubiConnFactory();
    }

    @Override
    protected DataSourceType getSourceType() {
        return DataSourceType.KYUUBI;
    }

    @Override
    public List<String> getTableList(ISourceDTO sourceDTO, SqlQueryDTO queryDTO) {
        Connection connection = getCon(sourceDTO, queryDTO);
        String sql;
        if (Objects.nonNull(queryDTO) && StringUtils.isNotEmpty(queryDTO.getTableNamePattern())) {
            sql = String.format(SHOW_TABLE_LIKE_SQL, addFuzzySign(queryDTO));
        } else {
            sql = SHOW_TABLE_SQL;
        }
        Statement statement = null;
        ResultSet rs = null;
        List<String> tableList = new ArrayList<>();
        try {
            statement = connection.createStatement();
            if (Objects.nonNull(queryDTO) && Objects.nonNull(queryDTO.getLimit())) {
                statement.setMaxRows(queryDTO.getLimit());
            }
            rs = statement.executeQuery(sql);
            int columnSize = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                tableList.add(rs.getString(columnSize == 1 ? 1 : 2));
            }
        } catch (Exception e) {
            throw new SourceException(String.format("get table exception,%s", e.getMessage()), e);
        } finally {
            DBUtil.closeDBResources(rs, statement, connection);
        }
        return tableList;
    }

    @Override
    public List<String> getTableListBySchema(ISourceDTO source, SqlQueryDTO queryDTO) {
        KyuubiSourceDTO kyuubiSourceDTO = (KyuubiSourceDTO) source;
        if (Objects.nonNull(queryDTO) && StringUtils.isNotBlank(queryDTO.getSchema())) {
            kyuubiSourceDTO.setSchema(queryDTO.getSchema());
        }
        return getTableList(kyuubiSourceDTO, queryDTO);
    }

    @Override
    public Boolean testCon(ISourceDTO sourceDTO) {
        return Boolean.TRUE;
    }

    @Override
    public Table getTable(ISourceDTO sourceDTO, SqlQueryDTO queryDTO) {
        Table table = new Table();
        // 读第一行数据, 解析字段信息
        String tableName = queryDTO.getTableName();
        KyuubiSourceDTO kyuubiSourceDTO = (KyuubiSourceDTO) sourceDTO;
        try {
            HdfsUtils hdfsUtils = new HdfsUtils(kyuubiSourceDTO);
            List<Path> filePaths = hdfsUtils.getFilePath(tableName);
            if (CollectionUtils.isEmpty(filePaths)) {
                return table;
            }
            JSONObject firstLine = hdfsUtils.getFirstLineJson(filePaths);
            if (Objects.isNull(firstLine)) {
                return table;
            }
            List<ColumnMetaDTO> columns = firstLine.keySet().stream().map(key -> {
                ColumnMetaDTO columnMetaDTO = new ColumnMetaDTO();
                columnMetaDTO.setKey(key);
                columnMetaDTO.setType("string");
                return columnMetaDTO;
            }).collect(Collectors.toList());
            table.setColumns(columns);
            return table;
        } catch (Exception e) {
            log.error("Failed to query table information.", e);
        }
        return table;
    }

    @Override
    public IDownloader getDownloader(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        return new KyuubiJsonTextDownload(source, queryDTO);
    }

}
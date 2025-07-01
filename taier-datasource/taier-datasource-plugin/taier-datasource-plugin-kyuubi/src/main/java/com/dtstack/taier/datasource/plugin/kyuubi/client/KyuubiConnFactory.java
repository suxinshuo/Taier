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

import com.dtstack.taier.datasource.api.dto.source.ISourceDTO;
import com.dtstack.taier.datasource.api.dto.source.KyuubiSourceDTO;
import com.dtstack.taier.datasource.api.exception.SourceException;
import com.dtstack.taier.datasource.api.source.DataBaseType;
import com.dtstack.taier.datasource.plugin.common.DtClassConsistent;
import com.dtstack.taier.datasource.plugin.common.exception.ErrorCode;
import com.dtstack.taier.datasource.plugin.common.utils.PropertiesUtil;
import com.dtstack.taier.datasource.plugin.kyuubi.KyuubiErrorPattern;
import com.dtstack.taier.datasource.plugin.rdbms.ConnFactory;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;

public class KyuubiConnFactory extends ConnFactory {

    public KyuubiConnFactory() {
        this.driverName = DataBaseType.KYUUBI.getDriverClassName();
        this.errorPattern = new KyuubiErrorPattern();
    }

    @Override
    public Connection getConn(ISourceDTO sourceDTO) throws Exception {
        init();
        KyuubiSourceDTO kyuubiSourceDTO = (KyuubiSourceDTO) sourceDTO;

        Properties properties = new Properties();
        properties.put(DtClassConsistent.PublicConsistent.USER, Optional.ofNullable(sourceDTO.getUsername()).orElse(""));
        properties.put(DtClassConsistent.PublicConsistent.PASSWORD, Optional.ofNullable(sourceDTO.getPassword()).orElse(""));

        Connection connection = DriverManager.getConnection(getConnUrl(kyuubiSourceDTO), properties);

        String schema = kyuubiSourceDTO.getSchema();
        if (StringUtils.isNotBlank(schema)) {
            connection.setSchema(schema);
        }
        return connection;
    }

    @Override
    protected boolean supportTransaction() {
        return false;
    }

    @Override
    protected boolean supportSelectSql() {
        return true;
    }

    @Override
    protected boolean supportProcedure(String sql) {
        return false;
    }

    @Override
    protected String getCallProc(String procName) {
        throw new SourceException(ErrorCode.NOT_SUPPORT.getDesc());
    }

    @Override
    protected String getDropProc(String procName) {
        throw new SourceException(ErrorCode.NOT_SUPPORT.getDesc());
    }

    private String getConnUrl(KyuubiSourceDTO kyuubiSourceDTO) {
        String url = kyuubiSourceDTO.getUrl();
        Properties sparkProp = PropertiesUtil.convertToPureProp(kyuubiSourceDTO, null, "spark.");
        if (sparkProp.isEmpty()) {
            return url;
        }
        if (StringUtils.contains(url, "#")) {
            String vars = StringUtils.split(url, "#")[1];
            url = StringUtils.split(url, "#")[0] + "#";
            for (String var : StringUtils.split(vars, ";")) {
                String[] varMapping = StringUtils.split(var, "=");
                if (varMapping.length != 2) {
                    continue;
                }
                String key = StringUtils.trim(varMapping[0]);
                String value = StringUtils.trim(varMapping[1]);
                if (!sparkProp.containsKey(key)) {
                    sparkProp.put(key, value);
                }
            }
        } else {
            url = StringUtils.endsWith(url, "#") ? url : url + "#";
        }
        StringJoiner joiner = new StringJoiner(";", url, "");
        sparkProp.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

}
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
import com.dtstack.taier.datasource.plugin.kyuubi.KyuubiErrorPattern;
import com.dtstack.taier.datasource.plugin.rdbms.ConnFactory;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.Properties;

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
        Connection connection = DriverManager.getConnection(kyuubiSourceDTO.getUrl(), properties);

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

}
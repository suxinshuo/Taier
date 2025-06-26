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

package com.dtstack.taier.datasource.plugin.kyuubi.util;

import com.alibaba.fastjson.JSONObject;
import com.dtstack.taier.datasource.api.dto.source.KyuubiSourceDTO;
import com.dtstack.taier.datasource.plugin.common.utils.JSONUtil;
import com.dtstack.taier.datasource.plugin.kerberos.core.hdfs.HadoopConfUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author suxinshuo
 * @date 2025/6/25 17:59
 */
public class HdfsUtils {

    private final FileSystem fs;
    private final KyuubiSourceDTO sourceDTO;

    public HdfsUtils(KyuubiSourceDTO sourceDTO) throws IOException {
        String defaultFs = sourceDTO.getDefaultFs();
        String hadoopConfig = sourceDTO.getHadoopConfig();
        Configuration conf = HadoopConfUtil.getHdfsConf(defaultFs, hadoopConfig, null);
        this.fs = FileSystem.get(conf);
        this.sourceDTO = sourceDTO;
    }

    /**
     * 获取表对应的文件路径
     *
     * @param tableName 表名
     * @return 文件路径
     */
    public List<Path> getFilePath(String tableName) throws IOException {
        String defaultFs = this.sourceDTO.getDefaultFs();
        String defaultResultPath = this.sourceDTO.getDefaultResultPath();
        String resultPath = defaultFs + defaultResultPath + tableName;
        // 找到目录下所有的文件
        FileStatus[] fileStatuses = this.fs.listStatus(
                new Path(resultPath),
                path -> !StringUtils.startsWith(path.getName(), ".") && !StringUtils.startsWith(path.getName(), "_SUCCESS")
        );
        if (Objects.isNull(fileStatuses) || fileStatuses.length == 0) {
            return Lists.newArrayList();
        }
        return Arrays.stream(fileStatuses).map(FileStatus::getPath).collect(Collectors.toList());
    }

    /**
     * 查找第一行数据
     *
     * @param filePaths 要查找的文件路径集合
     * @return JSONObject
     * @throws IOException 抛出的异常
     */
    public JSONObject getFirstLineJson(List<Path> filePaths) throws IOException {
        for (Path filePath : filePaths) {
            try (FSDataInputStream is = this.fs.open(filePath);
                 BufferedReader d = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String lineStr;
                while ((lineStr = d.readLine()) != null) {
                    if (StringUtils.isNotEmpty(lineStr)) {
                        return JSONUtil.parseJsonObject(lineStr);
                    }
                }
            }
        }
        return null;
    }

    public FSDataInputStream getFsDataInputStream(Path path) throws IOException {
        return this.fs.open(path);
    }

}

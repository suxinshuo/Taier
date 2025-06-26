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

package com.dtstack.taier.datasource.plugin.kyuubi.downloader;

import com.alibaba.fastjson.JSONObject;
import com.dtstack.taier.datasource.api.downloader.IDownloader;
import com.dtstack.taier.datasource.api.dto.SqlQueryDTO;
import com.dtstack.taier.datasource.api.dto.source.ISourceDTO;
import com.dtstack.taier.datasource.api.dto.source.KyuubiSourceDTO;
import com.dtstack.taier.datasource.plugin.common.utils.JSONUtil;
import com.dtstack.taier.datasource.plugin.kyuubi.util.HdfsUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author suxinshuo
 * @date 2025/6/25 17:50
 */
@Slf4j
public class KyuubiJsonTextDownload implements IDownloader {

    private final HdfsUtils hdfsUtils;
    private final List<Path> downloadFilePaths;
    private final List<String> queryFieldNames;

    private FSDataInputStream currentInputStream;
    private BufferedReader currentBufferedReader;
    private Integer currentPathIndex;
    private String nextLine;

    public KyuubiJsonTextDownload(ISourceDTO source, SqlQueryDTO queryDTO) {
        String tableName = queryDTO.getTableName();
        this.queryFieldNames = Optional.ofNullable(queryDTO.getColumns()).orElse(Lists.newArrayList());
        KyuubiSourceDTO kyuubiSourceDTO = (KyuubiSourceDTO) source;
        try {
            this.hdfsUtils = new HdfsUtils(kyuubiSourceDTO);
            this.downloadFilePaths = this.hdfsUtils.getFilePath(tableName);
            this.currentPathIndex = 0;
            findNextLine();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KyuubiJsonTextDownload", e);
        }
    }

    /**
     * 配置下载器
     *
     * @return 是否成功
     * @throws Exception 异常信息
     */
    @Override
    public boolean configure() throws Exception {
        return true;
    }

    /**
     * 获取元数据信息
     *
     * @return 元数据信息
     */
    @Override
    public List<String> getMetaInfo() {
        try {
            JSONObject firstLine = this.hdfsUtils.getFirstLineJson(this.downloadFilePaths);
            if (Objects.isNull(firstLine)) {
                return Lists.newArrayList();
            }
            return Lists.newArrayList(firstLine.keySet());
        } catch (Exception e) {
            log.error("Failed to query table information.", e);
        }
        return Lists.newArrayList();
    }

    /**
     * 读取下一行
     *
     * @return 下一行数据
     */
    @Override
    public Object readNext() {
        if (Objects.isNull(this.nextLine)) {
            return Lists.newArrayList();
        }
        List<String> result = Lists.newArrayList();
        JSONObject nextLineJson = JSONUtil.parseJsonObject(this.nextLine);
        for (String queryFieldName : this.queryFieldNames) {
            Object valueObj = nextLineJson.get(queryFieldName);
            result.add(Objects.nonNull(valueObj) ? valueObj.toString() : StringUtils.EMPTY);
        }
        // 找下一行数据并保存下来
        findNextLine();
        return result;
    }

    /**
     * 递归读取下一行数据
     */
    private void findNextLine() {
        if (this.currentPathIndex >= this.downloadFilePaths.size()) {
            this.nextLine = null;
            return;
        }
        try {
            if (Objects.isNull(this.currentBufferedReader)) {
                Path currentPath = this.downloadFilePaths.get(this.currentPathIndex);
                this.currentInputStream = this.hdfsUtils.getFsDataInputStream(currentPath);
                this.currentBufferedReader = new BufferedReader(new InputStreamReader(this.currentInputStream, StandardCharsets.UTF_8));
            }
            String line = this.currentBufferedReader.readLine();
            if (Objects.nonNull(line)) {
                this.nextLine = line;
                return;
            }
            // 当前这个文件读完了, 更新到下个文件
            this.currentBufferedReader = null;
            this.currentPathIndex++;
            findNextLine();
        } catch (IOException e) {
            throw new RuntimeException("Open InputStream failed.", e);
        }
    }

    /**
     * 是否读取到最后一行
     *
     * @return 是否是最后一行
     */
    @Override
    public boolean reachedEnd() {
        return Objects.isNull(this.nextLine);
    }

    /**
     * 关闭流
     *
     * @return 是否关闭成功
     * @throws Exception 异常信息
     */
    @Override
    public boolean close() throws Exception {
        if (Objects.nonNull(this.currentBufferedReader)) {
            this.currentBufferedReader.close();
        }
        if (Objects.nonNull(this.currentInputStream)) {
            this.currentInputStream.close();
        }
        return true;
    }

}

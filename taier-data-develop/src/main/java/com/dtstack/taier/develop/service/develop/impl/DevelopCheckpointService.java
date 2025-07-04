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

package com.dtstack.taier.develop.service.develop.impl;

import com.alibaba.fastjson.JSONObject;
import com.dtstack.taier.common.enums.EComponentType;
import com.dtstack.taier.common.exception.ErrorCode;
import com.dtstack.taier.common.exception.TaierDefineException;
import com.dtstack.taier.dao.domain.ScheduleJob;
import com.dtstack.taier.dao.domain.Task;
import com.dtstack.taier.develop.service.schedule.JobHistoryService;
import com.dtstack.taier.develop.vo.datasource.CheckPointListVO;
import com.dtstack.taier.pluginapi.constrant.ConfigConstant;
import com.dtstack.taier.pluginapi.enums.EDeployMode;
import com.dtstack.taier.pluginapi.pojo.FileResult;
import com.dtstack.taier.scheduler.executor.DatasourceOperator;
import com.dtstack.taier.scheduler.service.ClusterService;
import com.dtstack.taier.scheduler.service.ComponentService;
import com.dtstack.taier.scheduler.service.ScheduleDictService;
import com.dtstack.taier.scheduler.service.ScheduleJobService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * checkpoint 管理
 *
 * @author suxinshuo
 * @date 2025/7/4 10:42
 */
@Service
public class DevelopCheckpointService {

    @Resource
    private JobHistoryService jobHistoryService;

    @Resource
    private DatasourceOperator datasourceOperator;

    @Resource
    private ScheduleJobService scheduleJobService;

    @Resource
    private ClusterService clusterService;

    @Resource
    private ComponentService componentService;

    @Resource
    private DevelopTaskService developTaskService;

    @Resource
    private ScheduleDictService scheduleDictService;

    /**
     * 查询 checkpoint 目录下的文件列表
     *
     * @param checkPointVO 查询参数
     * @return 文件列表
     */
    public List<FileResult> listCheckPoint(CheckPointListVO checkPointVO) {
        String jobId = checkPointVO.getJobId();
        ScheduleJob scheduleJob = scheduleJobService.getByJobId(jobId);
        if (Objects.isNull(scheduleJob)) {
            throw new TaierDefineException(ErrorCode.CAN_NOT_FIND_JOB);
        }
        Long tenantId = scheduleJob.getTenantId();
        Task task = developTaskService.getByJobId(jobId, tenantId);
        if (Objects.isNull(task)) {
            throw new TaierDefineException(ErrorCode.CAN_NOT_FIND_TASK);
        }
        String applicationId = checkPointVO.getApplicationId();
        String engineId = jobHistoryService.getEngineIdByApplicationId(applicationId);
        if (StringUtils.isBlank(engineId)) {
            return Lists.newArrayList();
        }
        // 获取 job 对应的组件版本
        String componentVersionValue = scheduleDictService.convertVersionNameToValue(task.getComponentVersion(), task.getTaskType(), null);
        JSONObject configByKey = clusterService.getConfigByKey(tenantId, EComponentType.FLINK.getConfName(), componentVersionValue);
        JSONObject deployConfig = configByKey.getJSONObject(EDeployMode.PERJOB.getMode());
        String pointPathDir;
        if (checkPointVO.isGetSavePointPath()) {
            String prefixId = engineId.substring(0, 6);
            pointPathDir = deployConfig.getString(ConfigConstant.SAVE_POINTS_DIR) + File.separator + "savepoint-" + prefixId + "-*";
        } else {
            pointPathDir = deployConfig.getString(ConfigConstant.CHECK_POINTS_DIR) + File.separator + engineId;
        }
        if (StringUtils.isBlank(pointPathDir)) {
            throw new TaierDefineException(ErrorCode.CONFIG_ERROR);
        }
        Long clusterId = clusterService.getClusterIdByTenantId(tenantId);
        JSONObject pluginInfo = componentService.wrapperConfig(clusterId, EComponentType.HDFS.getTypeCode(), null, null);
        String typeName = componentService.buildHdfsTypeName(tenantId, clusterId);
        pluginInfo.put(ConfigConstant.TYPE_NAME_KEY, typeName);
        List<FileResult> fileResults = datasourceOperator.listFiles(pluginInfo, tenantId, pointPathDir, checkPointVO.isGetSavePointPath());
        fileResults = fileResults.stream().filter(file -> !file.getPath().endsWith("shared") && !file.getPath().endsWith("taskowned")).collect(Collectors.toList());
        return fileResults;
    }

}

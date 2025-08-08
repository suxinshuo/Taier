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

package com.dtstack.taier.common.alert.entity;

import com.dtstack.taier.common.annotation.VarName;

/**
 * 发送告警信息
 *
 * @author suxinshuo
 * @date 2025/7/16 17:08
 */
public class SendAlertEntity {

    @VarName("{{TASK_ID}}")
    private Long taskId;

    @VarName("{{TASK_NAME}}")
    private String taskName;

    @VarName("{{JOB_KEY}}")
    private String scheduleJobKey;

    @VarName("{{JOB_NAME}}")
    private String scheduleJobName;

    @VarName("{{NOW_TIME}}")
    private String nowTime;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getScheduleJobKey() {
        return scheduleJobKey;
    }

    public void setScheduleJobKey(String scheduleJobKey) {
        this.scheduleJobKey = scheduleJobKey;
    }

    public String getScheduleJobName() {
        return scheduleJobName;
    }

    public void setScheduleJobName(String scheduleJobName) {
        this.scheduleJobName = scheduleJobName;
    }

    public String getNowTime() {
        return nowTime;
    }

    public void setNowTime(String nowTime) {
        this.nowTime = nowTime;
    }

    @Override
    public String toString() {
        return "SendAlertEntity{" +
                "taskId=" + taskId +
                ", taskName='" + taskName + '\'' +
                ", scheduleJobKey='" + scheduleJobKey + '\'' +
                ", scheduleJobName='" + scheduleJobName + '\'' +
                ", nowTime='" + nowTime + '\'' +
                '}';
    }

}

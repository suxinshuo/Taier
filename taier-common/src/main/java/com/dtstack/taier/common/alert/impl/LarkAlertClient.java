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

package com.dtstack.taier.common.alert.impl;

import com.dtstack.taier.common.alert.AlertClient;
import com.dtstack.taier.common.alert.entity.SendAlertEntity;
import com.dtstack.taier.common.http.PoolHttpClient;
import com.dtstack.taier.common.util.FileUtil;
import com.dtstack.taier.common.util.JsonUtils;
import com.dtstack.taier.common.util.TemplateVarUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * lark 报警方式
 *
 * @author suxinshuo
 * @date 2025/7/16 16:59
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "alert.type",
        havingValue = "lark"
)
@PropertySource(value = "file:${user.dir.conf}/application.properties")
public class LarkAlertClient extends AlertClient {

    @Value("${alert.webhook:}")
    private String webhook;

    /**
     * 发送任务失败报警的消息模版 文件路径
     */
    @Value("${alert.failed_template.path:#{systemProperties['user.dir.conf']}/alert_template/lark_failed_template.json}")
    private String failedMsgTemplatePath;

    /**
     * 发送报警
     *
     * @param sendAlertEntity 报警信息
     */
    @Override
    public void sendJobFailed(SendAlertEntity sendAlertEntity) {
        log.debug("Send job failed alert. sendAlertEntity: {}", sendAlertEntity);
        // 获取发送消息内容
        String failedMsgTemplate = FileUtil.readContent(failedMsgTemplatePath);
        if (StringUtils.isBlank(failedMsgTemplate)) {
            log.error("Send job failed alert get message template failed. failedMsgTemplatePath: {}", failedMsgTemplatePath);
            return;
        }
        String failedMsg = TemplateVarUtils.matchTemplateVar(failedMsgTemplate, sendAlertEntity);
        log.debug("Send job failed alert message: {}", failedMsg);

        // 发送报警信息
        String responseBody = PoolHttpClient.post(webhook, failedMsg, null);
        log.debug("Send job failed alert. responseBody: {}", responseBody);
        if (StringUtils.isBlank(responseBody)) {
            log.error("Send job failed alert failed.");
            return;
        }
        JsonNode jsonNode = JsonUtils.parseJSON(responseBody);
        // code=0 表示请求成功
        if (Objects.isNull(jsonNode) || !Objects.equals(jsonNode.get("code").asLong(), 0L)) {
            log.error("Send job failed alert failed. responseBody: {}", responseBody);
        }
    }

}

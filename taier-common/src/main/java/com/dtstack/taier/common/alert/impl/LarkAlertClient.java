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
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * lark 报警方式
 *
 * @author suxinshuo
 * @date 2025/7/16 16:59
 */
public class LarkAlertClient extends AlertClient {

    private static final Logger logger = LoggerFactory.getLogger(LarkAlertClient.class);

    private final String webhook;

    private final String failedMsgTemplatePath;

    public LarkAlertClient(String webhook, String failedMsgTemplatePath) {
        this.webhook = webhook;
        this.failedMsgTemplatePath = failedMsgTemplatePath;
    }

    /**
     * 发送报警
     *
     * @param sendAlertEntity 报警信息
     */
    @Override
    public void sendJobFailed(SendAlertEntity sendAlertEntity) {
        logger.debug("Send job failed alert. sendAlertEntity: {}", sendAlertEntity);
        // 获取发送消息内容
        String failedMsgTemplate = FileUtil.readContent(failedMsgTemplatePath);
        if (StringUtils.isBlank(failedMsgTemplate)) {
            logger.error("Send job failed alert get message template failed. failedMsgTemplatePath: {}", failedMsgTemplatePath);
            return;
        }
        String failedMsg = TemplateVarUtils.matchTemplateVar(failedMsgTemplate, sendAlertEntity);
        logger.debug("Send job failed alert message: {}", failedMsg);

        // 发送报警信息
        String responseBody = PoolHttpClient.post(webhook, failedMsg, null);
        logger.debug("Send job failed alert. responseBody: {}", responseBody);
        if (StringUtils.isBlank(responseBody)) {
            logger.error("Send job failed alert failed.");
            return;
        }
        JsonNode jsonNode = JsonUtils.parseJSON(responseBody);
        // code=0 表示请求成功
        if (Objects.isNull(jsonNode) || !Objects.equals(jsonNode.get("code").asLong(), 0L)) {
            logger.error("Send job failed alert failed. responseBody: {}", responseBody);
        }
    }

}

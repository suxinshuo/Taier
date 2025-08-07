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

package com.dtstack.taier.common.alert;

import com.dtstack.taier.common.alert.impl.DefaultAlertClient;
import com.dtstack.taier.common.alert.impl.LarkAlertClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author suxinshuo
 * @date 2025/8/1 22:41
 */
@Component
public class AlertClientFactory {

    @Value("${alert.webhook:}")
    private String webhook;

    /**
     * 发送任务失败报警的消息模版 文件路径
     */
    @Value("${alert.failed_template.path:#{systemProperties['user.dir.conf']}/alert_template/lark_failed_template.json}")
    private String failedMsgTemplatePath;

    @Bean
    @ConditionalOnProperty(name = {"alert.type"}, havingValue = "lark")
    public AlertClient larkAlertClient() {
        return new LarkAlertClient(webhook, failedMsgTemplatePath);
    }

    @Bean
    @ConditionalOnMissingBean(AlertClient.class)
    public AlertClient defaultAlertClient() {
        return new DefaultAlertClient();
    }

}

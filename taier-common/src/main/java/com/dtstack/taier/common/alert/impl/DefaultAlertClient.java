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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认的实现
 *
 * @author suxinshuo
 * @date 2025/7/16 17:13
 */
@Component
@ConditionalOnMissingBean(AlertClient.class)
public class DefaultAlertClient extends AlertClient {

    /**
     * 发送任务失败报警
     *
     * @param sendAlertEntity 报警信息
     */
    @Override
    public void sendJobFailed(SendAlertEntity sendAlertEntity) {
        // do nothing
    }

}

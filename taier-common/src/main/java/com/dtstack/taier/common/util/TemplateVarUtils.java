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

package com.dtstack.taier.common.util;

import com.dtstack.taier.common.annotation.VarName;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * 模版变量工具类
 *
 * @author suxinshuo
 * @date 2025/7/16 17:57
 */
public class TemplateVarUtils {

    private static final Logger logger = LoggerFactory.getLogger(TemplateVarUtils.class);

    /**
     * 匹配并替换变量
     *
     * @param content 文本
     * @param obj  变量
     * @return 替换后的文本
     */
    public static String matchTemplateVar(String content, Object obj) {
        Map<String, String> templateVarMap = getTemplateVarMap(obj);
        return matchTemplateVar(content, templateVarMap);
    }

    /**
     * 匹配并替换变量
     *
     * @param content 文本
     * @param varMap  变量 map
     * @return 替换后的文本
     */
    public static String matchTemplateVar(String content, Map<String, String> varMap) {
        if (StringUtils.isEmpty(content) || CollectionUtils.isEmpty(varMap)) {
            logger.warn("matchTemplateVar 参数为空, content: {}, varMap: {}", content, varMap);
            return content;
        }
        String result = content;
        for (Map.Entry<String, String> entry : varMap.entrySet()) {
            String varName = entry.getKey();
            String value = entry.getValue();
            result = result.replace(varName, value);
        }
        return result;
    }

    /**
     * 获取模版变量 map
     *
     * @param obj 变量
     * @return map
     */
    private static Map<String, String> getTemplateVarMap(Object obj) {
        Map<String, String> varMap = Maps.newHashMap();
        try {
            Class<?> clazz = obj.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(Boolean.TRUE);
                Object value = field.get(obj);
                VarName varName = field.getAnnotation(VarName.class);
                if (Objects.isNull(varName) || Objects.isNull(varName.value())) {
                    continue;
                }
                if (Objects.isNull(value)) {
                    continue;
                }
                varMap.put(varName.value(), value.toString());
            }
        } catch (Exception e) {
            logger.error("转化变量失败", e);
        }
        return varMap;
    }

}

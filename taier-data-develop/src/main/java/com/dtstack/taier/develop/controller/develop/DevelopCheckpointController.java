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

package com.dtstack.taier.develop.controller.develop;

import com.dtstack.taier.common.lang.web.R;
import com.dtstack.taier.develop.mapstruct.vo.FileMapstructTransfer;
import com.dtstack.taier.develop.service.develop.impl.DevelopCheckpointService;
import com.dtstack.taier.develop.vo.datasource.CheckPointListVO;
import com.dtstack.taier.develop.vo.schedule.FileInfoVO;
import com.dtstack.taier.pluginapi.pojo.FileResult;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(value = "checkpoint管理", tags = {"checkpoint管理"})
@RestController
@RequestMapping(value = "/checkpoint")
public class DevelopCheckpointController {

    @Resource
    private DevelopCheckpointService developCheckpointService;

    @PostMapping(value = "/listCheckPoint")
    public R<List<FileInfoVO>> listCheckPoint(@RequestBody @Validated CheckPointListVO checkPointVO) throws Exception {
        List<FileResult> fileResults = developCheckpointService.listCheckPoint(checkPointVO);
        return R.ok(FileMapstructTransfer.INSTANCE.toInfoVO(fileResults));
    }

}

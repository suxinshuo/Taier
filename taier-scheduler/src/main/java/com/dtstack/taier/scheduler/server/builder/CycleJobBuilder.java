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

package com.dtstack.taier.scheduler.server.builder;

import com.dtstack.taier.common.enums.Deleted;
import com.dtstack.taier.common.enums.EScheduleStatus;
import com.dtstack.taier.common.enums.EScheduleType;
import com.dtstack.taier.common.exception.TaierDefineException;
import com.dtstack.taier.dao.domain.ScheduleTaskShade;
import com.dtstack.taier.pluginapi.util.DateUtil;
import com.dtstack.taier.pluginapi.util.RetryUtil;
import com.dtstack.taier.scheduler.server.ScheduleJobDetails;
import com.dtstack.taier.scheduler.service.JobGraphTriggerService;
import com.dtstack.taier.scheduler.utils.JobExecuteOrderUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 构造周期实例
 *
 * @author suxinshuo
 * @date 2025-07-16 10:57:00
 */
@Component
public class CycleJobBuilder extends AbstractJobBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CycleJobBuilder.class);

    private static final String CRON_JOB_NAME = "cronJob";

    @Resource
    protected JobGraphTriggerService jobGraphTriggerService;

    private final Lock lock = new ReentrantLock();

    @Transactional(rollbackFor = Exception.class)
    public void buildTaskJobGraph(String triggerDay) {
        if (!environmentContext.isOpenJobSchedule()) {
            return;
        }

        lock.lock();
        try {
            String triggerTimeStr = triggerDay + " 00:00:00";
            Timestamp triggerTime = Timestamp.valueOf(triggerTimeStr);

            // 检测今天是否已经生成过周期实例, 一天值生成一次周期实例
            boolean hasBuild = jobGraphTriggerService.checkHasBuildJobGraph(triggerTime);

            if (hasBuild) {
                LOGGER.info("trigger Day {} has build so break", triggerDay);
                return;
            }

            // 1. 获得今天预计要生成的所有周期实例
            Integer totalTask = getTotalTask();

            LOGGER.info("{} need build job : {}", triggerTimeStr, totalTask);
            if (totalTask <= 0) {
                saveJobGraph(triggerDay);
                return;
            }
            clearInterruptJob(triggerTime);
            // 2. 切割总数 限制 thread 并发
            int totalBatch = totalTask / environmentContext.getJobGraphTaskLimitSize();
            if (totalTask % environmentContext.getJobGraphTaskLimitSize() != 0) {
                totalBatch++;
            }

            Semaphore sph = new Semaphore(environmentContext.getMaxTaskBuildThread());
            CountDownLatch ctl = new CountDownLatch(totalBatch);
            AtomicJobSortWorker sortWorker = new AtomicJobSortWorker();

            // 3. 查询db多线程生成周期实例
            Long startId = 0L;
            AtomicBoolean buildFailed = new AtomicBoolean(false);
            for (int i = 0; i < totalBatch; i++) {
                // 默认取50个任务
                final List<ScheduleTaskShade> batchTaskShades = scheduleTaskService.listRunnableTask(startId,
                        Lists.newArrayList(EScheduleStatus.NORMAL.getVal(), EScheduleStatus.FREEZE.getVal()),
                        environmentContext.getJobGraphTaskLimitSize());

                // 如果取出来的任务集合是空的，处理周期实例生成过程中有任务被删除的情况
                if (CollectionUtils.isEmpty(batchTaskShades)) {
                    ctl.countDown();
                    continue;
                }

                startId = batchTaskShades.get(batchTaskShades.size() - 1).getId();
                LOGGER.info("job-number:{} startId:{}", i, startId);

                try {
                    sph.acquire();
                    jobGraphBuildPool.submit(() -> {
                        try {
                            for (ScheduleTaskShade batchTaskShade : batchTaskShades) {
                                List<ScheduleJobDetails> scheduleJobDetails = RetryUtil.executeWithRetry(() -> buildJob(batchTaskShade, triggerDay, sortWorker),
                                        environmentContext.getBuildJobErrorRetry(), 200, false);
                                // 插入周期实例
                                savaJobList(scheduleJobDetails);
                            }
                        } catch (Throwable e) {
                            LOGGER.error("!!! buildTaskJobGraph build job error !!!", e);
                            buildFailed.compareAndSet(false, true);
                        } finally {
                            sph.release();
                            ctl.countDown();
                        }
                    });

                    // 如果有构建实例失败, 直接跳出去
                    if (buildFailed.get()) {
                        break;
                    }
                } catch (Throwable e) {
                    LOGGER.error("[acquire pool error]:", e);
                    throw new TaierDefineException(e);
                }
            }
            ctl.await();
            // 整个构建失败, 不保存 JobGraph
            if (buildFailed.get()) {
                LOGGER.error("!!! buildTaskJobGraph Failed ！！！");
                return;
            }
            // 循环已经结束，说明周期实例已经全部生成了
            saveJobGraph(triggerDay);
        } catch (Exception e) {
            LOGGER.error("buildTaskJobGraph ！！！", e);
        } finally {
            LOGGER.info("buildTaskJobGraph exit & unlock ...");
            lock.unlock();
        }
    }

    private void clearInterruptJob(Timestamp triggerDay) {
        String date = DateUtil.getUnStandardFormattedDate(triggerDay.getTime());
        Long startExecuteOrder = JobExecuteOrderUtil.buildJobExecuteOrder(date, 0);
        LOGGER.info("clearInterruptJob start executor order {}", startExecuteOrder);
        scheduleJobService.clearInterruptJob(startExecuteOrder);
    }

    /**
     * 保存周期实例
     *
     * @param scheduleJobDetails 实例详情
     */
    @Transactional(rollbackFor = Exception.class)
    public void savaJobList(List<ScheduleJobDetails> scheduleJobDetails) {
        List<ScheduleJobDetails> savaJobDetails = Lists.newArrayList();
        for (ScheduleJobDetails scheduleJobDetail : scheduleJobDetails) {
            savaJobDetails.add(scheduleJobDetail);
            List<ScheduleJobDetails> flowBean = scheduleJobDetail.getFlowBean();

            if (CollectionUtils.isNotEmpty(flowBean)) {
                savaJobDetails.addAll(flowBean);
            }
        }

        scheduleJobService.insertJobList(savaJobDetails, getType());
    }

    /**
     * 保存生成的jobGraph记录
     */
    private void saveJobGraph(String triggerDay) {
        LOGGER.info("start saveJobGraph to db {}", triggerDay);
        // 记录当天job已经生成
        String triggerTimeStr = triggerDay + " 00:00:00";
        Timestamp timestamp = Timestamp.valueOf(triggerTimeStr);
        try {
            RetryUtil.executeWithRetry(() -> {
                jobGraphTriggerService.addJobTrigger(timestamp);
                return null;
            }, environmentContext.getBuildJobErrorRetry(), 200, false);
        } catch (Exception e) {
            LOGGER.error("addJobTrigger triggerTimeStr {} error ", triggerTimeStr, e);
            throw new TaierDefineException(e);
        }
    }

    private Integer getTotalTask() {
        return scheduleTaskService.lambdaQuery()
                .eq(ScheduleTaskShade::getIsDeleted, Deleted.NORMAL.getStatus())
                .in(ScheduleTaskShade::getScheduleStatus, Sets.newHashSet(EScheduleStatus.NORMAL.getVal(), EScheduleStatus.FREEZE.getVal()))
                .eq(ScheduleTaskShade::getFlowId, 0)
                .count();
    }

    @Override
    protected String getPrefix() {
        return CRON_JOB_NAME;
    }

    @Override
    protected Integer getType() {
        return EScheduleType.NORMAL_SCHEDULE.getType();
    }
}

package com.dtstack.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dtstack.engine.domain.ScheduleJob;
import com.dtstack.engine.domain.po.CountFillDataJobStatusPO;
import com.dtstack.engine.domain.po.SimpleScheduleJobPO;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * @Auther: dazhi
 * @Date: 2021/12/9 3:05 PM
 * @Email:dazhi@dtstack.com
 * @Description:
 */
public interface ScheduleJobMapper extends BaseMapper<ScheduleJob> {

    /**
     * 获得补数据实例运行的全部状态
     *
     * @param fillId   补数据id
     * @return
     */
    List<CountFillDataJobStatusPO> countByFillIdGetAllStatus(@Param("fillId") Set<Long> fillId);

    ScheduleJob getByTaskIdAndStatusOrderByIdLimit(@Param("taskId") Long taskId, @Param("status") Integer status, @Param("time") Timestamp time, @Param("type") Integer type);

    List<SimpleScheduleJobPO> listJobByStatusAddressAndPhaseStatus(@Param("startId") Long startId, @Param("statuses") List<Integer> statuses, @Param("nodeAddress") String nodeAddress, @Param("phaseStatus") Integer phaseStatus);

    void updateJobStatusAndExecTime(@Param("jobId") String jobId, @Param("status") int status);
}

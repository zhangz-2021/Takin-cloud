package io.shulie.takin.cloud.app.model.callback;

import lombok.Data;
import lombok.EqualsAndHashCode;

import io.shulie.takin.cloud.constant.enums.CallbackType;
import io.shulie.takin.cloud.app.model.callback.basic.Basic;
import io.shulie.takin.cloud.app.model.callback.basic.JobExample;
import io.shulie.takin.cloud.app.model.callback.JobExampleError.JobExampleErrorInfo;

/**
 * 任务实例异常
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobExampleError extends Basic<JobExampleErrorInfo> {

    private final CallbackType type = CallbackType.JMETER_ERROR;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class JobExampleErrorInfo extends JobExample {
        private String errorMessage;

        public JobExampleErrorInfo(JobExample jobExample) {
            setJobId(jobExample.getJobId());
            setResourceId(jobExample.getResourceId());
            setJobExampleId(jobExample.getJobExampleId());
            setResourceExampleId(jobExample.getResourceExampleId());
        }
    }
}
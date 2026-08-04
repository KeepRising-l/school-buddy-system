package com.buddy.task;

import com.buddy.service.CreditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 信誉分计算定时任务
 * 每天凌晨2点重新计算所有用户信誉分
 */
@Slf4j
@Component
public class CreditCalculateTask {

    @Autowired
    private CreditService creditService;

    /**
     * 每天凌晨2点执行：重新计算所有用户信誉分
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void recalculateCredit() {
        log.info("定时任务：开始重新计算所有用户信誉分");
        creditService.recalculateAllCredit();
        log.info("定时任务：信誉分重新计算完成");
    }
}

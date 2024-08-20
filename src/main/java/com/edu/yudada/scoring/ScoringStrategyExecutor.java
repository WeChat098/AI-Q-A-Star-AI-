package com.edu.yudada.scoring;

import com.edu.yudada.common.ErrorCode;
import com.edu.yudada.exception.BusinessException;
import com.edu.yudada.model.entity.App;
import com.edu.yudada.model.entity.UserAnswer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 评分策略执行器
 *
 * 根据不同的评分策略，首先使用注解获得这个类对应的注解信息，也就是元数据信息，之后得到对应的结果。
 */
@Service
public class ScoringStrategyExecutor {

    // 策略列表
    @Resource
    private List<ScoringStrategy> scoringStrategyList; // 所有只要实现了ScoringStrategy接口的Bean都会注入到ScoringStrategyList中
    // scoringStrategyList中包含了所属有的评分策略 只要实现了接口的Bean都会被注入进来
    /**
     * 评分
     *
     * @param choiceList
     * @param app
     * @return
     * @throws Exception
     */
    public UserAnswer doScore(List<String> choiceList,App app) throws Exception {
        Integer appType = app.getAppType();// 首先获取app的类型
        Integer appScoringStrategy = app.getScoringStrategy();// 获取app的评分策略
        if (appType == 0 && appScoringStrategy == 1){
            appScoringStrategy = 0;
        }
        if (appType == null || appScoringStrategy == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
        }
        // 根据注解获取策略
        for (ScoringStrategy strategy : scoringStrategyList) {
            // 检查当前的这个策略类是否有这个有使用这个注解
            if (strategy.getClass().isAnnotationPresent(ScoringStrategyConfig.class)) {
                // 获得当前这个策略类中对应的注解信息
                ScoringStrategyConfig scoringStrategyConfig = strategy.getClass().getAnnotation(ScoringStrategyConfig.class);
                if (scoringStrategyConfig.appType() == appType && scoringStrategyConfig.scoringStrategy() == appScoringStrategy) {
                    return strategy.doScore(choiceList, app);
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }
}

package com.edu.yudada.model.dto.question;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionContentDTO {// QuestionContentDTO中包含这道题目的标题和对象的Option 就是代表一道题目

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目选项列表
     */
    private List<Option> options;

    /**
     * 题目选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {// 代表一个选项
        private String result;
        private int score;
        private String value;
        private String key;
        //{"result":"I","value":"独自工作","key":"A"}
        // 从上面可以看到result对应的测评类的应用对应的是什么结果 比如I
        // score对应的得分类的应用，选了这个应用之后对应的结果应该是什么
        // 对应的是当前这个选项对应的题目是是什么
        //key是每一个选项都会有的，代表的是当前选择的是A 还是B
    }
}



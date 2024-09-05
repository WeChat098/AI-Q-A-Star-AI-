package com.edu.yudada.equalsTest;

/**
 * @Author : Yinghao Zhang
 * @Date : 2024/8/31 10:14
 * @Version : V1.2
 * @Description : 内部类进行测试
 */
// 成员内部类 表示一个类在另外一个类的内部
//public class OuterClass {
//    private String outerField = "Outer Field";
//
//    class InnerClass {
//        void display() {
//            System.out.println("Outer Field: " + outerField);// 成员内部类可以访问所有外部成员和方法，包含private；
//        }
//    }
//
//    public void createInner() {
//        InnerClass inner = new InnerClass();
//        inner.display();
//    }
//}
    // 静态内部类 与上面的区别是使用static进行修饰 只能获得static修饰的方法
//public class OuterClass {
//    private static String staticOuterField = "Static Outer Field";
//    public int age = 3;
//
//    static class StaticInnerClass {
//        void display() {
//            System.out.println("Static Outer Field: " + staticOuterField);
//
//        }
//    }
//
//    public static void createStaticInner() {
//        StaticInnerClass staticInner = new StaticInnerClass();
//        staticInner.display();
//    }
//}
    // 局部内部类
public class OuterClass {
    static String name = "zhangsan";
    public static void main(String[] args) {

    }
    OuterClass outerClass = new OuterClass();
    person p = outerClass.outerClass.p;

    class Node{
        int age;
        int score;
        public Node(int age,int score){
            this.age = age;
            this.score = score;
        }
    }
    static class person{
        int age;
        public person(int age) {
            this.age = age;
        }
    }
}


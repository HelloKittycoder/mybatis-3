package org.apache.ibatis.binding.functionalexpr;

import org.apache.ibatis.binding.functionalexpr.po.Person;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by shucheng on 2019-4-17 下午 14:03
 * Stream测试test
 */
public class StreamTest {

    private List<Person> personList;

    // 准备数据
    @BeforeEach
    public void init() {
        personList = new ArrayList<>();
        Person p = new Person("1", "张三", 10);
        personList.add(p);
        p = new Person("2", "李四", 25);
        personList.add(p);
        p = new Person("3", "王五", 30);
        personList.add(p);
    }

    @Test
    public void test1() {
        /*int count = 0;
        for (Person p : personList) {
            if (p.getAge() > 20) {
                count++;
            }
        }
        System.out.println(count);*/

        long count = personList.stream()
                .filter(person -> person.getAge() > 20)
                .count();
        System.out.println(count);
    }

    @Test
    public void test2() {
        List<String> collected = Stream.of("a", "b", "c")
                .collect(Collectors.toList());
        System.out.println(collected);
    }

    // 将字符串数组中的每个字母都转换成大写
    @Test
    public void test3() {
        List<String> collected = Stream.of("a", "b", "hello")
                .map(string -> string.toUpperCase())
                .collect(Collectors.toList());
        System.out.println(collected);
    }

    // 筛选第一个字符是数字的字符串
    @Test
    public void test4() {
        List<String> beginWithNumbers = Stream.of("a", "1abc", "abcl")
                .filter(value -> Character.isDigit(value.charAt(0)))
                .collect(Collectors.toList());
        System.out.println(beginWithNumbers);
    }

    // 将多个List合并成一个List
    @Test
    public void test5() {
        List<Integer> together = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4))
                .flatMap(numbers -> numbers.stream())
                .collect(Collectors.toList());
        System.out.println(together);
    }

    // 求最大值和最小值
    @Test
    public void test6() {
        List<Integer> list = Lists.newArrayList(3, 5, 2, 9, 1);
        int maxInt = list.stream()
                .max(Integer::compareTo)
                .get();
        int minInt = list.stream()
                .min(Integer::compareTo)
                .get();
        System.out.println("最大值为：" + maxInt);
        System.out.println("最小值为：" + minInt);
    }

    // reduce操作
    @Test
    public void test7() {
        int result = Stream.of(1, 2, 3, 4)
                .reduce(0, (acc, element) -> acc + element);
        System.out.println("累加值为：" + result);

        int result1 = Stream.of(1, 2, 3, 4)
                .reduce(1, (acc, element) -> acc * element);
        System.out.println("累乘值为：" + result1);
    }

    // 并行计算总长度
    @Test
    public void test8() {
        int sumSize = Stream.of("Apple", "Banana", "Orange", "Peer")
                .parallel()
                .map(s -> s.length())
                .reduce(Integer::sum)
                .get();
        System.out.println(sumSize);
    }

    // 元素排序
    @Test
    public void test9() {
        List<Integer> list = Lists.newArrayList(3, 5, 1, 10, 8);
        List<Integer> sortedList = list.stream()
                .sorted(Integer::compareTo)
                .collect(Collectors.toList());
        System.out.println(sortedList);
    }
}

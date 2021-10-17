package top.zuoyu.jpa;

import java.io.File;

import org.junit.jupiter.api.Test;

import top.zuoyu.jpa.data.enums.JdbcType;
import top.zuoyu.jpa.utils.ClassUtil;

/**
 * 无环境 .
 *
 * @author: zuoyu
 * @create: 2021-10-17 16:21
 */
public class NoContext {

    @Test
    public void loadClassPath() {
//        System.out.println(ClassUtil.getBasePath());
        System.out.println(JdbcType.valueOf(12).getJavaType().getName());
    }
}

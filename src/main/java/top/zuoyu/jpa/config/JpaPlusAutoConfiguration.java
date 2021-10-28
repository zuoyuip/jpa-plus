package top.zuoyu.jpa.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 自动装配 .
 *
 * @author: zuoyu
 * @create: 2021-10-28 16:20
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(JpaRepository.class)
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
public class JpaPlusAutoConfiguration {


}

package com.ruijie.listenevent.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.ruijie.listenevent.dao")
@Slf4j
public class MybatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setConfigLocation(new DefaultResourceLoader().getResource("mybatis-config.xml"));
        return sqlSessionFactoryBean.getObject();
    }

}

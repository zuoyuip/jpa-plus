package top.zuoyu.jpa;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ClassUtils;

import top.zuoyu.jpa.data.DataInfoLoad;
import top.zuoyu.jpa.data.model.Table;
import top.zuoyu.jpa.ssist.ModelStructure;
import top.zuoyu.jpa.temp.model.BaseModel;

@SpringBootTest
class JpaPlusApplicationTests {

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;


    @Test
    void contextLoads() {
        try {
            Connection connection = dataSource.getConnection();
            List<Table> tables = DataInfoLoad.getTables(connection);
            tables.forEach(table -> {
                table.getIndexs().forEach(System.out::println);
            });
//            tables.forEach(table -> {
//                ModelStructure.registerEntity(table);
//            });
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}

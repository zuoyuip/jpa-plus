package top.zuoyu.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaPlusApplicationTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void contextLoads() {
        entityManager.persist();

    }

}

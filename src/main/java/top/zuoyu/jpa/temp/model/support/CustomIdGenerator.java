package top.zuoyu.jpa.temp.model.support;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import top.zuoyu.jpa.utils.ReflectionUtils;

/**
 * 自定义自增 .
 *
 * @author: zuoyu
 * @create: 2021-10-28 14:56
 */
public class CustomIdGenerator extends IdentityGenerator implements Configurable {

    private String pkName;


    @Override
    public Serializable generate(SharedSessionContractImplementor s, Object obj) {
        Assert.notNull(pkName, "pkName must not be null");
        Object id = ReflectionUtils.getFieldValue(pkName, obj);
        if (id != null) {
            return (Serializable) id;
        }
        return super.generate(s, obj);
    }

    @Override
    public void configure(Type type, @NonNull Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        this.pkName = params.getProperty(PersistentIdentifierGenerator.PK);
    }
}

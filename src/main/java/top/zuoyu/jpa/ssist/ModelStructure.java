package top.zuoyu.jpa.ssist;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import top.zuoyu.jpa.data.enums.JdbcType;
import top.zuoyu.jpa.data.model.Column;
import top.zuoyu.jpa.data.model.Index;
import top.zuoyu.jpa.data.model.Table;
import top.zuoyu.jpa.exception.CustomException;
import top.zuoyu.jpa.temp.model.BaseModel;
import top.zuoyu.jpa.utils.ClassUtil;
import top.zuoyu.jpa.utils.StrUtil;

/**
 * 实体构建 .
 *
 * @author: zuoyu
 * @create: 2021-10-17 16:45
 */
public class ModelStructure {

    private static final CharSequence PACKAGE_SEPARATOR = ".";

    public static void registerEntity(@NonNull Table table) {
        ClassPool classPool = ClassPool.getDefault();
        String packageName = ClassUtils.getPackageName(BaseModel.class);
        // 创建一个空类
        CtClass ctClass = classPool.makeClass(packageName + PACKAGE_SEPARATOR + table.getTableName());
        ctClass.setModifiers(Modifier.PUBLIC);
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();

        try {
            // 实现Serializable接口
            CtClass serializableInterface = classPool.get(Serializable.class.getName());
            ctClass.addInterface(serializableInterface);

            // 实现Cloneable接口
            CtClass cloneableInterface = classPool.get(Cloneable.class.getName());
            ctClass.addInterface(cloneableInterface);

            // 添加serialVersionUID
            // TODO 待解决static和final
            CtClass longClass = classPool.get(Long.TYPE.getName());
            CtField serialVersionField = new CtField(longClass, "serialVersionUID", ctClass);
            serialVersionField.setModifiers(Modifier.PRIVATE);
            ctClass.addField(serialVersionField, CtField.Initializer.constant(ThreadLocalRandom.current().nextLong()));

            // 类上注解
            AnnotationsAttribute classAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation entityAnn = new Annotation("javax.persistence.Entity", constPool);
            Annotation tableAnn = new Annotation("javax.persistence.Table", constPool);
            classAttr.addAnnotation(entityAnn);
            tableAnn.addMemberValue("name", new StringMemberValue(table.getTableName(), constPool));
            ArrayMemberValue indexMemberValue = new ArrayMemberValue(constPool);
            Set<Index> indexs = table.getIndexs();
            AnnotationMemberValue[] elements = new AnnotationMemberValue[indexs.size()];
            for (int i = 0; i < indexs.size(); i++) {
                // TODO 赋值索引列表

            }
            indexMemberValue.setValue(elements);
            tableAnn.addMemberValue("indexes", indexMemberValue);

            // 添加无参的构造函数
            CtConstructor ctNotParamsConstructor = new CtConstructor(new CtClass[]{}, ctClass);
            ctClass.addConstructor(ctNotParamsConstructor);

        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
        }

        Collection<Column> columns = table.getColumns();
        columns.forEach(column -> {
            String dataType = column.getDataType();
            String columnName = column.getColumnName();
            int type = Integer.parseInt(dataType);
            try {
                // 新增字段
                CtClass typeClass = classPool.get(JdbcType.valueOf(type).getJavaType().getName());
                CtField param = new CtField(typeClass, columnName, ctClass);
                param.setModifiers(Modifier.PRIVATE);
                ctClass.addField(param);
                // 生成 getter、setter 方法
                ctClass.addMethod(CtNewMethod.getter("get" + StrUtil.captureName(columnName), param));
                ctClass.addMethod(CtNewMethod.setter("set" + StrUtil.captureName(columnName), param));
            } catch (NotFoundException | CannotCompileException e) {
                throw new CustomException("registerEntity is fail!", e);
            }
        });


        URL basePath = ClassUtil.getBasePath();
        try {
            ctClass.writeFile(basePath.getPath());
        } catch (CannotCompileException | IOException e) {
            throw new CustomException("writeFile is fail!", e);
        }
    }
}

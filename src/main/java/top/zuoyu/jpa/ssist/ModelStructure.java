package top.zuoyu.jpa.ssist;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
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
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String NULL = "null";

    public static void registerModel(@NonNull Table table) {
        ClassPool classPool = ClassPool.getDefault();
        String packageName = ClassUtils.getPackageName(BaseModel.class);
        // 创建一个空类
        CtClass ctClass = classPool.makeClass(packageName + PACKAGE_SEPARATOR + table.getTableName());
        ctClass.setModifiers(Modifier.PUBLIC);
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();

        registerEntity(classPool, classFile, ctClass, constPool, table);

        Collection<Column> columns = table.getColumns();
        columns.forEach(column -> registerField(classPool, ctClass, constPool, column));


        URL basePath = ClassUtil.getBasePath();
        try {
            ctClass.writeFile(basePath.getPath());
        } catch (CannotCompileException | IOException e) {
            throw new CustomException("writeFile is fail!", e);
        }
    }

    /**
     * 类部分的生成
     */
    public static void registerEntity(@NonNull ClassPool classPool, ClassFile classFile, @NonNull CtClass ctClass, ConstPool constPool, @NonNull Table table) {
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
            // 索引
            List<Index> indexs = table.getIndexs();
            ArrayMemberValue indexMemberValue = new ArrayMemberValue(constPool);
            AnnotationMemberValue[] elements = new AnnotationMemberValue[indexs.size()];
            for (int i = 0; i < indexs.size(); i++) {
                Annotation indexAnn = new Annotation("javax.persistence.Index", constPool);
                Index index = indexs.get(i);
                indexAnn.addMemberValue("name", new StringMemberValue(index.getIndexName(), constPool));
                indexAnn.addMemberValue("columnList", new StringMemberValue(index.getColumnName(), constPool));
                indexAnn.addMemberValue("unique", new BooleanMemberValue(!index.isNonUnique(), constPool));
                elements[i] = new AnnotationMemberValue(indexAnn, constPool);
            }
            indexMemberValue.setValue(elements);
            tableAnn.addMemberValue("indexes", indexMemberValue);
            classAttr.addAnnotation(tableAnn);
            classFile.addAttribute(classAttr);

            // 添加无参的构造函数
            CtConstructor ctNotParamsConstructor = new CtConstructor(new CtClass[]{}, ctClass);
            ctClass.addConstructor(ctNotParamsConstructor);

        } catch (CannotCompileException | NotFoundException e) {
            throw new CustomException("registerEntity is fail!", e);
        }
    }

    /**
     * 字段部分的生成
     */
    private static void registerField(@NonNull ClassPool classPool, CtClass ctClass, ConstPool constPool, @NonNull Column column) {
        String dataType = column.getDataType();
        String columnName = column.getColumnName();
        boolean isNullable = YES.equalsIgnoreCase(column.getIsNullable());
        Integer columnSize = column.getColumnSize();
        String columnDef = column.getColumnDef();
        int type = Integer.parseInt(dataType);
        try {
            // 新增字段
            CtClass typeClass = classPool.get(JdbcType.valueOf(type).getJavaType().getName());
            CtField field = new CtField(typeClass, columnName, ctClass);
            field.setModifiers(Modifier.PRIVATE);

            // 属性注解
            FieldInfo fieldInfo = field.getFieldInfo();
            AnnotationsAttribute fieldAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            // 是否主键
            if (column.isPrimaryKey()) {
                Annotation idAnn = new Annotation("javax.persistence.Id", constPool);
                fieldAttr.addAnnotation(idAnn);
                String isAutoincrement = column.getIsAutoincrement();
                // 是否自增
                if (YES.equalsIgnoreCase(isAutoincrement)) {
                    Annotation generatedValueAnn = new Annotation("javax.persistence.GeneratedValue", constPool);
                    EnumMemberValue generationTypeValue = new EnumMemberValue(constPool);
                    generationTypeValue.setType("javax.persistence.GenerationType");
                    generationTypeValue.setValue("IDENTITY");
                    generatedValueAnn.addMemberValue("strategy", generationTypeValue);
                    generatedValueAnn.addMemberValue("generator", new StringMemberValue("custom-id", constPool));
                    fieldAttr.addAnnotation(generatedValueAnn);

                    Annotation genericGeneratorAnn = new Annotation("org.hibernate.annotations.GenericGenerator", constPool);
                    genericGeneratorAnn.addMemberValue("name", new StringMemberValue("custom-id", constPool));
                    genericGeneratorAnn.addMemberValue("strategy", new StringMemberValue("top.zuoyu.jpa.temp.model.support.CustomIdGenerator", constPool));
                    fieldAttr.addAnnotation(genericGeneratorAnn);
                }
            }
            // 统一列
            Annotation columnAnn = new Annotation("javax.persistence.Column", constPool);
            columnAnn.addMemberValue("name", new StringMemberValue(columnName, constPool));
            columnAnn.addMemberValue("nullable", new BooleanMemberValue(isNullable, constPool));
            columnAnn.addMemberValue("length", new IntegerMemberValue(constPool, columnSize));
            fieldAttr.addAnnotation(columnAnn);
            // 是否存在默认值
            if (StringUtils.hasLength(columnDef) && !NULL.equalsIgnoreCase(columnDef)) {
                Annotation columnDefaultAnn = new Annotation("org.hibernate.annotations.ColumnDefault", constPool);
                columnDefaultAnn.addMemberValue("value", new StringMemberValue(columnDef, constPool));
                fieldAttr.addAnnotation(columnDefaultAnn);
            }
            // 是否为时间类型
            if (JdbcType.valueOf(type).getJavaType().isAssignableFrom(Date.class)) {
                Annotation temporalAnn = new Annotation("javax.persistence.Temporal", constPool);
                EnumMemberValue temporalValue = new EnumMemberValue(constPool);
                temporalValue.setType("javax.persistence.TemporalType");
                temporalValue.setValue("TIMESTAMP");
                temporalAnn.addMemberValue("value", temporalValue);
                fieldAttr.addAnnotation(temporalAnn);
            }
            fieldInfo.addAttribute(fieldAttr);
            ctClass.addField(field);
            // 生成 getter、setter 方法
            ctClass.addMethod(CtNewMethod.getter("get" + StrUtil.captureName(columnName), field));
            ctClass.addMethod(CtNewMethod.setter("set" + StrUtil.captureName(columnName), field));
        } catch (NotFoundException | CannotCompileException e) {
            throw new CustomException("registerField is fail!", e);
        }
    }
}

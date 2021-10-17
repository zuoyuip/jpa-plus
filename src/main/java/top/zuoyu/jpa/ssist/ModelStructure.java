package top.zuoyu.jpa.ssist;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import top.zuoyu.jpa.data.enums.JdbcType;
import top.zuoyu.jpa.data.model.Column;
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
        CtClass ctClass = classPool.makeClass(packageName + PACKAGE_SEPARATOR + table.getTableName());
        ctClass.setModifiers(Modifier.PUBLIC);

        Collection<Column> columns = table.getColumns();
        columns.forEach(column -> {
            String dataType = column.getDataType();
            String columnName = column.getColumnName();
            int type = Integer.parseInt(dataType);
            try {
                CtClass typeClass = classPool.get(JdbcType.valueOf(type).getJavaType().getName());
                CtField param = new CtField(typeClass, columnName, ctClass);
                param.setModifiers(Modifier.PRIVATE);
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

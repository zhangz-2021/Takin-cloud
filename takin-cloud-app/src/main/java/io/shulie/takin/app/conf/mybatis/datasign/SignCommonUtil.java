package io.shulie.takin.app.conf.mybatis.datasign;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableId;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.utils.security.MD5Utils;
import io.shulie.takin.cloud.data.annocation.EnableSign;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;

/**
 * @Author: 南风
 * @Date: 2022/3/2 2:52 下午
 */
@Slf4j
@Component
public class SignCommonUtil {


    private static SignCommonUtil instance;

    private SignCommonUtil() {
    }

    public static SignCommonUtil getInstance() {
        if (instance == null) {
            synchronized(SignCommonUtil.class) {
                if (instance == null) {
                    instance = new SignCommonUtil();
                }
            }
        }
        return instance;
    }

    public void setSign(MappedStatement mappedStatement, Object parameterObject, Statement statement, BoundSql boundSql) throws IllegalAccessException, SQLException, JSQLParserException {


        Class<?> clz = mappedStatement.getParameterMap().getType();
        if(clz == null){
            return;
        }
        boolean isSign = clz.isAnnotationPresent(EnableSign.class);
        if (isSign) {

            if (SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) {
                //新增方法获取id
                Field idField = ReflectUtil.getField(clz, "id");
                if (idField == null) {
                    //兼容 t_black_list
                    idField = ReflectUtil.getField(clz, "BLIST_ID");
                }
                idField.setAccessible(true);
                long id = Long.parseLong(idField.get(parameterObject).toString());
                Insert insert = (Insert) CCJSqlParserUtil.parse(boundSql.getSql());
                String tableName = insert.getTable().getName();
                String whereStr = " where " + idField.getAnnotation(TableId.class).value() + " = " + id;
                String querySql = "select * from " + tableName + whereStr;

                ResultSet rs = statement.getConnection().createStatement().executeQuery(querySql);
                ResultSetMetaData md = rs.getMetaData();
                Map<String, Object> map = new HashMap<>();
                while (rs.next()) {
                    for (int i = 0; i < md.getColumnCount(); i++) {
                        map.put(md.getColumnLabel(i + 1).toLowerCase(), rs.getObject(md.getColumnLabel(i + 1)));
                    }
                }
                //计算签名后再更新回去
                map.remove("sign");
                map.remove("gmt_create");
                map.remove("gmt_modified");
                map.remove("gmt_update");
                map.remove("create_time");
                map.remove("update_time");
                map.remove("UPDATE_TIME");
                map.remove("CREATE_TIME");
                String sign = MD5Utils.getInstance().getMD5(MapUtil.sort(map).toString());
                String updateSql = "update " + tableName + "  SET sign = " + "\'" + sign + "\'" + " where " + idField.getAnnotation(TableId.class).value() +" = "+ id;
                Connection connection = statement.getConnection();
                PreparedStatement ps1 = connection.prepareStatement(updateSql, PreparedStatement.RETURN_GENERATED_KEYS);
                ps1.execute();
                statement.close();
                return;
            }

            if (SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType())) {
                //解析sql,拿出where条件，构建查询sql获取更新范围的数据,进行验签
                String sql = boundSql.getSql();
                TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
                Configuration configuration = mappedStatement.getConfiguration();
                List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
                if (parameterMappings != null) {
                    for (ParameterMapping parameterMapping : parameterMappings) {
                        if (parameterMapping.getMode() != ParameterMode.OUT) {
                            Object value;
                            String propertyName = parameterMapping.getProperty();
                            if (boundSql.hasAdditionalParameter(propertyName)) {
                                value = boundSql.getAdditionalParameter(propertyName);
                            } else if (parameterObject == null) {
                                value = null;
                            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                                value = parameterObject;
                            } else {
                                value = configuration.newMetaObject(parameterObject).getValue(propertyName);
                            }

                            //类型处理
                            if (value instanceof LocalDateTime) {
                                value = DateUtil.format((LocalDateTime) value, DatePattern.NORM_DATETIME_PATTERN);
                            } else if (value instanceof Date) {
                                value = DateUtil.format((Date) value, DatePattern.NORM_DATETIME_PATTERN);
                            } else if (value instanceof Boolean) {
                                value = Boolean.FALSE.equals(value) ? "0" : "1";
                            }
                            String valueSet = String.valueOf(value).replaceAll("\\?", "!@#¥%");
                            sql= StringUtils.replaceOnce(sql,"?","'" + valueSet + "'");
                        }
                    }
                }
                sql = sql.replaceAll("!@#¥%", "\\?");

                Update update = (Update) CCJSqlParserUtil.parse(boundSql.getSql());
                String tableName = update.getTable().getName();
                String whereStr = " where" + sql.split("WHERE")[1];
                String querySql = "select * from " + tableName + whereStr;

                ResultSet rs = statement.getConnection().createStatement().executeQuery(querySql);
                ResultSetMetaData md = rs.getMetaData();
                Map<String, Object> map = new HashMap<>();
                List<String> sqlList = new ArrayList<>();
                while (rs.next()) {
                    for (int i = 0; i < md.getColumnCount(); i++) {
                        map.put(md.getColumnLabel(i + 1).toLowerCase(), rs.getObject(md.getColumnLabel(i + 1)));
                    }
                    //计算签名后再更新回去
                    map.remove("sign");
                    map.remove("gmt_create");
                    map.remove("gmt_modified");
                    map.remove("gmt_update");
                    map.remove("create_time");
                    map.remove("update_time");
                    map.remove("UPDATE_TIME");
                    map.remove("CREATE_TIME");
                    String sign = MD5Utils.getInstance().getMD5(MapUtil.sort(map).toString());
                    String id;
                    String updateSql = "";
                    if (map.containsKey("id")) {
                        id = map.get("id").toString();
                        updateSql = "update " + tableName + "  SET sign = " + "\'" + sign + "\'" + " where id = " + id;
                    }

                    if (map.containsKey("BLIST_ID")) {
                        id = map.get("BLIST_ID").toString();
                        updateSql = "update " + tableName + "  SET sign = " + "\'" + sign + "\'" + " where BLIST_ID = " + id;
                    }
                    sqlList.add(updateSql);
                }
                //rs.last();
                Connection connection = statement.getConnection();
                Statement st = connection.createStatement();
                for (String s : sqlList) {
                    if(st.isClosed()){
                        connection.createStatement().executeUpdate(s);
                    }else {
                        st.executeUpdate(s);
                    }
                }
            }

        }

    }


    public void preCheckData(MappedStatement mappedStatement, Object parameterObject, Statement statement, BoundSql boundSql) throws SQLException, JSQLParserException {
        boolean valid = true;
        if (SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType())) {
            //解析sql,拿出where条件，构建查询sql获取更新范围的数据,进行验签
            String sql = boundSql.getSql();
            TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
            Configuration configuration = mappedStatement.getConfiguration();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings != null) {
                for (ParameterMapping parameterMapping : parameterMappings) {
                    if (parameterMapping.getMode() != ParameterMode.OUT) {
                        Object value;
                        String propertyName = parameterMapping.getProperty();
                        if (boundSql.hasAdditionalParameter(propertyName)) {
                            value = boundSql.getAdditionalParameter(propertyName);
                        } else if (parameterObject == null) {
                            value = null;
                        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                            value = parameterObject;
                        } else {
                            value = configuration.newMetaObject(parameterObject).getValue(propertyName);
                        }

                        //类型处理
                        if (value instanceof LocalDateTime) {
                            value = DateUtil.format((LocalDateTime) value, DatePattern.NORM_DATETIME_PATTERN);
                        } else if (value instanceof Date) {
                            value = DateUtil.format((Date) value, DatePattern.NORM_DATETIME_PATTERN);
                        } else if (value instanceof Boolean) {
                            value = Boolean.FALSE.equals(value) ? "0" : "1";
                        }
                        //这里替换是为了防止value中存在?,导致sql替换出错
                        String valueSet = String.valueOf(value).replaceAll("\\?", "！@#¥%");
                        sql = StringUtils.replaceOnce(sql, "?", "'" + valueSet + "'");
                    }
                }
            }
            sql = sql.replaceAll("！@#¥%", "\\?");
            Update update = (Update) CCJSqlParserUtil.parse(boundSql.getSql());
            String tableName = update.getTable().getName();
            String whereStr = " where" + sql.split("WHERE")[1];
            String querySql = "select * from " + tableName + whereStr;

            ResultSet rs = statement.getConnection().createStatement().executeQuery(querySql);
            ResultSetMetaData md = rs.getMetaData();
            Map<String, Object> map = new HashMap<>();
            while (rs.next()) {

                for (int i = 0; i < md.getColumnCount(); i++) {
                    map.put(md.getColumnLabel(i + 1).toLowerCase(), rs.getObject(md.getColumnLabel(i + 1)));
                }

                if (map.get("sign") == null || Objects.equals(map.get("sign").toString(), "")) {
                    return;
                }

                String oldSign = String.valueOf(map.get("sign"));
                map.remove("sign");
                map.remove("gmt_create");
                map.remove("gmt_modified");
                map.remove("gmt_update");
                map.remove("create_time");
                map.remove("update_time");
                map.remove("UPDATE_TIME");
                map.remove("CREATE_TIME");
                String sign = MD5Utils.getInstance().getMD5(MapUtil.sort(map).toString());
                if (!oldSign.equals(sign)) {
                    log.error("【数据签名异常】-【pre-update-check】 sql:{}",querySql);
                    throw new TakinCloudException(TakinCloudExceptionEnum.DATA_SIGN_ERROR, "数据签名异常,请联系管理员!");
                }
            }
        }
    }

    public void validSign(MappedStatement mappedStatement, PreparedStatement ps,BoundSql boundSql) throws SQLException {
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        if(resultMaps.size() == 0){
            return;
        }
        Class<?> clz = mappedStatement.getResultMaps().get(0).getType();
        boolean isSign = clz.isAnnotationPresent(EnableSign.class);
        boolean valid = true;
        if (isSign) {
            ResultSet rs = ps.getResultSet();
            ResultSetMetaData md = rs.getMetaData();
            Map<String, Object> map = new HashMap<>();
            while (rs.next()) {
                for (int i = 0; i < md.getColumnCount(); i++) {
                    map.put(md.getColumnLabel(i + 1).toLowerCase(), rs.getObject(md.getColumnLabel(i + 1)));
                }
                if (map.get("sign") == null || Objects.equals(map.get("sign").toString(), "")) {
                    return;
                }
                String oldSign = String.valueOf(map.get("sign"));
                map.remove("sign");
                map.remove("gmt_create");
                map.remove("gmt_modified");
                map.remove("gmt_update");
                map.remove("create_time");
                map.remove("update_time");
                map.remove("UPDATE_TIME");
                map.remove("CREATE_TIME");

                String sign = MD5Utils.getInstance().getMD5(MapUtil.sort(map).toString());
                if (!oldSign.equals(sign)) {
                    log.error("【数据签名异常】-【select】 map:{} , sql:{}", JSON.toJSONString(map),boundSql.getSql());
                    throw new TakinCloudException(TakinCloudExceptionEnum.DATA_SIGN_ERROR, "数据签名异常,请联系管理员!");

                }
            }
        }
    }


}
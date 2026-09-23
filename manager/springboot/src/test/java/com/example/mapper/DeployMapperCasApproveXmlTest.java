package com.example.mapper;

import com.example.common.enums.DeployStatusEnum;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * DeployMapper.xml 里的 casApprove 在纯 Mockito 单元测试里不会被执行，这里有针对性地用真实 MyBatis
 * 解析并渲染它，避免出现「SQL 写错但测试全绿」：
 * 1. 整个 XML 能被 MyBatis 解析（DTD 校验、语句 id 不重复）
 * 2. remark 为 null / 非 null 两种分支渲染出的 SQL 都没有多余或缺失的逗号
 * 3. where 条件带上 id + deleted + 期望状态 + row_version 四重保护
 * 4. XML 里每个 #{} 都能对上 @Param 声明的参数名，并且七个参数都能被真实绑定
 */
class DeployMapperCasApproveXmlTest {

    private static final String STATEMENT = "com.example.mapper.DeployMapper.casApprove";

    private static final Set<String> DECLARED_PARAMS = new LinkedHashSet<String>(Arrays.asList(
            "id", "expectStatus", "rowVersion", "status", "approver", "approveTime", "remark"));

    private Configuration configuration;

    @BeforeEach
    void parseRealMapperXml() throws Exception {
        configuration = new Configuration();
        configuration.setEnvironment(new Environment("qa-xml", new JdbcTransactionFactory(), new UnpooledDataSource()));
        try (InputStream in = Resources.getResourceAsStream("mapper/DeployMapper.xml")) {
            XMLMapperBuilder builder = new XMLMapperBuilder(in, configuration,
                    "mapper/DeployMapper.xml", configuration.getSqlFragments());
            builder.parse();
        }
        // builder.parse() 会按 namespace 自动把 DeployMapper 接口注册进 MapperRegistry
    }

    @Test
    void parseSucceeds_andCasApproveIsRegisteredOnce() {
        List<String> casApprove = new ArrayList<String>();
        for (Object name : configuration.getMappedStatementNames()) {
            if (name.toString().endsWith(".casApprove")) {
                casApprove.add(name.toString());
            }
        }
        assertEquals(1, casApprove.size(), "casApprove 语句必须且只能注册一次，实际：" + casApprove);
        assertEquals(STATEMENT, casApprove.get(0));

        MappedStatement ms = configuration.getMappedStatement(STATEMENT);
        assertEquals(SqlCommandType.UPDATE, ms.getSqlCommandType());
    }

    @Test
    void mapperMethod_declaresSevenNamedParams() throws Exception {
        Method method = DeployMapper.class.getMethod("casApprove", Integer.class, String.class,
                Integer.class, String.class, String.class, String.class, String.class);
        assertEquals(7, method.getParameterCount());

        Set<String> names = new LinkedHashSet<String>();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            boolean hasParam = false;
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof Param) {
                    names.add(((Param) annotation).value());
                    hasParam = true;
                }
            }
            assertTrue(hasParam, "casApprove 第 " + (i + 1) + " 个参数缺少 @Param 注解");
        }
        assertEquals(DECLARED_PARAMS, names);
    }

    @Test
    void renderSql_withRemark_hasNoCommaIssue() {
        String sql = renderSql("同意上线");

        assertTrue(sql.startsWith("update deploy"), sql);
        assertTrue(sql.contains("status = ?"), sql);
        assertTrue(sql.contains("row_version = row_version + 1"), sql);
        assertTrue(sql.contains("approver = ?"), sql);
        assertTrue(sql.contains("approve_time = ?"), sql);
        assertTrue(sql.contains("remark = ?"), sql);
        assertSingleUpdate(sql);
        assertNoCommaGlitch(sql);
    }

    @Test
    void renderSql_withoutRemark_hasNoCommaIssueAndKeepsOldRemark() {
        String sql = renderSql(null);

        assertTrue(sql.contains("approve_time = ?"), sql);
        assertFalse(sql.contains("remark"), "remark 为 null 时不应出现在 SET 列表里，保留库里原有的审批意见");
        assertSingleUpdate(sql);
        assertNoCommaGlitch(sql);
    }

    @Test
    void whereClause_guardsWithIdDeletedStatusAndRowVersion() {
        String sql = renderSql("同意上线");

        assertTrue(sql.contains("id = ?"), sql);
        assertTrue(sql.contains("deleted = 0"), sql);
        assertTrue(sql.contains("status = ?"), sql);
        assertTrue(sql.contains("row_version = ?"), sql);
        assertTrue(sql.indexOf("row_version = ?") > sql.indexOf(" where "),
                "row_version 必须出现在 where 里做乐观锁，而不仅仅出现在 set 里：" + sql);
    }

    @Test
    void everyPlaceholder_mapsToDeclaredParam_andCanBeBound() throws Exception {
        Map<String, Object> params = params("同意上线");
        MappedStatement ms = configuration.getMappedStatement(STATEMENT);
        BoundSql boundSql = ms.getBoundSql(params);

        assertEquals(7, boundSql.getParameterMappings().size(), "XML 里的 #{} 数量应与 @Param 数量一致");
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String property = mapping.getProperty();
            assertTrue(DECLARED_PARAMS.contains(property),
                    "XML 里的 #{...} 参数 '" + property + "' 不在 @Param 声明列表里");
            // ParamMap.get 对不存在的 key 抛 BindingException，这里顺便触发一次
            params.get(property);
        }

        PreparedStatement statement = mock(PreparedStatement.class);
        ParameterHandler handler = configuration.newParameterHandler(ms, params, boundSql);
        handler.setParameters(statement);
    }

    private void assertSingleUpdate(String sql) {
        String lower = sql.toLowerCase();
        assertEquals(lower.indexOf("update"), lower.lastIndexOf("update"), "只允许一条 update，禁止两段式更新：" + sql);
        assertFalse(sql.contains(";"), "不允许拼接多条语句：" + sql);
    }

    private void assertNoCommaGlitch(String sql) {
        assertFalse(sql.matches("(?s).*,\\s*,.*"), "SQL 里出现连续逗号：" + sql);
        assertFalse(sql.matches("(?s).*,\\s*where.*"), "set 列表出现尾逗号：" + sql);
        assertFalse(sql.trim().endsWith(","), "SQL 以逗号结尾：" + sql);
    }

    private String renderSql(String remark) {
        MappedStatement ms = configuration.getMappedStatement(STATEMENT);
        BoundSql boundSql = ms.getBoundSql(params(remark));
        return boundSql.getSql().replaceAll("\\s+", " ");
    }

    /**
     * 还原 MyBatis 对多参数方法的处理：所有 @Param 名字都进 ParamMap，值可以为 null。
     */
    private Map<String, Object> params(String remark) {
        Map<String, Object> params = new MapperMethod.ParamMap<Object>();
        params.put("id", 1);
        params.put("expectStatus", DeployStatusEnum.PENDING.name());
        params.put("rowVersion", 3);
        params.put("status", DeployStatusEnum.APPROVED.name());
        params.put("approver", "ops1");
        params.put("approveTime", "2026-09-23 14:00:00");
        params.put("remark", remark);
        return params;
    }
}

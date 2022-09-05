import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcDataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcDataSourcesUploadInfo;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableInfos;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableUploadInfo;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcConnectionUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

public class NormalTest1 {

    private static String table = "m_user";
    private static String userName = "c##ws_test";
    private static String password = "oracle";
    private static String driverClassName = "oracle.jdbc.driver.OracleDriver";
    private static String url = "jdbc:oracle:thin:@192.168.1.208:1521:ORCL";
    //    private static String table = "user";
    //    private static String url = "jdbc:gbase://192.168.1.223:5258/atester";
    //    private static String userName = "root";
    //    private static String password = "gbase";
//    private static String driverClassName = "com.gbase.jdbc.Driver";
//    private static String driverClassName = "com.informix.jdbc.IfxDriver";

    public static void main(String[] args) throws Exception {
        printJdbcTableInfos();
    }


    private static void printJdbcDataSourcesUploadInfo() throws UnknownHostException {
        JdbcDataSourceEntity entity = new JdbcDataSourceEntity();
        entity.setUrl("jdbc:mysql://192.168.1.46:3306/atester?useUnicode=true");
        entity.setUserName("admin");
        entity.setPassword("**********");
        entity.setDriverClassName("com.mysql.jdbc.Driver");

        JdbcDataSourceEntity entity1 = new JdbcDataSourceEntity();
        entity1.setUrl("jdbc:oracle:thin:@192.168.1.208:1521:ORCL");
        entity1.setUserName("c##ws_test");
        entity1.setPassword("******");
        entity1.setDriverClassName("oracle.jdbc.driver.OracleDriver");

        JdbcDataSourcesUploadInfo uploadInfo = new JdbcDataSourcesUploadInfo();
        uploadInfo.setAppName("agent-jdbc");
        uploadInfo.setDataSourceEntities(Arrays.asList(entity, entity1));
        uploadInfo.setHost(InetAddress.getLocalHost().getHostAddress());

        System.out.println(JSON.toJSONString(uploadInfo));
    }

    private static void printJdbcTableInfos() throws Exception {
        JdbcTableUploadInfo uploadInfo = new JdbcTableUploadInfo();
        uploadInfo.setAppName("agent-jdbc");

        JdbcDataSourceEntity entity = new JdbcDataSourceEntity();
        entity.setUrl(url);
        entity.setUserName(userName);
        entity.setPassword(password);
        entity.setDriverClassName(driverClassName);
        uploadInfo.setDataSourceEntity(entity);

        uploadInfo.setTableInfos(Arrays.asList(buildTableInfos(table)));


        System.out.println(JSON.toJSONString(uploadInfo));
    }

    private static JdbcTableInfos buildTableInfos(String table) {
        Connection connection = null;
        JdbcTableInfos tableInfos = new JdbcTableInfos();
        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(url, userName, password);
            tableInfos.setTableName(table);
            tableInfos.setColumns(JdbcConnectionUtils.fetchColumnInfos(connection, table));
//        tableInfos.setCreateTableSql(JdbcConnectionUtils.buildCreateTableSql(table, tableInfos.getColumns()));
            JdbcConnectionUtils.fetchAllTables(connection);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tableInfos;
    }

    private static void checkShadowTablesAvailable() {
        Map<String, String> result = JdbcConnectionUtils.checkConnectionAndTableAvailable(driverClassName, url, userName, password, "user", "pt_user");
        System.out.println(JSON.toJSONString(result));
    }


}

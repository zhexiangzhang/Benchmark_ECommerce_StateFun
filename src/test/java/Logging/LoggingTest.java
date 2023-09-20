package Logging;

import Utils.PostgreHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LoggingTest {

    Connection conn;

    @Before
    public final void setUp() throws SQLException {
        conn = PostgreHelper.getConnection();
        PostgreHelper.initLogTable(conn);
    }

    @Test
    public void simpleLoggingExample() throws SQLException {

        Statement st = conn.createStatement();
        st.execute("INSERT INTO public.log (\"type\",\"key\",\"value\") VALUES ('test','test','test')");
        st.close();
        // INSERT INTO public.""{0}"" (""type"",""key"",""value"") VALUES ('{1}','{2}','{3}')

    }

    @After
    public final void after() throws SQLException {
        // PostgreHelper.truncateLogTable(conn);
    }

}

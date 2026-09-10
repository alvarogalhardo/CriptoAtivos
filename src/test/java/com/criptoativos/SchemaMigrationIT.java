package com.criptoativos;

import static org.assertj.core.api.Assertions.assertThat;

import com.criptoativos.support.AbstractIT;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SchemaMigrationIT extends AbstractIT {

    @Autowired DataSource dataSource;

    @Test
    void flywayCreatesEveryDomainTable() throws Exception {
        assertThat(tableNames())
                .contains(
                        "users",
                        "user_recovery_codes",
                        "assets",
                        "asset_inventory",
                        "wallets",
                        "holdings",
                        "operations",
                        "flyway_schema_history");
    }

    @Test
    void moneyColumnsAreNumericNeverFloatingPoint() throws Exception {
        assertThat(columnType("wallets", "cash_balance")).isEqualTo("numeric");
        assertThat(columnType("assets", "current_price")).isEqualTo("numeric");
        assertThat(columnType("holdings", "quantity")).isEqualTo("numeric");
        assertThat(columnType("operations", "total_amount")).isEqualTo("numeric");
    }

    private List<String> tableNames() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet rs =
                        connection
                                .getMetaData()
                                .getTables(null, "public", "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private String columnType(String table, String column) throws Exception {
        try (Connection connection = dataSource.getConnection();
                ResultSet rs = connection.getMetaData().getColumns(null, "public", table, column)) {
            assertThat(rs.next()).as("column %s.%s exists", table, column).isTrue();
            return rs.getString("TYPE_NAME");
        }
    }
}

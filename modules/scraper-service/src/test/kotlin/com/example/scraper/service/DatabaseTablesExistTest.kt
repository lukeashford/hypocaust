package com.example.scraper.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseTablesExistTest {

  @Test
  fun verifySourceDocTableExists() {
    var connection: Connection? = null
    try {
      // Connect to the PostgreSQL database
      val url = "jdbc:postgresql://localhost:5433/agents"
      val user = "agents"
      val password = "agents"

      println("[DEBUG_LOG] Connecting to database: $url")
      connection = DriverManager.getConnection(url, user, password)
      println("[DEBUG_LOG] Connected to database successfully")

      // Check if the source_doc table exists
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery(
        """
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' 
          AND table_name = 'source_doc'
        )
        """
      )

      var tableExists = false
      if (resultSet.next()) {
        tableExists = resultSet.getBoolean(1)
      }

      println("[DEBUG_LOG] Source_doc table exists: $tableExists")

      // List all tables in the public schema
      val tablesResultSet = statement.executeQuery(
        """
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public'
        """
      )

      println("[DEBUG_LOG] Tables in public schema:")
      val tables = mutableListOf<String>()
      while (tablesResultSet.next()) {
        val tableName = tablesResultSet.getString(1)
        tables.add(tableName)
        println("[DEBUG_LOG] - $tableName")
      }

      if (tables.isEmpty()) {
        println("[DEBUG_LOG] No tables found in public schema")
      }

      // Also check for Liquibase tables
      val liquibaseTablesResultSet = statement.executeQuery(
        """
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name LIKE 'databasechangelog%'
        """
      )

      println("[DEBUG_LOG] Liquibase tables in public schema:")
      val liquibaseTables = mutableListOf<String>()
      while (liquibaseTablesResultSet.next()) {
        val tableName = liquibaseTablesResultSet.getString(1)
        liquibaseTables.add(tableName)
        println("[DEBUG_LOG] - $tableName")
      }

      if (liquibaseTables.isEmpty()) {
        println("[DEBUG_LOG] No Liquibase tables found in public schema")
      }

      assertTrue(tableExists, "The source_doc table does not exist in the public schema")

    } catch (e: SQLException) {
      println("[DEBUG_LOG] Database error: ${e.message}")
      e.printStackTrace()
      throw e
    } finally {
      try {
        connection?.close()
      } catch (e: SQLException) {
        println("[DEBUG_LOG] Error closing connection: ${e.message}")
      }
    }
  }
}

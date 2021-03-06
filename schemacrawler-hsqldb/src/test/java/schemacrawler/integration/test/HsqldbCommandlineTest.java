/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2019, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/
package schemacrawler.integration.test;


import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.fileResource;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAndTypeAs;
import static schemacrawler.test.utility.TestUtility.flattenCommandlineArgs;
import static sf.util.DatabaseUtility.checkConnection;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import schemacrawler.Main;
import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaRetrievalOptions;
import schemacrawler.server.hsqldb.HyperSQLDatabaseConnector;
import schemacrawler.test.utility.BaseDatabaseTest;
import schemacrawler.test.utility.TestWriter;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.options.OutputFormat;
import schemacrawler.tools.options.TextOutputFormat;
import sf.util.IOUtility;

public class HsqldbCommandlineTest
  extends BaseDatabaseTest
{

  @Test
  public void testHsqldbMain()
    throws Exception
  {

    final Path testConfigFile = IOUtility.createTempFilePath("test",
                                                             "properties");
    try (
        final Writer writer = new PrintWriter(newBufferedWriter(testConfigFile,
                                                                UTF_8,
                                                                WRITE,
                                                                TRUNCATE_EXISTING,
                                                                CREATE));)
    {
      final Properties properties = new Properties();
      properties
        .setProperty("hsqldb.tables",
                     "SELECT TABLE_CAT, TABLE_SCHEM, TABLE_NAME, TABLE_TYPE, REMARKS FROM INFORMATION_SCHEMA.SYSTEM_TABLES");
      properties.store(writer, "testHsqldbMain");
    }

    final OutputFormat outputFormat = TextOutputFormat.text;
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout;)
    {
      final Map<String, String> argsMap = new HashMap<>();
      argsMap.put("server", "hsqldb");
      argsMap.put("database", "schemacrawler");
      argsMap.put("user", "sa");
      argsMap.put("password", null);
      argsMap.put("g", testConfigFile.toString());
      argsMap.put("noinfo", Boolean.FALSE.toString());
      argsMap.put("command", "details,dump,count,hsqldb.tables");
      argsMap.put("infolevel", "maximum");
      argsMap.put("synonyms", ".*");
      argsMap.put("routines", ".*");
      argsMap.put("outputfile", out.toString());

      Main.main(flattenCommandlineArgs(argsMap));
    }
    assertThat(fileResource(testout),
               hasSameContentAndTypeAs(classpathResource("hsqldb.main" + "."
                                                         + outputFormat
                                                           .getFormat()),
                                       outputFormat.getFormat()));
  }

  @Test
  public void testHsqldbWithConnection()
    throws Exception
  {

    final Connection connection = checkConnection(getConnection());
    final DatabaseConnector hsqldbSystemConnector = new HyperSQLDatabaseConnector();

    final SchemaRetrievalOptions schemaRetrievalOptions = hsqldbSystemConnector
      .getSchemaRetrievalOptionsBuilder(connection).toOptions();

    final SchemaCrawlerOptions schemaCrawlerOptions = schemaCrawlerOptionsWithMaximumSchemaInfoLevel();
    requireNonNull(schemaRetrievalOptions,
                   "No database specific override options provided");

    final SchemaCrawler schemaCrawler = new SchemaCrawler(connection,
                                                          schemaRetrievalOptions,
                                                          schemaCrawlerOptions);
    final Catalog catalog1 = schemaCrawler.crawl();

    final Catalog catalog = catalog1;
    assertNotNull(catalog);

    assertEquals(6, catalog.getSchemas().size());
    final Schema schema = catalog.lookupSchema("PUBLIC.BOOKS").orElse(null);
    assertNotNull(schema);

    assertEquals(10, catalog.getTables(schema).size());
    final Table table = catalog.lookupTable(schema, "AUTHORS").orElse(null);
    assertNotNull(table);

    assertEquals(1, table.getTriggers().size());
    assertNotNull(table.lookupTrigger("TRG_AUTHORS").orElse(null));

  }

}

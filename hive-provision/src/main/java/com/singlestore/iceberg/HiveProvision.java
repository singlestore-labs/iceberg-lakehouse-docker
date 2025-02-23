package com.singlestore.iceberg;


import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.iceberg.aws.s3.S3FileIO;
import org.apache.iceberg.aws.s3.S3FileIOProperties;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.hive.HiveCatalog;
import java.util.UUID;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.DataFile;

public class HiveProvision {
    private static GenericRecord record(Schema schema, Object... values) {
        GenericRecord rec = GenericRecord.create(schema);
        for (int i = 0; i < values.length; i += 1) {
          rec.set(i, values[i]);
        }
        return rec;
    }

    public static void main(String[] args) throws IOException{
        HiveCatalog hiveCatalog = new HiveCatalog();

        Map<String, String> catalogProperties = new HashMap<String, String>();
        catalogProperties.put(CatalogProperties.FILE_IO_IMPL, S3FileIO.class.getName());
        catalogProperties.put(S3FileIOProperties.CLIENT_FACTORY, "org.apache.iceberg.aws.s3.DefaultS3FileIOAwsClientFactory");
        // System.setProperty("aws.accessKeyId", "admin");
        // System.setProperty("aws.secretAccessKey", "password");
        // System.setProperty("aws.region", "us-east-1");
        catalogProperties.put(S3FileIOProperties.ENDPOINT, "http://minio:9000");
        catalogProperties.put(S3FileIOProperties.PATH_STYLE_ACCESS, "true");

        // NB: Need to include hostname(aka localhost, hive) to the certificate to avoid
        // "javax.net.ssl.SSLHandshakeException: No subject alternative names matching IP address 127.0.0.1 found"
        //
        catalogProperties.put(CatalogProperties.URI, "thrift://hive:9083");
        catalogProperties.put(CatalogProperties.WAREHOUSE_LOCATION, "s3://warehouse/hive/");

        Configuration conf = new Configuration();
        MetastoreConf.setVar(conf, ConfVars.METASTORE_CLIENT_AUTH_MODE, "PLAIN");
        MetastoreConf.setVar(conf, ConfVars.METASTORE_CLIENT_PLAIN_USERNAME, "hmsuser");
        MetastoreConf.setBoolVar(conf, ConfVars.USE_SSL, true);
        MetastoreConf.setBoolVar(conf, ConfVars.USE_THRIFT_SASL, false);
        MetastoreConf.setBoolVar(conf, ConfVars.EXECUTE_SET_UGI, false);
        MetastoreConf.setVar(conf, ConfVars.SSL_TRUSTSTORE_TYPE, "PKCS12");
        MetastoreConf.setVar(conf, ConfVars.SSL_TRUSTSTORE_PATH, "/temp/my-ts.p12");
        MetastoreConf.setVar(conf, ConfVars.SSL_TRUSTSTORE_PASSWORD, "changeit");

        conf.set("hmsuser", "hmspasswd");

        hiveCatalog.setConf(conf);
        hiveCatalog.initialize("hive", catalogProperties);

        Schema SCHEMA = new Schema(Types.NestedField.required(1, "col", Types.LongType.get()));
        Namespace namespace = Namespace.of("default");
        TableIdentifier name = TableIdentifier.of(namespace, "test_table");
        if (hiveCatalog.tableExists(name))
        {
            hiveCatalog.dropTable(name, true);
        }
        Table table = hiveCatalog.createTable(name, SCHEMA);

        Schema schema = table.schema(); // ids are reassigned
        List<GenericRecord> testRecords = Arrays.asList(
            record(schema, 1L),
            record(schema, 2L),
            record(schema, 3L),
            record(schema, 4L),
            record(schema, 5L));

        String filepath = table.location() + "/" + UUID.randomUUID().toString();
        OutputFile file = table.io().newOutputFile(filepath);
        DataWriter<GenericRecord> dataWriter =
            Parquet.writeData(file)
                .schema(schema)
                .createWriterFunc(GenericParquetWriter::buildWriter)
                .overwrite()
                .withSpec(PartitionSpec.unpartitioned())
                .build();

        try {
            for (GenericRecord record : testRecords) {
                dataWriter.write(record);
            }
        } finally {
            dataWriter.close();
        }

        DataFile dataFile = dataWriter.toDataFile();
        table.newAppend().appendFile(dataFile).commit();
    }
}
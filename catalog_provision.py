from pyspark.sql import SparkSession
import os
import shutil


def cleanup_catalogs():
    # cleanup for SQL
    #
    try:
        os.remove("/tmp/sql-catalog.db")
    except OSError:
        pass

    # cleanup for hadoop
    #
    shutil.rmtree("/tmp/hadoop-warehouse", ignore_errors=True)


def provision_spark():
    # Initialize all catalogs
    #
    spark = SparkSession.builder.getOrCreate()

    # I couldn't make hive work with advanced setup and python, so use Java for provisioning
    #
    catalog_names = ["rest", "hadoop", "jdbc"]

    for catalog_name in catalog_names:
        spark.sql(
            f"""
        CREATE DATABASE IF NOT EXISTS {catalog_name}.default;
        """
        )

        spark.sql(f"CREATE TABLE {catalog_name}.default.test_table (col INT) USING iceberg")
        spark.sql(f"INSERT INTO {catalog_name}.default.test_table VALUES (1), (2), (3), (4), (5)")


def provision_java():
    # Latest spark libs don't support security we need for the Hive, so provision it with Java
    #
    os.system("java -cp /temp/hive-provision.jar com.singlestore.iceberg.HiveProvision")


cleanup_catalogs()
provision_spark()
provision_java()

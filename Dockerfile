#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Modifications made by SingleStore Inc. © 2025.
#

FROM python:3.9-bullseye

RUN apt-get -qq update && \
    apt-get -qq install -y --no-install-recommends \
      sudo \
      curl \
      vim \
      maven \
      unzip \
      openjdk-11-jdk \
      build-essential \
      software-properties-common \
      ssh

# Optional env variables
ENV SPARK_HOME=${SPARK_HOME:-"/opt/spark"}
ENV HADOOP_HOME=${HADOOP_HOME:-"/opt/hadoop"}
ENV PYTHONPATH=$SPARK_HOME/python:$SPARK_HOME/python/lib/py4j-0.10.9.7-src.zip:$PYTHONPATH

RUN mkdir -p ${HADOOP_HOME} && mkdir -p ${SPARK_HOME} && mkdir -p /home/iceberg/spark-events
WORKDIR ${SPARK_HOME}

ENV SPARK_VERSION=3.5.0
ENV ICEBERG_SPARK_RUNTIME_VERSION=3.5_2.12
ENV ICEBERG_VERSION=1.5.0
ENV PYICEBERG_VERSION=0.7.1

RUN curl --retry 3 -s -C - https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.tgz -o spark-${SPARK_VERSION}-bin-hadoop3.tgz \
 && tar xzf spark-${SPARK_VERSION}-bin-hadoop3.tgz --directory /opt/spark --strip-components 1 \
 && rm -rf spark-${SPARK_VERSION}-bin-hadoop3.tgz

# Download iceberg spark runtime
RUN curl -s https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-${ICEBERG_SPARK_RUNTIME_VERSION}/${ICEBERG_VERSION}/iceberg-spark-runtime-${ICEBERG_SPARK_RUNTIME_VERSION}-${ICEBERG_VERSION}.jar -Lo iceberg-spark-runtime-${ICEBERG_SPARK_RUNTIME_VERSION}-${ICEBERG_VERSION}.jar \
 && mv iceberg-spark-runtime-${ICEBERG_SPARK_RUNTIME_VERSION}-${ICEBERG_VERSION}.jar /opt/spark/jars

# Download AWS bundle
RUN curl -s https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-aws-bundle/${ICEBERG_VERSION}/iceberg-aws-bundle-${ICEBERG_VERSION}.jar -Lo /opt/spark/jars/iceberg-aws-bundle-${ICEBERG_VERSION}.jar

# Download sqlite-jdbc
RUN curl -s https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar -Lo /opt/spark/jars/sqlite-jdbc-3.46.0.0.jar


COPY spark-defaults.conf /opt/spark/conf
ENV PATH="/opt/spark/sbin:/opt/spark/bin:${PATH}"

RUN chmod u+x /opt/spark/sbin/* && \
    chmod u+x /opt/spark/bin/*

RUN mkdir /temp
COPY hive-provision /temp/hive-provision/
COPY hive/hive-truststore.p12 /temp/my-ts.p12
RUN cd /temp/hive-provision && mvn clean package && mv /temp/hive-provision/target/hive-provision-1.0-SNAPSHOT.jar /temp/hive-provision.jar

RUN rm -rf ~/.m2/repository
RUN rm -rf /temp/hive-provision/

RUN apt-get remove --purge -y \
    curl \
    vim \
    maven \
    unzip \
    build-essential \
    software-properties-common \
    ssh

RUN apt-get autoremove -y && \
    apt-get -qq clean && \
    rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh .
COPY catalog_provision.py .

ENTRYPOINT ["./entrypoint.sh"]
CMD ["notebook"]

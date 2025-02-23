#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Modifications made by SingleStore Inc. © 2025.
#

FROM apache/hive:4.0.0

ENV AWSSDK_VERSION=1.12.44
ENV HADOOP_VERSION=3.1.0

USER root
RUN apt-get update -qq && apt-get -qq -y install openjdk-11-jdk curl

RUN curl https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWSSDK_VERSION}/aws-java-sdk-bundle-${AWSSDK_VERSION}.jar -Lo /opt/hive/lib/aws-java-sdk-bundle-${AWSSDK_VERSION}.jar
RUN curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar -Lo /opt/hive/lib/hadoop-aws-${HADOOP_VERSION}.jar

COPY core-site.xml /opt/hadoop/etc/hadoop/core-site.xml
COPY hive-site.xml /opt/hive/conf/hive-site.xml


RUN mkdir /temp
COPY SampleAuthenticator.java /temp/SampleAuthenticator.java
COPY hive-keystore.p12 /temp/my-ks.p12
COPY hive-truststore.p12 /temp/my-ts.p12

# Hive comes with java 8, so we need to compile the java code with java 8 to avoid "emp/SampleAuthenticator has been compiled by a more recent version of the Java Runtime"
#
RUN javac -cp "/opt/hive/lib/*" -source 1.8 -target 1.8 /temp/SampleAuthenticator.java

# package temp, and I use temp.SampleAuthenticator in hive-site.xml
#
RUN cd / && jar cf /temp/sampleauth.jar temp/SampleAuthenticator.class
RUN mv /temp/sampleauth.jar /opt/hive/lib/.
RUN rm /temp/SampleAuthenticator.java && rm /temp/SampleAuthenticator.class

RUN apt-get remove --purge -y openjdk-11-jdk curl && \
    apt-get autoremove -y && \
    apt-get -qq clean && \
    rm -rf /var/lib/apt/lists/*

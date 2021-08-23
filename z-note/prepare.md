# shardingsphere 本地install


官方指导
```
## download the code of shardingsphere
git clone https://github.com/apache/incubator-shardingsphere.git

## checkout a specific version, example is 4.0.0-RC1
cd incubator-shardingsphere && git checkout 4.0.0-RC1

## install the dependencies
mvn clean install -Prelease
```

部分单元测试有问题
```mvn clean install -Prelease -Dmaven.test.skip=true```

依然出错
```
[ERROR] shardingsphere/shardingsphere-test/shardingsphere-parser-test/src/main/java/org/apache/shardingsphere/test/sql/parser/parameterized/jaxb/cases/domain/segment/impl/projection/impl/subquery/ExpectedSubqueryProjection.java:[26,33] package javax.xml.bind.annotation does not exist

```
发现jdk版本是11，需要换成 JDK1.8

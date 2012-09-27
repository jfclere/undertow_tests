java -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
     -Dlogging.configuration=file://$HOME/undertow_tests/logging.properties \
     -jar target/load_test.jar

#     -Dorg.jboss.logging.provider=jboss \

test.jks : 123456
test.jceks : 123456
test.p12 : 123456
test_no_password.p12 : <password is an empty string>
test_truststore.jks : 123456

test.jceks: is a custom keystore with alias "test"
test.jks: same as test.jceks
test.p12: same as test.jceks
test_no_password.p12: same as test.jceks but with empty password

test_truststore.jks: is actually the "cacerts" keystore from the JVM

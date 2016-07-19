marathon-maven-plugin
=====================

Maven plugin for interacting with Marathon

It can both process `marathon.json` config and use it to push it to Marathon host for deployment. 

# Maven documentation

For detailed documentation of goals and configuration options run in your project:
`mvn com.holidaycheck:marathon-maven-plugin:help -Ddetail=true`

# Basic usage

This plugin plays well with [docker-maven-plugin](https://github.com/spotify/docker-maven-plugin)
so you might want to use it e.g. to include proper versioning of Docker `image` in `marathon.json`.

Add plugin configuration to your `pom.xml`:

```xml
<plugin>
	<groupId>com.holidaycheck</groupId>
	<artifactId>marathon-maven-plugin</artifactId>
	<version>0.0.2</version>
	<configuration>
		<image>${docker-image-prefix}/${project.build.finalName}:${project.version}-${gitShortCommitId}</image>
		<marathonHost>http://${mesos.host}:${mesos.port}</marathonHost>
	</configuration>
	<executions>
		<execution>
			<id>processConfig</id>
			<phase>install</phase>
			<goals>
				<goal>processConfig</goal>
			</goals>
		</execution>
		<execution>
			<id>deploy</id>
			<phase>deploy</phase>
			<goals>
				<goal>deploy</goal>
			</goals>
            <configuration>
                    <waitForDeploymentFinished>true</waitForDeploymentFinished>
                    <waitForDeploymentTimeout>10</waitForDeploymentTimeout>
            </configuration>
		</execution>
		<execution>
			<id>apptasks</id>
			<phase>pre-integration-test</phase>
			<goals>
				<goal>apptasks</goal>
			</goals>
			<configuration>
				<propertyPrefix>mesos-</propertyPrefix>
			</configuration>
		</execution>
		<execution>
			<id>delete</id>
			<phase>post-integration-test</phase>
			<goals>
				<goal>delete</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

By default your template `marathon.json` should be in the root project directory.

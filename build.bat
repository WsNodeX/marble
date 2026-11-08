@echo off
set JAVAC=javac
set JAVA=java

if not exist build mkdir build
if not exist build\main mkdir build\main
if not exist build\test mkdir build\test

echo == compiling main ==
%JAVAC% -d build\main ^
  src\main\java\marble\core\*.java ^
  src\main\java\marble\id\*.java ^
  src\main\java\marble\bloom\*.java ^
  src\main\java\marble\hash\*.java ^
  src\main\java\marble\ratelimit\*.java ^
  src\main\java\marble\lock\*.java ^
  src\main\java\marble\examples\*.java

echo == running demo ==
%JAVA% -cp build\main marble.examples.Demo

echo == compiling tests ==
%JAVAC% -cp build\main -d build\test src\test\java\marble\test\*.java

if defined JUNIT_JAR (
  echo == running tests ==
  %JAVA% -cp "build\main;build\test;%JUNIT_JAR%" org.junit.platform.console.ConsoleLauncher execute --scan-class-path build\test
) else (
  echo Set JUNIT_JAR to a junit-platform-console-standalone jar to also run the test suite.
)
